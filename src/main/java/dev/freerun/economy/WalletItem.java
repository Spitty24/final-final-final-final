package dev.freerun.economy;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Holds your cash so it doesn't fill your inventory. Use it to tuck every loose bill inside;
 * sneak-use to take out a $20. Shops and paychecks use the wallet automatically.
 */
public class WalletItem extends Item {
    public WalletItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack wallet = player.getItemInHand(hand);
        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                int bal = Money.walletBalance(wallet);
                int take = Math.min(20, bal);
                if (take <= 0) {
                    player.displayClientMessage(Component.literal("§7Your wallet is empty."), true);
                } else {
                    Money.setWalletBalance(wallet, bal - take);
                    for (ItemStack bill : Money.bills(take)) {
                        if (!player.getInventory().add(bill)) player.drop(bill, false);
                    }
                    player.displayClientMessage(Component.literal("§aTook out " + Money.fmt(take)
                            + ". §7Left in wallet: " + Money.fmt(bal - take)), true);
                }
            } else {
                int added = Money.depositAll(player, wallet);
                player.displayClientMessage(Component.literal(added > 0
                        ? "§aPut " + Money.fmt(added) + " in your wallet. §7Balance: " + Money.fmt(Money.walletBalance(wallet))
                        : "§7Wallet: §a" + Money.fmt(Money.walletBalance(wallet))), true);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 0.8f, 1.0f);
        }
        return InteractionResultHolder.sidedSuccess(wallet, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§aBalance: " + Money.fmt(Money.walletBalance(stack))));
        tooltip.add(Component.literal("§7Use: put all your loose bills in"));
        tooltip.add(Component.literal("§7Sneak-use: take out $20"));
    }
}
