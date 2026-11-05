package dev.freerun.client.laser;

import dev.freerun.laser.Dustin;
import dev.freerun.laser.WorkPhoneItem;
import dev.freerun.life.LifePayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

/**
 * The work phone. One app, and it's Teams: your chat with Dustin, with a few quick replies
 * (you can't type on a company phone, IT locked the keyboard).
 */
public class WorkPhoneScreen extends Screen {
    private static final int W = 190, H = 320;
    private static final int PURPLE = 0xFF4B53BC, PURPLE_DARK = 0xFF33398A;
    private int x0, y0;
    private boolean inChat;
    private double scroll;

    public WorkPhoneScreen() {
        super(Component.literal("Work Phone"));
    }

    @Override
    protected void init() {
        x0 = (width - W) / 2;
        y0 = (height - H) / 2;
    }

    private CompoundTag phone() {
        if (minecraft == null || minecraft.player == null) return new CompoundTag();
        ItemStack s = WorkPhoneItem.find(minecraft.player);
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        return d == null ? new CompoundTag() : d.copyTag();
    }

    private void open() {
        inChat = true;
        scroll = 0;
        CompoundTag t = new CompoundTag();
        t.putBoolean("Read", true);
        ClientPlayNetworking.send(new LifePayloads.Action(LifePayloads.TEAMS, t));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int sx = x0 + 10, sy = y0 + 24, sw = W - 20;
        if (!inChat) {
            if (mx >= sx && mx < sx + sw && my >= sy + 44 && my < sy + 76) {
                open();
                return true;
            }
        } else {
            if (mx >= sx && mx < sx + 22 && my >= sy && my < sy + 22) {   // back
                inChat = false;
                return true;
            }
            int ry = y0 + H - 70;
            int bx = sx + 4, by = ry;
            for (int i = 0; i < Dustin.REPLIES.length; i++) {
                int bw = font.width(Dustin.REPLIES[i]) + 10;
                if (bx + bw > sx + sw - 4) {
                    bx = sx + 4;
                    by += 15;
                }
                if (mx >= bx && mx < bx + bw && my >= by && my < by + 13) {
                    CompoundTag t = new CompoundTag();
                    t.putInt("Reply", i);
                    ClientPlayNetworking.send(new LifePayloads.Action(LifePayloads.TEAMS, t));
                    scroll = 0;
                    return true;
                }
                bx += bw + 4;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (inChat) scroll = Math.max(0, scroll + sy * 12);
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        // the handset
        g.fill(x0, y0, x0 + W, y0 + H, 0xFF101114);
        g.renderOutline(x0, y0, W, H, 0xFF3A3D44);
        int sx = x0 + 10, sy = y0 + 24, sw = W - 20, sh = H - 44;
        g.fill(sx, sy, sx + sw, sy + sh, 0xFFF5F5F7);
        g.drawString(font, "WORK", x0 + 10, y0 + 8, 0xFF8A8DFF, false);
        long now = minecraft.level == null ? 0 : minecraft.level.getGameTime();
        int clock = (int) ((minecraft.level == null ? 0 : minecraft.level.getDayTime() + 6000) % 24000);
        g.drawString(font, String.format("%d:%02d", clock / 1000 % 12 == 0 ? 12 : clock / 1000 % 12, clock % 1000 * 60 / 1000), x0 + W - 40, y0 + 8, 0xFFCCCCCC, false);
        CompoundTag t = phone();
        ListTag chat = t.getList("Chat", Tag.TAG_COMPOUND);
        int unread = t.getInt("Unread");

        // Teams header bar
        g.fill(sx, sy, sx + sw, sy + 22, PURPLE);
        if (!inChat) {
            g.drawString(font, "§lTeams", sx + 8, sy + 7, 0xFFFFFFFF, false);
            g.drawString(font, "Chat", sx + 8, sy + 30, 0xFF616161, false);
            // the one conversation
            boolean hover = mx >= sx && mx < sx + sw && my >= sy + 44 && my < sy + 76;
            g.fill(sx, sy + 44, sx + sw, sy + 76, hover ? 0xFFE8E8F4 : 0xFFF5F5F7);
            avatar(g, sx + 6, sy + 48);
            g.drawString(font, (unread > 0 ? "§l" : "") + Dustin.NAME, sx + 32, sy + 49, 0xFF242424, false);
            String last = chat.isEmpty() ? "No messages yet. Enjoy it." : chat.getCompound(chat.size() - 1).getString("Text");
            g.drawString(font, font.plainSubstrByWidth(last, sw - 56), sx + 32, sy + 61, 0xFF616161, false);
            if (unread > 0) {
                g.fill(sx + sw - 18, sy + 54, sx + sw - 6, sy + 66, 0xFFC4314B);
                g.drawCenteredString(font, String.valueOf(Math.min(9, unread)), sx + sw - 12, sy + 56, 0xFFFFFFFF);
            }
            g.drawString(font, "Teams is the only app.", sx + 8, sy + sh - 26, 0xFFA0A0A0, false);
            g.drawString(font, "IT has locked this device.", sx + 8, sy + 14 + sh - 28, 0xFFA0A0A0, false);
            return;
        }
        g.drawString(font, "<", sx + 7, sy + 7, 0xFFFFFFFF, false);
        avatar(g, sx + 20, sy + 2);
        g.drawString(font, "§l" + Dustin.NAME, sx + 44, sy + 3, 0xFFFFFFFF, false);
        g.drawString(font, "Busy · In a meeting", sx + 44, sy + 12, 0xFFD8D8F8, false);

        // messages, newest at the bottom
        int top = sy + 26, bottom = y0 + H - 76;
        g.enableScissor(sx, top, sx + sw, bottom);
        int y = bottom - 4 + (int) scroll;
        for (int i = chat.size() - 1; i >= 0 && y > top - 400; i--) {
            CompoundTag m = chat.getCompound(i);
            boolean his = m.getBoolean("Dustin");
            List<FormattedCharSequence> lines = font.split(Component.literal(m.getString("Text")), sw - 52);
            int bh = lines.size() * 10 + 6;
            int bw = 0;
            for (FormattedCharSequence l : lines) bw = Math.max(bw, font.width(l));
            bw += 10;
            y -= bh + 12;
            int bx = his ? sx + 28 : sx + sw - 6 - bw;
            if (his) avatar(g, sx + 4, y + 10);
            g.fill(bx, y + 10, bx + bw, y + 10 + bh, his ? 0xFFFFFFFF : 0xFFE8EBFA);
            g.renderOutline(bx, y + 10, bw, bh, his ? 0xFFE0E0E0 : 0xFFD0D4F2);
            long ago = Math.max(0, (now - m.getLong("Time")) / 1200);
            g.drawString(font, (his ? Dustin.NAME + "  " : "") + (ago == 0 ? "now" : ago + "m ago"), his ? bx : bx + bw - font.width(ago == 0 ? "now" : ago + "m ago"), y, 0xFF8A8A8A, false);
            int ly = y + 13;
            for (FormattedCharSequence l : lines) {
                g.drawString(font, l, bx + 5, ly, 0xFF242424, false);
                ly += 10;
            }
        }
        g.disableScissor();
        if (chat.isEmpty()) g.drawCenteredString(font, "Say hi to your manager!", sx + sw / 2, top + 40, 0xFF8A8A8A);

        // quick replies (the keyboard is disabled by IT)
        g.fill(sx, y0 + H - 76, sx + sw, sy + sh, 0xFFEDEDF2);
        int bx = sx + 4, by = y0 + H - 70;
        for (String r : Dustin.REPLIES) {
            int bw = font.width(r) + 10;
            if (bx + bw > sx + sw - 4) {
                bx = sx + 4;
                by += 15;
            }
            boolean hover = mx >= bx && mx < bx + bw && my >= by && my < by + 13;
            g.fill(bx, by, bx + bw, by + 13, hover ? PURPLE_DARK : PURPLE);
            g.drawString(font, r, bx + 5, by + 3, 0xFFFFFFFF, false);
            bx += bw + 4;
        }
    }

    private void avatar(GuiGraphics g, int x, int y) {
        g.fill(x, y + 2, x + 20, y + 16, 0xFFB04A2A);
        g.fill(x + 2, y, x + 18, y + 18, 0xFFB04A2A);
        g.drawCenteredString(font, "D", x + 10, y + 5, 0xFFFFFFFF);
        g.fill(x + 15, y + 13, x + 20, y + 18, 0xFFC4314B);   // red: busy
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** The pop-up when Dustin messages you. */
    public static final class Ping implements net.minecraft.client.gui.components.toasts.Toast {
        private final String text;

        public Ping(String text) {
            this.text = text;
        }

        @Override
        public Visibility render(GuiGraphics g, net.minecraft.client.gui.components.toasts.ToastComponent toasts, long time) {
            var font = Minecraft.getInstance().font;
            g.fill(0, 0, width(), height(), 0xF0FFFFFF);
            g.fill(0, 0, 3, height(), PURPLE);
            g.fill(6, 7, 24, 25, 0xFFB04A2A);
            g.drawCenteredString(font, "D", 15, 12, 0xFFFFFFFF);
            g.drawString(font, "§lDustin §r§7· Teams", 30, 5, 0xFF242424, false);
            g.drawString(font, font.plainSubstrByWidth(text, width() - 36), 30, 17, 0xFF424242, false);
            return time >= 6000L ? Visibility.HIDE : Visibility.SHOW;
        }

        @Override
        public int width() {
            return 200;
        }
    }

    public static void ping(CompoundTag data) {
        Minecraft mc = Minecraft.getInstance();
        mc.getToasts().addToast(new Ping(data.getString("Text")));
        if (mc.player != null) mc.player.playSound(dev.freerun.laser.LaserRegistry.TEAMS_PING, 0.9f, 1.0f);
    }

}
