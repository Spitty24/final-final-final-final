package dev.freerun.city;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/** /city (build one around you), /findcity (where's the nearest), /cityclear (unjam the traffic). */
public final class CityCommands {
    private CityCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("city").requires(s -> s.hasPermission(2)).executes(ctx -> city(ctx.getSource())));
        dispatcher.register(Commands.literal("findcity").executes(ctx -> find(ctx.getSource())));
        dispatcher.register(Commands.literal("cityclear").requires(s -> s.hasPermission(2)).executes(ctx -> {
            int n = TrafficControl.clearAll(ctx.getSource().getLevel(), BlockPos.containing(ctx.getSource().getPosition()));
            ctx.getSource().sendSuccess(() -> Component.literal("Cleared " + n + " stuck car" + (n == 1 ? "" : "s") + "."), true);
            return n;
        }));
    }

    /**
     * Lays out a full city centred on you. It's over a kilometre across, so it isn't built in one
     * go: the part around you goes up over the next few seconds, and the rest is built as you
     * explore and those chunks load.
     */
    private static int city(CommandSourceStack src) {
        ServerLevel level = src.getLevel();
        if (level.dimension() != net.minecraft.world.level.Level.OVERWORLD) {
            src.sendFailure(Component.literal("Cities only go in the overworld."));
            return 0;
        }
        BlockPos feet = BlockPos.containing(src.getPosition());
        BlockPos origin = feet.offset(-CityLayout.HALF, -1, -CityLayout.HALF);
        if (CityWorld.cityAt(level, feet) != null) {
            src.sendFailure(Component.literal("You're already in a city."));
            return 0;
        }
        if (CityWorld.overlapsManual(level, origin)) {
            src.sendFailure(Component.literal("Too close to another city. Try /findcity, or move a kilometre or so away."));
            return 0;
        }
        BlockPos natural = CityWorld.overlapsNatural(level, origin);
        if (natural != null) {
            BlockPos c = CityWorld.centre(natural);
            src.sendFailure(Component.literal("There's already a city planned here, centred at " + c.getX() + " " + c.getZ()
                    + ". Go and have a look, or move further away."));
            return 0;
        }
        CityWorld.addManual(level, origin);
        src.sendSuccess(() -> Component.literal("§aCity founded! §7" + CityLayout.SIZE + " × " + CityLayout.SIZE
                + " blocks, centred on you. The streets around you go up over the next few seconds and the rest as you explore. "
                + "Open your phone's Map app to find your way around."), true);
        return 1;
    }

    private static int find(CommandSourceStack src) {
        ServerLevel level = src.getLevel();
        BlockPos from = BlockPos.containing(src.getPosition());
        BlockPos here = CityWorld.cityAt(level, from);
        if (here != null) {
            BlockPos c = CityWorld.centre(here);
            src.sendSuccess(() -> Component.literal("You're in a city. Downtown (City Hall) is at " + c.getX() + " " + c.getZ() + "."), false);
            return 1;
        }
        BlockPos o = CityWorld.nearestCity(level, from, 2);
        if (o == null) {
            src.sendFailure(Component.literal("No city within a few kilometres. Use /city to found one."));
            return 0;
        }
        BlockPos c = CityWorld.centre(o);
        int dist = (int) Math.sqrt(c.distSqr(from));
        String dir = compass(c.getX() - from.getX(), c.getZ() - from.getZ());
        src.sendSuccess(() -> Component.literal("Nearest city: downtown at §e" + c.getX() + " " + c.getZ() + "§r, about "
                + dist + " blocks " + dir + "."), false);
        return 1;
    }

    public static String compass(double dx, double dz) {
        double a = Math.toDegrees(Math.atan2(dx, -dz));
        String[] names = {"north", "north-east", "east", "south-east", "south", "south-west", "west", "north-west"};
        return names[Math.floorMod((int) Math.round(a / 45.0), 8)];
    }
}
