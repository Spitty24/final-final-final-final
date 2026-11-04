package dev.freerun.item.gun;

import dev.freerun.economy.ModSounds;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A real bullet: very fast, nearly flat, traced from where it was to where it's going every tick so
 * it can't skip through things. Draws as a bright tracer streak. Hitting a block kicks up dust and
 * sparks with a ricochet crack; hitting a mob hurts it (shotgun pellets all count).
 */
public class BulletEntity extends Projectile {
    private static final int MAX_LIFE = 30;
    private float damage = 4.0f;
    private int life;

    public BulletEntity(EntityType<? extends BulletEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (++life > MAX_LIFE) {
            discard();
            return;
        }
        Vec3 from = position();
        Vec3 vel = getDeltaMovement();
        Vec3 to = from.add(vel);
        BlockHitResult block = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));
        if (block.getType() != HitResult.Type.MISS) to = block.getLocation();

        if (!level().isClientSide) {
            EntityHitResult hit = ProjectileUtil.getEntityHitResult(level(), this, from, to,
                    getBoundingBox().expandTowards(vel).inflate(1.0), this::canHitEntity, 0.15f);
            if (hit != null) {
                hitEntity(hit.getEntity(), hit.getLocation());
                return;
            }
            if (block.getType() != HitResult.Type.MISS) {
                hitBlock(block);
                return;
            }
        } else if (block.getType() != HitResult.Type.MISS) {
            discard();
            return;
        }
        setPos(to);
        setDeltaMovement(vel.x * 0.995, vel.y * 0.995 - 0.008, vel.z * 0.995);
        if (level().isClientSide && life == 1) {
            level().addParticle(ParticleTypes.SMOKE, from.x, from.y, from.z, vel.x * 0.01, 0.01, vel.z * 0.01);
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        if (!super.canHitEntity(target)) return false;
        return !(target instanceof BulletEntity) && target != getOwner() && !target.isPassengerOfSameVehicle(getOwner() == null ? this : getOwner());
    }

    private void hitEntity(Entity target, Vec3 at) {
        Entity owner = getOwner();
        LivingEntity shooter = owner instanceof LivingEntity l ? l : null;
        if (target instanceof LivingEntity living) living.invulnerableTime = 0;   // every pellet counts
        boolean hurt = target.hurt(damageSources().mobProjectile(this, shooter), damage);
        if (hurt && target instanceof LivingEntity living) {
            Vec3 push = getDeltaMovement().normalize().scale(0.15);
            living.push(push.x, 0.02, push.z);
        }
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.DAMAGE_INDICATOR, at.x, at.y, at.z, 2, 0.1, 0.1, 0.1, 0.05);
            server.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 4, 0.1, 0.1, 0.1, 0.2);
        }
        discard();
    }

    private void hitBlock(BlockHitResult hit) {
        Vec3 at = hit.getLocation();
        BlockState state = level().getBlockState(hit.getBlockPos());
        if (level() instanceof ServerLevel server) {
            if (!state.isAir()) {
                server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), at.x, at.y, at.z, 8,
                        0.05, 0.05, 0.05, 0.15);
            }
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 3, 0.03, 0.03, 0.03, 0.2);
            server.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 2, 0.02, 0.02, 0.02, 0.01);
            server.playSound(null, at.x, at.y, at.z, ModSounds.BULLET_IMPACT, SoundSource.NEUTRAL, 0.7f,
                    0.85f + random.nextFloat() * 0.35f);
        }
        discard();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        discard();   // bullets don't survive a reload
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    public AABB tracerBox() {
        return getBoundingBox();
    }
}
