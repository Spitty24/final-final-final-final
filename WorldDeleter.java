package dev.freerun.client.evil;

import dev.freerun.Freerun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelStorageSource;

/**
 * Deletes the current single-player save, the way Minecraft's own "Delete World" button does
 * (via {@link LevelStorageSource.LevelStorageAccess#deleteLevel()}), after leaving the world.
 *
 * Only ever acts on the integrated server's own level id — it has no notion of any other path,
 * and does nothing at all when not in single-player.
 */
public final class WorldDeleter {

    private WorldDeleter() {}

    public static void run() {
        Minecraft mc = Minecraft.getInstance();
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null) return;                       // multiplayer: nothing of ours to delete
        String levelId = server.storageSource.getLevelId();

        mc.execute(() -> {
            // Leave the world first — you can't delete a save while it's loaded.
            if (mc.level != null) {
                mc.level.disconnect();
            }
            mc.disconnect(new GenericMessageScreen(Component.literal("The world is gone.")));

            // Give the integrated server a moment to finish shutting down, then delete.
            new Thread(() -> {
                try {
                    for (int i = 0; i < 100 && server.isRunning(); i++) Thread.sleep(50);
                    try (LevelStorageSource.LevelStorageAccess access =
                                 mc.getLevelSource().createAccess(levelId)) {
                        access.deleteLevel();
                        Freerun.LOGGER.info("Deleted single-player level '{}'", levelId);
                    }
                } catch (Exception e) {
                    Freerun.LOGGER.error("Could not delete level '{}'", levelId, e);
                } finally {
                    mc.execute(() -> mc.setScreen(new TitleScreen()));
                }
            }, "freerun-world-delete").start();
        });
    }
}
