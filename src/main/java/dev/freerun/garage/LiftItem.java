package dev.freerun.garage;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/** Sets down a four-post lift centred on the block you click, runways along the way you face. */
public class LiftItem extends Item {
    public LiftItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
        Player player = ctx.getPlayer();
        Direction facing = player != null ? player.getDirection() : Direction.NORTH;
        LiftEntity lift = GarageRegistry.LIFT.create(level);
        if (lift == null) return InteractionResult.FAIL;
        lift.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        lift.setAlongX(facing.getAxis() == Direction.Axis.X);
        lift.setHeight(LiftEntity.LOW);
        if (!level.getEntitiesOfClass(LiftEntity.class, lift.getBoundingBox().inflate(0.5, 3, 0.5)).isEmpty()) {
            if (player != null) player.displayClientMessage(Component.literal("§cThere's already a lift there."), true);
            return InteractionResult.FAIL;
        }
        level.addFreshEntity(lift);
        ctx.getItemInHand().consume(1, player);
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.ANVIL_PLACE, net.minecraft.sounds.SoundSource.BLOCKS, 0.5f, 0.8f);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal("3 x 6 blocks, runways along the way you face.").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("Drive on, get out, right-click a post to raise it.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
