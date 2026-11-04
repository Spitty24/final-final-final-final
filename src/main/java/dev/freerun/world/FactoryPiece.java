package dev.freerun.world;

import dev.freerun.factory.CraneEntity;
import dev.freerun.factory.FactoryRegistry;
import dev.freerun.factory.ForkliftDriverEntity;
import dev.freerun.factory.ForkliftEntity;
import dev.freerun.factory.PartType;
import dev.freerun.factory.SteelPartEntity;
import dev.freerun.mobs.RoadhouseNpcEntity;
import dev.freerun.mobs.RoadhouseNpcEntity.Role;
import dev.freerun.mobs.SoldierRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * The welding factory: one big 41 x 31 shop floor under an 8-block-high roof. Front (z=0) to back:
 *
 *   x 0..16   PRESS DEPARTMENT: steel stock racks on the west wall, five press brakes with
 *             operators, pallets of sheets, hazard-striped floor around every machine
 *   x 17..23  central aisle (yellow lines) under the overhead gantry crane, big front door
 *   x 24..30  open work area: a public weld table + welder, anvil, grindstone, smithing table
 *   x 31..39  WELDING DEPARTMENT: four curtained booths, each with a weld table, welder machine,
 *             fume hood, quench bucket and a welder at work
 *   back      gear chest (gravity gun, torch, steel), a welded demo frame, break area
 *
 * Built in code with local coords (orientation SOUTH = local x/z map straight onto world x/z).
 * Entities only spawn when their spot is inside the chunk being generated, so each spawns once.
 */
public class FactoryPiece extends StructurePiece {
    static final int MAX_X = 40;
    static final int MAX_Z = 30;
    static final int ROOF = 9;
    private static final int[] PRESS_Z = {4, 9, 14, 19, 24};
    private static final int[] BOOTH_Z = {4, 10, 16, 22};
    private static final int CRANE_Z = 15;

    public FactoryPiece(BlockPos origin) {
        super(StructureRegistry.FACTORY_PIECE, 0, new BoundingBox(
                origin.getX(), origin.getY(), origin.getZ(),
                origin.getX() + MAX_X, origin.getY() + ROOF + 1, origin.getZ() + MAX_Z));
        setOrientation(Direction.SOUTH);
    }

    public FactoryPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(StructureRegistry.FACTORY_PIECE, tag);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        // bounding box + orientation are saved by the base class
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator chunkGenerator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        buildShell(level, box);
        buildCrane(level, box);
        buildPressDepartment(level, box);
        buildWorkArea(level, box);
        buildWeldingDepartment(level, box);
        buildBack(level, box);
        buildForklifts(level, box);
    }

    /**
     * Two forklifts on their rounds, each with a driver, plus a parked one by the door for you
     * (with a couple of sheets in front of it to practise on).
     */
    private void buildForklifts(WorldGenLevel level, BoundingBox box) {
        // Loop: down the press-department lane, across the back, up the main aisle, across the front.
        spawnForklift(level, box, 14.5, 4.5, List.of(new double[]{14.5, 24.0}, new double[]{21.0, 24.0},
                new double[]{21.0, 4.5}, new double[]{14.5, 4.5}), 0);
        // Back and forth along the welding-department lane.
        spawnForklift(level, box, 29.5, 4.0, List.of(new double[]{29.5, 27.0}, new double[]{29.5, 4.0}), 1);
        // Parked, free to drive.
        spawnForklift(level, box, 12.0, 2.5, List.of(), -1);
        spawnPart(level, box, 15.0, 1.0, 2.5, PartType.SHEET, 1);
        spawnPart(level, box, 15.0, 1.0625, 2.5, PartType.SHEET, 1);
    }

    private void spawnForklift(WorldGenLevel level, BoundingBox box, double lx, double lz, List<double[]> route,
                               int driverName) {
        double ox = getWorldX(0, 0);
        double oy = getWorldY(0) + 1.0;
        double oz = getWorldZ(0, 0);
        if (!box.isInside(BlockPos.containing(ox + lx, oy, oz + lz))) return;
        ForkliftEntity forklift = new ForkliftEntity(FactoryRegistry.FORKLIFT, level.getLevel());
        float yaw;
        if (route.isEmpty()) {
            yaw = Direction.EAST.toYRot();
        } else {
            double[] first = route.get(0);
            yaw = (float) (Mth.atan2(first[1] - lz, first[0] - lx) * Mth.RAD_TO_DEG) - 90.0f;
            List<Vec3> world = new ArrayList<>();
            for (double[] p : route) world.add(new Vec3(ox + p[0], oy, oz + p[1]));
            forklift.setRoute(world);
            forklift.setPallet(true);
            forklift.setLoaded(true);
        }
        forklift.moveTo(ox + lx, oy, oz + lz, yaw, 0.0f);
        forklift.setYBodyRot(yaw);
        forklift.setYHeadRot(yaw);
        if (driverName >= 0) {
            ForkliftDriverEntity driver = driverName == 1
                    ? new dev.freerun.mobs.JohnnySimEntity(dev.freerun.mobs.MoreMobs.JOHNNY_SIM, level.getLevel())
                    : new ForkliftDriverEntity(FactoryRegistry.FORKLIFT_DRIVER, level.getLevel());
            driver.setNameIndex(driverName);
            driver.moveTo(ox + lx, oy, oz + lz, yaw, 0.0f);
            driver.startRiding(forklift, true);
        }
        level.addFreshEntityWithPassengers(forklift);
    }

    // ------------------------------------------------------------------ shell

    private void buildShell(WorldGenLevel level, BoundingBox box) {
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState floor = Blocks.SMOOTH_STONE.defaultBlockState();
        BlockState line = Blocks.YELLOW_CONCRETE.defaultBlockState();
        BlockState lower = Blocks.GRAY_CONCRETE.defaultBlockState();
        BlockState upper = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
        BlockState column = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        BlockState glass = Blocks.GLASS.defaultBlockState();
        BlockState roof = Blocks.GRAY_CONCRETE.defaultBlockState();
        BlockState light = Blocks.SEA_LANTERN.defaultBlockState();
        BlockState parapet = Blocks.SMOOTH_STONE_SLAB.defaultBlockState();

        for (int x = 0; x <= MAX_X; x++) {
            for (int z = 0; z <= MAX_Z; z++) {
                boolean edge = x == 0 || x == MAX_X || z == 0 || z == MAX_Z;
                boolean aisleLine = !edge && (x == 17 || x == 23);
                placeBlock(level, aisleLine ? line : floor, x, 0, z, box);

                if (edge) {
                    boolean isColumn = (x == 0 || x == MAX_X) ? z % 5 == 0 : x % 5 == 0;
                    for (int y = 1; y < ROOF; y++) {
                        BlockState w;
                        if (isColumn) w = column;
                        else if (y == 6 || y == 7) w = glass;            // high clerestory windows
                        else w = y <= 4 ? lower : upper;
                        placeBlock(level, w, x, y, z, box);
                    }
                    placeBlock(level, roof, x, ROOF, z, box);
                    placeBlock(level, parapet, x, ROOF + 1, z, box);
                } else {
                    for (int y = 1; y < ROOF; y++) placeBlock(level, air, x, y, z, box);
                    BlockState r = roof;
                    if (z % 6 == 3 && x >= 2 && x <= MAX_X - 2) r = glass;   // skylight strips
                    else if (x % 6 == 2 && z % 6 == 0) r = light;           // roof lights
                    placeBlock(level, r, x, ROOF, z, box);
                }
            }
        }

        // Big roll-up door in the front wall, with hazard stripes above it.
        for (int x = 18; x <= 22; x++) {
            for (int y = 1; y <= 5; y++) placeBlock(level, air, x, y, 0, box);
        }
        for (int x = 17; x <= 23; x++) {
            placeBlock(level, (x % 2 == 0 ? Blocks.YELLOW_CONCRETE : Blocks.BLACK_CONCRETE).defaultBlockState(),
                    x, 6, 0, box);
        }
        // Lights either side of the door and department banners inside.
        placeBlock(level, Blocks.LANTERN.defaultBlockState(), 16, 1, 1, box);
        placeBlock(level, Blocks.LANTERN.defaultBlockState(), 24, 1, 1, box);
        placeBlock(level, Blocks.BLUE_WALL_BANNER.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH), 8, 5, 1, box);
        placeBlock(level, Blocks.ORANGE_WALL_BANNER.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH), 34, 5, 1, box);
    }

    /**
     * Overhead bridge crane: runway rails along both side walls, and the crane itself (bridge,
     * hoist and hook) as a live entity you drive with the Crane Pendant. It covers the whole floor.
     */
    private void buildCrane(WorldGenLevel level, BoundingBox box) {
        BlockState rail = Blocks.POLISHED_DEEPSLATE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
        for (int z = 1; z < MAX_Z; z++) {
            placeBlock(level, rail, 1, 7, z, box);
            placeBlock(level, rail, MAX_X - 1, 7, z, box);
        }
        BlockPos at = getWorldPos(20, 8, CRANE_Z).immutable();
        if (box.isInside(at)) {
            double ox = getWorldX(0, 0);
            double oz = getWorldZ(0, 0);
            CraneEntity crane = new CraneEntity(FactoryRegistry.CRANE, level.getLevel());
            crane.setBounds(ox + 2.5, ox + MAX_X - 1.5, oz + 2.0, oz + MAX_Z - 1.0, ox + 1.0, ox + MAX_X);
            crane.moveTo(at.getX() + 0.5, getWorldY(0) + 8.0, at.getZ() + 0.5, 0.0f, 0.0f);
            level.addFreshEntity(crane);
        }
    }

    // ------------------------------------------------------------------ press department

    private void buildPressDepartment(WorldGenLevel level, BoundingBox box) {
        // Steel stock racks along the west wall: two rows of barrels, the lower ones full of sheets.
        BlockState barrel = Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.EAST);
        for (int z = 2; z <= MAX_Z - 2; z++) {
            placeBlock(level, barrel, 1, 1, z, box);
            placeBlock(level, barrel, 1, 2, z, box);
            if (z % 3 == 0) fill(level, box, 1, 1, z, new ItemStack(FactoryRegistry.STEEL_SHEET, 12));
            if (z % 6 == 0) fill(level, box, 1, 2, z, new ItemStack(Items.IRON_INGOT, 6));
        }

        BlockState press = FactoryRegistry.PRESS_BRAKE.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST);
        BlockState pump = Blocks.BLAST_FURNACE.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST);
        BlockState pallet = Blocks.SPRUCE_SLAB.defaultBlockState();
        for (int z0 : PRESS_Z) {
            // Hazard-striped floor around the machine
            for (int x = 5; x <= 8; x++) {
                for (int z = z0 - 1; z <= z0 + 1; z++) {
                    placeBlock(level, ((x + z) % 2 == 0 ? Blocks.YELLOW_CONCRETE : Blocks.BLACK_CONCRETE)
                            .defaultBlockState(), x, 0, z, box);
                }
            }
            placeBlock(level, press, 6, 1, z0, box);
            // The rest of the 3-wide, 3-tall frame.
            BlockPos main = new BlockPos(6, 1, z0);
            for (BlockPos p : dev.freerun.factory.PressBrakeBlock.framePositions(main, Direction.EAST)) {
                placeBlock(level, dev.freerun.factory.PressBrakeBlock.frameState(Direction.EAST, main, p), p.getX(), p.getY(), p.getZ(), box);
            }
            placeBlock(level, pump, 5, 1, z0, box);        // hydraulic power pack behind the press
            placeBlock(level, pallet, 3, 1, z0, box);      // pallet of fresh sheets
            for (int i = 0; i < 3; i++) {
                spawnPart(level, box, 3.5, 1.5 + i * 0.07, z0 + 0.5, PartType.SHEET, 0);
            }
            spawnWorker(level, box, 7, z0, Direction.WEST, Role.PRESS_OPERATOR,
                    new ItemStack(FactoryRegistry.STEEL_SHEET));
        }
        buildConveyor(level, box);
    }

    /**
     * The finished-parts line: a belt runs behind the press operators (who drop each part they
     * form onto it), turns at the back wall, and feeds a bin barrel by the aisle.
     */
    private void buildConveyor(WorldGenLevel level, BoundingBox box) {
        for (int z = 2; z <= 26; z++) {
            placeBlock(level, FactoryRegistry.CONVEYOR.defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH), 9, 1, z, box);
        }
        for (int x = 9; x <= 15; x++) {
            placeBlock(level, FactoryRegistry.CONVEYOR.defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST), x, 1, 27, box);
        }
        placeBlock(level, Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP),
                16, 1, 27, box);
        fill(level, box, 16, 1, 27, new ItemStack(FactoryRegistry.STEEL_BRACKET, 6),
                new ItemStack(FactoryRegistry.STEEL_CHANNEL, 6), new ItemStack(FactoryRegistry.STEEL_PLATE, 8));
        placeBlock(level, Blocks.YELLOW_CONCRETE.defaultBlockState(), 16, 0, 26, box);
        placeBlock(level, Blocks.YELLOW_CONCRETE.defaultBlockState(), 16, 0, 28, box);
        // A couple of parts already riding the belt
        spawnPart(level, box, 9.5, 1.25, 8.5, PartType.BRACKET, 0);
        spawnPart(level, box, 9.5, 1.25, 17.5, PartType.CHANNEL, 0);
    }

    // ------------------------------------------------------------------ open work area

    private void buildWorkArea(WorldGenLevel level, BoundingBox box) {
        placeBlock(level, Blocks.ANVIL.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST), 25, 1, 3, box);
        placeBlock(level, Blocks.GRINDSTONE.defaultBlockState()
                .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST), 25, 1, 5, box);
        placeBlock(level, Blocks.SMITHING_TABLE.defaultBlockState(), 25, 1, 7, box);
        placeBlock(level, Blocks.CRAFTING_TABLE.defaultBlockState(), 25, 1, 9, box);
        placeBlock(level, Blocks.STONECUTTER.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST), 25, 1, 11, box);

        // A public weld table: two iron blocks and a welder machine. Bring your own parts.
        placeBlock(level, FactoryRegistry.WELDER.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST), 25, 1, 20, box);
        placeBlock(level, Blocks.IRON_BLOCK.defaultBlockState(), 27, 1, 20, box);
        placeBlock(level, Blocks.IRON_BLOCK.defaultBlockState(), 27, 1, 21, box);
        placeBlock(level, Blocks.IRON_BLOCK.defaultBlockState(), 27, 1, 22, box);
        spawnPart(level, box, 27.5, 2.0, 20.5, PartType.SHEET, 0);
        spawnPart(level, box, 27.5, 2.0, 21.5, PartType.SHEET, 0);
        spawnPart(level, box, 27.5, 2.0, 22.5, PartType.BRACKET, 0);
    }

    // ------------------------------------------------------------------ welding department

    private void buildWeldingDepartment(WorldGenLevel level, BoundingBox box) {
        BlockState curtain = Blocks.RED_STAINED_GLASS_PANE.defaultBlockState()
                .setValue(BlockStateProperties.EAST, true).setValue(BlockStateProperties.WEST, true);
        BlockState curtainEnd = curtain.setValue(BlockStateProperties.WEST, false);
        BlockState boothFloor = Blocks.POLISHED_ANDESITE.defaultBlockState();
        PartType[] onTable = {PartType.BRACKET, PartType.CHANNEL, PartType.SHEET, PartType.PLATE};

        for (int i = 0; i < BOOTH_Z.length; i++) {
            int zc = BOOTH_Z[i];
            for (int x = 31; x < MAX_X; x++) {
                for (int z = zc - 1; z <= zc + 2; z++) placeBlock(level, boothFloor, x, 0, z, box);
                for (int y = 1; y <= 3; y++) {
                    placeBlock(level, x == 31 ? curtainEnd : curtain, x, y, zc - 2, box);
                    placeBlock(level, x == 31 ? curtainEnd : curtain, x, y, zc + 3, box);
                }
            }
            // Weld table, the machine, a fume hood over the table, a quench bucket in the corner
            placeBlock(level, Blocks.IRON_BLOCK.defaultBlockState(), 35, 1, zc, box);
            placeBlock(level, Blocks.IRON_BLOCK.defaultBlockState(), 35, 1, zc + 1, box);
            placeBlock(level, FactoryRegistry.WELDER.defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST), 38, 1, zc, box);
            placeBlock(level, Blocks.HOPPER.defaultBlockState(), 35, 4, zc, box);
            for (int y = 5; y < ROOF; y++) placeBlock(level, Blocks.CHAIN.defaultBlockState(), 35, y, zc, box);
            placeBlock(level, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3),
                    39, 1, zc + 2, box);

            spawnPart(level, box, 35.5, 2.0, zc + 1.0, onTable[i], 0);
            spawnWorker(level, box, 34, zc, Direction.EAST, Role.WELDER,
                    new ItemStack(FactoryRegistry.WELDING_TORCH));
        }
    }

    // ------------------------------------------------------------------ back of the shop

    private void buildBack(WorldGenLevel level, BoundingBox box) {
        // The gear chest: everything you need to try the place out.
        placeBlock(level, Blocks.CHEST.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH),
                20, 1, 28, box);
        fill(level, box, 20, 1, 28,
                new ItemStack(FactoryRegistry.GRAVITY_GUN),
                new ItemStack(FactoryRegistry.WELDING_TORCH),
                new ItemStack(FactoryRegistry.STEEL_SHEET, 32),
                new ItemStack(FactoryRegistry.STEEL_PLATE, 8),
                new ItemStack(FactoryRegistry.STEEL_BRACKET, 8),
                new ItemStack(FactoryRegistry.STEEL_CHANNEL, 8),
                new ItemStack(FactoryRegistry.CRANE_PENDANT),
                new ItemStack(FactoryRegistry.CONVEYOR, 16),
                new ItemStack(FactoryRegistry.FORKLIFT_ITEM));
        placeBlock(level, Blocks.YELLOW_CONCRETE.defaultBlockState(), 19, 0, 28, box);
        placeBlock(level, Blocks.YELLOW_CONCRETE.defaultBlockState(), 21, 0, 28, box);

        // A welded demo beam on the aisle: two sheets with a bracket fin standing along the middle.
        BlockPos demo = getWorldPos(27, 1, 26).immutable();
        if (box.isInside(demo)) {
            SteelPartEntity frame = SteelPartEntity.single(level.getLevel(), PartType.SHEET, 0, 0, 0, 0);
            frame.setParts(List.of(
                    new SteelPartEntity.Part(PartType.SHEET, 0, -0.5, 0.0, 0.0),
                    new SteelPartEntity.Part(PartType.SHEET, 0, 0.5, 0.0, 0.0),
                    new SteelPartEntity.Part(PartType.BRACKET, 0, -0.5, 0.0625, 0.25),
                    new SteelPartEntity.Part(PartType.BRACKET, 2, 0.5, 0.0625, -0.25)));
            frame.moveTo(demo.getX() + 0.5, demo.getY(), demo.getZ() + 0.5, 0.0f, 0.0f);
            level.addFreshEntity(frame);
        }

        // Break area: a table, two chairs, a water cooler.
        placeBlock(level, BlockRegistry.TABLE.defaultBlockState(), 11, 1, 28, box);
        placeBlock(level, BlockRegistry.CHAIR.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST), 10, 1, 28, box);
        placeBlock(level, BlockRegistry.CHAIR.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST), 12, 1, 28, box);
        placeBlock(level, Blocks.WHITE_CONCRETE.defaultBlockState(), 14, 1, 29, box);
        placeBlock(level, Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState(), 14, 2, 29, box);
        // Lunch counter: a sausage stuffer and a barrel of pork for making boudin
        placeBlock(level, dev.freerun.food.FoodRegistry.SAUSAGE_STUFFER.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH), 9, 1, 29, box);
        placeBlock(level, Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP), 8, 1, 29, box);
        fill(level, box, 8, 1, 29, new ItemStack(Items.PORKCHOP, 16), new ItemStack(dev.freerun.food.FoodRegistry.BOUDIN, 4));

        // Three co-op interns, wandering the floor with even more questions
        for (int i = 0; i < 3; i++) {
            BlockPos at = getWorldPos(18 + i * 2, 1, 8 + i * 3).immutable();
            if (!box.isInside(at)) continue;
            dev.freerun.mobs.CoopEntity coop = new dev.freerun.mobs.CoopEntity(dev.freerun.mobs.MoreMobs.COOP, level.getLevel());
            coop.setVariant(i);
            coop.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0f, 0.0f);
            coop.setHome(getWorldPos(20, 1, 15).immutable(), 14);
            level.addFreshEntityWithPassengers(coop);
        }

        // Ethan James Patrick, wandering the floor with questions
        BlockPos ethanAt = getWorldPos(20, 1, 12).immutable();
        if (box.isInside(ethanAt)) {
            dev.freerun.mobs.EthanEntity ethan = new dev.freerun.mobs.EthanEntity(dev.freerun.mobs.MoreMobs.ETHAN, level.getLevel());
            ethan.moveTo(ethanAt.getX() + 0.5, ethanAt.getY(), ethanAt.getZ() + 0.5, 0.0f, 0.0f);
            ethan.setHome(getWorldPos(20, 1, 15).immutable(), 13);
            level.addFreshEntityWithPassengers(ethan);
        }
    }

    // ------------------------------------------------------------------ helpers

    private void fill(WorldGenLevel level, BoundingBox box, int x, int y, int z, ItemStack... stacks) {
        BlockPos pos = getWorldPos(x, y, z).immutable();
        if (!box.isInside(pos)) return;
        if (level.getBlockEntity(pos) instanceof Container c) {
            for (int i = 0; i < stacks.length && i < c.getContainerSize(); i++) c.setItem(i, stacks[i]);
        }
    }

    private void spawnPart(WorldGenLevel level, BoundingBox box, double lx, double ly, double lz,
                           PartType type, int rot) {
        double wx = getWorldX(0, 0) + lx;
        double wy = getWorldY(0) + ly;
        double wz = getWorldZ(0, 0) + lz;
        if (!box.isInside(BlockPos.containing(wx, wy, wz))) return;
        level.addFreshEntity(SteelPartEntity.single(level.getLevel(), type, wx, wy, wz, rot));
    }

    private void spawnWorker(WorldGenLevel level, BoundingBox box, int x, int z, Direction facing,
                             Role role, ItemStack held) {
        BlockPos pos = getWorldPos(x, 1, z).immutable();
        if (!box.isInside(pos)) return;
        RoadhouseNpcEntity npc = new RoadhouseNpcEntity(SoldierRegistry.ROADHOUSE_NPC, level.getLevel());
        npc.setRole(role, 0);
        npc.setItemSlot(EquipmentSlot.MAINHAND, held);
        npc.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
        float yaw = facing.toYRot();
        npc.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0.0f);
        npc.setYHeadRot(yaw);
        npc.setYBodyRot(yaw);
        level.addFreshEntityWithPassengers(npc);
    }
}
