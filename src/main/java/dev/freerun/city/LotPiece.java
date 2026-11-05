package dev.freerun.city;

import dev.freerun.vehicle.CarEntity;
import dev.freerun.vehicle.VehicleRegistry;
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
 * grass (kind 0) or a striped parking lot with a few parked cars (kind 1). Buildings that come
 * from elsewhere (the factory, the Roadhouse, the stores) are placed on top of one of these.
 */
public class LotPiece extends CityPieceBase {
    public static final int GRASS = 0, PARKING = 1;
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
        if (kind == GRASS) {
            ground(level, box, Blocks.GRASS_BLOCK.defaultBlockState(), h);
            return;
        }
        ground(level, box, Blocks.BLACK_CONCRETE.defaultBlockState(), h);
        BlockState paint = Blocks.WHITE_CONCRETE.defaultBlockState();
        // two rows of stalls facing a middle aisle
        for (int x = 1; x < w - 1; x++) {
            if (x % 4 == 0) {
                fill(level, box, paint, x, 0, 1, x, 0, 5);
                fill(level, box, paint, x, 0, d - 6, x, 0, d - 2);
            }
        }
        for (int x = 2; x + 3 < w - 1; x += 4) {
            if ((x * 7 + ox) % 3 == 0) continue;   // some empty spots
            car(level, box, x + 0.5, 3.5, 0.0f);
            if ((x * 5 + oz) % 2 == 0) car(level, box, x + 0.5, d - 3.5, 180.0f);
        }
    }

    private void car(WorldGenLevel level, BoundingBox box, double x, double z, float yaw) {
        BlockPos p = at((int) x, 1, (int) z);
        if (!box.isInside(p)) return;
        CarEntity car = new CarEntity(VehicleRegistry.CAR, server(level));
        car.moveTo(ox + x, oy + 1, oz + z, yaw, 0.0f);
        car.setYBodyRot(yaw);
        car.setYHeadRot(yaw);
        car.randomColor();
        car.setCity(city);
        level.addFreshEntity(car);
    }
}
