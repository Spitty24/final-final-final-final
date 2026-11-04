package dev.freerun.factory;

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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A press brake. Right-click it holding steel to form it:
 *   CUT mode:   sheet → 2 plates
 *   BEND mode:  sheet → bracket (L angle), plate → channel (U)
 * Right-click with an empty hand to switch modes. The ram visibly comes down for each stroke and
 * the finished part pops out of the front.
 */
public class PressBrakeBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<PressBrakeBlock> CODEC = simpleCodec(PressBrakeBlock::new);
    public static final BooleanProperty CUT = BooleanProperty.create("cut");
    public static final BooleanProperty PRESSING = BooleanProperty.create("pressing");
    private static final int STROKE_TICKS = 12;

    public PressBrakeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH).setValue(CUT, false).setValue(PRESSING, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CUT, PRESSING);
    }

    /** A slab of machine across the facing axis (the bed and ram reach out past it visually). */
    static final java.util.Map<Direction, net.minecraft.world.phys.shapes.VoxelShape> SHAPES = java.util.Map.of(
            Direction.NORTH, Block.box(0, 0, 2, 16, 16, 14), Direction.SOUTH, Block.box(0, 0, 2, 16, 16, 14),
            Direction.EAST, Block.box(2, 0, 0, 14, 16, 16), Direction.WEST, Block.box(2, 0, 0, 14, 16, 16));

    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                                                                    BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext ctx) {
        return SHAPES.get(state.getValue(FACING));
    }

    /** Every frame position around a press at {@code main}: across -1..+1, up 0..2, except the press itself. */
    public static java.util.List<BlockPos> framePositions(BlockPos main, Direction facing) {
        java.util.List<BlockPos> out = new java.util.ArrayList<>();
        Direction across = facing.getClockWise();
        for (int h = 0; h < 3; h++) {
            for (int w = 0; w < 3; w++) {
                if (w == 1 && h == 0) continue;
                out.add(main.relative(across, w - 1).above(h));
            }
        }
        return out;
    }

    public static BlockState frameState(Direction facing, BlockPos main, BlockPos at) {
        Direction across = facing.getClockWise();
        int w = (at.getX() - main.getX()) * across.getStepX() + (at.getZ() - main.getZ()) * across.getStepZ() + 1;
        return FactoryRegistry.PRESS_BRAKE_FRAME.defaultBlockState().setValue(FACING, facing)
                .setValue(PressBrakeFrameBlock.W, w).setValue(PressBrakeFrameBlock.H, at.getY() - main.getY());
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // The operator side faces whoever placed it, and the whole 3x3 frame needs room.
        Direction facing = ctx.getHorizontalDirection().getOpposite();
        for (BlockPos p : framePositions(ctx.getClickedPos(), facing)) {
            if (!ctx.getLevel().getBlockState(p).canBeReplaced(ctx)) return null;
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) return;
        Direction facing = state.getValue(FACING);
        for (BlockPos p : framePositions(pos, facing)) level.setBlock(p, frameState(facing, pos, p), Block.UPDATE_ALL);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!level.isClientSide && !newState.is(this)) {
            for (BlockPos p : framePositions(pos, state.getValue(FACING))) {
                if (level.getBlockState(p).getBlock() instanceof PressBrakeFrameBlock) level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    public ItemInteractionResult useItemOnPress(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                Player player, InteractionHand hand, BlockHitResult hit) {
        return useItemOn(stack, state, level, pos, player, hand, hit);
    }

    public InteractionResult useWithoutItemOnPress(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return useWithoutItem(state, level, pos, player, hit);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof PartItem partItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (state.getValue(PRESSING)) return ItemInteractionResult.CONSUME;   // mid-stroke
        boolean cut = state.getValue(CUT);
        PartType in = partItem.type();

        ItemStack out;
        String done;
        if (in == PartType.SHEET && cut) {
            out = new ItemStack(FactoryRegistry.STEEL_PLATE, 2);
            done = "Sheared the sheet into 2 plates.";
        } else if (in == PartType.SHEET) {
            out = new ItemStack(FactoryRegistry.STEEL_BRACKET);
            done = "Bent the sheet into a bracket.";
        } else if (in == PartType.PLATE && !cut) {
            out = new ItemStack(FactoryRegistry.STEEL_CHANNEL);
            done = "Bent the plate into a channel.";
        } else {
            if (!level.isClientSide) {
                String why = in == PartType.PLATE
                        ? "That's already cut. Switch to BEND (empty hand) to make a channel."
                        : "That part is already formed.";
                player.displayClientMessage(Component.literal("§e" + why), true);
            }
            return ItemInteractionResult.CONSUME;
        }

        if (level instanceof ServerLevel server) {
            stack.consume(1, player);
            cycle(server, pos, state);
            Block.popResourceFromFace(level, pos, state.getValue(FACING), out);
            player.displayClientMessage(Component.literal("§a" + done), true);
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) dev.freerun.career.Careers.addXp(sp, 5, "pressing");
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Empty hand: switch between CUT and BEND. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockState next = state.cycle(CUT);
        level.setBlock(pos, next, Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6f, next.getValue(CUT) ? 0.6f : 0.5f);
        player.displayClientMessage(Component.literal(next.getValue(CUT)
                ? "§cPress brake: CUT §7(sheet → 2 plates)"
                : "§aPress brake: BEND §7(sheet → bracket, plate → channel)"), true);
        return InteractionResult.CONSUME;
    }

    /** One stroke of the ram: down, a heavy thunk, then back up after {@link #STROKE_TICKS}. */
    public static void cycle(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getValue(PRESSING)) return;
        level.setBlock(pos, state.setValue(PRESSING, true), Block.UPDATE_ALL);
        level.scheduleTick(pos, state.getBlock(), STROKE_TICKS);
        level.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.9f, 0.45f);
        level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.45f, 0.5f);
        level.playSound(null, pos.above(2), SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.25f, 0.5f);   // hydraulics
        Direction front = state.getValue(FACING);
        level.sendParticles(ParticleTypes.CRIT,
                pos.getX() + 0.5 + front.getStepX() * 0.5, pos.getY() + 0.55, pos.getZ() + 0.5 + front.getStepZ() * 0.5,
                8, 0.3, 0.05, 0.3, 0.15);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(PRESSING)) {
            level.setBlock(pos, state.setValue(PRESSING, false), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.5f, 0.6f);
        }
    }
}
