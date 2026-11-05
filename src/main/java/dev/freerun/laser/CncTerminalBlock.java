package dev.freerun.laser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The CNC terminal next to the laser: draw a part, pick the assist gas, press CUT. */
public class CncTerminalBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 16, 13);

    public CncTerminalBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(HorizontalDirectionalBlock.FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    /** The nearest laser cutter within 6 blocks, or null. */
    public static LaserCutterBlockEntity cutterNear(Level level, BlockPos pos) {
        LaserCutterBlockEntity best = null;
        double bd = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-6, -2, -6), pos.offset(6, 2, 6))) {
            if (level.getBlockEntity(p) instanceof LaserCutterBlockEntity be) {
                double d = p.distSqr(pos);
                if (d < bd) {
                    bd = d;
                    best = be;
                }
            }
        }
        return best;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        LaserCutterBlockEntity be = cutterNear(level, pos);
        if (be == null) {
            player.displayClientMessage(Component.literal("§cNo laser cutter connected. §7Place one within 6 blocks of the terminal."), true);
            return InteractionResult.CONSUME;
        }
        CompoundTag data = new CompoundTag();
        data.putLong("Cutter", be.getBlockPos().asLong());
        data.putBoolean("Busy", be.busy());
        if (be.busy()) {
            data.putLong("Start", be.start());
            data.putInt("Duration", be.duration());
        }
        data.putInt("Sheets", player.getInventory().countItem(dev.freerun.factory.FactoryRegistry.STEEL_SHEET));
        data.putBoolean("Creative", player.getAbilities().instabuild);
        dev.freerun.life.LifePayloads.open((ServerPlayer) player, dev.freerun.life.LifePayloads.CNC, data);
        return InteractionResult.CONSUME;
    }
}
