package dev.freerun.laser;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** The work phone: company-issued, locked down, only Teams installed. Dustin can always reach you. */
public class WorkPhoneItem extends Item {
    public static Runnable OPEN = () -> { };

    public WorkPhoneItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) OPEN.run();
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer p) || entity.tickCount % 20 != 0) return;
        if (find(p) != stack) return;   // only one work phone pings, however many you carry
        Dustin.tick(p, stack);
    }

    @Override
    public boolean allowComponentsUpdateAnimation(Player player, InteractionHand hand, ItemStack oldStack, ItemStack newStack) {
        return false;
    }

    /** The first work phone in the player's inventory (hands first). */
    public static ItemStack find(Player p) {
        for (InteractionHand h : InteractionHand.values()) if (p.getItemInHand(h).getItem() instanceof WorkPhoneItem) return p.getItemInHand(h);
        for (ItemStack s : p.getInventory().items) if (s.getItem() instanceof WorkPhoneItem) return s;
        return ItemStack.EMPTY;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal("Company property. Teams only.").withStyle(ChatFormatting.GRAY));
        int unread = Dustin.data(stack).getInt("Unread");
        if (unread > 0) lines.add(Component.literal(unread + " unread from Dustin").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
