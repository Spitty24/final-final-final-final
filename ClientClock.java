package dev.freerun.client;

/** Client tick counter that stops while paused, so animations freeze with the game. */
public final class ClientClock {
    private static long ticks;

    private ClientClock() {}

    static void tick() {
        ticks++;
    }

    public static float now() {
        return ticks;
    }

    public static float now(float partialTick) {
        return ticks + partialTick;
    }
}
