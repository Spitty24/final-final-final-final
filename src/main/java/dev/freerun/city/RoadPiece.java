package dev.freerun.city;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * One block of street between two intersections: sidewalks with curbs on both sides, two
 * asphalt lanes with a dashed centre line, and street lights every 16 blocks.
 */
public class RoadPiece extends CityPieceBase {
    private final boolean alongX;
    private final int line, seg;

    public RoadPiece(BlockPos city, boolean alongX, int line, int seg) {
        super(CityRegistry.ROAD_PIECE, city,
                alongX ? CityLayout.XS[seg] + CityLayout.ROAD : CityLayout.XS[line],
                alongX ? CityLayout.ZS[line] : CityLayout.ZS[seg] + CityLayout.ROAD,
                alongX ? CityLayout.XS[seg + 1] - CityLayout.XS[seg] - CityLayout.ROAD : CityLayout.ROAD, 14,
                alongX ? CityLayout.ROAD : CityLayout.ZS[seg + 1] - CityLayout.ZS[seg] - CityLayout.ROAD);
        this.alongX = alongX;
        this.line = line;
        this.seg = seg;
    }

    public RoadPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(CityRegistry.ROAD_PIECE, tag);
        alongX = tag.getBoolean("AlongX");
        line = tag.getInt("Line");
        seg = tag.getInt("Seg");
    }

    @Override
    protected void save(CompoundTag tag) {
        tag.putBoolean("AlongX", alongX);
        tag.putInt("Line", line);
        tag.putInt("Seg", seg);
    }

    /** Local coordinate along the road and across it, back to piece x/z. */
    private int px(int along, int across) {
        return alongX ? along : across;
    }

    private int pz(int along, int across) {
        return alongX ? across : along;
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        BlockState asphalt = Blocks.BLACK_CONCRETE.defaultBlockState();
        ground(level, box, asphalt, 14);
        int len = alongX ? w : d;
        BlockState walk = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
        BlockState curb = Blocks.POLISHED_ANDESITE.defaultBlockState();
        BlockState yellow = Blocks.YELLOW_CONCRETE.defaultBlockState();
        BlockState patch = Blocks.GRAY_CONCRETE.defaultBlockState();
        for (int a = 0; a < len; a++) {
            for (int c = 0; c < CityLayout.ROAD; c++) {
                BlockState s;
                if (c == 0 || c == 9) s = walk;
                else if (c == 1 || c == 8) s = curb;
                else if ((c == 4 || c == 5) && Math.floorMod(a, 4) < 2) s = yellow;
                else if ((a * 13 + c * 7 + line * 5) % 29 == 0) s = patch;
                else s = asphalt;
                put(level, box, s, px(a, c), 0, pz(a, c));
            }
        }
        BusRoutes.Stop stop = BusRoutes.stopOn(alongX, line, seg);
        int shelterSide = stop == null ? -1 : stop.alongX == stop.positive ? 9 : 0;
        // street lights, alternating sides
        for (int a = 6; a < len - 3; a += 16) {
            int c = ((a / 16) % 2 == 0) ? 0 : 9;
            if (c == shelterSide && nearShelter(stop, a, len)) continue;
            int x = px(a, c), z = pz(a, c);
            fill(level, box, Blocks.IRON_BARS.defaultBlockState(), x, 1, z, x, 4, z);
            put(level, box, CityRegistry.STREET_LAMP, x, 5, z);
        }
        // a fire hydrant and a bench now and then
        if (len > 20) {
            int a = len / 2 + 3;
            put(level, box, Blocks.RED_CONCRETE_POWDER.defaultBlockState(), px(a, 0), 1, pz(a, 0));
            Direction faceRoad = alongX ? Direction.SOUTH : Direction.EAST;
            put(level, box, Blocks.SPRUCE_STAIRS.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.StairBlock.FACING, faceRoad.getOpposite()), px(a - 6, 0), 1, pz(a - 6, 0));
        }
        if (stop != null) shelter(level, box, stop, len, shelterSide);
    }

    /** Along-the-road block for a distance into the street in the bus's direction of travel. */
    private static int alongBlock(BusRoutes.Stop stop, int b, int len) {
        return stop.positive ? b : len - 1 - b;
    }

    private static boolean nearShelter(BusRoutes.Stop stop, int a, int len) {
        for (int b = BusRoutes.SHELTER_FROM - 1; b <= BusRoutes.SHELTER_TO + 3; b++) if (alongBlock(stop, b, len) == a) return true;
        return false;
    }

    /**
     * A bus shelter on the curb side: a glass end at each side, a bench facing the road, a roof
     * striped in the route's colour, and the stop sign on its post just beyond, where people wait.
     */
    private void shelter(WorldGenLevel level, BoundingBox box, BusRoutes.Stop stop, int len, int c) {
        int curb = c == 9 ? 8 : 1;
        // which way is the lot (behind the bench) and which way is the road
        Direction toLot = alongX ? (c == 9 ? Direction.SOUTH : Direction.NORTH) : (c == 9 ? Direction.EAST : Direction.WEST);
        Direction along = alongX ? Direction.EAST : Direction.SOUTH;
        BlockState roof = Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState();
        BlockState stripe = routeConcrete(stop.route.color);
        BlockState pane = Blocks.GLASS_PANE.defaultBlockState();
        for (int b = BusRoutes.SHELTER_FROM; b <= BusRoutes.SHELTER_TO; b++) {
            int a = alongBlock(stop, b, len);
            boolean end = b == BusRoutes.SHELTER_FROM || b == BusRoutes.SHELTER_TO;
            for (int y = 1; y <= 2; y++) {
                put(level, box, Blocks.AIR, px(a, c), y, pz(a, c));
                put(level, box, Blocks.AIR, px(a, curb), y, pz(a, curb));
            }
            if (end) {
                // glass sides on the sidewalk, a post at the curb
                BlockState side = pane.setValue(net.minecraft.world.level.block.IronBarsBlock.EAST, !alongX)
                        .setValue(net.minecraft.world.level.block.IronBarsBlock.WEST, !alongX)
                        .setValue(net.minecraft.world.level.block.IronBarsBlock.NORTH, alongX)
                        .setValue(net.minecraft.world.level.block.IronBarsBlock.SOUTH, alongX);
                put(level, box, side, px(a, c), 1, pz(a, c));
                put(level, box, side, px(a, c), 2, pz(a, c));
                put(level, box, Blocks.IRON_BARS, px(a, curb), 1, pz(a, curb));
                put(level, box, Blocks.IRON_BARS, px(a, curb), 2, pz(a, curb));
            } else {
                put(level, box, Blocks.SPRUCE_STAIRS.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.StairBlock.FACING, toLot), px(a, c), 1, pz(a, c));
            }
            put(level, box, end ? stripe : roof, px(a, c), 3, pz(a, c));
            put(level, box, stripe, px(a, curb), 3, pz(a, curb));
        }
        // the sign post where you wait, facing oncoming buses
        int a = alongBlock(stop, BusRoutes.SHELTER_TO + 2, len);
        Direction facing = stop.positive ? along.getOpposite() : along;
        put(level, box, Blocks.IRON_BARS, px(a, curb), 1, pz(a, curb));
        put(level, box, CityRegistry.BUS_STOP_SIGN.defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, facing), px(a, curb), 2, pz(a, curb));
        // a painted bus box on the road
        int lane = c == 9 ? 7 : 2;
        for (int b = (int) BusRoutes.STOP_AT - 4; b <= (int) BusRoutes.STOP_AT + 4; b++) {
            int aa = alongBlock(stop, b, len);
            if (b == (int) BusRoutes.STOP_AT - 4 || b == (int) BusRoutes.STOP_AT + 4 || (b & 1) == 0) put(level, box, Blocks.WHITE_CONCRETE, px(aa, lane), 0, pz(aa, lane));
        }
    }

    private static BlockState routeConcrete(int rgb) {
        return switch (rgb) {
            case 0xE8B820 -> Blocks.YELLOW_CONCRETE.defaultBlockState();
            case 0x2E7FD0 -> Blocks.BLUE_CONCRETE.defaultBlockState();
            case 0x3EA055 -> Blocks.LIME_CONCRETE.defaultBlockState();
            default -> Blocks.RED_CONCRETE.defaultBlockState();
        };
    }
}
