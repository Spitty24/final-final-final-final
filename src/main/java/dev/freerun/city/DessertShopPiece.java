package dev.freerun.city;

import dev.freerun.economy.EconomyRegistry;
import dev.freerun.economy.Shops;
import dev.freerun.food.FoodRegistry;
import dev.freerun.mobs.RoadhouseNpcEntity;
import dev.freerun.world.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Sugar Bayou Sweets: a pink-and-white bakery and sno-ball stand with a striped awning, a glass
 * case full of cakes, a patio with umbrella tables, and parking out back.
 */
public class DessertShopPiece extends CityPieceBase {
    public DessertShopPiece(BlockPos city, int lx, int lz, int w, int d) {
        super(CityRegistry.DESSERT_PIECE, city, lx, lz, w, 12, d);
    }

    public DessertShopPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(CityRegistry.DESSERT_PIECE, tag);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        ground(level, box, Blocks.GRASS_BLOCK.defaultBlockState(), 12);
        BlockState pink = Blocks.PINK_TERRACOTTA.defaultBlockState();
        BlockState white = Blocks.WHITE_CONCRETE.defaultBlockState();
        // patio and the walk out front
        fill(level, box, Blocks.SMOOTH_SANDSTONE.defaultBlockState(), 4, 0, 1, 33, 0, 9);
        LotPiece.parking(level, box, this, 2, 25, 34, 12, 3);
        int x0 = 11, x1 = 26, z0 = 10, z1 = 22;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
                put(level, box, ((x + z) % 2 == 0 ? Blocks.PINK_CONCRETE : Blocks.WHITE_CONCRETE), x, 0, z);
                for (int y = 1; y <= 5; y++) {
                    BlockState s = Blocks.AIR.defaultBlockState();
                    if (edge) s = (y == 2 || y == 3) && z == z0 && x > x0 + 1 && x < x1 - 1 ? Blocks.GLASS.defaultBlockState()
                            : y == 5 ? white : pink;
                    put(level, box, s, x, y, z);
                }
                put(level, box, Blocks.SMOOTH_QUARTZ, x, 6, z);
            }
        }
        // scalloped roof edge and a big cupcake on top
        for (int x = x0; x <= x1; x++) put(level, box, (x % 2 == 0 ? Blocks.PINK_WOOL : Blocks.WHITE_WOOL), x, 7, z0);
        fill(level, box, Blocks.BROWN_TERRACOTTA.defaultBlockState(), 17, 7, 15, 20, 8, 18);
        fill(level, box, Blocks.PINK_WOOL.defaultBlockState(), 17, 9, 15, 20, 9, 18);
        fill(level, box, Blocks.PINK_WOOL.defaultBlockState(), 18, 10, 16, 19, 10, 17);
        put(level, box, Blocks.RED_WOOL, 18, 11, 16);
        // striped awning over the windows
        for (int x = x0; x <= x1; x++) put(level, box, (x % 2 == 0 ? Blocks.PINK_CARPET : Blocks.WHITE_CARPET), x, 5, z0 - 1);
        fill(level, box, Blocks.AIR.defaultBlockState(), 18, 1, z0, 19, 2, z0);
        bigTextCentred(level, box, 4, 33, 13, z0, Direction.NORTH, "SWEETS", Blocks.MAGENTA_CONCRETE.defaultBlockState());
        fill(level, box, white, 4, 8, z0, 33, 13, z0);
        bigTextCentred(level, box, 4, 33, 13, z0 - 1, Direction.NORTH, "SWEETS", Blocks.MAGENTA_CONCRETE.defaultBlockState());
        sign(level, box, 15, 4, z0 - 1, Direction.NORTH, "", "§lSUGAR BAYOU", "SWEETS", "Est. 1952");
        sign(level, box, 22, 4, z0 - 1, Direction.NORTH, "Beignets", "King cake", "Sno-balls", "Pralines");
        // inside: the display case, the counter and the baker
        for (int x = x0 + 2; x <= x1 - 2; x++) {
            put(level, box, facing(FoodRegistry.COUNTER, Direction.NORTH), x, 1, z0 + 6);
            if (x % 2 == 0) put(level, box, Blocks.CAKE, x, 2, z0 + 6);
        }
        put(level, box, Blocks.AIR, x1 - 1, 1, z0 + 6);
        put(level, box, facing(FoodRegistry.COFFEE_MAKER, Direction.NORTH), x0 + 2, 1, z1 - 1);
        put(level, box, facing(EconomyRegistry.SLUSHIE_MACHINE, Direction.NORTH), x0 + 4, 1, z1 - 1);
        put(level, box, Blocks.SMOKER.defaultBlockState().setValue(net.minecraft.world.level.block.SmokerBlock.FACING, Direction.NORTH), x0 + 6, 1, z1 - 1);
        put(level, box, Blocks.SMOKER.defaultBlockState().setValue(net.minecraft.world.level.block.SmokerBlock.FACING, Direction.NORTH), x0 + 7, 1, z1 - 1);
        for (int x = x0 + 9; x <= x1 - 2; x++) put(level, box, facing(FoodRegistry.STORE_SHELF, Direction.NORTH), x, 1, z1 - 1);
        for (int[] t : new int[][]{{x0 + 2, z0 + 2}, {x1 - 2, z0 + 2}}) {
            put(level, box, BlockRegistry.TABLE, t[0], 1, t[1]);
            put(level, box, facing(BlockRegistry.CHAIR, Direction.SOUTH), t[0], 1, t[1] + 1);
        }
        put(level, box, Blocks.GLOWSTONE, 15, 5, 14);
        put(level, box, Blocks.GLOWSTONE, 22, 5, 18);
        staff(level, box, 18, z0 + 7, 180.0f, RoadhouseNpcEntity.Role.CLERK, Shops.Shop.DESSERT, new ItemStack(EconomyRegistry.BEIGNETS));
        // patio umbrellas
        for (int x = 6; x <= 31; x += 5) {
            if (x > x0 - 2 && x < x1 + 2) continue;
            umbrella(level, box, x, 5);
        }
        umbrella(level, box, 14, 4);
        umbrella(level, box, 23, 4);
    }

    private void umbrella(WorldGenLevel level, BoundingBox box, int x, int z) {
        put(level, box, BlockRegistry.TABLE, x, 1, z);
        put(level, box, Blocks.BIRCH_FENCE, x, 2, z);
        put(level, box, Blocks.BIRCH_FENCE, x, 3, z);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            put(level, box, ((dx + dz) % 2 == 0 ? Blocks.PINK_WOOL : Blocks.WHITE_WOOL), x + dx, 4, z + dz);
        }
        put(level, box, facing(BlockRegistry.CHAIR, Direction.EAST), x - 1, 1, z);
        put(level, box, facing(BlockRegistry.CHAIR, Direction.WEST), x + 1, 1, z);
    }
}
