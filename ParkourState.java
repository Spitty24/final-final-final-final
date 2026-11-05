package dev.freerun;

/**
 * Every move the mod knows about. The ordinal is what goes over the network,
 * so only ever append new states at the end.
 */
public enum ParkourState {
    //           duration  safe   showsArms
    NONE(          0,      false, false),
    WALLRUN_LEFT(  0,      true,  true),
    WALLRUN_RIGHT( 0,      true,  true),
    WALL_CLIMB(    0,      true,  true),
    LEDGE_HANG(    0,      true,  true),
    MANTLE(        10,     true,  true),
    VAULT(         8,      true,  true),
    SLIDE(         0,      false, true),
    ROLL_READY(    0,      false, true),
    ROLL(          12,     false, true),
    WALL_JUMP(     8,      false, true);

    private static final ParkourState[] VALUES = values();

    /** Fixed length in ticks for scripted moves (0 = open-ended, controlled by input). */
    public final int duration;
    /** Landing while in this state never causes fall damage. */
    public final boolean safe;
    /** First-person view replaces the held item with animated arms. */
    public final boolean showsArms;

    ParkourState(int duration, boolean safe, boolean showsArms) {
        this.duration = duration;
        this.safe = safe;
        this.showsArms = showsArms;
    }

    public static ParkourState byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : NONE;
    }

    public boolean isWallRun() {
        return this == WALLRUN_LEFT || this == WALLRUN_RIGHT;
    }
}
