package dev.freerun.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * One tile of the big 5x2 steakhouse sign. PART (0..9) picks which slice of the sign art the front
 * face shows: top row 0..4 left-to-right, bottom row 5..9.
 */
public class SignPanelBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<SignPanelBlock> CODEC = simpleCodec(SignPanelBlock::new);
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 9);

    public SignPanelBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, 0));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }
}
