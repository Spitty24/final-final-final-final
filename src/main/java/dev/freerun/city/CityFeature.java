package dev.freerun.city;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Cities are too big (over a kilometre across) for the structure system, which only looks about
 * eight chunks around a structure's start. So they're a decoration step instead: every overworld
 * chunk asks "is this chunk part of a city?" (see {@link CityWorld#naturalSite}) and, if so,
 * builds its slice of it.
 */
public class CityFeature extends Feature<NoneFeatureConfiguration> {
    public CityFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        ServerLevel server = level.getLevel();
        if (server.dimension() != Level.OVERWORLD) return false;
        ChunkPos cp = new ChunkPos(ctx.origin());
        int x = cp.getMiddleBlockX(), z = cp.getMiddleBlockZ();
        int rx = Mth.floorDiv(x, CityWorld.REGION), rz = Mth.floorDiv(z, CityWorld.REGION);
        BlockPos city = CityWorld.naturalSite(level.getSeed(), rx, rz, ctx.chunkGenerator(), server.getChunkSource().randomState(), level);
        if (city == null) return false;
        // does this chunk (16 blocks square) touch the city or its sloped margin?
        int m = CityWorld.MARGIN;
        if (cp.getMaxBlockX() < city.getX() - m || cp.getMinBlockX() >= city.getX() + CityLayout.SIZE + m) return false;
        if (cp.getMaxBlockZ() < city.getZ() - m || cp.getMinBlockZ() >= city.getZ() + CityLayout.SIZE + m) return false;
        try {
            CityPlan.buildChunk(level, city, cp, ctx.chunkGenerator(), ctx.random());
        } catch (RuntimeException e) {
            // never take world generation down with us; the hole gets filled in when the chunk loads
            dev.freerun.Freerun.LOGGER.error("City generation failed in chunk {} (city at {})", cp, city, e);
        }
        return true;
    }
}
