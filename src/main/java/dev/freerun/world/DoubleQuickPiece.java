package dev.freerun.world;

import dev.freerun.economy.EconomyRegistry;
import dev.freerun.food.FoodRegistry;
import dev.freerun.mobs.RoadhouseNpcEntity.Role;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * The Double Quick: a gas station with a convenience store. Forecourt on the west side under a
 * lit canopy with two pump islands (four working gas pumps, a car filling up), and the store on
 * the east: storefront glass, a checkout counter with the clerk, roller grill and hot food, a
 * coffee bar, slushie machine, two aisles of shelves, a wall of drink coolers, an ATM, a TV and a
 * vending machine out front.
 *
 *   x 0..12   forecourt + canopy       x 13..25  store (door in the west wall at z 10..11)
 */
public class DoubleQuickPiece extends StorePieceBase {
    static final int W = 26;
    static final int D = 24;
    private static final int H = 8;

    public DoubleQuickPiece(BlockPos origin) {
        super(StructureRegistry.DOUBLE_QUICK_PIECE, origin, W, H, D);
    }

    public DoubleQuickPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(StructureRegistry.DOUBLE_QUICK_PIECE, tag);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        BlockState asphalt = Blocks.BLACK_CONCRETE.defaultBlockState();
        clear(level, box, W, H, D, asphalt);
        // forecourt markings
        for (int z = 1; z < D - 1; z += 2) put(level, box, Blocks.GRAY_CONCRETE, 0, 0, z);
        for (int x = 1; x <= 11; x++) {
            if ((x * 5) % 7 == 0) put(level, box, Blocks.GRAY_CONCRETE, x, 0, 2);
        }

        buildCanopy(level, box);
        buildStore(level, box);

        // Big sign out by the road
        fill(level, box, Blocks.IRON_BARS, 0, 1, 0, 0, 4, 0);
        fill(level, box, Blocks.RED_CONCRETE, 0, 5, 0, 1, 6, 0);
        put(level, box, Blocks.WHITE_CONCRETE, 2, 5, 0);
        put(level, box, Blocks.WHITE_CONCRETE, 2, 6, 0);
        put(level, box, Blocks.WHITE_CONCRETE, 2, 4, 0);
        sign(level, box, 0, 5, 1, Direction.SOUTH, "§lDOUBLE", "§lQUICK", "Food · Fuel", "Open 24 hrs");
        sign(level, box, 1, 5, 1, Direction.SOUTH, "Regular  2.89", "Plus     3.19", "Premium  3.49", "Diesel   3.69");
        sign(level, box, 2, 4, 1, Direction.SOUTH, "HOT BOUDIN", "2 roller dogs", "for $3", "");

        // A car at the pump
        car(level, box, 6.5, 11.5, 180.0f);
    }

    private void buildCanopy(WorldGenLevel level, BoundingBox box) {
        BlockState post = Blocks.WHITE_CONCRETE.defaultBlockState();
        for (int[] p : new int[][]{{2, 4}, {10, 4}, {2, 19}, {10, 19}}) fill(level, box, post, p[0], 1, p[1], p[0], 5, p[1]);
        for (int x = 1; x <= 11; x++) {
            for (int z = 3; z <= 20; z++) {
                boolean edge = x == 1 || x == 11 || z == 3 || z == 20;
                BlockState roof = edge ? Blocks.RED_CONCRETE.defaultBlockState()
                        : (x % 3 == 0 && z % 4 == 1) ? Blocks.SEA_LANTERN.defaultBlockState() : Blocks.WHITE_CONCRETE.defaultBlockState();
                put(level, box, roof, x, 6, z);
            }
        }
        // Pump islands: a curb with two pumps on each, and a trash can + squeegee bucket.
        BlockState curb = Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        for (int x : new int[]{4, 8}) {
            for (int z = 6; z <= 17; z++) put(level, box, curb, x, 1, z);
            put(level, box, facing(EconomyRegistry.GAS_PUMP, x == 4 ? Direction.EAST : Direction.WEST), x, 1, 8);
            put(level, box, facing(EconomyRegistry.GAS_PUMP, x == 4 ? Direction.EAST : Direction.WEST), x, 1, 15);
            put(level, box, Blocks.CAULDRON, x, 1, 12);
            put(level, box, Blocks.WATER_CAULDRON.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 2), x, 1, 11);
        }
        sign(level, box, 2, 2, 5, Direction.SOUTH, "PUMPS", "Right-click with", "a car parked", "nearby. $1/10%");
        sign(level, box, 10, 2, 5, Direction.SOUTH, "Empty gas can?", "Right-click", "the pump with", "it. $6");
        // Ice chest and propane cage against the store
        put(level, box, Blocks.LIGHT_BLUE_CONCRETE, 12, 1, 15);
        put(level, box, Blocks.LIGHT_BLUE_CONCRETE, 12, 1, 16);
        sign(level, box, 12, 2, 15, Direction.WEST, "ICE", "$2", "", "");
        put(level, box, facing(EconomyRegistry.VENDING_MACHINE, Direction.WEST), 12, 1, 6);
        put(level, box, facing(EconomyRegistry.VENDING_MACHINE, Direction.WEST), 12, 1, 7);
    }

    private void buildStore(WorldGenLevel level, BoundingBox box) {
        BlockState wall = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
        BlockState glass = Blocks.GLASS.defaultBlockState();
        int x0 = 13, x1 = 25, z0 = 2, z1 = 21;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
                put(level, box, ((x + z) % 2 == 0 ? Blocks.WHITE_CONCRETE : Blocks.LIGHT_GRAY_CONCRETE).defaultBlockState(), x, 0, z);
                for (int y = 1; y <= 5; y++) {
                    BlockState s;
                    if (!edge) s = Blocks.AIR.defaultBlockState();
                    else if (x == x0 && y <= 3 && z > z0 && z < z1) s = glass;          // storefront
                    else if (x == x0 && y == 4) s = Blocks.RED_CONCRETE.defaultBlockState();
                    else if (x == x0 && y == 5) s = Blocks.WHITE_CONCRETE.defaultBlockState();
                    else if (z == z1 && y == 2 && x % 4 == 0) s = glass;
                    else s = wall;
                    put(level, box, s, x, y, z);
                }
                BlockState roof = (!edge && x % 4 == 1 && z % 4 == 0) ? Blocks.SEA_LANTERN.defaultBlockState()
                        : Blocks.GRAY_CONCRETE.defaultBlockState();
                put(level, box, roof, x, 6, z);
            }
        }
        // Automatic double door (always open)
        for (int y = 1; y <= 3; y++) {
            put(level, box, Blocks.AIR, x0, y, 10);
            put(level, box, Blocks.AIR, x0, y, 11);
        }
        put(level, box, Blocks.LIGHT_GRAY_CONCRETE, x0, 3, 10);
        put(level, box, Blocks.LIGHT_GRAY_CONCRETE, x0, 3, 11);
        put(level, box, Blocks.GRAY_CARPET, x0 + 1, 1, 10);
        put(level, box, Blocks.GRAY_CARPET, x0 + 1, 1, 11);
        // The name over the door
        sign(level, box, x0 - 1, 4, 9, Direction.WEST, "", "§l§4DOUBLE", "", "");
        sign(level, box, x0 - 1, 4, 10, Direction.WEST, "", "§l§4QUICK", "", "");
        sign(level, box, x0 - 1, 4, 11, Direction.WEST, "", "§lFOOD MART", "", "");
        sign(level, box, x0 - 1, 4, 12, Direction.WEST, "Open", "24 hours", "", "");

        // Checkout: counter across the front, clerk behind it, roller grill and hot case.
        for (int x = 15; x <= 21; x++) put(level, box, facing(FoodRegistry.COUNTER, Direction.SOUTH), x, 1, 6);
        put(level, box, facing(EconomyRegistry.ROLLER_GRILL, Direction.SOUTH), 17, 2, 6);
        put(level, box, Blocks.GLASS, 20, 2, 6);                                    // hot case
        put(level, box, facing(EconomyRegistry.SLUSHIE_MACHINE, Direction.SOUTH), 22, 1, 6);
        put(level, box, Blocks.SMOOTH_STONE_SLAB, 18, 2, 6);                         // register
        for (int x = 14; x <= 23; x++) {
            put(level, box, facing(FoodRegistry.STORE_SHELF, Direction.SOUTH), x, 1, 3);   // lottery & cigarette wall
            put(level, box, facing(FoodRegistry.STORE_SHELF, Direction.SOUTH), x, 2, 3);
        }
        put(level, box, facing(EconomyRegistry.TV, Direction.SOUTH).setValue(dev.freerun.economy.StoreBlocks.Tv.ON, true), 19, 3, 3);
        sign(level, box, 16, 3, 3, Direction.SOUTH, "SCRATCH-OFFS", "Lucky 7s", "$2", "");
        sign(level, box, 21, 3, 3, Direction.SOUTH, "We card", "everyone", "under 40", "");
        staff(level, box, 18, 4, 0.0f, Role.CLERK, new ItemStack(EconomyRegistry.SCRATCH_OFF));

        // ATM by the door
        put(level, box, facing(EconomyRegistry.ATM, Direction.EAST), 14, 1, 13);

        // Coffee bar along the east wall
        for (int z = 8; z <= 12; z++) put(level, box, facing(FoodRegistry.COUNTER, Direction.WEST), 24, 1, z);
        put(level, box, facing(FoodRegistry.COFFEE_MAKER, Direction.WEST), 24, 2, 9);
        put(level, box, facing(FoodRegistry.COFFEE_MAKER, Direction.WEST), 24, 2, 10);
        put(level, box, facing(FoodRegistry.MICROWAVE, Direction.WEST), 24, 2, 12);
        sign(level, box, 24, 3, 10, Direction.WEST, "COFFEE", "Fresh brewed", "(mostly)", "");

        // Two aisles of shelves
        for (int z = 10; z <= 17; z++) {
            put(level, box, facing(FoodRegistry.STORE_SHELF, Direction.EAST), 17, 1, z);
            put(level, box, facing(FoodRegistry.STORE_SHELF, Direction.WEST), 18, 1, z);
            put(level, box, facing(FoodRegistry.STORE_SHELF, Direction.EAST), 21, 1, z);
        }
        // Wall of drink coolers along the back and east walls
        for (int x = 14; x <= 23; x++) {
            put(level, box, facing(FoodRegistry.COOLER, Direction.NORTH), x, 1, 20);
            put(level, box, facing(FoodRegistry.COOLER, Direction.NORTH), x, 2, 20);
        }
        for (int z = 14; z <= 19; z++) {
            put(level, box, facing(FoodRegistry.COOLER, Direction.WEST), 24, 1, z);
            put(level, box, facing(FoodRegistry.COOLER, Direction.WEST), 24, 2, z);
        }
        sign(level, box, 19, 3, 20, Direction.NORTH, "COLD DRINKS", "Fizz Cola · Voltage", "Sweet Tea", "");

        // Supplies for the coffee bar (the shelves are for show: pay at the counter)
        stock(level, box, 24, 1, 9, FoodRegistry.COFFEE_GROUNDS, FoodRegistry.COFFEE_MUG, EconomyRegistry.FROZEN_BURRITO);
    }

    private void stock(WorldGenLevel level, BoundingBox box, int x, int y, int z, net.minecraft.world.item.Item... items) {
        BlockPos p = at(x, y, z);
        if (!box.isInside(p)) return;
        ItemStack[] stacks = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) stacks[i] = new ItemStack(items[i], 6);
        AnnexHelpers.fill(level, p, stacks);
    }
}
