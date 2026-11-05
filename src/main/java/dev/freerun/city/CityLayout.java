package dev.freerun.city;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The city plan, shared by world generation, the NPC drivers, the pedestrians and the phone map.
 * Everything is relative to the city origin (its north-west corner at street level).
 *
 * An 18 x 18 grid of city blocks, about 1,084 blocks across. Roads are {@link #ROAD} wide (a
 * 2-block sidewalk, two 3-block lanes with right-hand traffic, another sidewalk). Most blocks are
 * 38 x 38; five columns and five rows are wide (80), which makes room for the big stores, the
 * factory, strip malls and City Hall. Blocks that aren't one big building are split into 38 x 38
 * lots with a 4-block alley between them.
 *
 *   centre          downtown office towers around City Hall and the central park
 *   inner ring      the commercial district: MegaMart, Hargrove & Finch, strip malls, fast food,
 *                   dessert shops, the Double Quick, the Roadhouse, parking lots, a police precinct
 *   south-west      the hood                     south-east    the welding factory
 *   everywhere else suburbs, with parks, corner stores and precincts scattered through them
 */
public final class CityLayout {
    public static final int ROAD = 10;
    public static final int CELLS = 18;
    private static final int[] WIDE = {2, 6, 9, 12, 16};
    /** West edge of each north-south road (x), and north edge of each east-west road (z). */
    public static final int[] XS = new int[CELLS + 1];
    public static final int[] ZS = XS;
    public static final int SIZE;
    public static final int HALF;

    public enum District {
        SUBURB("Suburbs", 0x6FBF5A), OFFICE("Downtown", 0x7F9FC8), PARK("Park", 0x3E9B3E), CITY_HALL("City Hall", 0xE8DDB0),
        ROADHOUSE("Texas Roadhouse", 0xB5653A), GENERAL_STORE("General Store", 0x9A6B3F), HOOD("The Hood", 0x8A7A6A),
        FACTORY("Welding Factory", 0x9A9A9A), DOUBLE_QUICK("Double Quick", 0xD8262E), MEGAMART("MegaMart", 0x2E9E6B),
        DEPARTMENT("Hargrove & Finch", 0x6A3FA0), STRIP_MALL("Strip Mall", 0xD9A441), BURGER("Big Bun Burgers", 0xE0601E),
        CHICKEN("Cluck Shack", 0xC8402A), DESSERT("Sugar Bayou Sweets", 0xF07AB8), POLICE("Police", 0x2F4FA8),
        PARKING("Parking", 0x505056), ARCADE("Pixel Bayou Arcade", 0x9A30E0), BAR("The Rusty Pelican", 0x3A6E8A),
        FURNITURE("Bayou Home Furnishings", 0xB07A4A);

        public final String title;
        public final int color;

        District(String title, int color) {
            this.title = title;
            this.color = color;
        }

        public boolean landmark() {
            return this != SUBURB && this != OFFICE && this != HOOD && this != PARKING && this != PARK;
        }
    }

    /** One buildable lot: local corner, size, what goes on it, and a variant number. */
    public record Lot(int lx, int lz, int w, int d, District district, int variant) {
        public BlockPos centre() {
            return new BlockPos(lx + w / 2, 1, lz + d / 2);
        }

        public boolean contains(double x, double z) {
            return x >= lx && x < lx + w && z >= lz && z < lz + d;
        }
    }

    private static final List<Lot> LOTS = new ArrayList<>();
    @SuppressWarnings("unchecked")
    private static final List<Lot>[] BY_CELL = new List[CELLS * CELLS];

    static {
        int x = 0;
        for (int i = 0; i <= CELLS; i++) {
            XS[i] = x;
            if (i < CELLS) x += ROAD + (wide(i) ? 80 : 38);
        }
        SIZE = XS[CELLS] + ROAD;
        HALF = SIZE / 2;
        for (int r = 0; r < CELLS; r++) for (int c = 0; c < CELLS; c++) plan(c, r);
        spreadOut();
        for (List<Lot> cell : BY_CELL) LOTS.addAll(cell);
    }

    private CityLayout() {}

    private static boolean wide(int i) {
        for (int w : WIDE) if (w == i) return true;
        return false;
    }

    private static int hash(int a, int b, int salt) {
        int h = a * 73856093 ^ b * 19349663 ^ salt * 83492791;
        h ^= h >>> 13;
        h *= 0x5bd1e995;
        return (h ^ (h >>> 15)) & 0x7fffffff;
    }

    // ------------------------------------------------------------------ zoning

    private static District bigCell(int c, int r) {
        if (c == 12 && r == 12) return District.FACTORY;
        int[][] mega = {{6, 6}, {6, 12}, {16, 2}};
        int[][] dept = {{12, 6}, {2, 9}};
        int[][] mall = {{9, 6}, {6, 9}, {12, 9}, {9, 2}, {9, 16}, {16, 9}};
        for (int[] p : mega) if (p[0] == c && p[1] == r) return District.MEGAMART;
        for (int[] p : dept) if (p[0] == c && p[1] == r) return District.DEPARTMENT;
        for (int[] p : mall) if (p[0] == c && p[1] == r) return District.STRIP_MALL;
        return null;
    }

    private static District small(int c, int r, int sub) {
        double ring = Math.max(Math.abs(c - 8.5), Math.abs(r - 8.5));
        int h = hash(c, r, sub) % 100;
        if ((c == 4 || c == 13) && (r == 4 || r == 13)) return District.POLICE;
        if (c == 8 && r == 11) return District.POLICE;
        if (ring <= 2.5) return h < 8 ? District.PARKING : h < 12 ? District.DESSERT : District.OFFICE;
        if (c <= 7 && r >= 11) return h < 8 ? District.PARK : h < 13 ? District.DOUBLE_QUICK : h < 16 ? District.CHICKEN : District.HOOD;
        if (c >= 13 && r >= 13 && ring <= 6.5) return h < 50 ? District.PARKING : District.SUBURB;   // around the factory
        if (ring <= 4.5) {
            District[] mix = {District.PARKING, District.BURGER, District.CHICKEN, District.DESSERT, District.DOUBLE_QUICK,
                    District.GENERAL_STORE, District.ROADHOUSE, District.OFFICE, District.PARKING, District.PARK, District.ARCADE,
                    District.OFFICE, District.BAR, District.FURNITURE, District.PARKING, District.ARCADE, District.BAR, District.FURNITURE};
            return mix[h % mix.length];
        }
        if (h < 74) return District.SUBURB;
        if (h < 81) return District.PARK;
        if (h < 85) return District.DOUBLE_QUICK;
        if (h < 88) return District.BURGER;
        if (h < 90) return District.CHICKEN;
        if (h < 92) return District.DESSERT;
        if (h < 94) return District.GENERAL_STORE;
        if (h < 96) return District.ROADHOUSE;
        if (h < 97) return District.BAR;
        if (h < 98) return District.FURNITURE;
        return District.PARKING;
    }

    private static void plan(int c, int r) {
        List<Lot> cell = new ArrayList<>();
        int lx = XS[c] + ROAD, lz = ZS[r] + ROAD, w = lotW(c), d = lotD(r);
        if (c == 9 && r == 9) {   // City Hall on the north half, the central park on the south
            cell.add(new Lot(lx, lz, w, 38, District.CITY_HALL, 0));
            cell.add(new Lot(lx, lz + 42, w, 38, District.PARK, 1));
        } else {
            District big = w == 80 && d == 80 ? bigCell(c, r) : null;
            if (big != null) {
                cell.add(new Lot(lx, lz, w, d, big, hash(c, r, 99) % 16));
            } else {
                int sub = 0;
                for (int sz = 0; sz < d; sz += 42) {
                    for (int sx = 0; sx < w; sx += 42) {
                        District dist = small(c, r, sub);
                        int variant = dist == District.HOOD ? (sz / 42) % 2 : hash(c, r, sub + 7) % 16;
                        cell.add(new Lot(lx + sx, lz + sz, 38, 38, dist, variant));
                        sub++;
                    }
                }
                // wide offices become one 80-wide lot with twin towers
                if (w == 80 && d == 38 && cell.size() == 2 && cell.get(0).district() == District.OFFICE && cell.get(1).district() == District.OFFICE) {
                    cell.clear();
                    cell.add(new Lot(lx, lz, 80, 38, District.OFFICE, 0));
                }
            }
        }
        BY_CELL[r * CELLS + c] = cell;
    }

    /** Store types that shouldn't bunch up: no two of the same within this many blocks. */
    private static final class Spread {   // a holder, so it exists before the static plan above runs
        static final District[] TYPES = {District.BURGER, District.CHICKEN, District.DESSERT, District.DOUBLE_QUICK,
                District.GENERAL_STORE, District.ROADHOUSE, District.ARCADE, District.BAR, District.FURNITURE};
    }
    private static final int SPACING = 230;

    /**
     * Walks the plan and swaps any store that would sit too close to another of its kind for a
     * different store that isn't nearby, or failing that for offices, parking or a park (in the
     * commercial ring), apartments (in the hood) or houses (everywhere else).
     */
    private static void spreadOut() {
        List<Lot> placed = new ArrayList<>();
        for (int r = 0; r < CELLS; r++) {
            for (int c = 0; c < CELLS; c++) {
                List<Lot> cell = BY_CELL[r * CELLS + c];
                for (int i = 0; i < cell.size(); i++) {
                    Lot l = cell.get(i);
                    int k = indexOf(l.district());
                    if (k < 0) continue;
                    District pick = null;
                    int start = hash(c, r, i + 31) % Spread.TYPES.length;
                    for (int n = -1; n < Spread.TYPES.length && pick == null; n++) {
                        District cand = n < 0 ? l.district() : Spread.TYPES[(start + n) % Spread.TYPES.length];
                        if (farFromOthers(placed, cand, l)) pick = cand;
                    }
                    if (pick == null) pick = filler(c, r, i);
                    int variant = pick == District.HOOD ? ((l.lz() - lotZ(r)) / 42) % 2 : l.variant();
                    Lot replaced = new Lot(l.lx(), l.lz(), l.w(), l.d(), pick, variant);
                    cell.set(i, replaced);
                    if (indexOf(pick) >= 0) placed.add(replaced);
                }
            }
        }
    }

    private static int indexOf(District d) {
        for (int k = 0; k < Spread.TYPES.length; k++) if (Spread.TYPES[k] == d) return k;
        return -1;
    }

    private static boolean farFromOthers(List<Lot> placed, District d, Lot at) {
        BlockPos c = at.centre();
        for (Lot p : placed) {
            if (p.district() != d) continue;
            double dx = p.centre().getX() - c.getX(), dz = p.centre().getZ() - c.getZ();
            if (dx * dx + dz * dz < (double) SPACING * SPACING) return false;
        }
        return true;
    }

    private static District filler(int c, int r, int i) {
        double ring = Math.max(Math.abs(c - 8.5), Math.abs(r - 8.5));
        int h = hash(c, r, i + 57) % 10;
        if (c <= 7 && r >= 11) return h < 2 ? District.PARK : District.HOOD;
        if (ring <= 4.5) return h < 4 ? District.OFFICE : h < 7 ? District.PARKING : District.PARK;
        return h < 2 ? District.PARK : District.SUBURB;
    }

    public static List<Lot> lots() {
        return LOTS;
    }

    public static List<Lot> lotsIn(int col, int row) {
        return BY_CELL[row * CELLS + col];
    }

    public static int cols() {
        return CELLS;
    }

    public static int rows() {
        return CELLS;
    }

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

    /** Index of the cell column containing local x (or -1 if on a road / outside). */
    public static int colAt(double lx) {
        for (int c = 0; c < CELLS; c++) if (lx >= lotX(c) && lx < lotX(c) + lotW(c)) return c;
        return -1;
    }

    public static int rowAt(double lz) {
        return colAt(lz);
    }

    /** Arterial roads (every third line) get traffic lights; everywhere else, stop signs. */
    public static boolean hasLights(int i, int j) {
        boolean edge = i == 0 || j == 0 || i == CELLS || j == CELLS;
        return !edge && (i % 3 == 0 || j % 3 == 0);
    }

    public static Lot lotAt(double lx, double lz) {
        int c = colAt(lx), r = rowAt(lz);
        if (c < 0 || r < 0) return null;
        for (Lot l : lotsIn(c, r)) if (l.contains(lx, lz)) return l;
        return null;
    }

    public static District districtAt(double lx, double lz) {
        Lot l = lotAt(lx, lz);
        return l == null ? null : l.district();
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
            case EAST -> ZS[k] + 6.5;
            case WEST -> ZS[k] + 3.5;
            case SOUTH -> XS[k] + 3.5;
            default -> XS[k] + 6.5;
        };
    }

    public static boolean canLeave(int i, int j, int h) {
        return switch (h) {
            case EAST -> i < CELLS;
            case WEST -> i > 0;
            case SOUTH -> j < CELLS;
            default -> j > 0;
        };
    }

    public static Vec3 exitPoint(int i, int j, int h, double beyond) {
        return switch (h) {
            case EAST -> new Vec3(XS[i] + ROAD + beyond, 0, laneCoord(EAST, j));
            case WEST -> new Vec3(XS[i] - beyond, 0, laneCoord(WEST, j));
            case SOUTH -> new Vec3(laneCoord(SOUTH, i), 0, ZS[j] + ROAD + beyond);
            default -> new Vec3(laneCoord(NORTH, i), 0, ZS[j] - beyond);
        };
    }

    public static Vec3 intersectionCentre(int i, int j) {
        return new Vec3(XS[i] + ROAD / 2.0, 0, ZS[j] + ROAD / 2.0);
    }

    public static BlockPos intersectionKey(BlockPos origin, int i, int j) {
        return origin.offset(XS[i] + ROAD / 2, 0, ZS[j] + ROAD / 2);
    }

    // ------------------------------------------------------------------ places

    /** Centres of every lot of a district (local, ground level). */
    public static List<BlockPos> spots(District d) {
        List<BlockPos> out = new ArrayList<>();
        for (Lot l : LOTS) if (l.district() == d) out.add(l.centre());
        return out;
    }

    /** Centres of the lots of a district within {@code radius} of a local point. */
    public static List<BlockPos> spotsNear(District d, double lx, double lz, double radius) {
        List<BlockPos> out = new ArrayList<>();
        double r2 = radius * radius;
        for (Lot l : LOTS) {
            if (l.district() != d) continue;
            BlockPos c = l.centre();
            double dx = c.getX() - lx, dz = c.getZ() - lz;
            if (dx * dx + dz * dz < r2) out.add(c);
        }
        return out;
    }
}
