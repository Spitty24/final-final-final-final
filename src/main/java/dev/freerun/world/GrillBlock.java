package dev.freerun.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** A functional cooking block — works like a smoker but 25% faster (see GrillBlockEntity). */
public class GrillBlock extends AbstractFurnaceBlock {
    public static final MapCodec<GrillBlock> CODEC = simpleCodec(GrillBlock::new);

    public GrillBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends AbstractFurnaceBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GrillBlockEntity(pos, state);
    }

    @Override
    protected <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                     BlockEntityType<T> type) {
        return createFurnaceTicker(level, type, BlockRegistry.GRILL_BE);
    }

    @Override
    protected void openContainer(Level level, BlockPos pos, Player player) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof GrillBlockEntity grill) {
            player.openMenu(grill);
            player.awardStat(Stats.INTERACT_WITH_SMOKER);
        }
    }
}
