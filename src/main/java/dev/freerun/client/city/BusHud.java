package dev.freerun.client.city;

import dev.freerun.city.BusEntity;
import dev.freerun.city.BusRoutes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** On a bus: the route, the next stop, and how to get off. */
public final class BusHud {
    private BusHud() {}

    public static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !(mc.player.getVehicle() instanceof BusEntity bus)) return;
        BusRoutes.Route r = bus.route();
        BusRoutes.Stop next = bus.nextStop();
        Font f = mc.font;
        int w = g.guiWidth();
        String title = "Route " + r.title();
        String line = bus.doorsOpen() ? "§a● Doors open §7— " + (next == null ? "" : next.name)
                : "§7Next stop: §f" + (next == null ? "—" : next.name);
        String hint = "§8Sneak to get off" + (bus.doorsOpen() ? " here" : " (the bus will keep going)");
        int bw = Math.max(f.width(title), Math.max(f.width(line), f.width(hint))) + 20;
        int x0 = w / 2 - bw / 2;
        g.fill(x0, 6, x0 + bw, 44, 0xC0101418);
        g.fill(x0, 6, x0 + 4, 44, 0xFF000000 | r.color);
        g.drawCenteredString(f, title, w / 2 + 2, 10, 0xFF000000 | r.color);
        g.drawCenteredString(f, line, w / 2 + 2, 21, 0xFFFFFFFF);
        g.drawCenteredString(f, hint, w / 2 + 2, 32, 0xFFFFFFFF);
    }
}
