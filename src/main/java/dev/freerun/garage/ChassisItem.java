package dev.freerun.garage;

import dev.freerun.vehicle.CarEntity;
import net.minecraft.ChatFormatting;
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

/** A bare car frame: set it down, then bolt on the parts one by one. */
public class ChassisItem extends Item {
    public ChassisItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
        CarEntity car = Garage.chassis(level);
        Player player = ctx.getPlayer();
        float yaw = player != null ? player.getYRot() : 0.0f;
        car.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0.0f);
        car.setYBodyRot(yaw);
        car.setYHeadRot(yaw);
        if (!level.noCollision(car)) {
            if (player != null) player.displayClientMessage(Component.literal("§cNot enough room for a car frame there."), true);
            return InteractionResult.FAIL;
        }
        car.randomColor();
        car.addFuel(-60.0f + 25.0f);   // a splash in the tank to get you going
        level.addFreshEntity(car);
        ctx.getItemInHand().consume(1, player);
        if (player != null) {
            player.displayClientMessage(Component.literal("§eChassis down. §7Fit 4 wheels, an engine, transmission, battery, fuel tank, steering and seats."), false);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal("Set it down and bolt the parts on.").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("Then tune the top speed with a torque wrench.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
