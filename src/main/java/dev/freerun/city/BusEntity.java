package dev.freerun.city;

import dev.freerun.vehicle.CarEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * A city bus. It drives its route's loop (see {@link BusRoutes}) on rails, so to speak: it knows
 * where it is by distance along the loop, slows for the corners, stops for red lights and stop
 * signs, waits behind traffic, and pulls in at a stop whenever someone is waiting there or a
 * passenger wants off. Townspeople ride it across town; players pay $2 and sit down.
 */
public class BusEntity extends Entity {
    public static final double CRUISE = 0.36;
    public static final int SEATS = 16;
    private static final EntityDataAccessor<Integer> ROUTE = SynchedEntityData.defineId(BusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DIST = SynchedEntityData.defineId(BusEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SPEED = SynchedEntityData.defineId(BusEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DOORS = SynchedEntityData.defineId(BusEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> BRAKING = SynchedEntityData.defineId(BusEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<BlockPos> CITY = SynchedEntityData.defineId(BusEntity.class, EntityDataSerializers.BLOCK_POS);

    private double s, speed;
    private double clientS = Double.NaN;
    private int dwell;
    private int serving = -1, lastServed = -1;
    private int signWait, passedSign = -1;
    private int blocked, ghost;
    public boolean ambient = true;

    public BusEntity(EntityType<? extends BusEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public static BusEntity create(ServerLevel level, BlockPos city, BusRoutes.Route r, double s) {
        BusEntity b = new BusEntity(CityRegistry.BUS, level);
        b.entityData.set(CITY, city);
        b.entityData.set(ROUTE, r.id);
        b.s = r.wrap(s);
        b.place();
        return b;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(ROUTE, 0);
        b.define(DIST, 0f);
        b.define(SPEED, 0f);
        b.define(DOORS, false);
        b.define(BRAKING, false);
        b.define(CITY, BlockPos.ZERO);
    }

    public BusRoutes.Route route() {
        return BusRoutes.route(entityData.get(ROUTE));
    }

    public BlockPos city() {
        return entityData.get(CITY);
    }

    /** Distance along the loop (on the client, the smoothed one). */
    public double distance() {
        return level().isClientSide && !Double.isNaN(clientS) ? clientS : s;
    }

    public boolean doorsOpen() {
        return entityData.get(DOORS);
    }

    public boolean braking() {
        return entityData.get(BRAKING);
    }

    public float busSpeed() {
        return entityData.get(SPEED);
    }

    public BusRoutes.Stop nextStop() {
        return route().nextStop(distance() + (doorsOpen() ? 1.0 : -0.5));
    }

    private void place() {
        BusRoutes.Route r = route();
        double d = distance();
        Vec3 p = r.at(d);
        BlockPos c = city();
        setPos(c.getX() + p.x, c.getY() + 1, c.getZ() + p.z);
        setYRot(r.yawAt(d));
    }

    public Vec3 forward() {
        float y = getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(y), 0, Mth.cos(y));
    }

    /** The curb side. */
    public Vec3 right() {
        float y = getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.cos(y), 0, -Mth.sin(y));
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            BusRoutes.Route r = route();
            float synced = entityData.get(DIST);
            if (Double.isNaN(clientS)) clientS = synced;
            clientS += entityData.get(SPEED);
            double diff = r.wrap(synced - clientS + r.length / 2) - r.length / 2;
            clientS = r.wrap(clientS + diff * 0.25);
            place();
            return;
        }
        if (city().equals(BlockPos.ZERO)) {
            discard();
            return;
        }
        drive((ServerLevel) level());
        place();
        entityData.set(DIST, (float) s);
        entityData.set(SPEED, (float) speed);
    }

    private void drive(ServerLevel level) {
        BusRoutes.Route r = route();
        if (dwell > 0) {
            speed = 0;
            dwell--;
            entityData.set(DOORS, true);
            entityData.set(BRAKING, true);
            BusRoutes.Stop st = r.stops.get(serving);
            if (dwell % 10 == 0) dwell += board(level, st) * 12;
            if (dwell == 0) {
                entityData.set(DOORS, false);
                lastServed = serving;
                serving = -1;
                level.playSound(null, getX(), getY() + 1, getZ(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.NEUTRAL, 0.6f, 1.2f);
                level.playSound(null, getX(), getY() + 1, getZ(), dev.freerun.economy.ModSounds.BUS_ENGINE, SoundSource.NEUTRAL, 0.9f, 1.0f);
            }
            return;
        }
        double target = CRUISE;
        // corners
        for (double[] c : r.corners) {
            double d = r.ahead(s, c[0]), in = r.ahead(c[0], s);
            if (d < 9 || in < c[1] - c[0]) target = Math.min(target, 0.16);
        }
        // the next stop: pull in if anyone wants it
        for (BusRoutes.Stop st : r.stops) {
            double d = r.ahead(s, st.s);
            if (st.index == lastServed) {
                if (d < 2 || d > r.length - 40) continue;   // just left it
                lastServed = -1;
            }
            if (d > 30) continue;
            if (!needsStop(level, st)) continue;
            target = Math.min(target, Math.sqrt(2 * 0.005 * Math.max(0, d - 0.05)));
            if (d < 0.3 && speed < 0.05) {
                s = st.s;
                speed = 0;
                serving = st.index;
                dwell = 50;
                alight(level, st);
                dwell += board(level, st) * 12;
                level.playSound(null, getX(), getY() + 1, getZ(), SoundEvents.PISTON_CONTRACT, SoundSource.NEUTRAL, 0.5f, 0.6f);
                level.playSound(null, getX(), getY() + 1, getZ(), dev.freerun.economy.ModSounds.BUS_BRAKE, SoundSource.NEUTRAL, 1.0f, 1.0f);
                return;
            }
        }
        // lights and stop signs
        for (int k = 0; k < r.lines.size(); k++) {
            double[] l = r.lines.get(k);
            double d = r.ahead(s, l[0]);
            if (d > 28) continue;
            int i = (int) l[1], j = (int) l[2];
            boolean stopHere;
            if (CityLayout.hasLights(i, j)) {
                TrafficSystem.Signal sig = TrafficSystem.signal(level, CityLayout.intersectionKey(city(), i, j), l[3] > 0.5);
                stopHere = sig == TrafficSystem.Signal.RED || sig == TrafficSystem.Signal.YELLOW && d > 5;
            } else {
                stopHere = passedSign != k;
                if (stopHere && d < 0.6 && speed < 0.03 && ++signWait > 24) {
                    passedSign = k;
                    signWait = 0;
                    stopHere = false;
                }
            }
            if (stopHere) target = Math.min(target, Math.sqrt(2 * 0.006 * Math.max(0, d - 0.05)));
        }
        // traffic in front
        if (ghost > 0) ghost--;
        else {
            double gap = clearAhead(level);
            if (gap < 14) target = Math.min(target, Math.max(0, (gap - 3.0) * 0.05));
        }
        if (speed < 0.01 && target < 0.01) {
            if (++blocked > 300 && level.getNearestPlayer(this, 32) == null) {
                ghost = 100;
                blocked = 0;
            }
        } else {
            blocked = 0;
        }
        boolean brake = target < speed - 0.01;
        entityData.set(BRAKING, brake || speed < 0.01);
        speed = speed < target ? Math.min(target, speed + 0.005) : Math.max(target, speed - 0.018);
        s = r.wrap(s + speed);
    }

    /** Something within 1.4 blocks of the road ahead of the front bumper: how far, or 99. */
    private double clearAhead(ServerLevel level) {
        BusRoutes.Route r = route();
        BlockPos c = city();
        double front = s + BusRoutes.HALF_LENGTH;
        Vec3[] pts = new Vec3[9];
        AABB box = null;
        for (int k = 0; k < pts.length; k++) {
            Vec3 p = r.at(front + 0.5 + k * 1.5).add(c.getX(), c.getY() + 1, c.getZ());
            pts[k] = p;
            AABB b = new AABB(p.x - 2, p.y - 1, p.z - 2, p.x + 2, p.y + 3, p.z + 2);
            box = box == null ? b : box.minmax(b);
        }
        double best = 99;
        List<Entity> found = level.getEntities(this, box.inflate(5, 0, 5), e -> e.isAlive() && e.getVehicle() == null
                && (e instanceof CarEntity || e instanceof LivingEntity || e instanceof BusEntity));
        for (Entity e : found) {
            Vec3[] probe;
            double reach;
            if (e instanceof BusEntity other) {
                Vec3 f = other.forward().scale(BusRoutes.HALF_LENGTH - 0.5);
                probe = new Vec3[]{other.position(), other.position().add(f), other.position().subtract(f)};
                reach = 2.0;
            } else {
                probe = new Vec3[]{e.position()};
                reach = e instanceof CarEntity ? 2.0 : 1.4;
            }
            for (int k = 0; k < pts.length; k++) {
                for (Vec3 q : probe) {
                    double dx = q.x - pts[k].x, dz = q.z - pts[k].z;
                    if (dx * dx + dz * dz < reach * reach && Math.abs(q.y - pts[k].y) < 2.5) best = Math.min(best, 0.5 + k * 1.5);
                }
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ stops

    private Vec3 world(Vec3 local) {
        BlockPos c = city();
        return new Vec3(c.getX() + local.x, c.getY() + 1, c.getZ() + local.z);
    }

    private boolean needsStop(ServerLevel level, BusRoutes.Stop st) {
        for (Entity e : getPassengers()) {
            if (e instanceof Player) return true;
            if (e instanceof CitizenEntity c && c.busTo() == st.index && c.busRoute() == route().id) return true;
        }
        Vec3 w = world(st.wait);
        AABB area = new AABB(w.x - 3.5, w.y - 1, w.z - 3.5, w.x + 3.5, w.y + 2.5, w.z + 3.5);
        if (!level.getEntitiesOfClass(Player.class, area, p -> !p.isSpectator() && p.getVehicle() == null).isEmpty()) return true;
        return !level.getEntitiesOfClass(CitizenEntity.class, area, c -> c.waitingFor(route().id, st.index)).isEmpty();
    }

    private void alight(ServerLevel level, BusRoutes.Stop st) {
        Vec3 w = world(st.wait);
        for (Entity e : List.copyOf(getPassengers())) {
            if (e instanceof CitizenEntity c && c.busTo() == st.index) c.leaveBus(w.add((random.nextFloat() - 0.5) * 1.5, 0, (random.nextFloat() - 0.5) * 1.5));
        }
    }

    /** Lets on whoever's waiting. Returns how many got on. */
    private int board(ServerLevel level, BusRoutes.Stop st) {
        Vec3 w = world(st.wait);
        AABB area = new AABB(w.x - 3.5, w.y - 1, w.z - 3.5, w.x + 3.5, w.y + 2.5, w.z + 3.5);
        int n = 0;
        for (CitizenEntity c : level.getEntitiesOfClass(CitizenEntity.class, area, c -> c.waitingFor(route().id, st.index))) {
            if (getPassengers().size() >= SEATS) break;
            c.boardBus(this);
            n++;
        }
        return n;
    }

    // ------------------------------------------------------------------ riding

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (player.getVehicle() == this) return InteractionResult.PASS;
        if (speed > 0.05) {
            player.displayClientMessage(Component.literal("§7The bus is moving. Wait at a stop and it'll pull in for you."), true);
            return InteractionResult.CONSUME;
        }
        if (getPassengers().size() >= SEATS) {
            player.displayClientMessage(Component.literal("§cThe bus is full."), true);
            return InteractionResult.CONSUME;
        }
        if (!dev.freerun.economy.Money.pay(player, 2)) {
            player.displayClientMessage(Component.literal("§cThe fare is $2."), true);
            return InteractionResult.CONSUME;
        }
        player.startRiding(this);
        player.displayClientMessage(Component.literal("§eRoute " + route().title() + " §7· paid $2 · Sneak to get off"), true);
        level().playSound(null, getX(), getY() + 1, getZ(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.NEUTRAL, 0.6f, 1.4f);
        return InteractionResult.CONSUME;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < SEATS;
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction move) {
        int i = getPassengers().indexOf(passenger);
        if (i < 0) return;
        // two seats a row, eight rows, from just behind the driver to the back
        int row = i / 2, side = i % 2;
        double across = side == 0 ? 0.75 : -0.75, along = 1.9 - row * 0.85;
        Vec3 f = forward(), r = right();
        Vec3 at = position().add(f.scale(along)).add(r.scale(across * 1.0));
        double y = getY() + 0.97 - passenger.getVehicleAttachmentPoint(this).y;   // sat on the seat cushion
        move.accept(passenger, at.x, y, at.z);
        if (passenger instanceof CitizenEntity c) {
            c.setYBodyRot(getYRot());
            c.setYHeadRot(getYRot());
        }
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        return position().add(forward().scale(3.0)).add(right().scale(2.3)).add(0, 0.1, 0);
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
        if (source.getEntity() instanceof Player p && p.getAbilities().instabuild && !level().isClientSide) {
            ejectPassengers();
            discard();
            return true;
        }
        return false;
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        // the client works out where the bus is from the route itself
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return d < 160 * 160;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
