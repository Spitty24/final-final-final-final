package dev.freerun.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.StructurePiece;

import java.util.List;

/** Lets the city (in another package) reuse the existing buildings and helpers. */
public final class CityAccess {
    public static final int FACTORY_W = FactoryPiece.MAX_X + 1;
    public static final int LOT_DEPTH = ParkingLotPiece.DEPTH;

    private CityAccess() {}

    public static void signText(WorldGenLevel level, BlockPos pos, String... lines) {
        AnnexHelpers.signText(level, pos, lines);
    }

    public static void fill(WorldGenLevel level, BlockPos pos, ItemStack... stacks) {
        AnnexHelpers.fill(level, pos, stacks);
    }

    /** The whole welding-factory complex; {@code origin} is the factory's own front-left corner (floor level). */
    public static List<StructurePiece> factory(BlockPos origin) {
        return List.of(new FactoryPiece(origin), new ParkingLotPiece(origin), new BreakRoomPiece(origin), new ProcessingPiece(origin),
                new LaserBayPiece(origin));
    }

    public static StructurePiece roadhouse(BlockPos origin) {
        return new RoadhousePiece(origin);
    }

    public static StructurePiece doubleQuick(BlockPos origin) {
        return new DoubleQuickPiece(origin);
    }

    public static StructurePiece generalStore(BlockPos origin) {
        return new GeneralStorePiece(origin);
    }
}
