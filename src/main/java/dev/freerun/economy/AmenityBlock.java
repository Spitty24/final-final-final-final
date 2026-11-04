package dev.freerun.economy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Shared bits for the store fixtures: they face whoever placed them and have a fixed shape. */
public abstract class AmenityBlock extends HorizontalDirectionalBlock {
    private final VoxelShape shape;

    protected AmenityBlock(Properties properties, VoxelShape shape) {
        super(properties);
        this.shape = shape;
        registerDefaultState(defaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)));
    }

    /** Subclasses add their own property defaults here. */
    protected BlockState defaultState(BlockState state) {
        return state;
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
        return shape;
    }

    protected static void say(Player player, String text) {
        player.displayClientMessage(Component.literal(text), true);
    }

    protected static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    /** Charges the player, or tells them they're short. */
    protected static boolean charge(Player player, int price) {
        if (Money.pay(player, price)) return true;
        say(player, "§cThat's " + Money.fmt(price) + ". You have " + Money.fmt(Money.balance(player)) + ".");
        return false;
    }
}
