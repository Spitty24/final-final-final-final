package dev.freerun.factory;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A powered belt. Carries anything sitting on it (steel parts, dropped items, mobs, you) in the
 * direction it faces; place it and it runs away from you. A redstone signal stops it.
 * Belts chain into turns, and a belt that runs into a chest, barrel or hopper loads single
 * steel parts and items into it.
 */
public class ConveyorBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<ConveyorBlock> CODEC = simpleCodec(ConveyorBlock::new);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    /** Belt surface height. */
    public static final double TOP = 4.0 / 16.0;
    /** Set slightly above the target speed: friction takes its share after the push. */
    private static final double SPEED = 0.16;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 4, 16);

    public ConveyorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection())
                .setValue(POWERED, ctx.getLevel().hasNeighborSignal(ctx.getClickedPos()));
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos from,
                                   boolean moving) {
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (state.getValue(POWERED)) return;
        if (entity.isPassenger() || entity.isSpectator() || entity instanceof ForkliftEntity || entity instanceof CraneEntity) return;
        if (entity instanceof Player && !level.isClientSide) return;          // players move themselves, client side
        if (entity instanceof SteelPartEntity part && part.isHeld()) return;   // crane, forklift or gravity gun has it
        // Only the belt under the entity's centre moves it, and only if it's sitting on the belt.
        if (Mth.floor(entity.getX()) != pos.getX() || Mth.floor(entity.getZ()) != pos.getZ()) return;
        double feet = entity.getY() - pos.getY();
        if (feet < TOP - 0.1 || feet > TOP + 0.2) return;

        Direction dir = state.getValue(FACING);
        double dx = dir.getStepX();
        double dz = dir.getStepZ();
        Vec3 v = entity.getDeltaMovement();
        double along = v.x * dx + v.z * dz;
        double add = Math.max(0.0, SPEED - along);
        double nx = v.x + dx * add;
        double nz = v.z + dz * add;
        if (!(entity instanceof LivingEntity)) {
            // Keep cargo centred on the belt so it follows turns instead of sliding off the side.
            double cx = pos.getX() + 0.5 - entity.getX();
            double cz = pos.getZ() + 0.5 - entity.getZ();
            if (dx == 0) nx = cx * 0.25;
            else nz = cz * 0.25;
        }
        entity.setDeltaMovement(nx, v.y, nz);

        if (!level.isClientSide) feedContainer(level, pos, dir, entity);
    }

    /** At the far end of the belt, load the cargo into a container in front of it, hopper style. */
    private static void feedContainer(Level level, BlockPos pos, Direction dir, Entity entity) {
        double rel = (entity.getX() - (pos.getX() + 0.5)) * dir.getStepX()
                + (entity.getZ() - (pos.getZ() + 0.5)) * dir.getStepZ();
        if (rel < 0.15) return;
        Container container = HopperBlockEntity.getContainerAt(level, pos.relative(dir));
        if (container == null) return;
        if (entity instanceof ItemEntity item) {
            ItemStack rest = HopperBlockEntity.addItem(null, container, item.getItem().copy(), dir.getOpposite());
            if (rest.isEmpty()) item.discard();
            else item.setItem(rest);
        } else if (entity instanceof SteelPartEntity part && part.partCount() == 1) {
            ItemStack stack = part.getParts().get(0).toItem();
            ItemStack rest = HopperBlockEntity.addItem(null, container, stack, dir.getOpposite());
            if (rest.isEmpty()) part.discard();
        }
    }
}
