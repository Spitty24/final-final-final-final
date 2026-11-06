package dev.freerun.alien;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A visible piece of a Swampwatch power: roots, a shockwave, a lightning arc, a splash. Nothing
 * collides with it and it never saves; it shows itself, follows its target if it has one, and goes.
 */
public class AbilityFx extends Entity {
    public static final int ROOTS = 0, SHOCKWAVE = 1, ARC = 2, DRAIN = 3, SNAP = 4, MIST = 5, DUST = 6, SPLAT = 7, AFTERIMAGE = 8, SLAG = 9;

    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(AbilityFx.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET = SynchedEntityData.defineId(AbilityFx.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LIFE = SynchedEntityData.defineId(AbilityFx.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(AbilityFx.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> BX = SynchedEntityData.defineId(AbilityFx.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> BY = SynchedEntityData.defineId(AbilityFx.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> BZ = SynchedEntityData.defineId(AbilityFx.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> YAW = SynchedEntityData.defineId(AbilityFx.class, EntityDataSerializers.FLOAT);

    public AbilityFx(EntityType<? extends AbilityFx> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(KIND, 0);
        b.define(TARGET, -1);
        b.define(LIFE, 20);
        b.define(RADIUS, 1.0f);
        b.define(BX, 0f);
        b.define(BY, 0f);
        b.define(BZ, 0f);
        b.define(YAW, 0f);
    }

    /** Spawns one. {@code target} may be null; {@code b} is a second point (arcs, drains). */
    public static AbilityFx spawn(ServerLevel level, int kind, Vec3 at, Entity target, Vec3 b, float radius, int life, float yaw) {
        AbilityFx fx = new AbilityFx(AlienRegistry.FX, level);
        fx.setPos(at.x, at.y, at.z);
        fx.entityData.set(KIND, kind);
        fx.entityData.set(TARGET, target == null ? -1 : target.getId());
        fx.entityData.set(LIFE, life);
        fx.entityData.set(RADIUS, radius);
        if (b != null) {
            fx.entityData.set(BX, (float) b.x);
            fx.entityData.set(BY, (float) b.y);
            fx.entityData.set(BZ, (float) b.z);
        }
        fx.entityData.set(YAW, yaw);
        level.addFreshEntity(fx);
        return fx;
    }

    public int kind() {
        return entityData.get(KIND);
    }

    public int life() {
        return entityData.get(LIFE);
    }

    public float radius() {
        return entityData.get(RADIUS);
    }

    public float fxYaw() {
        return entityData.get(YAW);
    }

    public Vec3 pointB() {
        return new Vec3(entityData.get(BX), entityData.get(BY), entityData.get(BZ));
    }

    public Entity target() {
        int id = entityData.get(TARGET);
        return id < 0 ? null : level().getEntity(id);
    }

    @Override
    public void tick() {
        super.tick();
        Entity t = target();
        // roots and puddles stay with whoever they caught; the slag glob rides its fireball
        if (t != null && (kind() == ROOTS || kind() == SLAG || kind() == SPLAT && radius() < 1.5f)) {
            setPos(t.getX(), kind() == SLAG ? t.getY() : t.getY(), t.getZ());
            if (kind() == SLAG && !t.isAlive() && !level().isClientSide) discard();
        }
        if (!level().isClientSide && tickCount >= life()) discard();
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return d < 128 * 128;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
