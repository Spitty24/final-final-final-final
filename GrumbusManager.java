package dev.freerun.mobs;

import dev.freerun.evil.EvilConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Ensures each player has a Grumbus pestering them: first one appears ~5 seconds (100 ticks) after
 * they join, and a fresh one turns up ~5 seconds after the last is defeated.
 */
public final class GrumbusManager {
    private int ticks;
    private int respawnTimer;
    private GrumbusEntity grumbus;

    public void tick(ServerPlayer player) {
        if (!EvilConfig.get().grumbus) {
            if (grumbus != null) { grumbus.discard(); grumbus = null; }
            return;
        }
        ticks++;

        if (grumbus != null && !grumbus.isAlive()) {
            grumbus = null;
            respawnTimer = 100;
        }

        if (grumbus == null) {
            if (respawnTimer > 0) { respawnTimer--; return; }
            if (ticks < 100) return;              // the first 5 seconds of grace
            spawn((ServerLevel) player.level(), player);
            return;
        }

        grumbus.setTarget(player);                // never lets you go
    }

    private void spawn(ServerLevel level, ServerPlayer player) {
        Vec3 pos = player.position().add((player.getRandom().nextDouble() - 0.5) * 6, 0,
                (player.getRandom().nextDouble() - 0.5) * 6);
        GrumbusEntity g = new GrumbusEntity(SoldierRegistry.GRUMBUS, level);
        g.moveTo(pos.x, player.getY(), pos.z, player.getYRot(), 0);
        g.setTarget(player);
        level.addFreshEntity(g);
        grumbus = g;
        player.displayClientMessage(net.minecraft.network.chat.Component.literal("§7Grumbus has arrived."), true);
    }
}
