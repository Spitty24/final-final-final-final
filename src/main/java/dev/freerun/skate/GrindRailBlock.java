package dev.freerun.skate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A round steel grind rail on posts. Flat rails sit at {@link #FLAT_TOP}; sloped ones (handrails
 * down a stair set) climb one block per block, and have no collision so you can walk under them.
 * SLOPE: 0 flat, 1 rising toward +axis, 2 rising toward -axis.
 */
public class GrindRailBlock extends Block {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public static final IntegerProperty SLOPE = IntegerProperty.create("slope", 0, 2);
    public static final double FLAT_TOP = 11 / 16.0;

    private static final VoxelShape FLAT_Z = Shapes.or(Block.box(6.5, 8.5, 0, 9.5, 11, 16), Block.box(7, 0, 7, 9, 9, 9));
    private static final VoxelShape FLAT_X = Shapes.or(Block.box(0, 8.5, 6.5, 16, 11, 9.5), Block.box(7, 0, 7, 9, 9, 9));
    private static final VoxelShape POST = Block.box(6.5, 0, 6.5, 9.5, 14, 9.5);

    public GrindRailBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.Z).setValue(SLOPE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(AXIS, SLOPE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(AXIS, ctx.getHorizontalDirection().getAxis());
    }

    /** Height of the rail's top (0 to 1.4 above the block) at a point inside it (fx, fz in [0, 1]). */
    public static double top(BlockState s, double fx, double fz) {
        int slope = s.getValue(SLOPE);
        if (slope == 0) return FLAT_TOP;
        double a = s.getValue(AXIS) == Direction.Axis.X ? fx : fz;
        a = Math.max(0, Math.min(1, a));
        return slope == 1 ? 0.4 + a : 1.4 - a;
    }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        if (s.getValue(SLOPE) != 0) return POST;
        return s.getValue(AXIS) == Direction.Axis.X ? FLAT_X : FLAT_Z;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        if (s.getValue(SLOPE) != 0) return Shapes.empty();
        return getShape(s, level, pos, ctx);
    }
}
