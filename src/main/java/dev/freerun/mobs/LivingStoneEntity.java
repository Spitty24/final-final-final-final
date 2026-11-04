package dev.freerun.mobs;

import dev.freerun.world.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.EnumSet;
import java.util.UUID;

/**
 * A block of the mod's fake "Stone" that came alive when you mined it. It follows whoever mined
 * it everywhere (teleporting to catch up, like a pet) and calls them a chud. Harmless. Kill it and
 * you get the stone back.
 */
public class LivingStoneEntity extends PathfinderMob {
    private static final String[] LINES = {
            "Chud.", "Hey. Chud.", "Nice pickaxe, chud.", "You mined me. Real classy, chud.",
            "Where are we going, chud?", "Walk faster, chud.", "I was a perfectly good rock, chud.",
            "Chud alert. Oh wait, it's just you.", "Don't look at me, chud.", "Classic chud behavior.",
            "What are you building, chud? A chud house?", "I'm telling the other rocks about you, chud.",
            "You call that a staircase, chud?", "Chud.", "Absolute chud."
    };

    private static final String[] CHUD_MAXING_LINES = {
            "Now THAT is chud-maxing.", "Look at you. Peak chud.", "I've never been prouder, chud.",
            "You ate the sausage. You absolute chud.", "Chud-maxing achieved. I'm literally crying."
    };

    private UUID owner;
    private int talkCooldown = 60;

    public LivingStoneEntity(EntityType<? extends LivingStoneEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
    }

    public void setOwner(Player player) {
        owner = player.getUUID();
    }

    private Player owner() {
        return owner == null ? null : level().getPlayerByUUID(owner);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new FollowOwnerGoal());
        goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 10.0f));
        goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    /** Waddles after its owner; if it falls far behind it just shows up next to them. */
    private class FollowOwnerGoal extends Goal {
        FollowOwnerGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Player p = owner();
            return p != null && !p.isSpectator() && distanceToSqr(p) > 3.0 * 3.0;
        }

        @Override
        public boolean canContinueToUse() {
            Player p = owner();
            return p != null && distanceToSqr(p) > 2.0 * 2.0;
        }

        @Override
        public void tick() {
            Player p = owner();
            if (p == null) return;
            getLookControl().setLookAt(p, 20.0f, getMaxHeadXRot());
            if (distanceToSqr(p) > 24.0 * 24.0) {
                BlockPos at = p.blockPosition().offset(random.nextInt(3) - 1, 0, random.nextInt(3) - 1);
                if (level().noCollision(LivingStoneEntity.this, getDimensions(getPose()).makeBoundingBox(net.minecraft.world.phys.Vec3.atBottomCenterOf(at)))) {
                    moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, getYRot(), getXRot());
                    getNavigation().stop();
                    say(p, "Thought you could lose me, chud?");
                }
                return;
            }
            getNavigation().moveTo(p, 1.1);
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }
    }

    public void say(Player to, String line) {
        Component msg = Component.literal("§7<Stone> §f" + line);
        if (to != null) to.displayClientMessage(msg, false);
        for (Player p : level().players()) {
            if (p != to && distanceToSqr(p) < 12 * 12) p.displayClientMessage(msg, false);
        }
        playSound(SoundEvents.STONE_HIT, 1.0f, 0.6f + random.nextFloat() * 0.2f);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (--talkCooldown > 0) return;
        talkCooldown = 300 + random.nextInt(400);   // every 15-35 seconds
        Player p = owner();
        if (p == null || distanceToSqr(p) >= 16 * 16) return;
        if (p.hasEffect(dev.freerun.food.FoodRegistry.CHUD_MAXING)) {
            say(p, CHUD_MAXING_LINES[random.nextInt(CHUD_MAXING_LINES.length)]);
        } else {
            say(p, LINES[random.nextInt(LINES.length)]);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && isAlive() && source.getEntity() instanceof Player p && random.nextInt(2) == 0) {
            say(p, random.nextBoolean() ? "Ow. Chud." : "Violence. Typical chud.");
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide) {
            if (source.getEntity() instanceof Player p) say(p, "Whatever... chud...");
            spawnAtLocation(new ItemStack(BlockRegistry.POINTLESS_ORE));
        }
        super.die(source);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.STONE_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.STONE_BREAK;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (owner != null) tag.putUUID("Owner", owner);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) owner = tag.getUUID("Owner");
    }
}
