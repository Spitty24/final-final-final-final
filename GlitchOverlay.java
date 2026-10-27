package dev.freerun.client.evil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

import java.util.Random;

/** A brief full-screen "your game broke" glitch: static bars, colour tears, a hard flash. */
public final class GlitchOverlay {
    private static final Random RAND = new Random();
    private static int ticksLeft;
    private static int total;

    private GlitchOverlay() {}

    public static void trigger(int ticks) {
        ticksLeft = ticks;
        total = ticks;
    }

    public static void render(GuiGraphics g) {
        if (ticksLeft <= 0) return;
        Minecraft mc = Minecraft.getInstance();
        int w = g.guiWidth();
        int h = g.guiHeight();
        float life = ticksLeft / (float) Math.max(1, total);
        float intensity = Mth.clamp(life * 1.4f, 0f, 1f);

        // hard black/white flash at the start
        if (life > 0.8f) {
            g.fill(0, 0, w, h, (life > 0.9f ? 0xFFFFFFFF : 0xE0000000));
        }
        // torn static bars
        int bars = (int) (40 * intensity);
        for (int i = 0; i < bars; i++) {
            int y = RAND.nextInt(h);
            int bh = 1 + RAND.nextInt(6);
            int x = RAND.nextInt(Math.max(1, w / 3)) - w / 6;
            int bw = w - RAND.nextInt(Math.max(1, w / 2));
            int a = (int) (120 + RAND.nextInt(135) * intensity) << 24;
            int rgb = switch (RAND.nextInt(5)) {
                case 0 -> 0x00FF0033;
                case 1 -> 0x0000FFAA;
                case 2 -> 0x00FFFFFF;
                default -> 0x00000000;
            };
            g.fill(x, y, x + bw, y + bh, a | rgb);
        }
        // red vignette creeping in
        int edge = (int) (0x90 * intensity) << 24 | 0x00FF0000;
        int band = (int) (h * 0.12f);
        g.fillGradient(0, 0, w, band, edge, 0x00000000);
        g.fillGradient(0, h - band, w, h, 0x00000000, edge);

        if (!mc.isPaused()) ticksLeft--;
    }
}
