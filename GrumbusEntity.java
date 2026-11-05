package dev.freerun.mobs;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Grumbus: a small rotund gray man who follows you and beats you up — harmlessly. He deals no
 * damage and causes no knockback; he just swings, makes the no-damage "tap" sound, and throws
 * angry particles to annoy you. Summoned by {@link GrumbusManager} ~5 seconds after you spawn.
 */
public class GrumbusEntity extends PathfinderMob {

    public GrumbusEntity(EntityType<? extends GrumbusEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3, true));
        goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 12.0f));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, null));
    }

    /** All annoyance, no harm: swing + sound + particles, but no damage and no knockback. */
    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        swing(InteractionHand.MAIN_HAND);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_ATTACK_NODAMAGE,
                SoundSource.HOSTILE, 1.0f, 1.4f);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                    target.getX(), target.getEyeY(), target.getZ(), 4, 0.25, 0.25, 0.25, 0.0);
        }
        return true; // "hit" registered for the AI, but we dealt nothing
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
