package dev.freerun.mobs;

/**
 * Timing for the looping office scene between Supervisor Slater and Safety Nick. It's driven by
 * world game time, which the client also knows, so the server (dialogue) and client (animations)
 * stay in sync without extra networking. Lines are spaced 3-10 seconds apart so they can be read.
 *
 *   0   .. 160  Slater lectures Nick (points at him)
 *   160 .. 540  Nick is on the floor rolling around crying
 *   540 .. 620  Nick stands back up, sniffling
 *   620 .. 840  Slater's phone rings; he takes a call from his boss
 *   840 .. 900  pause, then it all starts again
 */
public final class OfficeSkit {
    public static final int CYCLE = 900;          // 45 seconds
    public static final int DOWN_START = 160;
    public static final int DOWN_END = 540;
    public static final int CALL_START = 620;
    public static final int CALL_END = 840;

    // When each line is said (ticks into the cycle).
    public static final int LINE_IT_MEETING = 0;
    public static final int LINE_SICK_AND_TIRED = 80;
    public static final int LINE_HATES_ME = DOWN_START;
    public static final int LINE_HATES_ME_AGAIN = DOWN_START + 190;
    public static final int LINE_SNIFF = DOWN_END + 35;
    public static final int RING_1 = CALL_START;
    public static final int RING_2 = CALL_START + 14;
    public static final int LINE_ANSWER = CALL_START + 35;
    public static final int LINE_BOSS = CALL_START + 100;
    public static final int LINE_REPLY = CALL_START + 180;

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
        return t >= CALL_START + 25 && t < CALL_END;
    }
}
