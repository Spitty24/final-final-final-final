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
 * Where two streets cross: corner sidewalks, zebra crosswalks on every side, and either a traffic
 * light on a pole for each direction (downtown) or a stop sign for each (the outer ring).
 */
public class IntersectionPiece extends CityPieceBase {
    private final int i, j;

    public IntersectionPiece(BlockPos city, int i, int j) {
        super(CityRegistry.INTERSECTION_PIECE, city, CityLayout.XS[i], CityLayout.ZS[j], CityLayout.ROAD, 14, CityLayout.ROAD);
        this.i = i;
        this.j = j;
    }

    public IntersectionPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(CityRegistry.INTERSECTION_PIECE, tag);
        i = tag.getInt("I");
        j = tag.getInt("J");
    }

    @Override
    protected void save(CompoundTag tag) {
        tag.putInt("I", i);
        tag.putInt("J", j);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        BlockState asphalt = Blocks.BLACK_CONCRETE.defaultBlockState();
        ground(level, box, asphalt, 14);
        BlockState walk = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
        BlockState white = Blocks.WHITE_CONCRETE.defaultBlockState();
        for (int x = 0; x < 10; x++) {
            for (int z = 0; z < 10; z++) {
                boolean cx = x < 2 || x > 7, cz = z < 2 || z > 7;
                BlockState s = asphalt;
                if (cx && cz) s = walk;
                else if (cx && (z % 2 == 0)) s = white;     // crosswalks across the E-W road
                else if (cz && (x % 2 == 0)) s = white;     // crosswalks across the N-S road
                put(level, box, s, x, 0, z);
            }
        }
        BlockPos key = CityLayout.intersectionKey(city, i, j);
        boolean lights = CityLayout.hasLights(i, j);
        // one control per approach: {corner x, corner z, facing, approach exists}
        Object[][] approaches = {
                {0, 9, Direction.WEST, i > 0},                          // eastbound traffic coming from the west
                {9, 0, Direction.EAST, i < CityLayout.XS.length - 1},   // westbound, from the east
                {0, 0, Direction.NORTH, j > 0},                         // southbound, from the north
                {9, 9, Direction.SOUTH, j < CityLayout.ZS.length - 1},  // northbound, from the south
        };
        for (Object[] a : approaches) {
            if (!(Boolean) a[3]) continue;
            int x = (Integer) a[0], z = (Integer) a[1];
            Direction f = (Direction) a[2];
            if (lights) {
                fill(level, box, Blocks.IRON_BARS.defaultBlockState(), x, 1, z, x, 3, z);
                BlockPos p = at(x, 4, z);
                if (box.isInside(p)) {
                    level.setBlock(p, CityRegistry.TRAFFIC_LIGHT.defaultBlockState().setValue(TrafficLightBlock.FACING, f),
                            net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
                    if (level.getBlockEntity(p) instanceof CityLightBlockEntity be) be.setIntersection(key);
                }
            } else {
                put(level, box, CityRegistry.STOP_SIGN.defaultBlockState().setValue(StopSignBlock.FACING, f), x, 1, z);
            }
        }
    }
}
