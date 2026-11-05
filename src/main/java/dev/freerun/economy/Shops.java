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
        DOUBLE_QUICK("Double Quick", 0xD8262E),
        GENERAL_STORE("General Store", 0x8A5A2B);

        public final String title;
        public final int color;

        Shop(String title, int color) {
            this.title = title;
            this.color = color;
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

    static void build(List<Offer> dqBuy, List<Offer> dqSell, List<Offer> gsBuy, List<Offer> gsSell) {
        BUY.clear();
        SELL.clear();
        BUY.add(List.copyOf(dqBuy));
        BUY.add(List.copyOf(gsBuy));
        SELL.add(List.copyOf(dqSell));
        SELL.add(List.copyOf(gsSell));
    }

    static Offer of(Item item, int count, int price) {
        return new Offer(new ItemStack(item, count), price);
    }

    // ------------------------------------------------------------------ server side

    /** Shops inside a city charge whatever local demand says (see the city hub); elsewhere, list price. */
    public static int pricePercent(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos) {
        dev.freerun.city.CityHubEntity hub = dev.freerun.city.CityHubEntity.nearest(level, pos);
        return hub == null ? 100 : (int) Math.round(hub.priceMultiplier() * 100);
    }

    public static int price(int list, int percent) {
        return Math.max(1, (int) Math.round(list * percent / 100.0));
    }

    public static void buy(ServerPlayer player, Entity clerk, Shop shop, int index, int times) {
        List<Offer> list = buys(shop);
        if (index < 0 || index >= list.size() || clerk == null || player.distanceToSqr(clerk) > 10 * 10) return;
        Offer o = list.get(index);
        int each = price(o.price(), pricePercent(player.serverLevel(), clerk.blockPosition()));
        each = Math.max(1, (int) Math.round(each * dev.freerun.career.Skills.shopDiscount(dev.freerun.career.Careers.get(player).skills())));
        int bought = 0;
        for (int i = 0; i < Math.max(1, Math.min(times, 16)); i++) {
            if (!Money.pay(player, each)) break;
            ItemStack give = o.stack().copy();
            if (!player.getInventory().add(give)) player.drop(give, false);
            bought++;
        }
        if (bought == 0) {
            player.displayClientMessage(Component.literal("§cYou can't afford that. §7(" + Money.fmt(each)
                    + ", you have " + Money.fmt(Money.balance(player)) + ")"), true);
            return;
        }
        player.level().playSound(null, clerk.blockPosition(), ModSounds.CASH_REGISTER, SoundSource.NEUTRAL, 0.8f, 1.0f);
        player.displayClientMessage(Component.literal("§aBought " + (bought > 1 ? bought + "× " : "")
                + o.stack().getHoverName().getString() + " for " + Money.fmt(each * bought) + ". §7Balance: "
                + Money.fmt(Money.balance(player))), true);
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
