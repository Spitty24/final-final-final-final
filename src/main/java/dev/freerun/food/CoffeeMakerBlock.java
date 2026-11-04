package dev.freerun.food;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A drip coffee maker. Right-click with Coffee Grounds to brew a pot (about 8 seconds: gurgling,
 * steam, drips), then right-click with a Coffee Mug to pour a cup. One pot is four cups.
 */
public class CoffeeMakerBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<CoffeeMakerBlock> CODEC = simpleCodec(CoffeeMakerBlock::new);
    public static final BooleanProperty BREWING = BooleanProperty.create("brewing");
    public static final IntegerProperty CUPS = IntegerProperty.create("cups", 0, 4);
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 12, 13);

    public CoffeeMakerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(BREWING, false).setValue(CUPS, 0));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, BREWING, CUPS);
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
        boolean grounds = stack.is(FoodRegistry.COFFEE_GROUNDS);
        boolean mug = stack.is(FoodRegistry.COFFEE_MUG);
        if (!grounds && !mug) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (state.getValue(BREWING)) {
            player.displayClientMessage(Component.literal("§7Still brewing. Give it a second."), true);
            return ItemInteractionResult.CONSUME;
        }
        int cups = state.getValue(CUPS);
        if (grounds) {
            if (cups > 0) {
                player.displayClientMessage(Component.literal("§7There's still coffee in the pot (" + cups + " cups)."), true);
                return ItemInteractionResult.CONSUME;
            }
            stack.consume(1, player);
            level.setBlock(pos, state.setValue(BREWING, true), Block.UPDATE_ALL);
            level.scheduleTick(pos, this, 160);
            level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.8f, 0.7f);
            player.displayClientMessage(Component.literal("§6Brewing a fresh pot..."), true);
            return ItemInteractionResult.SUCCESS;
        }
        if (cups <= 0) {
            player.displayClientMessage(Component.literal("§7The pot's empty. Brew some with Coffee Grounds."), true);
            return ItemInteractionResult.CONSUME;
        }
        stack.consume(1, player);
        ItemStack coffee = new ItemStack(FoodRegistry.CUP_OF_COFFEE);
        if (!player.getInventory().add(coffee)) player.drop(coffee, false);
        level.setBlock(pos, state.setValue(CUPS, cups - 1), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0f, 0.8f);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!level.isClientSide) {
            int cups = state.getValue(CUPS);
            player.displayClientMessage(Component.literal(state.getValue(BREWING) ? "§7Brewing..."
                    : cups > 0 ? "§6" + cups + " cup" + (cups == 1 ? "" : "s") + " left. §7Use a Coffee Mug to pour."
                    : "§7Empty. Add Coffee Grounds to brew."), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(BREWING)) return;
        level.setBlock(pos, state.setValue(BREWING, false).setValue(CUPS, 4), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.8f, 1.6f);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5, y = pos.getY() + 0.8, z = pos.getZ() + 0.5;
        if (state.getValue(BREWING)) {
            if (random.nextInt(2) == 0) level.addParticle(ParticleTypes.CLOUD, x, y + 0.1, z, 0.0, 0.03, 0.0);
            if (random.nextInt(3) == 0) level.addParticle(ParticleTypes.FALLING_HONEY, x, pos.getY() + 0.5, z, 0, 0, 0);
            if (random.nextInt(12) == 0) {
                level.playLocalSound(x, y, z, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.BLOCKS, 0.5f, 1.2f, false);
            }
        } else if (state.getValue(CUPS) > 0 && random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.CLOUD, x, y, z, 0.0, 0.01, 0.0);
        }
    }
}
