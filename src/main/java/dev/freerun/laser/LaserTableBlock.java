package dev.freerun.laser;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A section of the cutting bed: a frame with steel slats the sheet rests on. */
public class LaserTableBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 12, 16);

    public LaserTableBlock(Properties p) {
        super(p);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }
}
