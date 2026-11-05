package dev.freerun.life;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A desktop computer: flat-panel monitor on a stand, keyboard and mouse, sitting on a desk.
 * SCREEN 0 off, 1 the desktop, 2 crashed (blue screen, for IT to fix), 3 a spreadsheet waiting
 * for an accountant.
 */
public class ComputerBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<ComputerBlock> CODEC = simpleCodec(ComputerBlock::new);
    public static final IntegerProperty SCREEN = IntegerProperty.create("screen", 0, 3);
    public static final int OFF = 0, ON = 1, CRASHED = 2, LEDGER = 3;
    private static final VoxelShape SHAPE = Block.box(1, 0, 3, 15, 12, 14);
    private static final String[] DESKTOP = {"You check the email: 47 unread, all \"Re: Re: Fwd: quick question\".",
            "Someone left a spreadsheet open called FINAL_final_v3_REAL.xlsx.", "The screensaver is a slideshow of somebody's boat.",
            "There's a sticky note on the monitor with the password on it.", "A meeting reminder pops up: \"Meeting about meetings\".",
            "The calendar is booked solid with 'Focus Time'.", "A solitaire game, paused mid-move."};

    public ComputerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SCREEN, ON));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SCREEN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return FurnitureBlock.rotate(SHAPE, state.getValue(FACING));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
        int screen = state.getValue(SCREEN);
        if (screen == CRASHED) {
            OfficeJobs.repairComputer(sp, pos, state);
        } else if (screen == LEDGER) {
            OfficeJobs.openLedger(sp, pos, state);
        } else if (player.isShiftKeyDown()) {
            level.setBlock(pos, state.setValue(SCREEN, screen == OFF ? ON : OFF), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.4f, 1.8f);
        } else if (screen == OFF) {
            level.setBlock(pos, state.setValue(SCREEN, ON), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.4f, 1.4f);
            sp.displayClientMessage(Component.literal("§7*bing* The computer boots up."), true);
        } else {
            sp.displayClientMessage(Component.literal("§7" + DESKTOP[level.getRandom().nextInt(DESKTOP.length)]), true);
            level.playSound(null, pos, SoundEvents.WOODEN_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.3f, 2.0f);
        }
        return InteractionResult.CONSUME;
    }
}
