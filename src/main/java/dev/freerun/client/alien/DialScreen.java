package dev.freerun.client.alien;

import com.mojang.blaze3d.platform.Lighting;
import dev.freerun.Freerun;
import dev.freerun.alien.Alien;
import dev.freerun.alien.AlienPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/**
 * The Swampwatch dial: ten creatures round a copper ring, the chosen one turning in the middle
 * with its powers listed. Scroll or use the arrow keys to turn the dial, click or press Enter
 * (or the ability key) to become it.
 */
public class DialScreen extends Screen {
    private static int selected;
    private float spin;
    private float dialAngle, dialTarget;
    private int ticks;

    public DialScreen() {
        super(Component.literal("Swampwatch"));
    }

    @Override
    protected void init() {
        dialAngle = dialTarget = -selected * 36f;
    }

    private void turn(int by) {
        selected = Math.floorMod(selected + by, Alien.values().length);
        dialTarget -= by * 36f;
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_HAT.value(), 1.6f, 0.8f));
    }

    private void pick() {
        ClientPlayNetworking.send(new AlienPayloads.Action(AlienPayloads.SELECT, selected));
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL.value(), 1.3f, 1.0f));
        onClose();
    }

    @Override
    public void tick() {
        ticks++;
    }

    private int cx() {
        return width / 2;
    }

    private int cy() {
        return height / 2 + 6;
    }

    private static final int RADIUS = 92;

    private int[] slot(int i, float partial) {
        float ang = (dialAngle + (dialTarget - dialAngle) * 0.0f + i * 36f) * Mth.DEG_TO_RAD - Mth.HALF_PI;
        return new int[]{cx() + (int) (Mth.cos(ang) * RADIUS), cy() + (int) (Mth.sin(ang) * RADIUS)};
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        turn(sy > 0 ? -1 : 1);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_D) {
            turn(1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_A) {
            turn(-1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_G) {
            pick();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (int i = 0; i < Alien.values().length; i++) {
            int[] s = slot(i, 0);
            if (Math.abs(mx - s[0]) < 18 && Math.abs(my - s[1]) < 18) {
                if (i == selected) pick();
                else {
                    int d = Math.floorMod(i - selected, 10);
                    turn(d <= 5 ? d : d - 10);
                }
                return true;
            }
        }
        if (Math.abs(mx - cx()) < 40 && Math.abs(my - cy()) < 50) {
            pick();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        dialAngle += (dialTarget - dialAngle) * 0.25f;
        spin += 1.2f;
        int cx = cx(), cy = cy();
        Alien a = Alien.values()[selected];
        // the copper ring and the teal face
        ring(g, cx, cy, RADIUS + 26, RADIUS + 20, 0xFFC87A3A);
        ring(g, cx, cy, RADIUS + 20, RADIUS - 24, 0xE0103C3A);
        ring(g, cx, cy, RADIUS - 24, RADIUS - 28, 0xFF2AA890);
        ring(g, cx, cy, 58, 0, 0xF0082826);
        // the creature in the middle
        g.pose().pushPose();
        g.pose().translate(cx, cy + 46, 100);
        float s = 48f / Math.max(1.2f, a.height);
        g.pose().scale(s, s, -s);
        g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180));
        g.pose().mulPose(com.mojang.math.Axis.XP.rotationDegrees(-8));
        Lighting.setupForEntityInInventory();
        AlienRenderers.renderPreview(a, g.pose(), g.bufferSource(), spin, ticks + partial);
        g.flush();
        Lighting.setupFor3DItems();
        g.pose().popPose();
        // the ten stops on the dial
        for (int i = 0; i < Alien.values().length; i++) {
            int[] p = slot(i, partial);
            boolean sel = i == selected;
            Alien ai = Alien.values()[i];
            g.fill(p[0] - 17, p[1] - 17, p[0] + 17, p[1] + 17, sel ? 0xFF40E8C8 : 0xFF1A4A46);
            g.fill(p[0] - 15, p[1] - 15, p[0] + 15, p[1] + 15, 0xFF0A1E1C);
            g.blit(ResourceLocation.fromNamespaceAndPath(Freerun.MOD_ID, "textures/gui/alien/" + ai.id() + ".png"), p[0] - 14, p[1] - 14, 0, 0, 28, 28, 28, 28);
        }
        // the name plate and the powers
        g.drawCenteredString(font, "§l" + a.title.toUpperCase(), cx, cy - 52, 0xFF40E8C8);
        g.drawCenteredString(font, a.tagline, cx, cy - 41, 0xFFB0D8D0);
        int y = height - 54;
        g.fill(cx - 170, y - 6, cx + 170, height - 6, 0xC0081816);
        g.drawString(font, "§a● §f" + a.passive1, cx - 164, y, 0xFFFFFFFF, false);
        g.drawString(font, "§a● §f" + a.passive2, cx - 164, y + 11, 0xFFFFFFFF, false);
        g.drawString(font, "§e★ §f" + a.active, cx - 164, y + 22, 0xFFFFFFFF, false);
        g.drawString(font, "§7Scroll / arrows to turn the dial · Enter or click to transform · 90s a go", cx - 164, y + 36, 0xFF90A8A4, false);
        g.drawCenteredString(font, "§6§lSWAMPWATCH", cx, 10, 0xFFFFFFFF);
    }

    private static void ring(GuiGraphics g, int cx, int cy, int r1, int r0, int color) {
        // drawn as stacked horizontal spans
        for (int dy = -r1; dy <= r1; dy++) {
            int outer = (int) Math.sqrt(r1 * r1 - dy * dy);
            int inner = Math.abs(dy) < r0 ? (int) Math.sqrt(r0 * r0 - dy * dy) : 0;
            if (inner > 0) {
                g.fill(cx - outer, cy + dy, cx - inner, cy + dy + 1, color);
                g.fill(cx + inner, cy + dy, cx + outer, cy + dy + 1, color);
            } else {
                g.fill(cx - outer, cy + dy, cx + outer, cy + dy + 1, color);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
