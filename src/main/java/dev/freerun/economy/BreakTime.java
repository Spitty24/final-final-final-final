package dev.freerun.economy;

import dev.freerun.career.CareerData;
import dev.freerun.career.Careers;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/**
 * Break time: every 10 minutes (real time, 12,000 ticks) the buzzer goes and everybody gets a
 * 2-minute break. Factory workers walk over to the Double Quick for a snack, and anyone with a
 * career gets their paycheck by direct deposit.
 */
public final class BreakTime {
    public static final int CYCLE = 12000;
    public static final int LENGTH = 2400;

    private BreakTime() {}

    private static long phase(Level level) {
        return Math.floorMod(level.getGameTime(), (long) CYCLE);
    }

    public static boolean isBreak(Level level) {
        return phase(level) >= CYCLE - LENGTH;
    }

    /** Ticks left until the next break starts (0 while one is on). */
    public static int ticksUntilBreak(Level level) {
        long p = phase(level);
        return p >= CYCLE - LENGTH ? 0 : (int) (CYCLE - LENGTH - p);
    }

    /** Ticks left in the current break (0 when not on break). */
    public static int breakTicksLeft(Level level) {
        long p = phase(level);
        return p >= CYCLE - LENGTH ? (int) (CYCLE - p) : 0;
    }

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(level -> {
            if (level.dimension() != Level.OVERWORLD) return;
            long p = phase(level);
            if (p == CYCLE - LENGTH) start(level);
            else if (p == 0 && level.getGameTime() > 0) end(level);
        });
    }

    private static void start(ServerLevel level) {
        for (ServerPlayer p : level.players()) {
            p.displayClientMessage(Component.literal(
                    "§e§l*BZZZZZT* §r§eBreak time! §7Two minutes. The crew's heading over to the Double Quick."), false);
            p.playNotifySound(ModSounds.BREAK_BUZZER, SoundSource.MASTER, 0.7f, 1.0f);
            CareerData d = Careers.get(p);
            int pay = d.career() == 0 ? 10 : dev.freerun.career.Skills.paycheck(d, 15 + 3 * d.level() + 10 * d.prestigeCount());
            Money.give(p, pay);
            p.displayClientMessage(Component.literal("§a💵 Payday! Direct deposit: " + Money.fmt(pay)
                    + " §7(balance " + Money.fmt(Money.balance(p)) + ")"), false);
        }
    }

    private static void end(ServerLevel level) {
        for (ServerPlayer p : level.players()) {
            p.displayClientMessage(Component.literal("§7*BZZT* Break's over. Back to work."), false);
            p.playNotifySound(ModSounds.BREAK_BUZZER, SoundSource.MASTER, 0.5f, 1.3f);
        }
    }
}
