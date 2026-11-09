package dev.freerun.skate;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Builds skate park features, used by both the kits you place and the city's skate park. Works in
 * a local frame: u across (negative = left), v forward (away from whoever placed it), y up from
 * the ground the feature stands on.
 */
public final class ParkBuilder {
    @FunctionalInterface
    public interface Put {
        void put(int u, int y, int v, BlockState s);
    }

    public enum Kind {
        QUARTER_PIPE("Quarter Pipe", 5, 5), HALF_PIPE("Half Pipe", 7, 14), KICKER("Kicker Ramp", 3, 2),
        FUNBOX("Funbox", 5, 8), BANK("Bank Ramp", 5, 4), RAIL("Grind Rail", 1, 6),
        STAIR_SET("Stair Set & Handrail", 5, 10), MANUAL_PAD("Manual Pad", 3, 6), LEDGE("Grind Ledge", 2, 8);

        public final String title;
        public final int width, length;

        Kind(String title, int width, int length) {
            this.title = title;
            this.width = width;
            this.length = length;
        }
    }

    private ParkBuilder() {}

    static BlockState ramp(Direction facing, int shape) {
        return Skating.RAMP.defaultBlockState().setValue(SkateRampBlock.FACING, facing).setValue(SkateRampBlock.SHAPE, shape);
    }

    static BlockState park() {
        return Skating.PARK_CONCRETE.defaultBlockState();
    }

    static BlockState rail(Direction forward, int slopeTowardMinusV) {
        int slope = 0;
        if (slopeTowardMinusV != 0) {
            boolean positive = forward.getAxisDirection() == Direction.AxisDirection.POSITIVE;
            slope = positive ? 2 : 1;
        }
        return Skating.RAIL_BLOCK.defaultBlockState().setValue(GrindRailBlock.AXIS, forward.getAxis()).setValue(GrindRailBlock.SLOPE, slope);
    }

    /** A quarter pipe whose curve starts at v = v0 and rises toward +v ({@code up}), 3 high, with a 2-deep deck. */
    static void quarterPipe(Put put, int u0, int u1, int v0, int dir, Direction forward) {
        Direction facing = dir > 0 ? forward : forward.getOpposite();
        for (int u = u0; u <= u1; u++) {
            for (int col = 0; col < 3; col++) {
                int v = v0 + dir * col;
                for (int row = 0; row < 3; row++) {
                    int k = RampProfiles.qpKind(col, row);
                    if (k == 1) put.put(u, row, v, ramp(facing, RampProfiles.qp(col, row)));
                    else if (k == 2) put.put(u, row, v, park());
                }
            }
            for (int dv = 3; dv <= 4; dv++) for (int y = 0; y < 3; y++) put.put(u, y, v0 + dir * dv, park());
            put.put(u, 3, v0 + dir * 4, Blocks.IRON_BARS.defaultBlockState());   // a railing at the back of the deck
        }
    }

    public static void build(Kind kind, Direction f, Put put) {
        switch (kind) {
            case QUARTER_PIPE -> {
                floor(put, -2, 2, 0, 4);
                quarterPipe(put, -2, 2, 0, 1, f);
            }
            case HALF_PIPE -> {
                floor(put, -3, 3, 0, 13);
                quarterPipe(put, -3, 3, 4, -1, f);    // the near wall, rising back toward you, deck at v 0..1
                quarterPipe(put, -3, 3, 9, 1, f);     // the far wall, deck at v 12..13
            }
            case KICKER -> {
                for (int u = -1; u <= 1; u++) {
                    put.put(u, 0, 0, ramp(f, RampProfiles.BANK_LOW));
                    put.put(u, 0, 1, ramp(f, RampProfiles.BANK_HIGH));
                }
            }
            case FUNBOX -> {
                for (int u = -2; u <= 2; u++) {
                    put.put(u, 0, 0, ramp(f, RampProfiles.BANK_LOW));
                    put.put(u, 0, 1, ramp(f, RampProfiles.BANK_HIGH));
                    for (int v = 2; v <= 5; v++) put.put(u, 0, v, park());
                    put.put(u, 0, 6, ramp(f.getOpposite(), RampProfiles.BANK_HIGH));
                    put.put(u, 0, 7, ramp(f.getOpposite(), RampProfiles.BANK_LOW));
                }
                for (int v = 2; v <= 5; v++) put.put(0, 1, v, rail(f, 0));   // a rail along the top
            }
            case BANK -> {
                for (int u = -2; u <= 2; u++) {
                    put.put(u, 0, 0, ramp(f, RampProfiles.BANK_LOW));
                    put.put(u, 0, 1, ramp(f, RampProfiles.BANK_HIGH));
                    put.put(u, 0, 2, park());
                    put.put(u, 0, 3, park());
                }
            }
            case RAIL -> {
                for (int v = 0; v < 6; v++) put.put(0, 0, v, rail(f, 0));
            }
            case MANUAL_PAD -> {
                for (int u = -1; u <= 1; u++) for (int v = 0; v < 6; v++) put.put(u, 0, v, park());
            }
            case LEDGE -> {
                for (int u = 0; u <= 1; u++) for (int v = 0; v < 8; v++) put.put(u, 0, v, park());
            }
            case STAIR_SET -> stairSet(put, f);
        }
    }

    /** A 3-high platform, three steps down (stair blocks) and a handrail down the middle, then a run-out. */
    private static void stairSet(Put put, Direction f) {
        BlockState stair = Blocks.POLISHED_ANDESITE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, f.getOpposite());
        for (int u = -2; u <= 2; u++) {
            for (int v = 0; v <= 2; v++) for (int y = 0; y < 3; y++) put.put(u, y, v, park());
            for (int k = 0; k < 3; k++) {
                int v = 3 + k, top = 2 - k;
                for (int y = 0; y < top; y++) put.put(u, y, v, park());
                put.put(u, top, v, stair);
            }
        }
        // the handrail: flat along the top of the platform edge, then down the steps
        for (int k = 0; k < 3; k++) put.put(0, 3 - k, 3 + k, rail(f, 1));
        floor(put, -2, 2, 6, 9);
    }

    /** A smooth concrete floor (replacing the ground block) under part of a feature. */
    private static void floor(Put put, int u0, int u1, int v0, int v1) {
        for (int u = u0; u <= u1; u++) for (int v = v0; v <= v1; v++) put.put(u, -1, v, park());
    }
}
