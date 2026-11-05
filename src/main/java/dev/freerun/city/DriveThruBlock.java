package dev.freerun.city;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The two halves of a drive-thru. The menu board (with its speaker) takes your order when you
 * right-click it from a car; the pickup window is where the bag comes out.
 */
public class DriveThruBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<DriveThruBlock> SPEAKER_CODEC = simpleCodec(p -> new DriveThruBlock(p, true));
    public static final MapCodec<DriveThruBlock> WINDOW_CODEC = simpleCodec(p -> new DriveThruBlock(p, false));
    private static final VoxelShape BOARD = Block.box(1, 0, 6, 15, 16, 10);
    private final boolean speaker;

    public DriveThruBlock(Properties properties, boolean speaker) {
        super(properties);
        this.speaker = speaker;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return speaker ? SPEAKER_CODEC : WINDOW_CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return speaker ? BOARD : Shapes.block();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
        if (speaker) {
            dev.freerun.economy.DriveThru.order(sp, pos);
        } else {
            sp.displayClientMessage(Component.literal(dev.freerun.economy.DriveThru.hasOrder(sp)
                    ? "§eStop your car right here and they'll hand your order out."
                    : "§7\"Please order at the menu board first!\""), true);
        }
        return InteractionResult.CONSUME;
    }
}
