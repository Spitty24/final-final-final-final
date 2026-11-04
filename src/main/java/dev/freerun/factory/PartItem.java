package dev.freerun.factory;

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
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * A steel part in item form. Right-click a block to set it down as a free physics part
 * (centred on the block you click, turned to face the way you're looking).
 */
public class PartItem extends Item {
    private final PartType type;

    public PartItem(PartType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public PartType type() {
        return type;
    }

    /** Quarter-turn rotation matching the direction the player faces. */
    public static int rotationFor(Player player) {
        return player == null ? 0 : player.getDirection().get2DDataValue() & 3;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;

        Vec3 click = ctx.getClickLocation();
        double x, y, z;
        if (ctx.getClickedFace() == Direction.UP) {
            x = Math.floor(click.x) + 0.5;
            y = click.y;
            z = Math.floor(click.z) + 0.5;
        } else {
            BlockPos p = ctx.getClickedPos().relative(ctx.getClickedFace());
            x = p.getX() + 0.5;
            y = p.getY();
            z = p.getZ() + 0.5;
        }
        Player player = ctx.getPlayer();
        SteelPartEntity part = SteelPartEntity.single(level, type, x, y, z, rotationFor(player));
        if (!level.noCollision(part, part.getBoundingBox().deflate(1.0E-3))) {
            if (player != null) player.displayClientMessage(Component.literal("§cNo room for that part there."), true);
            return InteractionResult.FAIL;
        }
        level.addFreshEntity(part);
        ctx.getItemInHand().consume(1, player);
        level.playSound(null, x, y, z, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0f, 0.8f);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7Place it, then weld it to other parts."));
        switch (type) {
            case SHEET -> tooltip.add(Component.literal("§8Press brake: CUT → 2 plates, BEND → bracket"));
            case PLATE -> tooltip.add(Component.literal("§8Press brake: BEND → channel"));
            default -> { }
        }
    }
}
