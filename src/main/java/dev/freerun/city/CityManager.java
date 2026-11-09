package dev.freerun.city;

import dev.freerun.vehicle.CarEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps the streets busy wherever a player is. A city of over a thousand blocks can't simulate
 * every resident at once, so instead the part of town around each player is always populated:
 * a few dozen people on the sidewalks (fewer at night), traffic on the roads, and a couple of
 * police cruisers on patrol. New arrivals appear a good way off, never right in front of you;
 * people and cars that end up far from every player are quietly tidied away.
 */
public final class CityManager {
    private static final int PEOPLE_DAY = 34, PEOPLE_NIGHT = 14;
    private static final int CARS_DAY = 13, CARS_NIGHT = 7;
    private static final int CRUISERS = 2;
    private static final double FAR = 150.0;

    private CityManager() {}

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(level -> {
            if (level.dimension() != Level.OVERWORLD) return;
            long now = level.getGameTime();
            if (now % 40 == 13) {
                for (ServerPlayer p : level.players()) {
                    if (p.isSpectator()) continue;
                    BlockPos city = CityWorld.cityAt(level, p.blockPosition());
                    if (city != null) {
                        populate(level, city, p);
                        BusManager.populate(level, city, p);
                    }
                }
            }
            if (now % 100 == 51) {
                tidy(level);
                BusManager.tidy(level);
            }
        });
    }

    private static boolean night(ServerLevel level) {
        long t = level.getDayTime() % 24000L;
        return t >= 13000L && t < 23500L;
    }

    private static void populate(ServerLevel level, BlockPos city, ServerPlayer p) {
        boolean night = night(level);
        RandomSource r = level.getRandom();
        // people on foot
        int people = level.getEntitiesOfClass(CitizenEntity.class, p.getBoundingBox().inflate(64.0, 20.0, 64.0),
                c -> !c.isPassenger()).size();
        int want = night ? PEOPLE_NIGHT : PEOPLE_DAY;
        for (int k = 0; k < 3 && people < want; k++) if (spawnPedestrian(level, city, p, r)) people++;
        // traffic
        int cars = level.getEntitiesOfClass(CarEntity.class, p.getBoundingBox().inflate(96.0, 24.0, 96.0),
                c -> c.npcDriven() && !c.isPolice()).size();
        int wantCars = night ? CARS_NIGHT : CARS_DAY;
        for (int k = 0; k < 2 && cars < wantCars; k++) {
            if (spawnTraffic(level, city, p.blockPosition(), r, 40, 96, false) != null) cars++;
        }
        // police on patrol
        int cruisers = level.getEntitiesOfClass(CarEntity.class, p.getBoundingBox().inflate(128.0, 24.0, 128.0),
                c -> c.isPolice() && c.getFirstPassenger() instanceof PoliceOfficerEntity).size();
        if (cruisers < CRUISERS && r.nextInt(3) == 0) spawnCruiser(level, city, p.blockPosition(), 60, 120, false);
    }

    private static boolean nearAnyPlayer(ServerLevel level, double x, double y, double z, double d) {
        return level.getNearestPlayer(x, y, z, d, false) != null;
    }

    /** Someone stepping out onto the sidewalk 24 to 60 blocks away. */
    private static boolean spawnPedestrian(ServerLevel level, BlockPos city, ServerPlayer p, RandomSource r) {
        for (int attempt = 0; attempt < 6; attempt++) {
            double ang = r.nextDouble() * Math.PI * 2, dist = 24 + r.nextDouble() * 36;
            double lx = p.getX() + Math.cos(ang) * dist - city.getX(), lz = p.getZ() + Math.sin(ang) * dist - city.getZ();
            if (lx < 1 || lz < 1 || lx > CityLayout.SIZE - 2 || lz > CityLayout.SIZE - 2) continue;
            double[] walk = sidewalk(lx, lz, r);
            double x = city.getX() + walk[0], z = city.getZ() + walk[1], y = city.getY() + 1;
            BlockPos at = BlockPos.containing(x, y, z);
            if (!level.isLoaded(at) || !level.getBlockState(at.below()).isSolid() || !level.getBlockState(at).isAir()) continue;
            if (nearAnyPlayer(level, x, y, z, 20.0)) continue;
            CitizenEntity c = new CitizenEntity(CityRegistry.CITIZEN, level);
            CitizenEntity.Job[] jobs = CitizenEntity.Job.values();
            CitizenEntity.Job job = jobs[r.nextInt(jobs.length)];
            BlockPos home = homeNear(city, walk[0], walk[1], r, at);
            c.setup(r, city, home, job);
            c.setAmbient(true);
            c.moveTo(x, y, z, r.nextFloat() * 360f, 0f);
            if (!level.noCollision(c)) continue;
            level.addFreshEntity(c);
            return true;
        }
        return false;
    }

    private static BlockPos homeNear(BlockPos city, double lx, double lz, RandomSource r, BlockPos fallback) {
        List<BlockPos> homes = new ArrayList<>(CityLayout.spotsNear(CityLayout.District.SUBURB, lx, lz, 140));
        homes.addAll(CityLayout.spotsNear(CityLayout.District.HOOD, lx, lz, 140));
        return homes.isEmpty() ? fallback : city.offset(homes.get(r.nextInt(homes.size())));
    }

    /** Snaps a local point to the nearest sidewalk (the outer block of the nearest street). */
    private static double[] sidewalk(double lx, double lz, RandomSource r) {
        int bx = 0, bz = 0;
        double dx = Double.MAX_VALUE, dz = Double.MAX_VALUE;
        for (int x : CityLayout.XS) if (Math.abs(lx - (x + 5)) < dx) { dx = Math.abs(lx - (x + 5)); bx = x; }
        for (int z : CityLayout.ZS) if (Math.abs(lz - (z + 5)) < dz) { dz = Math.abs(lz - (z + 5)); bz = z; }
        double side = r.nextBoolean() ? 0.5 : 9.5;
        return dx < dz ? new double[]{bx + side, lz} : new double[]{lx, bz + side};
    }

    /**
     * A car in a lane between {@code min} and {@code max} blocks from {@code near}, out of sight
     * of every player, driving away from the nearest intersection. Police if asked.
     */
    static CarEntity spawnTraffic(ServerLevel level, BlockPos city, BlockPos near, RandomSource r, int min, int max, boolean police) {
        for (int attempt = 0; attempt < 6; attempt++) {
            double ang = r.nextDouble() * Math.PI * 2, dist = min + r.nextDouble() * (max - min);
            double lx = near.getX() + Math.cos(ang) * dist - city.getX(), lz = near.getZ() + Math.sin(ang) * dist - city.getZ();
            boolean alongX = r.nextBoolean();
            int line = nearestLine(alongX ? lz : lx);
            int seg = segment(alongX ? lx : lz);
            if (seg < 0) continue;
            double a0 = CityLayout.XS[seg] + CityLayout.ROAD + 4, a1 = CityLayout.XS[seg + 1] - 4;
            if (a1 <= a0) continue;
            double along = Mth.clamp(alongX ? lx : lz, a0, a1);
            int h = alongX ? (r.nextBoolean() ? CityLayout.EAST : CityLayout.WEST) : (r.nextBoolean() ? CityLayout.SOUTH : CityLayout.NORTH);
            double lane = CityLayout.laneCoord(h, line);
            double x = city.getX() + (alongX ? along : lane), z = city.getZ() + (alongX ? lane : along), y = city.getY() + 1;
            BlockPos at = BlockPos.containing(x, y, z);
            if (!level.isLoaded(at) || !level.getBlockState(at).isAir()) continue;
            if (nearAnyPlayer(level, x, y, z, Math.min(30, min - 5))) continue;
            CarEntity car = police ? cruiser(level, city, x, y, z, h, r) : CityHubEntity.spawnDrivenCar(level, city, x, y, z, h, r);
            if (car == null) continue;
            car.setAmbient(true);
            if (car.getFirstPassenger() instanceof CitizenEntity c) c.setAmbient(true);
            return car;
        }
        return null;
    }

    private static CarEntity cruiser(ServerLevel level, BlockPos city, double x, double y, double z, int h, RandomSource r) {
        CarEntity car = new CarEntity(dev.freerun.vehicle.VehicleRegistry.CAR, level);
        float yaw = CityLayout.yaw(h);
        car.moveTo(x, y, z, yaw, 0f);
        car.setYBodyRot(yaw);
        car.setYHeadRot(yaw);
        car.setPolice(true);
        car.addFuel(100f);
        if (!level.noCollision(car)) return null;
        car.setCity(city);
        PoliceOfficerEntity cop = new PoliceOfficerEntity(CityRegistry.POLICE_OFFICER, level);
        cop.setup(r.nextInt(1000), null);
        cop.setAmbient(true);
        cop.moveTo(x, y, z, yaw, 0f);
        cop.startRiding(car, true);
        cop.setCar(car);
        level.addFreshEntityWithPassengers(car);
        return car;
    }

    /** A police cruiser on the road near {@code near}; returns the officer at the wheel. */
    static PoliceOfficerEntity spawnCruiser(ServerLevel level, BlockPos city, BlockPos near, int min, int max, boolean responding) {
        CarEntity car = spawnTraffic(level, city, near, level.getRandom(), min, max, true);
        if (car == null) return null;
        if (responding) car.setSiren(true);
        return car.getFirstPassenger() instanceof PoliceOfficerEntity cop ? cop : null;
    }

    private static int nearestLine(double local) {
        int best = 0;
        double bestD = Double.MAX_VALUE;
        for (int k = 0; k < CityLayout.XS.length; k++) {
            double d = Math.abs(CityLayout.XS[k] + CityLayout.ROAD / 2.0 - local);
            if (d < bestD) {
                bestD = d;
                best = k;
            }
        }
        return best;
    }

    private static int segment(double local) {
        for (int k = 0; k < CityLayout.XS.length - 1; k++) if (local >= CityLayout.XS[k] && local < CityLayout.XS[k + 1]) return k;
        return -1;
    }

    /** Sends home whoever the city brought out, once no player is anywhere near. */
    private static void tidy(ServerLevel level) {
        List<Entity> gone = new ArrayList<>();
        for (CitizenEntity c : level.getEntities(CityRegistry.CITIZEN, CitizenEntity::ambient)) {
            if (!nearAnyPlayer(level, c.getX(), c.getY(), c.getZ(), FAR)) gone.add(c);
        }
        for (PoliceOfficerEntity o : level.getEntities(CityRegistry.POLICE_OFFICER, PoliceOfficerEntity::ambient)) {
            if (!o.busy() && !nearAnyPlayer(level, o.getX(), o.getY(), o.getZ(), FAR + 20)) gone.add(o);
        }
        for (CarEntity car : level.getEntities(dev.freerun.vehicle.VehicleRegistry.CAR, CarEntity::ambient)) {
            boolean empty = car.getPassengers().isEmpty();
            boolean mobDriven = car.npcDriven();
            if ((empty || mobDriven) && !nearAnyPlayer(level, car.getX(), car.getY(), car.getZ(), FAR)) gone.add(car);
        }
        for (Entity e : gone) {
            if (e.getVehicle() instanceof CarEntity car && car.ambient()) {
                car.ejectPassengers();
                car.discard();
            }
            if (e instanceof CarEntity car) {
                for (Entity passenger : car.getPassengers()) passenger.discard();
            }
            e.discard();
        }
    }
}
