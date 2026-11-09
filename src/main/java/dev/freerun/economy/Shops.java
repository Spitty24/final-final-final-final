package dev.freerun.economy;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * What each store sells and buys. The lists are built once at startup (after the items exist) and
 * are identical on client and server, so the shop screen and the server agree on indexes.
 */
public final class Shops {
    public enum Shop {
        DOUBLE_QUICK("Double Quick", 0xD8262E, "Double Quick Clerk", false),
        GENERAL_STORE("General Store", 0x8A5A2B, "Shopkeeper", false),
        MEGAMART("MegaMart Supercenter", 0x2E9E6B, "MegaMart Associate", false),
        DEPARTMENT("Hargrove & Finch", 0x6A3FA0, "Hargrove & Finch Clerk", false),
        DESSERT("Sugar Bayou Sweets", 0xF07AB8, "Sugar Bayou Baker", false),
        BURGER("Big Bun Burgers", 0xE0601E, "Big Bun Crew", true),
        CHICKEN("Cluck Shack", 0xC8402A, "Cluck Shack Crew", true),
        FURNITURE("Bayou Home Furnishings", 0x8A5A3A, "Furniture Sales Associate", false),
        BAR("The Rusty Pelican", 0x3A6E8A, "Bartender", false),
        PRIZES("Prize Counter", 0xC03AD8, "Prize Counter Attendant", false),
        SHOES("Foot Lockup", 0xE04040, "Sneaker Associate", false),
        CLOTHES("Bayou Threads", 0x3A7AD8, "Sales Associate", false),
        AUTO_PARTS("Bayou Auto & Alignment", 0xC8402A, "Parts Counter", false),
        SKATE("Bayou Boardshop", 0x40B0E0, "Boardshop Clerk", false);

        public final String title;
        public final int color;
        /** What the person behind the register is called. */
        public final String worker;
        /** Has a drive-thru: orders placed from a car are handed out at the pickup window. */
        public final boolean driveThru;

        Shop(String title, int color, String worker, boolean driveThru) {
            this.title = title;
            this.color = color;
            this.worker = worker;
            this.driveThru = driveThru;
        }

        /** The prize counter takes arcade tickets instead of money. */
        public boolean tickets() {
            return this == PRIZES;
        }

        public static Shop byId(int id) {
            Shop[] all = values();
            return all[Math.floorMod(id, all.length)];
        }
    }

    /** A line on the shelf: {@code stack} for {@code price} dollars (buying), or paid {@code price} for it (selling). */
    public record Offer(ItemStack stack, int price) {}

    private static final List<List<Offer>> BUY = new ArrayList<>();
    private static final List<List<Offer>> SELL = new ArrayList<>();

    private Shops() {}

    public static List<Offer> buys(Shop shop) {
        return BUY.isEmpty() ? List.of() : BUY.get(shop.ordinal());
    }

    public static List<Offer> sells(Shop shop) {
        return SELL.isEmpty() ? List.of() : SELL.get(shop.ordinal());
    }

    static void build(java.util.Map<Shop, List<Offer>> buy, java.util.Map<Shop, List<Offer>> sell) {
        BUY.clear();
        SELL.clear();
        for (Shop s : Shop.values()) {
            BUY.add(List.copyOf(buy.getOrDefault(s, List.of())));
            SELL.add(List.copyOf(sell.getOrDefault(s, List.of())));
        }
    }

    static Offer of(Item item, int count, int price) {
        return new Offer(new ItemStack(item, count), price);
    }

    // ------------------------------------------------------------------ server side

    /** Shops inside a city charge whatever local demand says (see the city hub); elsewhere, list price. */
    public static int pricePercent(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos) {
        return dev.freerun.city.CityWorld.pricePercent(level, pos);
    }

    public static int price(int list, int percent) {
        return Math.max(1, (int) Math.round(list * percent / 100.0));
    }

    public static void buy(ServerPlayer player, Entity clerk, Shop shop, int index, int times) {
        List<Offer> list = buys(shop);
        boolean drive = shop.driveThru && player.getVehicle() instanceof dev.freerun.vehicle.CarEntity;
        double reach = drive ? DriveThru.REACH : 10;
        if (index < 0 || index >= list.size() || clerk == null || player.distanceToSqr(clerk) > reach * reach) return;
        Offer o = list.get(index);
        if (shop.tickets()) {
            int bought = 0;
            for (int i = 0; i < Math.max(1, Math.min(times, 16)); i++) {
                if (count(player, dev.freerun.life.LifeRegistry.TICKET) < o.price()) break;
                remove(player, dev.freerun.life.LifeRegistry.TICKET, o.price());
                ItemStack give = o.stack().copy();
                if (!player.getInventory().add(give)) player.drop(give, false);
                bought++;
            }
            player.displayClientMessage(Component.literal(bought == 0 ? "§cYou need " + o.price() + " tickets for that."
                    : "§d🎟 Traded " + o.price() * bought + " tickets for " + o.stack().getHoverName().getString() + "!"), true);
            return;
        }
        int each = price(o.price(), pricePercent(player.serverLevel(), clerk.blockPosition()));
        each = Math.max(1, (int) Math.round(each * dev.freerun.career.Skills.shopDiscount(dev.freerun.career.Careers.get(player).skills())));
        int bought = 0;
        for (int i = 0; i < Math.max(1, Math.min(times, 16)); i++) {
            if (!Money.pay(player, each)) break;
            ItemStack give = o.stack().copy();
            if (drive) DriveThru.queue(player, clerk, shop, give);
            else if (!player.getInventory().add(give)) player.drop(give, false);
            bought++;
        }
        if (bought == 0) {
            player.displayClientMessage(Component.literal("§cYou can't afford that. §7(" + Money.fmt(each)
                    + ", you have " + Money.fmt(Money.balance(player)) + ")"), true);
            return;
        }
        player.level().playSound(null, clerk.blockPosition(), ModSounds.CASH_REGISTER, SoundSource.NEUTRAL, 0.8f, 1.0f);
        dev.freerun.city.CityWorld.recordSale(player.serverLevel(), clerk.blockPosition(), each * bought);
        player.displayClientMessage(Component.literal("§aBought " + (bought > 1 ? bought + "× " : "")
                + o.stack().getHoverName().getString() + " for " + Money.fmt(each * bought) + ". §7Balance: "
                + Money.fmt(Money.balance(player)) + (drive ? " §e— please pull around to the window." : "")), true);
    }

    public static void sell(ServerPlayer player, Entity clerk, Shop shop, int index, int times) {
        List<Offer> list = sells(shop);
        if (index < 0 || index >= list.size() || clerk == null || player.distanceToSqr(clerk) > 10 * 10) return;
        Offer o = list.get(index);
        int sold = 0;
        for (int i = 0; i < Math.max(1, Math.min(times, 64)); i++) {
            if (count(player, o.stack().getItem()) < o.stack().getCount()) break;
            remove(player, o.stack().getItem(), o.stack().getCount());
            Money.give(player, dev.freerun.career.Careers.has(player, "quality_control") ? (int) Math.round(o.price() * 1.15) : o.price());
            sold++;
        }
        if (sold == 0) {
            player.displayClientMessage(Component.literal("§cYou need " + o.stack().getCount() + "× "
                    + o.stack().getHoverName().getString() + " to sell."), true);
            return;
        }
        player.level().playSound(null, clerk.blockPosition(), ModSounds.CASH_REGISTER, SoundSource.NEUTRAL, 0.8f, 1.15f);
        player.displayClientMessage(Component.literal("§aSold for " + Money.fmt(o.price() * sold) + ". §7Balance: "
                + Money.fmt(Money.balance(player))), true);
    }

    public static int count(net.minecraft.world.entity.player.Player player, Item item) {
        int n = 0;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).is(item)) n += inv.getItem(i).getCount();
        return n;
    }

    private static void remove(ServerPlayer player, Item item, int amount) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize() && amount > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.is(item)) continue;
            int take = Math.min(amount, s.getCount());
            s.shrink(take);
            amount -= take;
        }
        inv.setChanged();
    }
}
