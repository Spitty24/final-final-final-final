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
