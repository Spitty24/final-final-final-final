package dev.freerun.city;

import dev.freerun.economy.EconomyRegistry;
import dev.freerun.economy.Shops;
import dev.freerun.food.FoodRegistry;
import dev.freerun.life.LifeRegistry;
import dev.freerun.mobs.RoadhouseNpcEntity;
import dev.freerun.world.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Three kinds of place to spend an evening (and your money), each a 26 x 20 building on a
 * 38 x 38 lot with parking behind:
 *
 *   ARCADE     Pixel Bayou Arcade: rows of cabinets, claw machines, a dart board and the prize counter
 *   BAR        The Rusty Pelican: a long bar with stools and taps, booths, pool tables, darts, a jukebox and a stage
 *   FURNITURE  Bayou Home Furnishings: a showroom of living rooms, kitchens, bedrooms, bathrooms and home offices
 */
public class FunPiece extends CityPieceBase {
    public static final int ARCADE = 0, BAR = 1, FURNITURE = 2;
    private static final int X0 = 6, X1 = 31, Z0 = 3, Z1 = 22, H = 6;
    private final int kind;

    public FunPiece(BlockPos city, int lx, int lz, int w, int d, int kind) {
        super(CityRegistry.FUN_PIECE, city, lx, lz, w, 14, d);
        this.kind = kind;
    }

    public FunPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(CityRegistry.FUN_PIECE, tag);
        kind = tag.getInt("Kind");
    }

    @Override
    protected void save(CompoundTag tag) {
        tag.putInt("Kind", kind);
    }

    private static BlockState f(Block b, Direction d) {
        return b.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, d);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        ground(level, box, Blocks.GRASS_BLOCK.defaultBlockState(), 14);
        fill(level, box, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), 2, 0, 0, 35, 0, 2);
        LotPiece.parking(level, box, this, 2, 25, 34, 12, 3);
        BlockState wall = switch (kind) {
            case ARCADE -> Blocks.BLACK_CONCRETE.defaultBlockState();
            case BAR -> Blocks.DARK_OAK_PLANKS.defaultBlockState();
            default -> Blocks.WHITE_TERRACOTTA.defaultBlockState();
        };
        BlockState trim = switch (kind) {
            case ARCADE -> Blocks.MAGENTA_CONCRETE.defaultBlockState();
            case BAR -> Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState();
            default -> Blocks.BROWN_TERRACOTTA.defaultBlockState();
        };
        BlockState floor = switch (kind) {
            case ARCADE -> Blocks.BLACK_CONCRETE.defaultBlockState();
            case BAR -> Blocks.SPRUCE_PLANKS.defaultBlockState();
            default -> Blocks.BIRCH_PLANKS.defaultBlockState();
        };
        for (int x = X0; x <= X1; x++) {
            for (int z = Z0; z <= Z1; z++) {
                boolean edge = x == X0 || x == X1 || z == Z0 || z == Z1;
                put(level, box, floor, x, 0, z);
                for (int y = 1; y < H; y++) {
                    BlockState s = Blocks.AIR.defaultBlockState();
                    if (edge) {
                        boolean window = z == Z0 && (y == 2 || y == 3) && x > X0 + 2 && x < X1 - 2 && (x - X0) % 4 != 0;
                        s = y == H - 1 ? trim : window ? (kind == ARCADE ? Blocks.PURPLE_STAINED_GLASS : Blocks.GLASS).defaultBlockState() : wall;
                    }
                    put(level, box, s, x, y, z);
                }
                put(level, box, Blocks.SMOOTH_STONE, x, H, z);
            }
        }
        fill(level, box, Blocks.AIR.defaultBlockState(), 18, 1, Z0, 19, 3, Z0);
        for (int x = X0 + 3; x < X1 - 1; x += 5) for (int z = Z0 + 3; z < Z1 - 1; z += 5) put(level, box, Blocks.SEA_LANTERN, x, H, z);
        // the sign over the door
        fill(level, box, wall, X0 + 2, H + 1, Z0, X1 - 2, H + 7, Z0);
        String name = kind == ARCADE ? "ARCADE" : kind == BAR ? "BAR" : "FURNITURE";
        BlockState paint = (kind == ARCADE ? Blocks.LIME_CONCRETE : kind == BAR ? Blocks.ORANGE_CONCRETE : Blocks.BROWN_CONCRETE).defaultBlockState();
        bigTextCentred(level, box, X0, X1, H + 6, Z0 - 1, Direction.NORTH, name, paint);
        switch (kind) {
            case ARCADE -> arcade(level, box);
            case BAR -> bar(level, box);
            default -> furniture(level, box);
        }
    }

    // ------------------------------------------------------------------ Pixel Bayou Arcade

    private void arcade(WorldGenLevel level, BoundingBox box) {
        sign(level, box, 16, 4, Z0 - 1, Direction.NORTH, "", "§lPIXEL BAYOU", "ARCADE", "$1 a play");
        sign(level, box, 21, 4, Z0 - 1, Direction.NORTH, "Win tickets!", "Trade them", "for prizes", "inside.");
        // a neon carpet pattern
        Block[] neon = {Blocks.PURPLE_CARPET, Blocks.BLUE_CARPET, Blocks.MAGENTA_CARPET, Blocks.CYAN_CARPET};
        for (int x = X0 + 1; x < X1; x++) for (int z = Z0 + 1; z < Z1; z++) {
            if ((x * 3 + z * 5) % 7 < 2) put(level, box, neon[(x + z) % neon.length], x, 1, z);
        }
        // cabinets down both side walls and back to back in the middle
        int n = 0;
        for (int z = Z0 + 3; z <= Z1 - 3; z += 2) {
            cabinet(level, box, X0 + 1, z, Direction.EAST, n++);
            cabinet(level, box, X1 - 1, z, Direction.WEST, n++);
        }
        for (int x = X0 + 7; x <= X1 - 7; x += 2) {
            cabinet(level, box, x, 12, Direction.NORTH, n++);
            cabinet(level, box, x, 13, Direction.SOUTH, n++);
        }
        // claw machines and a dart board by the door, the prize counter at the back
        put(level, box, f(LifeRegistry.CLAW_MACHINE, Direction.SOUTH), X0 + 2, 1, Z0 + 1);
        put(level, box, f(LifeRegistry.CLAW_MACHINE, Direction.SOUTH), X1 - 2, 1, Z0 + 1);
        put(level, box, f(LifeRegistry.DART_BOARD, Direction.SOUTH), X0 + 5, 2, Z0 + 1);
        for (int x = 14; x <= 23; x++) {
            put(level, box, f(FoodRegistry.COUNTER, Direction.NORTH), x, 1, Z1 - 4);
            if (x % 2 == 0) put(level, box, Blocks.GLASS, x, 2, Z1 - 4);
        }
        for (int x = 14; x <= 23; x++) put(level, box, f(FoodRegistry.STORE_SHELF, Direction.NORTH), x, 1, Z1 - 1);
        put(level, box, Blocks.CAKE, 15, 2, Z1 - 4);
        put(level, box, Blocks.JUKEBOX, X0 + 1, 1, Z1 - 1);
        put(level, box, f(EconomyRegistry.ATM, Direction.SOUTH), X0 + 3, 1, Z0 + 1);
        put(level, box, f(EconomyRegistry.VENDING_MACHINE, Direction.SOUTH), X1 - 4, 1, Z0 + 1);
        sign(level, box, 18, 3, Z1 - 4, Direction.NORTH, "", "PRIZES", "Tickets only", "");
        staff(level, box, 18, Z1 - 3, 180.0f, RoadhouseNpcEntity.Role.CLERK, Shops.Shop.PRIZES, new ItemStack(LifeRegistry.TICKET));
        for (int x = X0 + 2; x < X1 - 1; x += 3) put(level, box, (x % 2 == 0 ? Blocks.MAGENTA_STAINED_GLASS : Blocks.CYAN_STAINED_GLASS), x, H - 1, Z1);
    }

    private void cabinet(WorldGenLevel level, BoundingBox box, int x, int z, Direction face, int n) {
        put(level, box, LifeRegistry.ARCADE_CABINET.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, face)
                .setValue(dev.freerun.life.ArcadeBlock.GAME, n % 3), x, 1, z);
    }

    // ------------------------------------------------------------------ The Rusty Pelican

    private void bar(WorldGenLevel level, BoundingBox box) {
        sign(level, box, 16, 4, Z0 - 1, Direction.NORTH, "", "§lTHE RUSTY", "§lPELICAN", "Bar & Grill");
        sign(level, box, 21, 4, Z0 - 1, Direction.NORTH, "Pool · Darts", "Live music", "Fri & Sat", "Happy hour 4-6");
        // the bar along the back wall: counter, taps, stools, shelves of bottles behind
        for (int x = X0 + 3; x <= X1 - 3; x++) {
            put(level, box, f(FoodRegistry.COUNTER, Direction.NORTH), x, 1, Z1 - 4);
            if (x % 3 == 0) put(level, box, f(LifeRegistry.BAR_TAP, Direction.SOUTH), x, 2, Z1 - 4);
            if (x % 2 == 0) put(level, box, f(LifeRegistry.BAR_STOOL, Direction.SOUTH), x, 1, Z1 - 5);
            put(level, box, f(FoodRegistry.STORE_SHELF, Direction.NORTH), x, 1, Z1 - 1);
            put(level, box, f(FoodRegistry.STORE_SHELF, Direction.NORTH), x, 2, Z1 - 1);
            if (x % 2 == 1) put(level, box, (x % 4 == 1 ? Blocks.GREEN_STAINED_GLASS : Blocks.BROWN_STAINED_GLASS), x, 3, Z1 - 1);
        }
        put(level, box, Blocks.AIR, X1 - 3, 1, Z1 - 4);   // the gap to get behind the bar
        put(level, box, f(LifeRegistry.FRIDGE, Direction.NORTH), X0 + 2, 1, Z1 - 1);
        put(level, box, f(FoodRegistry.COFFEE_MAKER, Direction.NORTH), X0 + 3, 1, Z1 - 3);
        staff(level, box, 14, Z1 - 3, 180.0f, RoadhouseNpcEntity.Role.CLERK, Shops.Shop.BAR, new ItemStack(Items.GLASS_BOTTLE));
        staff(level, box, 23, Z1 - 3, 180.0f, RoadhouseNpcEntity.Role.CLERK, Shops.Shop.BAR, new ItemStack(EconomyRegistry.BAYOU_BREW));
        put(level, box, f(EconomyRegistry.TV, Direction.NORTH).setValue(dev.freerun.economy.StoreBlocks.Tv.ON, true), 18, 4, Z1 - 1);
        // two pool tables, each three by two
        for (int[] t : new int[][]{{X0 + 3, Z0 + 5}, {X0 + 3, Z0 + 10}}) {
            for (int x = t[0]; x < t[0] + 3; x++) for (int z = t[1]; z < t[1] + 2; z++) put(level, box, f(LifeRegistry.POOL_TABLE, Direction.NORTH), x, 1, z);
            put(level, box, Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true), t[0] + 1, H - 1, t[1]);
        }
        fill(level, box, Blocks.BIRCH_FENCE.defaultBlockState(), X0 + 1, 1, Z0 + 15, X0 + 1, 2, Z0 + 15);   // cue rack
        // darts on the west wall
        put(level, box, f(LifeRegistry.DART_BOARD, Direction.EAST), X0 + 1, 2, Z0 + 4);
        put(level, box, f(LifeRegistry.DART_BOARD, Direction.EAST), X0 + 1, 2, Z0 + 8);
        // booths by the windows
        for (int x = 15; x <= 27; x += 6) {
            put(level, box, f(LifeRegistry.SOFA, Direction.SOUTH), x, 1, Z0 + 1);
            put(level, box, f(LifeRegistry.SOFA, Direction.SOUTH), x + 1, 1, Z0 + 1);
            put(level, box, BlockRegistry.TABLE, x, 1, Z0 + 2);
            put(level, box, BlockRegistry.TABLE, x + 1, 1, Z0 + 2);
            put(level, box, f(LifeRegistry.SOFA, Direction.NORTH), x, 1, Z0 + 3);
            put(level, box, f(LifeRegistry.SOFA, Direction.NORTH), x + 1, 1, Z0 + 3);
        }
        // the stage, the jukebox, an arcade cabinet in the corner
        fill(level, box, Blocks.SPRUCE_SLAB.defaultBlockState(), X1 - 6, 1, Z0 + 7, X1 - 1, 1, Z0 + 11);
        put(level, box, f(LifeRegistry.PIANO, Direction.WEST), X1 - 1, 2, Z0 + 9);
        put(level, box, Blocks.NOTE_BLOCK, X1 - 3, 2, Z0 + 8);
        put(level, box, Blocks.JUKEBOX, X1 - 1, 1, Z0 + 13);
        cabinet(level, box, X1 - 1, Z0 + 15, Direction.WEST, 1);
        sign(level, box, X1 - 1, 3, Z0 + 11, Direction.WEST, "", "OPEN MIC", "Thursdays", "");
    }

    // ------------------------------------------------------------------ Bayou Home Furnishings

    private void furniture(WorldGenLevel level, BoundingBox box) {
        sign(level, box, 16, 4, Z0 - 1, Direction.NORTH, "", "§lBAYOU HOME", "§lFURNISHINGS", "");
        sign(level, box, 21, 4, Z0 - 1, Direction.NORTH, "Sofas · Kitchens", "Bedrooms", "Bathrooms", "Free delivery*");
        // living room (front left)
        fill(level, box, LifeRegistry.RUG.defaultBlockState(), X0 + 2, 1, Z0 + 2, X0 + 6, 1, Z0 + 6);
        put(level, box, f(LifeRegistry.SOFA, Direction.SOUTH), X0 + 3, 1, Z0 + 2);
        put(level, box, f(LifeRegistry.SOFA, Direction.SOUTH), X0 + 4, 1, Z0 + 2);
        put(level, box, f(LifeRegistry.SOFA, Direction.SOUTH), X0 + 5, 1, Z0 + 2);
        put(level, box, f(LifeRegistry.ARMCHAIR, Direction.EAST), X0 + 2, 1, Z0 + 4);
        put(level, box, f(LifeRegistry.COFFEE_TABLE, Direction.NORTH), X0 + 4, 1, Z0 + 4);
        put(level, box, f(LifeRegistry.FLOOR_LAMP, Direction.NORTH).setValue(dev.freerun.life.FurnitureBlock.LIT, true), X0 + 6, 1, Z0 + 2);
        put(level, box, f(EconomyRegistry.TV, Direction.NORTH).setValue(dev.freerun.economy.StoreBlocks.Tv.ON, true), X0 + 4, 1, Z0 + 7);
        put(level, box, f(LifeRegistry.FIREPLACE, Direction.EAST).setValue(dev.freerun.life.FurnitureBlock.LIT, true), X0 + 1, 1, Z0 + 6);
        // kitchen (front right)
        int k = X1 - 8;
        put(level, box, f(LifeRegistry.FRIDGE, Direction.SOUTH), k, 1, Z0 + 1);
        put(level, box, f(FoodRegistry.COUNTER, Direction.SOUTH), k + 1, 1, Z0 + 1);
        put(level, box, f(FoodRegistry.MICROWAVE, Direction.SOUTH), k + 1, 2, Z0 + 1);
        put(level, box, f(LifeRegistry.STOVE, Direction.SOUTH), k + 2, 1, Z0 + 1);
        put(level, box, f(LifeRegistry.KITCHEN_SINK, Direction.SOUTH), k + 3, 1, Z0 + 1);
        put(level, box, f(FoodRegistry.COUNTER, Direction.SOUTH), k + 4, 1, Z0 + 1);
        put(level, box, f(FoodRegistry.COFFEE_MAKER, Direction.SOUTH), k + 4, 2, Z0 + 1);
        put(level, box, BlockRegistry.TABLE, k + 2, 1, Z0 + 4);
        put(level, box, f(BlockRegistry.CHAIR, Direction.EAST), k + 1, 1, Z0 + 4);
        put(level, box, f(BlockRegistry.CHAIR, Direction.WEST), k + 3, 1, Z0 + 4);
        // bedroom (back left)
        Direction bed = Direction.SOUTH;
        put(level, box, Blocks.BLUE_BED.defaultBlockState().setValue(net.minecraft.world.level.block.BedBlock.FACING, bed)
                .setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.FOOT), X0 + 3, 1, Z1 - 4);
        put(level, box, Blocks.BLUE_BED.defaultBlockState().setValue(net.minecraft.world.level.block.BedBlock.FACING, bed)
                .setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.HEAD), X0 + 3, 1, Z1 - 3);
        put(level, box, f(LifeRegistry.NIGHTSTAND, Direction.NORTH), X0 + 2, 1, Z1 - 2);
        put(level, box, f(LifeRegistry.WARDROBE, Direction.NORTH), X0 + 6, 1, Z1 - 1);
        put(level, box, f(LifeRegistry.BIG_PLANT, Direction.NORTH), X0 + 1, 1, Z1 - 1);
        put(level, box, f(LifeRegistry.GRANDFATHER_CLOCK, Direction.NORTH), X0 + 8, 1, Z1 - 1);
        // bathroom (back right)
        put(level, box, f(LifeRegistry.TOILET, Direction.NORTH), X1 - 2, 1, Z1 - 2);
        put(level, box, f(LifeRegistry.BATHTUB, Direction.WEST), X1 - 4, 1, Z1 - 1);
        put(level, box, f(LifeRegistry.BATHTUB, Direction.WEST), X1 - 5, 1, Z1 - 1);
        put(level, box, f(LifeRegistry.KITCHEN_SINK, Direction.WEST), X1 - 1, 1, Z1 - 4);
        // home office and the piano in the middle
        put(level, box, f(LifeRegistry.OFFICE_DESK, Direction.SOUTH), 17, 1, 12);
        put(level, box, f(LifeRegistry.COMPUTER, Direction.SOUTH), 17, 2, 12);
        put(level, box, f(LifeRegistry.OFFICE_CHAIR, Direction.NORTH), 17, 1, 13);
        put(level, box, f(LifeRegistry.FILING_CABINET, Direction.SOUTH), 18, 1, 12);
        put(level, box, f(LifeRegistry.PIANO, Direction.NORTH), 21, 1, 12);
        put(level, box, f(LifeRegistry.BAR_STOOL, Direction.NORTH), 21, 1, 11);
        // the register
        for (int x = 15; x <= 21; x++) put(level, box, f(FoodRegistry.COUNTER, Direction.NORTH), x, 1, Z0 + 8);
        put(level, box, Blocks.SMOOTH_STONE_SLAB, 18, 2, Z0 + 8);
        staff(level, box, 18, Z0 + 9, 180.0f, RoadhouseNpcEntity.Role.SHOPKEEPER, Shops.Shop.FURNITURE, new ItemStack(LifeRegistry.SOFA));
        sign(level, box, 20, 3, Z0 + 8, Direction.NORTH, "Everything", "you see is", "for sale. Ask", "at the register.");
    }
}
