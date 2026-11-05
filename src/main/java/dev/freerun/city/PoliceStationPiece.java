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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * A police precinct: a two-storey stone building with a front desk, a squad room, a holding
 * cell and a break room, a flagpole out front, and a lot of cruisers round the back. Two
 * officers work out of each one.
 */
public class PoliceStationPiece extends CityPieceBase {
    public PoliceStationPiece(BlockPos city, int lx, int lz, int w, int d) {
        super(CityRegistry.POLICE_PIECE, city, lx, lz, w, 16, d);
    }

    public PoliceStationPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(CityRegistry.POLICE_PIECE, tag);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        ground(level, box, Blocks.GRASS_BLOCK.defaultBlockState(), 16);
        BlockState wall = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState band = Blocks.BLUE_CONCRETE.defaultBlockState();
        fill(level, box, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), 4, 0, 1, 33, 0, 6);
        int x0 = 4, x1 = 33, z0 = 7, z1 = 22;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
                put(level, box, Blocks.POLISHED_ANDESITE, x, 0, z);
                for (int y = 1; y <= 9; y++) {
                    BlockState s = Blocks.AIR.defaultBlockState();
                    if (edge) {
                        boolean window = (y == 2 || y == 3 || y == 7 || y == 8) && ((z == z0 || z == z1) ? (x - x0) % 4 == 2 : (z - z0) % 4 == 2);
                        s = y == 5 ? band : window ? Blocks.GLASS.defaultBlockState() : wall;
                    }
                    put(level, box, s, x, y, z);
                }
                put(level, box, Blocks.SMOOTH_STONE, x, 5, z);   // upstairs floor
                put(level, box, Blocks.SMOOTH_STONE, x, 10, z);
            }
        }
        // the band shows outside, the floor inside
        for (int x = x0; x <= x1; x++) {
            put(level, box, band, x, 5, z0);
            put(level, box, band, x, 5, z1);
        }
        for (int z = z0; z <= z1; z++) {
            put(level, box, band, x0, 5, z);
            put(level, box, band, x1, 5, z);
        }
        // entrance with steps and lamps
        fill(level, box, Blocks.AIR.defaultBlockState(), 18, 1, z0, 19, 3, z0);
        put(level, box, Blocks.LANTERN, 16, 3, z0 - 1);
        put(level, box, Blocks.LANTERN, 21, 3, z0 - 1);
        put(level, box, Blocks.STONE_BRICK_WALL, 16, 2, z0 - 1);
        put(level, box, Blocks.STONE_BRICK_WALL, 21, 2, z0 - 1);
        put(level, box, Blocks.STONE_BRICK_WALL, 16, 1, z0 - 1);
        put(level, box, Blocks.STONE_BRICK_WALL, 21, 1, z0 - 1);
        bigTextCentred(level, box, x0, x1, 15, z0, Direction.NORTH, "POLICE", Blocks.BLUE_CONCRETE.defaultBlockState());
        fill(level, box, wall, x0 + 3, 11, z0, x1 - 3, 15, z0);
        bigTextCentred(level, box, x0, x1, 15, z0 - 1, Direction.NORTH, "POLICE", Blocks.WHITE_CONCRETE.defaultBlockState());
        sign(level, box, 20, 4, z0 - 1, Direction.NORTH, "", "§lCITY POLICE", "Precinct", "Open 24 hours");
        // flagpole
        fill(level, box, Blocks.IRON_BARS.defaultBlockState(), 7, 1, 3, 7, 12, 3);
        put(level, box, Blocks.BLUE_WOOL, 8, 12, 3);
        put(level, box, Blocks.WHITE_WOOL, 9, 12, 3);
        put(level, box, Blocks.RED_WOOL, 8, 11, 3);
        put(level, box, Blocks.WHITE_WOOL, 9, 11, 3);

        // ---- ground floor: front desk, waiting area, holding cell
        for (int x = 15; x <= 22; x++) put(level, box, facing(FoodRegistry.COUNTER, Direction.NORTH), x, 1, z0 + 4);
        put(level, box, facing(EconomyRegistry.TV, Direction.NORTH).setValue(dev.freerun.economy.StoreBlocks.Tv.ON, true), 20, 2, z0 + 4);
        for (int x = x0 + 2; x <= x0 + 6; x++) put(level, box, facing(BlockRegistry.CHAIR, Direction.EAST), x, 1, z0 + 2);
        BlockState bars = Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true);
        for (int z = z0 + 8; z < z1; z++) for (int y = 1; y <= 4; y++) put(level, box, bars, x1 - 7, y, z);
        put(level, box, Blocks.WHITE_BED.defaultBlockState().setValue(net.minecraft.world.level.block.BedBlock.FACING, Direction.SOUTH)
                .setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.FOOT), x1 - 3, 1, z1 - 3);
        put(level, box, Blocks.WHITE_BED.defaultBlockState().setValue(net.minecraft.world.level.block.BedBlock.FACING, Direction.SOUTH)
                .setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.HEAD), x1 - 3, 1, z1 - 2);
        put(level, box, Blocks.CAULDRON, x1 - 2, 1, z0 + 9);
        sign(level, box, x1 - 7, 3, z0 + 7, Direction.NORTH, "", "HOLDING", "", "");
        // squad room: desks with monitors
        for (int x = x0 + 2; x <= x0 + 14; x += 4) {
            for (int z = z0 + 8; z <= z1 - 3; z += 4) {
                put(level, box, BlockRegistry.TABLE, x, 1, z);
                put(level, box, facing(EconomyRegistry.TV, Direction.SOUTH).setValue(dev.freerun.economy.StoreBlocks.Tv.ON, (x + z) % 3 != 0), x, 2, z);
                put(level, box, facing(BlockRegistry.CHAIR, Direction.NORTH), x, 1, z + 1);
            }
        }
        // coffee, the most important equipment in the building
        put(level, box, facing(FoodRegistry.COUNTER, Direction.SOUTH), x0 + 1, 1, z1 - 1);
        put(level, box, facing(FoodRegistry.COFFEE_MAKER, Direction.SOUTH), x0 + 1, 2, z1 - 1);
        put(level, box, facing(FoodRegistry.COUNTER, Direction.SOUTH), x0 + 2, 1, z1 - 1);
        put(level, box, facing(FoodRegistry.MICROWAVE, Direction.SOUTH), x0 + 2, 2, z1 - 1);
        for (int[] l : new int[][]{{10, 12}, {26, 12}, {10, 18}, {26, 18}}) {
            put(level, box, Blocks.SEA_LANTERN, l[0], 4, l[1]);
            put(level, box, Blocks.SEA_LANTERN, l[0], 9, l[1]);
        }
        // stairs up (ladder) and an office upstairs
        for (int y = 1; y <= 5; y++) put(level, box, Blocks.LADDER.defaultBlockState()
                .setValue(net.minecraft.world.level.block.LadderBlock.FACING, Direction.NORTH), x0 + 16, y, z1 - 1);
        put(level, box, Blocks.AIR, x0 + 16, 5, z1 - 1);
        for (int x = x0 + 3; x <= x1 - 3; x += 6) {
            put(level, box, BlockRegistry.TABLE, x, 6, z0 + 5);
            put(level, box, facing(BlockRegistry.CHAIR, Direction.NORTH), x, 6, z0 + 6);
            put(level, box, Blocks.BOOKSHELF, x + 2, 6, z0 + 1);
            put(level, box, Blocks.BOOKSHELF, x + 2, 7, z0 + 1);
        }
        // officers on duty
        officer(level, box, 18, z0 + 5, 180.0f, 1);
        officer(level, box, x0 + 8, z0 + 12, 90.0f, 2);

        // ---- the lot out back, with cruisers
        LotPiece.parking(level, box, this, 2, 25, 34, 12, 99);
        cruiser(level, box, 8.5, 27.5, 180.0f);
        cruiser(level, box, 14.5, 27.5, 180.0f);
        cruiser(level, box, 26.5, 27.5, 180.0f);
        fill(level, box, Blocks.BLACK_CONCRETE.defaultBlockState(), 34, 0, 8, 37, 0, 37);   // driveway round the side
    }

    private void officer(WorldGenLevel level, BoundingBox box, int x, int z, float yaw, int seed) {
        BlockPos p = at(x, 1, z);
        if (!box.isInside(p)) return;
        PoliceOfficerEntity cop = new PoliceOfficerEntity(CityRegistry.POLICE_OFFICER, server(level));
        cop.setup(Math.floorMod(ox * 31 + oz * 17 + seed, 1000), p);
        cop.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, yaw, 0.0f);
        cop.setYHeadRot(yaw);
        cop.setYBodyRot(yaw);
        level.addFreshEntity(cop);
    }

    private void cruiser(WorldGenLevel level, BoundingBox box, double x, double z, float yaw) {
        BlockPos p = at((int) Math.floor(x), 1, (int) Math.floor(z));
        if (!box.isInside(p)) return;
        CarEntity car = new CarEntity(VehicleRegistry.CAR, server(level));
        car.moveTo(ox + x, oy + 1, oz + z, yaw, 0.0f);
        car.setYBodyRot(yaw);
        car.setYHeadRot(yaw);
        car.setPolice(true);
        car.addFuel(100.0f);
        car.setCity(city);
        level.addFreshEntity(car);
    }
}
