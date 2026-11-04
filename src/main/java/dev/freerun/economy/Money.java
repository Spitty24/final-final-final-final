package dev.freerun.economy;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Cash. Money is real items: $1, $5, $20 and $100 bills, plus a Wallet that holds a balance of its
 * own. Paying takes from your wallet first, then your bills, and hands back change. Works on both
 * sides (the client uses {@link #balance} for the HUD and the shop screen).
 */
public final class Money {
    private Money() {}

    public static String fmt(int dollars) {
        return "$" + String.format("%,d", dollars);
    }

    public static int walletBalance(ItemStack wallet) {
        CustomData data = wallet.get(DataComponents.CUSTOM_DATA);
        return data == null ? 0 : data.copyTag().getInt("Balance");
    }

    public static void setWalletBalance(ItemStack wallet, int balance) {
        CustomData.update(DataComponents.CUSTOM_DATA, wallet, tag -> tag.putInt("Balance", Math.max(0, balance)));
    }

    /** Value of one item of {@code stack} if it's a bill, else 0. */
    public static int billValue(ItemStack stack) {
        return stack.getItem() instanceof CashItem cash ? cash.value() : 0;
    }

    public static int balance(Player player) {
        int total = 0;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof WalletItem) total += walletBalance(s);
            else total += billValue(s) * s.getCount();
        }
        return total;
    }

    private static ItemStack findWallet(Player player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).getItem() instanceof WalletItem) return inv.getItem(i);
        }
        return ItemStack.EMPTY;
    }

    /** Takes {@code amount} from the player, making change. Returns false (taking nothing) if they're short. */
    public static boolean pay(Player player, int amount) {
        if (amount <= 0) return true;
        if (player.getAbilities().instabuild) return true;
        if (balance(player) < amount) return false;
        int left = amount;
        Inventory inv = player.getInventory();
        // Wallets first.
        for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (!(s.getItem() instanceof WalletItem)) continue;
            int bal = walletBalance(s);
            int take = Math.min(bal, left);
            setWalletBalance(s, bal - take);
            left -= take;
        }
        // Then bills, smallest first, breaking a bigger one if we have to.
        for (int denom : EconomyRegistry.DENOMINATIONS_ASC) {
            for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
                ItemStack s = inv.getItem(i);
                if (billValue(s) != denom) continue;
                while (!s.isEmpty() && left > 0) {
                    s.shrink(1);
                    left -= denom;
                }
            }
        }
        if (left < 0) give(player, -left);
        inv.setChanged();
        return true;
    }

    /** Pays the player: into their wallet if they carry one, otherwise as bills. */
    public static void give(Player player, int amount) {
        if (amount <= 0) return;
        ItemStack wallet = findWallet(player);
        if (!wallet.isEmpty()) {
            setWalletBalance(wallet, walletBalance(wallet) + amount);
            return;
        }
        for (ItemStack bill : bills(amount)) {
            if (!player.getInventory().add(bill)) player.drop(bill, false);
        }
    }

    /** The fewest bills that add up to {@code amount}. */
    public static java.util.List<ItemStack> bills(int amount) {
        java.util.List<ItemStack> out = new java.util.ArrayList<>();
        int[] desc = EconomyRegistry.DENOMINATIONS_ASC;
        for (int i = desc.length - 1; i >= 0 && amount > 0; i--) {
            int n = amount / desc[i];
            amount -= n * desc[i];
            while (n > 0) {
                int c = Math.min(n, 64);
                out.add(new ItemStack(EconomyRegistry.bill(desc[i]), c));
                n -= c;
            }
        }
        return out;
    }

    /** Moves every loose bill in the inventory into the wallet. Returns the amount deposited. */
    public static int depositAll(Player player, ItemStack wallet) {
        int sum = 0;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            int v = billValue(s);
            if (v > 0) {
                sum += v * s.getCount();
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
        if (sum > 0) setWalletBalance(wallet, walletBalance(wallet) + sum);
        return sum;
    }

    static CompoundTag tag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }
}
