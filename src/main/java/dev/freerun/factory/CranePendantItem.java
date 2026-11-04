package dev.freerun.factory;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The crane's hand pendant (the push-button box that hangs from real overhead cranes).
 *   hold right-click        drive the hook toward where you're looking
 *   sneak + right-click     lower the hook to pick up / set down
 * Works with the nearest overhead crane within 64 blocks.
 */
public class CranePendantItem extends Item {

    public CranePendantItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            if (!level.isClientSide) {
                CraneEntity crane = CraneEntity.linkedTo(player);
                if (crane == null) noCrane(player);
                else crane.hookAction(player);
            }
            return InteractionResultHolder.consume(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (level.isClientSide || !(entity instanceof Player player)) return;
        CraneEntity crane = CraneEntity.linkedTo(player);
        if (crane != null) {
            crane.drive(player);
        } else if (remaining % 40 == 0) {
            noCrane(player);
        }
    }

    private static void noCrane(Player player) {
        player.displayClientMessage(Component.literal(
                "§cNo overhead crane within 64 blocks. §7There's one in every welding factory."), true);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7Hold right-click: drive the crane to where you look"));
        tooltip.add(Component.literal("§7Sneak + right-click: hook up / set down"));
        tooltip.add(Component.literal("§8Controls the nearest overhead crane"));
    }
}
