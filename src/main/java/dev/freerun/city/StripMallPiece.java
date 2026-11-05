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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * A strip mall: six storefronts in a row under one long roof with a covered walkway, and a big
 * shared parking lot out front. Every strip mall has a Sugar Bayou Sweets and a hardware-and-
 * phones store you can shop in; the other units rotate through a laundromat, a pawn shop, a
 * bookstore, a nail salon, a po'boy shop and a dry cleaner.
 *
 *   z 0..45 parking   z 47..49 covered walk   z 50..75 storefronts (x 4..75, 12 wide each)
 */
public class StripMallPiece extends CityPieceBase {
    private static final String[][] TENANTS = {
            {"LAGNIAPPE", "LAUNDRY", "Wash · Dry · Fold"}, {"PAWN &", "PROSPER", "We buy gold"},
            {"BAYOU", "BOOKS", "New & used"}, {"SECOND LINE", "NAILS", "Walk-ins welcome"},
            {"GUMBO POT", "PO'BOYS", "Since 1979"}, {"PELICAN", "CLEANERS", "Same-day service"}};
    private static final Block[] AWNINGS = {Blocks.BLUE_WOOL, Blocks.GREEN_WOOL, Blocks.RED_WOOL, Blocks.ORANGE_WOOL,
            Blocks.CYAN_WOOL, Blocks.PURPLE_WOOL};
    private final int variant;

    public StripMallPiece(BlockPos city, int lx, int lz, int w, int d, int variant) {
        super(CityRegistry.STRIP_MALL_PIECE, city, lx, lz, w, 12, d);
        this.variant = variant;
    }

    public StripMallPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(CityRegistry.STRIP_MALL_PIECE, tag);
        variant = tag.getInt("Variant");
    }

    @Override
    protected void save(CompoundTag tag) {
        tag.putInt("Variant", variant);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        ground(level, box, Blocks.BLACK_CONCRETE.defaultBlockState(), 12);
        LotPiece.parking(level, box, this, 2, 2, 76, 43, 4);
        // pylon sign by the road
        fill(level, box, Blocks.STONE_BRICKS.defaultBlockState(), 38, 1, 1, 41, 9, 1);
        sign(level, box, 39, 8, 0, Direction.NORTH, "", "§lMAGNOLIA", "§lPLAZA", "");
        sign(level, box, 40, 8, 0, Direction.NORTH, "Sweets", "Hardware", "Laundry", "& more");

        int z0 = 50, z1 = 75;
        fill(level, box, Blocks.SMOOTH_SANDSTONE.defaultBlockState(), 2, 0, 46, 77, 0, 49);   // the walk
        int sweets = Math.floorMod(variant, 6), hardware = Math.floorMod(variant + 3, 6);
        for (int u = 0; u < 6; u++) {
            int x0 = 4 + u * 12, x1 = x0 + 11;
            BlockState wall = (u % 2 == 0 ? Blocks.WHITE_TERRACOTTA : Blocks.LIGHT_GRAY_TERRACOTTA).defaultBlockState();
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
                    put(level, box, Blocks.SMOOTH_STONE, x, 0, z);
                    for (int y = 1; y <= 6; y++) {
                        BlockState s = Blocks.AIR.defaultBlockState();
                        if (edge) s = z == z0 && y >= 1 && y <= 4 && x > x0 + 1 && x < x1 - 1 ? Blocks.GLASS.defaultBlockState() : wall;
                        put(level, box, s, x, y, z);
                    }
                    put(level, box, Blocks.SMOOTH_STONE, x, 7, z);
                }
            }
            put(level, box, Blocks.AIR, x0 + 5, 1, z0);
            put(level, box, Blocks.AIR, x0 + 5, 2, z0);
            put(level, box, Blocks.AIR, x0 + 6, 1, z0);
            put(level, box, Blocks.AIR, x0 + 6, 2, z0);
            // awning and the store's sign band
            for (int x = x0; x <= x1; x++) {
                put(level, box, AWNINGS[(u + variant) % AWNINGS.length], x, 5, z0 - 1);
                for (int y = 5; y <= 7; y++) put(level, box, Blocks.SMOOTH_QUARTZ, x, y, z0);
            }
            put(level, box, Blocks.GLOWSTONE, x0 + 3, 6, z0 + 6);
            put(level, box, Blocks.GLOWSTONE, x0 + 8, 6, z0 + 16);
            if (u == sweets) sweets(level, box, x0, x1, z0, z1);
            else if (u == hardware) hardware(level, box, x0, x1, z0, z1);
            else tenant(level, box, x0, x1, z0, z1, TENANTS[(u + variant) % TENANTS.length], (u + variant) % TENANTS.length);
        }
        // the covered walkway roof on posts
        for (int x = 4; x <= 75; x++) for (int z = 46; z <= 48; z++) put(level, box, Blocks.SMOOTH_STONE_SLAB, x, 6, z);
        for (int x = 4; x <= 75; x += 6) fill(level, box, Blocks.SPRUCE_FENCE.defaultBlockState(), x, 1, 46, x, 5, 46);
    }

    private void sweets(WorldGenLevel level, BoundingBox box, int x0, int x1, int z0, int z1) {
        sign(level, box, x0 + 3, 6, z0 - 1, Direction.NORTH, "", "§lSUGAR BAYOU", "SWEETS", "");
        sign(level, box, x0 + 8, 6, z0 - 1, Direction.NORTH, "Beignets", "King cake", "Sno-balls", "");
        for (int x = x0 + 1; x < x1; x++) for (int z = z0 + 1; z < z1; z++) put(level, box, ((x + z) % 2 == 0 ? Blocks.PINK_CONCRETE : Blocks.WHITE_CONCRETE), x, 0, z);
        for (int x = x0 + 2; x <= x1 - 2; x++) {
            put(level, box, facing(FoodRegistry.COUNTER, Direction.NORTH), x, 1, z0 + 8);
            if (x % 2 == 0) put(level, box, Blocks.CAKE, x, 2, z0 + 8);
        }
        put(level, box, Blocks.AIR, x1 - 1, 1, z0 + 8);
        put(level, box, facing(EconomyRegistry.SLUSHIE_MACHINE, Direction.NORTH), x0 + 3, 1, z1 - 1);
        put(level, box, facing(FoodRegistry.COFFEE_MAKER, Direction.NORTH), x0 + 5, 1, z1 - 1);
        put(level, box, BlockRegistry.TABLE, x0 + 3, 1, z0 + 3);
        put(level, box, facing(BlockRegistry.CHAIR, Direction.EAST), x0 + 2, 1, z0 + 3);
        staff(level, box, x0 + 6, z0 + 9, 180.0f, RoadhouseNpcEntity.Role.CLERK, Shops.Shop.DESSERT, new ItemStack(EconomyRegistry.KING_CAKE_SLICE));
    }

    private void hardware(WorldGenLevel level, BoundingBox box, int x0, int x1, int z0, int z1) {
        sign(level, box, x0 + 3, 6, z0 - 1, Direction.NORTH, "", "§lCRESCENT", "HARDWARE", "& Cellular");
        sign(level, box, x0 + 8, 6, z0 - 1, Direction.NORTH, "Phones", "Tools", "Bait", "Keys cut");
        for (int z = z0 + 3; z <= z1 - 3; z += 3) {
            for (int x = x0 + 2; x <= x0 + 6; x++) put(level, box, facing(FoodRegistry.STORE_SHELF, Direction.NORTH), x, 1, z);
        }
        for (int z = z0 + 3; z <= z0 + 8; z++) put(level, box, facing(FoodRegistry.COUNTER, Direction.WEST), x1 - 3, 1, z);
        put(level, box, facing(EconomyRegistry.TV, Direction.WEST).setValue(dev.freerun.economy.StoreBlocks.Tv.ON, true), x1 - 1, 2, z0 + 5);
        staff(level, box, x1 - 2, z0 + 5, 90.0f, RoadhouseNpcEntity.Role.SHOPKEEPER, Shops.Shop.GENERAL_STORE, new ItemStack(EconomyRegistry.SMARTPHONE));
    }

    private void tenant(WorldGenLevel level, BoundingBox box, int x0, int x1, int z0, int z1, String[] name, int kind) {
        sign(level, box, x0 + 5, 6, z0 - 1, Direction.NORTH, "", "§l" + name[0], name[1], name[2]);
        sign(level, box, x0 + 5, 2, z0 + 1, Direction.SOUTH, "", kind == 1 ? "Sorry," : "Opening", kind == 1 ? "we're closed" : "soon!", "");
        switch (kind) {
            case 0, 5 -> {   // laundromat / cleaners: rows of machines
                for (int x = x0 + 2; x <= x1 - 2; x++) {
                    put(level, box, Blocks.SMOKER.defaultBlockState().setValue(net.minecraft.world.level.block.SmokerBlock.FACING, Direction.SOUTH), x, 1, z0 + 6);
                    put(level, box, Blocks.BLAST_FURNACE.defaultBlockState().setValue(net.minecraft.world.level.block.BlastFurnaceBlock.FACING, Direction.NORTH), x, 1, z0 + 7);
                }
                for (int x = x0 + 2; x <= x1 - 2; x += 2) put(level, box, facing(BlockRegistry.CHAIR, Direction.NORTH), x, 1, z0 + 2);
            }
            case 1 -> {   // pawn shop: glass cases and shelves
                for (int x = x0 + 2; x <= x1 - 2; x++) put(level, box, Blocks.GLASS, x, 1, z0 + 6);
                for (int x = x0 + 2; x <= x1 - 2; x += 3) put(level, box, Blocks.GOLD_BLOCK, x, 2, z0 + 6);
                for (int x = x0 + 1; x < x1; x++) put(level, box, facing(FoodRegistry.STORE_SHELF, Direction.NORTH), x, 1, z1 - 1);
            }
            case 2 -> {   // bookstore
                for (int z = z0 + 3; z <= z1 - 2; z += 3) for (int x = x0 + 2; x <= x1 - 2; x++) {
                    put(level, box, Blocks.BOOKSHELF, x, 1, z);
                    put(level, box, Blocks.BOOKSHELF, x, 2, z);
                }
                put(level, box, Blocks.LECTERN, x0 + 2, 1, z0 + 2);
            }
            case 3 -> {   // nail salon: chairs and stations
                for (int x = x0 + 2; x <= x1 - 2; x += 3) {
                    put(level, box, facing(BlockRegistry.CHAIR, Direction.SOUTH), x, 1, z0 + 5);
                    put(level, box, BlockRegistry.TABLE, x, 1, z0 + 6);
                    put(level, box, Blocks.POTTED_PINK_TULIP, x, 2, z0 + 6);
                }
            }
            default -> {  // po'boy shop: counter and tables
                for (int x = x0 + 2; x <= x1 - 2; x++) put(level, box, facing(FoodRegistry.COUNTER, Direction.NORTH), x, 1, z0 + 10);
                for (int x = x0 + 3; x <= x1 - 3; x += 4) {
                    put(level, box, BlockRegistry.TABLE, x, 1, z0 + 4);
                    put(level, box, facing(BlockRegistry.CHAIR, Direction.EAST), x - 1, 1, z0 + 4);
                    put(level, box, facing(BlockRegistry.CHAIR, Direction.WEST), x + 1, 1, z0 + 4);
                }
                put(level, box, facing(BlockRegistry.GRILL, Direction.NORTH), x0 + 3, 1, z1 - 1);
            }
        }
    }
}
