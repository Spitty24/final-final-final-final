package dev.freerun.client.evil;

import dev.freerun.ParkourState;
import dev.freerun.evil.EvilConfig;
import net.minecraft.client.player.LocalPlayer;

import java.util.Random;

/**
 * The "cursed" twist on the base parkour: wall moves randomly give out, and hard landings
 * leave you limping. Queried by {@link dev.freerun.client.ParkourController}.
 */
public final class CursedParkour {
    private static final Random RAND = new Random();

    private CursedParkour() {}

    /** True if the current wall move should randomly fail this tick. */
    public static boolean shouldFail(ParkourState state) {
        EvilConfig cfg = EvilConfig.get();
        if (!cfg.evilMode || !cfg.cursedParkour) return false;
        if (!(state.isWallRun() || state == ParkourState.WALL_CLIMB || state == ParkourState.LEDGE_HANG)) {
            return false;
        }
        return RAND.nextDouble() < cfg.failChance;
    }

    /** Ticks a grabbed ledge survives before crumbling (Integer.MAX_VALUE = never). */
    public static int ledgeLifetime() {
        EvilConfig cfg = EvilConfig.get();
        if (!cfg.evilMode || !cfg.cursedParkour || cfg.ledgeCrumbleTicks <= 0) return Integer.MAX_VALUE;
        return cfg.ledgeCrumbleTicks;
    }

    /** Apply a limp (slowness) after a rough landing. */
    public static void maybeLimp(LocalPlayer player, float fallDistance) {
        EvilConfig cfg = EvilConfig.get();
        if (!cfg.evilMode || !cfg.cursedParkour || cfg.limpTicks <= 0) return;
        if (fallDistance > 4f) {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, cfg.limpTicks, 1));
        }
    }
}
