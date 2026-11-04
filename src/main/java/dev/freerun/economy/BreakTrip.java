package dev.freerun.economy;

import dev.freerun.mobs.RoadhouseNpcEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A worker's walk over to the Double Quick on break: down the aisle, out across the parking lot,
 * up to the counter for a snack, then back to exactly where they were standing when the buzzer
 * went. Long walks are done in short hops so pathfinding never gives up, and anybody who gets
 * properly stuck is quietly put where they were going.
 */
public final class BreakTrip {
    private enum Stage { IDLE, GOING, SNACKING, RETURNING }

    private static final String[] ORDERS = {
            "One of them roller dogs, please.", "Can I get a sweet tea? The big one.", "Just a coffee. Black.",
            "Two corn dogs and a lottery ticket.", "Whatever's freshest in the hot case.", "Pack of sunflower seeds, please.",
            "Ten on pump three. Oh wait, I walked here.", "You got any boudin left?", "Energy drink. The blue one.",
            "Chicken tenders. And a slushie. Don't judge me."
    };

    private Stage stage = Stage.IDLE;
    private BlockPos home;
    private float homeYaw;
    private ItemStack savedHand = ItemStack.EMPTY;
    private Vec3 target;
    private int stuck;
    private Vec3 lastPos = Vec3.ZERO;
    private int bites;

    public boolean active() {
        return stage != Stage.IDLE;
    }

    /** Returns true while the trip is in charge of the mob (it shouldn't do its normal work). */
    public boolean tick(PathfinderMob mob) {
        if (!(mob.level() instanceof ServerLevel level) || mob.isPassenger()) return false;
        boolean onBreak = BreakTime.isBreak(level);
        switch (stage) {
            case IDLE -> {
                if (!onBreak || BreakTime.breakTicksLeft(level) < 900 || (mob.tickCount + mob.getId()) % 20 != 0) return false;
                RoadhouseNpcEntity clerk = findClerk(mob);
                if (clerk == null) return false;
                home = mob.blockPosition();
                homeYaw = mob.getYRot();
                savedHand = mob.getMainHandItem().copy();
                mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                target = clerk.position();
                stage = Stage.GOING;
                stuck = 0;
                return true;
            }
            case GOING -> {
                if (!onBreak) {
                    stage = Stage.RETURNING;
                    return true;
                }
                if (walk(mob, target, 3.8)) {
                    stage = Stage.SNACKING;
                    bites = 0;
                    Item snack = EconomyRegistry.BREAK_SNACKS.get(mob.getRandom().nextInt(EconomyRegistry.BREAK_SNACKS.size()));
                    mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(snack));
                    say(mob, ORDERS[mob.getRandom().nextInt(ORDERS.length)]);
                    level.playSound(null, mob.blockPosition(), ModSounds.CASH_REGISTER, net.minecraft.sounds.SoundSource.NEUTRAL, 0.5f, 1.0f);
                }
                return true;
            }
            case SNACKING -> {
                mob.getNavigation().stop();
                if (!onBreak) {
                    mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                    stage = Stage.RETURNING;
                    stuck = 0;
                    return true;
                }
                if ((mob.tickCount + mob.getId()) % 50 == 0 && bites < 6) {
                    bites++;
                    ItemStack held = mob.getMainHandItem();
                    if (!held.isEmpty()) {
                        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, held), mob.getX(), mob.getEyeY() - 0.2,
                                mob.getZ(), 6, 0.15, 0.1, 0.15, 0.03);
                    }
                    mob.playSound(SoundEvents.GENERIC_EAT, 0.6f, 1.0f);
                    mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                }
                return true;
            }
            case RETURNING -> {
                if (home == null || walk(mob, Vec3.atBottomCenterOf(home), 0.6)) {
                    if (home != null) {
                        mob.moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, homeYaw, 0.0f);
                        mob.setYHeadRot(homeYaw);
                        mob.setYBodyRot(homeYaw);
                    }
                    mob.getNavigation().stop();
                    mob.setItemSlot(EquipmentSlot.MAINHAND, savedHand);
                    savedHand = ItemStack.EMPTY;
                    stage = Stage.IDLE;
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    /** One step of a long walk. Returns true once within {@code reach} of {@code to}. */
    private boolean walk(PathfinderMob mob, Vec3 to, double reach) {
        Vec3 here = mob.position();
        double dx = to.x - here.x;
        double dz = to.z - here.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < reach && Math.abs(to.y - here.y) < 2.5) return true;
        if (mob.tickCount % 10 == 0) {
            if (here.distanceToSqr(lastPos) < 0.04) stuck += 10;
            else stuck = Math.max(0, stuck - 5);
            lastPos = here;
            if (stuck > 240) {   // 12 seconds going nowhere: just get there
                Vec3 spot = reach > 1.0 ? standSpot(mob, to) : to;
                mob.moveTo(spot.x, spot.y, spot.z, mob.getYRot(), 0.0f);
                mob.getNavigation().stop();
                stuck = 0;
                return true;
            }
            Vec3 hop = to;
            if (dist > 12.0) hop = new Vec3(here.x + dx / dist * 11.0, here.y, here.z + dz / dist * 11.0);
            if (mob.getNavigation().isDone() || mob.tickCount % 40 == 0) {
                mob.getNavigation().moveTo(hop.x, hop.y, hop.z, 1.0);
            }
        }
        return false;
    }

    /** Somewhere to stand near {@code to}: open floor at least a couple of blocks away (not behind the counter). */
    private static Vec3 standSpot(PathfinderMob mob, Vec3 to) {
        BlockPos c = BlockPos.containing(to);
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-4, -1, -4), c.offset(4, 1, 4))) {
            double dc = p.distToCenterSqr(to);
            if (dc < 4.0) continue;
            if (!mob.level().getBlockState(p).isAir() || !mob.level().getBlockState(p.above()).isAir()) continue;
            if (!mob.level().getBlockState(p.below()).isFaceSturdy(mob.level(), p.below(), net.minecraft.core.Direction.UP)) continue;
            double d = p.distToCenterSqr(mob.position()) + dc * 2.0;
            if (d < bestD) {
                bestD = d;
                best = p.immutable();
            }
        }
        return best == null ? to : Vec3.atBottomCenterOf(best);
    }

    private static RoadhouseNpcEntity findClerk(PathfinderMob mob) {
        return mob.level().getEntitiesOfClass(RoadhouseNpcEntity.class, new AABB(mob.blockPosition()).inflate(110.0, 24.0, 110.0),
                        n -> n.getRole() == RoadhouseNpcEntity.Role.CLERK).stream()
                .min(java.util.Comparator.comparingDouble(n -> n.distanceToSqr(mob))).orElse(null);
    }

    private static void say(PathfinderMob mob, String line) {
        Component name = mob.getCustomName() != null ? mob.getCustomName() : mob.getName();
        Component msg = Component.literal("§7<" + name.getString() + "> §f" + line);
        for (Player p : mob.level().players()) if (p.distanceToSqr(mob) < 12 * 12) p.displayClientMessage(msg, false);
    }

    public void save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        if (stage == Stage.IDLE || home == null) return;
        CompoundTag t = new CompoundTag();
        t.putInt("Stage", stage.ordinal());
        t.putInt("X", home.getX());
        t.putInt("Y", home.getY());
        t.putInt("Z", home.getZ());
        t.putFloat("Yaw", homeYaw);
        if (!savedHand.isEmpty()) t.put("Hand", savedHand.save(registries));
        tag.put("BreakTrip", t);
    }

    public void load(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        if (!tag.contains("BreakTrip")) return;
        CompoundTag t = tag.getCompound("BreakTrip");
        home = new BlockPos(t.getInt("X"), t.getInt("Y"), t.getInt("Z"));
        homeYaw = t.getFloat("Yaw");
        savedHand = t.contains("Hand") ? ItemStack.parseOptional(registries, t.getCompound("Hand")) : ItemStack.EMPTY;
        stage = Stage.RETURNING;   // whatever they were doing, head back
    }
}
