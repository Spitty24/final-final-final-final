package dev.freerun.city;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps the buses running near players: on every route that passes near someone there's a bus on
 * its way toward them, and a few people waiting at the stops close by. Buses that end up far from
 * everyone are put away, passengers and all.
 */
public final class BusManager {
    private static final double FAR = 230;

    private BusManager() {}

    static void populate(ServerLevel level, BlockPos city, ServerPlayer p) {
        RandomSource rnd = level.getRandom();
        double lx = p.getX() - city.getX(), lz = p.getZ() - city.getZ();
        for (BusRoutes.Route r : BusRoutes.ROUTES) {
            double[] near = r.nearest(lx, lz);
            if (near[1] > 170) continue;
            List<BusEntity> buses = level.getEntitiesOfClass(BusEntity.class, p.getBoundingBox().inflate(FAR, 48, FAR), b -> b.route() == r);
            boolean coming = false;
            for (BusEntity b : buses) if (r.ahead(b.distance(), near[0]) < 170) coming = true;
            if (!coming && buses.size() < 3 && rnd.nextInt(2) == 0) spawnBus(level, city, r, near[0], rnd);
            // a few people waiting at the stops around here
            for (BusRoutes.Stop st : r.stops) {
                Vec3 w = st.wait.add(city.getX(), city.getY() + 1, city.getZ());
                double d = w.distanceTo(p.position());
                if (d < 22 || d > 80 || rnd.nextInt(10) != 0) continue;
                if (!level.isLoaded(BlockPos.containing(w))) continue;
                if (level.getNearestPlayer(w.x, w.y, w.z, 20, false) != null) continue;
                int waiting = level.getEntitiesOfClass(CitizenEntity.class, new net.minecraft.world.phys.AABB(w, w).inflate(4, 2, 4),
                        c -> c.waitingFor(r.id, st.index)).size();
                if (waiting >= 2) continue;
                spawnRider(level, city, r, st, w, rnd);
            }
        }
    }

    private static void spawnBus(ServerLevel level, BlockPos city, BusRoutes.Route r, double playerS, RandomSource rnd) {
        for (int attempt = 0; attempt < 4; attempt++) {
            double s = r.wrap(playerS - 70 - rnd.nextDouble() * 80);
            Vec3 p = r.at(s).add(city.getX(), city.getY() + 1, city.getZ());
            if (!level.isLoaded(BlockPos.containing(p))) continue;
            if (level.getNearestPlayer(p.x, p.y, p.z, 40, false) != null) continue;
            if (!level.getEntitiesOfClass(BusEntity.class, new net.minecraft.world.phys.AABB(p, p).inflate(25, 6, 25)).isEmpty()) continue;
            BusEntity bus = BusEntity.create(level, city, r, s);
            level.addFreshEntity(bus);
            // a few people already on board, off to stops further round
            int riders = rnd.nextInt(6);
            for (int k = 0; k < riders; k++) {
                BusRoutes.Stop to = r.stops.get(rnd.nextInt(r.stops.size()));
                CitizenEntity c = citizen(level, city, p, rnd);
                if (c == null) break;
                c.setBusTrip(r.id, -1, to.index, BlockPos.containing(to.wait.add(city.getX(), city.getY() + 1, city.getZ())).offset(rnd.nextInt(30) - 15, 0, rnd.nextInt(30) - 15));
                level.addFreshEntity(c);
                c.boardBus(bus);
            }
            return;
        }
    }

    private static void spawnRider(ServerLevel level, BlockPos city, BusRoutes.Route r, BusRoutes.Stop st, Vec3 w, RandomSource rnd) {
        // off to a stop somewhere further round the loop
        int hops = 2 + rnd.nextInt(Math.max(1, r.stops.size() / 2));
        BusRoutes.Stop to = r.stops.get((st.index + hops) % r.stops.size());
        Vec3 at = w.add((rnd.nextFloat() - 0.5) * 2, 0, (rnd.nextFloat() - 0.5) * 2);
        CitizenEntity c = citizen(level, city, at, rnd);
        if (c == null || !level.noCollision(c)) return;
        BlockPos goal = BlockPos.containing(to.wait.add(city.getX(), city.getY() + 1, city.getZ())).offset(rnd.nextInt(30) - 15, 0, rnd.nextInt(30) - 15);
        c.setBusTrip(r.id, st.index, to.index, goal);
        c.startWaiting();
        level.addFreshEntity(c);
    }

    private static CitizenEntity citizen(ServerLevel level, BlockPos city, Vec3 at, RandomSource rnd) {
        CitizenEntity c = new CitizenEntity(CityRegistry.CITIZEN, level);
        CitizenEntity.Job[] jobs = CitizenEntity.Job.values();
        BlockPos home = BlockPos.containing(at);
        List<BlockPos> homes = new ArrayList<>(CityLayout.spotsNear(CityLayout.District.SUBURB, at.x - city.getX(), at.z - city.getZ(), 200));
        if (!homes.isEmpty()) home = city.offset(homes.get(rnd.nextInt(homes.size())));
        c.setup(rnd, city, home, jobs[rnd.nextInt(jobs.length)]);
        c.setAmbient(true);
        c.moveTo(at.x, at.y, at.z, rnd.nextFloat() * 360f, 0f);
        return c;
    }

    static void tidy(ServerLevel level) {
        List<Entity> gone = new ArrayList<>();
        for (BusEntity b : level.getEntities(CityRegistry.BUS, e -> true)) {
            if (level.getNearestPlayer(b.getX(), b.getY(), b.getZ(), FAR, false) == null) gone.add(b);
        }
        for (Entity e : gone) {
            for (Entity passenger : List.copyOf(e.getPassengers())) {
                if (passenger instanceof CitizenEntity) passenger.discard();
                else passenger.stopRiding();
            }
            e.discard();
        }
    }
}
