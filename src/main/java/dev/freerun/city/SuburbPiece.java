package dev.freerun.city;

import dev.freerun.economy.EconomyRegistry;
import dev.freerun.food.FoodRegistry;
import dev.freerun.vehicle.CarEntity;
import dev.freerun.vehicle.VehicleRegistry;
import dev.freerun.world.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * A suburban block: four houses on 19 x 19 lots, each facing the street. Every house has a lawn,
 * a front walk, a driveway with the family car, a tree out back, and a furnished inside (bed,
 * kitchen with a microwave, dining table, TV). One or two residents live in each.
 */
public class SuburbPiece extends CityPieceBase {
    private static final int PLOT = 19;
    private static final Block[][] PALETTES = {
            {Blocks.WHITE_TERRACOTTA, Blocks.STRIPPED_OAK_LOG, Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_SLAB},
            {Blocks.LIGHT_BLUE_TERRACOTTA, Blocks.STRIPPED_BIRCH_LOG, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB},
            {Blocks.YELLOW_TERRACOTTA, Blocks.STRIPPED_SPRUCE_LOG, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB},
            {Blocks.BRICKS, Blocks.STRIPPED_DARK_OAK_LOG, Blocks.STONE_BRICK_STAIRS, Blocks.STONE_BRICK_SLAB},
            {Blocks.BIRCH_PLANKS, Blocks.STRIPPED_OAK_LOG, Blocks.BRICK_STAIRS, Blocks.BRICK_SLAB},
            {Blocks.LIGHT_GRAY_TERRACOTTA, Blocks.STRIPPED_MANGROVE_LOG, Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_SLAB},
    };

    public SuburbPiece(BlockPos city, int lx, int lz, int w, int d) {
        super(CityRegistry.SUBURB_PIECE, city, lx, lz, w, 16, d);
    }

    public SuburbPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(CityRegistry.SUBURB_PIECE, tag);
    }

    // plot-relative helpers: houses on the south row are mirrored so they face south
    private int bx, bz;
    private boolean flip;

    private int zz(int z) {
        return bz + (flip ? PLOT - 1 - z : z);
    }

    private Direction dir(Direction d) {
        return flip && d.getAxis() == Direction.Axis.Z ? d.getOpposite() : d;
    }

    private void hp(WorldGenLevel level, BoundingBox box, BlockState s, int x, int y, int z) {
        put(level, box, s, bx + x, y, zz(z));
    }

    private void hf(WorldGenLevel level, BoundingBox box, BlockState s, int x0, int y0, int z0, int x1, int y1, int z1) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) hp(level, box, s, x, y, z);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        ground(level, box, Blocks.GRASS_BLOCK.defaultBlockState(), 16);
        for (int px = 0; px < 2; px++) {
            for (int pz = 0; pz < 2; pz++) {
                bx = px * PLOT;
                bz = pz * PLOT;
                flip = pz == 1;
                int seed = Math.floorMod((ox + bx) * 31 + (oz + bz) * 17, 997);
                house(level, box, PALETTES[seed % PALETTES.length], seed);
            }
        }
    }

    private void house(WorldGenLevel level, BoundingBox box, Block[] pal, int seed) {
        BlockState wall = pal[0].defaultBlockState();
        BlockState corner = pal[1].defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        // front walk, driveway
        hf(level, box, Blocks.DIRT_PATH.defaultBlockState(), 9, 0, 0, 9, 0, 3);
        hf(level, box, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), 15, 0, 0, 17, 0, 11);
        // floor + walls
        hf(level, box, Blocks.OAK_PLANKS.defaultBlockState(), 4, 0, 4, 14, 0, 12);
        for (int x = 4; x <= 14; x++) {
            for (int z = 4; z <= 12; z++) {
                boolean edge = x == 4 || x == 14 || z == 4 || z == 12;
                if (!edge) continue;
                boolean c = (x == 4 || x == 14) && (z == 4 || z == 12);
                for (int y = 1; y <= 3; y++) {
                    BlockState s = c ? corner : wall;
                    boolean window = y == 2 && !c && ((z == 4 || z == 12) ? (x == 6 || x == 12) : (z == 7 || z == 10));
                    if (window) s = Blocks.GLASS.defaultBlockState();
                    hp(level, box, s, x, y, z);
                }
            }
        }
        // door
        BlockState door = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, dir(Direction.NORTH))
                .setValue(DoorBlock.HINGE, DoorHingeSide.LEFT).setValue(DoorBlock.OPEN, false);
        hp(level, box, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), 9, 1, 4);
        hp(level, box, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 9, 2, 4);
        hp(level, box, Blocks.LANTERN.defaultBlockState(), 10, 3, 3);
        // hip roof: three rings of stairs and a slab cap
        BlockState stair = pal[2].defaultBlockState();
        for (int ring = 0; ring < 3; ring++) {
            int x0 = 3 + ring, x1 = 15 - ring, z0 = 3 + ring, z1 = 13 - ring, y = 4 + ring;
            for (int x = x0; x <= x1; x++) {
                hp(level, box, stair.setValue(StairBlock.FACING, dir(Direction.SOUTH)), x, y, z0);
                hp(level, box, stair.setValue(StairBlock.FACING, dir(Direction.NORTH)), x, y, z1);
            }
            for (int z = z0 + 1; z < z1; z++) {
                hp(level, box, stair.setValue(StairBlock.FACING, Direction.EAST), x0, y, z);
                hp(level, box, stair.setValue(StairBlock.FACING, Direction.WEST), x1, y, z);
            }
        }
        hf(level, box, pal[3].defaultBlockState(), 6, 7, 6, 12, 7, 10);
        hf(level, box, Blocks.OAK_PLANKS.defaultBlockState(), 5, 4, 5, 13, 4, 11);   // ceiling
        hf(level, box, air, 5, 1, 5, 13, 3, 11);
        // inside
        hp(level, box, Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true), 9, 3, 8);
        Direction bedDir = dir(Direction.SOUTH);
        hp(level, box, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, bedDir).setValue(BedBlock.PART, BedPart.FOOT), 5, 1, 9);
        hp(level, box, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, bedDir).setValue(BedBlock.PART, BedPart.HEAD), 5, 1, 10);
        for (int x = 10; x <= 13; x++) hp(level, box, facing(FoodRegistry.COUNTER, dir(Direction.NORTH)), x, 1, 11);
        hp(level, box, facing(FoodRegistry.MICROWAVE, dir(Direction.NORTH)), 11, 2, 11);
        hp(level, box, facing(FoodRegistry.COFFEE_MAKER, dir(Direction.NORTH)), 12, 2, 11);
        hp(level, box, BlockRegistry.TABLE.defaultBlockState(), 9, 1, 8);
        hp(level, box, facing(BlockRegistry.CHAIR, Direction.EAST), 8, 1, 8);
        hp(level, box, facing(BlockRegistry.CHAIR, Direction.WEST), 10, 1, 8);
        hp(level, box, facing(EconomyRegistry.TV, Direction.WEST).setValue(dev.freerun.economy.StoreBlocks.Tv.ON, seed % 3 == 0), 13, 1, 6);
        hp(level, box, Blocks.POTTED_FERN.defaultBlockState(), 5, 1, 5);
        // yard: tree out back, flower bed by the door, mailbox
        tree(level, box, 3, 15 + (seed % 2));
        hp(level, box, Blocks.POPPY.defaultBlockState(), 7, 1, 3);
        hp(level, box, Blocks.DANDELION.defaultBlockState(), 11, 1, 3);
        hp(level, box, Blocks.SPRUCE_FENCE.defaultBlockState(), 13, 1, 1);
        hp(level, box, Blocks.POTTED_RED_TULIP.defaultBlockState(), 13, 2, 1);
        // the family car on the driveway, nose to the street
        BlockPos carAt = at(bx + 16, 1, zz(6));
        if (box.isInside(carAt) && seed % 3 != 0) {
            CarEntity car = new CarEntity(VehicleRegistry.CAR, server(level));
            float yaw = flip ? 0.0f : 180.0f;
            car.moveTo(carAt.getX() + 0.5, carAt.getY(), carAt.getZ() + 0.5, yaw, 0.0f);
            car.setYBodyRot(yaw);
            car.setYHeadRot(yaw);
            car.randomColor();
            car.setCity(city);
            level.addFreshEntity(car);
        }
        // the family lives here; the city brings them out when someone is around to see them
    }

    private void tree(WorldGenLevel level, BoundingBox box, int x, int z) {
        BlockState log = Blocks.OAK_LOG.defaultBlockState();
        BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++)
                for (int y = 3; y <= 5; y++) {
                    if (Math.abs(dx) + Math.abs(dz) + (y == 5 ? 2 : 0) > 3) continue;
                    hp(level, box, leaves, x + dx, y, z + dz);
                }
        for (int y = 1; y <= 4; y++) hp(level, box, log, x, y, z);
    }
}
