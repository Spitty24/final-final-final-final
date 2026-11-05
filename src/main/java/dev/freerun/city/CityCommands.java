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
        dispatcher.register(Commands.literal("cityclean").requires(s -> s.hasPermission(2))
                .executes(ctx -> clean(ctx.getSource(), 4))
                .then(Commands.argument("radius", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 16))
                        .executes(ctx -> clean(ctx.getSource(), com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "radius")))));
        dispatcher.register(Commands.literal("cityclear").requires(s -> s.hasPermission(2)).executes(ctx -> {
            int n = TrafficControl.clearAll(ctx.getSource().getLevel(), BlockPos.containing(ctx.getSource().getPosition()));
            ctx.getSource().sendSuccess(() -> Component.literal("Cleared " + n + " stuck car" + (n == 1 ? "" : "s") + "."), true);
            return n;
        }));
    }

    /** Sweeps stray trees, grass and dirt out of the city chunks within {@code radius} chunks of you. */
    private static int clean(CommandSourceStack src, int radius) {
        ServerLevel level = src.getLevel();
        net.minecraft.world.level.ChunkPos here = new net.minecraft.world.level.ChunkPos(BlockPos.containing(src.getPosition()));
        int blocks = 0, chunks = 0;
        for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
            net.minecraft.world.level.ChunkPos cp = new net.minecraft.world.level.ChunkPos(here.x + dx, here.z + dz);
            if (level.getChunkSource().getChunkNow(cp.x, cp.z) == null) continue;
            BlockPos o = CityWorld.anyCityFor(level, cp);
            if (o == null) continue;
            blocks += CityCleaner.clean(level, o, cp, true);
            chunks++;
        }
        int b = blocks, c = chunks;
        if (c == 0) src.sendFailure(Component.literal("You're not in a city."));
        else src.sendSuccess(() -> Component.literal("Swept " + c + " city chunks: removed " + b + " stray leaves, logs, plants and dirt."), true);
        return b;
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
        // street level is the ground under you (the typical height across the city), not wherever you're flying
        var gen = level.getChunkSource().getGenerator();
        int[] h = new int[25];
        for (int i = 0; i < 5; i++) for (int j = 0; j < 5; j++) {
            int x = feet.getX() - CityLayout.HALF + i * (CityLayout.SIZE - 1) / 4, z = feet.getZ() - CityLayout.HALF + j * (CityLayout.SIZE - 1) / 4;
            h[i * 5 + j] = level.hasChunk(x >> 4, z >> 4)
                    ? level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR, x, z)
                    : gen.getBaseHeight(x, z, net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG, level, level.getChunkSource().randomState());
        }
        java.util.Arrays.sort(h);
        int ground = Math.max(h[12], gen.getSeaLevel() + 1);
        int here = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR, feet.getX(), feet.getZ());
        if (Math.abs(here - ground) <= 6) ground = Math.max(here, gen.getSeaLevel() + 1);   // flat-ish: match the ground you're on
        BlockPos origin = new BlockPos(feet.getX() - CityLayout.HALF, ground - 1, feet.getZ() - CityLayout.HALF);
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
        if (!CityWorld.surveyed(level, from, 2)) {
            // the map's still being drawn up: check back in a moment, off the main thread
            src.sendSuccess(() -> Component.literal("§7Checking the maps..."), false);
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                long until = System.currentTimeMillis() + 30_000;
                while (!CityWorld.surveyed(level, from, 2) && System.currentTimeMillis() < until) {
                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                src.getServer().execute(() -> report(src, level, from));
            });
            return 1;
        }
        return report(src, level, from);
    }

    private static int report(CommandSourceStack src, ServerLevel level, BlockPos from) {
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
        BlockPos city = o;
        BlockPos key = CityLayout.intersectionKey(city, 9, 9);
        int cx = key.getX() >> 4, cz = key.getZ() >> 4;
        if (!check) {
            for (int x = cx - 3; x <= cx + 3; x++) for (int z = cz - 3; z <= cz + 3; z++) level.setChunkForced(x, z, true);
            src.sendSuccess(() -> Component.literal("City probe: forcing 49 chunks around " + key.getX() + " " + key.getZ()
                    + " (downtown " + CityWorld.centre(city).getX() + " " + CityWorld.centre(city).getZ() + ")"), true);
            return 1;
        }
        int holes = 0, checked = 0;
        if (check) {
            for (int x = cx - 3; x <= cx + 3; x++) for (int z = cz - 3; z <= cz + 3; z++) {
                if (level.getChunkSource().getChunkNow(x, z) == null) continue;
                checked++;
                if (CityWorld.unbuiltColumns(level, city, new net.minecraft.world.level.ChunkPos(x, z)) >= 4) holes++;
            }
        }
        int holeCount = holes, checkedCount = checked;
        boolean loaded = level.isLoaded(key);
        String block = loaded ? level.getBlockState(key).getBlock().getDescriptionId() : "(not loaded)";
        boolean street = loaded && level.getBlockState(key).is(net.minecraft.world.level.block.Blocks.BLACK_CONCRETE);
        int people = level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, new net.minecraft.world.phys.AABB(key).inflate(48)).size();
        src.sendSuccess(() -> Component.literal("City probe: street found = " + street + " (" + block + "), "
                + people + " mobs within 48, city here = " + (CityWorld.cityAt(level, key) != null)
                + ", holes = " + holeCount + " of " + checkedCount + " chunks"), true);
        for (int x = cx - 3; x <= cx + 3; x++) for (int z = cz - 3; z <= cz + 3; z++) level.setChunkForced(x, z, false);
        return street ? 1 : 0;
    }

    public static String compass(double dx, double dz) {
        double a = Math.toDegrees(Math.atan2(dx, -dz));
        String[] names = {"north", "north-east", "east", "south-east", "south", "south-west", "west", "north-west"};
        return names[Math.floorMod((int) Math.round(a / 45.0), 8)];
    }
}
