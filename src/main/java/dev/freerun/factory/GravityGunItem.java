package dev.freerun.factory;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Picks up physics parts, dropped items, mobs, minecarts, boats, falling blocks and lit TNT.
 *   right-click          grab / drop
 *   sneak + right-click  launch (what you're holding, or whatever you're aiming at)
 */
public class GravityGunItem extends Item {
    private static final int COOLDOWN = 6;

    public GravityGunItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.pass(stack);
        if (player instanceof ServerPlayer sp) GravityGunManager.use(sp, null);
        player.getCooldowns().addCooldown(this, COOLDOWN);
        return InteractionResultHolder.consume(stack);
    }

    /** Mobs under the crosshair come here instead of {@link #use}. */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
                                                  InteractionHand hand) {
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResult.PASS;
        if (player instanceof ServerPlayer sp) GravityGunManager.use(sp, target);
        player.getCooldowns().addCooldown(this, COOLDOWN);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7Right-click: grab / drop"));
        tooltip.add(Component.literal("§7Sneak + right-click: launch"));
        tooltip.add(Component.literal("§8Parts, items, mobs, carts, boats, TNT"));
    }
}
