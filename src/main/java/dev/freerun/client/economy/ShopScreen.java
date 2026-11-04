package dev.freerun.client.economy;

import dev.freerun.economy.Money;
import dev.freerun.economy.Shops;
import dev.freerun.net.ShopPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * A store's shelves: a grid of everything for sale with prices (or, on the Sell tab, what the store
 * will pay you for). Click to buy one; shift-click to buy five. Your balance is in the corner.
 */
public class ShopScreen extends Screen {
    private static final int COLS = 8;
    private static final int CELL = 26;
    private static final int ROWS = 6;

    private final int clerkId;
    private final Shops.Shop shop;
    private boolean selling;
    private int scroll;
    private int left, top, w, h;

    public ShopScreen(int clerkId, Shops.Shop shop) {
        super(Component.literal(shop.title));
        this.clerkId = clerkId;
        this.shop = shop;
    }

    private List<Shops.Offer> list() {
        return selling ? Shops.sells(shop) : Shops.buys(shop);
    }

    @Override
    protected void init() {
        w = COLS * CELL + 16;
        h = ROWS * CELL + 58;
        left = (width - w) / 2;
        top = (height - h) / 2;
        addRenderableWidget(Button.builder(Component.literal("Buy"), b -> { selling = false; scroll = 0; })
                .bounds(left + 6, top + 18, 50, 16).build());
        addRenderableWidget(Button.builder(Component.literal("Sell"), b -> { selling = true; scroll = 0; })
                .bounds(left + 60, top + 18, 50, 16).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        g.fill(left, top, left + w, top + h, 0xE0101014);
        g.fill(left, top, left + w, top + 14, 0xFF000000 | shop.color);
        g.drawString(font, "§l" + shop.title, left + 6, top + 3, 0xFFFFFF);
        int bal = minecraft != null && minecraft.player != null ? Money.balance(minecraft.player) : 0;
        String b = "Cash: " + Money.fmt(bal);
        g.drawString(font, b, left + w - 6 - font.width(b), top + 3, 0x7CFF7C);
        g.drawString(font, selling ? "§7What they'll pay you" : "§7Click: buy 1 · Shift-click: buy 5", left + 116, top + 22, 0xAAAAAA);
        for (var r : renderables) r.render(g, mouseX, mouseY, partialTick);

        List<Shops.Offer> list = list();
        int gx = left + 8, gy = top + 40;
        Shops.Offer hovered = null;
        for (int i = 0; i < COLS * ROWS; i++) {
            int idx = i + scroll * COLS;
            if (idx >= list.size()) break;
            Shops.Offer o = list.get(idx);
            int x = gx + (i % COLS) * CELL, y = gy + (i / COLS) * CELL;
            boolean hover = mouseX >= x && mouseX < x + CELL - 2 && mouseY >= y && mouseY < y + CELL - 2;
            boolean afford = selling || bal >= o.price();
            g.fill(x, y, x + CELL - 2, y + CELL - 2, hover ? 0xFF3C3C48 : 0xFF24242C);
            g.renderItem(o.stack(), x + 4, y + 1);
            g.renderItemDecorations(font, o.stack(), x + 4, y + 1);
            String p = "$" + o.price();
            g.pose().pushPose();
            g.pose().translate(0, 0, 200);
            g.pose().scale(0.75f, 0.75f, 1.0f);
            g.drawString(font, p, (int) ((x + CELL - 3) / 0.75f) - font.width(p), (int) ((y + 17) / 0.75f),
                    afford ? 0x7CFF7C : 0xFF6060);
            g.pose().popPose();
            if (hover) hovered = o;
        }
        int totalRows = (list.size() + COLS - 1) / COLS;
        if (totalRows > ROWS) {
            g.drawString(font, "§8scroll for more (" + (scroll + 1) + "/" + (totalRows - ROWS + 1) + ")", left + 8, top + h - 12, 0x888888);
        }
        if (hovered != null) {
            List<Component> tip = new java.util.ArrayList<>(getTooltipFromItem(minecraft, hovered.stack()));
            tip.add(Component.literal(selling
                    ? "§eSell " + hovered.stack().getCount() + " for §a" + Money.fmt(hovered.price())
                    : "§ePrice: §a" + Money.fmt(hovered.price())));
            g.renderComponentTooltip(font, tip, mouseX, mouseY);
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(g);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<Shops.Offer> list = list();
        int gx = left + 8, gy = top + 40;
        for (int i = 0; i < COLS * ROWS; i++) {
            int idx = i + scroll * COLS;
            if (idx >= list.size()) break;
            int x = gx + (i % COLS) * CELL, y = gy + (i / COLS) * CELL;
            if (mouseX >= x && mouseX < x + CELL - 2 && mouseY >= y && mouseY < y + CELL - 2) {
                int times = hasShiftDown() ? 5 : 1;
                if (ClientPlayNetworking.canSend(ShopPayloads.Trade.TYPE)) {
                    ClientPlayNetworking.send(new ShopPayloads.Trade(clerkId, idx, selling, times));
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int totalRows = (list().size() + COLS - 1) / COLS;
        int max = Math.max(0, totalRows - ROWS);
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(scrollY)));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
