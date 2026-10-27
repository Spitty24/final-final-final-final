package dev.freerun.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Right-click a block to bore a 20x20 tunnel (20 wide, 20 tall) in the direction you're facing,
 * {@link #LENGTH} blocks long. Skips unbreakable blocks (bedrock). Unbreakable tool.
 *
 * It drops everything it digs — a 20x20x20 tunnel is thousands of blocks, so a single use spawns
 * a huge number of item entities and will cause a noticeable lag spike.
 */
public class GiantShovelItem extends Item {
    private static final int WIDTH = 20;    // across (and tall)
    private static final int LENGTH = 20;   // forward

    public GiantShovelItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos origin = ctx.getClickedPos();
        Direction forward = ctx.getHorizontalDirection();   // player's facing (N/S/E/W)

        if (!level.isClientSide) {
            // Horizontal axis perpendicular to the tunnel direction.
            int perpX = forward.getStepX() != 0 ? 0 : 1;
            int perpZ = forward.getStepX() != 0 ? 1 : 0;
            int minY = level.getMinBuildHeight();
            int maxY = level.getMaxBuildHeight() - 1;
            int half = WIDTH / 2;

            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            for (int f = 0; f < LENGTH; f++) {
                for (int w = -half; w < WIDTH - half; w++) {
                    for (int h = -1; h < WIDTH - 1; h++) {   // one below the aimed block, 20 tall
                        int x = origin.getX() + forward.getStepX() * f + perpX * w;
                        int z = origin.getZ() + forward.getStepZ() * f + perpZ * w;
                        int y = origin.getY() + h;
                        if (y < minY || y > maxY) continue;
                        p.set(x, y, z);
                        BlockState state = level.getBlockState(p);
                        if (state.isAir()) continue;
                        if (state.getDestroySpeed(level, p) < 0) continue;   // skip bedrock/unbreakable
                        level.destroyBlock(p, true);   // drop everything it digs
                    }
                }
            }
            level.playSound(null, origin, SoundEvents.NETHERITE_BLOCK_BREAK, SoundSource.BLOCKS, 1.0f, 0.5f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
