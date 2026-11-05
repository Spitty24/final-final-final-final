package dev.freerun.client.phone;

import dev.freerun.city.CitizenEntity;
import dev.freerun.city.StreetLampBlock;
import dev.freerun.city.TrafficLightBlock;
import dev.freerun.economy.Amenities;
import dev.freerun.economy.EconomyRegistry;
import dev.freerun.economy.Money;
import dev.freerun.gang.GangsterEntity;
import dev.freerun.mobs.RoadhouseNpcEntity;
import dev.freerun.net.PhonePayloads;
import dev.freerun.vehicle.CarEntity;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The Blackwire profiler: while you hold your phone (or always, if you turn that on in the app),
 * whatever you look at within 48 blocks gets a little readout, plus the hacks you can run on it.
 * H runs the first hack, Shift+H the second.
 */
public final class Blackwire {
    private static final double RANGE = 48.0;
    private static Entity targetEntity;
    private static BlockPos targetBlock;
    private static List<String> lines = List.of();
    private static String hack1, hack2;

    private Blackwire() {}

    public static ItemStack phone(Player p) {
        if (p.getMainHandItem().getItem() instanceof Amenities.Smartphone) return p.getMainHandItem();
        if (p.getOffhandItem().getItem() instanceof Amenities.Smartphone) return p.getOffhandItem();
        if (PhoneState.alwaysProfile) {
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                if (p.getInventory().getItem(i).getItem() instanceof Amenities.Smartphone) return p.getInventory().getItem(i);
            }
        }
        return ItemStack.EMPTY;
    }

    /** Re-aim every client tick. */
    public static void tick(Minecraft mc) {
        targetEntity = null;
        targetBlock = null;
        lines = List.of();
        hack1 = hack2 = null;
        Player p = mc.player;
        if (p == null || mc.level == null || phone(p).isEmpty()) return;
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getViewVector(1.0f);
        Vec3 end = eye.add(look.scale(RANGE));
        HitResult block = mc.level.clip(new net.minecraft.world.level.ClipContext(eye, end,
                net.minecraft.world.level.ClipContext.Block.OUTLINE, net.minecraft.world.level.ClipContext.Fluid.NONE, p));
        double reach = block.getType() == HitResult.Type.MISS ? RANGE : block.getLocation().distanceTo(eye);
        EntityHitResult ent = ProjectileUtil.getEntityHitResult(p, eye, eye.add(look.scale(reach)),
                new AABB(eye, eye.add(look.scale(reach))).inflate(1.0),
                e -> !e.isSpectator() && e != p && (e instanceof LivingEntity || e instanceof CarEntity), reach * reach);
        if (ent != null) {
            Entity e = ent.getEntity();
            if (e.getVehicle() instanceof CarEntity car && e instanceof CitizenEntity) e = car;   // aim at the driver = the car
            describe(e);
            return;
        }
        if (block instanceof BlockHitResult bh && block.getType() == HitResult.Type.BLOCK) {
            BlockState st = mc.level.getBlockState(bh.getBlockPos());
            describeBlock(bh.getBlockPos(), st);
        }
    }

    private static void describe(Entity e) {
        List<String> out = new ArrayList<>();
        if (e instanceof CitizenEntity c) {
            out.add("§b" + c.getName().getString() + "§7, " + c.age());
            out.add("§f" + c.job().title + (c.job().wage > 0 ? " §7· earns " + Money.fmt(c.job().wage) + "/day" : ""));
            out.add("§7Bank balance: §a" + Money.fmt(c.money()));
            out.add("§8" + c.fact());
            hack1 = "Transfer from account (25%)";
            hack2 = "Blow up their phone (10%)";
        } else if (e instanceof CarEntity car) {
            if (car.npcDriven() && car.getFirstPassenger() instanceof CitizenEntity driver) {
                out.add("§bVehicle §7· driven by " + driver.getName().getString());
                out.add("§7" + driver.job().title + " · " + (int) Math.round(Math.abs(car.speed()) * 72) + " km/h");
                hack1 = "Kill the engine (20%)";
                hack2 = "Override throttle (30%)";
            } else {
                out.add("§bParked vehicle §7· fuel " + Math.round(car.fuel()) + "%");
                hack1 = hack2 = "Trigger car alarm (5%)";
            }
        } else if (e instanceof GangsterEntity g) {
            out.add(g.crew().color + g.crew().member + " §7· armed");
            out.add("§8Encrypted phone. Mostly group chats.");
            hack1 = "Overload phone (30%)";
            hack2 = "Leak location to rivals (20%)";
        } else if (e instanceof RoadhouseNpcEntity n) {
            out.add("§b" + n.getName().getString());
            if (n.shop() != null) {
                out.add("§7Card reader: §aonline");
                hack1 = hack2 = "Skim the register (30%)";
            } else {
                out.add("§8Phone: nothing interesting");
            }
        } else if (e instanceof LivingEntity le) {
            out.add("§b" + le.getName().getString());
            hack1 = hack2 = "Zap (30%)";
        }
        if (out.isEmpty()) return;
        targetEntity = e;
        lines = out;
    }

    private static void describeBlock(BlockPos pos, BlockState st) {
        List<String> out = new ArrayList<>();
        if (st.getBlock() instanceof TrafficLightBlock) {
            out.add("§bTraffic signal");
            int s = st.getValue(TrafficLightBlock.SIGNAL);
            out.add(s == 0 ? "§aGreen" : s == 1 ? "§eYellow" : "§cRed");
            hack1 = "All directions green (25%)";
            hack2 = "All directions red (25%)";
        } else if (st.getBlock() instanceof StreetLampBlock) {
            out.add("§bStreet light grid");
            hack1 = hack2 = "Black out the block (30%)";
        } else if (st.is(EconomyRegistry.ATM)) {
            out.add("§bATM §7· Bayou Federal Credit Union");
            hack1 = hack2 = "Dispense cash (35%)";
        } else if (st.is(EconomyRegistry.GAS_PUMP)) {
            out.add("§bFuel pump controller");
            hack1 = hack2 = "Overload (50%)";
        } else if (st.is(EconomyRegistry.VENDING_MACHINE)) {
            out.add("§bVending machine");
            hack1 = hack2 = "Free snack (10%)";
        } else if (st.is(EconomyRegistry.TV)) {
            out.add("§bSmart TV");
            hack1 = hack2 = "Change channel (5%)";
        }
        if (out.isEmpty()) return;
        targetBlock = pos.immutable();
        lines = out;
    }

    /** H pressed. */
    public static void hack(boolean alt) {
        if (!ClientPlayNetworking.canSend(PhonePayloads.Hack.TYPE)) return;
        if (targetEntity != null) {
            ClientPlayNetworking.send(new PhonePayloads.Hack(0, targetEntity.getId(), BlockPos.ZERO, alt));
            PhoneState.log((alt ? hack2 : hack1) + " → " + targetEntity.getName().getString());
        } else if (targetBlock != null) {
            ClientPlayNetworking.send(new PhonePayloads.Hack(1, 0, targetBlock, alt));
            PhoneState.log((alt ? hack2 : hack1) + " → " + lines.get(0).substring(2));
        }
    }

    public static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.screen != null) return;
        ItemStack phone = phone(mc.player);
        if (phone.isEmpty()) return;
        int bat = (int) Amenities.Smartphone.battery(phone);
        int x = g.guiWidth() / 2 + 14, y = g.guiHeight() / 2 + 10;
        // crosshair brackets
        int col = lines.isEmpty() ? 0x6033CCDD : 0xFF33CCDD;
        g.fill(g.guiWidth() / 2 - 9, g.guiHeight() / 2 - 9, g.guiWidth() / 2 - 5, g.guiHeight() / 2 - 8, col);
        g.fill(g.guiWidth() / 2 + 5, g.guiHeight() / 2 + 8, g.guiWidth() / 2 + 9, g.guiHeight() / 2 + 9, col);
        if (lines.isEmpty()) {
            g.drawString(mc.font, "§3BLACKWIRE §8" + bat + "%", x, y, 0xFFFFFF);
            return;
        }
        int w = 0;
        for (String l : lines) w = Math.max(w, mc.font.width(l));
        String k1 = hack1 != null ? "§e[H] §f" + hack1 : null;
        String k2 = hack2 != null && !hack2.equals(hack1) ? "§e[Shift+H] §f" + hack2 : null;
        if (k1 != null) w = Math.max(w, mc.font.width(k1));
        if (k2 != null) w = Math.max(w, mc.font.width(k2));
        int hgt = lines.size() * 10 + (k1 != null ? 12 : 0) + (k2 != null ? 10 : 0) + 14;
        g.fill(x - 3, y - 3, x + w + 3, y + hgt, 0xB0081418);
        g.fill(x - 3, y - 3, x - 2, y + hgt, 0xFF33CCDD);
        g.drawString(mc.font, "§3BLACKWIRE §8· battery " + bat + "%", x, y, 0xFFFFFF);
        int yy = y + 12;
        for (String l : lines) {
            g.drawString(mc.font, l, x, yy, 0xFFFFFF);
            yy += 10;
        }
        yy += 2;
        if (k1 != null) {
            g.drawString(mc.font, k1, x, yy, 0xFFFFFF);
            yy += 10;
        }
        if (k2 != null) g.drawString(mc.font, k2, x, yy, 0xFFFFFF);
    }
}
