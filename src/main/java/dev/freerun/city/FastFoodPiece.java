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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * A fast-food joint on a 38 x 38 lot: Big Bun Burgers or the Cluck Shack. A small restaurant
 * with a counter, a kitchen (grills, fryers, the shake machine), a few booths and a patio, a
 * parking strip out front, and a drive-thru lane that wraps around the building: in along the
 * west side, past the menu board on the south side, and out past the pickup window on the east.
 *
 *   z 1..8 parking   z 11..23 restaurant (x 11..26)   lane: x 5..8 down, z 26..29 across, x 29..32 up
 */
public class FastFoodPiece extends CityPieceBase {
    private final Shops.Shop brand;

    public FastFoodPiece(BlockPos city, int lx, int lz, int w, int d, Shops.Shop brand) {
        super(CityRegistry.FAST_FOOD_PIECE, city, lx, lz, w, 14, d);
        this.brand = brand;
    }

    public FastFoodPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(CityRegistry.FAST_FOOD_PIECE, tag);
        brand = Shops.Shop.byId(tag.getInt("Brand"));
    }

    @Override
    protected void save(CompoundTag tag) {
        tag.putInt("Brand", brand.ordinal());
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        boolean burger = brand == Shops.Shop.BURGER;
        BlockState wall = (burger ? Blocks.ORANGE_TERRACOTTA : Blocks.WHITE_TERRACOTTA).defaultBlockState();
        BlockState trim = (burger ? Blocks.YELLOW_CONCRETE : Blocks.RED_CONCRETE).defaultBlockState();
        BlockState roof = (burger ? Blocks.RED_CONCRETE : Blocks.RED_TERRACOTTA).defaultBlockState();
        BlockState asphalt = Blocks.BLACK_CONCRETE.defaultBlockState();
        BlockState yellow = Blocks.YELLOW_CONCRETE.defaultBlockState();

        ground(level, box, Blocks.GRASS_BLOCK.defaultBlockState(), 14);
        // parking strip along the front, and the paved lot around the building
        fill(level, box, asphalt, 0, 0, 0, w - 1, 0, 10);
        LotPiece.parking(level, box, this, 10, 2, 22, 7, 3);
        fill(level, box, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), 10, 0, 9, 27, 0, 10);     // front walk
        // drive-thru lane: down the west side, across the back, up the east side
        fill(level, box, asphalt, 4, 0, 9, 9, 0, 30);
        fill(level, box, asphalt, 4, 0, 25, 33, 0, 30);
        fill(level, box, asphalt, 28, 0, 9, 33, 0, 30);
        for (int z = 11; z <= 24; z += 3) {
            put(level, box, yellow, 4, 0, z);
            put(level, box, yellow, 33, 0, z);
        }
        for (int x = 11; x <= 26; x += 3) put(level, box, yellow, x, 0, 30);
        // painted arrows (chevrons) showing the way round
        put(level, box, Blocks.WHITE_CONCRETE, 6, 0, 14);
        put(level, box, Blocks.WHITE_CONCRETE, 7, 0, 15);
        put(level, box, Blocks.WHITE_CONCRETE, 8, 0, 14);
        put(level, box, Blocks.WHITE_CONCRETE, 30, 0, 16);
        put(level, box, Blocks.WHITE_CONCRETE, 31, 0, 15);
        put(level, box, Blocks.WHITE_CONCRETE, 32, 0, 16);
        sign(level, box, 3, 2, 9, Direction.NORTH, "", "DRIVE-THRU", "↓ this way ↓", "");
        fill(level, box, Blocks.SPRUCE_FENCE.defaultBlockState(), 3, 1, 9, 3, 1, 9);
        put(level, box, Blocks.SPRUCE_FENCE, 3, 1, 9);
        sign(level, box, 34, 2, 9, Direction.NORTH, "", "EXIT", "Thank you!", "");
        put(level, box, Blocks.SPRUCE_FENCE, 34, 1, 9);

        // ---- the building
        int x0 = 11, x1 = 26, z0 = 11, z1 = 23;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
                put(level, box, Blocks.WHITE_CONCRETE, x, 0, z);
                for (int y = 1; y <= 5; y++) {
                    BlockState s = Blocks.AIR.defaultBlockState();
                    if (edge) s = y == 5 ? trim : (y == 2 || y == 3) && z == z0 && (x - x0) % 3 != 0 ? Blocks.GLASS.defaultBlockState() : wall;
                    put(level, box, s, x, y, z);
                }
                put(level, box, roof, x, 6, z);
            }
        }
        // checkered floor inside
        for (int x = x0 + 1; x < x1; x++) for (int z = z0 + 1; z < z1; z++) {
            put(level, box, ((x + z) % 2 == 0 ? Blocks.WHITE_CONCRETE : (burger ? Blocks.RED_CONCRETE : Blocks.BLACK_CONCRETE)), x, 0, z);
        }
        // front door
        put(level, box, Blocks.AIR, 18, 1, z0);
        put(level, box, Blocks.AIR, 18, 2, z0);
        put(level, box, Blocks.AIR, 19, 1, z0);
        put(level, box, Blocks.AIR, 19, 2, z0);
        // roof sign
        String name = burger ? "BIG BUN" : "CLUCK";
        bigTextCentred(level, box, x0, x1, 11, z0, Direction.NORTH, name, (burger ? Blocks.YELLOW_CONCRETE : Blocks.WHITE_CONCRETE).defaultBlockState());
        fill(level, box, roof, x0, 7, z0 + 1, x1, 7, z0 + 1);
        sign(level, box, 16, 4, z0 - 1, Direction.NORTH, "", burger ? "§lBIG BUN" : "§lCLUCK SHACK", burger ? "BURGERS" : "Fried Chicken", "Open late");
        // pole sign by the road
        fill(level, box, Blocks.IRON_BARS.defaultBlockState(), 1, 1, 2, 1, 7, 2);
        fill(level, box, trim, 0, 8, 2, 2, 9, 2);
        sign(level, box, 1, 9, 1, Direction.NORTH, "", burger ? "BIG BUN" : "CLUCK", burger ? "BURGERS" : "SHACK", "");

        // ---- inside: booths up front, counter, kitchen
        for (int x : new int[]{x0 + 2, x0 + 5, x1 - 5, x1 - 2}) {
            put(level, box, BlockRegistry.TABLE, x, 1, z0 + 2);
            put(level, box, facing(BlockRegistry.CHAIR, Direction.EAST), x - 1, 1, z0 + 2);
            put(level, box, facing(BlockRegistry.CHAIR, Direction.WEST), x + 1, 1, z0 + 2);
        }
        for (int x = x0 + 2; x <= x1 - 2; x++) put(level, box, facing(FoodRegistry.COUNTER, Direction.NORTH), x, 1, z0 + 5);
        put(level, box, Blocks.AIR, x1 - 1, 1, z0 + 5);
        put(level, box, Blocks.SMOOTH_STONE_SLAB, 17, 2, z0 + 5);   // register
        put(level, box, Blocks.SMOOTH_STONE_SLAB, 20, 2, z0 + 5);
        put(level, box, facing(EconomyRegistry.TV, Direction.NORTH).setValue(dev.freerun.economy.StoreBlocks.Tv.ON, true), 18, 4, z0 + 6);
        put(level, box, facing(EconomyRegistry.TV, Direction.NORTH).setValue(dev.freerun.economy.StoreBlocks.Tv.ON, true), 19, 4, z0 + 6);
        for (int x = x0 + 1; x <= x0 + 4; x++) put(level, box, facing(BlockRegistry.GRILL, Direction.NORTH), x, 1, z1 - 1);
        put(level, box, Blocks.SMOKER.defaultBlockState().setValue(net.minecraft.world.level.block.SmokerBlock.FACING, Direction.NORTH), x0 + 6, 1, z1 - 1);
        put(level, box, Blocks.CAULDRON, x0 + 7, 1, z1 - 1);                          // the fryer
        put(level, box, Blocks.CAULDRON, x0 + 8, 1, z1 - 1);
        put(level, box, facing(EconomyRegistry.SLUSHIE_MACHINE, Direction.NORTH), x0 + 10, 1, z1 - 1);
        put(level, box, facing(FoodRegistry.COOLER, Direction.NORTH), x0 + 12, 1, z1 - 1);
        put(level, box, facing(FoodRegistry.COOLER, Direction.NORTH), x0 + 12, 2, z1 - 1);
        for (int x = x0 + 2; x <= x0 + 9; x++) put(level, box, facing(FoodRegistry.COUNTER, Direction.SOUTH), x, 1, z0 + 8);
        for (int[] l : new int[][]{{x0 + 4, z0 + 3}, {x1 - 4, z0 + 3}, {x0 + 5, z1 - 3}, {x1 - 5, z1 - 3}}) put(level, box, Blocks.GLOWSTONE, l[0], 5, l[1]);
        sign(level, box, 15, 3, z0 + 6, Direction.NORTH, "§lMENU", burger ? "Big Bun  $6" : "Cluck Sandwich $6",
                burger ? "Bacon Stacker $8" : "Spicy Cluck $7", burger ? "Shakes  $4" : "Wings  $7");

        // ---- drive-thru: menu board on the back lane, pickup window on the east wall
        put(level, box, facing(CityRegistry.DRIVE_THRU_SPEAKER, Direction.SOUTH), 15, 1, z1 + 2);
        put(level, box, trim, 15, 2, z1 + 2);
        sign(level, box, 15, 2, z1 + 3, Direction.SOUTH, "ORDER HERE", "Right-click", "the board", "from your car");
        put(level, box, facing(CityRegistry.PICKUP_WINDOW, Direction.EAST), x1, 1, 17);
        put(level, box, Blocks.GLASS, x1, 2, 17);
        put(level, box, Blocks.GLASS, x1, 2, 18);
        put(level, box, facing(CityRegistry.PICKUP_WINDOW, Direction.EAST), x1, 1, 18);
        fill(level, box, (burger ? Blocks.YELLOW_WOOL : Blocks.RED_WOOL).defaultBlockState(), x1 + 1, 4, 15, x1 + 1, 4, 20);   // awning
        sign(level, box, x1 + 1, 3, 16, Direction.EAST, "", "PICKUP", "WINDOW", "");
        // the crew: one at the register, one on the window
        staff(level, box, 18, z0 + 6, 180.0f, RoadhouseNpcEntity.Role.CLERK, brand, new ItemStack(burger ? EconomyRegistry.BIG_BUN_BURGER : EconomyRegistry.CLUCK_SANDWICH));
        staff(level, box, x1 - 1, 17, -90.0f, RoadhouseNpcEntity.Role.CLERK, brand, new ItemStack(Items.PAPER));

        // ---- patio and planters
        for (int x : new int[]{x0 + 1, x1 - 1}) {
            put(level, box, BlockRegistry.TABLE, x, 1, z0 - 2);
            put(level, box, Blocks.SPRUCE_FENCE, x, 2, z0 - 2);
            put(level, box, (burger ? Blocks.YELLOW_CARPET : Blocks.RED_CARPET), x, 3, z0 - 2);
        }
        put(level, box, Blocks.RED_CONCRETE_POWDER, 35, 1, 35);
        hedge(level, box, 0, 32, w - 1, 37);
        put(level, box, Blocks.SPRUCE_FENCE.defaultBlockState().setValue(FenceBlock.EAST, true), 2, 1, 33);
    }

    private void hedge(WorldGenLevel level, BoundingBox box, int x0, int z0, int x1, int z1) {
        BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true);
        for (int x = x0; x <= x1; x++) {
            put(level, box, leaves, x, 1, z1);
            if (x % 6 == 0) {
                for (int y = 1; y <= 3; y++) put(level, box, Blocks.OAK_LOG, x, y, z1 - 2);
                for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) put(level, box, leaves, x + dx, 4, z1 - 2 + dz);
            }
        }
        Block grass = Blocks.GRASS_BLOCK;
        for (int x = x0; x <= x1; x++) for (int z = z0 + 1; z <= z1; z++) put(level, box, grass, x, 0, z);
    }
}
