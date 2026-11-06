package dev.freerun.alien;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The Swampwatch: a copper-cased wristwatch with a teal dial that was never made to tell time.
 * Use it to open the dial and pick a creature; use it again while transformed to turn back.
 */
public class SwampwatchItem extends Item {
    public static Runnable OPEN_DIAL = () -> { };

    public SwampwatchItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Form f = Transformations.form(player);
        if (f.transformed()) {
            if (!level.isClientSide && f.active()) Transformations.revert((ServerPlayer) player, false);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (level.isClientSide) OPEN_DIAL.run();
        else {
            int left = Transformations.rechargeLeft((ServerPlayer) player);
            if (left > 0 && !player.isCreative()) player.displayClientMessage(Component.literal("§cRecharging: " + left + "s"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public static ItemStack find(Player p) {
        for (InteractionHand h : InteractionHand.values()) if (p.getItemInHand(h).getItem() instanceof SwampwatchItem) return p.getItemInHand(h);
        for (ItemStack s : p.getInventory().items) if (s.getItem() instanceof SwampwatchItem) return s;
        return ItemStack.EMPTY;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal("Ten bayou creatures on the dial.").withStyle(ChatFormatting.AQUA));
        lines.add(Component.literal("Use: open the dial. Use again: turn back.").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("90 seconds a go, then 30 to recharge.").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
