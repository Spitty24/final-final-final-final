package dev.freerun.world;

import dev.freerun.factory.FactoryRegistry;
import dev.freerun.factory.PartType;
import dev.freerun.factory.SteelPartEntity;
import dev.freerun.laser.LaserRegistry;
import dev.freerun.laser.TarynEntity;
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
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * The laser bay, built onto the factory's east side (through a door at the back of the welding
 * department). A flatbed fiber laser in its enclosure, with orange laser-safety viewing windows, a
 * 3 x 3 cutting bed and the gantry over it; the CNC terminal at the loading side; a shuttle table
 * with sheets waiting (the pallet changer); the laser source cabinet and its chiller; racks of
 * oxygen and nitrogen bottles; a sheet rack, a scrap bin for skeletons, a parts table; and Taryn.
 *
 * Coordinates are the factory's: x 40 is the factory's east wall, the bay runs to x 58.
 */
public class LaserBayPiece extends StructurePiece {
    private static final int X0 = 40, X1 = 58, Z0 = 2, Z1 = 30, ROOF = FactoryPiece.ROOF;
    private static final int CX = 50, CZ = 12;   // the laser cutter (middle of the bed)

    public LaserBayPiece(BlockPos factoryOrigin) {
        super(StructureRegistry.LASER_BAY_PIECE, 0, new BoundingBox(
                factoryOrigin.getX() + X0, factoryOrigin.getY(), factoryOrigin.getZ() + Z0,
                factoryOrigin.getX() + X1, factoryOrigin.getY() + ROOF + 1, factoryOrigin.getZ() + Z1));
        setOrientation(Direction.SOUTH);
    }

    public LaserBayPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(StructureRegistry.LASER_BAY_PIECE, tag);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
    }

    // local helpers relative to the factory origin: the piece's own box starts at X0/Z0
    private void put(WorldGenLevel level, BoundingBox box, BlockState s, int x, int y, int z) {
        placeBlock(level, s, x - X0, y, z - Z0, box);
    }

    private BlockPos pos(int x, int y, int z) {
        return getWorldPos(x - X0, y, z - Z0).immutable();
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        BlockState air = Blocks.AIR.defaultBlockState();
        // shell: floor, walls, roof (the west wall is the factory's)
        for (int x = X0; x <= X1; x++) for (int z = Z0; z <= Z1; z++) {
            boolean wall = x == X1 || z == Z0 || z == Z1;
            put(level, box, (x + z) % 2 == 0 ? Blocks.POLISHED_ANDESITE.defaultBlockState() : Blocks.SMOOTH_STONE.defaultBlockState(), x, 0, z);
            for (int y = -3; y < 0; y++) put(level, box, Blocks.STONE.defaultBlockState(), x, y, z);
            if (x == X0) continue;
            for (int y = 1; y < ROOF; y++) {
                BlockState s = air;
                if (wall) {
                    boolean column = x == X1 ? z % 5 == 0 : x % 5 == 0;
                    s = column ? Blocks.POLISHED_DEEPSLATE.defaultBlockState()
                            : y == 6 || y == 7 ? Blocks.GLASS.defaultBlockState()
                            : (y <= 4 ? Blocks.GRAY_CONCRETE : Blocks.LIGHT_GRAY_CONCRETE).defaultBlockState();
                }
                put(level, box, s, x, y, z);
            }
            put(level, box, (!wall && x % 5 == 2 && z % 5 == 2 ? Blocks.SEA_LANTERN : Blocks.GRAY_CONCRETE).defaultBlockState(), x, ROOF, z);
            if (wall) put(level, box, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), x, ROOF + 1, z);
        }
        // the door through from the welding department
        for (int z = 27; z <= 28; z++) for (int y = 1; y <= 3; y++) put(level, box, air, X0, y, z);
        for (int z = 26; z <= 29; z++) put(level, box, Blocks.YELLOW_CONCRETE.defaultBlockState(), X0, 4, z);
        // a walkway (yellow lines) from the door to the machine
        for (int x = X0 + 1; x <= 46; x++) {
            put(level, box, Blocks.YELLOW_CONCRETE.defaultBlockState(), x, 0, 26);
            put(level, box, Blocks.YELLOW_CONCRETE.defaultBlockState(), x, 0, 29);
        }

        buildMachine(level, box);
        buildSupport(level, box);
        spawn(level, box);
    }

    private void buildMachine(WorldGenLevel level, BoundingBox box) {
        BlockState frame = Blocks.WHITE_CONCRETE.defaultBlockState();
        BlockState accent = Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState();
        BlockState window = Blocks.ORANGE_STAINED_GLASS.defaultBlockState();   // laser-safety viewing glass
        // hazard floor round the loading side
        for (int z = CZ - 3; z <= CZ + 3; z++) for (int x = CX - 6; x <= CX - 4; x++) {
            put(level, box, ((x + z) % 2 == 0 ? Blocks.YELLOW_CONCRETE : Blocks.BLACK_CONCRETE).defaultBlockState(), x, 0, z);
        }
        // the cutting bed: the cutter in the middle, slatted table round it
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            BlockState s = dx == 0 && dz == 0
                    ? LaserRegistry.LASER_CUTTER.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.WEST)
                    : LaserRegistry.LASER_TABLE.defaultBlockState();
            put(level, box, s, CX + dx, 1, CZ + dz);
        }
        // the enclosure: frame corners, windows on three sides, a roof; open (light curtain) on the loading side
        int ex0 = CX - 3, ex1 = CX + 3, ez0 = CZ - 3, ez1 = CZ + 3;
        for (int x = ex0; x <= ex1; x++) for (int z = ez0; z <= ez1; z++) {
            boolean edge = x == ex0 || x == ex1 || z == ez0 || z == ez1;
            if (edge) {
                boolean corner = (x == ex0 || x == ex1) && (z == ez0 || z == ez1);
                for (int y = 1; y <= 3; y++) {
                    if (x == ex0 && !corner) continue;   // loading side
                    put(level, box, corner ? frame : y == 1 ? accent : window, x, y, z);
                }
            }
            put(level, box, x == ex0 || x == ex1 || z == ez0 || z == ez1 ? frame : Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), x, 4, z);
        }
        put(level, box, Blocks.RED_CONCRETE.defaultBlockState(), ex0, 4, CZ);   // the stack light
        put(level, box, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, true), ex0, 5, CZ);
        // the shuttle table outside the back: the pallet changer, with the next sheets ready
        for (int dz = -1; dz <= 1; dz++) for (int x = ex1 + 1; x <= ex1 + 3; x++) {
            put(level, box, LaserRegistry.LASER_TABLE.defaultBlockState(), x, 1, CZ + dz);
        }
        // the CNC terminal at the loading side, facing the operator
        put(level, box, LaserRegistry.CNC_TERMINAL.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.WEST),
                ex0 - 1, 1, CZ - 3);
        // signs
        sign(level, box, ex0 - 1, 2, CZ + 3, Direction.WEST, "§lDANGER", "CLASS 4", "LASER", "Glasses on");
        sign(level, box, X0 + 1, 3, 25, Direction.EAST, "", "§lLASER BAY", "TruCut 3030", "fiber");
    }

    private void buildSupport(WorldGenLevel level, BoundingBox box) {
        // the laser source cabinet and its chiller, against the east wall
        for (int z = 16; z <= 18; z++) for (int y = 1; y <= 3; y++) {
            put(level, box, (y == 3 ? Blocks.WHITE_CONCRETE : Blocks.LIGHT_GRAY_CONCRETE).defaultBlockState(), X1 - 2, y, z);
            put(level, box, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), X1 - 1, y, z);
        }
        put(level, box, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, true), X1 - 2, 2, 17);
        sign(level, box, X1 - 3, 2, 16, Direction.WEST, "LASER SOURCE", "12 kW fiber", "", "Do not open");
        for (int z = 20; z <= 21; z++) for (int y = 1; y <= 2; y++) put(level, box, Blocks.BLUE_CONCRETE.defaultBlockState(), X1 - 1, y, z);
        put(level, box, Blocks.IRON_TRAPDOOR.defaultBlockState(), X1 - 1, 3, 20);
        sign(level, box, X1 - 2, 2, 20, Direction.WEST, "CHILLER", "keeps the", "source at", "22 C");
        // cable tray from the source to the machine
        for (int x = CX + 1; x <= X1 - 2; x++) put(level, box, Blocks.CHAIN.defaultBlockState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.X), x, 5, 17);
        // gas bottles: oxygen (green) and nitrogen (black), chained to the wall
        for (int i = 0; i < 4; i++) {
            int z = 4 + i;
            BlockState bottle = (i < 2 ? Blocks.GREEN_CONCRETE : Blocks.BLACK_CONCRETE).defaultBlockState();
            put(level, box, bottle, X1 - 1, 1, z);
            put(level, box, bottle, X1 - 1, 2, z);
            put(level, box, Blocks.LIGHTNING_ROD.defaultBlockState(), X1 - 1, 3, z);
        }
        sign(level, box, X1 - 2, 2, 5, Direction.WEST, "O2", "fast, dark", "oxide edge", "2-8 bar");
        sign(level, box, X1 - 2, 2, 7, Direction.WEST, "N2", "bright edge", "weld-ready", "10-25 bar");
        // the sheet rack by the door, the scrap bin, the parts table
        for (int z = 20; z <= 23; z++) {
            put(level, box, Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP), X0 + 1, 1, z);
            fill(level, box, X0 + 1, 1, z, new ItemStack(FactoryRegistry.STEEL_SHEET, 16));
        }
        sign(level, box, X0 + 2, 2, 22, Direction.EAST, "", "SHEET STOCK", "1 per cut", "");
        put(level, box, Blocks.CAULDRON.defaultBlockState(), X0 + 1, 1, CZ);
        sign(level, box, X0 + 1, 2, CZ, Direction.EAST, "", "SKELETONS", "& scrap", "");
        for (int x = 44; x <= 46; x++) put(level, box, Blocks.IRON_BLOCK.defaultBlockState(), x, 1, 4);
        put(level, box, Blocks.CRAFTING_TABLE.defaultBlockState(), 47, 1, 4);
        put(level, box, Blocks.GRINDSTONE.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE,
                net.minecraft.world.level.block.state.properties.AttachFace.FLOOR), 43, 1, 4);
        sign(level, box, 45, 2, 3, Direction.SOUTH, "", "DEBURR &", "SORT", "");
        // Taryn's desk: a chair, a monitor, coffee
        put(level, box, Blocks.SPRUCE_PLANKS.defaultBlockState(), 44, 1, 17);
        put(level, box, Blocks.SPRUCE_PLANKS.defaultBlockState(), 45, 1, 17);
        put(level, box, BlockRegistry.CHAIR.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH), 44, 1, 18);
        put(level, box, dev.freerun.food.FoodRegistry.COFFEE_MAKER.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH), 45, 2, 17);
    }

    private void spawn(WorldGenLevel level, BoundingBox box) {
        // sheets waiting on the shuttle table
        for (int i = 0; i < 3; i++) {
            BlockPos at = pos(CX + 5, 1, CZ);
            if (!box.isInside(at)) break;
            level.addFreshEntity(SteelPartEntity.single(level.getLevel(), PartType.SHEET, at.getX() + 0.5, at.getY() + 0.76 + i * 0.07, at.getZ() + 0.5, 0));
        }
        BlockPos t = pos(45, 1, 15);
        if (box.isInside(t)) {
            TarynEntity taryn = new TarynEntity(LaserRegistry.TARYN, level.getLevel());
            taryn.moveTo(t.getX() + 0.5, t.getY(), t.getZ() + 0.5, 90.0f, 0.0f);
            taryn.setHome(pos(46, 1, 14));
            level.addFreshEntity(taryn);
        }
    }

    private void sign(WorldGenLevel level, BoundingBox box, int x, int y, int z, Direction facing, String... lines) {
        BlockPos p = pos(x, y, z);
        if (!box.isInside(p)) return;
        put(level, box, Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, facing), x, y, z);
        AnnexHelpers.signText(level, p, lines);
    }

    private void fill(WorldGenLevel level, BoundingBox box, int x, int y, int z, ItemStack... stacks) {
        BlockPos p = pos(x, y, z);
        if (!box.isInside(p)) return;
        if (level.getBlockEntity(p) instanceof Container c) {
            for (int i = 0; i < stacks.length && i < c.getContainerSize(); i++) c.setItem(i, stacks[i]);
        }
    }
}
