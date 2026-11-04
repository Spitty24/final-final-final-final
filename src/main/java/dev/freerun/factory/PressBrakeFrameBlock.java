package dev.freerun.factory;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The rest of a press brake: the press is 3 blocks wide and 3 tall, and these invisible pieces
 * fill out its frame so it's solid all over. The top-middle piece draws the crown (hydraulic
 * cylinders and the top beam). Clicking any piece works the press; breaking any piece breaks it.
 *
 *   W = 0..2 across the press (1 is the middle), H = 0..2 up from the floor.
 */
public class PressBrakeFrameBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<PressBrakeFrameBlock> CODEC = simpleCodec(PressBrakeFrameBlock::new);
    public static final IntegerProperty W = IntegerProperty.create("w", 0, 2);
    public static final IntegerProperty H = IntegerProperty.create("h", 0, 2);

    public PressBrakeFrameBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(W, 0).setValue(H, 1));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, W, H);
    }

    public static boolean isCrown(BlockState state) {
        return state.getValue(W) == 1 && state.getValue(H) == 2;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return isCrown(state) ? RenderShape.MODEL : RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return PressBrakeBlock.SHAPES.get(state.getValue(FACING));
    }

    /** Where this piece's press brake (bottom-middle) is. */
    public static BlockPos mainPos(BlockPos pos, BlockState state) {
        Direction across = state.getValue(FACING).getClockWise();
        return pos.relative(across, 1 - state.getValue(W)).below(state.getValue(H));
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        super.onRemove(state, level, pos, newState, moved);
        if (level.isClientSide || newState.is(this)) return;
        BlockPos main = mainPos(pos, state);
        if (level.getBlockState(main).getBlock() instanceof PressBrakeBlock) level.destroyBlock(main, true);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        BlockPos main = mainPos(pos, state);
        BlockState ms = level.getBlockState(main);
        if (!(ms.getBlock() instanceof PressBrakeBlock press)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        return press.useItemOnPress(stack, ms, level, main, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos main = mainPos(pos, state);
        BlockState ms = level.getBlockState(main);
        if (!(ms.getBlock() instanceof PressBrakeBlock press)) return InteractionResult.PASS;
        return press.useWithoutItemOnPress(ms, level, main, player, hit);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(FactoryRegistry.PRESS_BRAKE);
    }
}
