package dev.freerun.client.phone;

import dev.freerun.career.Career;
import dev.freerun.career.CareerClientState;
import dev.freerun.career.CareerData;
import dev.freerun.city.CityLayout;
import dev.freerun.economy.Amenities;
import dev.freerun.economy.BreakTime;
import dev.freerun.economy.Money;
import dev.freerun.economy.WalletItem;
import dev.freerun.net.PhonePayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * The smartphone: a home screen of apps, and each app's page. Click an app to open it, the
 * home bar at the bottom to go back, Esc to put the phone away.
 */
public class PhoneScreen extends Screen {
    private static final int W = 132, H = 236;
    private enum App {
        BANK("Bank", 0xFF2E8B57, "$"), MAP("Map", 0xFF3B7DD8, "◎"), BLACKWIRE("Blackwire", 0xFF0B2A33, "⌁"),
        JOBS("Jobs", 0xFFE08A1E, "✎"), CITY("City", 0xFF7A4FC9, "▦"), MESSAGES("Messages", 0xFF23A455, "✉");

        final String label;
        final int color;
        final String glyph;

        App(String label, int color, String glyph) {
            this.label = label;
            this.color = color;
            this.glyph = glyph;
        }
    }

    private static final String[] TEXTS = {
            "§fMom: §7call me when you get a chance", "§fSupervisor Slater: §7where are you",
            "§fDouble Quick Rewards: §72-for-$3 roller dogs today!", "§fEthan James Patrick: §7what am i supposed to do here",
            "§fJohnny Sim: §7who parked in front of my forklift", "§fCo-op group chat (37): §7did anyone figure out the assignment",
            "§fBayou Federal: §7direct deposit received", "§fUnknown: §7we've been trying to reach you about your car's warranty"};

    private App app;
    private int left, top;

    public PhoneScreen() {
        super(Component.literal("Phone"));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        if (ClientPlayNetworking.canSend(PhonePayloads.Request.TYPE)) ClientPlayNetworking.send(new PhonePayloads.Request(0));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(g);
    }

    private int sx() { return left + 8; }
    private int sy() { return top + 18; }
    private int sw() { return W - 16; }
    private int sh() { return H - 40; }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        // the handset
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFF0C0C0E);
        g.fill(left, top, left + W, top + H, 0xFF1C1C20);
        g.fill(sx(), sy(), sx() + sw(), sy() + sh(), app == null ? 0xFF14324A : 0xFF101418);
        if (app == null) {   // wallpaper gradient
            for (int i = 0; i < sh(); i += 4) g.fill(sx(), sy() + i, sx() + sw(), sy() + i + 4, blend(0xFF1D4E72, 0xFF2A1B3D, i / (float) sh()));
        }
        statusBar(g);
        if ((app == App.MAP || app == App.CITY) && minecraft.level.getGameTime() - lastRefresh > 40) {
            lastRefresh = minecraft.level.getGameTime();
            if (ClientPlayNetworking.canSend(PhonePayloads.Request.TYPE)) ClientPlayNetworking.send(new PhonePayloads.Request(0));
        }
        if (app == null) home(g, mouseX, mouseY);
        else {
            g.fill(sx(), sy(), sx() + sw(), sy() + 14, app.color);
            g.drawString(font, "§l" + app.label, sx() + 4, sy() + 3, 0xFFFFFF);
            switch (app) {
                case BANK -> bank(g);
                case MAP -> map(g, mouseX, mouseY);
                case BLACKWIRE -> blackwire(g, mouseX, mouseY);
                case JOBS -> jobs(g);
                case CITY -> city(g);
                case MESSAGES -> messages(g);
            }
        }
        // home bar
        int hx = left + W / 2 - 16, hy = top + H - 14;
        boolean over = mouseX >= hx && mouseX < hx + 32 && mouseY >= hy - 3 && mouseY < hy + 6;
        g.fill(hx, hy, hx + 32, hy + 3, over ? 0xFFFFFFFF : 0xFFAAAAAA);
    }

    private static int blend(int a, int b, float t) {
        int r = (int) Mth.lerp(t, (a >> 16) & 255, (b >> 16) & 255);
        int gg = (int) Mth.lerp(t, (a >> 8) & 255, (b >> 8) & 255);
        int bb = (int) Mth.lerp(t, a & 255, b & 255);
        return 0xFF000000 | r << 16 | gg << 8 | bb;
    }

    private void statusBar(GuiGraphics g) {
        long day = minecraft.level.getDayTime() % 24000L;
        int hours = (int) ((day / 1000L + 6L) % 24L);
        int minutes = (int) (day % 1000L * 60L / 1000L);
        String clock = String.format("%d:%02d", hours % 12 == 0 ? 12 : hours % 12, minutes);
        g.drawString(font, clock, left + 10, top + 6, 0xFFFFFF, false);
        int bat = (int) Amenities.Smartphone.battery(Blackwire.phone(minecraft.player).isEmpty()
                ? minecraft.player.getMainHandItem() : Blackwire.phone(minecraft.player));
        String right = (PhoneState.hasCity() ? "▮▮▮ " : "✕ ") + bat + "%";
        g.drawString(font, right, left + W - 10 - font.width(right), top + 6, 0xFFFFFF, false);
    }

    private void home(GuiGraphics g, int mx, int my) {
        App[] apps = App.values();
        for (int i = 0; i < apps.length; i++) {
            int x = sx() + 10 + (i % 3) * 34, y = sy() + 16 + (i / 3) * 46;
            boolean over = mx >= x && mx < x + 28 && my >= y && my < y + 28;
            g.fill(x, y, x + 28, y + 28, apps[i].color);
            if (over) g.fill(x, y, x + 28, y + 28, 0x30FFFFFF);
            g.drawCenteredString(font, apps[i].glyph, x + 14, y + 10, 0xFFFFFF);
            g.pose().pushPose();
            g.pose().translate(x + 14, y + 31, 0);
            g.pose().scale(0.7f, 0.7f, 1);
            g.drawCenteredString(font, apps[i].label, 0, 0, 0xFFFFFF);
            g.pose().popPose();
        }
        g.drawCenteredString(font, "§7" + Money.fmt(Money.balance(minecraft.player)), sx() + sw() / 2, sy() + sh() - 14, 0xFFFFFF);
    }

    private int line(GuiGraphics g, int y, String text) {
        for (var l : font.split(Component.literal(text), sw() - 8)) {
            g.drawString(font, l, sx() + 4, y, 0xFFFFFF, false);
            y += 10;
        }
        return y;
    }

    private void bank(GuiGraphics g) {
        int y = sy() + 20;
        g.drawString(font, "§7Bayou Federal Credit Union", sx() + 4, y, 0xFFFFFF, false);
        y += 14;
        g.pose().pushPose();
        g.pose().translate(sx() + 4, y, 0);
        g.pose().scale(1.6f, 1.6f, 1);
        g.drawString(font, "§a" + Money.fmt(Money.balance(minecraft.player)), 0, 0, 0xFFFFFF, false);
        g.pose().popPose();
        y += 22;
        boolean wallet = false;
        for (int i = 0; i < minecraft.player.getInventory().getContainerSize(); i++) {
            if (minecraft.player.getInventory().getItem(i).getItem() instanceof WalletItem) wallet = true;
        }
        y = line(g, y, wallet ? "§7Wallet: §fyes. Paychecks go straight in." : "§7No wallet. §fAny ATM will give you one free.");
        y += 4;
        y = line(g, y, "§7Next paycheck: §f" + (BreakTime.isBreak(minecraft.level) ? "paid this break" : "next break, in "
                + BreakTime.ticksUntilBreak(minecraft.level) / 1200 + " min"));
        y += 4;
        if (!PhoneState.city.getString("Lease").isEmpty()) y = line(g, y, "§7Home: §f" + PhoneState.city.getString("Lease"));
        if (!PhoneState.city.getString("Shift").isEmpty()) y = line(g, y, "§7On the clock: §f" + PhoneState.city.getString("Shift"));
        if (PhoneState.hasCity()) {
            int pi = PhoneState.city.getInt("PriceIndex");
            line(g, y, "§7Local prices: " + (pi > 100 ? "§c+" + (pi - 100) + "%" : pi < 100 ? "§a-" + (100 - pi) + "%" : "§fnormal")
                    + " §8(demand in town)");
        }
    }

    private static final int[] MAP_SPAN = {CityLayout.SIZE, 360, 130};
    private static final String[] ZOOM_NAMES = {"City", "District", "Street"};
    private long lastRefresh;

    private void map(GuiGraphics g, int mouseX, int mouseY) {
        int size = sw() - 8;
        int mx = sx() + 4, my = sy() + 18;
        CompoundTag c = PhoneState.city;
        if (!PhoneState.hasCity()) {
            int y = line(g, my, "§7No city signal here.");
            if (c.contains("NX")) {
                double dx = c.getInt("NX") - minecraft.player.getX(), dz = c.getInt("NZ") - minecraft.player.getZ();
                int dist = (int) Math.sqrt(dx * dx + dz * dz);
                y = line(g, y + 2, "§fNearest city: §e" + dist + " blocks " + dev.freerun.city.CityCommands.compass(dx, dz));
                y = line(g, y, "§7Downtown at " + c.getInt("NX") + ", " + c.getInt("NZ"));
                // a compass needle pointing the way
                int cx = sx() + sw() / 2, cy = y + 34;
                g.fill(cx - 26, cy - 26, cx + 26, cy + 26, 0xFF1A2430);
                float rel = (float) (Math.atan2(dz, dx) - Math.toRadians(minecraft.player.getYRot() + 90.0));
                for (int i = 0; i < 22; i++) {
                    int px = cx + Math.round(Mth.cos(rel) * i), py = cy + Math.round(Mth.sin(rel) * i);
                    g.fill(px - 1, py - 1, px + 1, py + 1, i > 16 ? 0xFFFF5050 : 0xFFE0E0E0);
                }
                g.drawCenteredString(font, "▲ you face up", cx, cy + 30, 0x8899AA);
            } else {
                line(g, y + 2, "§7Cities spawn out in the world; use §e/findcity§7, or found one with §e/city§7.");
            }
            return;
        }
        int zoom = Mth.clamp(PhoneState.mapZoom, 0, MAP_SPAN.length - 1);
        double ox = c.getInt("OX"), oz = c.getInt("OZ");
        double px = minecraft.player.getX() - ox, pz = minecraft.player.getZ() - oz;
        double span = MAP_SPAN[zoom];
        double vx = zoom == 0 ? CityLayout.HALF : Mth.clamp(px, span / 2, CityLayout.SIZE - span / 2);
        double vz = zoom == 0 ? CityLayout.HALF : Mth.clamp(pz, span / 2, CityLayout.SIZE - span / 2);
        double minX = vx - span / 2, minZ = vz - span / 2;
        float s = (float) (size / span);
        g.enableScissor(mx, my, mx + size, my + size);
        g.fill(mx, my, mx + size, my + size, 0xFF2E4A2E);
        int cx0 = mx + Math.round((float) (-minX) * s), cz0 = my + Math.round((float) (-minZ) * s);
        g.fill(cx0, cz0, cx0 + Math.round(CityLayout.SIZE * s), cz0 + Math.round(CityLayout.SIZE * s), 0xFF4A4A52);
        for (CityLayout.Lot l : CityLayout.lots()) {
            if (l.lx() + l.w() < minX || l.lz() + l.d() < minZ || l.lx() > minX + span || l.lz() > minZ + span) continue;
            int x0 = mx + Math.round((float) (l.lx() - minX) * s), z0 = my + Math.round((float) (l.lz() - minZ) * s);
            int x1 = mx + Math.round((float) (l.lx() + l.w() - minX) * s), z1 = my + Math.round((float) (l.lz() + l.d() - minZ) * s);
            g.fill(x0, z0, Math.max(x1, x0 + 1), Math.max(z1, z0 + 1), 0xFF000000 | l.district().color);
            if (zoom == 2 && l.district() == CityLayout.District.PARKING) {
                for (int k = 2; k < l.d(); k += 6) g.fill(x0, z0 + Math.round(k * s), x1, z0 + Math.round(k * s) + 1, 0xFF6A6A72);
            }
        }
        // the street centre lines when zoomed in
        if (zoom == 2) {
            for (int x : CityLayout.XS) {
                int sxl = mx + Math.round((float) (x + 5 - minX) * s);
                g.fill(sxl, cz0, sxl + 1, cz0 + Math.round(CityLayout.SIZE * s), 0x80D8C040);
            }
            for (int z : CityLayout.ZS) {
                int szl = my + Math.round((float) (z + 5 - minZ) * s);
                g.fill(cx0, szl, cx0 + Math.round(CityLayout.SIZE * s), szl + 1, 0x80D8C040);
            }
        }
        // labels for the landmarks
        for (CityLayout.Lot l : CityLayout.lots()) {
            CityLayout.District d = l.district();
            if (!d.landmark()) continue;
            if (zoom == 0 && l.w() < 80 && d != CityLayout.District.POLICE) continue;
            double lx = l.lx() + l.w() / 2.0, lz = l.lz() + l.d() / 2.0;
            if (lx < minX || lz < minZ || lx > minX + span || lz > minZ + span) continue;
            g.pose().pushPose();
            g.pose().translate(mx + (float) (lx - minX) * s, my + (float) (lz - minZ) * s - 2, 0);
            float scale = zoom == 2 ? 0.6f : 0.45f;
            g.pose().scale(scale, scale, 1);
            String label = d == CityLayout.District.POLICE ? "★ Police" : d.title;
            g.drawCenteredString(font, label, 0, 0, 0xFFFFFF);
            g.pose().popPose();
        }
        // who's about: police (blue), suspects (flashing red), traffic (grey), other players (green)
        boolean flash = (minecraft.level.getGameTime() / 6) % 2 == 0;
        for (net.minecraft.world.entity.Entity e : minecraft.level.entitiesForRendering()) {
            int color;
            int r = 1;
            if (e instanceof dev.freerun.city.CriminalEntity) {
                color = flash ? 0xFFFF2020 : 0xFFFFFFFF;
                r = 2;
            } else if (e instanceof dev.freerun.city.PoliceOfficerEntity) {
                color = 0xFF4080FF;
                r = 2;
            } else if (e instanceof dev.freerun.vehicle.CarEntity car && car.isPolice()) {
                color = car.sirenOn() && flash ? 0xFFFF3030 : 0xFF3060FF;
                r = 2;
            } else if (e instanceof dev.freerun.vehicle.CarEntity && zoom > 0) {
                color = 0xFFB0B0B0;
            } else if (e instanceof net.minecraft.world.entity.player.Player pl && pl != minecraft.player) {
                color = 0xFF40FF40;
                r = 2;
            } else if (e instanceof dev.freerun.city.CitizenEntity && zoom == 2) {
                color = 0xFFE0C080;
            } else {
                continue;
            }
            double ex = e.getX() - ox, ez = e.getZ() - oz;
            if (ex < minX || ez < minZ || ex > minX + span || ez > minZ + span) continue;
            int qx = mx + Math.round((float) (ex - minX) * s), qz = my + Math.round((float) (ez - minZ) * s);
            g.fill(qx - r + 1, qz - r + 1, qx + r, qz + r, color);
        }
        // you
        int dx = mx + (int) Mth.clamp((px - minX) * s, 0, size - 1), dz = my + (int) Mth.clamp((pz - minZ) * s, 0, size - 1);
        g.fill(dx - 2, dz - 2, dx + 2, dz + 2, 0xFFFFFFFF);
        float yaw = minecraft.player.getYRot() * Mth.DEG_TO_RAD;
        int ax = dx + Math.round(-Mth.sin(yaw) * 5), az = dz + Math.round(Mth.cos(yaw) * 5);
        g.fill(ax - 1, az - 1, ax + 1, az + 1, 0xFFFF4040);
        g.disableScissor();
        // zoom buttons
        int bx = mx + size - 22, by = my + 2;
        g.fill(bx, by, bx + 10, by + 10, 0xC0000000);
        g.fill(bx + 11, by, bx + 21, by + 10, 0xC0000000);
        g.drawCenteredString(font, "-", bx + 5, by + 1, 0xFFFFFF);
        g.drawCenteredString(font, "+", bx + 16, by + 1, 0xFFFFFF);
        int y = my + size + 3;
        CityLayout.District here = CityLayout.districtAt(px, pz);
        boolean inside = px >= 0 && pz >= 0 && px < CityLayout.SIZE && pz < CityLayout.SIZE;
        y = line(g, y, inside ? "§fYou're in: §e" + (here != null ? here.title : CityLayout.onRoad(px, pz) ? "the street" : "an alley")
                : "§7You're outside the city limits.");
        line(g, y, "§8" + ZOOM_NAMES[zoom] + " view · scroll or +/- to zoom");
    }

    private void blackwire(GuiGraphics g, int mx, int my) {
        int y = sy() + 20;
        int bat = (int) Amenities.Smartphone.battery(Blackwire.phone(minecraft.player).isEmpty()
                ? minecraft.player.getMainHandItem() : Blackwire.phone(minecraft.player));
        g.fill(sx() + 4, y, sx() + sw() - 4, y + 6, 0xFF203038);
        g.fill(sx() + 4, y, sx() + 4 + (sw() - 8) * bat / 100, y + 6, 0xFF33CCDD);
        y += 10;
        // toggle
        int bx = sx() + 4, by = y;
        boolean over = mx >= bx && mx < sx() + sw() - 4 && my >= by && my < by + 14;
        g.fill(bx, by, sx() + sw() - 4, by + 14, over ? 0xFF2E5560 : 0xFF1C3840);
        g.drawString(font, "Profiler: " + (PhoneState.alwaysProfile ? "§aalways on" : "§ewhen held"), bx + 4, by + 3, 0xFFFFFF, false);
        y += 20;
        y = line(g, y, "§7Look at something, then §eH§7 to hack, §eShift+H§7 for the other option.");
        y += 2;
        for (String s : List.of("§bPeople§7: drain account · ring phone", "§bCars§7: kill engine · floor it",
                "§bLights§7: all green · all red", "§bStreet lamps§7: blackout", "§bATMs§7: cash out",
                "§bClerks§7: skim register", "§bGangs§7: overload · leak", "§bGas pumps§7: overload")) {
            y = line(g, y, s);
        }
    }

    private void jobs(GuiGraphics g) {
        int y = sy() + 20;
        Career c = Career.byId(CareerClientState.career);
        if (c == Career.NONE) {
            line(g, y, "§7No career yet. Press §eP§7 to pick one.");
            return;
        }
        y = line(g, y, "§f" + c.title + " §7· level " + CareerClientState.level);
        int need = CareerData.xpToNext(CareerClientState.level);
        g.fill(sx() + 4, y, sx() + sw() - 4, y + 4, 0xFF333333);
        g.fill(sx() + 4, y, sx() + 4 + (sw() - 8) * Math.min(need, CareerClientState.xp) / Math.max(1, need), y + 4, 0xFF000000 | c.color);
        y += 10;
        int stars = Integer.bitCount(CareerClientState.completed);
        int pay = dev.freerun.career.Skills.paycheck(CareerClientState.data(), 15 + 3 * CareerClientState.level + 10 * stars);
        y = line(g, y, "§7Paycheck: §a" + Money.fmt(pay) + " §7each break");
        int until = BreakTime.ticksUntilBreak(minecraft.level);
        y = line(g, y, BreakTime.isBreak(minecraft.level) ? "§aOn break now!"
                : "§7Next break in §f" + until / 1200 + "m " + (until % 1200) / 20 + "s");
        y += 4;
        line(g, y, "§8Press P for your skill tree.");
    }

    private void city(GuiGraphics g) {
        int y = sy() + 20;
        if (!PhoneState.hasCity()) {
            line(g, y, "§7No city signal nearby.");
            return;
        }
        CompoundTag c = PhoneState.city;
        y = line(g, y, "§fPeople out and about: §b" + c.getInt("Population"));
        y = line(g, y, "§fEmployed: §b" + c.getInt("Employed"));
        y = line(g, y, "§fAvg. savings: §a" + Money.fmt(c.getInt("AvgMoney")));
        y = line(g, y, "§fCars on the road: §b" + c.getInt("Cars"));
        y += 4;
        y = line(g, y, "§7Today");
        y = line(g, y, "§fWages paid: §a" + Money.fmt(c.getInt("Wages")));
        y = line(g, y, "§fStore sales: §a" + Money.fmt(c.getInt("Sales")));
        y = line(g, y, "§fRent collected: §a" + Money.fmt(c.getInt("Rent")));
        y = line(g, y, "§fYesterday's sales: §a" + Money.fmt(c.getInt("SalesYesterday")));
        y += 4;
        int pi = c.getInt("PriceIndex");
        y = line(g, y, "§fPrice index: " + (pi > 100 ? "§c" : pi < 100 ? "§a" : "§f") + pi + "%");
        y += 4;
        y = line(g, y, "§7Public safety");
        y = line(g, y, "§fOfficers nearby: §9" + c.getInt("Police"));
        y = line(g, y, "§fCrimes reported: §c" + c.getInt("Crimes"));
        line(g, y, "§fStopped: §a" + c.getInt("Stopped") + " §7(" + c.getInt("Arrests") + " by police)");
    }

    private void messages(GuiGraphics g) {
        int y = sy() + 20;
        long seed = minecraft.level.getDayTime() / 3000L;
        for (int i = 0; i < 3; i++) y = line(g, y, TEXTS[(int) Math.floorMod(seed * 7 + i * 3, TEXTS.length)]) + 3;
        y += 2;
        y = line(g, y, "§3Blackwire log");
        int n = 0;
        for (String l : PhoneState.LOG) {
            if (y > sy() + sh() - 12 || n++ > 8) break;
            y = line(g, y, "§7" + l);
        }
        if (PhoneState.LOG.isEmpty()) line(g, y, "§8(nothing yet)");
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (app == App.MAP && scrollY != 0) {
            PhoneState.mapZoom = Mth.clamp(PhoneState.mapZoom + (scrollY > 0 ? 1 : -1), 0, MAP_SPAN.length - 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int hx = left + W / 2 - 16, hy = top + H - 14;
        if (mouseX >= hx && mouseX < hx + 32 && mouseY >= hy - 3 && mouseY < hy + 6) {
            app = null;
            return true;
        }
        if (app == null) {
            App[] apps = App.values();
            for (int i = 0; i < apps.length; i++) {
                int x = sx() + 10 + (i % 3) * 34, y = sy() + 16 + (i / 3) * 46;
                if (mouseX >= x && mouseX < x + 28 && mouseY >= y && mouseY < y + 28) {
                    app = apps[i];
                    minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                            net.minecraft.sounds.SoundEvents.NOTE_BLOCK_HAT.value(), 1.8f));
                    if (ClientPlayNetworking.canSend(PhonePayloads.Request.TYPE)) ClientPlayNetworking.send(new PhonePayloads.Request(0));
                    return true;
                }
            }
        } else if (app == App.MAP && PhoneState.hasCity()) {
            int size = sw() - 8, bx = sx() + 4 + size - 22, by = sy() + 18 + 2;
            if (mouseY >= by && mouseY < by + 10) {
                if (mouseX >= bx && mouseX < bx + 10) {
                    PhoneState.mapZoom = Math.max(0, PhoneState.mapZoom - 1);
                    return true;
                }
                if (mouseX >= bx + 11 && mouseX < bx + 21) {
                    PhoneState.mapZoom = Math.min(MAP_SPAN.length - 1, PhoneState.mapZoom + 1);
                    return true;
                }
            }
        } else if (app == App.BLACKWIRE) {
            int by = sy() + 30;
            if (mouseX >= sx() + 4 && mouseX < sx() + sw() - 4 && mouseY >= by && mouseY < by + 14) {
                PhoneState.alwaysProfile = !PhoneState.alwaysProfile;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
