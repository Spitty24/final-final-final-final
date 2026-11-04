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
    public static StructurePieceType PARKING_LOT_PIECE;
    public static StructurePieceType BREAK_ROOM_PIECE;
    public static StructurePieceType PROCESSING_PIECE;
    public static StructureType<DoubleQuickStructure> DOUBLE_QUICK;
    public static StructurePieceType DOUBLE_QUICK_PIECE;
    public static StructurePieceType GENERAL_STORE_PIECE;

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
        PARKING_LOT_PIECE = Registry.register(BuiltInRegistries.STRUCTURE_PIECE,
                Freerun.id("factory_parking_lot"), ParkingLotPiece::new);
        BREAK_ROOM_PIECE = Registry.register(BuiltInRegistries.STRUCTURE_PIECE,
                Freerun.id("factory_break_room"), BreakRoomPiece::new);
        PROCESSING_PIECE = Registry.register(BuiltInRegistries.STRUCTURE_PIECE,
                Freerun.id("factory_processing"), ProcessingPiece::new);
        DOUBLE_QUICK = Registry.register(BuiltInRegistries.STRUCTURE_TYPE,
                Freerun.id("double_quick"), () -> DoubleQuickStructure.CODEC);
        DOUBLE_QUICK_PIECE = Registry.register(BuiltInRegistries.STRUCTURE_PIECE,
                Freerun.id("double_quick"), DoubleQuickPiece::new);
        GENERAL_STORE_PIECE = Registry.register(BuiltInRegistries.STRUCTURE_PIECE,
                Freerun.id("general_store"), GeneralStorePiece::new);
    }
}
