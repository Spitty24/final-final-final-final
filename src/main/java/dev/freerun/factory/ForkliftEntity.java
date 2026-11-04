package dev.freerun.factory;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * A sit-down counterbalance forklift.
 *
 * Players: right-click to climb on. W/S drive, A/D steer, the Forks Up / Forks Down keys (R / V by
 * default) raise and lower the forks, sneak to climb off. Drive the forks under a steel part (or a
 * stack of them), raise them and the load comes with you; lower it onto the floor or another part
 * to set it down. Sneak-punch an empty forklift to pick it up as an item.
 *
 * Factory forklifts carry a {@link ForkliftDriverEntity} who drives a fixed route on his own,
 * dropping off and picking up a pallet at each stop and honking at anyone in his way.
 *
 * Driving is client-side for the player at the wheel, like a horse; everything to do with the
 * forks and the load runs on the server.
 */
public class ForkliftEntity extends LivingEntity {
    public static final double MAX_FORK = 1.6;
    private static final double FORK_SPEED = 0.05;
    // Where the forks are, relative to the forklift (blocks): forward from the mast, half-width.
    private static final double FORK_NEAR = 0.75;
    private static final double FORK_FAR = 2.1;
    private static final double FORK_HALF_WIDTH = 0.55;
    private static final double FORK_THICKNESS = 1.0 / 16.0;

    private static final EntityDataAccessor<Float> DATA_FORK =
            SynchedEntityData.defineId(ForkliftEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_PALLET =
            SynchedEntityData.defineId(ForkliftEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_LOADED =
            SynchedEntityData.defineId(ForkliftEntity.class, EntityDataSerializers.BOOLEAN);

    /** A part riding on the forks, kept at a fixed offset in the forklift's own frame. */
    private record Carried(SteelPartEntity part, double side, double forward, double up) {}

    private final List<Carried> carried = new ArrayList<>();
    private int forkInput;
    private float clientForkO;
    private float clientFork;

    // NPC route (world coordinates), only used when a driver NPC is aboard.
    private final List<Vec3> route = new ArrayList<>();
    private int routeIndex;
    private int pauseTicks;
    private int stuckTicks;
    private int honkCooldown;
    private Vec3 lastPos = Vec3.ZERO;

    public ForkliftEntity(EntityType<? extends ForkliftEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.11)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.STEP_HEIGHT, 0.6);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FORK, 0.0f);
        builder.define(DATA_PALLET, false);
        builder.define(DATA_LOADED, false);
    }

    public float forkHeight() {
        return entityData.get(DATA_FORK);
    }

    /** Fork height smoothed between ticks, for rendering. */
    public float forkHeight(float partialTick) {
        return Mth.lerp(partialTick, clientForkO, clientFork);
    }

    public boolean hasPallet() {
        return entityData.get(DATA_PALLET);
    }

    public void setPallet(boolean pallet) {
        entityData.set(DATA_PALLET, pallet);
    }

    /** Whether the (decorative) pallet on an NPC forklift has a stack of steel on it. */
    public boolean isLoaded() {
        return entityData.get(DATA_LOADED);
    }

    public void setLoaded(boolean loaded) {
        entityData.set(DATA_LOADED, loaded);
    }

    public void setRoute(List<Vec3> points) {
        route.clear();
        route.addAll(points);
        routeIndex = 0;
    }

    public boolean hasRoute() {
        return !route.isEmpty();
    }

    /** From the Forks Up / Forks Down keys of the player driving: -1, 0 or +1. */
    public void setForkInput(Player player, int dir) {
        if (getControllingPassenger() == player) forkInput = Mth.clamp(dir, -1, 1);
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

    private Vec3 toWorld(double side, double fwd, double up) {
        return position().add(right().scale(side)).add(forward().scale(fwd)).add(0.0, up, 0.0);
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            clientForkO = clientFork;
            clientFork = forkHeight();
            return;
        }
        float fork = forkHeight();
        boolean playerDriving = getControllingPassenger() instanceof Player;
        int dir = playerDriving ? forkInput : npcForkDir;
        if (!playerDriving && !(getFirstPassenger() instanceof ForkliftDriverEntity)) dir = 0;
        if (dir != 0) {
            double speed = FORK_SPEED;
            if (playerDriving && getControllingPassenger() instanceof Player driver
                    && (dev.freerun.career.Careers.has(driver, "forklift_certified")
                    || dev.freerun.career.Careers.has(driver, "heavy_equipment"))) {
                speed *= 2.0;
            }
            float next = (float) Mth.clamp(fork + dir * speed, 0.0, MAX_FORK);
            if (next != fork) {
                if (dir > 0 && playerDriving && carried.isEmpty()) pickUpLoad(next);
                entityData.set(DATA_FORK, next);
                if (tickCount % 6 == 0) {
                    level().playSound(null, getX(), getY() + 1, getZ(), SoundEvents.CHAIN_STEP, SoundSource.NEUTRAL, 0.5f, 0.7f);
                }
                fork = next;
            }
        }
        carryLoad(fork, dir);
    }

    /**
     * Forks rising with nothing on them: if they've come up under a part, it and anything stacked
     * on it come along, keeping their positions relative to the forks.
     */
    private void pickUpLoad(float fork) {
        Vec3 f = forward();
        Vec3 r = right();
        AABB search = getBoundingBox().inflate(FORK_FAR + 0.5, 3.0 + fork, FORK_FAR + 0.5);
        List<Carried> found = new ArrayList<>();
        boolean touching = false;
        for (SteelPartEntity part : level().getEntitiesOfClass(SteelPartEntity.class, search, p -> !p.isHeld())) {
            Vec3 d = part.position().subtract(position());
            double fwd = d.dot(f);
            double side = d.dot(r);
            double up = part.getY() - getY();
            if (fwd < FORK_NEAR - 0.3 || fwd > FORK_FAR + 0.2 || Math.abs(side) > FORK_HALF_WIDTH + 0.4) continue;
            if (up < fork - 0.15 || up > fork + 2.6) continue;
            if (up <= fork + 0.25) touching = true;
            found.add(new Carried(part, side, fwd, up - fork));
        }
        if (!touching) return;
        carried.addAll(found);
        if (!carried.isEmpty()) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_PLACE, SoundSource.NEUTRAL, 0.3f, 1.5f);
            if (getControllingPassenger() instanceof net.minecraft.server.level.ServerPlayer sp) {
                dev.freerun.career.Careers.addXp(sp, 4, "forklift work");
            }
        }
    }

    private void carryLoad(float fork, int dir) {
        if (carried.isEmpty()) return;
        if (fork <= 0.0f) {   // forks on the floor: set everything down
            carried.clear();
            return;
        }
        carried.removeIf(c -> {
            SteelPartEntity part = c.part();
            if (!part.isAlive()) return true;
            Vec3 target = toWorld(c.side(), c.forward(), fork + c.up());
            Vec3 delta = target.subtract(part.position());
            if (delta.lengthSqr() > 2.5 * 2.5) return true;   // knocked off the forks
            // Lowering onto something: let go once it's resting.
            if (dir < 0 && part.onGround() && delta.y < -0.05) return true;
            Vec3 v = delta.scale(0.6);
            if (v.length() > 1.2) v = v.normalize().scale(1.2);
            part.setDeltaMovement(v);
            part.hurtMarked = true;
            part.hold();
            return false;
        });
    }

    /** The forklift doesn't bump into whatever is sitting on its own raised forks. */
    @Override
    public boolean canCollideWith(Entity other) {
        if (other instanceof SteelPartEntity && forkHeight() > 0.15
                && other.getY() >= getY() + forkHeight() - 0.15) {
            return false;
        }
        return super.canCollideWith(other);
    }

    // ------------------------------------------------------------------ player driving

    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player player ? player : null;
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 travelVector) {
        float forward = player.zza;
        if (forward < 0.0f) forward *= 0.5f;     // reverse is slower
        return new Vec3(0.0, 0.0, forward);
    }

    @Override
    protected void tickRidden(Player player, Vec3 travelVector) {
        super.tickRidden(player, travelVector);
        if (!isControlledByLocalInstance()) return;
        // A/D turn the forklift, and the driver's view turns with it.
        float turn = -player.xxa * (Math.abs(travelVector.z) > 0.01 ? 4.0f : 2.5f);
        if (travelVector.z < 0) turn = -turn;      // steer like a car when backing up
        setYRot(getYRot() + turn);
        yBodyRot = getYRot();
        yHeadRot = getYRot();
        player.setYRot(player.getYRot() + turn);
        player.setYHeadRot(player.getYHeadRot() + turn);
        if (travelVector.z < -0.01 && tickCount % 10 == 0) {
            playSound(SoundEvents.NOTE_BLOCK_BIT.value(), 0.6f, 1.7f);   // reverse beeper
        }
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        float base = (float) getAttributeValue(Attributes.MOVEMENT_SPEED);
        return dev.freerun.career.Careers.has(player, "forklift_certified") ? base * 1.35f : base;
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        forkInput = 0;   // don't keep lifting after the driver hops off mid-press
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction move) {
        super.positionRider(passenger, move);
        if (!(passenger instanceof Player)) {
            passenger.setYRot(getYRot());
            if (passenger instanceof LivingEntity living) living.setYBodyRot(getYRot());
        }
    }

    @Override
    protected float tickHeadTurn(float yRot, float animStep) {
        yBodyRot = getYRot();   // the chassis always points where it's facing, even reversing
        yHeadRot = getYRot();
        return animStep;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive() || player.getItemInHand(hand).getItem() instanceof GravityGunItem) {
            return InteractionResult.PASS;
        }
        Entity first = getFirstPassenger();
        if (first instanceof ForkliftDriverEntity driver) {
            if (!level().isClientSide) driver.chatter(player);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (first != null) return InteractionResult.PASS;
        if (!level().isClientSide && player.startRiding(this)) {
            player.displayClientMessage(Component.literal(
                    "§eForklift: §fW/S drive · A/D steer · R/V forks up/down · sneak to get off"), true);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    // ------------------------------------------------------------------ NPC driving

    private int npcForkDir;

    @Override
    protected void serverAiStep() {
        super.serverAiStep();
        npcForkDir = 0;
        if (!(getFirstPassenger() instanceof ForkliftDriverEntity driver) || route.isEmpty()) {
            zza = 0.0f;
            return;
        }
        if (honkCooldown > 0) honkCooldown--;
        if (pauseTicks > 0) {
            // At a stop: forks down to the floor, unload (or load) the steel, back up to carry height.
            pauseTicks--;
            zza = 0.0f;
            if (pauseTicks > 40) {
                if (forkHeight() > 0.0f) npcForkDir = -1;
            } else if (pauseTicks == 40) {
                setLoaded(!isLoaded());
                playSound(SoundEvents.ANVIL_PLACE, 0.4f, isLoaded() ? 1.3f : 1.6f);
            } else if (forkHeight() < 0.3f) {
                npcForkDir = 1;
            }
            return;
        }
        if (forkHeight() < 0.3f) npcForkDir = 1;
        else if (forkHeight() > 0.35f) npcForkDir = -1;

        Vec3 target = route.get(routeIndex);
        double dx = target.x - getX();
        double dz = target.z - getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 0.5) {
            routeIndex = (routeIndex + 1) % route.size();
            pauseTicks = 70;
            stuckTicks = 0;
            return;
        }
        float desired = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0f;
        float diff = Mth.wrapDegrees(desired - getYRot());
        float turn = Mth.clamp(diff, -5.0f, 5.0f);
        setYRot(getYRot() + turn);
        yBodyRot = getYRot();
        yHeadRot = getYRot();

        // Somebody in the way: stop and lean on the horn.
        if (blockedAhead()) {
            zza = 0.0f;
            if (honkCooldown <= 0) {
                honkCooldown = 40;
                playSound(SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(), 1.0f, 1.4f);
                playSound(SoundEvents.NOTE_BLOCK_BIT.value(), 1.0f, 0.8f);
                driver.yellAtObstacle();
            }
            return;
        }
        setSpeed((float) getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.75f);
        zza = Math.abs(diff) > 40.0f ? 0.0f : (float) Math.min(1.0, 0.35 + dist / 3.0);

        // Wedged on something for 5 seconds: skip to the next stop.
        if (position().distanceToSqr(lastPos) < 1.0E-4 && zza > 0) {
            if (++stuckTicks > 100) {
                routeIndex = (routeIndex + 1) % route.size();
                stuckTicks = 0;
            }
        } else {
            stuckTicks = 0;
        }
        lastPos = position();
    }

    private boolean blockedAhead() {
        Vec3 ahead = toWorld(0.0, 1.9, 0.0);
        AABB box = new AABB(ahead.x - 0.8, getY(), ahead.z - 0.8, ahead.x + 0.8, getY() + 2.0, ahead.z + 0.8);
        return !level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != this && e.isAlive() && !(e instanceof ForkliftEntity) && !e.isPassenger()
                        && !(e instanceof net.minecraft.world.entity.decoration.ArmorStand)).isEmpty();
    }

    // ------------------------------------------------------------------ damage & misc

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(source, amount);
        if (level().isClientSide) return false;
        if (source.getEntity() instanceof Player player && source.getDirectEntity() == player
                && player.isShiftKeyDown() && getPassengers().stream().noneMatch(p -> p instanceof Player)) {
            if (!player.getAbilities().instabuild) spawnAtLocation(new ItemStack(FactoryRegistry.FORKLIFT_ITEM));
            playSound(SoundEvents.ANVIL_PLACE, 0.6f, 1.2f);
            discard();
            return true;
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
        return HumanoidArm.RIGHT;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Fork", forkHeight());
        tag.putBoolean("Pallet", hasPallet());
        tag.putBoolean("Loaded", isLoaded());
        ListTag list = new ListTag();
        for (Vec3 p : route) {
            CompoundTag t = new CompoundTag();
            t.putDouble("x", p.x);
            t.putDouble("z", p.z);
            list.add(t);
        }
        tag.put("Route", list);
        tag.putInt("RouteIndex", routeIndex);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_FORK, (float) Mth.clamp(tag.getFloat("Fork"), 0.0, MAX_FORK));
        setPallet(tag.getBoolean("Pallet"));
        setLoaded(tag.getBoolean("Loaded"));
        route.clear();
        ListTag list = tag.getList("Route", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            route.add(new Vec3(t.getDouble("x"), 0.0, t.getDouble("z")));
        }
        routeIndex = route.isEmpty() ? 0 : Math.floorMod(tag.getInt("RouteIndex"), route.size());
    }
}
