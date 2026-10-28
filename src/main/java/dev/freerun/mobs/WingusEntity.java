package dev.freerun.mobs;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * The Wingus: flies around aimlessly looking for a Grumbus to eat. When it catches one, the
 * Grumbus vanishes and the Wingus flies off north until it despawns.
 *
 * Flavus Florbington can beckon and tame one. A tamed Wingus carries him around, keeps hunting
 * Grumbus for fun, never flies off, and is saved with the world.
 *
 * Flies through blocks (like a Vex) so it never gets stuck. Wild ones are never saved to disk.
 */
public class WingusEntity extends PathfinderMob {
    private enum State { WANDER, HUNT, BECKONED, TAMED, LEAVING }

    private static final double WANDER_SPEED = 0.25;
    private static final double HUNT_SPEED = 0.45;
    private static final int LEAVE_TICKS = 400;          // ~20s of flying north, then gone

    private State state = State.WANDER;
    private boolean tamed;
    private Vec3 wanderTarget;
    private int wanderTimer;
    private int searchTimer;
    private int leaveTicks;
    private GrumbusEntity prey;
    private Entity beckoner;

    public WingusEntity(EntityType<? extends WingusEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setPersistenceRequired();   // no random despawning while it wanders
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FLYING_SPEED, 0.6);
    }

    public boolean isTamed() {
        return tamed;
    }

    /** Called by Flavus: come here. Ignored once tamed or while flying off. */
    public void beckon(Entity who) {
        if (tamed || state == State.LEAVING) return;
        beckoner = who;
        state = State.BECKONED;
    }

    public void tameBy(Entity who) {
        tamed = true;
        beckoner = null;
        state = State.TAMED;
    }

    @Override
    protected void registerGoals() {
        // all movement is handled in customServerAiStep
    }

    @Override
    public void tick() {
        this.noPhysics = true;      // fly through terrain, Vex-style
        super.tick();
        this.noPhysics = false;
        setNoGravity(true);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!tamed && !getPassengers().isEmpty()) tameBy(getFirstPassenger());

        switch (state) {
            case WANDER, TAMED -> wanderAndSearch();
            case HUNT -> {
                if (prey == null || !prey.isAlive()) {
                    prey = null;
                    state = tamed ? State.TAMED : State.WANDER;
                } else {
                    flyToward(prey.position().add(0, 0.4, 0), HUNT_SPEED);
                    if (distanceTo(prey) < 1.5f) eat();
                }
            }
            case BECKONED -> {
                if (beckoner == null || !beckoner.isAlive()) {
                    beckoner = null;
                    state = tamed ? State.TAMED : State.WANDER;
                } else {
                    flyToward(beckoner.position().add(0, 1.2, 0), HUNT_SPEED);
                }
            }
            case LEAVING -> {   // head north (-Z) and slightly up until despawn
                setDeltaMovement(0, 0.04, -0.5);
                face(getDeltaMovement());
                if (++leaveTicks > LEAVE_TICKS) discard();
            }
        }
    }

    private void wanderAndSearch() {
        if (--searchTimer <= 0) {
            searchTimer = 10;
            prey = findGrumbus();
            if (prey != null) {
                state = State.HUNT;
                return;
            }
        }
        if (wanderTarget == null || --wanderTimer <= 0 || position().distanceToSqr(wanderTarget) < 4) {
            pickWanderTarget();
        }
        flyToward(wanderTarget, WANDER_SPEED);
    }

    private GrumbusEntity findGrumbus() {
        List<GrumbusEntity> found = level().getEntitiesOfClass(GrumbusEntity.class,
                getBoundingBox().inflate(32), GrumbusEntity::isAlive);
        return found.stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
    }

    private void pickWanderTarget() {
        Vec3 center = position();
        double range = 32;
        if (tamed) {                       // tamed ones circle the nearest player
            Player p = level().getNearestPlayer(this, 64);
            if (p != null) center = p.position();
            range = 20;
        }
        double x = center.x + (random.nextDouble() - 0.5) * range;
        double z = center.z + (random.nextDouble() - 0.5) * range;
        int ground = level().getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
        double y = ground + (tamed ? 3 + random.nextInt(4) : 4 + random.nextInt(8));
        wanderTarget = new Vec3(x, y, z);
        wanderTimer = 100;
    }

    private void flyToward(Vec3 target, double speed) {
        Vec3 d = target.subtract(position());
        double len = d.length();
        if (len < 1.0E-3) return;
        Vec3 wanted = d.scale(speed / len);
        setDeltaMovement(getDeltaMovement().scale(0.6).add(wanted.scale(0.4)));   // smooth, swoopy turns
        face(getDeltaMovement());
    }

    private void face(Vec3 v) {
        if (v.horizontalDistanceSqr() < 1.0E-6) return;
        float yaw = (float) (Mth.atan2(v.z, v.x) * (180.0 / Math.PI)) - 90.0f;
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
    }

    private void eat() {
        if (!(level() instanceof ServerLevel server)) return;
        server.sendParticles(ParticleTypes.POOF, prey.getX(), prey.getY() + 0.4, prey.getZ(),
                20, 0.4, 0.4, 0.4, 0.02);
        prey.discard();
        server.playSound(null, blockPosition(), SoundEvents.PLAYER_BURP, SoundSource.NEUTRAL, 1.5f, 0.7f);
        prey = null;
        if (tamed) {
            state = State.TAMED;          // tamed: keep cruising with its rider
        } else {
            state = State.LEAVING;        // wild: fly off north and despawn
            leaveTicks = 0;
        }
    }

    /** The Wingus steers itself even with a rider aboard. */
    @Override
    public LivingEntity getControllingPassenger() {
        return null;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return tamed && super.shouldBeSaved();   // only tamed ones (with Flavus) persist
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Tamed", tamed);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        tamed = tag.getBoolean("Tamed");
        if (tamed) state = State.TAMED;
    }
}
