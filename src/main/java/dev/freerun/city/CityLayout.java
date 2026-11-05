package dev.freerun.city;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The city plan, shared by world generation, the NPC drivers, the pedestrians and the phone map.
 * Everything is relative to the city origin (its north-west corner at street level).
 *
 * Five road lines each way. Roads are {@link #ROAD} wide: a 2-block sidewalk, two 3-block lanes
 * (right-hand traffic) and another sidewalk. Between the roads are 4 x 4 city blocks ("cells").
 *
 *            c0 (38)    c1 (38)    c2 (80)              c3 (38)
 *   r0 (38)  suburb     suburb     park + city hall      suburb
 *   r1 (38)  suburb     offices    office towers         offices
 *   r2 (38)  suburb     offices    Texas Roadhouse       General Store
 *   r3 (80)  the hood   the hood   the welding factory   Double Quick
 */
public final class CityLayout {
    public static final int ROAD = 10;
    /** West edge of each north-south road (x), and north edge of each east-west road (z). */
    public static final int[] XS = {0, 48, 96, 186, 234};
    public static final int[] ZS = {0, 48, 96, 144, 234};
    public static final int SIZE = 244;
    public static final int HALF = SIZE / 2;

    public enum District {
        SUBURB("Suburbs", 0x6FBF5A), OFFICE("Downtown", 0x7F9FC8), PARK("City Hall & Park", 0x3E9B3E),
        ROADHOUSE("Texas Roadhouse", 0xB5653A), GENERAL_STORE("General Store", 0x9A6B3F),
        HOOD("The Hood", 0x8A7A6A), FACTORY("Welding Factory", 0x9A9A9A), DOUBLE_QUICK("Double Quick", 0xD8262E);

        public final String title;
        public final int color;

        District(String title, int color) {
            this.title = title;
            this.color = color;
        }
    }

    private static final District[][] CELLS = {   // [row][col]
            {District.SUBURB, District.SUBURB, District.PARK, District.SUBURB},
            {District.SUBURB, District.OFFICE, District.OFFICE, District.OFFICE},
            {District.SUBURB, District.OFFICE, District.ROADHOUSE, District.GENERAL_STORE},
            {District.HOOD, District.HOOD, District.FACTORY, District.DOUBLE_QUICK},
    };

    private CityLayout() {}

    public static District district(int col, int row) {
        return CELLS[row][col];
    }

    public static int cols() {
        return XS.length - 1;
    }

    public static int rows() {
        return ZS.length - 1;
    }

    /** Lot (inside the roads) of cell (col,row): local x0, z0, width, depth. */
    public static int lotX(int col) {
        return XS[col] + ROAD;
    }

    public static int lotZ(int row) {
        return ZS[row] + ROAD;
    }

    public static int lotW(int col) {
        return XS[col + 1] - XS[col] - ROAD;
    }

    public static int lotD(int row) {
        return ZS[row + 1] - ZS[row] - ROAD;
    }

    /** Main intersections get traffic lights; the outer ring has stop signs. */
    public static boolean hasLights(int i, int j) {
        return i > 0 && i < XS.length - 1 && j > 0 && j < ZS.length - 1;
    }

    /** Which district (if any) a local position is in, for the map. -1 = road. */
    public static District districtAt(double lx, double lz) {
        int col = -1, row = -1;
        for (int c = 0; c < cols(); c++) if (lx >= lotX(c) && lx < lotX(c) + lotW(c)) col = c;
        for (int r = 0; r < rows(); r++) if (lz >= lotZ(r) && lz < lotZ(r) + lotD(r)) row = r;
        return col < 0 || row < 0 ? null : district(col, row);
    }

    public static boolean onRoad(double lx, double lz) {
        if (lx < 0 || lz < 0 || lx >= SIZE || lz >= SIZE) return false;
        for (int x : XS) if (lx >= x && lx < x + ROAD) return true;
        for (int z : ZS) if (lz >= z && lz < z + ROAD) return true;
        return false;
    }

    // ------------------------------------------------------------------ driving

    /** Headings: 0 east (+x), 1 south (+z), 2 west (-x), 3 north (-z). */
    public static final int EAST = 0, SOUTH = 1, WEST = 2, NORTH = 3;

    public static int dx(int h) {
        return h == EAST ? 1 : h == WEST ? -1 : 0;
    }

    public static int dz(int h) {
        return h == SOUTH ? 1 : h == NORTH ? -1 : 0;
    }

    public static float yaw(int h) {
        return switch (h) {
            case EAST -> -90.0f;
            case SOUTH -> 0.0f;
            case WEST -> 90.0f;
            default -> 180.0f;
        };
    }

    /** Lane centre line (local) for travel in heading h along road index k. */
    public static double laneCoord(int h, int k) {
        return switch (h) {
            case EAST -> ZS[k] + 6.5;    // eastbound keeps to the south half
            case WEST -> ZS[k] + 3.5;
            case SOUTH -> XS[k] + 3.5;   // southbound keeps to the west half
            default -> XS[k] + 6.5;
        };
    }

    /** Can you leave intersection (i,j) heading h? */
    public static boolean canLeave(int i, int j, int h) {
        return switch (h) {
            case EAST -> i < XS.length - 1;
            case WEST -> i > 0;
            case SOUTH -> j < ZS.length - 1;
            default -> j > 0;
        };
    }

    /** Where a car leaving (i,j) heading h is aiming: just past the intersection, in its lane (local). */
    public static Vec3 exitPoint(int i, int j, int h, double beyond) {
        return switch (h) {
            case EAST -> new Vec3(XS[i] + ROAD + beyond, 0, laneCoord(EAST, j));
            case WEST -> new Vec3(XS[i] - beyond, 0, laneCoord(WEST, j));
            case SOUTH -> new Vec3(laneCoord(SOUTH, i), 0, ZS[j] + ROAD + beyond);
            default -> new Vec3(laneCoord(NORTH, i), 0, ZS[j] - beyond);
        };
    }

    /** Local centre of intersection (i,j). */
    public static Vec3 intersectionCentre(int i, int j) {
        return new Vec3(XS[i] + ROAD / 2.0, 0, ZS[j] + ROAD / 2.0);
    }

    public static BlockPos intersectionKey(BlockPos origin, int i, int j) {
        return origin.offset(XS[i] + ROAD / 2, 0, ZS[j] + ROAD / 2);
    }

    // ------------------------------------------------------------------ places

    /** A few spots in every cell of a district (local, ground level), for citizens to head for. */
    public static List<BlockPos> spots(District d) {
        List<BlockPos> out = new ArrayList<>();
        for (int r = 0; r < rows(); r++) {
            for (int c = 0; c < cols(); c++) {
                if (district(c, r) != d) continue;
                out.add(new BlockPos(lotX(c) + lotW(c) / 2, 1, lotZ(r) + lotD(r) / 2));
            }
        }
        return out;
    }

    /** The sidewalk just outside the north side of cell (col,row) (local). */
    public static BlockPos frontWalk(int col, int row) {
        return new BlockPos(lotX(col) + lotW(col) / 2, 1, lotZ(row) - 1);
    }
}
