package dev.freerun.city;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

import java.util.Set;

/**
 * Keeps nature out of the streets. Two halves:
 * <ul>
 *   <li>during world generation, {@link #suppress} tells the feature mixin to skip trees, grass,
 *       flowers, mushrooms, disks and other vegetation in and around a city (a tree growing in the
 *       chunk next door used to drop its leaves, logs and dirt into a city block already built);</li>
 *   <li>for cities generated before that fix, {@link #clean} sweeps a loaded chunk once: natural
 *       (non-persistent) leaves, the logs of those trees, wild grass and ferns, and stray dirt.</li>
 * </ul>
 * Everything the city places itself uses persistent leaves, so it's left alone.
 */
public final class CityCleaner {
    private CityCleaner() {}

    private static final Set<Feature<?>> VEGETATION = Set.of(
            Feature.TREE, Feature.RANDOM_SELECTOR, Feature.SIMPLE_RANDOM_SELECTOR, Feature.RANDOM_BOOLEAN_SELECTOR,
            Feature.RANDOM_PATCH, Feature.FLOWER, Feature.NO_BONEMEAL_FLOWER, Feature.SIMPLE_BLOCK,
            Feature.HUGE_BROWN_MUSHROOM, Feature.HUGE_RED_MUSHROOM, Feature.BAMBOO, Feature.VINES, Feature.BLOCK_PILE,
            Feature.DISK, Feature.VEGETATION_PATCH, Feature.WATERLOGGED_VEGETATION_PATCH, Feature.ROOT_SYSTEM,
            Feature.BLOCK_COLUMN, Feature.SEAGRASS, Feature.KELP, Feature.SEA_PICKLE, Feature.ICE_SPIKE,
            Feature.ICEBERG, Feature.DESERT_WELL, Feature.LAKE, Feature.FOSSIL, Feature.FREEZE_TOP_LAYER);

    /** Should this decoration feature be skipped in this chunk? Called from worldgen threads. */
    public static boolean suppress(Feature<?> feature, WorldGenLevel level, ChunkGenerator gen, BlockPos origin) {
        if (!VEGETATION.contains(feature)) return false;
        if (feature == Feature.FREEZE_TOP_LAYER) return false;   // snow on roofs is fine
        var server = level.getLevel();
        if (server.dimension() != net.minecraft.world.level.Level.OVERWORLD) return false;
        ChunkPos cp = new ChunkPos(origin);
        int rx = net.minecraft.util.Mth.floorDiv(cp.getMiddleBlockX(), CityWorld.REGION);
        int rz = net.minecraft.util.Mth.floorDiv(cp.getMiddleBlockZ(), CityWorld.REGION);
        BlockPos city = CityWorld.naturalSite(level.getSeed(), rx, rz, gen, server.getChunkSource().randomState(), level);
        if (city == null) return false;
        int pad = CityWorld.MARGIN + 12;   // trees just outside reach in a few blocks
        return cp.getMaxBlockX() >= city.getX() - pad && cp.getMinBlockX() < city.getX() + CityLayout.SIZE + pad
                && cp.getMaxBlockZ() >= city.getZ() - pad && cp.getMinBlockZ() < city.getZ() + CityLayout.SIZE + pad;
    }

    private static boolean wildPlant(BlockState s) {
        return s.is(Blocks.SHORT_GRASS) || s.is(Blocks.TALL_GRASS) || s.is(Blocks.FERN) || s.is(Blocks.LARGE_FERN)
                || s.is(Blocks.DEAD_BUSH) || s.is(Blocks.SWEET_BERRY_BUSH) || s.is(Blocks.BROWN_MUSHROOM) || s.is(Blocks.RED_MUSHROOM)
                || s.is(Blocks.BROWN_MUSHROOM_BLOCK) || s.is(Blocks.RED_MUSHROOM_BLOCK) || s.is(Blocks.MUSHROOM_STEM)
                || s.is(Blocks.VINE) || s.is(Blocks.HANGING_ROOTS) || s.is(Blocks.COCOA) || s.is(Blocks.BEE_NEST)
                || s.is(Blocks.AZALEA) || s.is(Blocks.FLOWERING_AZALEA) || s.is(Blocks.MOSS_CARPET) || s.is(Blocks.PUMPKIN)
                || s.is(Blocks.MELON) || s.is(Blocks.BAMBOO) || s.is(Blocks.SUGAR_CANE) || s.is(Blocks.LILY_PAD)
                || s.is(Blocks.PINK_PETALS);
    }

    private static boolean strayGround(BlockState s) {
        return s.is(Blocks.DIRT) || s.is(Blocks.PODZOL) || s.is(Blocks.ROOTED_DIRT) || s.is(Blocks.MYCELIUM)
                || s.is(Blocks.MOSS_BLOCK) || s.is(Blocks.MUD) || s.is(Blocks.CLAY);
    }

    private static boolean naturalLog(BlockState s) {
        if (!s.is(BlockTags.LOGS)) return false;
        String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
        return !id.startsWith("stripped_");
    }

    /**
     * Sweeps one chunk of the city at {@code city}. With {@code ground} also clears dirt piled
     * above street level and turns podzol / rooted dirt on the lawns back into grass (only done
     * for chunks nobody has built in yet, or when asked for by command).
     */
    public static int clean(ServerLevel level, BlockPos city, ChunkPos cp, boolean ground) {
        int street = city.getY();
        int lo = Math.max(level.getMinBuildHeight(), street - 2), hi = Math.min(level.getMaxBuildHeight() - 1, street + 64);
        BlockState air = Blocks.AIR.defaultBlockState(), grass = Blocks.GRASS_BLOCK.defaultBlockState();
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        LongOpenHashSet leaves = new LongOpenHashSet();
        int n = 0;
        for (int x = cp.getMinBlockX(); x <= cp.getMaxBlockX(); x++) {
            for (int z = cp.getMinBlockZ(); z <= cp.getMaxBlockZ(); z++) {
                if (!CityWorld.inCity(city, x, z, 0)) continue;
                int top = Math.min(hi, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z));
                for (int y = lo; y <= top; y++) {
                    m.set(x, y, z);
                    BlockState s = level.getBlockState(m);
                    if (s.isAir()) continue;
                    if (s.getBlock() instanceof LeavesBlock && !s.getValue(LeavesBlock.PERSISTENT)) {
                        leaves.add(m.asLong());
                        level.setBlock(m, s.getFluidState().isEmpty() ? air : Blocks.WATER.defaultBlockState(), flags);
                        n++;
                    } else if (y > street - 1 && wildPlant(s)) {
                        level.setBlock(m, air, flags);
                        n++;
                    } else if (ground && y > street && strayGround(s)) {
                        level.setBlock(m, air, flags);
                        n++;
                    } else if (ground && y == street && (s.is(Blocks.PODZOL) || s.is(Blocks.ROOTED_DIRT) || s.is(Blocks.MYCELIUM)
                            || s.is(Blocks.MOSS_BLOCK) || s.is(Blocks.DIRT)) && level.getBlockState(m.above()).isAir()) {
                        level.setBlock(m, grass, flags);
                        n++;
                    }
                }
            }
        }
        if (leaves.isEmpty()) return n;
        // the trees those leaves belonged to: logs touching them, and every log joined to those
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        LongOpenHashSet seen = new LongOpenHashSet();
        for (long l : leaves) {
            BlockPos p = BlockPos.of(l);
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                long k = BlockPos.asLong(p.getX() + dx, p.getY() + dy, p.getZ() + dz);
                if (seen.add(k)) queue.enqueue(k);
            }
        }
        BlockPos.MutableBlockPos q = new BlockPos.MutableBlockPos();
        int guard = 0;
        while (!queue.isEmpty() && guard++ < 20000) {
            long k = queue.dequeueLong();
            m.set(k);
            if ((m.getX() >> 4) != cp.x || (m.getZ() >> 4) != cp.z || m.getY() < lo || m.getY() > hi) continue;
            if (!CityWorld.inCity(city, m.getX(), m.getZ(), 0)) continue;
            BlockState s = level.getBlockState(m);
            if (!naturalLog(s) && !(s.is(Blocks.DIRT) && ground)) continue;
            if (s.is(Blocks.DIRT) && m.getY() <= street) continue;
            // a log the city placed has its own (persistent) leaves around it: leave it
            boolean ours = false;
            for (int dx = -1; dx <= 1 && !ours; dx++) for (int dy = -1; dy <= 1 && !ours; dy++) for (int dz = -1; dz <= 1 && !ours; dz++) {
                BlockState o = level.getBlockState(q.set(m.getX() + dx, m.getY() + dy, m.getZ() + dz));
                if (o.getBlock() instanceof LeavesBlock && o.getValue(LeavesBlock.PERSISTENT)) ours = true;
            }
            if (ours) continue;
            level.setBlock(m, air, flags);
            n++;
            int bx = m.getX(), by = m.getY(), bz = m.getZ();
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                long nk = BlockPos.asLong(bx + dx, by + dy, bz + dz);
                if (seen.add(nk)) queue.enqueue(nk);
            }
        }
        return n;
    }
}
