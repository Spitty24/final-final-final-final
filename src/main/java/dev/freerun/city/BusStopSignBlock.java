package dev.freerun.city;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The bus stop sign on its post. Right-click it to see the route and when the next bus is due. */
public class BusStopSignBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<BusStopSignBlock> CODEC = simpleCodec(BusStopSignBlock::new);
    private static final VoxelShape SHAPE = Block.box(6, 0, 6, 10, 16, 10);

    public BusStopSignBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
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
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        BlockPos city = CityWorld.cityAt(server, pos);
        BusRoutes.Stop stop = city == null ? null : BusRoutes.stopAtSign(pos.getX() - city.getX(), pos.getZ() - city.getZ());
        if (stop == null) {
            player.displayClientMessage(Component.literal("§eBus stop. §7(Not on a route.)"), false);
            return InteractionResult.CONSUME;
        }
        BusRoutes.Route r = stop.route;
        double best = Double.MAX_VALUE;
        for (BusEntity bus : server.getEntitiesOfClass(BusEntity.class, new AABB(pos).inflate(500, 40, 500), b -> b.route() == r)) {
            best = Math.min(best, r.ahead(bus.distance(), stop.s));
        }
        String eta = best == Double.MAX_VALUE ? "every few minutes" : best < 15 ? "§aarriving now" : "in about " + Math.max(1, (int) Math.round(best / 6.0 / 10.0) * 10) + "s";
        player.displayClientMessage(Component.literal("■ Route " + r.title()).withStyle(st -> st.withColor(r.color).withBold(true))
                .append(Component.literal(" — " + stop.name).withStyle(st -> st.withColor(0xAAAAAA).withBold(false))), false);
        player.displayClientMessage(Component.literal("§7Next bus " + eta + ". Fare §f$2§7. Wait by the sign and it'll stop for you."), false);
        return InteractionResult.CONSUME;
    }
}
