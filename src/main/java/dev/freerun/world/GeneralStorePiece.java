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
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * A country general store: a wooden building with a covered front porch (rocking chairs, a bench,
 * barrels), shelves of hardware and goods inside, the shopkeeper at the counter, and a used car
 * for sale out back.
 *
 *   z 0..2  porch (north)   z 3..14  store   z 15..19  gravel lot with the car for sale
 */
public class GeneralStorePiece extends StorePieceBase {
    static final int W = 17;
    static final int D = 20;
    private static final int H = 8;

    public GeneralStorePiece(BlockPos origin) {
        super(StructureRegistry.GENERAL_STORE_PIECE, origin, W, H, D);
    }

    public GeneralStorePiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(StructureRegistry.GENERAL_STORE_PIECE, tag);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        clear(level, box, W, H, D, Blocks.GRASS_BLOCK.defaultBlockState());
        BlockState planks = Blocks.SPRUCE_PLANKS.defaultBlockState();
        BlockState log = Blocks.STRIPPED_OAK_LOG.defaultBlockState();
        BlockState floor = Blocks.OAK_PLANKS.defaultBlockState();
        int x0 = 1, x1 = 15, z0 = 3, z1 = 14;

        // ---- walls, floor, roof
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
                boolean corner = (x == x0 || x == x1) && (z == z0 || z == z1);
                put(level, box, floor, x, 0, z);
                for (int y = 1; y <= 4; y++) {
                    BlockState s;
                    if (!edge) s = Blocks.AIR.defaultBlockState();
                    else if (corner) s = log;
                    else if (y == 2 && ((z == z0 || z == z1) ? x % 3 == 1 : z % 3 == 1)) s = Blocks.GLASS.defaultBlockState();
                    else s = planks;
                    put(level, box, s, x, y, z);
                }
                put(level, box, Blocks.DARK_OAK_PLANKS, x, 5, z);
            }
        }
        // a pitched false front over the porch, like an old-time store
        for (int x = x0; x <= x1; x++) put(level, box, planks, x, 6, z0);
        for (int x = x0 + 2; x <= x1 - 2; x++) put(level, box, planks, x, 7, z0);
        // lights
        for (int[] p : new int[][]{{4, 6}, {12, 6}, {4, 11}, {12, 11}}) {
            put(level, box, Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true), p[0], 4, p[1]);
        }
        // door
        put(level, box, Blocks.AIR, 8, 1, z0);
        put(level, box, Blocks.AIR, 8, 2, z0);
        put(level, box, Blocks.AIR, 7, 1, z0);
        put(level, box, Blocks.AIR, 7, 2, z0);

        // ---- porch
        BlockState porch = Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
        for (int x = x0; x <= x1; x++) {
            for (int z = 0; z < z0; z++) {
                put(level, box, Blocks.SPRUCE_PLANKS, x, 0, z);
                put(level, box, porch, x, 4, z);
            }
        }
        BlockState rail = Blocks.SPRUCE_FENCE.defaultBlockState().setValue(FenceBlock.EAST, true).setValue(FenceBlock.WEST, true);
        for (int x = x0; x <= x1; x++) if (x != 7 && x != 8) put(level, box, rail, x, 1, 0);
        for (int x : new int[]{x0, x1, 5, 11}) for (int y = 1; y <= 3; y++) put(level, box, Blocks.SPRUCE_FENCE, x, y, 0);
        put(level, box, Blocks.SPRUCE_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true), 5, 1, 0);
        put(level, box, facing(BlockRegistry.CHAIR, Direction.NORTH), 3, 1, 1);
        put(level, box, facing(BlockRegistry.CHAIR, Direction.NORTH), 4, 1, 1);
        put(level, box, Blocks.BARREL, 13, 1, 1);
        put(level, box, Blocks.BARREL, 14, 1, 1);
        put(level, box, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH), 11, 1, 1);
        put(level, box, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH), 12, 1, 1);
        sign(level, box, 6, 6, z0 - 1, Direction.NORTH, "", "§lGENERAL", "§lSTORE", "");
        sign(level, box, 10, 6, z0 - 1, Direction.NORTH, "Hardware · Bait", "Groceries", "Phones · Cars", "We buy steel");

        // ---- inside: counter + shopkeeper, shelves, barrels
        for (int z = 5; z <= 10; z++) put(level, box, facing(FoodRegistry.COUNTER, Direction.WEST), 12, 1, z);
        put(level, box, Blocks.SMOOTH_STONE_SLAB, 12, 2, 7);                         // register
        put(level, box, facing(EconomyRegistry.TV, Direction.WEST), 14, 1, 12);
        staff(level, box, 13, 8, 90.0f, Role.SHOPKEEPER, new ItemStack(net.minecraft.world.item.Items.PAPER));
        for (int z = 5; z <= 12; z++) {
            put(level, box, facing(FoodRegistry.STORE_SHELF, Direction.EAST), 2, 1, z);
            put(level, box, facing(FoodRegistry.STORE_SHELF, Direction.EAST), 2, 2, z);
        }
        for (int x = 5; x <= 9; x++) {
            put(level, box, facing(FoodRegistry.STORE_SHELF, Direction.NORTH), x, 1, 9);
            put(level, box, facing(FoodRegistry.STORE_SHELF, Direction.SOUTH), x, 1, 10);
            put(level, box, facing(FoodRegistry.STORE_SHELF, Direction.NORTH), x, 1, 13);
        }
        for (int x = 5; x <= 9; x += 2) put(level, box, Blocks.BARREL, x, 1, 6);
        put(level, box, facing(FoodRegistry.COOLER, Direction.WEST), 14, 1, 5);
        put(level, box, facing(FoodRegistry.COOLER, Direction.WEST), 14, 2, 5);
        put(level, box, Blocks.ANVIL, 14, 1, 13);
        sign(level, box, 14, 3, 8, Direction.WEST, "We buy:", "steel, sausage,", "crops, ores", "& cans");
        sign(level, box, 2, 3, 8, Direction.EAST, "Prices are", "firm. Mostly.", "", "");

        // ---- out back: the car for sale
        for (int x = 2; x <= 14; x++) for (int z = 15; z <= 19; z++) put(level, box, Blocks.GRAVEL, x, 0, z);
        car(level, box, 8.5, 17.5, 90.0f);
        put(level, box, Blocks.SPRUCE_PLANKS, 4, 1, 17);
        put(level, box, Blocks.SPRUCE_PLANKS, 4, 2, 17);
        sign(level, box, 5, 2, 17, Direction.EAST, "§lFOR SALE", "$400", "Runs good.", "Ask inside.");
    }
}
