package dev.freerun.mobs;

/**
 * Timing for the looping office scene between Supervisor Slater and Safety Nick. It's driven by
 * world game time, which the client also knows, so the server (dialogue) and client (animations)
 * stay in sync without extra networking.
 *
 *   0   .. 90   Slater lectures Nick (points at him)
 *   90  .. 310  Nick is on the floor rolling around crying
 *   310 .. 360  Nick stands back up, sniffling
 *   360 .. 480  Slater's phone rings; he takes a call from his boss
 *   480 .. 520  pause, then it all starts again
 */
public final class OfficeSkit {
    public static final int CYCLE = 520;          // 26 seconds
    public static final int DOWN_START = 90;
    public static final int DOWN_END = 310;
    public static final int CALL_START = 360;
    public static final int CALL_END = 480;

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

    /** Phone held up to his ear (from just after the ringing until the call ends). */
    public static boolean slaterOnCall(long gameTime) {
        int t = time(gameTime);
        return t >= CALL_START + 15 && t < CALL_END;
    }
}
