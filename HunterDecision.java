package dev.freerun.evil.ai;

/** One high-level plan from the model. All fields optional; defaults are safe. */
public final class HunterDecision {
    /** "chase" (go straight at them), "cutoff" (head to targetX/Y/Z to intercept), "flank", "ambush". */
    public String strategy = "chase";
    public Double targetX;
    public Double targetY;
    public Double targetZ;
    /** 0..1 — scales the hunter's speed this plan. */
    public double aggression = 0.6;
    /** A short line shown to the player (optional). */
    public String taunt;

    public boolean hasPoint() {
        return targetX != null && targetY != null && targetZ != null;
    }
}
