package dev.freerun.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * The welding factory. Only generates on fairly flat, dry land: it samples the four corners and the
 * middle of its footprint and gives up if any of them is under water or the ground varies by more
 * than 6 blocks. Terrain adaptation (beard_box, in the JSON) fills in the rest underneath.
 */
public class FactoryStructure extends Structure {
    public static final MapCodec<FactoryStructure> CODEC = simpleCodec(FactoryStructure::new);
    private static final int MAX_SLOPE = 6;
    /** The Double Quick sits east of the parking lot; the General Store beside the factory's east wall. */
    static final int DQ_X = FactoryPiece.MAX_X + 3, DQ_Z = -DoubleQuickPiece.D;
    static final int GS_X = FactoryPiece.MAX_X + 4, GS_Z = 3;

    public FactoryStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos cp = context.chunkPos();
        int x0 = cp.getMinBlockX();
        int z0 = cp.getMinBlockZ();
        int[][] samples = {{0, 0}, {FactoryPiece.MAX_X, 0}, {0, FactoryPiece.MAX_Z},
                {FactoryPiece.MAX_X, FactoryPiece.MAX_Z}, {FactoryPiece.MAX_X / 2, FactoryPiece.MAX_Z / 2}};
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int centre = 0;
        for (int[] s : samples) {
            int x = x0 + s[0];
            int z = z0 + s[1];
            int ground = context.chunkGenerator().getFirstFreeHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG,
                    context.heightAccessor(), context.randomState());
            int surface = context.chunkGenerator().getFirstFreeHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
                    context.heightAccessor(), context.randomState());
            if (surface > ground) return Optional.empty();   // water on top: ocean, river, lake
            min = Math.min(min, ground);
            max = Math.max(max, ground);
            centre = ground;   // the last sample is the middle
        }
        if (max - min > MAX_SLOPE) return Optional.empty();
        if (centre <= context.heightAccessor().getMinBuildHeight() + 2) return Optional.empty();

        BlockPos origin = new BlockPos(x0, centre - 1, z0);   // the floor replaces the top ground block
        return Optional.of(new GenerationStub(origin, builder -> {
            builder.addPiece(new FactoryPiece(origin));
            builder.addPiece(new ParkingLotPiece(origin));
            builder.addPiece(new BreakRoomPiece(origin));
            builder.addPiece(new ProcessingPiece(origin));
            builder.addPiece(new DoubleQuickPiece(origin.offset(DQ_X, 0, DQ_Z)));
            builder.addPiece(new GeneralStorePiece(origin.offset(GS_X, 0, GS_Z)));
        }));
    }

    @Override
    public StructureType<?> type() {
        return StructureRegistry.WELDING_FACTORY;
    }
}
