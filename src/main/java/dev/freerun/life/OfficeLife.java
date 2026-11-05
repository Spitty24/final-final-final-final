package dev.freerun.life;

import dev.freerun.city.CitizenEntity;
import dev.freerun.city.CityLayout;
import dev.freerun.city.CityRegistry;
import dev.freerun.city.CityWorld;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fills the office towers with people during working hours (7 AM to 5 PM). Whenever a player is
 * in or next to an office building, empty desk chairs around them get somebody sitting at them:
 * typing away, getting up for the water cooler or the printer, stopping by a coworker's desk,
 * and heading back. They pack up and leave at quitting time.
 */
public final class OfficeLife {
    private static final int WORKERS = 14;

    private OfficeLife() {}

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(level -> {
            if (level.dimension() != Level.OVERWORLD || level.getGameTime() % 60 != 21) return;
            long t = level.getDayTime() % 24000L;
            if (t < 1000L || t > 11000L) return;
            for (ServerPlayer p : level.players()) {
                if (!p.isSpectator() && nearOffice(level, p)) staff(level, p);
            }
        });
    }

    private static boolean nearOffice(ServerLevel level, ServerPlayer p) {
        BlockPos city = CityWorld.cityAt(level, p.blockPosition());
        if (city == null) return false;
        double lx = p.getX() - city.getX(), lz = p.getZ() - city.getZ();
        for (int dx = -16; dx <= 16; dx += 16) for (int dz = -16; dz <= 16; dz += 16) {
            if (CityLayout.districtAt(lx + dx, lz + dz) == CityLayout.District.OFFICE) return true;
        }
        return false;
    }

    private static void staff(ServerLevel level, ServerPlayer p) {
        AABB area = p.getBoundingBox().inflate(22, 8, 22);
        List<CitizenEntity> at = level.getEntitiesOfClass(CitizenEntity.class, area, c -> c.officeSeat() != null);
        if (at.size() >= WORKERS) return;
        Set<BlockPos> taken = new HashSet<>();
        for (CitizenEntity c : at) taken.add(c.officeSeat());
        List<BlockPos> chairs = new ArrayList<>();
        BlockPos c = p.blockPosition();
        for (BlockPos q : BlockPos.betweenClosed(c.offset(-22, -6, -22), c.offset(22, 8, 22))) {
            if (level.getBlockState(q).is(LifeRegistry.OFFICE_CHAIR) && !taken.contains(q)) chairs.add(q.immutable());
        }
        if (chairs.isEmpty()) return;
        RandomSource r = level.getRandom();
        BlockPos city = CityWorld.cityAt(level, p.blockPosition());
        for (int n = 0; n < 3 && at.size() + n < WORKERS && !chairs.isEmpty(); n++) {
            BlockPos chair = chairs.remove(r.nextInt(chairs.size()));
            if (chair.distSqr(p.blockPosition()) < 7 * 7) continue;   // not right in front of you
            CitizenEntity w = new CitizenEntity(CityRegistry.CITIZEN, level);
            CitizenEntity.Job job = CitizenEntity.Job.OFFICE[r.nextInt(CitizenEntity.Job.OFFICE.length)];
            w.setup(r, city, chair, job);
            w.setAmbient(true);
            w.setOffice(chair);
            w.moveTo(chair.getX() + 0.5, chair.getY(), chair.getZ() + 0.5, r.nextFloat() * 360f, 0f);
            level.addFreshEntity(w);
        }
    }
}
