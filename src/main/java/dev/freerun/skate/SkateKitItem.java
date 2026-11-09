package dev.freerun.skate;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** A flat-packed skate park feature. Use it on the ground and it builds itself in front of you. */
public class SkateKitItem extends Item {
    public final ParkBuilder.Kind kind;

    public SkateKitItem(ParkBuilder.Kind kind, Properties p) {
        super(p);
        this.kind = kind;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal("Footprint " + kind.width + " × " + kind.length).withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("Use on the ground: it builds away from you").withStyle(ChatFormatting.AQUA));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        Direction f = ctx.getHorizontalDirection();
        BlockPos origin = ctx.getClickedFace() == Direction.UP ? ctx.getClickedPos().above() : ctx.getClickedPos().relative(ctx.getClickedFace());
        Direction right = f.getClockWise();
        List<BlockPos> where = new ArrayList<>();
        List<BlockState> what = new ArrayList<>();
        ParkBuilder.build(kind, f, (u, y, v, s) -> {
            where.add(origin.relative(right, u).relative(f, v + 1).above(y));
            what.add(s);
        });
        for (int i = 0; i < where.size(); i++) {
            BlockPos p = where.get(i);
            BlockState there = level.getBlockState(p);
            boolean floor = p.getY() < origin.getY();
            if (floor ? !there.isSolid() && !there.canBeReplaced() : !there.canBeReplaced()) {
                if (player != null && !level.isClientSide) {
                    player.displayClientMessage(Component.literal("§cNot enough room for the " + kind.title + " (" + kind.width + " × " + kind.length + ")."), true);
                }
                return InteractionResult.FAIL;
            }
        }
        if (level.isClientSide) return InteractionResult.SUCCESS;
        for (int i = 0; i < where.size(); i++) level.setBlock(where.get(i), what.get(i), 3);
        level.playSound(null, origin, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0f, 0.8f);
        level.playSound(null, origin, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.3f, 1.6f);
        if (player != null && !player.getAbilities().instabuild) ctx.getItemInHand().shrink(1);
        if (player != null) player.displayClientMessage(Component.literal("§a" + kind.title + " built."), true);
        return InteractionResult.CONSUME;
    }
}
