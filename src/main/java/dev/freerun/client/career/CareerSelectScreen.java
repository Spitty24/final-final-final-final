package dev.freerun.client.career;

import dev.freerun.career.Career;
import dev.freerun.career.CareerClientState;
import dev.freerun.career.Skill;
import dev.freerun.career.Skills;
import dev.freerun.net.CareerPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Pick a career: four cards, each with its tagline and a taste of its skill tree. */
public class CareerSelectScreen extends Screen {
    private static final Career[] CHOICES = {Career.EMPLOYEE, Career.SUPERVISOR, Career.SUPERINTENDENT, Career.UPPER_MANAGEMENT};
    private static final int CARD_W = 190;
    private static final int CARD_H = 92;

    private final Screen parent;
    private int confirming = -1;
    private final Button[] buttons = new Button[CHOICES.length];

    public CareerSelectScreen(Screen parent) {
        super(Component.literal("Choose your career"));
        this.parent = parent;
    }

    private int cardX(int i) {
        return width / 2 - CARD_W - 5 + (i % 2) * (CARD_W + 10);
    }

    private int cardY(int i) {
        return height / 2 - CARD_H - 12 + (i / 2) * (CARD_H + 10) + 14;
    }

    @Override
    protected void init() {
        for (int i = 0; i < CHOICES.length; i++) {
            final int idx = i;
            Career c = CHOICES[i];
            boolean current = CareerClientState.career == c.ordinal();
            boolean mastered = (CareerClientState.completed & (1 << c.ordinal())) != 0;
            buttons[i] = addRenderableWidget(Button.builder(Component.literal(
                            mastered ? "★ Mastered" : current ? "Current career" : "Become " + c.title), b -> pick(idx))
                    .bounds(cardX(i) + 8, cardY(i) + CARD_H - 24, CARD_W - 16, 18).build());
            buttons[i].active = !current && !mastered;
        }
        if (parent != null) {
            addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose())
                    .bounds(width / 2 - 40, cardY(2) + CARD_H + 6, 80, 18).build());
        }
    }

    private void pick(int i) {
        boolean switching = CareerClientState.career != Career.NONE.ordinal();
        if (switching && confirming != i) {
            confirming = i;
            for (int k = 0; k < buttons.length; k++) {
                if (buttons[k].active) buttons[k].setMessage(Component.literal(
                        k == i ? "§eClick again: unfinished skills reset" : "Become " + CHOICES[k].title));
            }
            return;
        }
        ClientPlayNetworking.send(new CareerPayloads.Choose(CHOICES[i].ordinal()));
        if (minecraft != null) minecraft.setScreen(null);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title.copy().withStyle(s -> s.withBold(true)), width / 2, cardY(0) - 34, 0xFFFFFF);
        g.drawCenteredString(font, Component.literal("Finish a tree to get promoted to the next career and keep every skill."),
                width / 2, cardY(0) - 20, 0xAAAAAA);
        for (int i = 0; i < CHOICES.length; i++) {
            Career c = CHOICES[i];
            int x = cardX(i);
            int y = cardY(i);
            g.fill(x, y, x + CARD_W, y + CARD_H, 0xD0101014);
            g.renderOutline(x, y, CARD_W, CARD_H, 0xFF000000 | c.color);
            List<Skill> skills = Skills.forCareer(c);
            if (!skills.isEmpty()) g.renderItem(new ItemStack(skills.get(skills.size() - 1).icon().get()), x + 6, y + 5);
            g.drawString(font, Component.literal(c.title).withStyle(s -> s.withBold(true)), x + 26, y + 9, c.color);
            int ty = y + 25;
            for (FormattedCharSequence line : font.split(Component.literal(c.tagline), CARD_W - 14)) {
                g.drawString(font, line, x + 7, ty, 0xDDDDDD, false);
                ty += 10;
            }
            String taste = skills.size() >= 3 ? "e.g. " + skills.get(0).name() + ", " + skills.get(2).name() : "";
            g.drawString(font, font.plainSubstrByWidth(taste, CARD_W - 14), x + 7, y + CARD_H - 36, 0x888888, false);
        }
    }
}
