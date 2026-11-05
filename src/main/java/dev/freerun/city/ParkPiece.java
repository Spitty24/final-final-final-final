package dev.freerun.city;

import dev.freerun.world.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * City Hall (columns, steps, flags, a council chamber and a clerk's counter) on the west half of
 * the block, and the city park on the east: paths, a fountain, trees, flower beds, benches, and a
 * statue of the town's favourite sausage.
 */
public class ParkPiece extends CityPieceBase {
    public ParkPiece(BlockPos city, int lx, int lz, int w, int d) {
        super(CityRegistry.PARK_PIECE, city, lx, lz, w, 20, d);
    }

    public ParkPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(CityRegistry.PARK_PIECE, tag);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        ground(level, box, Blocks.GRASS_BLOCK.defaultBlockState(), 20);
        cityHall(level, box, 2, 5);
        park(level, box, 40, 0);
    }

    private void cityHall(WorldGenLevel level, BoundingBox box, int x0, int z0) {
        int x1 = x0 + 32, z1 = z0 + 28;
        BlockState wall = Blocks.SMOOTH_SANDSTONE.defaultBlockState();
        BlockState pillar = Blocks.QUARTZ_PILLAR.defaultBlockState();
        fill(level, box, Blocks.POLISHED_DIORITE.defaultBlockState(), x0, 0, z0, x1, 0, z1);
        walls(level, box, wall, x0, 1, z0 + 4, x1, 8, z1);
        for (int x = x0 + 2; x < x1 - 1; x += 4) fill(level, box, Blocks.GLASS.defaultBlockState(), x, 3, z0 + 4, x + 1, 6, z0 + 4);
        fill(level, box, Blocks.SMOOTH_QUARTZ.defaultBlockState(), x0, 9, z0, x1, 9, z1);
        fill(level, box, Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState(), x0 + 2, 10, z0 + 2, x1 - 2, 10, z1 - 2);
        // portico: steps up, a row of columns, a pediment
        for (int x = x0 + 4; x <= x1 - 4; x++) {
            put(level, box, Blocks.SMOOTH_QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH), x, 1, z0);
        }
        fill(level, box, Blocks.SMOOTH_QUARTZ.defaultBlockState(), x0 + 4, 1, z0 + 1, x1 - 4, 1, z0 + 3);
        for (int x = x0 + 4; x <= x1 - 4; x += 3) fill(level, box, pillar, x, 2, z0 + 1, x, 8, z0 + 1);
        // doors
        int mid = (x0 + x1) / 2;
        fill(level, box, Blocks.AIR.defaultBlockState(), mid - 1, 2, z0 + 4, mid + 1, 4, z0 + 4);
        sign(level, box, mid, 7, z0, Direction.NORTH, "", "§lCITY HALL", "Est. 1887", "");
        put(level, box, Blocks.BLUE_BANNER, x0 + 1, 1, z0 + 2);
        put(level, box, Blocks.WHITE_BANNER, x1 - 1, 1, z0 + 2);
        // inside: the clerk's counter, a waiting area, the council chamber
        for (int x = mid - 4; x <= mid + 4; x++) put(level, box, facing(dev.freerun.food.FoodRegistry.COUNTER, Direction.NORTH), x, 2, z0 + 12);
        fill(level, box, Blocks.POLISHED_DIORITE.defaultBlockState(), x0 + 1, 1, z0 + 5, x1 - 1, 1, z1 - 1);
        for (int x = x0 + 3; x <= x0 + 9; x++) put(level, box, facing(BlockRegistry.CHAIR, Direction.NORTH), x, 2, z0 + 8);
        for (int x = mid - 6; x <= mid + 6; x++) put(level, box, BlockRegistry.TABLE, x, 2, z1 - 5);
        for (int x = mid - 6; x <= mid + 6; x += 2) put(level, box, facing(BlockRegistry.CHAIR, Direction.SOUTH), x, 2, z1 - 6);
        put(level, box, Blocks.LECTERN, mid, 2, z1 - 3);
        for (int x = x0 + 4; x < x1 - 2; x += 6) for (int z = z0 + 8; z < z1 - 1; z += 6) put(level, box, Blocks.SEA_LANTERN, x, 8, z);
        sign(level, box, mid, 2, z0 + 11, Direction.NORTH, "PERMITS", "LICENCES", "COMPLAINTS", "(take a number)");
        // flag pole
        fill(level, box, Blocks.IRON_BARS.defaultBlockState(), x1 + 2, 1, z0, x1 + 2, 12, z0);
        put(level, box, Blocks.BLUE_WOOL, x1 + 3, 11, z0);
        put(level, box, Blocks.WHITE_WOOL, x1 + 4, 11, z0);
        put(level, box, Blocks.RED_WOOL, x1 + 3, 10, z0);
        put(level, box, Blocks.WHITE_WOOL, x1 + 4, 10, z0);
    }

    private void park(WorldGenLevel level, BoundingBox box, int x0, int z0) {
        int x1 = w - 1, z1 = d - 1;
        int cx = (x0 + x1) / 2, cz = (z0 + z1) / 2;
        // cross paths
        fill(level, box, Blocks.GRAVEL.defaultBlockState(), cx - 1, 0, z0, cx + 1, 0, z1);
        fill(level, box, Blocks.GRAVEL.defaultBlockState(), x0, 0, cz - 1, x1, 0, cz + 1);
        // fountain
        for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
            int r = Math.max(Math.abs(dx), Math.abs(dz));
            if (r == 4) put(level, box, Blocks.STONE_BRICK_SLAB, cx + dx, 1, cz + dz);
            if (r <= 4) put(level, box, Blocks.STONE_BRICKS, cx + dx, 0, cz + dz);
            if (r < 4) put(level, box, Blocks.WATER, cx + dx, 1, cz + dz);
        }
        fill(level, box, Blocks.STONE_BRICKS.defaultBlockState(), cx, 1, cz, cx, 3, cz);
        put(level, box, Blocks.WATER, cx, 4, cz);
        // the statue: Big Boudin, in bronze-ish terracotta, on a plinth
        int sx = x0 + 6, sz = z1 - 6;
        fill(level, box, Blocks.POLISHED_ANDESITE.defaultBlockState(), sx - 1, 1, sz - 1, sx + 1, 1, sz + 1);
        fill(level, box, Blocks.BROWN_TERRACOTTA.defaultBlockState(), sx, 2, sz, sx, 5, sz);
        put(level, box, Blocks.YELLOW_TERRACOTTA, sx, 6, sz);
        sign(level, box, sx, 1, sz - 2, Direction.NORTH, "BIG BOUDIN", "Plant legend", "& folk hero", "");
        // trees, flower beds, benches
        int[][] trees = {{x0 + 4, z0 + 4}, {x1 - 4, z0 + 4}, {x1 - 4, z1 - 4}, {x0 + 14, z0 + 6}, {x1 - 12, z1 - 7}, {cx + 9, z0 + 4}};
        for (int[] t : trees) tree(level, box, t[0], t[1]);
        for (int x = cx - 8; x <= cx + 8; x++) {
            if (Math.abs(x - cx) < 2) continue;
            put(level, box, ((x & 1) == 0 ? Blocks.RED_TULIP : Blocks.CORNFLOWER).defaultBlockState(), x, 1, cz - 3);
            put(level, box, ((x & 1) == 0 ? Blocks.OXEYE_DAISY : Blocks.ALLIUM).defaultBlockState(), x, 1, cz + 3);
        }
        for (int z : new int[]{z0 + 3, z1 - 3}) {
            put(level, box, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST), cx + 2, 1, z);
            put(level, box, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST), cx - 2, 1, z);
        }
        for (int[] l : new int[][]{{cx - 6, cz - 2}, {cx + 6, cz + 2}}) {
            fill(level, box, Blocks.IRON_BARS.defaultBlockState(), l[0], 1, l[1], l[0], 3, l[1]);
            put(level, box, CityRegistry.STREET_LAMP, l[0], 4, l[1]);
        }
    }

    private void tree(WorldGenLevel level, BoundingBox box, int x, int z) {
        BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) for (int y = 4; y <= 7; y++) {
            if (Math.abs(dx) + Math.abs(dz) + (y >= 6 ? y - 5 : 0) > 3) continue;
            put(level, box, leaves, x + dx, y, z + dz);
        }
        for (int y = 1; y <= 5; y++) put(level, box, Blocks.OAK_LOG, x, y, z);
    }
}
