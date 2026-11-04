package dev.freerun.world;

import dev.freerun.food.CoffeeMakerBlock;
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
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * The factory break room, built onto the back of the welding department (local z=0 is the
 * factory's back wall, with the door in it). Kitchenette with a coffee maker, microwave, sink,
 * fridge, storage counters and cabinets; four tables with chairs; a TV lounge; lockers; vending
 * machines; coffee plants growing in planters; and a couple of workers on break.
 */
public class BreakRoomPiece extends StructurePiece {
    static final int OFFSET_X = 25;
    static final int OFFSET_Z = FactoryPiece.MAX_Z;
    private static final int W = 15;
    private static final int D = 14;
    private static final int CEIL = 6;

    public BreakRoomPiece(BlockPos factoryOrigin) {
        super(StructureRegistry.BREAK_ROOM_PIECE, 0, new BoundingBox(
                factoryOrigin.getX() + OFFSET_X, factoryOrigin.getY(), factoryOrigin.getZ() + OFFSET_Z,
                factoryOrigin.getX() + OFFSET_X + W, factoryOrigin.getY() + CEIL + 1, factoryOrigin.getZ() + OFFSET_Z + D));
        setOrientation(Direction.SOUTH);
    }

    public BreakRoomPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(StructureRegistry.BREAK_ROOM_PIECE, tag);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
    }

    private BlockState facing(net.minecraft.world.level.block.Block b, Direction d) {
        return b.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, d);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator chunkGenerator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        // ---- shell (z=0 is the factory's own back wall)
        for (int x = 0; x <= W; x++) {
            for (int z = 1; z <= D; z++) {
                boolean edge = x == 0 || x == W || z == D;
                placeBlock(level, ((x + z) % 2 == 0 ? Blocks.WHITE_TERRACOTTA : Blocks.LIGHT_GRAY_TERRACOTTA).defaultBlockState(), x, 0, z, box);
                for (int y = 1; y < CEIL; y++) {
                    BlockState s;
                    if (!edge) s = Blocks.AIR.defaultBlockState();
                    else if (y <= 2) s = Blocks.LIGHT_BLUE_TERRACOTTA.defaultBlockState();
                    else if (y == 3 && ((x == W && z % 3 == 1) || (z == D && x % 3 == 1))) s = Blocks.GLASS.defaultBlockState();
                    else s = Blocks.WHITE_CONCRETE.defaultBlockState();
                    placeBlock(level, s, x, y, z, box);
                }
                placeBlock(level, (!edge && x % 4 == 2 && z % 4 == 2 ? Blocks.SEA_LANTERN : Blocks.WHITE_CONCRETE).defaultBlockState(), x, CEIL, z, box);
                placeBlock(level, Blocks.GRAY_CONCRETE.defaultBlockState(), x, CEIL + 1, z, box);
            }
        }
        for (int x = 7; x <= 8; x++) {   // doorway through the factory wall
            for (int y = 1; y <= 3; y++) placeBlock(level, Blocks.AIR.defaultBlockState(), x, y, 0, box);
        }
        BlockPos s1 = getWorldPos(7, 4, 1).immutable();
        placeBlock(level, Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, Direction.SOUTH), 7, 4, 1, box);
        if (box.isInside(s1)) AnnexHelpers.signText(level, s1, "DAYS WITHOUT", "AN INCIDENT:", "0", "(parking lot)");
        BlockPos s2 = getWorldPos(8, 4, 1).immutable();
        placeBlock(level, Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, Direction.SOUTH), 8, 4, 1, box);
        if (box.isInside(s2)) AnnexHelpers.signText(level, s2, "BREAK ROOM", "Wash your mug.", "No metal in the", "microwave! -Mgmt");

        // ---- kitchenette on the east wall
        int kx = W - 1;
        for (int z = 2; z <= 9; z++) {
            if (z == 6) continue;
            placeBlock(level, facing(FoodRegistry.COUNTER, Direction.WEST), kx, 1, z, box);
            placeBlock(level, Blocks.SPRUCE_TRAPDOOR.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.WEST)
                    .setValue(TrapDoorBlock.OPEN, true).setValue(TrapDoorBlock.HALF, Half.TOP), kx, 3, z, box);
        }
        placeBlock(level, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3), kx, 1, 6, box);
        placeBlock(level, facing(Blocks.TRIPWIRE_HOOK, Direction.WEST), kx, 2, 6, box);   // faucet
        placeBlock(level, facing(FoodRegistry.COFFEE_MAKER, Direction.WEST).setValue(CoffeeMakerBlock.CUPS, 4), kx, 2, 3, box);
        placeBlock(level, Blocks.BREWING_STAND.defaultBlockState(), kx, 2, 4, box);        // the espresso machine
        placeBlock(level, facing(FoodRegistry.MICROWAVE, Direction.WEST), kx, 2, 8, box);
        placeBlock(level, Blocks.POTTED_FERN.defaultBlockState(), kx, 2, 9, box);
        placeBlock(level, Blocks.QUARTZ_PILLAR.defaultBlockState(), kx, 1, 11, box);       // fridge
        placeBlock(level, Blocks.QUARTZ_PILLAR.defaultBlockState(), kx, 2, 11, box);
        placeBlock(level, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.WALL)
                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST), kx - 1, 2, 11, box);
        BlockPos supplies = getWorldPos(kx, 1, 2).immutable();
        if (box.isInside(supplies)) {
            AnnexHelpers.fill(level, supplies, new ItemStack(FoodRegistry.COFFEE_BEANS, 16), new ItemStack(FoodRegistry.COFFEE_GROUNDS, 8),
                    new ItemStack(FoodRegistry.COFFEE_MUG, 8), new ItemStack(FoodRegistry.CUP_OF_COFFEE, 4),
                    new ItemStack(FoodRegistry.BOUDIN, 4), new ItemStack(FoodRegistry.BREAKFAST_LINKS, 4));
        }

        // ---- four tables with chairs
        int[][] tables = {{3, 4}, {3, 8}, {8, 4}, {8, 8}};
        for (int[] t : tables) {
            for (int dx = 0; dx <= 1; dx++) {
                placeBlock(level, BlockRegistry.TABLE.defaultBlockState(), t[0] + dx, 1, t[1], box);
                placeBlock(level, facing(BlockRegistry.CHAIR, Direction.SOUTH), t[0] + dx, 1, t[1] - 1, box);
                placeBlock(level, facing(BlockRegistry.CHAIR, Direction.NORTH), t[0] + dx, 1, t[1] + 1, box);
            }
        }

        // ---- TV lounge along the south wall
        for (int x = 4; x <= 7; x++) {
            placeBlock(level, Blocks.SPRUCE_SLAB.defaultBlockState(), x, 1, D - 1, box);
            placeBlock(level, Blocks.BLACK_CONCRETE.defaultBlockState(), x, 2, D - 1, box);
            placeBlock(level, Blocks.BLACK_CONCRETE.defaultBlockState(), x, 3, D - 1, box);
            placeBlock(level, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH), x, 1, D - 3, box);
        }
        placeBlock(level, facing(BlockRegistry.CHAIR, Direction.SOUTH), 2, 1, D - 3, box);
        placeBlock(level, facing(BlockRegistry.CHAIR, Direction.SOUTH), 9, 1, D - 3, box);

        // ---- lockers and vending machines on the west wall
        for (int z = 2; z <= 8; z++) {
            DoorHingeSide hinge = z % 2 == 0 ? DoorHingeSide.LEFT : DoorHingeSide.RIGHT;
            BlockState door = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.EAST).setValue(DoorBlock.HINGE, hinge);
            placeBlock(level, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), 1, 1, z, box);
            placeBlock(level, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 1, 2, z, box);
        }
        placeBlock(level, Blocks.RED_CONCRETE.defaultBlockState(), 1, 1, 10, box);
        placeBlock(level, Blocks.RED_STAINED_GLASS.defaultBlockState(), 1, 2, 10, box);
        placeBlock(level, Blocks.BLUE_CONCRETE.defaultBlockState(), 1, 1, 12, box);
        placeBlock(level, Blocks.BLUE_STAINED_GLASS.defaultBlockState(), 1, 2, 12, box);

        // ---- coffee planters along the north wall, watered from under a trapdoor
        planter(level, box, 1, 6, 4, new int[]{7, 7, 5, 0, 7, 3});
        planter(level, box, 10, 13, 11, new int[]{7, 0, 6, 7});

        // ---- a couple of the crew on their break
        BlockPos c1 = getWorldPos(3, 1, 3).immutable();
        if (box.isInside(c1)) AnnexHelpers.seatedWorker(level, c1, Direction.SOUTH, Role.WELDER, new ItemStack(FoodRegistry.CUP_OF_COFFEE));
        BlockPos c2 = getWorldPos(9, 1, 9).immutable();
        if (box.isInside(c2)) AnnexHelpers.seatedWorker(level, c2, Direction.NORTH, Role.PRESS_OPERATOR, new ItemStack(FoodRegistry.BOUDIN));
    }

    private void planter(WorldGenLevel level, BoundingBox box, int x0, int x1, int waterX, int[] ages) {
        for (int x = x0; x <= x1; x++) {
            if (x == waterX) {
                placeBlock(level, Blocks.SPRUCE_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.TOP)
                        .setValue(BlockStateProperties.WATERLOGGED, true), x, 0, 1, box);
                continue;
            }
            placeBlock(level, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7), x, 0, 1, box);
            int age = ages[(x - x0) % ages.length];
            placeBlock(level, FoodRegistry.COFFEE_PLANT.defaultBlockState().setValue(CropBlock.AGE, age), x, 1, 1, box);
        }
    }
}
