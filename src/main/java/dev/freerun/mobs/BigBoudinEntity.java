package dev.freerun.mobs;

import dev.freerun.food.FoodRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Big Boudin: the sausage plant's legend, a seven-foot walking link in a hard hat. Rumour says he
 * lives in the walk-in cooler; Ethan landing on Safety Nick shakes him loose. He waddles around
 * swatting people with a giant grill spatula (lots of knockback, not much damage), shouts his own
 * name, and after a few minutes waddles back off to wherever he came from. Beat him and he drops
 * a pile of boudin.
 */
public class BigBoudinEntity extends PathfinderMob {
    private static final String[] SHOUTS = {"BIG! BOUDIN!", "WHO ORDERED THE LINK?!", "SPATULA TIME!",
            "I AM PORK! I AM RICE! I AM BOUDIN!", "NOBODY LEAVES BEFORE LUNCH!", "SMOKED! FRESH! ANGRY!"};
    private int life = 3600;
    private int shoutCooldown = 40;

    public BigBoudinEntity(EntityType<? extends BigBoudinEntity> type, Level level) {
        super(type, level);
        setCustomName(Component.literal("§6Big Boudin"));
        setCustomNameVisible(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.ATTACK_KNOCKBACK, 2.5)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 40, true, false,
                e -> e instanceof Player p ? !p.isCreative() && !p.isSpectator()
                        : e instanceof RoadhouseNpcEntity || e instanceof CoopEntity || e instanceof EthanEntity));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            playSound(SoundEvents.SLIME_ATTACK, 1.5f, 0.6f);
            playSound(SoundEvents.ANVIL_PLACE, 0.4f, 1.8f);   // *CLANG* goes the spatula
        }
        return hit;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (--shoutCooldown <= 0) {
            shoutCooldown = 160 + random.nextInt(200);
            Component msg = Component.literal("§6<Big Boudin> §f" + SHOUTS[random.nextInt(SHOUTS.length)]);
            for (Player p : level().players()) if (distanceToSqr(p) < 32 * 32) p.displayClientMessage(msg, false);
            playSound(SoundEvents.RAVAGER_ROAR, 0.7f, 1.6f);
        }
        if (tickCount % 8 == 0 && level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 3.2, getZ(), 1, 0.2, 0.1, 0.2, 0.01);   // still smoking
        }
        if (--life <= 0) {
            Component msg = Component.literal("§6Big Boudin §7waddles off back toward the walk-in cooler. Until next time.");
            for (Player p : level().players()) if (distanceToSqr(p) < 48 * 48) p.displayClientMessage(msg, false);
            if (level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1.5, getZ(), 30, 0.6, 1.2, 0.6, 0.05);
            }
            discard();
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        spawnAtLocation(new ItemStack(FoodRegistry.BOUDIN, 4 + random.nextInt(5)));
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SLIME_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SLIME_DEATH;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SLIME_SQUISH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Life", life);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Life")) life = tag.getInt("Life");
    }
}
