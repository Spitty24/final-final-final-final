package dev.freerun.client.alien;

import dev.freerun.alien.Abilities;
import dev.freerun.alien.Form;
import dev.freerun.alien.Transformations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** While transformed: the creature's name, the time left, and the ability and its cooldown. */
public final class AlienHud {
    private AlienHud() {}

    public static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        Form f = Transformations.form(mc.player);
        if (!f.transformed()) return;
        var font = mc.font;
        long now = mc.level.getGameTime();
        int cx = g.guiWidth() / 2, y = 4;
        String name = f.alien().title.toUpperCase();
        int w = 140;
        g.fill(cx - w / 2 - 4, y, cx + w / 2 + 4, y + 30, 0xA0081816);
        g.drawCenteredString(font, "§b§l" + name, cx, y + 4, 0xFFFFFFFF);
        if (f.phase() == Form.ACTIVE) {
            float left = (f.timeoutAt() - now) / (float) Form.DURATION;
            int color = left < 0.12f && (now / 5) % 2 == 0 ? 0xFFFF4040 : left < 0.12f ? 0xFFB02020 : 0xFF40E8C8;
            g.fill(cx - w / 2, y + 16, cx + w / 2, y + 20, 0xFF102A28);
            g.fill(cx - w / 2, y + 16, cx - w / 2 + (int) (w * Math.max(0, left)), y + 20, color);
            int cd = Abilities.cooldownLeft(mc.player, now);
            String ab = f.alien().activeName() + (cd > 0 ? " §7" + (cd / 20 + 1) + "s" : " §a[" + keyName() + "]");
            g.drawCenteredString(font, "§e" + ab, cx, y + 22, 0xFFFFFFFF);
        } else {
            g.drawCenteredString(font, f.phase() == Form.MORPH_IN ? "§7transforming..." : "§7turning back...", cx, y + 18, 0xFFFFFFFF);
        }
    }

    private static String keyName() {
        return dev.freerun.client.FreerunClient.abilityKey == null ? "G" : dev.freerun.client.FreerunClient.abilityKey.getTranslatedKeyMessage().getString();
    }
}
