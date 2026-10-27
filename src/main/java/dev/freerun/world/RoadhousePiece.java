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

        // Sign board over the door (front wall, y==3)
        for (int x = 4; x <= 10; x++) {
            BlockState s = (x == 4 || x == 10) ? yellow : (x == 7 ? white : red);
            placeBlock(level, s, x, 3, 0, box);
        }

        // Interior tables (fence + pressure plate) and peanut barrels
        int[][] tables = {{4, 4}, {10, 4}, {4, 10}, {10, 10}, {7, 7}};
        for (int[] t : tables) {
            placeBlock(level, Blocks.SPRUCE_FENCE.defaultBlockState(), t[0], 1, t[1], box);
            placeBlock(level, Blocks.OAK_PRESSURE_PLATE.defaultBlockState(), t[0], 2, t[1], box);
        }
        placeBlock(level, Blocks.BARREL.defaultBlockState(), 2, 1, 2, box);
        placeBlock(level, Blocks.BARREL.defaultBlockState(), 12, 1, 2, box);
        placeBlock(level, Blocks.LANTERN.defaultBlockState(), 2, 1, 12, box);
        placeBlock(level, Blocks.LANTERN.defaultBlockState(), 12, 1, 12, box);
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
