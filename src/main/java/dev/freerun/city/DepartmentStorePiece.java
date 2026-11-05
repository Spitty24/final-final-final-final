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
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Hargrove &amp; Finch, the old department store: a two-storey brick-and-stone building with a
 * clock over the doors. Tools and hardware, appliances, home goods, and apparel downstairs;
 * furniture and electronics up the grand staircase; an Auto Center wing on the side with its
 * own service bays and a car on the showroom floor.
 *
 *   z 0..27 parking   z 30..77 the store (x 4..60)   x 62..77 the Auto Center
 */
public class DepartmentStorePiece extends CityPieceBase {
    public DepartmentStorePiece(BlockPos city, int lx, int lz, int w, int d) {
        super(CityRegistry.DEPARTMENT_PIECE, city, lx, lz, w, 22, d);
    }

    public DepartmentStorePiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(CityRegistry.DEPARTMENT_PIECE, tag);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        ground(level, box, Blocks.BLACK_CONCRETE.defaultBlockState(), 22);
        LotPiece.parking(level, box, this, 2, 2, 76, 25, 4);
        fill(level, box, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), 2, 0, 27, 77, 0, 29);

        int x0 = 4, x1 = 60, z0 = 30, z1 = 77;
        BlockState brick = Blocks.BRICKS.defaultBlockState();
        BlockState stone = Blocks.SMOOTH_SANDSTONE.defaultBlockState();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
                put(level, box, Blocks.POLISHED_DIORITE, x, 0, z);
                for (int y = 1; y <= 13; y++) {
                    BlockState s = Blocks.AIR.defaultBlockState();
                    if (edge) {
                        boolean window = (y >= 2 && y <= 4 || y >= 8 && y <= 11) && ((z == z0 || z == z1) ? (x - x0) % 5 >= 2 : (z - z0) % 5 >= 2);
                        s = y == 6 || y == 13 ? stone : window ? Blocks.GLASS.defaultBlockState() : brick;
                    }
                    put(level, box, s, x, y, z);
                }
                put(level, box, Blocks.DARK_OAK_PLANKS, x, 6, z);     // second floor
                put(level, box, Blocks.SMOOTH_STONE, x, 14, z);
            }
        }
        for (int x = x0; x <= x1; x++) {   // the stone band shows outside
            put(level, box, stone, x, 6, z0);
            put(level, box, stone, x, 6, z1);
            put(level, box, Blocks.SMOOTH_SANDSTONE_SLAB, x, 15, z0);
        }
        for (int z = z0; z <= z1; z++) {
            put(level, box, stone, x0, 6, z);
            put(level, box, stone, x1, 6, z);
        }
        // grand entrance: columns, a clock, the name
        int mid = (x0 + x1) / 2;
        fill(level, box, Blocks.AIR.defaultBlockState(), mid - 2, 1, z0, mid + 2, 4, z0);
        for (int x : new int[]{mid - 4, mid + 4}) fill(level, box, Blocks.QUARTZ_PILLAR.defaultBlockState(), x, 1, z0 - 1, x, 5, z0 - 1);
        fill(level, box, Blocks.SMOOTH_QUARTZ.defaultBlockState(), mid - 5, 6, z0 - 2, mid + 5, 6, z0 - 1);
        fill(level, box, stone, x0 + 4, 14, z0, x1 - 4, 21, z0);
        bigTextCentred(level, box, x0, x1, 20, z0 - 1, Direction.NORTH, "HARGROVE", Blocks.PURPLE_CONCRETE.defaultBlockState());
        put(level, box, Blocks.WHITE_CONCRETE, mid, 9, z0 - 1);
        put(level, box, Blocks.BLACK_CONCRETE, mid, 10, z0 - 1);
        sign(level, box, mid, 8, z0 - 1, Direction.NORTH, "", "§lHARGROVE", "§l& FINCH", "Since 1886");
        sign(level, box, mid - 6, 3, z0 - 1, Direction.NORTH, "Tools", "Appliances", "Home", "Apparel");
        sign(level, box, mid + 6, 3, z0 - 1, Direction.NORTH, "Lifetime", "warranty on", "every Hargrove", "tool.");

        // ---- downstairs departments
        // tools & hardware (west)
        for (int z = z0 + 4; z <= z1 - 4; z += 4) {
            for (int x = x0 + 2; x <= x0 + 14; x++) {
                put(level, box, facing(FoodRegistry.STORE_SHELF, Direction.NORTH), x, 1, z);
                put(level, box, facing(FoodRegistry.STORE_SHELF, Direction.SOUTH), x, 1, z + 1);
            }
        }
        put(level, box, Blocks.ANVIL, x0 + 2, 1, z0 + 2);
        put(level, box, Blocks.GRINDSTONE, x0 + 4, 1, z0 + 2);
        put(level, box, Blocks.SMITHING_TABLE, x0 + 6, 1, z0 + 2);
        sign(level, box, x0 + 8, 4, z0 + 3, Direction.NORTH, "", "TOOLS &", "HARDWARE", "");
        // appliances (middle)
        for (int x = x0 + 20; x <= x0 + 32; x += 2) {
            put(level, box, facing(FoodRegistry.MICROWAVE, Direction.NORTH), x, 1, z0 + 12);
            put(level, box, facing(FoodRegistry.COFFEE_MAKER, Direction.NORTH), x, 1, z0 + 16);
            put(level, box, Blocks.FURNACE.defaultBlockState().setValue(net.minecraft.world.level.block.FurnaceBlock.FACING, Direction.NORTH), x, 1, z0 + 20);
            put(level, box, Blocks.SMOKER.defaultBlockState().setValue(net.minecraft.world.level.block.SmokerBlock.FACING, Direction.NORTH), x + 1, 1, z0 + 20);
        }
        sign(level, box, x0 + 26, 4, z0 + 10, Direction.NORTH, "", "APPLIANCES", "", "");
        // apparel and jewellery (east)
        Block[] carpets = {Blocks.PURPLE_CARPET, Blocks.MAGENTA_CARPET};
        for (int x = x1 - 16; x <= x1 - 2; x++) for (int z = z0 + 2; z <= z1 - 2; z++) put(level, box, carpets[(x / 4 + z / 4) % 2], x, 1, z);
        for (int z = z0 + 6; z <= z1 - 6; z += 5) {
            for (int x = x1 - 14; x <= x1 - 4; x += 5) {
                put(level, box, Blocks.SPRUCE_FENCE, x, 1, z);
                put(level, box, Blocks.SPRUCE_FENCE, x, 2, z);
                put(level, box, ((x + z) % 3 == 0 ? Blocks.LIGHT_BLUE_WOOL : (x + z) % 3 == 1 ? Blocks.WHITE_WOOL : Blocks.BROWN_WOOL), x, 3, z);
            }
        }
        for (int x = x1 - 14; x <= x1 - 6; x++) put(level, box, Blocks.GLASS, x, 1, z1 - 3);
        sign(level, box, x1 - 9, 4, z0 + 4, Direction.NORTH, "", "APPAREL &", "JEWELRY", "");
        // the registers
        for (int x = mid - 4; x <= mid + 4; x++) put(level, box, facing(FoodRegistry.COUNTER, Direction.NORTH), x, 1, z0 + 6);
        put(level, box, Blocks.SMOOTH_STONE_SLAB, mid, 2, z0 + 6);
        staff(level, box, mid, z0 + 7, 180.0f, RoadhouseNpcEntity.Role.SHOPKEEPER, Shops.Shop.DEPARTMENT, new ItemStack(Items.IRON_PICKAXE));
        staff(level, box, x1 - 9, z1 - 5, 180.0f, RoadhouseNpcEntity.Role.SHOPKEEPER, Shops.Shop.DEPARTMENT, new ItemStack(Items.GOLD_NUGGET));
        // grand staircase up the middle-back
        for (int i = 0; i < 6; i++) {
            for (int x = mid - 1; x <= mid + 1; x++) {
                put(level, box, Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH), x, 1 + i, z1 - 14 + i);
                put(level, box, Blocks.AIR, x, 6, z1 - 14 + i);
            }
        }
        // lights
        for (int x = x0 + 5; x < x1 - 2; x += 7) for (int z = z0 + 5; z < z1 - 2; z += 7) {
            put(level, box, Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true), x, 5, z);
            put(level, box, Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true), x, 12, z);
        }
        // ---- upstairs: furniture and electronics
        for (int x = x0 + 3; x <= x0 + 25; x += 6) {
            for (int z = z0 + 4; z <= z1 - 18; z += 7) {
                put(level, box, BlockRegistry.TABLE, x, 7, z);
                put(level, box, facing(BlockRegistry.CHAIR, Direction.NORTH), x, 7, z + 1);
                put(level, box, facing(BlockRegistry.CHAIR, Direction.SOUTH), x, 7, z - 1);
                put(level, box, Blocks.RED_BED.defaultBlockState().setValue(net.minecraft.world.level.block.BedBlock.FACING, Direction.SOUTH)
                        .setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.FOOT), x + 3, 7, z);
                put(level, box, Blocks.RED_BED.defaultBlockState().setValue(net.minecraft.world.level.block.BedBlock.FACING, Direction.SOUTH)
                        .setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.HEAD), x + 3, 7, z + 1);
            }
        }
        for (int z = z0 + 3; z <= z1 - 3; z++) {
            put(level, box, facing(EconomyRegistry.TV, Direction.WEST).setValue(dev.freerun.economy.StoreBlocks.Tv.ON, z % 3 != 0), x1 - 1, 8, z);
            put(level, box, facing(FoodRegistry.STORE_SHELF, Direction.WEST), x1 - 1, 7, z);
        }
        put(level, box, Blocks.JUKEBOX, x1 - 4, 7, z0 + 4);
        sign(level, box, mid, 9, z1 - 15, Direction.NORTH, "", "UPSTAIRS:", "Furniture &", "Electronics");

        // ---- the Auto Center
        int a0 = 62, a1 = 77;
        for (int x = a0; x <= a1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean edge = x == a0 || x == a1 || z == z0 || z == z1;
                put(level, box, Blocks.GRAY_CONCRETE, x, 0, z);
                for (int y = 1; y <= 7; y++) {
                    BlockState s = Blocks.AIR.defaultBlockState();
                    if (edge) s = y == 7 ? Blocks.PURPLE_CONCRETE.defaultBlockState() : Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
                    put(level, box, s, x, y, z);
                }
                put(level, box, Blocks.SMOOTH_STONE, x, 8, z);
            }
        }
        for (int bay = 0; bay < 3; bay++) {   // roll-up bay doors facing the lot
            int bx = a0 + 2 + bay * 5;
            fill(level, box, Blocks.AIR.defaultBlockState(), bx, 1, z0, bx + 3, 4, z0);
            put(level, box, Blocks.CHAIN, bx + 1, 5, z0 + 1);
        }
        sign(level, box, (a0 + a1) / 2, 6, z0 - 1, Direction.NORTH, "", "§lAUTO CENTER", "Tires · Oil", "Batteries");
        parkedCar(level, box, a0 + 4.5, z0 + 10.5, 0.0f);
        parkedCar(level, box, a0 + 10.5, z0 + 18.5, 0.0f);
        for (int z = z0 + 26; z <= z1 - 2; z += 2) put(level, box, Blocks.COAL_BLOCK, a1 - 2, 1, z);   // stacked tyres
        put(level, box, facing(EconomyRegistry.GAS_PUMP, Direction.WEST), a1 - 1, 1, z0 + 6);
        sign(level, box, a1 - 1, 3, z0 + 9, Direction.WEST, "Showroom car:", "$520", "Ask at the", "registers.");
    }
}
