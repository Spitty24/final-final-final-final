package dev.freerun.mobs;

/**
 * Timing for the looping office scene between Supervisor Slater and Safety Nick. It's driven by
 * world game time, which the client also knows, so the server (dialogue) and client (Nick's
 * meltdown animation) stay in sync without extra networking.
 *
 *   0  .. 90   Slater lectures Nick (points at him)
 *   90 .. 310  Nick is on the floor rolling around crying
 *   310.. 400  Nick stands back up, sniffling
 */
public final class OfficeSkit {
    public static final int CYCLE = 400;          // 20 seconds
    public static final int DOWN_START = 90;
    public static final int DOWN_END = 310;

    private OfficeSkit() {}

    public static int time(long gameTime) {
        return (int) Math.floorMod(gameTime, (long) CYCLE);
    }

    public static boolean nickDown(long gameTime) {
        int t = time(gameTime);
        return t >= DOWN_START && t < DOWN_END;
    }

    public static boolean slaterLecturing(long gameTime) {
        return time(gameTime) < DOWN_START;
    }
}
