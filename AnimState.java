package dev.freerun.client;

import dev.freerun.ParkourState;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Per-player animation timeline: current + previous move, used to cross-fade poses. */
public final class AnimState {
    /** Ticks to cross-fade between two poses. */
    public static final float BLEND_TICKS = 3f;

    private static final Map<UUID, AnimState> STATES = new HashMap<>();

    public ParkourState state = ParkourState.NONE;
    public ParkourState prev = ParkourState.NONE;
    private float start;
    private float prevStart;
    private float changed = -1000f;

    public static AnimState get(UUID id) {
        return STATES.computeIfAbsent(id, k -> new AnimState());
    }

    public static AnimState peek(UUID id) {
        return STATES.get(id);
    }

    public static void clearAll() {
        STATES.clear();
    }

    public void set(ParkourState s, float now) {
        if (s == state) return;
        prev = state;
        prevStart = start;
        start = now;
        changed = now;
        state = s;
    }

    /** Ticks spent in the current state. */
    public float age(float now) {
        return Math.max(0f, now - start);
    }

    /** How long the previous state lasted (its pose is frozen at that moment while fading out). */
    public float prevAge() {
        return Math.max(0f, changed - prevStart);
    }

    /** 0 = fully previous pose, 1 = fully current pose (smoothstepped). */
    public float blend(float now) {
        float x = Mth.clamp((now - changed) / BLEND_TICKS, 0f, 1f);
        return x * x * (3f - 2f * x);
    }

    public static float ease(float x) {
        x = Mth.clamp(x, 0f, 1f);
        return x * x * (3f - 2f * x);
    }

    /** Normalised progress through a fixed-length move. */
    public static float progress(ParkourState s, float age) {
        return s.duration > 0 ? Mth.clamp(age / s.duration, 0f, 1f) : 0f;
    }
}
