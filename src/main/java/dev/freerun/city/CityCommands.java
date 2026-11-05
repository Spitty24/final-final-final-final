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
        dispatcher.register(Commands.literal("cityprobe").requires(s -> s.hasPermission(2))
                .executes(ctx -> probe(ctx.getSource(), false))
                .then(Commands.literal("check").executes(ctx -> probe(ctx.getSource(), true))));
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

    /**
     * Test helper: force-load the middle of the nearest generated city (so world generation builds
     * it even with no players about), then later check that the streets are really there.
     */
    private static int probe(CommandSourceStack src, boolean check) {
        ServerLevel level = src.getServer().overworld();
        BlockPos from = BlockPos.containing(src.getPosition());
        BlockPos o = null;
        double best = Double.MAX_VALUE;
        int rx = Math.floorDiv(from.getX(), CityWorld.REGION), rz = Math.floorDiv(from.getZ(), CityWorld.REGION);
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            BlockPos s = CityWorld.naturalSite(level.getSeed(), rx + dx, rz + dz, level.getChunkSource().getGenerator(),
                    level.getChunkSource().randomState(), level);
            if (s != null && CityWorld.centre(s).distSqr(from) < best) {
                best = CityWorld.centre(s).distSqr(from);
                o = s;
            }
        }
        if (o == null) {
            src.sendFailure(Component.literal("City probe: no natural city site nearby."));
            return 0;
        }
        BlockPos key = CityLayout.intersectionKey(o, 9, 9);
        int cx = key.getX() >> 4, cz = key.getZ() >> 4;
        if (!check) {
            for (int x = cx - 2; x <= cx + 2; x++) for (int z = cz - 2; z <= cz + 2; z++) level.setChunkForced(x, z, true);
            src.sendSuccess(() -> Component.literal("City probe: forcing 25 chunks around " + key.getX() + " " + key.getZ()
                    + " (downtown " + CityWorld.centre(o).getX() + " " + CityWorld.centre(o).getZ() + ")"), true);
            return 1;
        }
        boolean loaded = level.isLoaded(key);
        String block = loaded ? level.getBlockState(key).getBlock().getDescriptionId() : "(not loaded)";
        boolean street = loaded && level.getBlockState(key).is(net.minecraft.world.level.block.Blocks.BLACK_CONCRETE);
        int people = level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, new net.minecraft.world.phys.AABB(key).inflate(48)).size();
        src.sendSuccess(() -> Component.literal("City probe: street found = " + street + " (" + block + "), "
                + people + " mobs within 48, city here = " + (CityWorld.cityAt(level, key) != null)), true);
        for (int x = cx - 2; x <= cx + 2; x++) for (int z = cz - 2; z <= cz + 2; z++) level.setChunkForced(x, z, false);
        return street ? 1 : 0;
    }

    public static String compass(double dx, double dz) {
        double a = Math.toDegrees(Math.atan2(dx, -dz));
        String[] names = {"north", "north-east", "east", "south-east", "south", "south-west", "west", "north-west"};
        return names[Math.floorMod((int) Math.round(a / 45.0), 8)];
    }
}
