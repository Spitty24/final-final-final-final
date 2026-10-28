package dev.freerun.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;

/**
 * Builds the Texas Roadhouse: spruce-log frame, red roof and accent band, windows, an open doorway,
 * a lit interior with tables and barrels, and a red/yellow/white sign board over the door.
 * Footprint 15x15, placed with local coordinates 0..14 via {@link #placeBlock}.
 */
public class RoadhousePiece extends StructurePiece {
    private static final int SIZE = 14;   // max index (15 wide)
    private static final int WALL = 4;

    public RoadhousePiece(BlockPos origin) {
        super(StructureRegistry.ROADHOUSE_PIECE, 0, new BoundingBox(
                origin.getX(), origin.getY(), origin.getZ(),
                origin.getX() + SIZE, origin.getY() + 6, origin.getZ() + SIZE));
        // Give the piece a concrete orientation so placeBlock() has a non-null rotation/mirror
        // (SOUTH maps local x/y/z straight to world with no rotation). Without this, placing a
        // directional block like a spruce log throws an NPE and crashes world generation.
        setOrientation(Direction.SOUTH);
    }

    public RoadhousePiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(StructureRegistry.ROADHOUSE_PIECE, tag);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        // bounding box is saved by the base class; nothing extra to store
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator chunkGenerator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        BlockState planks = Blocks.SPRUCE_PLANKS.defaultBlockState();
        BlockState log = Blocks.SPRUCE_LOG.defaultBlockState();
        BlockState red = Blocks.RED_TERRACOTTA.defaultBlockState();
        BlockState yellow = Blocks.YELLOW_TERRACOTTA.defaultBlockState();
        BlockState white = Blocks.WHITE_TERRACOTTA.defaultBlockState();
        BlockState glass = Blocks.GLASS_PANE.defaultBlockState();
        BlockState glow = Blocks.GLOWSTONE.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        // Floor
        for (int x = 0; x <= SIZE; x++)
            for (int z = 0; z <= SIZE; z++)
                placeBlock(level, planks, x, 0, z, box);

        // Carve interior
        for (int x = 1; x < SIZE; x++)
            for (int z = 1; z < SIZE; z++)
                for (int y = 1; y <= WALL; y++)
                    placeBlock(level, air, x, y, z, box);

        // Walls
        for (int x = 0; x <= SIZE; x++) {
            for (int z = 0; z <= SIZE; z++) {
                boolean edge = x == 0 || x == SIZE || z == 0 || z == SIZE;
                if (!edge) continue;
                boolean corner = (x == 0 || x == SIZE) && (z == 0 || z == SIZE);
                for (int y = 1; y <= WALL; y++) {
                    BlockState s = corner ? log : planks;
                    if (y == 2 && !corner) s = red;                 // red accent band
                    placeBlock(level, s, x, y, z, box);
                }
                // Windows
                if (y3Window(x, z)) placeBlock(level, glass, x, 3, z, box);
            }
        }

        // Doorway (front wall is z==0), centred on x==7
        placeBlock(level, air, 7, 1, 0, box);
        placeBlock(level, air, 7, 2, 0, box);

        // Ceiling with embedded glowstone
        for (int x = 1; x < SIZE; x++)
            for (int z = 1; z < SIZE; z++)
                placeBlock(level, ((x + z) % 5 == 0) ? glow : planks, x, WALL, z, box);

        // Roof: full red layer, then a smaller stepped layer for shape
        for (int x = 0; x <= SIZE; x++)
            for (int z = 0; z <= SIZE; z++)
                placeBlock(level, red, x, 5, z, box);
        for (int x = 2; x <= SIZE - 2; x++)
            for (int z = 2; z <= SIZE - 2; z++)
                placeBlock(level, red, x, 6, z, box);

        // --- Custom Texas Roadhouse sign over the door (front wall z==0, facing out = NORTH) ---
        BlockState sign = dev.freerun.world.BlockRegistry.TEXAS_ROADHOUSE_SIGN.defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.NORTH);
        for (int x = 6; x <= 8; x++) placeBlock(level, sign, x, 3, 0, box);

        // --- Dining room (front half, low z): tables with checkered cloths + hanging lanterns ---
        BlockState cloth = dev.freerun.world.BlockRegistry.CHECKERED_TABLECLOTH.defaultBlockState();
        BlockState fence = Blocks.SPRUCE_FENCE.defaultBlockState();
        int[][] diningTables = {{3, 3}, {7, 3}, {11, 3}, {3, 6}, {11, 6}};
        for (int[] t : diningTables) {
            placeBlock(level, fence, t[0], 1, t[1], box);
            placeBlock(level, cloth, t[0], 2, t[1], box);
        }
        placeBlock(level, Blocks.BARREL.defaultBlockState(), 2, 1, 2, box);   // peanut barrel by the door
        placeBlock(level, hangingLantern(), 4, WALL - 1, 4, box);
        placeBlock(level, hangingLantern(), 10, WALL - 1, 4, box);

        // --- Divider wall between dining and kitchen at z==9, doorway at x==7 ---
        for (int x = 1; x < SIZE; x++) {
            for (int y = 1; y <= WALL; y++) {
                if (x == 7 && (y == 1 || y == 2)) continue;   // doorway
                placeBlock(level, planks, x, y, 9, box);
            }
        }

        // --- Kitchen (back, z 10..12) ---
        BlockState grill = dev.freerun.world.BlockRegistry.GRILL.defaultBlockState();
        BlockState counter = Blocks.SMOOTH_QUARTZ.defaultBlockState();   // stainless prep counters
        // Cooking line along the back wall (z==12): alternating grills and smokers.
        for (int x = 2; x <= 12; x++) {
            placeBlock(level, (x % 2 == 0) ? grill : Blocks.SMOKER.defaultBlockState(), x, 1, 12, box);
        }
        // Prep island counter (z==10)
        for (int x = 4; x <= 9; x++) placeBlock(level, counter, x, 1, 10, box);
        // Sink + pantry barrels
        placeBlock(level, Blocks.WATER_CAULDRON.defaultBlockState(), 2, 1, 10, box);
        placeBlock(level, Blocks.BARREL.defaultBlockState(), 12, 1, 10, box);
        placeBlock(level, Blocks.BARREL.defaultBlockState(), 12, 1, 11, box);
        placeBlock(level, hangingLantern(), 7, WALL - 1, 11, box);
    }

    private static BlockState hangingLantern() {
        return Blocks.LANTERN.defaultBlockState()
                .setValue(net.minecraft.world.level.block.LanternBlock.HANGING, Boolean.TRUE);
    }

    private static boolean y3Window(int x, int z) {
        boolean sideWall = x == 0 || x == SIZE;
        boolean frontBack = z == 0 || z == SIZE;
        boolean colX = x == 3 || x == 5 || x == 9 || x == 11;
        boolean colZ = z == 3 || z == 5 || z == 9 || z == 11;
        if (frontBack && colX && !(z == 0 && x == 7)) return true;   // not over the door
        return sideWall && colZ;
    }
}
