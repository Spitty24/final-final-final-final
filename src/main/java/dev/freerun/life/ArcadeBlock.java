package dev.freerun.life;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** An arcade cabinet (two blocks tall to look at). GAME picks which game it runs. Costs $1 a play. */
public class ArcadeBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<ArcadeBlock> CODEC = simpleCodec(ArcadeBlock::new);
    public static final IntegerProperty GAME = IntegerProperty.create("game", 0, 2);
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 16, 15);

    public ArcadeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(GAME, 0));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, GAME);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        int game = Math.floorMod(ctx.getClickedPos().getX() + ctx.getClickedPos().getZ(), 3);
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite()).setValue(GAME, game);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp) {
            if (sp.isShiftKeyDown() && sp.isCreative()) {
                level.setBlock(pos, state.cycle(GAME), Block.UPDATE_ALL);   // swap the game board out
            } else {
                Arcade.open(sp, pos, state.getValue(GAME));
            }
        }
        return InteractionResult.CONSUME;
    }
}
