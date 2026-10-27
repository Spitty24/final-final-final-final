package dev.freerun.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * A Texas Roadhouse building. The structure just picks a surface spot at the chunk centre and drops
 * a single {@link RoadhousePiece}, which builds the restaurant out of blocks in code (no .nbt file).
 */
public class RoadhouseStructure extends Structure {
    public static final MapCodec<RoadhouseStructure> CODEC = simpleCodec(RoadhouseStructure::new);

    public RoadhouseStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int x = context.chunkPos().getMiddleBlockX();
        int z = context.chunkPos().getMiddleBlockZ();
        int y = context.chunkGenerator().getFirstFreeHeight(
                x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
        if (y <= context.heightAccessor().getMinBuildHeight() + 2) return Optional.empty();

        BlockPos origin = new BlockPos(x, y - 1, z);   // sink one so the floor sits at ground level
        return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(new RoadhousePiece(origin))));
    }

    @Override
    public StructureType<?> type() {
        return StructureRegistry.TEXAS_ROADHOUSE;
    }
}
