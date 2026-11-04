package dev.freerun.vehicle;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/** Places a car (random paint, half a tank) facing the way you're looking. */
public class CarItem extends Item {
    public CarItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
        CarEntity car = new CarEntity(VehicleRegistry.CAR, level);
        Player player = ctx.getPlayer();
        float yaw = player != null ? player.getYRot() : 0.0f;
        car.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0.0f);
        car.setYBodyRot(yaw);
        car.setYHeadRot(yaw);
        if (!level.noCollision(car)) {
            if (player != null) player.displayClientMessage(Component.literal("§cNot enough room to park a car there."), true);
            return InteractionResult.FAIL;
        }
        car.randomColor();
        level.addFreshEntity(car);
        ctx.getItemInHand().consume(1, player);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7Place it, then right-click to get in."));
        tooltip.add(Component.literal("§7W/S gas & brake · A/D steer · Space handbrake · R horn"));
        tooltip.add(Component.literal("§8Sneak-punch an empty car to pick it back up."));
    }
}
