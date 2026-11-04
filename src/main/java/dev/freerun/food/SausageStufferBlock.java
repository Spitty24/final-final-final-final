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
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A hand-cranked sausage stuffer. Load a filler crop into the hopper (optional, depending on the
 * sausage), then right-click with meat: it grinds and stuffs two links that pop out of the
 * nozzle. See {@link SausageRecipes} for what makes what. Empty hand: shows what's loaded.
 */
public class SausageStufferBlock extends HorizontalDirectionalBlock implements EntityBlock {
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
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SausageStufferBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    private SausageStufferBlockEntity entity(Level level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof SausageStufferBlockEntity be) return be;
        SausageStufferBlockEntity be = new SausageStufferBlockEntity(pos, state);   // placed before it had storage
        level.setBlockEntity(be);
        return be;
    }

    private static String name(ItemStack s) {
        return s.getHoverName().getString().toLowerCase();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        boolean meat = SausageRecipes.isMeat(stack);
        boolean filler = SausageRecipes.isFiller(stack);
        if (!meat && !filler) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (state.getValue(CRANKING)) return ItemInteractionResult.CONSUME;
        if (!(level instanceof ServerLevel server)) return ItemInteractionResult.SUCCESS;
        SausageStufferBlockEntity be = entity(level, pos, state);

        if (filler && !meat) {
            if (!be.filler().isEmpty()) {
                player.displayClientMessage(Component.literal("§eAlready loaded with " + name(be.filler())
                        + ". Add meat, or empty-hand click to take it out."), true);
                return ItemInteractionResult.CONSUME;
            }
            be.setFiller(stack.copyWithCount(1));
            stack.consume(1, player);
            server.playSound(null, pos, SoundEvents.COMPOSTER_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
            player.displayClientMessage(Component.literal("§aLoaded " + name(be.filler()) + " into the hopper. §7Now add meat."), true);
            return ItemInteractionResult.SUCCESS;
        }

        Item result = SausageRecipes.result(stack, be.filler());
        if (result == null) {
            String loaded = be.filler().isEmpty() ? "nothing loaded" : name(be.filler()) + " loaded";
            player.displayClientMessage(Component.literal("§cNo sausage from that (" + loaded + "). §7"
                    + SausageRecipes.hint(stack)), true);
            return ItemInteractionResult.CONSUME;
        }
        ItemStack used = stack.copyWithCount(1);
        stack.consume(1, player);
        be.setFiller(ItemStack.EMPTY);
        crank(server, pos, state);
        server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, used),
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 10, 0.15, 0.1, 0.15, 0.05);
        ItemStack out = new ItemStack(result, 2);
        Block.popResourceFromFace(level, pos, state.getValue(FACING), out);
        player.displayClientMessage(Component.literal("§6Stuffed two links of " + out.getHoverName().getString() + "."), true);
        if (player instanceof ServerPlayer sp) Careers.addXp(sp, 3, "sausage making");
        return ItemInteractionResult.SUCCESS;
    }

    /** One turn of the crank: the handle comes down and it squelches. */
    public static void crank(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getValue(CRANKING)) return;
        level.setBlock(pos, state.setValue(CRANKING, true), Block.UPDATE_ALL);
        level.scheduleTick(pos, state.getBlock(), 16);
        level.playSound(null, pos, SoundEvents.SLIME_SQUISH, SoundSource.BLOCKS, 0.8f, 0.8f);
        level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.4f, 1.4f);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        SausageStufferBlockEntity be = entity(level, pos, state);
        if (!be.filler().isEmpty() && player.getMainHandItem().isEmpty()) {
            ItemStack back = be.filler();
            be.setFiller(ItemStack.EMPTY);
            if (!player.getInventory().add(back)) player.drop(back, false);
            player.displayClientMessage(Component.literal("§7Took the filler back out."), true);
        } else {
            player.displayClientMessage(Component.literal("§7Load a crop (optional), then feed it meat. "
                    + "Check the recipe signs in the processing plant."), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof SausageStufferBlockEntity be
                && !be.filler().isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), be.filler());
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(CRANKING)) level.setBlock(pos, state.setValue(CRANKING, false), Block.UPDATE_ALL);
    }
}
