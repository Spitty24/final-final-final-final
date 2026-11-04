package dev.freerun.factory;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An overhead bridge crane. The entity is the hoist trolley; the bridge beam it hangs from is
 * drawn spanning the whole bay and travels with it along the runway rails.
 *
 * Operated with a {@link CranePendantItem}: hold right-click and the hook drives to wherever you
 * point (a red laser dot shows the spot under the hook); sneak + right-click lowers the hook to
 * pick up the steel part beneath it, or to set down what it's carrying.
 */
public class CraneEntity extends Entity {
    private static final double SPEED = 0.2;
    private static final double HOIST_SPEED = 0.14;
    /** Cable length while travelling. */
    public static final double CARRY_DROP = 0.8;
    /** From the trolley's origin down to where the cable leaves it. */
    public static final double TROLLEY_DEPTH = 6.0 / 16.0;
    /** Height of the hook block below the cable. */
    public static final double HOOK_LEN = 11.0 / 16.0;

    private static final EntityDataAccessor<Float> DATA_DROP =
            SynchedEntityData.defineId(CraneEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_BEAM_MIN =
            SynchedEntityData.defineId(CraneEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_BEAM_MAX =
            SynchedEntityData.defineId(CraneEntity.class, EntityDataSerializers.FLOAT);

    private enum State { IDLE, LOWER_GRAB, LOWER_RELEASE, RAISE }

    /** Which crane each player's pendant is linked to (server side). */
    private static final Map<UUID, CraneEntity> LINKS = new ConcurrentHashMap<>();

    private double minX, maxX, minZ, maxZ;
    private boolean boundsSet;
    private State state = State.IDLE;
    private SteelPartEntity load;
    private double targetX, targetZ;
    private int driveTicks;
    private UUID operator;
    private float clientDropO = (float) CARRY_DROP;
    private float clientDrop = (float) CARRY_DROP;

    public CraneEntity(EntityType<? extends CraneEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_DROP, (float) CARRY_DROP);
        builder.define(DATA_BEAM_MIN, 0.0f);
        builder.define(DATA_BEAM_MAX, 0.0f);
    }

    /** Hook travel limits and the ends of the bridge beam, in world coordinates. */
    public void setBounds(double minX, double maxX, double minZ, double maxZ, double beamMin, double beamMax) {
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
        this.boundsSet = true;
        entityData.set(DATA_BEAM_MIN, (float) beamMin);
        entityData.set(DATA_BEAM_MAX, (float) beamMax);
    }

    public float beamMin() {
        return entityData.get(DATA_BEAM_MIN);
    }

    public float beamMax() {
        return entityData.get(DATA_BEAM_MAX);
    }

    public float drop(float partialTick) {
        return Mth.lerp(partialTick, clientDropO, clientDrop);
    }

    private double drop() {
        return entityData.get(DATA_DROP);
    }

    private void setDrop(double d) {
        entityData.set(DATA_DROP, (float) d);
    }

    /** The bottom of the hook. */
    public Vec3 hookTip() {
        return new Vec3(getX(), getY() - TROLLEY_DEPTH - drop() - HOOK_LEN, getZ());
    }

    // ------------------------------------------------------------------ pendant controls

    /** The crane this player's pendant controls: the one they used last, or the nearest. */
    public static CraneEntity linkedTo(Player player) {
        CraneEntity c = LINKS.get(player.getUUID());
        if (c != null && c.isAlive() && c.level() == player.level() && c.distanceToSqr(player) < 96 * 96) return c;
        List<CraneEntity> near = player.level().getEntitiesOfClass(CraneEntity.class,
                player.getBoundingBox().inflate(64.0, 32.0, 64.0));
        c = near.stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(player))).orElse(null);
        if (c != null) {
            LINKS.put(player.getUUID(), c);
            player.displayClientMessage(Component.literal(
                    "§eLinked to the overhead crane. §7Hold right-click to drive · sneak + right-click to hook"), true);
        }
        return c;
    }

    public static void forget(UUID player) {
        LINKS.remove(player);
    }

    /** Called every tick the pendant's button is held: drive toward the spot the player aims at. */
    public void drive(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0f).scale(96.0));
        HitResult hit = level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 aim = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
        targetX = Mth.clamp(aim.x, minX, maxX);
        targetZ = Mth.clamp(aim.z, minZ, maxZ);
        driveTicks = 3;
        operator = player.getUUID();
    }

    /** Sneak + right-click: hook down to grab, or down to set the load on the floor. */
    public void hookAction(Player player) {
        operator = player.getUUID();
        if (state != State.IDLE) {
            player.displayClientMessage(Component.literal("§7The hook is still moving."), true);
            return;
        }
        state = load != null ? State.LOWER_RELEASE : State.LOWER_GRAB;
        player.displayClientMessage(Component.literal(load != null ? "§eSetting the load down..." : "§eLowering the hook..."), true);
        playSound(SoundEvents.CHAIN_STEP, 1.0f, 0.6f);
    }

    private void tell(String text) {
        if (operator == null) return;
        Player p = level().getPlayerByUUID(operator);
        if (p != null && p.distanceToSqr(this) < 96 * 96) p.displayClientMessage(Component.literal(text), true);
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            clientDropO = clientDrop;
            clientDrop = (float) drop();
            return;
        }
        if (!boundsSet) {   // summoned by hand: give it a 33x33 bay around where it appeared
            setBounds(getX() - 16, getX() + 16, getZ() - 16, getZ() + 16, getX() - 17, getX() + 17);
        }

        if (driveTicks > 0) {
            driveTicks--;
            double dx = targetX - getX();
            double dz = targetZ - getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > 0.02) {
                double step = Math.min(SPEED, dist);
                setPos(getX() + dx / dist * step, getY(), getZ() + dz / dist * step);
                if (tickCount % 20 == 0) {
                    level().playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.4f, 0.5f);
                }
            }
        }

        switch (state) {
            case LOWER_GRAB -> lowerToGrab();
            case LOWER_RELEASE -> lowerToRelease();
            case RAISE -> {
                setDrop(Math.max(CARRY_DROP, drop() - HOIST_SPEED));
                if (drop() <= CARRY_DROP) state = State.IDLE;
                if (tickCount % 6 == 0) playSound(SoundEvents.CHAIN_STEP, 0.6f, 0.8f);
            }
            default -> { }
        }

        carry();
        if ((driveTicks > 0 || state != State.IDLE) && tickCount % 3 == 0) laser();
    }

    /** Deepest the hook can go right now: stop just above the floor or whatever is below. */
    private double maxDrop() {
        Vec3 top = new Vec3(getX(), getY() - TROLLEY_DEPTH - 0.05, getZ());
        HitResult hit = level().clip(new ClipContext(top, top.add(0.0, -48.0, 0.0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        double floor = hit.getType() == HitResult.Type.MISS ? top.y - 48.0 : hit.getLocation().y;
        return Math.max(CARRY_DROP, (getY() - TROLLEY_DEPTH - HOOK_LEN) - floor - 0.02);
    }

    private void lowerToGrab() {
        setDrop(Math.min(maxDrop(), drop() + HOIST_SPEED));
        if (tickCount % 6 == 0) playSound(SoundEvents.CHAIN_STEP, 0.6f, 0.7f);
        Vec3 tip = hookTip();
        AABB around = new AABB(tip.x - 1.5, tip.y - 0.5, tip.z - 1.5, tip.x + 1.5, tip.y + 0.3, tip.z + 1.5);
        for (SteelPartEntity part : level().getEntitiesOfClass(SteelPartEntity.class, around, p -> !p.isHeld())) {
            AABB bb = part.getBoundingBox();
            boolean under = tip.x >= bb.minX - 0.3 && tip.x <= bb.maxX + 0.3 && tip.z >= bb.minZ - 0.3 && tip.z <= bb.maxZ + 0.3;
            if (under && tip.y <= bb.maxY + 0.15 && tip.y >= bb.maxY - 0.4) {
                load = part;
                state = State.RAISE;
                playSound(SoundEvents.CHAIN_PLACE, 1.0f, 0.8f);
                playSound(SoundEvents.ANVIL_PLACE, 0.3f, 1.6f);
                tell("§aHooked a " + (part.partCount() > 1 ? "welded assembly" : part.getParts().get(0).type().displayName()) + ".");
                return;
            }
        }
        if (drop() >= maxDrop() - 1.0E-3) {
            state = State.RAISE;
            tell("§7Nothing under the hook to pick up. Steer it right over a steel part.");
        }
    }

    private void lowerToRelease() {
        if (load == null || !load.isAlive()) {
            load = null;
            state = State.RAISE;
            return;
        }
        boolean touching = !level().noCollision(load, load.getBoundingBox().move(0.0, -0.12, 0.0));
        if (touching || drop() >= maxDrop() + 0.5) {
            load = null;
            state = State.RAISE;
            playSound(SoundEvents.CHAIN_BREAK, 0.8f, 1.0f);
            tell("§aLoad set down.");
            return;
        }
        setDrop(drop() + HOIST_SPEED * 0.75);
        if (tickCount % 6 == 0) playSound(SoundEvents.CHAIN_STEP, 0.6f, 0.7f);
    }

    /** Keeps the load hanging right under the hook. */
    private void carry() {
        if (load == null) return;
        if (!load.isAlive()) {
            load = null;
            return;
        }
        Vec3 tip = hookTip();
        Vec3 target = new Vec3(tip.x, tip.y - load.getBbHeight(), tip.z);
        Vec3 delta = target.subtract(load.position());
        if (delta.lengthSqr() > 3.5 * 3.5) {
            load = null;
            tell("§cThe load snagged and slipped off the hook!");
            return;
        }
        Vec3 v = delta.scale(0.5);
        if (v.length() > 1.2) v = v.normalize().scale(1.2);
        load.setDeltaMovement(v);
        load.hurtMarked = true;
        load.hold();
    }

    /** Red positioning laser: a dot on the floor straight under the hook. */
    private void laser() {
        if (!(level() instanceof ServerLevel server)) return;
        Vec3 tip = hookTip();
        HitResult hit = level().clip(new ClipContext(tip, tip.add(0.0, -48.0, 0.0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() == HitResult.Type.MISS) return;
        Vec3 p = hit.getLocation();
        server.sendParticles(new DustParticleOptions(new Vector3f(1.0f, 0.1f, 0.1f), 1.2f),
                p.x, p.y + 0.05, p.z, 2, 0.04, 0.0, 0.04, 0.0);
    }

    // ------------------------------------------------------------------ misc

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        // The bridge spans the whole bay, so cull against all of it, not just the trolley.
        return new AABB(beamMin(), getY() - 8.0, getZ() - 1.0, beamMax(), getY() + 1.0, getZ() + 1.0);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 160.0 * 160.0;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("MinX")) {
            setBounds(tag.getDouble("MinX"), tag.getDouble("MaxX"), tag.getDouble("MinZ"), tag.getDouble("MaxZ"),
                    tag.getDouble("BeamMin"), tag.getDouble("BeamMax"));
        }
        setDrop(CARRY_DROP);   // any load was dropped when the world closed
        state = State.IDLE;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (!boundsSet) return;
        tag.putDouble("MinX", minX);
        tag.putDouble("MaxX", maxX);
        tag.putDouble("MinZ", minZ);
        tag.putDouble("MaxZ", maxZ);
        tag.putDouble("BeamMin", beamMin());
        tag.putDouble("BeamMax", beamMax());
    }
}
