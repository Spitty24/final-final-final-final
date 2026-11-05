package dev.freerun.client.garage;

import dev.freerun.garage.Alignment;
import dev.freerun.life.LifePayloads;
import dev.freerun.vehicle.CarEntity;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/**
 * The HawkSight readings screen, laid out like a shop aligner's: the car from above with a panel
 * at each wheel. Each reading is a bar meter: the shaded band is the spec, the needle is the live
 * reading, green when it's in. Click − / + to turn that adjuster (you need a torque wrench on you).
 */
public class AlignmentScreen extends Screen {
    private static final String[] WHEELS = {"LEFT FRONT", "RIGHT FRONT", "LEFT REAR", "RIGHT REAR"};
    private final CompoundTag data;
    private int x0, y0;
    private static final int W = 360, H = 236;

    public AlignmentScreen(CompoundTag data) {
        super(Component.literal("HawkSight"));
        this.data = data;
    }

    private CarEntity car() {
        return minecraft != null && minecraft.level != null && minecraft.level.getEntity(data.getInt("Car")) instanceof CarEntity c ? c : null;
    }

    @Override
    protected void init() {
        x0 = (width - W) / 2;
        y0 = (height - H) / 2;
        for (int w = 0; w < 4; w++) {
            int px = panelX(w), py = panelY(w);
            String[] what = w < 2 ? new String[]{"camber", "caster", "toe"} : new String[]{"camber", "toe"};
            for (int k = 0; k < what.length; k++) {
                int wheel = w;
                String f = what[k];
                int by = py + 16 + k * 22;
                addRenderableWidget(Button.builder(Component.literal("−"), b -> nudge(wheel, f, -1)).bounds(px + 2, by + 8, 14, 12).build());
                addRenderableWidget(Button.builder(Component.literal("+"), b -> nudge(wheel, f, 1)).bounds(px + 110, by + 8, 14, 12).build());
            }
        }
        addRenderableWidget(Button.builder(Component.literal("Finish"), b -> {
            CompoundTag t = new CompoundTag();
            t.putLong("Pos", data.getLong("Pos"));
            t.putBoolean("Finish", true);
            ClientPlayNetworking.send(new LifePayloads.Action(LifePayloads.ALIGN, t));
            onClose();
        }).bounds(x0 + W / 2 - 40, y0 + H - 24, 80, 18).build());
    }

    private int panelX(int w) {
        return w % 2 == 0 ? x0 + 8 : x0 + W - 134;
    }

    private int panelY(int w) {
        return w < 2 ? y0 + 22 : y0 + 122;
    }

    private void nudge(int wheel, String what, int dir) {
        CompoundTag t = new CompoundTag();
        t.putLong("Pos", data.getLong("Pos"));
        t.putInt("Wheel", wheel);
        t.putString("What", what);
        t.putDouble("Delta", hasShiftDown() ? dir * 5 : dir);
        ClientPlayNetworking.send(new LifePayloads.Action(LifePayloads.ALIGN, t));
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        super.renderBackground(g, mx, my, partial);
        g.fill(x0, y0, x0 + W, y0 + H, 0xF0102238);
        g.renderOutline(x0, y0, W, H, 0xFF3A8AD8);
        g.fill(x0, y0, x0 + W, y0 + 16, 0xFF1C5FB0);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        g.drawString(font, "§lHUNTERRA §rHawkSight · Live Readings", x0 + 6, y0 + 4, 0xFFFFFFFF, false);
        CarEntity car = car();
        if (car == null) {
            g.drawCenteredString(font, "Vehicle not found", x0 + W / 2, y0 + H / 2, 0xFFFF6060);
            return;
        }
        Alignment a = car.alignment();
        // the car from above in the middle
        int cx = x0 + W / 2, cy = y0 + 112;
        g.fill(cx - 26, cy - 66, cx + 26, cy + 66, 0xFF2A4A70);
        g.renderOutline(cx - 26, cy - 66, 52, 132, 0xFF5A9AE0);
        g.fill(cx - 20, cy - 40, cx + 20, cy - 10, 0xFF1A3050);
        g.drawCenteredString(font, "▲", cx, cy - 62, 0xFF8AC0FF);
        for (int w = 0; w < 4; w++) {
            int wx = w % 2 == 0 ? cx - 32 : cx + 26, wy = w < 2 ? cy - 52 : cy + 30;
            boolean ok = wheelOk(a, w);
            g.fill(wx, wy, wx + 6, wy + 22, ok ? 0xFF30C050 : 0xFFD03030);
        }
        boolean all = a.inSpec();
        g.drawCenteredString(font, all ? "§aIN SPEC" : "§cOUT OF SPEC", cx, cy + 72, 0xFFFFFFFF);
        double pull = a.pull();
        String pullText = Math.abs(pull) < 0.02 ? "Drives straight" : "Pulls " + (pull > 0 ? "right" : "left");
        g.drawCenteredString(font, pullText, cx, cy + 84, Math.abs(pull) < 0.02 ? 0xFF90E090 : 0xFFFFB040);
        g.drawCenteredString(font, String.format("Scrub: -%d%% speed", Math.round(a.scrub() * 100)), cx, cy - 78, a.scrub() > 0 ? 0xFFFFB040 : 0xFF90A0B0);

        for (int w = 0; w < 4; w++) {
            int px = panelX(w), py = panelY(w);
            g.fill(px, py, px + 126, py + (w < 2 ? 84 : 62), 0xFF16304E);
            g.drawString(font, "§l" + WHEELS[w], px + 4, py + 4, 0xFFFFFFFF, false);
            meter(g, px, py + 16, "Camber", a.camber[w], Alignment.CAMBER, Alignment.CAMBER_TOL, 2.5);
            if (w < 2) {
                meter(g, px, py + 38, "Caster", a.caster[w], Alignment.CASTER, Alignment.CASTER_TOL, 2.5);
                meter(g, px, py + 60, "Toe", a.toe[w], Alignment.TOE, Alignment.TOE_TOL, 0.6);
            } else {
                meter(g, px, py + 38, "Toe", a.toe[w], Alignment.TOE, Alignment.TOE_TOL, 0.6);
            }
        }
        g.drawString(font, "§7Shift-click for big steps · needs a torque wrench", x0 + 6, y0 + H - 12, 0xFF90A0B0, false);
    }

    private static boolean wheelOk(Alignment a, int w) {
        boolean ok = Alignment.ok(a.camber[w], Alignment.CAMBER, Alignment.CAMBER_TOL) && Alignment.ok(a.toe[w], Alignment.TOE, Alignment.TOE_TOL);
        if (w < 2) ok &= Alignment.ok(a.caster[w], Alignment.CASTER, Alignment.CASTER_TOL);
        return ok;
    }

    /** A bar meter: spec band in the middle, needle at the reading, value printed above. */
    private void meter(GuiGraphics g, int px, int y, String label, double v, double spec, double tol, double range) {
        boolean ok = Alignment.ok(v, spec, tol);
        int bx = px + 18, bw = 90, by = y + 10;
        g.drawString(font, label, px + 18, y, 0xFFB8C8D8, false);
        String val = String.format("%+.2f°", v);
        g.drawString(font, val, px + 108 - font.width(val), y, ok ? 0xFF60F080 : 0xFFFF6060, false);
        g.fill(bx, by, bx + bw, by + 8, 0xFF0A1626);
        int s0 = bx + (int) ((tol * -1 + range) / (2 * range) * bw), s1 = bx + (int) ((tol + range) / (2 * range) * bw);
        g.fill(s0, by, s1, by + 8, 0xFF1E5A30);
        double off = Math.max(-range, Math.min(range, v - spec));
        int nx = bx + (int) ((off + range) / (2 * range) * bw);
        g.fill(nx - 1, by - 1, nx + 2, by + 9, ok ? 0xFF60F080 : 0xFFFF4040);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
