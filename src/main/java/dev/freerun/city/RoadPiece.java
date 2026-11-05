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
 * asphalt lanes with a dashed centre line, street lights every 16 blocks, and sometimes a car
 * already driving along it.
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
        // street lights, alternating sides
        for (int a = 6; a < len - 3; a += 16) {
            int c = ((a / 16) % 2 == 0) ? 0 : 9;
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
        // traffic already on the road
        int mid = len / 2;
        BlockPos spawn = at(px(mid, 5), 1, pz(mid, 5));
        if (box.isInside(spawn) && random.nextInt(100) < 45) {
            int h = alongX ? (random.nextBoolean() ? CityLayout.EAST : CityLayout.WEST)
                    : (random.nextBoolean() ? CityLayout.SOUTH : CityLayout.NORTH);
            double laneLocal = CityLayout.laneCoord(h, line);
            double x = alongX ? ox + mid + 0.5 : city.getX() + laneLocal;
            double z = alongX ? city.getZ() + laneLocal : oz + mid + 0.5;
            CityHubEntity.spawnDrivenCar(level, city, x, oy + 1, z, h, random);
        }
    }
}
