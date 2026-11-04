package dev.freerun.food;

import com.mojang.serialization.MapCodec;
import dev.freerun.career.Careers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A hand-cranked sausage stuffer. Right-click it holding pork (raw or cooked porkchop) and it
 * grinds and stuffs it into two links of boudin, which come out of the nozzle at the front.
 */
public class SausageStufferBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<SausageStufferBlock> CODEC = simpleCodec(SausageStufferBlock::new);
    public static final BooleanProperty CRANKING = BooleanProperty.create("cranking");
    private static final VoxelShape SHAPE = Shapes.or(Block.box(2, 0, 2, 14, 3, 14), Block.box(4, 3, 4, 12, 15, 12));

    public SausageStufferBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CRANKING, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CRANKING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Items.PORKCHOP) && !stack.is(Items.COOKED_PORKCHOP)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (state.getValue(CRANKING)) return ItemInteractionResult.CONSUME;
        if (level instanceof ServerLevel server) {
            ItemStack used = stack.copyWithCount(1);
            stack.consume(1, player);
            server.setBlock(pos, state.setValue(CRANKING, true), Block.UPDATE_ALL);
            server.scheduleTick(pos, this, 16);
            server.playSound(null, pos, SoundEvents.SLIME_SQUISH, SoundSource.BLOCKS, 1.0f, 0.8f);
            server.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.6f, 1.4f);
            server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, used),
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 10, 0.15, 0.1, 0.15, 0.05);
            Block.popResourceFromFace(level, pos, state.getValue(FACING), new ItemStack(FoodRegistry.BOUDIN, 2));
            player.displayClientMessage(Component.literal("§6Stuffed two links of boudin."), true);
            if (player instanceof ServerPlayer sp) Careers.addXp(sp, 3, "making boudin");
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!level.isClientSide) {
            player.displayClientMessage(Component.literal("§7Feed it a porkchop to make boudin."), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(CRANKING)) level.setBlock(pos, state.setValue(CRANKING, false), Block.UPDATE_ALL);
    }
}
