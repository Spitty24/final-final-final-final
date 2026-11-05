package dev.freerun.world;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/**
 * /roadhouse — builds a Texas Roadhouse at the command source's position immediately, in code.
 * This guarantees you can always get one to look at, regardless of world generation.
 */
public final class RoadhouseCommand {

    private RoadhouseCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("roadhouse")
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> build(ctx.getSource())));
        dispatcher.register(Commands.literal("factory")
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> buildFactory(ctx.getSource())));
        dispatcher.register(Commands.literal("city")
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> buildCity(ctx.getSource())));
        dispatcher.register(Commands.literal("doublequick")
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> buildDoubleQuick(ctx.getSource())));
    }

    /**
     * /factory — builds the welding factory with its front door just ahead of you (the building
     * extends south, +Z). Adds a stone foundation so it doesn't float on uneven ground.
     */
    private static int buildFactory(CommandSourceStack src) {
        ServerLevel level = src.getLevel();
        BlockPos feet = BlockPos.containing(src.getPosition());
        BlockPos origin = feet.offset(-FactoryPiece.MAX_X / 2, -1, 2 + ParkingLotPiece.DEPTH);
        for (int x = 0; x <= FactoryStructure.DQ_X + DoubleQuickPiece.W; x++) {
            for (int z = FactoryStructure.DQ_Z; z <= FactoryPiece.MAX_Z + 18; z++) {
                for (int y = 1; y <= 6; y++) {
                    BlockPos p = origin.offset(x, -y, z);
                    net.minecraft.world.level.block.state.BlockState st = level.getBlockState(p);
                    if (!st.isAir() && !st.liquid() && !st.canBeReplaced()) break;
                    level.setBlock(p, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 2);
                }
            }
        }
        FactoryPiece piece = new FactoryPiece(origin);
        piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(),
                level.getRandom(), piece.getBoundingBox(), new ChunkPos(origin), origin);
        ParkingLotPiece lot = new ParkingLotPiece(origin);
        lot.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(),
                level.getRandom(), lot.getBoundingBox(), new ChunkPos(origin), origin);
        for (net.minecraft.world.level.levelgen.structure.StructurePiece annex :
                new net.minecraft.world.level.levelgen.structure.StructurePiece[]{new BreakRoomPiece(origin), new ProcessingPiece(origin),
                        new DoubleQuickPiece(origin.offset(FactoryStructure.DQ_X, 0, FactoryStructure.DQ_Z)),
                        new GeneralStorePiece(origin.offset(FactoryStructure.GS_X, 0, FactoryStructure.GS_Z))}) {
            annex.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(),
                    level.getRandom(), annex.getBoundingBox(), new ChunkPos(origin), origin);
        }
        src.sendSuccess(() -> Component.literal(
                "Welding factory built: the parking lot starts just ahead of you (south), the building is past it. "
                        + "The Double Quick is east of the lot, the General Store beside the factory."), true);
        return 1;
    }

    /**
     * /city: builds a whole city centred on you (244 x 244 blocks: streets, downtown, suburbs, the
     * hood, the park, the factory, the Roadhouse and the stores). Takes a few seconds.
     */
    private static int buildCity(CommandSourceStack src) {
        ServerLevel level = src.getLevel();
        BlockPos feet = BlockPos.containing(src.getPosition());
        BlockPos city = feet.offset(-dev.freerun.city.CityLayout.HALF, -1, -dev.freerun.city.CityLayout.HALF);
        java.util.List<net.minecraft.world.level.levelgen.structure.StructurePiece> pieces = dev.freerun.city.CityPlan.pieces(city);
        net.minecraft.world.level.levelgen.structure.BoundingBox all = new net.minecraft.world.level.levelgen.structure.BoundingBox(
                city.getX(), city.getY() - 8, city.getZ(), city.getX() + dev.freerun.city.CityLayout.SIZE, city.getY() + 64,
                city.getZ() + dev.freerun.city.CityLayout.SIZE);
        // build chunk by chunk, the way world generation would
        for (int cx = all.minX() >> 4; cx <= all.maxX() >> 4; cx++) {
            for (int cz = all.minZ() >> 4; cz <= all.maxZ() >> 4; cz++) {
                ChunkPos cp = new ChunkPos(cx, cz);
                net.minecraft.world.level.levelgen.structure.BoundingBox chunkBox = new net.minecraft.world.level.levelgen.structure.BoundingBox(
                        cp.getMinBlockX(), level.getMinBuildHeight(), cp.getMinBlockZ(), cp.getMaxBlockX(), level.getMaxBuildHeight(), cp.getMaxBlockZ());
                for (var piece : pieces) {
                    if (!piece.getBoundingBox().intersects(chunkBox)) continue;
                    piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(),
                            level.getRandom(), chunkBox, cp, city);
                }
            }
        }
        src.sendSuccess(() -> Component.literal("City built around you. Open your phone's Map app to find your way around."), true);
        return 1;
    }

    /** /doublequick: a Double Quick and a General Store, starting just ahead of you. */
    private static int buildDoubleQuick(CommandSourceStack src) {
        ServerLevel level = src.getLevel();
        BlockPos feet = BlockPos.containing(src.getPosition());
        BlockPos origin = feet.offset(-DoubleQuickPiece.W / 2, -1, 3);
        net.minecraft.world.level.levelgen.structure.StructurePiece[] pieces = {new DoubleQuickPiece(origin),
                new GeneralStorePiece(origin.offset(DoubleQuickStructure.STORE_X, 0, 2))};
        for (var piece : pieces) {
            var bb = piece.getBoundingBox();
            for (int x = bb.minX(); x <= bb.maxX(); x++) {
                for (int z = bb.minZ(); z <= bb.maxZ(); z++) {
                    for (int y = 2; y <= 7; y++) {
                        BlockPos p = new BlockPos(x, origin.getY() - y, z);
                        net.minecraft.world.level.block.state.BlockState st = level.getBlockState(p);
                        if (!st.isAir() && !st.liquid() && !st.canBeReplaced()) break;
                        level.setBlock(p, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 2);
                    }
                }
            }
            piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(),
                    level.getRandom(), piece.getBoundingBox(), new ChunkPos(origin), origin);
        }
        src.sendSuccess(() -> Component.literal("Double Quick and General Store built just ahead of you (south)."), true);
        return 1;
    }

    private static int build(CommandSourceStack src) {
        ServerLevel level = src.getLevel();
        BlockPos pos = BlockPos.containing(src.getPosition());
        RoadhousePiece piece = new RoadhousePiece(pos);
        piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(),
                level.getRandom(), piece.getBoundingBox(), new ChunkPos(pos), pos);
        src.sendSuccess(() -> Component.literal("Texas Roadhouse built at " + pos.getX() + " " + pos.getY() + " " + pos.getZ()), true);
        return 1;
    }
}
