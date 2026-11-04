package dev.freerun.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * A roadside Double Quick with a General Store next door, out in the countryside. Same flat, dry
 * ground check as the factory.
 */
public class DoubleQuickStructure extends Structure {
    public static final MapCodec<DoubleQuickStructure> CODEC = simpleCodec(DoubleQuickStructure::new);
    static final int STORE_X = DoubleQuickPiece.W + 3;

    public DoubleQuickStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos cp = context.chunkPos();
        int x0 = cp.getMinBlockX();
        int z0 = cp.getMinBlockZ();
        int maxX = STORE_X + GeneralStorePiece.W;
        int maxZ = DoubleQuickPiece.D;
        int[][] samples = {{0, 0}, {maxX, 0}, {0, maxZ}, {maxX, maxZ}, {maxX / 2, maxZ / 2}};
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE, centre = 0;
        for (int[] s : samples) {
            int ground = context.chunkGenerator().getFirstFreeHeight(x0 + s[0], z0 + s[1], Heightmap.Types.OCEAN_FLOOR_WG,
                    context.heightAccessor(), context.randomState());
            int surface = context.chunkGenerator().getFirstFreeHeight(x0 + s[0], z0 + s[1], Heightmap.Types.WORLD_SURFACE_WG,
                    context.heightAccessor(), context.randomState());
            if (surface > ground) return Optional.empty();
            min = Math.min(min, ground);
            max = Math.max(max, ground);
            centre = ground;
        }
        if (max - min > 4) return Optional.empty();
        BlockPos origin = new BlockPos(x0, centre - 1, z0);
        return Optional.of(new GenerationStub(origin, builder -> {
            builder.addPiece(new DoubleQuickPiece(origin));
            builder.addPiece(new GeneralStorePiece(origin.offset(STORE_X, 0, 2)));
        }));
    }

    @Override
    public StructureType<?> type() {
        return StructureRegistry.DOUBLE_QUICK;
    }
}
