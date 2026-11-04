package dev.freerun.food;

import com.mojang.serialization.MapCodec;
import dev.freerun.factory.PartItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
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
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Break-room microwave. Right-click with food: raw food comes out cooked (anything a furnace can
 * cook), cooked food comes out nice and warm. Four seconds and a ding. Do NOT put metal in it.
 */
public class MicrowaveBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<MicrowaveBlock> CODEC = simpleCodec(MicrowaveBlock::new);
    public static final BooleanProperty RUNNING = BooleanProperty.create("running");
    private static final VoxelShape SHAPE = Block.box(1, 0, 3, 15, 9, 13);
    private static final int COOK_TICKS = 80;
    private static final int METAL_TICKS = 40;

    public MicrowaveBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(RUNNING, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, RUNNING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MicrowaveBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    private static boolean isMetal(ItemStack s) {
        return s.getItem() instanceof PartItem || s.getItem() instanceof TieredItem
                || s.is(Items.IRON_INGOT) || s.is(Items.GOLD_INGOT) || s.is(Items.COPPER_INGOT)
                || s.is(Items.IRON_NUGGET) || s.is(Items.GOLD_NUGGET) || s.is(Items.NETHERITE_INGOT)
                || s.is(Items.CHAIN) || s.is(Items.BUCKET);
    }

    private static ItemStack cooked(Level level, ItemStack in) {
        return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(in), level)
                .map(r -> r.value().getResultItem(level.registryAccess()).copy())
                .filter(out -> out.has(DataComponents.FOOD))
                .orElse(ItemStack.EMPTY);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        boolean metal = isMetal(stack);
        boolean food = stack.has(DataComponents.FOOD) || !cooked(level, stack.copyWithCount(1)).isEmpty();
        if (!metal && !food) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!(level instanceof ServerLevel server)) return ItemInteractionResult.SUCCESS;
        if (state.getValue(RUNNING)) {
            player.displayClientMessage(Component.literal("§7Wait for the ding."), true);
            return ItemInteractionResult.CONSUME;
        }
        if (!(level.getBlockEntity(pos) instanceof MicrowaveBlockEntity be)) return ItemInteractionResult.CONSUME;
        be.setInside(stack.copyWithCount(1));
        stack.consume(1, player);
        server.setBlock(pos, state.setValue(RUNNING, true), Block.UPDATE_ALL);
        server.scheduleTick(pos, this, metal ? METAL_TICKS : COOK_TICKS);
        server.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.6f, 1.6f);
        server.playSound(null, pos, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.8f, 1.7f);
        if (metal) {
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5,
                    25, 0.2, 0.15, 0.2, 0.2);
            server.playSound(null, pos, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 2.0f, 2.0f);
            player.displayClientMessage(Component.literal("§cUh oh. That's metal."), true);
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!level.isClientSide) {
            player.displayClientMessage(Component.literal(state.getValue(RUNNING) ? "§7Humming away..."
                    : "§7Put food in: raw comes out cooked, cooked comes out warm. No metal!"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(RUNNING)) return;
        level.setBlock(pos, state.setValue(RUNNING, false), Block.UPDATE_ALL);
        if (!(level.getBlockEntity(pos) instanceof MicrowaveBlockEntity be) || be.inside().isEmpty()) return;
        ItemStack in = be.inside();
        be.setInside(ItemStack.EMPTY);
        Direction front = state.getValue(FACING);
        if (isMetal(in)) {
            // Sparks, a bang, and the fork comes flying back out. No blocks harmed.
            level.explode(null, pos.getX() + 0.5 + front.getStepX() * 0.6, pos.getY() + 0.5, pos.getZ() + 0.5 + front.getStepZ() * 0.6,
                    1.0f, Level.ExplosionInteraction.NONE);
            Block.popResourceFromFace(level, pos, front, in);
            for (Player p : level.players()) {
                if (p.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 16 * 16) {
                    p.displayClientMessage(Component.literal("§c§lWho microwaved metal?!"), false);
                }
            }
            return;
        }
        ItemStack out = cooked(level, in);
        if (out.isEmpty()) out = in;   // already cooked: just warmer now
        Block.popResourceFromFace(level, pos, front, out);
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 1.0f, 2.0f);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(RUNNING) && random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 0, 0.01, 0);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof MicrowaveBlockEntity be
                && !be.inside().isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), be.inside());
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
