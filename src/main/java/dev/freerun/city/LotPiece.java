package dev.freerun.city;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Prepares a patch of a city block: levels it, clears it out to {@code clear} blocks up, and lays
 * grass, plain asphalt (the service lanes that join neighbouring lots together), a paved plaza,
 * or a striped parking lot with light poles and a few parked cars. Buildings that come from
 * elsewhere (the factory, the Roadhouse, the stores) are placed on top of one of these.
 */
public class LotPiece extends CityPieceBase {
    public static final int GRASS = 0, PARKING = 1, PAVED = 2, PLAZA = 3, CONCRETE = 4;
    private final int kind;

    public LotPiece(BlockPos city, int lx, int lz, int w, int d, int clear, int kind) {
        super(CityRegistry.LOT_PIECE, city, lx, lz, w, clear, d);
        this.kind = kind;
    }

    public LotPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(CityRegistry.LOT_PIECE, tag);
        kind = tag.getInt("Kind");
    }

    @Override
    protected void save(CompoundTag tag) {
        tag.putInt("Kind", kind);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        switch (kind) {
            case GRASS -> ground(level, box, Blocks.GRASS_BLOCK.defaultBlockState(), h);
            case PAVED -> ground(level, box, Blocks.BLACK_CONCRETE.defaultBlockState(), h);
            case CONCRETE -> ground(level, box, Blocks.GRAY_CONCRETE.defaultBlockState(), h);
            case PLAZA -> {
                ground(level, box, Blocks.SMOOTH_STONE.defaultBlockState(), h);
                for (int x = 0; x < w; x++) for (int z = 0; z < d; z++) {
                    if ((x + z) % 2 == 0) put(level, box, Blocks.POLISHED_ANDESITE, x, 0, z);
                }
            }
            default -> parking(level, box, this, 0, 0, w, d, 4);
        }
    }

    /**
     * Stripes a parking lot into {@code piece}'s local box (x0, z0, w, d): back-to-back rows of
     * stalls along x with 6-block aisles between them, light poles, and a car in roughly one
     * stall in {@code oneIn}. Shared with the store pieces.
     */
    public static void parking(WorldGenLevel level, BoundingBox box, CityPieceBase piece, int x0, int z0, int w, int d, int oneIn) {
        BlockState asphalt = Blocks.BLACK_CONCRETE.defaultBlockState();
        BlockState paint = Blocks.WHITE_CONCRETE.defaultBlockState();
        piece.fill(level, box, Blocks.AIR.defaultBlockState(), x0, 1, z0, x0 + w - 1, piece.h, z0 + d - 1);
        piece.fill(level, box, asphalt, x0, 0, z0, x0 + w - 1, 0, z0 + d - 1);
        piece.support(level, box, x0, z0, x0 + w - 1, z0 + d - 1);
        // bands of stalls, 5 deep. Pattern down the lot: stalls, aisle, stalls|stalls, aisle, ...
        int z = z0 + 1;
        boolean facingSouth = true;
        int band = 0;
        while (z + 5 <= z0 + d) {
            for (int x = x0 + 1; x < x0 + w - 1; x++) {
                if ((x - x0) % 3 == 0) piece.fill(level, box, paint, x, 0, z, x, 0, z + 4);
            }
            for (int x = x0 + 2; x + 1 < x0 + w - 1; x += 3) {
                int hash = Math.floorMod((piece.ox + x) * 73 + (piece.oz + z) * 31, 97);
                if (hash % oneIn != 0) continue;
                piece.parkedCar(level, box, x + 0.5, z + 2.5, facingSouth ? 180.0f : 0.0f);
            }
            // light pole at the end of every other band
            if (band % 2 == 1) {
                int lx = x0 + w / 2, lz = facingSouth ? z : z + 4;
                piece.fill(level, box, Blocks.IRON_BARS.defaultBlockState(), lx, 1, lz, lx, 5, lz);
                piece.put(level, box, CityRegistry.STREET_LAMP, lx, 6, lz);
            }
            band++;
            if (facingSouth) {
                z += 5 + 6;          // stalls then an aisle
                facingSouth = false;
            } else {
                z += 5;              // back-to-back with the next row
                facingSouth = true;
            }
        }
    }
}
