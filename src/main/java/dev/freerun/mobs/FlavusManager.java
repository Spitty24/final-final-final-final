package dev.freerun.mobs;

import dev.freerun.evil.EvilConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;

/**
 * Brings Flavus Florbington to each player 3 minutes after they join, unless he's already
 * around. If he's killed, he comes back 5 minutes later.
 */
public final class FlavusManager {
    private static final int FIRST_DELAY = 3600;     // 3 minutes
    private static final int RESPAWN_DELAY = 6000;   // 5 minutes

    private int ticks;
    private int nextSpawn = FIRST_DELAY;
    private FlavusEntity flavus;

    public void tick(ServerPlayer player) {
        if (!EvilConfig.get().flavus) return;
        ticks++;

        if (flavus != null && flavus.isRemoved()) {
            boolean killed = flavus.getRemovalReason() == Entity.RemovalReason.KILLED;
            flavus = null;
            // If he was only unloaded with his chunk, he still exists in the world — don't make another.
            nextSpawn = killed ? ticks + RESPAWN_DELAY : Integer.MAX_VALUE;
        }
        if (flavus == null && ticks >= nextSpawn) {
            ServerLevel level = (ServerLevel) player.level();
            List<FlavusEntity> existing = level.getEntitiesOfClass(FlavusEntity.class,
                    player.getBoundingBox().inflate(128), FlavusEntity::isAlive);
            if (!existing.isEmpty()) {
                flavus = existing.get(0);       // he's already nearby
            } else {
                spawn(level, player);
            }
        }
    }

    private void spawn(ServerLevel level, ServerPlayer player) {
        double angle = player.getRandom().nextDouble() * Math.PI * 2;
        double dist = 6 + player.getRandom().nextDouble() * 4;
        int x = (int) Math.floor(player.getX() + Math.cos(angle) * dist);
        int z = (int) Math.floor(player.getZ() + Math.sin(angle) * dist);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

        FlavusEntity f = new FlavusEntity(SoldierRegistry.FLAVUS, level);
        f.moveTo(x + 0.5, y, z + 0.5, player.getYRot(), 0);
        level.addFreshEntity(f);
        flavus = f;
        player.displayClientMessage(Component.literal("§6Flavus Florbington has arrived. He smells gold..."), true);
    }
}
