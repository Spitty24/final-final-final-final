package dev.freerun.mobs;

import dev.freerun.evil.EvilConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Spawns a Wingus near each player once they've been in the world for 2 minutes. After a Wingus
 * leaves (or dies), another one shows up 2 minutes later.
 */
public final class WingusManager {
    private static final int DELAY = 2400;   // 2 minutes in ticks

    private int ticks;
    private int nextSpawn = DELAY;
    private WingusEntity wingus;

    public void tick(ServerPlayer player) {
        if (!EvilConfig.get().wingus) {
            if (wingus != null) { wingus.discard(); wingus = null; }
            return;
        }
        ticks++;

        if (wingus != null && !wingus.isAlive()) {
            wingus = null;
            nextSpawn = ticks + DELAY;
        }
        if (wingus == null && ticks >= nextSpawn) {
            spawn((ServerLevel) player.level(), player);
        }
    }

    private void spawn(ServerLevel level, ServerPlayer player) {
        double angle = player.getRandom().nextDouble() * Math.PI * 2;
        double dist = 8 + player.getRandom().nextDouble() * 6;
        double x = player.getX() + Math.cos(angle) * dist;
        double z = player.getZ() + Math.sin(angle) * dist;

        WingusEntity w = new WingusEntity(SoldierRegistry.WINGUS, level);
        w.moveTo(x, player.getY() + 6, z, 0, 0);
        level.addFreshEntity(w);
        wingus = w;
        player.displayClientMessage(Component.literal("§bA Wingus is circling overhead..."), true);
    }
}
