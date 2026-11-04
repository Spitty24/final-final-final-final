package dev.freerun.client.economy;

import dev.freerun.economy.BreakTime;
import dev.freerun.economy.Money;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Your cash (bottom-left, above the hotbar line) and, during a break, how long is left. */
public final class CashHud {
    private CashHud() {}

    public static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.level == null) return;
        int bal = Money.balance(mc.player);
        int y = g.guiHeight() - 12;
        if (bal > 0) {
            g.drawString(mc.font, "§a" + Money.fmt(bal), 4, y, 0xFFFFFF);
            y -= 10;
        }
        if (mc.player.getVehicle() instanceof dev.freerun.vehicle.CarEntity car) {
            int kmh = (int) Math.round(Math.abs(car.renderSpeed()) * 20.0 * 3.6);
            float fuel = car.fuel();
            String gauge = (fuel < 15 ? "§c" : fuel < 35 ? "§e" : "§a") + "⛽ " + Math.round(fuel) + "%";
            String s = "§f" + kmh + " km/h   " + gauge;
            g.drawString(mc.font, s, (g.guiWidth() - mc.font.width(s)) / 2, g.guiHeight() - 62, 0xFFFFFF);
            if (fuel <= 0) {
                String out = "§cOut of gas. Push it to a pump, or use a gas can.";
                g.drawString(mc.font, out, (g.guiWidth() - mc.font.width(out)) / 2, g.guiHeight() - 72, 0xFFFFFF);
            }
        }
        if (BreakTime.isBreak(mc.level)) {
            int left = BreakTime.breakTicksLeft(mc.level) / 20;
            g.drawString(mc.font, "§e☕ Break: " + left / 60 + ":" + String.format("%02d", left % 60), 4, y, 0xFFFFFF);
        }
    }
}
