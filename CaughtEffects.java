package dev.freerun.client.evil;

import dev.freerun.net.CaughtPayload;
import net.minecraft.client.Minecraft;

/** Client reaction to a caught packet. Screen glitch is drawn by {@link GlitchOverlay}. */
public final class CaughtEffects {
    private CaughtEffects() {}

    public static void handle(int kind) {
        Minecraft mc = Minecraft.getInstance();
        switch (kind) {
            case CaughtPayload.FAKE_OUT -> GlitchOverlay.trigger(70);
            case CaughtPayload.SCATTER -> GlitchOverlay.trigger(40);
            case CaughtPayload.CURSE -> GlitchOverlay.trigger(30);
            case CaughtPayload.DELETE_WORLD -> {
                GlitchOverlay.trigger(50);
                WorldDeleter.run();
            }
            default -> { }
        }
    }
}
