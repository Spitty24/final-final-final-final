package dev.freerun.client.life;

import dev.freerun.economy.Money;
import dev.freerun.life.LifePayloads;
import dev.freerun.life.OfficeJobs;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/** The job board in an office lobby: pick a job and clock in, or clock out. */
public class OfficeJobScreen extends Screen {
    private static final int W = 260, H = 196;
    private final CompoundTag data;
    private int left, top;

    public OfficeJobScreen(CompoundTag data) {
        super(Component.literal("Job Board"));
        this.data = data;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        int current = data.getInt("Job");
        OfficeJobs.Job[] jobs = OfficeJobs.Job.values();
        for (int i = 0; i < jobs.length; i++) {
            OfficeJobs.Job j = jobs[i];
            int idx = i;
            Button b = Button.builder(Component.literal((i == current ? "§a✔ " : "") + j.title), btn -> send(idx))
                    .bounds(left + 8, top + 26 + i * 28, 100, 20).build();
            b.active = i != current;
            addRenderableWidget(b);
        }
        if (current >= 0) {
            addRenderableWidget(Button.builder(Component.literal("§cClock out"), btn -> send(-1)).bounds(left + 8, top + H - 26, 100, 20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Close"), btn -> onClose()).bounds(left + W - 68, top + H - 26, 60, 20).build());
    }

    private void send(int job) {
        CompoundTag t = new CompoundTag();
        t.putInt("Job", job);
        if (ClientPlayNetworking.canSend(LifePayloads.Action.TYPE)) ClientPlayNetworking.send(new LifePayloads.Action(LifePayloads.BOARD, t));
        onClose();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(g);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFF5A3A1E);
        g.fill(left, top, left + W, top + H, 0xFFC9A26B);   // cork
        for (int i = 0; i < 40; i++) {
            int x = left + (i * 53) % W, y = top + (i * 37) % H;
            g.fill(x, y, x + 1, y + 1, 0xFFAD8550);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, "§l📌 NOW HIRING", left + W / 2, top + 8, 0x3A2410);
        int bonus = data.getInt("Bonus");
        OfficeJobs.Job[] jobs = OfficeJobs.Job.values();
        for (int i = 0; i < jobs.length; i++) {
            int y = top + 26 + i * 28;
            g.fill(left + 114, y - 1, left + W - 8, y + 23, 0xFFF4EEDC);
            g.drawString(font, "§2" + Money.fmt(jobs[i].pay + bonus) + " §8a job", left + 118, y + 1, 0x222222, false);
            g.drawWordWrap(font, Component.literal("§8" + jobs[i].blurb), left + 118, y + 11, W - 130, 0x444444);
        }
        if (data.getInt("Job") >= 0) {
            g.drawString(font, "§0This shift: " + data.getInt("Done") + " jobs, " + Money.fmt(data.getInt("Earned")),
                    left + 114, top + H - 20, 0x222222, false);
        } else {
            g.drawString(font, "§0Pay rises with your career level.", left + 114, top + H - 20, 0x222222, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
