package dev.freerun.city;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * A whole city: streets, downtown, suburbs, the hood, the park, and every building the mod has
 * (the factory, the Roadhouse, the Double Quick, the General Store). It's centred on the chunk the
 * game picks, so everything stays within reach of the structure system. Needs dry land: it gives
 * up if water shows up across the footprint, and levels whatever terrain it lands on.
 */
public class CityStructure extends Structure {
    public static final MapCodec<CityStructure> CODEC = simpleCodec(CityStructure::new);

    public CityStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos cp = context.chunkPos();
        int cx = cp.getMiddleBlockX(), cz = cp.getMiddleBlockZ();
        int wet = 0, min = Integer.MAX_VALUE, max = Integer.MIN_VALUE, centre = 0;
        for (int sx = -2; sx <= 2; sx++) {
            for (int sz = -2; sz <= 2; sz++) {
                int x = cx + sx * 55, z = cz + sz * 55;
                int ground = context.chunkGenerator().getFirstFreeHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
                int surface = context.chunkGenerator().getFirstFreeHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
                if (surface > ground) wet++;
                min = Math.min(min, ground);
                max = Math.max(max, ground);
                if (sx == 0 && sz == 0) centre = ground;
            }
        }
        if (wet > 2 || max - min > 24) return Optional.empty();
        if (centre <= context.heightAccessor().getMinBuildHeight() + 8) return Optional.empty();
        BlockPos city = new BlockPos(cx - CityLayout.HALF, centre - 1, cz - CityLayout.HALF);
        return Optional.of(new GenerationStub(new BlockPos(cx, centre, cz), builder -> CityPlan.pieces(city).forEach(builder::addPiece)));
    }

    @Override
    public StructureType<?> type() {
        return CityRegistry.CITY;
    }
}
