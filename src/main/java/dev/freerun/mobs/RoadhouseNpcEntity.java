package dev.freerun.mobs;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Restaurant staff and customers. They stay at their spot (no wandering), look at nearby players,
 * and respond when right-clicked:
 *  - HOST greets you.
 *  - WAITER brings a steak and rolls (30s cooldown each).
 *  - COOK hands over a baked potato (30s cooldown each).
 *  - CUSTOMER makes small talk.
 */
public class RoadhouseNpcEntity extends PathfinderMob {
    /**
     * New roles must be appended (the ordinal is saved). The office pair only appear in the office;
     * WELDER and PRESS_OPERATOR work in the welding factory.
     */
    public enum Role { HOST, WAITER, COOK, CUSTOMER, SUPERVISOR, SAFETY, WELDER, PRESS_OPERATOR, SAUSAGE_MAKER, CLERK, SHOPKEEPER }

    private static final Role[] RESTAURANT_ROLES = {Role.HOST, Role.WAITER, Role.COOK, Role.CUSTOMER};

    public static final int CUSTOMER_VARIANTS = 3;
    private static final int GIFT_COOLDOWN = 600;   // 30 seconds

    private static final EntityDataAccessor<Integer> DATA_ROLE =
            SynchedEntityData.defineId(RoadhouseNpcEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_VARIANT =
            SynchedEntityData.defineId(RoadhouseNpcEntity.class, EntityDataSerializers.INT);
    /** Slater and Nick on a visit to the factory floor (not the office pair in the Roadhouse). */
    private static final EntityDataAccessor<Boolean> DATA_VISITOR =
            SynchedEntityData.defineId(RoadhouseNpcEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_ON_BREAK =
            SynchedEntityData.defineId(RoadhouseNpcEntity.class, EntityDataSerializers.BOOLEAN);

    private final dev.freerun.economy.BreakTrip breakTrip = new dev.freerun.economy.BreakTrip();

    private static final String[] CLERK_LINES = {
            "Welcome to Double Quick! Roller dogs are fresh. Ish.",
            "Pump three's acting up again, just so you know.",
            "We got hot boudin by the register.",
            "Bathroom key's on the hubcap. Don't ask.",
            "You want a scratch-off with that?"
    };
    private static final String[] SHOPKEEPER_LINES = {
            "Come on in, we got a little bit of everything.",
            "If we don't have it, you don't need it.",
            "I'll buy that steel off you if you're selling.",
            "Car's out back if you got the money. Runs good. Mostly."
    };

    private static final String[] HOST_LINES = {
            "Welcome in! Just one today? Grab any open seat.",
            "Howdy, partner! No wait at all right now.",
            "Make yourself at home. Peanuts are by the door!",
            "Table's ready whenever you are."
    };
    private static final String[] CUSTOMER_LINES = {
            "These rolls are unbelievable.",
            "I come here every Friday, no exceptions.",
            "Get the steak. Medium rare. Trust me.",
            "Did you see that Wingus outside?!",
            "Best peanuts in the whole county.",
            "I'm saving room for dessert. Probably.",
            "Is it just me or is the cook singing back there?"
    };

    private static final String[] WELDER_LINES = {
            "Don't stare at the arc. Trust me on that one.",
            "Need a torch? Empty hand, right-click any welder machine.",
            "Click one part, then the part touching it. That's a weld.",
            "Sneak-click with the torch if you need to cut a weld back apart.",
            "Gravity gun's the only way to line up the big assemblies.",
            "Forty years of welding and I still burn my wrist every Tuesday."
    };
    private static final String[] PRESS_LINES = {
            "Sheet goes in, part comes out. Mind your fingers.",
            "Empty hand on the press flips it between CUT and BEND.",
            "CUT gets you two plates. BEND gets you a bracket, or a channel from a plate.",
            "Stock racks are along the west wall. Help yourself.",
            "The gear chest is at the back. Don't tell the foreman I told you.",
            "This press has been running since before you were born."
    };

    private static final String[] SAUSAGE_LINES = {
            "Load the crop in the hopper first, then the meat. Never the other way around.",
            "Pork on its own makes boudin. Add beetroot and it's chorizo.",
            "Beef and potato: kielbasa. Beef and wheat: bratwurst. Write that down.",
            "Chicken wants an apple. Mutton wants beetroot. Rabbit wants a carrot.",
            "Fish and dried kelp. Don't make that face, it's good.",
            "Somebody keeps making chud sausage on night shift. Rotten flesh and Stone. Disgusting.",
            "Hairnets are not optional."
    };

    private long lastGiftTime = -GIFT_COOLDOWN;

    public RoadhouseNpcEntity(EntityType<? extends RoadhouseNpcEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ROLE, Role.CUSTOMER.ordinal());
        builder.define(DATA_VARIANT, 0);
        builder.define(DATA_VISITOR, false);
        builder.define(DATA_ON_BREAK, false);
    }

    /** Away from their station on a break (the model drops the work pose). */
    public boolean onBreak() {
        return entityData.get(DATA_ON_BREAK);
    }

    public boolean isVisitor() {
        return entityData.get(DATA_VISITOR);
    }

    private int visitorIdle;

    /** Called every tick by the visit script that's directing this NPC. */
    public void keepVisiting() {
        visitorIdle = 0;
    }

    public void setVisitor(boolean visitor) {
        entityData.set(DATA_VISITOR, visitor);
    }

    /** The store this NPC runs the register at, if any. */
    public dev.freerun.economy.Shops.Shop shop() {
        return switch (getRole()) {
            case CLERK -> dev.freerun.economy.Shops.Shop.DOUBLE_QUICK;
            case SHOPKEEPER -> dev.freerun.economy.Shops.Shop.GENERAL_STORE;
            default -> null;
        };
    }

    /** Overheard lines (used by visits and robberies). */
    public void shout(String line) {
        say(line);
    }

    public Role getRole() {
        Role[] roles = Role.values();
        return roles[Mth.clamp(entityData.get(DATA_ROLE), 0, roles.length - 1)];
    }

    public int getVariant() {
        return entityData.get(DATA_VARIANT);
    }

    public void setRole(Role role, int variant) {
        entityData.set(DATA_ROLE, role.ordinal());
        entityData.set(DATA_VARIANT, variant);
        setCustomName(Component.literal(title(role)));
    }

    private static String title(Role role) {
        return switch (role) {
            case HOST -> "Host";
            case WAITER -> "Server";
            case COOK -> "Cook";
            case CUSTOMER -> "Customer";
            case SUPERVISOR -> "Supervisor Slater";
            case SAFETY -> "Safety Nick";
            case WELDER -> "Welder";
            case PRESS_OPERATOR -> "Press Operator";
            case SAUSAGE_MAKER -> "Sausage Maker";
            case CLERK -> "Double Quick Clerk";
            case SHOPKEEPER -> "Shopkeeper";
        };
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    /** Spawn eggs give a random role. (Roadhouse-placed NPCs set theirs explicitly.) */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType reason, SpawnGroupData spawnData) {
        SpawnGroupData out = super.finalizeSpawn(level, difficulty, reason, spawnData);
        setRole(RESTAURANT_ROLES[random.nextInt(RESTAURANT_ROLES.length)], random.nextInt(CUSTOMER_VARIANTS));
        return out;
    }

    // --- the office scene ---

    private RoadhouseNpcEntity partner;
    private int partnerScan;

    // --- factory work ---

    private net.minecraft.core.BlockPos workSpot;
    private int workScan;
    private int lookAtPlayerTicks;

    /** True while this worker's arc is lit; the client model uses the same clock for its pose. */
    public static boolean arcOn(long gameTime, int entityId) {
        return Math.floorMod(gameTime + entityId * 7L, 90L) < 60L;
    }

    /** Ticks into this operator's 100-tick press cycle. */
    public static int pressPhase(long gameTime, int entityId) {
        return (int) Math.floorMod(gameTime + entityId * 13L, 100L);
    }

    private boolean isWorkBlock(net.minecraft.core.BlockPos pos) {
        net.minecraft.world.level.block.state.BlockState st = level().getBlockState(pos);
        return switch (getRole()) {
            case WELDER -> st.is(net.minecraft.world.level.block.Blocks.IRON_BLOCK);
            case SAUSAGE_MAKER -> st.getBlock() instanceof dev.freerun.food.SausageStufferBlock;
            default -> st.getBlock() instanceof dev.freerun.factory.PressBrakeBlock;
        };
    }

    private void doFactoryWork(Role role) {
        if (workSpot == null || --workScan <= 0) {
            workScan = 100;
            if (workSpot == null || !isWorkBlock(workSpot)) {
                workSpot = null;
                double best = Double.MAX_VALUE;
                for (net.minecraft.core.BlockPos p : net.minecraft.core.BlockPos.betweenClosed(
                        blockPosition().offset(-2, -1, -2), blockPosition().offset(2, 1, 2))) {
                    if (isWorkBlock(p)) {
                        double d = p.distToCenterSqr(position());
                        if (d < best) {
                            best = d;
                            workSpot = p.immutable();
                        }
                    }
                }
            }
        }
        if (lookAtPlayerTicks > 0) {
            lookAtPlayerTicks--;
            return;
        }
        if (workSpot == null || !(level() instanceof net.minecraft.server.level.ServerLevel server)) return;

        double tx = workSpot.getX() + 0.5;
        double ty = workSpot.getY() + (role == Role.WELDER ? 1.0 : 0.6);
        double tz = workSpot.getZ() + 0.5;
        getLookControl().setLookAt(tx, ty, tz, 30.0f, 30.0f);
        long now = level().getGameTime();

        if (role == Role.WELDER) {
            if (!arcOn(now, getId())) return;
            long t = now + getId();
            if (t % 3 == 0) {
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                        tx, ty + 0.05, tz, 3, 0.12, 0.04, 0.12, 0.08);
            }
            if (t % 20 == 0) {
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                        tx, ty + 0.3, tz, 2, 0.08, 0.1, 0.08, 0.01);
            }
            if (t % 15 == 0) playSound(net.minecraft.sounds.SoundEvents.CAMPFIRE_CRACKLE, 0.8f, 1.8f);
        } else if (role == Role.SAUSAGE_MAKER) {
            if (Math.floorMod(now + getId() * 11L, 140L) == 0L) {
                net.minecraft.world.level.block.state.BlockState st = level().getBlockState(workSpot);
                if (st.getBlock() instanceof dev.freerun.food.SausageStufferBlock) {
                    dev.freerun.food.SausageStufferBlock.crank(server, workSpot, st);
                    server.sendParticles(new net.minecraft.core.particles.ItemParticleOption(
                                    net.minecraft.core.particles.ParticleTypes.ITEM,
                                    new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.PORKCHOP)),
                            tx, ty + 0.4, tz, 6, 0.1, 0.1, 0.1, 0.04);
                }
            }
        } else if (pressPhase(now, getId()) == 0) {
            net.minecraft.world.level.block.state.BlockState st = level().getBlockState(workSpot);
            if (st.getBlock() instanceof dev.freerun.factory.PressBrakeBlock) {
                dev.freerun.factory.PressBrakeBlock.cycle(server, workSpot, st);
                if (Math.floorMod(now / 100L + getId(), 2L) == 0L) dropPartOnBelt();
            }
        }
    }

    private static final dev.freerun.factory.PartType[] PRODUCTS = {
            dev.freerun.factory.PartType.BRACKET, dev.freerun.factory.PartType.CHANNEL, dev.freerun.factory.PartType.PLATE};

    /** Press operators put each part they form onto the conveyor behind them, if there's room on it. */
    private void dropPartOnBelt() {
        java.util.Optional<net.minecraft.core.BlockPos> found = net.minecraft.core.BlockPos.findClosestMatch(
                blockPosition(), 3, 1, p -> level().getBlockState(p).getBlock() instanceof dev.freerun.factory.ConveyorBlock);
        if (found.isEmpty()) return;
        net.minecraft.core.BlockPos belt = found.get().immutable();
        net.minecraft.world.phys.AABB near = new net.minecraft.world.phys.AABB(belt).inflate(1.5, 1.0, 1.5);
        if (!level().getEntitiesOfClass(dev.freerun.factory.SteelPartEntity.class, near).isEmpty()) return;
        dev.freerun.factory.SteelPartEntity part = dev.freerun.factory.SteelPartEntity.single(level(),
                PRODUCTS[random.nextInt(PRODUCTS.length)], belt.getX() + 0.5,
                belt.getY() + dev.freerun.factory.ConveyorBlock.TOP, belt.getZ() + 0.5, random.nextInt(4));
        if (level().noCollision(part, part.getBoundingBox().deflate(1.0E-3))) level().addFreshEntity(part);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        Role role = getRole();
        if (role == Role.WELDER || role == Role.PRESS_OPERATOR || role == Role.SAUSAGE_MAKER) {
            boolean away = breakTrip.tick(this);
            if (away != onBreak()) entityData.set(DATA_ON_BREAK, away);
            if (away) return;
            doFactoryWork(role);
            return;
        }
        if (role != Role.SUPERVISOR && role != Role.SAFETY) return;
        if (isVisitor()) {   // the factory visit scripts these two itself; leave if it's gone
            if (++visitorIdle > 400) discard();
            return;
        }

        // Keep facing each other.
        if (--partnerScan <= 0 || partner == null || !partner.isAlive()) {
            partnerScan = 20;
            Role other = role == Role.SUPERVISOR ? Role.SAFETY : Role.SUPERVISOR;
            partner = level().getEntitiesOfClass(RoadhouseNpcEntity.class, getBoundingBox().inflate(8),
                    n -> n.getRole() == other).stream().findFirst().orElse(null);
        }
        boolean onCall = role == Role.SUPERVISOR && OfficeSkit.slaterOnCall(level().getGameTime());
        if (partner != null && !onCall) getLookControl().setLookAt(partner, 30.0f, 30.0f);

        int t = OfficeSkit.time(level().getGameTime());
        if (role == Role.SUPERVISOR) {
            if (t == OfficeSkit.LINE_IT_MEETING) say("Nick. IT has a meeting in here. You need to leave.");
            if (t == OfficeSkit.LINE_SICK_AND_TIRED) say("I am getting SICK and TIRED of you, Safety Nick.");
            // The phone call
            if (t == OfficeSkit.RING_1 || t == OfficeSkit.RING_2) {
                playSound(net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, 1.5f, 1.6f);   // ringtone
            }
            if (t == OfficeSkit.LINE_ANSWER) say("*answers phone* Slater speaking.");
            if (t == OfficeSkit.LINE_BOSS) {
                say("§d<Slater's Boss (on the phone)> §fDid you finally get rid of that sick bastard?", true);
            }
            if (t == OfficeSkit.LINE_REPLY) say("Damn right I did. I hate him.");
        } else {
            if (t == OfficeSkit.LINE_HATES_ME) say("SLATER HATES ME!!");
            if (t == OfficeSkit.LINE_HATES_ME_AGAIN) say("SLATER HATES MEEEEEE");
            if (t == OfficeSkit.LINE_SNIFF) say("*sniff* ...he hates me, man.");
            if (OfficeSkit.nickDown(level().getGameTime()) && level() instanceof net.minecraft.server.level.ServerLevel server) {
                if (t % 4 == 0) {
                    server.sendParticles(net.minecraft.core.particles.ParticleTypes.SPLASH,
                            getX(), getY() + 0.4, getZ(), 4, 0.6, 0.15, 0.6, 0.1);   // tears everywhere
                }
                if (t % 25 == 0) {
                    playSound(net.minecraft.sounds.SoundEvents.VILLAGER_HURT, 1.0f, 1.7f);
                }
            }
        }
    }

    /** Dialogue the player overhears when they're near the office. */
    private void say(String line) {
        say("§7<" + title(getRole()) + "> §f" + line, true);
    }

    /** Sends a fully formatted line (speaker included) to nearby players. */
    private void say(String formatted, boolean raw) {
        Component msg = Component.literal(formatted);
        for (Player p : level().players()) {
            if (distanceToSqr(p) < 12 * 12) p.displayClientMessage(msg, false);
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!level().isClientSide) respond(player);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    private void respond(Player player) {
        long now = level().getGameTime();
        boolean ready = now - lastGiftTime >= GIFT_COOLDOWN;
        String line;
        switch (getRole()) {
            case HOST -> line = pick(HOST_LINES);
            case WAITER -> {
                if (ready) {
                    give(player, new ItemStack(Items.COOKED_BEEF));
                    give(player, new ItemStack(Items.BREAD, 2));
                    lastGiftTime = now;
                    line = "Here you go: one steak, cooked just how you like it, and some fresh rolls!";
                } else {
                    line = "I'll be right back to check on you, hon!";
                }
            }
            case COOK -> {
                if (ready) {
                    give(player, new ItemStack(Items.BAKED_POTATO));
                    lastGiftTime = now;
                    line = "Order up! One loaded baked potato.";
                } else {
                    line = "Grill's slammed, give me a minute!";
                }
            }
            case SUPERVISOR -> line = isVisitor() ? "Do you work here? Then get back to work."
                    : OfficeSkit.slaterOnCall(now)
                    ? "*covers phone* Hold on, I'm on with the boss."
                    : "Not now. I'm dealing with Nick.";
            case SAFETY -> line = isVisitor() ? "Not now, I'm writing Ethan up."
                    : OfficeSkit.nickDown(now) ? "*incoherent sobbing*" : "*sniff* Did you hear what he said to me?";
            case WELDER -> line = pick(WELDER_LINES);
            case PRESS_OPERATOR -> line = pick(PRESS_LINES);
            case SAUSAGE_MAKER -> line = pick(SAUSAGE_LINES);
            case CLERK, SHOPKEEPER -> {
                line = pick(getRole() == Role.CLERK ? CLERK_LINES : SHOPKEEPER_LINES);
                if (player instanceof net.minecraft.server.level.ServerPlayer sp
                        && net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(sp, dev.freerun.net.ShopPayloads.Open.TYPE)) {
                    net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(sp,
                            new dev.freerun.net.ShopPayloads.Open(getId(), shop().ordinal(),
                                    dev.freerun.economy.Shops.pricePercent(sp.serverLevel(), blockPosition())));
                }
            }
            default -> line = pick(CUSTOMER_LINES);
        }
        getLookControl().setLookAt(player, 30.0f, 30.0f);
        lookAtPlayerTicks = 40;
        player.displayClientMessage(Component.literal("§6<" + title(getRole()) + "> §f" + line), false);
    }

    private String pick(String[] lines) {
        return lines[random.nextInt(lines.length)];
    }

    private static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    @Override
    public boolean isPushable() {
        return false;   // staff stay at their stations
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Role", getRole().ordinal());
        tag.putInt("Variant", getVariant());
        tag.putLong("LastGift", lastGiftTime);
        tag.putBoolean("Visitor", isVisitor());
        breakTrip.save(tag, registryAccess());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        Role[] roles = Role.values();
        setRole(roles[Mth.clamp(tag.getInt("Role"), 0, roles.length - 1)], tag.getInt("Variant"));
        lastGiftTime = tag.getLong("LastGift");
        setVisitor(tag.getBoolean("Visitor"));
        breakTrip.load(tag, registryAccess());
    }
}
