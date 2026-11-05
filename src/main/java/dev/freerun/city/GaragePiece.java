package dev.freerun.city;

import dev.freerun.economy.Shops;
import dev.freerun.garage.Alignment;
import dev.freerun.garage.CarPart;
import dev.freerun.garage.Garage;
import dev.freerun.garage.GarageRegistry;
import dev.freerun.garage.LiftEntity;
import dev.freerun.mobs.RoadhouseNpcEntity;
import dev.freerun.vehicle.CarEntity;
import dev.freerun.vehicle.VehicleRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Bayou Auto & Alignment, across the road from the factory: a three-bay shop with roll-up doors.
 *
 *   bay 1  a four-post lift with a customer's car up on it and the HawkSight aligner in front
 *   bay 2  a lift down at floor level with runways leading in from the door
 *   bay 3  the build bay: a chassis on stands with a few parts already on, parts racks, tool chests
 *   back   the parts counter (buy every car part, a wrench, a lift, the aligner) and the office
 *
 * Lot-local: x 0..37, z 0..37, street to the north (z = 0).
 */
public class GaragePiece extends CityPieceBase {
    private static final int X0 = 2, X1 = 35, Z0 = 4, Z1 = 33, ROOF = 8;

    public GaragePiece(BlockPos city, int lx, int lz, int w, int d) {
        super(CityRegistry.GARAGE_PIECE, city, lx, lz, w, 16, d);
    }

    public GaragePiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(CityRegistry.GARAGE_PIECE, tag);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        ground(level, box, Blocks.GRAY_CONCRETE.defaultBlockState(), 16);
        BlockState air = Blocks.AIR.defaultBlockState();
        // shell
        for (int x = X0; x <= X1; x++) for (int z = Z0; z <= Z1; z++) {
            boolean wall = x == X0 || x == X1 || z == Z0 || z == Z1;
            put(level, box, (x + z) % 7 == 0 ? Blocks.LIGHT_GRAY_CONCRETE : Blocks.SMOOTH_STONE, x, 0, z);
            for (int y = 1; y < ROOF; y++) {
                BlockState s = air;
                if (wall) s = (y <= 3 ? Blocks.BRICKS : y == 6 ? Blocks.GLASS : Blocks.LIGHT_GRAY_CONCRETE).defaultBlockState();
                put(level, box, s, x, y, z);
            }
            put(level, box, (!wall && x % 5 == 0 && z % 5 == 0 ? Blocks.SEA_LANTERN : Blocks.GRAY_CONCRETE).defaultBlockState(), x, ROOF, z);
        }
        // the front: a tall parapet with the name, three roll-up doors, a hazard stripe over each
        for (int x = X0; x <= X1; x++) for (int y = ROOF; y <= 14; y++) put(level, box, Blocks.WHITE_CONCRETE.defaultBlockState(), x, y, Z0);
        bigTextCentred(level, box, X0, X1, 14, Z0 - 1, Direction.NORTH, "AUTO", Blocks.RED_CONCRETE.defaultBlockState());
        sign(level, box, 18, 8, Z0 - 1, Direction.NORTH, "§lBAYOU AUTO", "& ALIGNMENT", "Repairs · Builds", "HawkSight 3D");
        int[] doors = {5, 16, 27};
        for (int dx : doors) {
            for (int x = dx; x <= dx + 5; x++) {
                for (int y = 1; y <= 5; y++) put(level, box, air, x, y, Z0);
                put(level, box, ((x & 1) == 0 ? Blocks.YELLOW_CONCRETE : Blocks.BLACK_CONCRETE).defaultBlockState(), x, 6, Z0);
                put(level, box, Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(BlockStateProperties.OPEN, true)
                        .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH)
                        .setValue(BlockStateProperties.HALF, net.minecraft.world.level.block.state.properties.Half.TOP), x, 7, Z0 + 1);   // the rolled-up door
            }
        }
        // bay dividers: a yellow line on the floor and a tool wall at the back
        for (int z = Z0 + 1; z < Z1 - 5; z++) {
            put(level, box, Blocks.YELLOW_CONCRETE, 14, 0, z);
            put(level, box, Blocks.YELLOW_CONCRETE, 25, 0, z);
        }
        // the apron out front
        for (int x = X0; x <= X1; x++) for (int z = 0; z < Z0; z++) put(level, box, Blocks.BLACK_CONCRETE, x, 0, z);

        bayOne(level, box);
        bayTwo(level, box);
        buildBay(level, box);
        back(level, box);
    }

    /** The lift with a customer's car up on it, and the aligner in front. */
    private void bayOne(WorldGenLevel level, BoundingBox box) {
        put(level, box, GarageRegistry.ALIGNER.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH), 8, 1, 11);
        sign(level, box, 9, 1, 11, Direction.NORTH, "HUNTERRA", "HawkSight", "Right-click", "to align");
        lift(level, box, 8.5, 19.5, true, LiftEntity.HIGH * 0.5);
    }

    private void bayTwo(WorldGenLevel level, BoundingBox box) {
        for (int z = Z0 + 1; z <= 14; z++) {
            put(level, box, GarageRegistry.RUNWAY, 18, 1, z);
            put(level, box, GarageRegistry.RUNWAY, 21, 1, z);
        }
        lift(level, box, 19.5, 19.5, true, LiftEntity.LOW);
    }

    /** A chassis on the floor with an engine and two wheels already on. Racks of parts and the tool chests. */
    private void buildBay(WorldGenLevel level, BoundingBox box) {
        BlockPos at = at(30, 1, 18);
        if (box.isInside(at)) {
            CarEntity car = Garage.chassis(server(level));
            car.setBuild(CarPart.ENGINE.bit() | CarPart.WHEEL_FL.bit() | CarPart.WHEEL_FR.bit());
            car.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 180.0f, 0.0f);
            car.setYBodyRot(180.0f);
            car.setColor(0x9EA3A8);
            level.addFreshEntity(car);
        }
        // parts racks: barrels of wheels, an engine stand, chests of parts
        chest(level, box, 34, 1, 10, Direction.WEST, new ItemStack(GarageRegistry.WHEEL, 4), new ItemStack(GarageRegistry.BATTERY),
                new ItemStack(GarageRegistry.FUEL_TANK));
        chest(level, box, 34, 1, 12, Direction.WEST, new ItemStack(GarageRegistry.TRANSMISSION), new ItemStack(GarageRegistry.STEERING),
                new ItemStack(GarageRegistry.SEATS));
        chest(level, box, 34, 1, 14, Direction.WEST, new ItemStack(GarageRegistry.BODY_PANELS), new ItemStack(GarageRegistry.WINDOWS));
        sign(level, box, 33, 2, 12, Direction.WEST, "", "PARTS", "for the build", "");
        // red tool chests along the back of the bay
        for (int x = 27; x <= 32; x += 2) {
            chest(level, box, x, 1, 27, Direction.NORTH, new ItemStack(GarageRegistry.WRENCH));
            put(level, box, Blocks.RED_CARPET, x, 2, 27);
        }
        put(level, box, Blocks.ANVIL, 26, 1, 24);
        put(level, box, Blocks.SMITHING_TABLE, 26, 1, 22);
        put(level, box, Blocks.GRINDSTONE.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE,
                net.minecraft.world.level.block.state.properties.AttachFace.FLOOR), 26, 1, 20);
        put(level, box, dev.freerun.factory.FactoryRegistry.WELDER.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.EAST), 26, 1, 16);
        sign(level, box, 30, 2, 26, Direction.NORTH, "", "TORQUE", "WRENCHES", "tune top speed");
    }

    /** Parts counter and the office. */
    private void back(WorldGenLevel level, BoundingBox box) {
        for (int x = X0 + 1; x < X1; x++) put(level, box, Blocks.BRICKS, x, 1, Z1 - 5);
        for (int x = X0 + 1; x < X1; x++) for (int y = 2; y <= 3; y++) put(level, box, Blocks.BRICKS, x, y, Z1 - 5);
        for (int x = 15; x <= 23; x++) {
            put(level, box, facing(dev.freerun.food.FoodRegistry.COUNTER, Direction.NORTH), x, 1, Z1 - 5);
            put(level, box, Blocks.AIR, x, 2, Z1 - 5);
            put(level, box, Blocks.AIR, x, 3, Z1 - 5);
        }
        put(level, box, Blocks.AIR, 24, 1, Z1 - 5);
        put(level, box, Blocks.AIR, 24, 2, Z1 - 5);
        for (int x = 15; x <= 23; x += 2) put(level, box, facing(dev.freerun.food.FoodRegistry.STORE_SHELF, Direction.NORTH), x, 1, Z1 - 1);
        sign(level, box, 19, 3, Z1 - 6, Direction.NORTH, "", "§lPARTS", "COUNTER", "");
        staff(level, box, 19, Z1 - 4, 180.0f, RoadhouseNpcEntity.Role.SHOPKEEPER, Shops.Shop.AUTO_PARTS, new ItemStack(GarageRegistry.WRENCH));
        // the office: desk, computer, a chair, a calendar of cars
        put(level, box, dev.freerun.life.LifeRegistry.OFFICE_DESK.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH), 6, 1, Z1 - 2);
        put(level, box, dev.freerun.life.LifeRegistry.COMPUTER.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH), 6, 2, Z1 - 2);
        put(level, box, dev.freerun.life.LifeRegistry.OFFICE_CHAIR.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH), 6, 1, Z1 - 3);
        put(level, box, dev.freerun.life.LifeRegistry.WATER_COOLER.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH), 10, 1, Z1 - 1);
    }

    private void lift(WorldGenLevel level, BoundingBox box, double lx, double lz, boolean withCar, double height) {
        BlockPos at = at((int) Math.floor(lx), 1, (int) Math.floor(lz));
        if (!box.isInside(at)) return;
        var sl = server(level);
        LiftEntity lift = GarageRegistry.LIFT.create(sl);
        if (lift == null) return;
        lift.moveTo(ox + lx, oy + 1, oz + lz, 0, 0);
        lift.setAlongX(false);
        lift.setHeight(height);
        level.addFreshEntity(lift);
        if (!withCar) return;
        CarEntity car = new CarEntity(VehicleRegistry.CAR, sl);
        car.moveTo(ox + lx, lift.deckTop() + 0.02, oz + lz, 180.0f, 0.0f);
        car.setYBodyRot(180.0f);
        car.setYHeadRot(180.0f);
        car.randomColor();
        car.setAlignment(Alignment.worn(sl.getRandom()));
        level.addFreshEntity(car);
    }

    private void chest(WorldGenLevel level, BoundingBox box, int x, int y, int z, Direction facing, ItemStack... items) {
        put(level, box, Blocks.CHEST.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing), x, y, z);
        BlockPos p = at(x, y, z);
        if (box.isInside(p) && level.getBlockEntity(p) instanceof Container c) {
            for (int i = 0; i < items.length && i < c.getContainerSize(); i++) c.setItem(i, items[i]);
        }
    }
}
