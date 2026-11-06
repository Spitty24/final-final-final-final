package dev.freerun.alien;

/**
 * What a player currently is. {@code phase} walks NONE → MORPH_IN → ACTIVE → MORPH_OUT → NONE;
 * the tick values are world game time, so every client animates the same thing.
 */
public record Form(Alien alien, int phase, long phaseStart, long abilityStart, long timeoutAt) {
    public static final int NONE = 0, MORPH_IN = 1, ACTIVE = 2, MORPH_OUT = 3;
    public static final int MORPH_IN_TICKS = 34, MORPH_OUT_TICKS = 18;
    /** The first ticks of MORPH_IN: still human, raising the wrist and slapping the dial. */
    public static final int SLAP_TICKS = 12;

    /** Still human, mid slap (0 → 1), or -1. */
    public float slap(long now, float partial) {
        if (alien == null || phase != MORPH_IN) return -1;
        float t = now - phaseStart + partial;
        return t < SLAP_TICKS ? t / SLAP_TICKS : -1;
    }
    public static final int DURATION = 20 * 90, RECHARGE = 20 * 30;

    public static final Form HUMAN = new Form(null, NONE, 0, 0, 0);

    public boolean transformed() {
        return alien != null && phase != NONE;
    }

    /** Transformed and done morphing: powers on, shape fixed. */
    public boolean active() {
        return alien != null && phase == ACTIVE;
    }

    public Form with(int phase, long now) {
        return new Form(alien, phase, now, abilityStart, timeoutAt);
    }

    public Form ability(long now) {
        return new Form(alien, phase, phaseStart, now, timeoutAt);
    }
}
