package dev.freerun.city;

import dev.freerun.economy.Shops;
import dev.freerun.mobs.RoadhouseNpcEntity;
import dev.freerun.skate.ParkBuilder;
import dev.freerun.skate.Skating;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Bayou Bowl Skate Park: a fenced concrete lot with a half pipe along the back, a pair of quarter
 * pipes facing each other across the plaza with a funbox between them, rails, a ledge and a
 * manual pad, a kicker and a bank, a stair set with a handrail, lights, benches and the
 * boardshop (boards and every park kit). Lot-local x 0..37, z 0..37, street to the north.
 */
public class SkateParkPiece extends CityPieceBase {
    public SkateParkPiece(BlockPos city, int lx, int lz, int w, int d) {
        super(CityRegistry.SKATEPARK_PIECE, city, lx, lz, w, 16, d);
    }

    public SkateParkPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(CityRegistry.SKATEPARK_PIECE, tag);
    }

    /** Builds a park feature with its (u = 0, v = 0) corner at local (x, z), standing on the ground. */
    private void feature(WorldGenLevel level, BoundingBox box, ParkBuilder.Kind kind, Direction f, int x, int z) {
        Direction right = f.getClockWise();
        ParkBuilder.build(kind, f, (u, y, v, s) -> put(level, box, s,
                x + right.getStepX() * u + f.getStepX() * v, 1 + y, z + right.getStepZ() * u + f.getStepZ() * v));
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        BlockState concrete = Skating.PARK_CONCRETE.defaultBlockState();
        ground(level, box, concrete, 16);
        // a darker band around the edge, and painted lines across the plaza
        for (int x = 0; x < 38; x++) for (int z = 0; z < 38; z++) {
            if (x == 0 || z == 0 || x == 37 || z == 37) put(level, box, Blocks.GRAY_CONCRETE, x, 0, z);
            else if ((x + z) % 23 == 0) put(level, box, Blocks.LIGHT_GRAY_CONCRETE, x, 0, z);
        }
        // chain-link fence, with a wide gate on the street side
        for (int i = 0; i < 38; i++) {
            for (int y = 1; y <= 3; y++) {
                if (i < 12 || i > 21) put(level, box, Blocks.IRON_BARS, i, y, 0);
                put(level, box, Blocks.IRON_BARS, i, y, 37);
                put(level, box, Blocks.IRON_BARS, 0, y, i);
                put(level, box, Blocks.IRON_BARS, 37, y, i);
            }
        }
        // the gate's sign board, up on two posts
        for (int x = 5; x <= 29; x++) {
            put(level, box, Blocks.BLACK_CONCRETE, x, 4, 0);
            put(level, box, Blocks.BLACK_CONCRETE, x, 10, 0);
        }
        for (int y = 1; y <= 10; y++) {
            put(level, box, Blocks.POLISHED_BLACKSTONE, 5, y, 0);
            put(level, box, Blocks.POLISHED_BLACKSTONE, 29, y, 0);
        }
        for (int x = 6; x <= 28; x++) for (int y = 5; y <= 9; y++) put(level, box, Blocks.WHITE_CONCRETE, x, y, 0);
        bigTextCentred(level, box, 6, 28, 9, 0, Direction.NORTH, "SKATE", Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState());
        sign(level, box, 12, 3, 1, Direction.SOUTH, "§lBAYOU BOWL", "SKATE PARK", "Open dawn to dusk", "Helmets advised");

        // features
        feature(level, box, ParkBuilder.Kind.HALF_PIPE, Direction.EAST, 2, 31);
        feature(level, box, ParkBuilder.Kind.QUARTER_PIPE, Direction.WEST, 6, 10);
        feature(level, box, ParkBuilder.Kind.FUNBOX, Direction.EAST, 13, 12);
        feature(level, box, ParkBuilder.Kind.QUARTER_PIPE, Direction.EAST, 25, 11);
        feature(level, box, ParkBuilder.Kind.STAIR_SET, Direction.SOUTH, 34, 13);
        feature(level, box, ParkBuilder.Kind.RAIL, Direction.EAST, 8, 20);
        feature(level, box, ParkBuilder.Kind.RAIL, Direction.EAST, 8, 24);
        feature(level, box, ParkBuilder.Kind.LEDGE, Direction.EAST, 17, 19);
        feature(level, box, ParkBuilder.Kind.MANUAL_PAD, Direction.EAST, 17, 24);
        feature(level, box, ParkBuilder.Kind.KICKER, Direction.EAST, 4, 4);
        feature(level, box, ParkBuilder.Kind.BANK, Direction.WEST, 26, 4);

        // lights
        int[][] lamps = {{1, 1}, {1, 18}, {1, 27}, {36, 27}, {17, 36}, {28, 36}, {9, 1}, {24, 1}};
        for (int[] l : lamps) {
            fill(level, box, Blocks.IRON_BARS.defaultBlockState(), l[0], 1, l[1], l[0], 5, l[1]);
            put(level, box, CityRegistry.STREET_LAMP, l[0], 6, l[1]);
        }
        // benches along the west fence, facing the plaza
        for (int z : new int[]{16, 17, 23, 24}) {
            put(level, box, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST), 1, 1, z);
        }
        put(level, box, Blocks.BARREL, 1, 1, 20);   // a bin
        // spray paint on the back of the quarter pipe decks
        BlockState[] paint = {Blocks.MAGENTA_CONCRETE.defaultBlockState(), Blocks.LIME_CONCRETE.defaultBlockState(),
                Blocks.ORANGE_CONCRETE.defaultBlockState(), Blocks.CYAN_CONCRETE.defaultBlockState()};
        for (int z = 8; z <= 12; z++) for (int y = 1; y <= 2; y++) put(level, box, paint[(z + y) % 4], 2, y, z);

        boardshop(level, box);
    }

    /** A little shop in the north-east corner: brick walls, a flat roof, a counter facing the park. */
    private void boardshop(WorldGenLevel level, BoundingBox box) {
        int x0 = 30, x1 = 36, z0 = 1, z1 = 7;
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            boolean wall = x == x0 || x == x1 || z == z0 || z == z1;
            put(level, box, Blocks.SPRUCE_PLANKS, x, 0, z);
            for (int y = 1; y <= 4; y++) put(level, box, wall ? (y == 3 && x != x0 ? Blocks.GLASS : Blocks.BRICKS) : Blocks.AIR, x, y, z);
            put(level, box, Blocks.BLACK_CONCRETE, x, 5, z);
        }
        // the counter window on the park side, with a striped awning
        for (int z = 2; z <= 6; z++) {
            put(level, box, facing(dev.freerun.food.FoodRegistry.COUNTER, Direction.WEST), x0, 1, z);
            put(level, box, Blocks.AIR, x0, 2, z);
            put(level, box, Blocks.AIR, x0, 3, z);
            put(level, box, (z & 1) == 0 ? Blocks.LIGHT_BLUE_WOOL : Blocks.WHITE_WOOL, x0 - 1, 4, z);
        }
        put(level, box, Blocks.AIR, x0 + 3, 1, z1);   // a back door
        put(level, box, Blocks.AIR, x0 + 3, 2, z1);
        for (int z = 2; z <= 6; z += 2) put(level, box, facing(dev.freerun.food.FoodRegistry.STORE_SHELF, Direction.WEST), x1 - 1, 1, z);
        sign(level, box, x0 - 1, 3, 4, Direction.WEST, "§lBOARDSHOP", "Decks · Ramps", "Rails · Kits", "Shred responsibly");
        staff(level, box, x0 + 2, 4, 90.0f, RoadhouseNpcEntity.Role.SHOPKEEPER, Shops.Shop.SKATE, new ItemStack(Skating.BOARDS[0]));
    }
}
