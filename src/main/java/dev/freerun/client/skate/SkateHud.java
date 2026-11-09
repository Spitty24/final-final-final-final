package dev.freerun.client.skate;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** The trick line, the points, landed/bailed pop-ups, the flick stick and the manual balance meter. */
public final class SkateHud {
    private SkateHud() {}

    public static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;
        Font f = mc.font;
        int w = g.guiWidth(), h = g.guiHeight();
        // pop-ups live on for a moment after you step off
        int py = h / 2 - 50;
        for (SkateClient.Popup p : SkateClient.popups) {
            int a = Mth.clamp(p.ticks()[0] * 10, 0, 255);
            if (a < 8) continue;
            g.drawCenteredString(f, p.text(), w / 2, py, (a << 24) | 0xFFFFFF);
            py -= 12;
        }
        if (!SkateClient.active()) return;

        // the line so far
        if (!SkateClient.line.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            int from = Math.max(0, SkateClient.line.size() - 5);
            if (from > 0) sb.append("... + ");
            for (int i = from; i < SkateClient.line.size(); i++) {
                if (i > from) sb.append(" + ");
                sb.append(SkateClient.line.get(i));
            }
            String text = sb.toString();
            int tw = f.width(text);
            g.fill(w / 2 - tw / 2 - 6, 26, w / 2 + tw / 2 + 6, 52, 0x90000000);
            g.drawCenteredString(f, "§f" + text, w / 2, 30, 0xFFFFFF);
            g.drawCenteredString(f, "§e" + String.format("%,d", SkateClient.linePoints) + " §7× §b" + SkateClient.lineMult, w / 2, 41, 0xFFFFFF);
        }
        // score
        g.drawString(f, "§7SCORE §f" + String.format("%,d", SkateClient.totalScore), 8, 8, 0xFFFFFF);
        g.drawString(f, String.format("§7%.0f km/h", SkateClient.speed() * 20 * 3.6), 8, 19, 0xFFFFFF);

        // the stick: a ring with a dot, bottom right
        int cx = w - 34, cy = h - 50, r = 20;
        ring(g, cx, cy, r, 0x80FFFFFF);
        ring(g, cx, cy, r - 1, 0x40000000);
        int prep = SkateClient.prepState();
        if (prep != 0) {
            int zy = prep == 1 ? cy + r - 6 : cy - r + 6;
            g.fill(cx - 3, zy - 3, cx + 3, zy + 3, 0x90FFD040);
        }
        int dx = cx + Math.round(SkateClient.stickX() * (r - 3)), dy = cy - Math.round(SkateClient.stickY() * (r - 3));
        g.fill(dx - 3, dy - 3, dx + 3, dy + 3, prep != 0 ? 0xFFFFD040 : 0xFFFFFFFF);
        g.drawCenteredString(f, "§7flick", cx, cy + r + 4, 0xFFFFFF);

        // manual balance
        float b = SkateClient.balance();
        if (!Float.isNaN(b)) {
            int bw = 90, bx = w / 2 - bw / 2, by = h / 2 + 30;
            g.fill(bx - 1, by - 1, bx + bw + 1, by + 7, 0xA0000000);
            g.fill(bx + bw / 2 - 12, by, bx + bw / 2 + 12, by + 6, 0x6040FF40);
            int mx = bx + bw / 2 + Math.round(Mth.clamp(b, -1, 1) * bw / 2);
            g.fill(mx - 2, by - 3, mx + 2, by + 9, Math.abs(b) > 0.7f ? 0xFFFF5040 : 0xFFFFFFFF);
            g.drawCenteredString(f, "§7balance: mouse left / right", w / 2, by + 11, 0xFFFFFF);
        }
    }

    private static void ring(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int i = 0; i < 48; i++) {
            float a = i / 48f * Mth.TWO_PI;
            int x = cx + Math.round(Mth.cos(a) * r), y = cy + Math.round(Mth.sin(a) * r);
            g.fill(x, y, x + 1, y + 1, color);
        }
    }
}
