package dev.freerun.client.career;

import dev.freerun.career.Career;
import dev.freerun.career.CareerClientState;
import dev.freerun.career.CareerData;
import dev.freerun.career.Skill;
import dev.freerun.career.Skills;
import dev.freerun.client.FreerunClient;
import dev.freerun.net.CareerPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The skill tree (P). Five tiers left to right; click a glowing node to spend a skill point.
 *   gold border   unlocked
 *   green border  available now
 *   gray          locked (level too low, or no skill in the tier before it yet)
 */
public class SkillTreeScreen extends Screen {
    private static final int TIERS = Skill.TIER_LEVEL.length;
    private static final int COL_W = 80;
    private static final int GAP = 10;
    private static final int NODE_H = 40;
    private static final int PANEL_W = TIERS * COL_W + (TIERS - 1) * GAP + 20;
    private static final int PANEL_H = 240;

    private record Node(Skill skill, int x, int y) {}

    private final List<Node> nodes = new ArrayList<>();
    private int px, py;
    /** Which tree is shown: your current career, or one you've mastered (read-only). */
    private Career viewing;
    private Button promote;
    private boolean confirmPromote;

    public SkillTreeScreen() {
        super(Component.literal("Skill Tree"));
    }

    @Override
    protected void init() {
        if (CareerClientState.career == Career.NONE.ordinal()) {
            if (minecraft != null) minecraft.setScreen(new CareerSelectScreen(null));
            return;
        }
        px = (width - PANEL_W) / 2;
        py = (height - PANEL_H) / 2 + 8;
        nodes.clear();
        CareerData data = CareerClientState.data();
        if (viewing == null || (viewing != data.careerType() && !data.isCompleted(viewing))) viewing = data.careerType();
        Career career = viewing;

        // Tabs for your current career and every career you've mastered
        int tabX = px;
        for (Career c : Career.values()) {
            if (c == Career.NONE || (c != data.careerType() && !data.isCompleted(c))) continue;
            String label = (data.isCompleted(c) ? "★ " : "") + c.title;
            int w = font.width(label) + 14;
            Button tab = addRenderableWidget(Button.builder(Component.literal(label), b -> {
                viewing = c;
                rebuildWidgets();
            }).bounds(tabX, py - 20, w, 18).build());
            tab.active = c != viewing;
            tabX += w + 4;
        }
        for (int tier = 1; tier <= TIERS; tier++) {
            final int t = tier;
            List<Skill> inTier = Skills.forCareer(career).stream().filter(s -> s.tier() == t).toList();
            int colX = px + 10 + (tier - 1) * (COL_W + GAP);
            int top = py + 62;
            for (int i = 0; i < inTier.size(); i++) {
                int y = top + (3 - inTier.size()) * (NODE_H + 4) / 2 + i * (NODE_H + 4);
                nodes.add(new Node(inTier.get(i), colX, y));
            }
        }
        addRenderableWidget(Button.builder(Component.literal("Change Career"),
                b -> minecraft.setScreen(new CareerSelectScreen(this))).bounds(px + 10, py + PANEL_H - 26, 100, 18).build());
        Career next = data.nextCareer();
        promote = addRenderableWidget(Button.builder(Component.literal(promoteLabel(data)), b -> {
            if (!confirmPromote) {
                confirmPromote = true;
                b.setMessage(Component.literal("§eClick again: start " + next.title + " at Lv 1"));
                return;
            }
            ClientPlayNetworking.send(new CareerPayloads.Prestige(0));
            onClose();
        }).bounds(px + PANEL_W / 2 - 95, py + PANEL_H - 26, 190, 18).build());
        promote.active = data.canPrestige();
        promote.visible = data.careerType() == viewing;
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(px + PANEL_W - 90, py + PANEL_H - 26, 80, 18).build());
    }

    private static String promoteLabel(CareerData d) {
        if (d.nextCareer() == Career.NONE) return d.treeComplete() ? "★ Top of the ladder ★" : "Finish your tree to get promoted";
        return d.canPrestige() ? "★ Get Promoted → " + d.nextCareer().title : "Finish your tree to get promoted";
    }

    private enum State { OWNED, AVAILABLE, LOCKED }

    private State state(Skill s, CareerData d) {
        if (d.has(s.id())) return State.OWNED;
        if (s.career() != d.careerType()) return State.LOCKED;
        boolean prereq = s.tier() == 1 || Skills.forCareer(s.career()).stream()
                .anyMatch(o -> o.tier() == s.tier() - 1 && d.has(o.id()));
        return prereq && d.level() >= s.requiredLevel() && d.points() > 0 ? State.AVAILABLE : State.LOCKED;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        CareerData d = CareerClientState.data();
        Career career = viewing != null ? viewing : d.careerType();
        boolean mastered = career != d.careerType();

        g.fill(px, py, px + PANEL_W, py + PANEL_H, 0xE40E0E12);
        g.renderOutline(px, py, PANEL_W, PANEL_H, 0xFF000000 | career.color);

        // Header: career, prestige stars, level, XP bar, points
        g.drawString(font, Component.literal(career.title).withStyle(s -> s.withBold(true)), px + 10, py + 9, career.color);
        if (d.prestigeCount() > 0) {
            g.drawString(font, "★".repeat(d.prestigeCount()), px + 16 + font.width(career.title) + 4, py + 9, 0xFFD040);
        }
        if (mastered) {
            g.drawString(font, "Mastered: all of these stay with you", px + 10, py + 23, 0xE0B030, false);
        } else {
        String lvl = mastered ? "" : "Level " + d.level() + (d.level() >= CareerData.MAX_LEVEL ? " (max)" : "");
        g.drawString(font, lvl, px + PANEL_W - 10 - font.width(lvl), py + 9, 0xFFFFFF);
        int barX = px + 10, barY = py + 23, barW = PANEL_W - 20;
        int need = CareerData.xpToNext(d.level());
        float frac = d.level() >= CareerData.MAX_LEVEL ? 1.0f : Math.min(1.0f, d.xp() / (float) need);
        g.fill(barX, barY, barX + barW, barY + 5, 0xFF2A2A30);
        g.fill(barX, barY, barX + (int) (barW * frac), barY + 5, 0xFF000000 | career.color);
        String xpText = d.level() >= CareerData.MAX_LEVEL ? "Max level" : d.xp() + " / " + need + " XP";
        g.drawString(font, xpText, barX, barY + 8, 0xAAAAAA, false);
        String pts = d.points() + (d.points() == 1 ? " skill point" : " skill points") + " to spend";
        g.drawString(font, pts, px + PANEL_W - 10 - font.width(pts), barY + 8, d.points() > 0 ? 0x55FF55 : 0x888888, false);
        }

        // Tier headings
        for (int tier = 1; tier <= TIERS; tier++) {
            int colX = px + 10 + (tier - 1) * (COL_W + GAP);
            String h = "Tier " + tier + " · Lv " + Skill.TIER_LEVEL[tier - 1];
            g.drawString(font, h, colX + (COL_W - font.width(h)) / 2, py + 54, d.level() >= Skill.TIER_LEVEL[tier - 1] ? 0xCCCCCC : 0x666666, false);
        }

        // Connectors from each node to every node in the next tier
        for (Node a : nodes) {
            for (Node b : nodes) {
                if (b.skill().tier() != a.skill().tier() + 1) continue;
                boolean lit = d.has(a.skill().id()) && d.has(b.skill().id());
                int col = lit ? 0xFFE0B030 : (d.has(a.skill().id()) ? 0xFF4A7A4A : 0xFF3A3A40);
                int ax = a.x() + COL_W, ay = a.y() + NODE_H / 2;
                int bx = b.x(), by = b.y() + NODE_H / 2;
                int mx = ax + GAP / 2;
                g.fill(ax, ay - 1, mx + 1, ay + 1, col);
                g.fill(mx - 1, Math.min(ay, by) - 1, mx + 1, Math.max(ay, by) + 1, col);
                g.fill(mx - 1, by - 1, bx, by + 1, col);
            }
        }

        // Nodes
        Node hovered = null;
        for (Node n : nodes) {
            State st = state(n.skill(), d);
            int bg = st == State.OWNED ? 0xFF3A2E10 : st == State.AVAILABLE ? 0xFF12301A : 0xFF1A1A1E;
            int border = st == State.OWNED ? 0xFFE0B030 : st == State.AVAILABLE ? 0xFF55DD66 : 0xFF4A4A52;
            boolean over = mouseX >= n.x() && mouseX < n.x() + COL_W && mouseY >= n.y() && mouseY < n.y() + NODE_H;
            if (over) {
                hovered = n;
                bg += 0x00101010;
            }
            g.fill(n.x(), n.y(), n.x() + COL_W, n.y() + NODE_H, bg);
            g.renderOutline(n.x(), n.y(), COL_W, NODE_H, border);
            g.renderItem(new ItemStack(n.skill().icon().get()), n.x() + 4, n.y() + 4);
            int ty = n.y() + 5;
            List<FormattedCharSequence> lines = font.split(Component.literal(n.skill().name()), COL_W - 26);
            for (int i = 0; i < Math.min(2, lines.size()); i++) {
                g.drawString(font, lines.get(i), n.x() + 23, ty, st == State.LOCKED ? 0x888888 : 0xFFFFFF, false);
                ty += 10;
            }
            String tag = st == State.OWNED ? (mastered ? "Mastered" : "Unlocked") : st == State.AVAILABLE ? "Click to unlock"
                    : d.level() < n.skill().requiredLevel() ? "Needs Lv " + n.skill().requiredLevel() : "Locked";
            g.drawString(font, tag, n.x() + 5, n.y() + NODE_H - 11,
                    st == State.OWNED ? 0xE0B030 : st == State.AVAILABLE ? 0x55DD66 : 0x777777, false);
        }

        g.drawCenteredString(font, Component.literal("§7Earn XP by doing the job: pressing, welding, crane & forklift work, mining, fighting, clocking in."),
                width / 2, py + PANEL_H + 6, 0xAAAAAA);

        if (hovered != null) {
            Skill s = hovered.skill();
            List<Component> tip = new ArrayList<>();
            tip.add(Component.literal(s.name()).withStyle(st -> st.withColor(career.color).withBold(true)));
            tip.add(Component.literal("§f" + s.description()));
            tip.add(Component.literal("§7Tier " + s.tier() + " · requires level " + s.requiredLevel()
                    + (s.tier() > 1 ? " and a tier " + (s.tier() - 1) + " skill" : "")));
            State st = state(s, d);
            tip.add(Component.literal(st == State.OWNED ? "§6Unlocked" : st == State.AVAILABLE ? "§aClick to unlock (1 point)" : "§8Locked"));
            g.renderComponentTooltip(font, tip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            CareerData d = CareerClientState.data();
            for (Node n : nodes) {
                if (mouseX >= n.x() && mouseX < n.x() + COL_W && mouseY >= n.y() && mouseY < n.y() + NODE_H) {
                    if (state(n.skill(), d) == State.AVAILABLE) {
                        ClientPlayNetworking.send(new CareerPayloads.Unlock(n.skill().id()));
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (FreerunClient.skillsKeyMatches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
