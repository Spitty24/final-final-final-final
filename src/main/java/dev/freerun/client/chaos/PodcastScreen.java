package dev.freerun.client.chaos;

import dev.freerun.chaos.ChaosPayloads;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** You're a guest on the Creeper Cast. Pick an answer. Choose wisely. */
public class PodcastScreen extends Screen {
    final int creeperId;
    private String round;
    private List<String> q;
    private String lastReply = "";
    private boolean answered;
    private String endedAs;
    private int endTimer;
    private final List<Button> answers = new ArrayList<>();

    PodcastScreen(int creeperId, String round, List<String> q) {
        super(Component.literal("The Creeper Cast"));
        this.creeperId = creeperId;
        this.round = round;
        this.q = q;
    }

    void question(String round, List<String> q) {
        this.round = round;
        this.q = q;
        answered = false;
        rebuildWidgets();
    }

    void reply(String line) {
        lastReply = line;
    }

    void ended(String how) {
        endedAs = how;
        endTimer = how.equals("meltdown") ? 70 : 50;
        answered = true;
        rebuildWidgets();
    }

    @Override
    protected void init() {
        answers.clear();
        if (answered || q == null) return;
        int bw = Math.min(300, width - 40), y = height - 96;
        for (int i = 0; i < 3; i++) {
            final int k = i;
            Button b = Button.builder(Component.literal((char) ('A' + i) + ")  " + q.get(i + 1)), btn -> pick(k))
                    .bounds(width / 2 - bw / 2, y + i * 24, bw, 20).build();
            answers.add(addRenderableWidget(b));
        }
    }

    private void pick(int k) {
        if (answered) return;
        answered = true;
        ChaosClient.send(ChaosPayloads.PODCAST_ANSWER, creeperId, k);
        rebuildWidgets();
    }

    @Override
    public void tick() {
        if (endedAs != null && --endTimer <= 0) onClose();
    }

    @Override
    public void onClose() {
        if (!answered) {
            answered = true;
            ChaosClient.send(ChaosPayloads.PODCAST_ANSWER, creeperId, -1);   // staring silently counts against you
        }
        super.onClose();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        g.fillGradient(0, height / 2 - 30, width, height, 0x00000000, 0xD0000000);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        int top = height - 190;
        // the studio sign
        boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
        g.fill(width / 2 - 72, top, width / 2 + 72, top + 16, 0xE0101010);
        g.fill(width / 2 - 66, top + 5, width / 2 - 60, top + 11, blink ? 0xFFFF2020 : 0xFF601010);
        g.drawCenteredString(font, "§a§lTHE CREEPER CAST §r§7· REC", width / 2 + 6, top + 4, 0xFFFFFFFF);
        if (!lastReply.isEmpty()) g.drawCenteredString(font, "§a" + "Creeper: §f§o" + lastReply, width / 2, top + 24, 0xFFFFFFFF);
        if (endedAs != null) {
            String msg = endedAs.equals("meltdown") ? "§c§lthe creeper is having an emotional moment." : "§7that's a wrap.";
            g.drawCenteredString(font, msg, width / 2, top + 48, 0xFFFFFFFF);
            return;
        }
        g.drawCenteredString(font, "§7" + round, width / 2, top + 40, 0xFFFFFFFF);
        int y = top + 54;
        for (FormattedCharSequence line : font.split(Component.literal("\"" + q.get(0) + "\""), Math.min(320, width - 40))) {
            g.drawCenteredString(font, line, width / 2, y, 0xFFFFFFFF);
            y += 11;
        }
        if (answered) g.drawCenteredString(font, "§8the creeper is thinking about your answer...", width / 2, height - 30, 0xFFFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
