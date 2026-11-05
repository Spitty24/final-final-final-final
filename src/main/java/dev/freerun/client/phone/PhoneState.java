package dev.freerun.client.phone;

import net.minecraft.nbt.CompoundTag;

import java.util.ArrayDeque;
import java.util.Deque;

/** Client-side phone state: the last city data from the server, settings, and the message log. */
public final class PhoneState {
    public static CompoundTag city = new CompoundTag();
    public static long cityAt;
    /** Blackwire profiler shown even when the phone isn't in your hand. */
    public static boolean alwaysProfile;
    /** Map app zoom: 0 whole city, 1 district, 2 street. */
    public static int mapZoom = 1;
    public static final Deque<String> LOG = new ArrayDeque<>();

    private PhoneState() {}

    public static boolean hasCity() {
        return city.contains("OX");
    }

    public static void log(String line) {
        LOG.addFirst(line);
        while (LOG.size() > 30) LOG.removeLast();
    }
}
