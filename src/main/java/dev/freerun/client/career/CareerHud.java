package dev.freerun.client.career;

import dev.freerun.career.Career;
import dev.freerun.career.CareerClientState;
import dev.freerun.career.CareerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** A small career XP readout in the top-left corner that pops up for a few seconds after you earn XP. */
public final class CareerHud {
    private static final long SHOW_MS = 6000;

    private CareerHud() {}

    public static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || CareerClientState.career == Career.NONE.ordinal()) return;
        long since = System.currentTimeMillis() - CareerClientState.lastGainMillis;
        boolean levelUp = System.currentTimeMillis() - CareerClientState.levelUpMillis < SHOW_MS;
        if (since > SHOW_MS && !levelUp) return;

        Career c = Career.byId(CareerClientState.career);
        Font font = mc.font;
        int x = 6, y = 6, w = 128;
        g.fill(x - 3, y - 3, x + w + 3, y + 24, 0x90000000);
        String head = c.title + " · Lv " + CareerClientState.level;
        g.drawString(font, head, x, y, c.color);
        if (since <= SHOW_MS && CareerClientState.lastGain > 0) {
            String gain = "+" + CareerClientState.lastGain + " XP";
            g.drawString(font, gain, x + w - font.width(gain), y, 0x55FF55);
        }
        int need = CareerData.xpToNext(CareerClientState.level);
        float frac = CareerClientState.level >= CareerData.MAX_LEVEL ? 1.0f : Math.min(1.0f, CareerClientState.xp / (float) need);
        g.fill(x, y + 11, x + w, y + 14, 0xFF2A2A30);
        g.fill(x, y + 11, x + (int) (w * frac), y + 14, 0xFF000000 | c.color);
        String sub = levelUp ? "§6Level up! Press P" : CareerClientState.lastReason;
        g.drawString(font, font.plainSubstrByWidth(sub, w), x, y + 16, 0xAAAAAA, false);
    }
}
