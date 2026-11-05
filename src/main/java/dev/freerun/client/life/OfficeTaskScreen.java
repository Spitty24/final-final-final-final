package dev.freerun.client.life;

import dev.freerun.life.LifePayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** A work task on screen: a ledger, a coworker's request, a phone call. Pick an answer. */
public class OfficeTaskScreen extends Screen {
    private static final int W = 240, H = 200;
    private final CompoundTag data;
    private int left, top;
    private final List<FormattedCharSequence> lines = new ArrayList<>();
    private boolean answered;

    public OfficeTaskScreen(CompoundTag data) {
        super(Component.literal(data.getString("Title")));
        this.data = data;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        lines.clear();
        for (String para : data.getString("Prompt").split("\n", -1)) {
            if (para.isEmpty()) lines.add(FormattedCharSequence.EMPTY);
            else lines.addAll(font.split(Component.literal(para), W - 20));
        }
        ListTag opts = data.getList("Options", Tag.TAG_STRING);
        int n = opts.size();
        int bw = n > 2 ? (W - 24) / 2 : (W - 24 - 8) / Math.max(1, n);
        for (int i = 0; i < n; i++) {
            int idx = i;
            int col = n > 2 ? i % 2 : i, row = n > 2 ? i / 2 : 0;
            int x = left + 10 + col * (bw + 4), y = top + H - (n > 2 ? 52 : 30) + row * 24;
            addRenderableWidget(Button.builder(Component.literal(opts.getString(i)), b -> answer(idx)).bounds(x, y, bw, 20).build());
        }
    }

    private void answer(int option) {
        if (answered) return;
        answered = true;
        CompoundTag t = new CompoundTag();
        t.putInt("Id", data.getInt("Id"));
        t.putInt("Option", option);
        if (ClientPlayNetworking.canSend(LifePayloads.Action.TYPE)) ClientPlayNetworking.send(new LifePayloads.Action(LifePayloads.TASK, t));
        onClose();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(g);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFF1E2A3A);
        g.fill(left, top, left + W, top + H, 0xFFF2F4F7);
        g.fill(left, top, left + W, top + 16, 0xFF2B5797);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawString(font, "§l" + data.getString("Title"), left + 6, top + 4, 0xFFFFFF);
        int y = top + 24;
        for (FormattedCharSequence l : lines) {
            g.drawString(font, l, left + 10, y, 0x1A1A1A, false);
            y += 10;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
