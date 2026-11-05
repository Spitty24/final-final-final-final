package dev.freerun.city;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The city's signals. Each intersection with lights runs an 18-second cycle (green, yellow, then
 * the other road's turn), offset per intersection so they don't all change at once. Hacks can
 * force an intersection all-green or all-red for a while, and black out street lights.
 */
public final class TrafficSystem {
    public enum Signal { GREEN, YELLOW, RED }

    public enum Override { NONE, ALL_GREEN, ALL_RED }

    private static final int CYCLE = 360;
    private static final int GREEN = 150;
    private static final int YELLOW = 30;

    private record Forced(Override mode, long until) {}
    private record Blackout(BlockPos centre, double radiusSq, long until) {}

    private static final Map<BlockPos, Forced> FORCED = new ConcurrentHashMap<>();
    private static final List<Blackout> BLACKOUTS = new ArrayList<>();

    private TrafficSystem() {}

    private static int offset(BlockPos key) {
        return Math.floorMod(key.getX() * 31 + key.getZ() * 17, CYCLE);
    }

    /** The light shown to traffic travelling along x ({@code alongX}) or z at this intersection. */
    public static Signal signal(Level level, BlockPos key, boolean alongX) {
        Forced f = FORCED.get(key);
        if (f != null) {
            if (level.getGameTime() < f.until()) return f.mode() == Override.ALL_GREEN ? Signal.GREEN : Signal.RED;
            FORCED.remove(key);
        }
        int t = (int) Math.floorMod(level.getGameTime() + offset(key), (long) CYCLE);
        int phase = alongX ? t : (t + CYCLE / 2) % CYCLE;
        if (phase < GREEN) return Signal.GREEN;
        if (phase < GREEN + YELLOW) return Signal.YELLOW;
        return Signal.RED;
    }

    public static void force(Level level, BlockPos key, Override mode, int ticks) {
        FORCED.put(key.immutable(), new Forced(mode, level.getGameTime() + ticks));
    }

    public static synchronized void blackout(Level level, BlockPos centre, double radius, int ticks) {
        BLACKOUTS.add(new Blackout(centre.immutable(), radius * radius, level.getGameTime() + ticks));
    }

    public static synchronized boolean blackedOut(Level level, BlockPos pos) {
        long now = level.getGameTime();
        BLACKOUTS.removeIf(b -> now >= b.until());
        for (Blackout b : BLACKOUTS) if (b.centre().distSqr(pos) < b.radiusSq()) return true;
        return false;
    }

    public static boolean night(Level level) {
        long t = level.getDayTime() % 24000L;
        return t > 12500L && t < 23500L;
    }
}
