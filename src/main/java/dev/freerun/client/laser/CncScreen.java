package dev.freerun.client.laser;

import dev.freerun.laser.LaserCutterBlockEntity;
import dev.freerun.laser.LaserShape;
import dev.freerun.life.LifePayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The CNC terminal: draw the part on a 32 x 32 sheet (pen, eraser, line, box, circle, fill, or a
 * preset), pick oxygen or nitrogen assist gas, and press CUT. Your last drawing is kept.
 */
public class CncScreen extends Screen {
    private static final int N = LaserShape.N, CELL = 6, GRID = N * CELL;
    private static final int PEN = 0, ERASE = 1, LINE = 2, BOX = 3, CIRCLE = 4, FILL = 5;
    private static final String[] TOOLS = {"Pen", "Eraser", "Line", "Box", "Circle", "Fill"};
    private static final String[] PRESETS = {"Bracket", "Washer", "Gear", "Star", "Gusset", "Clear"};

    private static long[] drawing = new long[LaserShape.LONGS];
    private static int gas = LaserCutterBlockEntity.NITROGEN;
    private static int tool = PEN;

    private final CompoundTag data;
    private final Deque<long[]> undo = new ArrayDeque<>();
    private int gx, gy, px;
    private int dragX = -1, dragY = -1, curX, curY;
    private boolean dragging, erasing;
    private Button cutButton;

    public CncScreen(CompoundTag data) {
        super(Component.literal("CNC Terminal"));
        this.data = data;
    }

    @Override
    protected void init() {
        int w = GRID + 150, h = GRID + 40;
        gx = (width - w) / 2 + 8;
        gy = (height - h) / 2 + 28;
        px = gx + GRID + 12;
        for (int i = 0; i < TOOLS.length; i++) {
            int t = i;
            addRenderableWidget(Button.builder(Component.literal(TOOLS[i]), b -> tool = t)
                    .bounds(px + (i % 2) * 64, gy + (i / 2) * 18, 62, 16).build());
        }
        for (int i = 0; i < PRESETS.length; i++) {
            int k = i;
            addRenderableWidget(Button.builder(Component.literal(PRESETS[i]), b -> preset(k))
                    .bounds(px + (i % 2) * 64, gy + 62 + (i / 2) * 18, 62, 16).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Undo"), b -> {
            if (!undo.isEmpty()) drawing = undo.pop();
        }).bounds(px, gy + 120, 62, 16).build());
        addRenderableWidget(Button.builder(gasLabel(), b -> {
            gas = 1 - gas;
            b.setMessage(gasLabel());
        }).bounds(px + 64, gy + 120, 62, 16).build());
        cutButton = addRenderableWidget(Button.builder(Component.literal("§l§cCUT"), b -> cut())
                .bounds(px, gy + GRID - 20, 126, 20).build());
    }

    private Component gasLabel() {
        return Component.literal(gas == LaserCutterBlockEntity.OXYGEN ? "Gas: O₂" : "Gas: N₂");
    }

    private boolean busy() {
        if (!data.getBoolean("Busy") || minecraft == null || minecraft.level == null) return false;
        return minecraft.level.getGameTime() < data.getLong("Start") + data.getInt("Duration");
    }

    private void cut() {
        if (busy() || LaserShape.empty(drawing)) return;
        CompoundTag t = new CompoundTag();
        t.putLong("Cutter", data.getLong("Cutter"));
        t.putLongArray("Shape", drawing);
        t.putInt("Gas", gas);
        ClientPlayNetworking.send(new LifePayloads.Action(LifePayloads.CNC, t));
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.8f));
        onClose();
    }

    // ------------------------------------------------------------------ drawing

    private void push() {
        undo.push(LaserShape.copy(drawing));
        while (undo.size() > 30) undo.removeLast();
    }

    private int cellX(double mx) {
        return (int) Math.floor((mx - gx) / CELL);
    }

    private int cellY(double my) {
        return (int) Math.floor((my - gy) / CELL);
    }

    private boolean inGrid(double mx, double my) {
        return mx >= gx && my >= gy && mx < gx + GRID && my < gy + GRID;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (inGrid(mx, my)) {
            int x = cellX(mx), y = cellY(my);
            push();
            erasing = button == 1 || tool == ERASE;
            if (tool == FILL) {
                flood(x, y, !erasing);
                return true;
            }
            dragging = true;
            dragX = curX = x;
            dragY = curY = y;
            if (tool == PEN || tool == ERASE) LaserShape.set(drawing, x, y, !erasing);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging) {
            int x = Math.max(0, Math.min(N - 1, cellX(mx))), y = Math.max(0, Math.min(N - 1, cellY(my)));
            if (tool == PEN || tool == ERASE) line(drawing, curX, curY, x, y, !erasing);
            curX = x;
            curY = y;
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging) {
            dragging = false;
            if (tool == LINE || tool == BOX || tool == CIRCLE) shapeInto(drawing, tool, dragX, dragY, curX, curY, !erasing);
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    private static void line(long[] s, int x0, int y0, int x1, int y1, boolean on) {
        int dx = Math.abs(x1 - x0), dy = -Math.abs(y1 - y0), sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1, err = dx + dy;
        while (true) {
            LaserShape.set(s, x0, y0, on);
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 >= dy) {
                err += dy;
                x0 += sx;
            }
            if (e2 <= dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    private static void shapeInto(long[] s, int tool, int x0, int y0, int x1, int y1, boolean on) {
        switch (tool) {
            case LINE -> line(s, x0, y0, x1, y1, on);
            case BOX -> {
                for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
                    for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) LaserShape.set(s, x, y, on);
            }
            case CIRCLE -> {
                double cx = (x0 + x1) / 2.0, cy = (y0 + y1) / 2.0;
                double rx = Math.abs(x1 - x0) / 2.0 + 0.5, ry = Math.abs(y1 - y0) / 2.0 + 0.5;
                for (int y = 0; y < N; y++) for (int x = 0; x < N; x++) {
                    double u = (x - cx) / rx, v = (y - cy) / ry;
                    if (u * u + v * v <= 1.0) LaserShape.set(s, x, y, on);
                }
            }
            default -> {}
        }
    }

    private void flood(int x, int y, boolean on) {
        if (LaserShape.get(drawing, x, y) == on) return;
        Deque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{x, y});
        while (!q.isEmpty()) {
            int[] c = q.poll();
            if (c[0] < 0 || c[1] < 0 || c[0] >= N || c[1] >= N || LaserShape.get(drawing, c[0], c[1]) == on) continue;
            LaserShape.set(drawing, c[0], c[1], on);
            q.add(new int[]{c[0] + 1, c[1]});
            q.add(new int[]{c[0] - 1, c[1]});
            q.add(new int[]{c[0], c[1] + 1});
            q.add(new int[]{c[0], c[1] - 1});
        }
    }

    private void preset(int k) {
        push();
        long[] s = new long[LaserShape.LONGS];
        switch (k) {
            case 0 -> {   // an L bracket with two bolt holes
                shapeInto(s, BOX, 4, 4, 11, 27, true);
                shapeInto(s, BOX, 4, 20, 27, 27, true);
                shapeInto(s, CIRCLE, 6, 7, 9, 10, false);
                shapeInto(s, CIRCLE, 21, 22, 24, 25, false);
            }
            case 1 -> {   // a washer
                shapeInto(s, CIRCLE, 3, 3, 28, 28, true);
                shapeInto(s, CIRCLE, 11, 11, 20, 20, false);
            }
            case 2 -> {   // a gear: a disc, teeth, a bore
                shapeInto(s, CIRCLE, 6, 6, 25, 25, true);
                for (int i = 0; i < 12; i++) {
                    double a = i * Math.PI / 6;
                    int cx = (int) Math.round(15.5 + Math.cos(a) * 12.5), cy = (int) Math.round(15.5 + Math.sin(a) * 12.5);
                    shapeInto(s, BOX, cx - 1, cy - 1, cx + 1, cy + 1, true);
                }
                shapeInto(s, CIRCLE, 12, 12, 19, 19, false);
            }
            case 3 -> {   // a five-pointed star
                for (int y = 0; y < N; y++) for (int x = 0; x < N; x++) {
                    double dx = x - 15.5, dy = y - 16.5, r = Math.hypot(dx, dy), a = Math.atan2(dy, dx) + Math.PI / 2;
                    double f = Math.abs(((a / (2 * Math.PI / 5)) % 1 + 1) % 1 - 0.5) * 2;   // 0 at a point, 1 between
                    if (r <= 14.5 - 8.0 * f) LaserShape.set(s, x, y, true);
                }
            }
            case 4 -> {   // a triangular gusset plate with a lightening hole
                for (int y = 4; y <= 27; y++) for (int x = 4; x <= 4 + (y - 4); x++) LaserShape.set(s, x, y, true);
                shapeInto(s, CIRCLE, 8, 17, 13, 22, false);
            }
            default -> {}
        }
        drawing = s;
    }

    // ------------------------------------------------------------------ drawing the screen

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        int x0 = gx - 8, y0 = gy - 28;
        g.drawString(font, "§lTRUMF §rTruCut 3030 fiber  ·  CNC", x0 + 8, y0 + 8, 0x7FD3FF, false);
        boolean creative = data.getBoolean("Creative");
        int sheets = data.getInt("Sheets");
        g.drawString(font, creative ? "Sheets: ∞" : "Sheets: " + sheets, px, y0 + 8, sheets > 0 || creative ? 0xC8C8C8 : 0xFF6060, false);
        // the sheet
        g.fill(gx - 1, gy - 1, gx + GRID + 1, gy + GRID + 1, 0xFF0A0A0A);
        long[] show = drawing;
        if (dragging && (tool == LINE || tool == BOX || tool == CIRCLE)) {
            show = LaserShape.copy(drawing);
            shapeInto(show, tool, dragX, dragY, curX, curY, !erasing);
        }
        for (int y = 0; y < N; y++) for (int x = 0; x < N; x++) {
            boolean on = LaserShape.get(show, x, y);
            int c = on ? (gas == LaserCutterBlockEntity.NITROGEN ? 0xFFD8DEE6 : 0xFFA8A090) : ((x + y) % 2 == 0 ? 0xFF3A3F46 : 0xFF353A40);
            g.fill(gx + x * CELL, gy + y * CELL, gx + x * CELL + CELL, gy + y * CELL + CELL, c);
            if (on) {   // the cut line round the outline
                int edge = 0xFFFF6A1A;
                if (!LaserShape.get(show, x - 1, y)) g.fill(gx + x * CELL, gy + y * CELL, gx + x * CELL + 1, gy + y * CELL + CELL, edge);
                if (!LaserShape.get(show, x + 1, y)) g.fill(gx + x * CELL + CELL - 1, gy + y * CELL, gx + x * CELL + CELL, gy + y * CELL + CELL, edge);
                if (!LaserShape.get(show, x, y - 1)) g.fill(gx + x * CELL, gy + y * CELL, gx + x * CELL + CELL, gy + y * CELL + 1, edge);
                if (!LaserShape.get(show, x, y + 1)) g.fill(gx + x * CELL, gy + y * CELL + CELL - 1, gx + x * CELL + CELL, gy + y * CELL + CELL, edge);
            }
        }
        if (inGrid(mx, my)) {
            int cx = cellX(mx), cy = cellY(my);
            g.renderOutline(gx + cx * CELL, gy + cy * CELL, CELL, CELL, 0xFFFFFF00);
        }
        // job info
        int iy = gy + 142;
        int cells = LaserShape.count(drawing), per = LaserShape.perimeter(drawing), pierce = LaserShape.pierces(drawing);
        g.drawString(font, "Tool: " + TOOLS[tool], px, iy, 0xFFFFFF, false);
        g.drawString(font, "Cut length: " + per * 3 + " mm", px, iy + 11, 0xC8C8C8, false);
        g.drawString(font, "Pierces: " + pierce, px, iy + 22, 0xC8C8C8, false);
        g.drawString(font, "Est. time: " + (cells == 0 ? 0 : LaserCutterBlockEntity.cutTicks(drawing, gas) / 20) + " s", px, iy + 33, 0xC8C8C8, false);
        g.drawString(font, gas == LaserCutterBlockEntity.OXYGEN ? "O₂: fast, dark edge" : "N₂: bright, weld-ready", px, iy + 44,
                gas == LaserCutterBlockEntity.OXYGEN ? 0xFFB040 : 0x7FD3FF, false);
        if (busy()) {
            double t = (minecraft.level.getGameTime() + partial - data.getLong("Start")) / Math.max(1, data.getInt("Duration"));
            g.fill(px, gy + GRID - 34, px + 126, gy + GRID - 26, 0xFF202020);
            g.fill(px, gy + GRID - 34, px + (int) (126 * Math.min(1, t)), gy + GRID - 26, 0xFFFF6A1A);
            g.drawString(font, "Cutting...", px, gy + GRID - 46, 0xFF6A1A, false);
        }
        cutButton.active = !busy() && cells > 0 && (creative || sheets > 0);
        g.drawString(font, "§7Left: draw  Right: erase", gx, gy + GRID + 4, 0x909090, false);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        super.renderBackground(g, mx, my, partial);
        int x0 = gx - 8, y0 = gy - 28;
        g.fill(x0, y0, x0 + GRID + 150, y0 + GRID + 52, 0xF01C2128);
        g.renderOutline(x0, y0, GRID + 150, GRID + 52, 0xFF3A7AB0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
