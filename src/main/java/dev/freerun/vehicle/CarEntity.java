package dev.freerun.vehicle;

import dev.freerun.economy.EconomyRegistry;
import dev.freerun.economy.ModSounds;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A drivable sedan with simple but honest car physics, simulated by whoever is driving (like a
 * boat or horse, so it's smooth even on a server):
 *
 *  - throttle builds speed along an engine curve (quick off the line, tapering to ~70 km/h),
 *    with rolling resistance and aerodynamic drag; S brakes hard, then reverses
 *  - steering eases toward the input instead of snapping, and the turn rate follows a bicycle
 *    model (speed × tan(steer angle) / wheelbase), so it turns tighter slow and wider fast,
 *    and can't turn at all standing still
 *  - lateral grip bleeds off sideways slip; hold Space for the handbrake and the rear lets go
 *  - it steps up curbs and single blocks, crashes into walls, hurts whatever it runs into, and
 *    burns fuel (refill at a gas pump or with a gas can)
 *
 * Seats four. W/S throttle and brake, A/D steer, Space handbrake, R horn, sneak to get out.
 */
public class CarEntity extends LivingEntity {
    public static final float MAX_FUEL = 100.0f;
    private static final double WHEELBASE = 2.7;
    private static final double MAX_FORWARD = 1.0;      // blocks/tick (~72 km/h)
    private static final double MAX_REVERSE = 0.28;
    private static final double ENGINE = 0.032;
    private static final double BRAKE = 0.06;
    private static final double ROLLING = 0.004;
    private static final double DRAG = 0.025;
    private static final float MAX_STEER_DEG = 34.0f;
    private static final int[] PAINT = {0xB3202A, 0x1E4FA8, 0xEDEDED, 0x1C1C1E, 0x9EA3A8, 0x2F6B3A, 0xE3B417, 0xD9661F,
            0x6B1E2A, 0x2A8C8C};

    private static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_FUEL = SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.FLOAT);

    private double speed;
    private float steer;
    // client-side, for the model and engine sound
    private float steerO;
    private float wheelSpin, wheelSpinO;
    private double renderSpeed;
    private int hornCooldown;
    private int crashCooldown;
    // server side: the driver's client moves the car between ticks, so track movement ourselves
    private Vec3 lastServerPos;
    // a resident at the wheel
    private net.minecraft.core.BlockPos city;
    private final dev.freerun.city.Autopilot autopilot = new dev.freerun.city.Autopilot();
    private double lastServerSpeed;

    public CarEntity(EntityType<? extends CarEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.STEP_HEIGHT, 1.05)
                .add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_COLOR, PAINT[0]);
        builder.define(DATA_FUEL, 60.0f);
    }

    public int color() {
        return entityData.get(DATA_COLOR);
    }

    public void setColor(int rgb) {
        entityData.set(DATA_COLOR, rgb & 0xFFFFFF);
    }

    public void randomColor() {
        setColor(PAINT[random.nextInt(PAINT.length)]);
    }

    public float fuel() {
        return entityData.get(DATA_FUEL);
    }

    public void addFuel(float amount) {
        entityData.set(DATA_FUEL, Mth.clamp(fuel() + amount, 0.0f, MAX_FUEL));
    }

    public void setCity(net.minecraft.core.BlockPos city) {
        this.city = city == null ? null : city.immutable();
    }

    public net.minecraft.core.BlockPos city() {
        return city;
    }

    public boolean npcDriven() {
        return getFirstPassenger() instanceof dev.freerun.city.CitizenEntity;
    }

    /** Forward speed in blocks per tick on the side simulating the car. */
    public double speed() {
        return level().isClientSide ? renderSpeed : speed;
    }

    public Vec3 forwardVec() {
        return forward();
    }

    /** Hacked: the engine cuts out for a while. */
    public void stall(int ticks) {
        autopilot.stallTicks = ticks;
        autopilot.joyrideTicks = 0;
    }

    /** Hacked: floor it and ignore every light. */
    public void joyride(int ticks) {
        autopilot.joyrideTicks = ticks;
        autopilot.stallTicks = 0;
    }

    @Override
    protected void serverAiStep() {
        super.serverAiStep();
        if (npcDriven() && city != null) autopilot.step(this, city);
    }

    public float steer(float partialTick) {
        return Mth.lerp(partialTick, steerO, steer);
    }

    public float wheelSpin(float partialTick) {
        return Mth.lerp(partialTick, wheelSpinO, wheelSpin);
    }

    /** Forward speed in blocks per tick, as seen on this side (for the engine note). */
    public double renderSpeed() {
        return renderSpeed;
    }

    // ------------------------------------------------------------------ frame helpers

    private Vec3 forward() {
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
    }

    private Vec3 right() {
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.cos(yaw), 0.0, -Mth.sin(yaw));
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void tick() {
        steerO = steer;
        wheelSpinO = wheelSpin;
        super.tick();
        Vec3 moved = position().subtract(xo, yo, zo);
        double fwd = moved.dot(forward());
        renderSpeed = fwd;
        wheelSpin += (float) (fwd / 0.375);   // wheel radius 6px = 0.375 blocks
        if (!isControlledByLocalInstance() && level().isClientSide) {
            // Someone else is driving: ease the front wheels toward where the car is turning.
            float yawRate = Mth.wrapDegrees(getYRot() - yRotO);
            float guess = Math.abs(fwd) > 0.02 ? (float) Mth.clamp(-yawRate * WHEELBASE / (fwd * 40.0), -1.0, 1.0) : steer;
            steer += (guess - steer) * 0.3f;
        }
        if (level().isClientSide) return;
        if (hornCooldown > 0) hornCooldown--;
        if (crashCooldown > 0) crashCooldown--;

        if (lastServerPos == null) lastServerPos = position();
        moved = position().subtract(lastServerPos);
        lastServerPos = position();
        double horiz = Math.sqrt(moved.x * moved.x + moved.z * moved.z);
        if (horiz > 3.0) horiz = 0.0;   // teleported, not driven
        if (horiz > 0.01 && getControllingPassenger() != null) addFuel((float) (-horiz * 0.045));
        if (horiz > 3.0 && npcDriven()) autopilot.reset();
        if (horiz > 0.32) runOver(moved, horiz);
        // Went from speed to a dead stop in one tick: that was a wall.
        if (lastServerSpeed > 0.5 && horiz < lastServerSpeed * 0.3 && crashCooldown <= 0) {
            crashCooldown = 20;
            level().playSound(null, getX(), getY(), getZ(), ModSounds.CAR_CRASH, SoundSource.NEUTRAL,
                    (float) Math.min(1.6, lastServerSpeed * 1.8), 0.9f);
            if (level() instanceof ServerLevel server) {
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, getX(), getY() + 0.8, getZ(), 12, 1.0, 0.3, 1.0, 0.2);
            }
            if (lastServerSpeed > 0.65) {
                for (Entity p : getPassengers()) p.hurt(damageSources().flyIntoWall(), (float) ((lastServerSpeed - 0.6) * 12.0));
            }
        }
        lastServerSpeed = horiz;
    }

    /** Anything in the way at speed gets hurt and thrown. */
    private void runOver(Vec3 moved, double horiz) {
        AABB box = getBoundingBox().inflate(0.25).expandTowards(moved.x, 0.0, moved.z);
        Entity driver = getControllingPassenger();
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != this && e.isAlive() && !e.isPassenger() && !(e instanceof CarEntity)
                        && !(e instanceof net.minecraft.world.entity.decoration.ArmorStand))) {
            DamageSource src = driver instanceof Player p ? damageSources().playerAttack(p) : damageSources().mobAttack(this);
            if (e.hurt(src, (float) (horiz * 16.0))) {
                Vec3 push = moved.normalize().scale(horiz * 1.4);
                e.push(push.x, 0.35 + horiz * 0.3, push.z);
                e.hurtMarked = true;
                level().playSound(null, e.getX(), e.getY(), e.getZ(), ModSounds.CAR_CRASH, SoundSource.NEUTRAL, 0.8f, 1.3f);
            }
        }
    }

    @Override
    public void travel(Vec3 input) {
        if (!isControlledByLocalInstance()) {
            super.travel(input);
            return;
        }
        LivingEntity driver = getControllingPassenger();
        float throttle = 0.0f, steerIn = 0.0f;
        boolean handbrake = false;
        float steerLimit = 1.0f;
        if (driver instanceof Player p) {
            throttle = p.zza;
            steerIn = p.xxa;
            handbrake = p.jumping;
        } else if (npcDriven() && city != null) {
            throttle = autopilot.throttle;
            steerIn = autopilot.steer;
            steerLimit = 1.35f;
        }
        boolean grounded = onGround();
        if (fuel() <= 0.0f && throttle > 0.0f && speed >= -0.01) throttle = 0.0f;

        // Steering eases toward the input and recentres on its own.
        float target = Mth.clamp(steerIn, -steerLimit, steerLimit);
        steer += Mth.clamp(target - steer, -0.09f, 0.09f);

        double v = speed;
        if (grounded) {
            if (throttle > 0.0f) {
                if (v < -0.01) v = Math.min(0.0, v + BRAKE);
                else v += ENGINE * throttle * Math.max(0.0, 1.0 - (v / MAX_FORWARD) * (v / MAX_FORWARD));
            } else if (throttle < 0.0f) {
                if (v > 0.01) v = Math.max(0.0, v - BRAKE);
                else v -= ENGINE * 0.6 * -throttle * Math.max(0.0, 1.0 - (v / MAX_REVERSE) * (v / MAX_REVERSE));
            }
            double resist = ROLLING + DRAG * v * v;
            if (Math.abs(v) <= resist) v = 0.0;
            else v -= Math.signum(v) * resist;
            if (handbrake) v *= 0.97;
        }

        // Bicycle-model yaw rate: tighter at low speed, wider at high speed.
        double angle = Math.toRadians(MAX_STEER_DEG * steer) / (1.0 + Math.abs(v) * 1.1);
        double yawRate = Math.toDegrees(v * Math.tan(angle) / WHEELBASE);
        if (handbrake && Math.abs(v) > 0.25) yawRate *= 1.7;
        if (grounded) {
            float turn = (float) -yawRate;
            setYRot(getYRot() + turn);
            yBodyRot = getYRot();
            yHeadRot = getYRot();
            if (driver != null) {
                driver.setYRot(driver.getYRot() + turn);
                driver.setYHeadRot(driver.getYHeadRot() + turn);
            }
        }

        // Grip: keep most of the old sideways slide only while drifting or airborne.
        Vec3 f = forward();
        Vec3 r = right();
        Vec3 old = getDeltaMovement();
        double lateral = new Vec3(old.x, 0.0, old.z).dot(r);
        lateral *= !grounded ? 0.98 : handbrake ? 0.9 : 0.35;
        Vec3 h = f.scale(v).add(r.scale(lateral));
        double vy = grounded ? -0.05 : (old.y - 0.08) * 0.98;
        setDeltaMovement(h.x, vy, h.z);
        Vec3 before = position();
        move(MoverType.SELF, getDeltaMovement());

        Vec3 actual = position().subtract(before);
        if (horizontalCollision) v = actual.dot(f) * 0.5;   // the server plays the crash
        speed = v;
        steerTick();
    }

    private void steerTick() {
        if (level().isClientSide && isControlledByLocalInstance() && Math.abs(speed) > 0.05 && tickCount % 4 == 0
                && getControllingPassenger() instanceof Player p && p.jumping && Math.abs(steer) > 0.3) {
            // Tyre smoke while drifting.
            Vec3 rear = position().add(forward().scale(-1.5));
            level().addParticle(net.minecraft.core.particles.ParticleTypes.CLOUD, rear.x, getY() + 0.1, rear.z, 0, 0.02, 0);
        }
    }

    // ------------------------------------------------------------------ driving & riding

    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player player ? player : null;
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 travelVector) {
        return new Vec3(player.xxa, 0.0, player.zza);
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        return 0.3f;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < 4;
    }

    /** Seat {@code i}: driver front-left, passenger front-right, then the back seat. */
    private Vec3 seat(int i) {
        double side = (i % 2 == 0) ? -0.42 : 0.42;
        double fwd = i < 2 ? -0.05 : -1.05;
        return position().add(right().scale(side)).add(forward().scale(fwd)).add(0.0, 0.38, 0.0);
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction move) {
        if (!hasPassenger(passenger)) return;
        int i = getPassengers().indexOf(passenger);
        Vec3 s = seat(i);
        Vec3 att = passenger.getVehicleAttachmentPoint(this);
        move.accept(passenger, s.x, s.y - att.y, s.z);
        if (!(passenger instanceof Player)) {
            passenger.setYRot(getYRot());
            if (passenger instanceof LivingEntity living) living.setYBodyRot(getYRot());
        }
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        int i = Math.max(0, getPassengers().indexOf(passenger));
        double side = (i % 2 == 0) ? -1.9 : 1.9;
        Vec3 out = seat(i).add(right().scale(side - (i % 2 == 0 ? -0.42 : 0.42)));
        AABB box = passenger.getBoundingBox().move(out.x - passenger.getX(), getY() - passenger.getY(), out.z - passenger.getZ());
        if (level().noCollision(passenger, box)) return new Vec3(out.x, getY(), out.z);
        return super.getDismountLocationForPassenger(passenger);
    }

    @Override
    protected float tickHeadTurn(float yRot, float animStep) {
        yBodyRot = getYRot();
        yHeadRot = getYRot();
        return animStep;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(EconomyRegistry.GAS_CAN)) {
            if (!level().isClientSide) {
                addFuel(40.0f);
                if (!player.getAbilities().instabuild) player.setItemInHand(hand, new ItemStack(EconomyRegistry.EMPTY_GAS_CAN));
                playSound(SoundEvents.BUCKET_EMPTY, 1.0f, 0.7f);
                player.displayClientMessage(Component.literal("§aFuel: " + Math.round(fuel()) + "%"), true);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (!canAddPassenger(player)) return InteractionResult.PASS;
        if (!level().isClientSide && player.startRiding(this)) {
            if (getControllingPassenger() == player) {
                player.displayClientMessage(Component.literal("§eW/S §fgas & brake  §eA/D §fsteer  §eSpace §fhandbrake  §eR §fhorn"
                        + "  §7· Fuel " + Math.round(fuel()) + "%"), true);
                playSound(SoundEvents.IRON_DOOR_CLOSE, 0.6f, 1.6f);
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    public void honk() {
        if (hornCooldown > 0) return;
        hornCooldown = 10;
        level().playSound(null, getX(), getY() + 1, getZ(), ModSounds.CAR_HORN, SoundSource.NEUTRAL, 2.0f, 1.0f);
    }

    // ------------------------------------------------------------------ damage & misc

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(source, amount);
        if (level().isClientSide) return false;
        if (source.getEntity() instanceof Player player && source.getDirectEntity() == player
                && player.isShiftKeyDown() && getPassengers().isEmpty()) {
            ItemStack item = new ItemStack(VehicleRegistry.CAR_ITEM);
            if (!player.getAbilities().instabuild) spawnAtLocation(item);
            playSound(SoundEvents.IRON_DOOR_CLOSE, 0.8f, 0.8f);
            discard();
            return true;
        }
        if (source.getDirectEntity() instanceof dev.freerun.item.gun.BulletEntity && level() instanceof ServerLevel server) {
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.8, getZ(),
                    3, 0.6, 0.3, 0.6, 0.1);
        }
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return isAlive();
    }

    @Override
    public boolean isAffectedByPotions() {
        return false;
    }

    @Override
    protected MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }

    @Override
    public boolean shouldShowName() {
        return false;
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return NonNullList.withSize(4, ItemStack.EMPTY);
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.LEFT;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Color", color());
        tag.putFloat("Fuel", fuel());
        if (city != null) {
            tag.putInt("CityX", city.getX());
            tag.putInt("CityY", city.getY());
            tag.putInt("CityZ", city.getZ());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Color")) setColor(tag.getInt("Color"));
        if (tag.contains("Fuel")) entityData.set(DATA_FUEL, Mth.clamp(tag.getFloat("Fuel"), 0.0f, MAX_FUEL));
        if (tag.contains("CityX")) city = new net.minecraft.core.BlockPos(tag.getInt("CityX"), tag.getInt("CityY"), tag.getInt("CityZ"));
    }
}
