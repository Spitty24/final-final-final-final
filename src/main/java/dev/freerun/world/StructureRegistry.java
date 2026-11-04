package dev.freerun.world;

import dev.freerun.Freerun;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

/**
 * Registers the structure's code-side pieces. The actual placement (where/how often it spawns) is
 * defined by the JSON in data/freerun/worldgen/structure and .../structure_set.
 */
public final class StructureRegistry {
    public static StructureType<RoadhouseStructure> TEXAS_ROADHOUSE;
    public static StructurePieceType ROADHOUSE_PIECE;
    public static StructureType<FactoryStructure> WELDING_FACTORY;
    public static StructurePieceType FACTORY_PIECE;

    private StructureRegistry() {}

    public static void register() {
        TEXAS_ROADHOUSE = Registry.register(BuiltInRegistries.STRUCTURE_TYPE,
                Freerun.id("texas_roadhouse"), () -> RoadhouseStructure.CODEC);
        ROADHOUSE_PIECE = Registry.register(BuiltInRegistries.STRUCTURE_PIECE,
                Freerun.id("texas_roadhouse"), RoadhousePiece::new);
        WELDING_FACTORY = Registry.register(BuiltInRegistries.STRUCTURE_TYPE,
                Freerun.id("welding_factory"), () -> FactoryStructure.CODEC);
        FACTORY_PIECE = Registry.register(BuiltInRegistries.STRUCTURE_PIECE,
                Freerun.id("welding_factory"), FactoryPiece::new);
    }
}
