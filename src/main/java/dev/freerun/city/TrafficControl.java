package dev.freerun.city;

import dev.freerun.vehicle.CarEntity;
import dev.freerun.vehicle.VehicleRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The city's traffic department.
 *
 * Stop signs: a driver who has stopped at the line asks for the intersection; only one car holds
 * it at a time (until it's through, or for four seconds at most), so cars take turns instead of
 * all pulling out together and locking up in the middle.
 *
 * Jams: every five seconds it looks for three or more cars stuck around the same intersection.
 * When it finds one, it waves them through: they pass through each other for a few seconds,
 * which is enough to untangle any knot, and anyone nearby hears about it.
 */
public final class TrafficControl {
    private record Claim(int car, long until) {}

    private static final Map<Level, Map<BlockPos, Claim>> CLAIMS = new HashMap<>();
    private static final int HOLD = 80;

    private TrafficControl() {}

    public static boolean claim(Level level, BlockPos key, CarEntity car) {
        Map<BlockPos, Claim> m = CLAIMS.computeIfAbsent(level, l -> new HashMap<>());
        long now = level.getGameTime();
        Claim c = m.get(key);
        if (c == null || c.until() < now || c.car() == car.getId()) {
            m.put(key, new Claim(car.getId(), now + HOLD));
            return true;
        }
        return false;
    }

    public static void release(Level level, BlockPos key, CarEntity car) {
        Map<BlockPos, Claim> m = CLAIMS.get(level);
        if (m == null) return;
        Claim c = m.get(key);
        if (c != null && c.car() == car.getId()) m.remove(key);
    }

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(level -> {
            if (level.getGameTime() % 100 != 37) return;
            Map<BlockPos, Claim> m = CLAIMS.get(level);
            if (m != null) m.values().removeIf(c -> c.until() < level.getGameTime());
            findJams(level);
        });
    }

    private static void findJams(ServerLevel level) {
        Map<BlockPos, List<CarEntity>> stuck = new HashMap<>();
        for (CarEntity car : level.getEntities(VehicleRegistry.CAR, c -> c.npcDriven() && c.city() != null && !c.ghosting())) {
            if (car.autopilot().stuckTicks() < 100) continue;
            BlockPos key = car.autopilot().nextKey(car.city());
            if (key == null) key = car.blockPosition();
            stuck.computeIfAbsent(key.atY(0), k -> new ArrayList<>()).add(car);
        }
        for (Map.Entry<BlockPos, List<CarEntity>> e : stuck.entrySet()) {
            List<CarEntity> cars = e.getValue();
            if (cars.size() < 3) continue;
            for (int i = 0; i < cars.size(); i++) {
                CarEntity c = cars.get(i);
                c.ghost(60 + i * 15);   // staggered so they don't all leave as one blob
                c.autopilot().reset();
            }
            CarEntity first = cars.get(0);
            Component msg = Component.literal("§7🚦 Traffic control cleared a jam of " + cars.size() + " cars nearby.");
            for (ServerPlayer p : level.players()) if (p.distanceToSqr(first) < 48 * 48) p.displayClientMessage(msg, true);
        }
    }

    /** /cityclear: wave every stuck car within 256 blocks through. */
    public static int clearAll(ServerLevel level, BlockPos near) {
        int n = 0;
        for (CarEntity car : level.getEntities(VehicleRegistry.CAR, c -> c.npcDriven() && c.distanceToSqr(near.getCenter()) < 256 * 256)) {
            if (car.autopilot().stuckTicks() > 20 || Math.abs(car.speed()) < 0.02) {
                car.ghost(80);
                car.autopilot().reset();
                n++;
            }
        }
        return n;
    }
}
