package dev.freerun.chaos;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/**
 * Reverse Mode, switched on and off with the Backwards Clock. While it's on, saplings grow
 * upside down (the leaves appear first, up in the air, and the trunk grows down to the ground),
 * water from a small pool or a bucket climbs upward into a fountain instead of running down, and
 * the Ender Dragon turns up at your bed, homesick, waiting for you to walk her back to the End.
 */
public final class ReverseMode extends SavedData {
    /** Fast flag for the fluid code. */
    public static volatile boolean ON;
    public boolean on;
    public boolean dragonOut;

    private static final List<Growth> GROWING = new ArrayList<>();

    private record Growth(ServerLevel level, BlockPos ground, BlockState log, BlockState leaves, int height, int[] step) {}

    private static final Factory<ReverseMode> FACTORY = new Factory<>(ReverseMode::new, ReverseMode::load, null);

    private static ReverseMode load(CompoundTag tag, HolderLookup.Provider lookup) {
        ReverseMode m = new ReverseMode();
        m.on = tag.getBoolean("On");
        m.dragonOut = tag.getBoolean("DragonOut");
        return m;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup) {
        tag.putBoolean("On", on);
        tag.putBoolean("DragonOut", dragonOut);
        return tag;
    }

    public static ReverseMode get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, "freerun_reverse_mode");
    }

    static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> ON = get(server).on);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            ON = false;
            GROWING.clear();
        });
        ServerTickEvents.END_SERVER_TICK.register(ReverseMode::tick);
        // walk through to the End with her close by and she's home
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, from, to) -> {
            if (to.dimension() != Level.END) return;
            for (HomesickDragon d : from.getEntitiesOfClass(HomesickDragon.class, player.getBoundingBox().inflate(96))) d.goHome(from);
        });
    }

    public static void toggle(ServerPlayer p) {
        ReverseMode m = get(p.server);
        m.on = !m.on;
        ON = m.on;
        m.setDirty();
        if (m.on) {
            p.server.getPlayerList().broadcastSystemMessage(Component.literal("§d§l⟲ REVERSE MODE§r§d. Trees grow down. Water flows up. Somewhere, a dragon is lost."), false);
            if (!m.dragonOut) spawnDragon(p);
        } else {
            p.server.getPlayerList().broadcastSystemMessage(Component.literal("§7⟳ Reverse Mode is off. Things fall the right way again."), false);
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0f, 0.6f);
    }

    /** The Ender Dragon appears at your bed (or the world spawn), lost. */
    private static void spawnDragon(ServerPlayer p) {
        ServerLevel level = p.server.overworld();
        BlockPos bed = p.getRespawnPosition();
        if (bed == null || p.getRespawnDimension() != Level.OVERWORLD) bed = level.getSharedSpawnPos();
        HomesickDragon d = new HomesickDragon(ChaosRegistry.HOMESICK_DRAGON, level);
        d.moveTo(bed.getX() + 0.5, bed.getY() + 10, bed.getZ() + 0.5, p.getRandom().nextFloat() * 360, 0);
        level.addFreshEntity(d);
        ReverseMode m = get(p.server);
        m.dragonOut = true;
        m.setDirty();
        p.sendSystemMessage(Component.literal("§5The Ender Dragon is at your bed. She looks lost. §8She wants to go home."));
    }

    static void dragonHome(ServerLevel level) {
        ReverseMode m = get(level.getServer());
        m.dragonOut = false;
        m.setDirty();
    }

    // ------------------------------------------------------------------ upside-down trees

    /** From the sapling mixin: true = we've taken over this tree. */
    public static boolean growUpsideDown(ServerLevel level, BlockPos pos, BlockState sapling) {
        if (!ON) return false;
        String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(sapling.getBlock()).getPath();
        String wood = id.replace("_sapling", "").replace("_propagule", "");
        Block log = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.withDefaultNamespace(wood + "_log"));
        Block leaves = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.withDefaultNamespace(wood + "_leaves"));
        if (log == Blocks.AIR) log = Blocks.OAK_LOG;
        if (leaves == Blocks.AIR) leaves = Blocks.OAK_LEAVES;
        BlockState lv = leaves.defaultBlockState();
        if (lv.hasProperty(LeavesBlock.PERSISTENT)) lv = lv.setValue(LeavesBlock.PERSISTENT, true);
        int height = 5 + level.random.nextInt(3);
        level.removeBlock(pos, false);
        // the canopy first, up in the air
        BlockPos c = pos.above(height + 1);
        for (int dx = -2; dx <= 2; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -2; dz <= 2; dz++) {
            if (Math.abs(dx) + Math.abs(dz) + Math.abs(dy) > 3) continue;
            BlockPos b = c.offset(dx, dy, dz);
            if (level.getBlockState(b).canBeReplaced()) level.setBlock(b, lv, 3);
        }
        level.setBlock(c, log.defaultBlockState(), 3);
        level.playSound(null, c, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1.0f, 0.6f);
        GROWING.add(new Growth(level, pos, log.defaultBlockState(), lv, height, new int[]{0, 0}));
        return true;
    }

    private static void tick(MinecraftServer server) {
        // trunks growing down from their canopies, a block every half second
        for (int i = GROWING.size() - 1; i >= 0; i--) {
            Growth g = GROWING.get(i);
            if (++g.step()[1] % 10 != 0) continue;
            int k = ++g.step()[0];
            BlockPos b = g.ground().above(g.height() + 1 - k);
            if (k > g.height() + 1 || !g.level().getBlockState(b).canBeReplaced() && !g.level().getBlockState(b).is(g.leaves().getBlock())) {
                GROWING.remove(i);
                continue;
            }
            g.level().setBlock(b, g.log(), 3);
            g.level().playSound(null, b, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.6f, 0.8f);
            if (b.equals(g.ground())) {
                // roots: leaves spreading over the ground
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    if (g.level().getBlockState(b.relative(d)).canBeReplaced()) g.level().setBlock(b.relative(d), g.leaves(), 3);
                }
                GROWING.remove(i);
            }
        }
    }

    // ------------------------------------------------------------------ fountains

    /** A water source on its own (not part of a lake or the sea). */
    private static boolean loneSource(Level level, BlockPos pos) {
        FluidState f = level.getFluidState(pos);
        if (!f.getType().isSame(Fluids.WATER) || !f.isSource()) return false;
        int n = 0;
        for (Direction d : Direction.Plane.HORIZONTAL) if (level.getFluidState(pos.relative(d)).isSourceOfType(Fluids.WATER)) n++;
        return n <= 1;
    }

    /** Should water at {@code pos} flow up? Yes if it's a lone source, or in a column of water rising from one. */
    public static boolean flowsUp(Level level, BlockPos pos) {
        if (!ON || level.isClientSide) return false;
        if (loneSource(level, pos)) return true;
        return fedFromBelow(level, pos) > 0;
    }

    /** How high above its source this column of water is (0 = not in one). Columns stop at 6. */
    public static int fedFromBelow(Level level, BlockPos pos) {
        if (!ON || level.isClientSide) return 0;
        for (int d = 1; d <= 6; d++) {
            BlockPos b = pos.below(d);
            if (loneSource(level, b)) return d < 6 ? d : 0;
            if (!level.getFluidState(b).getType().isSame(Fluids.WATER)) return 0;
        }
        return 0;
    }
}
