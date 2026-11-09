package dev.freerun.chaos;

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

/** A clock that runs backwards. Use it to switch Reverse Mode on or off for the whole world. */
public class BackwardsClockItem extends Item {
    public BackwardsClockItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp) {
            ReverseMode.toggle(sp);
            sp.getCooldowns().addCooldown(this, 40);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return ReverseMode.ON;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal("Its hands run the wrong way.").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
        lines.add(Component.literal("Use: turn Reverse Mode on / off").withStyle(ChatFormatting.GRAY));
    }
}
