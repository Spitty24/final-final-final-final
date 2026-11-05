package dev.freerun.garage;

import dev.freerun.vehicle.CarEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * A four-post lift: four columns, a cross-beam at each end, and two runways you drive onto. It's
 * 3 blocks wide and 6 long; the runways are a solid deck that rises to 2.1 blocks so you can walk
 * under the car. Right-click a post to raise or lower it (the safety locks click on the way up);
 * it won't move with somebody sitting in the driver's seat. Sneak + right-click with an empty hand
 * to fold it back up into an item.
 */
public class LiftEntity extends Entity {
    public static final double LOW = 0.2, HIGH = 2.1, SPEED = 0.03;
    public static final double HALF_W = 1.5, HALF_L = 3.0, DECK = 0.18;

    private static final EntityDataAccessor<Float> DATA_HEIGHT = SynchedEntityData.defineId(LiftEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_DIR = SynchedEntityData.defineId(LiftEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_ALONG_X = SynchedEntityData.defineId(LiftEntity.class, EntityDataSerializers.BOOLEAN);

    private float heightO = (float) LOW;

    public LiftEntity(EntityType<? extends LiftEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(DATA_HEIGHT, (float) LOW);
        b.define(DATA_DIR, 0);
        b.define(DATA_ALONG_X, false);
    }

    public double height() {
        return entityData.get(DATA_HEIGHT);
    }

    public float height(float partial) {
        return Mth.lerp(partial, heightO, entityData.get(DATA_HEIGHT));
    }

    /** -1 lowering, 0 stopped, +1 raising. */
    public int dir() {
        return entityData.get(DATA_DIR);
    }

    /** The runways run along X (true) or Z (false). */
    public boolean alongX() {
        return entityData.get(DATA_ALONG_X);
    }

    public void setAlongX(boolean v) {
        entityData.set(DATA_ALONG_X, v);
        refreshDimensions();
        setBoundingBox(makeBoundingBox());
    }

    public void setHeight(double h) {
        entityData.set(DATA_HEIGHT, (float) Mth.clamp(h, LOW, HIGH));
        setBoundingBox(makeBoundingBox());
    }

    /** Deck top in world Y. */
    public double deckTop() {
        return getY() + height() + DECK;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_HEIGHT.equals(key) || DATA_ALONG_X.equals(key)) setBoundingBox(makeBoundingBox());
    }

    @Override
    protected AABB makeBoundingBox() {
        if (entityData == null) return super.makeBoundingBox();
        double hw = alongX() ? HALF_L : HALF_W, hl = alongX() ? HALF_W : HALF_L;
        double y = getY() + height();
        return new AABB(getX() - hw, y, getZ() - hl, getX() + hw, y + DECK, getZ() + hl);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.fixed((float) (HALF_L * 2), (float) (HIGH + DECK));
    }

    @Override
    public void tick() {
        heightO = (float) height();
        super.tick();
        setBoundingBox(makeBoundingBox());
        if (level().isClientSide) return;
        int d = dir();
        if (d == 0) return;
        double h = height(), nh = Mth.clamp(h + d * SPEED, LOW, HIGH);
        double dh = nh - h;
        if (dh != 0) {
            // everything standing on the deck goes along
            AABB deck = getBoundingBox();
            AABB above = new AABB(deck.minX, deck.maxY - 0.05, deck.minZ, deck.maxX, deck.maxY + 0.6, deck.maxZ);
            List<Entity> riders = level().getEntities(this, above, e -> !(e instanceof LiftEntity) && !e.isPassenger() && !e.noPhysics);
            setHeight(nh);
            for (Entity e : riders) {
                if (e instanceof CarEntity car) car.suspend();
                double ny = Math.max(e.getY() + dh, deckTop());
                if (dh < 0) ny = e.getY() + dh;   // coming down, the deck lowers away and they ride it down
                if (e instanceof ServerPlayer sp) sp.connection.teleport(sp.getX(), ny, sp.getZ(), sp.getYRot(), sp.getXRot());
                else e.setPos(e.getX(), ny, e.getZ());
                e.setDeltaMovement(e.getDeltaMovement().multiply(1, 0, 1));
                e.resetFallDistance();
                for (Entity p : e.getPassengers()) p.resetFallDistance();
            }
            // the safety locks click every half block on the way up
            if (d > 0 && Math.floor(h * 2) != Math.floor(nh * 2)) {
                level().playSound(null, getX(), getY() + nh, getZ(), SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.7f, 1.6f);
            }
            if (tickCount % 10 == 0) level().playSound(null, getX(), getY() + 1, getZ(), SoundEvents.MINECART_INSIDE, SoundSource.BLOCKS, 0.15f, 0.6f);
        }
        if (nh <= LOW || nh >= HIGH) {
            entityData.set(DATA_DIR, 0);
            level().playSound(null, getX(), getY() + nh, getZ(), SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.6f, 0.7f);
        }
    }

    /** The car sitting on the runways, if any. */
    public CarEntity car() {
        AABB deck = getBoundingBox();
        return level().getEntitiesOfClass(CarEntity.class, new AABB(deck.minX, deck.maxY - 0.3, deck.minZ, deck.maxX, deck.maxY + 1.0, deck.maxZ))
                .stream().findFirst().orElse(null);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (level().isClientSide) return InteractionResult.SUCCESS;
        ItemStack held = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && held.isEmpty()) {
            if (car() != null) {
                player.displayClientMessage(Component.literal("§cDrive the car off before folding the lift up."), true);
                return InteractionResult.CONSUME;
            }
            if (!player.getAbilities().instabuild && !player.getInventory().add(new ItemStack(GarageRegistry.LIFT_ITEM))) {
                spawnAtLocation(new ItemStack(GarageRegistry.LIFT_ITEM));
            }
            playSound(SoundEvents.IRON_DOOR_CLOSE, 0.8f, 0.7f);
            discard();
            return InteractionResult.CONSUME;
        }
        CarEntity car = car();
        if (car != null && car.getControllingPassenger() != null) {
            player.displayClientMessage(Component.literal("§cNot with somebody in the driver's seat. Everyone out first."), true);
            return InteractionResult.CONSUME;
        }
        int d = dir() != 0 ? 0 : height() > (LOW + HIGH) / 2 ? -1 : 1;
        if (dir() != 0) d = 0;
        entityData.set(DATA_DIR, d);
        if (d != 0) {
            level().playSound(null, getX(), getY() + 1, getZ(), SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.6f, 0.6f);
            player.displayClientMessage(Component.literal(d > 0 ? "§eRaising the lift..." : "§eLowering the lift... §7stand clear"), true);
        } else {
            player.displayClientMessage(Component.literal("§7Lift stopped."), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(0.5).expandTowards(0, -HIGH - 0.5, 0).expandTowards(0, 1.2, 0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(DATA_ALONG_X, tag.getBoolean("AlongX"));
        setHeight(tag.contains("Height") ? tag.getDouble("Height") : LOW);
        entityData.set(DATA_DIR, 0);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("AlongX", alongX());
        tag.putDouble("Height", height());
    }
}
