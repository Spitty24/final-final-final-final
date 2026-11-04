package dev.freerun.career;

import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.UUID;

/** The local player's career, as last synced from the server. Only filled in on the client. */
public final class CareerClientState {
    public static UUID owner;
    public static int career;
    public static int level = 1;
    public static int xp;
    public static List<String> skills = List.of();
    public static long lastGainMillis;
    public static int lastGain;
    public static String lastReason = "";
    public static long levelUpMillis;

    private CareerClientState() {}

    public static CareerData data() {
        return new CareerData(career, level, xp, skills);
    }

    public static boolean has(Player p, String skill) {
        return owner != null && owner.equals(p.getUUID()) && skills.contains(skill);
    }
}
