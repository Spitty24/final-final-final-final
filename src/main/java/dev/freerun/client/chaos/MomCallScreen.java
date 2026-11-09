package dev.freerun.client.chaos;

import dev.freerun.chaos.ChaosPayloads;
import dev.freerun.chaos.ChaosRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/** The screen goes black. A phone rings. It's Mom. */
public class MomCallScreen extends Screen {
    private final String name;
    private int age;
    private boolean answered, decided;
    private int talk;
    private final List<String> lines = new ArrayList<>();
    private ChaosClient.Loop ring;

    MomCallScreen(String name) {
        super(Component.literal("Incoming call"));
        this.name = name;
        lines.add("STEVE.");
        if (!name.equalsIgnoreCase("steve")) lines.add("...I know, I know, online you're \"" + name + "\". You'll always be Steve to me.");
        lines.add("Dinner's ready. It's meatloaf.");
        lines.add("Come home. Not in five minutes. NOW.");
        lines.add("Your bed is where you left it. That means home.");
        lines.add("And wear a helmet. Love you. Okay bye. Bye. Okay. Bye.");
    }

    @Override
    protected void init() {
        clearWidgets();
        if (answered) {
            if (talk >= lines.size() * 50) {
                addRenderableWidget(Button.builder(Component.literal("Okay Mom. Hang up."), b -> onClose())
                        .bounds(width / 2 - 70, height / 2 + 80, 140, 20).build());
            }
            return;
        }
        addRenderableWidget(Button.builder(Component.literal("§a✆ Answer"), b -> answer(true)).bounds(width / 2 - 104, height / 2 + 80, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("§c✕ Ignore"), b -> answer(false)).bounds(width / 2 + 4, height / 2 + 80, 100, 20).build());
    }

    private void answer(boolean yes) {
        if (decided) return;
        decided = true;
        ChaosClient.send(ChaosPayloads.MOM_ANSWER, 0, yes ? 1 : 0);
        if (ring != null) ring.kill();
        if (!yes) {
            onClose();
            return;
        }
        answered = true;
        rebuildWidgets();
    }

    @Override
    public void tick() {
        age++;
        if (ring == null && !answered) {
            ring = new ChaosClient.Loop(ChaosRegistry.PHONE_RING, 1.0f, () -> !decided);
            Minecraft.getInstance().getSoundManager().play(ring);
        }
        if (!decided && age > 20 * 20) answer(false);
        if (answered) {
            int before = talk;
            talk++;
            if (before < lines.size() * 50 && talk >= lines.size() * 50) rebuildWidgets();
        }
    }

    @Override
    public void onClose() {
        if (!decided) answer(false);
        if (ring != null) ring.kill();
        super.onClose();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        int a = (int) (Mth.clamp((age + pt) / 15f, 0, 1) * 255);
        g.fill(0, 0, width, height, (a << 24));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        int cx = width / 2, cy = height / 2;
        // the phone
        float shake = answered ? 0 : (age % 40 < 20 ? Mth.sin((age + pt) * 2.2f) * 2 : 0);
        int px = cx - 60 + (int) shake, py = cy - 100;
        g.fill(px - 2, py - 2, px + 122, py + 172, 0xFF2A2A30);
        g.fill(px, py, px + 120, py + 170, 0xFF0D1018);
        g.fill(px + 45, py + 6, px + 75, py + 9, 0xFF333844);
        if (!answered) {
            g.drawCenteredString(font, "§7incoming call", cx, py + 30, 0xFFFFFFFF);
            g.fill(cx - 22, py + 46, cx + 22, py + 90, 0xFFE07AA8);
            g.drawCenteredString(font, "§lM", cx, py + 63, 0xFFFFFFFF);
            g.drawCenteredString(font, "§f§lMom ♥", cx, py + 100, 0xFFFFFFFF);
            g.drawCenteredString(font, "§8mobile", cx, py + 112, 0xFFFFFFFF);
            if ((age / 10) % 2 == 0) g.drawCenteredString(font, "§a♪ ring ring ♪", cx, py + 140, 0xFFFFFFFF);
            return;
        }
        int secs = talk / 20;
        g.drawCenteredString(font, "§f§lMom ♥ §r§7" + String.format("%d:%02d", secs / 60, secs % 60), cx, py + 14, 0xFFFFFFFF);
        int shown = Math.min(lines.size(), talk / 50 + 1);
        int y = cy + 12 - shown * 12;
        int w = Math.min(300, width - 30);
        y = Math.max(py + 30, y);
        for (int i = Math.max(0, shown - 4); i < shown; i++) {
            String line = lines.get(i);
            int chars = i == shown - 1 ? Math.min(line.length(), (talk % 50) * 2 + 1) : line.length();
            if (i == shown - 1 && talk >= lines.size() * 50) chars = line.length();
            for (FormattedCharSequence s : font.split(Component.literal("§d" + line.substring(0, chars)), w)) {
                g.drawCenteredString(font, s, cx, y, 0xFFFFFFFF);
                y += 11;
            }
            y += 4;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !answered || talk >= lines.size() * 50;
    }
}
