package dev.freerun.skate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Predicate;

/**
 * A curved or sloped piece of skate ramp. To someone walking it's a set of small steps; to a skater
 * it has no collision at all, because the skate controller rides its real surface
 * ({@link #surface}) instead.
 */
public class SkateRampBlock extends HorizontalDirectionalBlock {
    public static final com.mojang.serialization.MapCodec<SkateRampBlock> CODEC = simpleCodec(SkateRampBlock::new);
    public static final IntegerProperty SHAPE = IntegerProperty.create("shape", 0, RampProfiles.COUNT - 1);
    /** Is this entity on a board right now? Set by each side: the client for its own player, the server from the relay. */
    public static Predicate<Entity> CLIENT_SKATER = e -> false;

    private static final VoxelShape[][] STEPS = new VoxelShape[4][RampProfiles.COUNT];

    public SkateRampBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SHAPE, 0));
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, SHAPE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    public static boolean skater(Entity e) {
        if (!(e instanceof Player p)) return false;
        if (p.level().isClientSide) return CLIENT_SKATER.test(p);
        return p instanceof net.minecraft.server.level.ServerPlayer sp && Skating.skating(sp);
    }

    /** Fraction of the way up the ramp at a point inside the block (fx, fz in [0, 1]). */
    public static double along(Direction facing, double fx, double fz) {
        return switch (facing) {
            case NORTH -> 1 - fz;
            case SOUTH -> fz;
            case EAST -> fx;
            default -> 1 - fx;
        };
    }

    /** The ramp's surface height inside the block, 0 to 1. */
    public static double surface(BlockState s, double fx, double fz) {
        return RampProfiles.height(s.getValue(SHAPE), along(s.getValue(FACING), fx, fz));
    }

    private static VoxelShape steps(BlockState s) {
        int f = s.getValue(FACING).get2DDataValue(), p = s.getValue(SHAPE);
        VoxelShape v = STEPS[f][p];
        if (v != null) return v;
        Direction d = s.getValue(FACING);
        VoxelShape out = Shapes.empty();
        int n = 8;
        for (int i = 0; i < n; i++) {
            double t0 = i / (double) n, t1 = (i + 1) / (double) n;
            double h = RampProfiles.height(p, (t0 + t1) / 2);
            if (h < 0.02) continue;
            double y = Math.max(0.0625, Math.round(h * 16) / 16.0);
            double a0 = t0, a1 = t1;
            VoxelShape slice = switch (d) {
                case NORTH -> Shapes.box(0, 0, 1 - a1, 1, y, 1 - a0);
                case SOUTH -> Shapes.box(0, 0, a0, 1, y, a1);
                case EAST -> Shapes.box(a0, 0, 0, a1, y, 1);
                default -> Shapes.box(1 - a1, 0, 0, 1 - a0, y, 1);
            };
            out = Shapes.or(out, slice);
        }
        if (out.isEmpty()) out = Shapes.box(0, 0, 0, 1, 0.0625, 1);
        STEPS[f][p] = out.optimize();
        return STEPS[f][p];
    }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return steps(s);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        if (ctx instanceof EntityCollisionContext ec && ec.getEntity() != null && skater(ec.getEntity())) return Shapes.empty();
        return steps(s);
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState s) {
        return true;
    }
}
