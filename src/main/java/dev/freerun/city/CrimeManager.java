package dev.freerun.city;

import dev.freerun.career.Careers;
import dev.freerun.economy.Money;
import dev.freerun.vehicle.CarEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Crime, rarely. For each player in a city, every two and a half to five minutes there's about a
 * one-in-three chance something happens somewhere near them, close enough to see: a purse
 * snatching, a mugging at gunpoint, or somebody stealing a car out from under its driver. It's
 * called in on the radio, the nearest police officer responds, and if you get there first you can
 * stop it yourself for a reward.
 */
public final class CrimeManager {
    private static final Map<UUID, Long> NEXT = new HashMap<>();

    private CrimeManager() {}

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(level -> {
            if (level.dimension() != Level.OVERWORLD || level.getGameTime() % 20 != 7) return;
            long now = level.getGameTime();
            for (ServerPlayer p : level.players()) {
                if (p.isSpectator()) continue;
                long next = NEXT.computeIfAbsent(p.getUUID(), k -> now + interval(p.getRandom()));
                if (now < next) continue;
                NEXT.put(p.getUUID(), now + interval(p.getRandom()));
                BlockPos city = CityWorld.cityAt(level, p.blockPosition());
                if (city == null || p.getRandom().nextFloat() > 0.35f) continue;
                if (!level.getEntitiesOfClass(CriminalEntity.class, p.getBoundingBox().inflate(160.0)).isEmpty()) continue;
                stage(level, p, city, null);
            }
        });
    }

    private static long interval(RandomSource r) {
        return 20L * (150 + r.nextInt(151));
    }

    /** Starts a crime near {@code p}. {@code kind} null picks one. Returns false if there was nobody to rob. */
    public static boolean stage(ServerLevel level, ServerPlayer p, BlockPos city, CriminalEntity.Kind kind) {
        RandomSource r = p.getRandom();
        if (kind == null) {
            int roll = r.nextInt(100);
            kind = roll < 45 ? CriminalEntity.Kind.SNATCHER : roll < 80 ? CriminalEntity.Kind.MUGGER : CriminalEntity.Kind.CAR_THIEF;
        }
        AABB ring = p.getBoundingBox().inflate(60.0, 12.0, 60.0);
        CarEntity car = null;
        CitizenEntity victim = null;
        if (kind == CriminalEntity.Kind.CAR_THIEF) {
            List<CarEntity> cars = level.getEntitiesOfClass(CarEntity.class, ring.inflate(15.0),
                    c -> c.getFirstPassenger() instanceof CitizenEntity && !c.isPolice() && c.distanceToSqr(p) > 18 * 18);
            if (cars.isEmpty()) kind = CriminalEntity.Kind.SNATCHER;
            else car = cars.get(r.nextInt(cars.size()));
        }
        if (car == null) {
            List<CitizenEntity> people = level.getEntitiesOfClass(CitizenEntity.class, ring,
                    c -> !c.isPassenger() && c.distanceToSqr(p) > 14 * 14 && c.money() > 10);
            if (people.isEmpty()) return false;
            victim = people.get(r.nextInt(people.size()));
        }
        LivingAnchor a = car != null ? new LivingAnchor(car.position(), car.getYRot()) : new LivingAnchor(victim.position(), victim.getYRot());
        CriminalEntity crook = new CriminalEntity(CityRegistry.CRIMINAL, level);
        // come from behind the victim, a few blocks back
        double ang = Math.toRadians(a.yaw) + (r.nextDouble() - 0.5);
        double dist = car != null ? 9.0 : 6.0;
        Vec3 from = a.pos.add(Math.sin(ang) * dist, 0, -Math.cos(ang) * dist);   // behind: opposite the way they face
        crook.moveTo(from.x, a.pos.y, from.z, r.nextFloat() * 360f, 0f);
        if (!level.noCollision(crook)) crook.moveTo(a.pos.x + 2, a.pos.y, a.pos.z + 2, 0f, 0f);
        crook.setup(kind, victim, car);
        level.addFreshEntity(crook);
        return true;
    }

    private record LivingAnchor(Vec3 pos, float yaw) {}

    /** The crime has happened: call it in and send a unit. */
    static void committed(ServerLevel level, CriminalEntity crook, CriminalEntity.Kind kind, int took) {
        BlockPos at = crook.blockPosition();
        String where = landmark(level, at);
        String what = switch (kind) {
            case SNATCHER -> "Purse snatching";
            case MUGGER -> "Armed robbery in progress";
            case CAR_THIEF -> "Vehicle stolen";
        };
        Police.announce(level, at, "§c🚨 Dispatch: " + what + " " + where + ". Suspect fleeing"
                + (kind == CriminalEntity.Kind.CAR_THIEF ? " in the vehicle." : " on foot.")
                + " §7(Take them down and cuff them for a reward.)", 200);
        CityWorld.recordCrime(level, at, false, false);
        Police.dispatch(level, crook, at);
    }

    private static String landmark(ServerLevel level, BlockPos at) {
        BlockPos city = CityWorld.cityAt(level, at);
        if (city == null) return "nearby";
        double lx = at.getX() - city.getX(), lz = at.getZ() - city.getZ();
        CityLayout.Lot best = null;
        double bestD = 90 * 90;
        for (CityLayout.Lot l : CityLayout.lots()) {
            if (!l.district().landmark()) continue;
            double dx = l.centre().getX() - lx, dz = l.centre().getZ() - lz;
            double d = dx * dx + dz * dz;
            if (d < bestD) {
                bestD = d;
                best = l;
            }
        }
        if (best != null) return "near the " + best.district().title;
        CityLayout.District d = CityLayout.districtAt(lx, lz);
        return d != null ? "in " + d.title : "on the street";
    }

    /** What the city pays a citizen who stops a crime: $60 to $150 (plus what they got back), doubled by Loss Prevention. */
    static int reward(ServerPlayer p, CriminalEntity.Kind kind, int recovered) {
        return reward(p, kind, recovered, 1);
    }

    static int reward(ServerPlayer p, CriminalEntity.Kind kind, int recovered, int divisor) {
        int base = switch (kind) {
            case SNATCHER -> 60;
            case MUGGER -> 110;
            case CAR_THIEF -> 150;
        };
        base += p.getRandom().nextInt(31);
        if (Careers.has(p, "loss_prevention")) base *= 2;
        base = Math.max(1, base / Math.max(1, divisor));
        Money.give(p, base);
        Careers.addXp(p, kind == CriminalEntity.Kind.SNATCHER ? 15 : 25, "stopped a crime");
        return base;
    }
}
