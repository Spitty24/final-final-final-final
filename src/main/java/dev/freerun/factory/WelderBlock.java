package dev.freerun.factory;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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

import java.util.Set;

/**
 * A MIG welding machine on a cart, with its gas bottle strapped on the back. It powers every
 * Welding Torch within 12 blocks. Right-click it with an empty hand to take a torch.
 */
public class WelderBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<WelderBlock> CODEC = simpleCodec(WelderBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(2, 0, 2, 14, 12, 14),    // machine body on its cart
            Block.box(4, 0, 10, 12, 16, 15));  // gas bottle

    public WelderBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
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
        return SHAPE;   // close enough for every rotation; the machine is nearly square
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player.getInventory().hasAnyOf(Set.of(FactoryRegistry.WELDING_TORCH))) {
            player.displayClientMessage(Component.literal(
                    "§7The welder hums. Your torch works anywhere within 12 blocks of it."), true);
        } else {
            ItemStack torch = new ItemStack(FactoryRegistry.WELDING_TORCH);
            if (!player.getInventory().add(torch)) player.drop(torch, false);
            player.displayClientMessage(Component.literal(
                    "§aTook a welding torch. §7Click a part, then a part touching it, to weld them."), false);
            level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_CHAIN.value(), SoundSource.BLOCKS, 1.0f, 1.0f);
        }
        return InteractionResult.CONSUME;
    }

    /** Little crackles of static off the front panel. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) != 0) return;
        Direction front = state.getValue(FACING);
        double x = pos.getX() + 0.5 + front.getStepX() * 0.4 + (random.nextDouble() - 0.5) * 0.4;
        double z = pos.getZ() + 0.5 + front.getStepZ() * 0.4 + (random.nextDouble() - 0.5) * 0.4;
        level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, pos.getY() + 0.75, z, 0.0, 0.02, 0.0);
    }
}
