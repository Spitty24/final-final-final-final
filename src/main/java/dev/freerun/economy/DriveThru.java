package dev.freerun.economy;

import dev.freerun.mobs.RoadhouseNpcEntity;
import dev.freerun.vehicle.CarEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Drive-thrus at Big Bun Burgers and the Cluck Shack. Pull up to the menu board in a car and
 * right-click it to order (the crew inside answers over the speaker), pay, then drive around to
 * the pickup window, where your bag is handed through as soon as you stop beside it.
 */
public final class DriveThru {
    /** How far the crew can be from the menu board and still take your order. */
    public static final double REACH = 28.0;
    private static final long FORGOTTEN = 20 * 60 * 6;

    private record Order(Shops.Shop shop, BlockPos window, List<ItemStack> items, long placed) {}

    private static final Map<UUID, Order> ORDERS = new HashMap<>();
    private static final String[] GREETINGS = {
            "*crackle* Welcome to %s, can I take your order?",
            "*bzzt* Hi, welcome to %s! Whenever you're ready.",
            "*crackle* ...sorry, go ahead. Welcome to %s.",
    };
    private static final String[] HANDOFFS = {
            "Here you go! Sauce is in the bag. Have a good one!",
            "Careful, it's hot. Thanks for coming to %s!",
            "One bag, extra napkins. Drive safe!",
            "Your order's complete! We threw in a little lagniappe.",
    };

    private DriveThru() {}

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(level -> {
            if (level.getGameTime() % 5 != 0 || ORDERS.isEmpty()) return;
            tick(level);
        });
    }

    /** Right-clicking a menu board: from a car, the nearest crew member takes your order. */
    public static void order(ServerPlayer player, BlockPos speaker) {
        if (!(player.getVehicle() instanceof CarEntity)) {
            player.displayClientMessage(Component.literal("§7The menu board says §fDRIVE-THRU ONLY§7. Pull up in a car, or order at the counter."), true);
            return;
        }
        RoadhouseNpcEntity crew = crewNear(player.serverLevel(), speaker);
        if (crew == null) {
            player.displayClientMessage(Component.literal("§7*crackle*... Nobody's answering. Looks like they're closed."), true);
            return;
        }
        Shops.Shop shop = crew.shop();
        player.displayClientMessage(Component.literal("§e" + String.format(GREETINGS[player.getRandom().nextInt(GREETINGS.length)], shop.title)), false);
        player.serverLevel().playSound(null, speaker, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 0.5f, 1.6f);
        if (ServerPlayNetworking.canSend(player, dev.freerun.net.ShopPayloads.Open.TYPE)) {
            ServerPlayNetworking.send(player, new dev.freerun.net.ShopPayloads.Open(crew.getId(), shop.ordinal(),
                    Shops.pricePercent(player.serverLevel(), crew.blockPosition())));
        }
    }

    private static RoadhouseNpcEntity crewNear(ServerLevel level, BlockPos at) {
        return level.getEntitiesOfClass(RoadhouseNpcEntity.class, new AABB(at).inflate(REACH - 4, 6, REACH - 4),
                        n -> n.shop() != null && n.shop().driveThru)
                .stream().min(java.util.Comparator.comparingDouble(n -> n.distanceToSqr(at.getCenter()))).orElse(null);
    }

    /** Called by {@link Shops#buy} for an order placed from a car. */
    static void queue(ServerPlayer player, Entity clerk, Shops.Shop shop, ItemStack stack) {
        Order o = ORDERS.get(player.getUUID());
        if (o == null || o.shop() != shop) {
            BlockPos window = findWindow(player.serverLevel(), clerk.blockPosition());
            o = new Order(shop, window, new ArrayList<>(), player.serverLevel().getGameTime());
            ORDERS.put(player.getUUID(), o);
        }
        o.items().add(stack);
    }

    private static BlockPos findWindow(ServerLevel level, BlockPos near) {
        Block window = dev.freerun.city.CityRegistry.PICKUP_WINDOW;
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(near.offset(-14, -3, -14), near.offset(14, 4, 14))) {
            if (!level.getBlockState(p).is(window)) continue;
            double d = p.distSqr(near);
            if (d < bestD) {
                bestD = d;
                best = p.immutable();
            }
        }
        return best;
    }

    private static void tick(ServerLevel level) {
        Iterator<Map.Entry<UUID, Order>> it = ORDERS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Order> e = it.next();
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(e.getKey());
            Order o = e.getValue();
            if (p == null) {
                if (level.getGameTime() - o.placed() > FORGOTTEN) it.remove();
                continue;
            }
            if (p.serverLevel() != level) continue;
            boolean late = level.getGameTime() - o.placed() > FORGOTTEN;
            boolean atWindow = o.window() == null
                    ? p.getVehicle() instanceof CarEntity car && car.speed() < 0.05
                    : p.getVehicle() instanceof CarEntity && p.position().distanceToSqr(o.window().getCenter()) < 4.5 * 4.5;
            if (!atWindow && !late) {
                if (level.getGameTime() % 100 == 0 && o.window() != null) {
                    p.displayClientMessage(Component.literal("§e🍔 Your " + o.shop().title + " order is ready — pull up to the window."), true);
                }
                continue;
            }
            for (ItemStack s : o.items()) if (!p.getInventory().add(s)) p.drop(s, false);
            if (p.getRandom().nextInt(4) == 0) {
                ItemStack napkins = new ItemStack(net.minecraft.world.item.Items.PAPER, 2);
                napkins.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("Napkins"));
                p.getInventory().add(napkins);
            }
            p.displayClientMessage(Component.literal(late
                    ? "§7Your " + o.shop().title + " order turned up on your seat. It's a little cold."
                    : "§a" + String.format(HANDOFFS[p.getRandom().nextInt(HANDOFFS.length)], o.shop().title)), false);
            level.playSound(null, p.blockPosition(), SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 1.0f, 1.0f);
            dev.freerun.career.Careers.addXp(p, 2, "drive-thru");
            it.remove();
        }
    }

    public static boolean hasOrder(ServerPlayer p) {
        return ORDERS.containsKey(p.getUUID());
    }
}
