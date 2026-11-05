package dev.freerun.city;

import dev.freerun.economy.EconomyRegistry;
import dev.freerun.economy.Shops;
import dev.freerun.food.FoodRegistry;
import dev.freerun.mobs.RoadhouseNpcEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * MegaMart Supercenter: the big box. A huge parking lot with cart corrals and light poles, and
 * a warehouse-sized store behind it: grocery aisles, a wall of coolers, produce, a pharmacy,
 * electronics, home & garden, sporting goods, a row of checkout lanes and a service desk.
 *
 *   z 0..40 parking   z 42..77 the store (x 4..75)
 */
public class MegaMartPiece extends CityPieceBase {
    public MegaMartPiece(BlockPos city, int lx, int lz, int w, int d) {
        super(CityRegistry.MEGAMART_PIECE, city, lx, lz, w, 16, d);
    }

    public MegaMartPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(CityRegistry.MEGAMART_PIECE, tag);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        ground(level, box, Blocks.BLACK_CONCRETE.defaultBlockState(), 16);
        LotPiece.parking(level, box, this, 2, 2, 76, 38, 5);
        // cart corrals
        BlockState rail = Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.EAST, true).setValue(IronBarsBlock.WEST, true);
        for (int x : new int[]{20, 56}) {
            fill(level, box, rail, x, 1, 12, x + 3, 1, 12);
            fill(level, box, rail, x, 1, 14, x + 3, 1, 14);
        }
        fill(level, box, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), 2, 0, 40, 77, 0, 41);   // the walk along the front

        int x0 = 4, x1 = 75, z0 = 42, z1 = 77, top = 10;
        BlockState wall = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
        BlockState stripe = Blocks.GREEN_CONCRETE.defaultBlockState();
        BlockState stripe2 = Blocks.LIME_CONCRETE.defaultBlockState();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
                put(level, box, Blocks.SMOOTH_STONE, x, 0, z);
                for (int y = 1; y < top; y++) {
                    BlockState s = Blocks.AIR.defaultBlockState();
                    if (edge) s = y == top - 1 ? stripe : y == top - 2 ? stripe2 : wall;
                    put(level, box, s, x, y, z);
                }
                put(level, box, Blocks.SMOOTH_STONE, x, top, z);
            }
        }
        // the facade: a raised centre with the name, two entrances with glass
        fill(level, box, wall, 22, top, z0, 57, top + 6, z0);
        fill(level, box, stripe, 22, top + 7, z0, 57, top + 7, z0);
        bigTextCentred(level, box, 22, 57, top + 6, z0 - 1, Direction.NORTH, "MEGAMART", Blocks.GREEN_CONCRETE.defaultBlockState());
        for (int[] door : new int[][]{{16, 20}, {59, 63}}) {
            fill(level, box, Blocks.GLASS.defaultBlockState(), door[0], 1, z0, door[1], 4, z0);
            fill(level, box, Blocks.AIR.defaultBlockState(), door[0] + 1, 1, z0, door[1] - 1, 3, z0);
            fill(level, box, stripe, door[0] - 1, 5, z0 - 1, door[1] + 1, 5, z0 - 1);
            sign(level, box, (door[0] + door[1]) / 2, 4, z0 - 1, Direction.NORTH, "", door[0] < 40 ? "GROCERY" : "HOME & MORE", "Open 24 hrs", "");
        }
        sign(level, box, 40, 4, z0 - 1, Direction.NORTH, "§lMEGAMART", "SUPERCENTER", "Save more.", "Live... more?");
        // skylights
        for (int x = x0 + 4; x < x1 - 2; x += 8) for (int z = z0 + 4; z < z1 - 2; z += 8) put(level, box, Blocks.GLASS, x, top, z);
        for (int x = x0 + 4; x < x1 - 2; x += 6) for (int z = z0 + 5; z < z1 - 2; z += 6) put(level, box, Blocks.SEA_LANTERN, x, top - 1, z);

        // ---- checkout lanes along the front
        for (int x = 24; x <= 54; x += 5) {
            for (int z = z0 + 4; z <= z0 + 7; z++) put(level, box, facing(FoodRegistry.COUNTER, Direction.WEST), x, 1, z);
            put(level, box, Blocks.SMOOTH_STONE_SLAB, x, 2, z0 + 5);
            put(level, box, Blocks.IRON_BARS, x, 2, z0 + 7);
            put(level, box, Blocks.REDSTONE_LAMP, x, 3, z0 + 7);
        }
        staff(level, box, 25, z0 + 5, 90.0f, RoadhouseNpcEntity.Role.SHOPKEEPER, Shops.Shop.MEGAMART, new ItemStack(Items.PAPER));
        staff(level, box, 40, z0 + 5, 90.0f, RoadhouseNpcEntity.Role.SHOPKEEPER, Shops.Shop.MEGAMART, new ItemStack(EconomyRegistry.SCRATCH_OFF));
        // service desk by the east door
        for (int x = 64; x <= 71; x++) put(level, box, facing(FoodRegistry.COUNTER, Direction.NORTH), x, 1, z0 + 6);
        put(level, box, facing(EconomyRegistry.ATM, Direction.NORTH), 66, 1, z0 + 1);
        put(level, box, facing(EconomyRegistry.VENDING_MACHINE, Direction.NORTH), 67, 1, z0 + 1);
        staff(level, box, 68, z0 + 7, 180.0f, RoadhouseNpcEntity.Role.SHOPKEEPER, Shops.Shop.MEGAMART, new ItemStack(EconomyRegistry.SMARTPHONE));
        sign(level, box, 68, 3, z0 + 6, Direction.NORTH, "", "CUSTOMER", "SERVICE", "Returns");

        // ---- the aisles (north-south runs of double-sided shelving)
        for (int x = x0 + 5; x <= x1 - 8; x += 4) {
            if (x > 58 && x < 64) continue;
            for (int z = z0 + 12; z <= z1 - 6; z++) {
                if (z == z0 + 22) continue;   // cross aisle
                put(level, box, facing(FoodRegistry.STORE_SHELF, Direction.WEST), x, 1, z);
                put(level, box, facing(FoodRegistry.STORE_SHELF, Direction.EAST), x + 1, 1, z);
                put(level, box, facing(FoodRegistry.STORE_SHELF, Direction.WEST), x, 2, z);
                put(level, box, facing(FoodRegistry.STORE_SHELF, Direction.EAST), x + 1, 2, z);
                if (z % 5 == 0) put(level, box, Blocks.BARREL, x, 3, z);
            }
            String[] aisle = {"Snacks", "Canned", "Cereal", "Baking", "Drinks", "Paper", "Pets", "Baby", "Health",
                    "Beauty", "Toys", "Crafts", "Auto", "Hardware", "Garden", "Sports"};
            int k = (x - x0 - 5) / 4;
            sign(level, box, x, 4, z0 + 11, Direction.NORTH, "AISLE " + (k + 1), aisle[Math.floorMod(k, aisle.length)], "", "");
        }
        // coolers along the back wall
        for (int x = x0 + 1; x <= x1 - 1; x++) {
            put(level, box, facing(FoodRegistry.COOLER, Direction.NORTH), x, 1, z1 - 1);
            put(level, box, facing(FoodRegistry.COOLER, Direction.NORTH), x, 2, z1 - 1);
        }
        // produce bins at the west end
        for (int z = z0 + 12; z <= z1 - 6; z += 3) {
            put(level, box, Blocks.MELON, x0 + 2, 1, z);
            put(level, box, Blocks.PUMPKIN, x0 + 2, 1, z + 1);
            put(level, box, Blocks.HAY_BLOCK, x0 + 3, 1, z);
        }
        // electronics wall at the east end, a pharmacy counter beside it
        for (int z = z0 + 10; z <= z1 - 4; z++) {
            put(level, box, facing(EconomyRegistry.TV, Direction.WEST).setValue(dev.freerun.economy.StoreBlocks.Tv.ON, z % 2 == 0), x1 - 1, 2, z);
            put(level, box, facing(FoodRegistry.STORE_SHELF, Direction.WEST), x1 - 1, 1, z);
        }
        for (int x = 60; x <= 66; x++) put(level, box, facing(FoodRegistry.COUNTER, Direction.NORTH), x, 1, z1 - 8);
        sign(level, box, 63, 3, z1 - 8, Direction.NORTH, "", "PHARMACY", "Drop-off", "& pick-up");
        // a greeter's sign by each door
        sign(level, box, 18, 2, z0 + 2, Direction.NORTH, "Welcome to", "MegaMart!", "Carts are", "outside.");
    }
}
