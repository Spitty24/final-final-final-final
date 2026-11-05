package dev.freerun.city;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Where the cities are, and what each one remembers.
 *
 * The overworld is cut into 2,048-block regions; most regions get one city, placed somewhere
 * inside it on reasonably dry, reasonably flat land. Where exactly is worked out from the world
 * seed and the terrain generator alone, so world generation (on any thread) and the game (when a
 * resident needs to know which city they live in) always agree. Cities made with /city are kept
 * in the world's saved data and built a chunk at a time as those chunks load.
 *
 * Each city also keeps its books here: wages paid, rent collected, what people spend in the
 * stores, and crimes stopped. Shop prices follow demand.
 */
public final class CityWorld {
    public static final int REGION = 2048;
    /** Terrain is sloped off for this many blocks around the city so it doesn't end in a cliff. */
    public static final int MARGIN = 14;
    private static final int CHANCE = 70;

    private static final Map<Long, Optional<BlockPos>> SITES = new ConcurrentHashMap<>();
    private static volatile long sitesSeed = Long.MIN_VALUE;
    /** Natural cities confirmed (or not) to actually exist in the world, by origin. */
    private static final Map<BlockPos, Boolean> REAL = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Long> REAL_CHECKED = new ConcurrentHashMap<>();

    private CityWorld() {}

    // ------------------------------------------------------------------ natural sites

    private static long mix(long a, long b, long c) {
        long h = a * 0x9E3779B97F4A7C15L ^ b * 0xC2B2AE3D27D4EB4FL ^ c * 0x165667B19E3779F9L;
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return h;
    }

    /** The natural city in region (rx, rz), if there is one; origin is its north-west corner at street level. */
    public static BlockPos naturalSite(long seed, int rx, int rz, ChunkGenerator gen, RandomState rs, LevelHeightAccessor heights) {
        if (sitesSeed != seed) {
            synchronized (SITES) {
                if (sitesSeed != seed) {
                    SITES.clear();
                    REAL.clear();
                    REAL_CHECKED.clear();
                    sitesSeed = seed;
                }
            }
        }
        long key = ((long) rx << 32) ^ (rz & 0xffffffffL);
        return SITES.computeIfAbsent(key, k -> findSite(seed, rx, rz, gen, rs, heights)).orElse(null);
    }

    private static Optional<BlockPos> findSite(long seed, int rx, int rz, ChunkGenerator gen, RandomState rs, LevelHeightAccessor heights) {
        long h = mix(seed, rx, rz);
        if (Math.floorMod(h, 100L) >= CHANCE) return Optional.empty();
        int span = REGION - CityLayout.SIZE - 2 * (MARGIN + 16);
        for (int attempt = 0; attempt < 3; attempt++) {
            long a = mix(h, attempt, 7);
            int x0 = rx * REGION + MARGIN + 16 + (int) Math.floorMod(a, (long) span);
            int z0 = rz * REGION + MARGIN + 16 + (int) Math.floorMod(a >>> 24, (long) span);
            int n = 7;
            int[] ground = new int[n * n];
            int wet = 0;
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    int x = x0 + i * (CityLayout.SIZE - 1) / (n - 1), z = z0 + j * (CityLayout.SIZE - 1) / (n - 1);
                    int g = gen.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, heights, rs);
                    int s = gen.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, heights, rs);
                    if (s > g + 1) wet++;
                    ground[i * n + j] = g;
                }
            }
            int[] sorted = ground.clone();
            Arrays.sort(sorted);
            int median = sorted[sorted.length / 2];
            int spread = sorted[(int) (sorted.length * 0.9)] - sorted[(int) (sorted.length * 0.1)];
            if (wet > 6 || spread > 40) continue;
            if (median <= heights.getMinBuildHeight() + 10 || median > heights.getMaxBuildHeight() - 90) continue;
            return Optional.of(new BlockPos(x0, median - 1, z0));
        }
        return Optional.empty();
    }

    public static boolean inCity(BlockPos origin, double x, double z, int margin) {
        return x >= origin.getX() - margin && z >= origin.getZ() - margin
                && x < origin.getX() + CityLayout.SIZE + margin && z < origin.getZ() + CityLayout.SIZE + margin;
    }

    private static BlockPos naturalAt(ServerLevel level, double x, double z, int margin) {
        if (level.dimension() != Level.OVERWORLD) return null;
        int rx = Mth.floor(x / REGION), rz = Mth.floor(z / REGION);
        BlockPos o = naturalSite(level.getSeed(), rx, rz, level.getChunkSource().getGenerator(), level.getChunkSource().randomState(), level);
        return o != null && inCity(o, x, z, margin) ? o : null;
    }

    /**
     * Is the natural city at {@code origin} really there? Superflat worlds without features, and
     * worlds explored before the mod, have the site but not the buildings. Checked by looking at
     * the street under the nearest intersection, once that chunk is loaded.
     */
    private static boolean real(ServerLevel level, BlockPos origin, double x, double z) {
        Boolean known = REAL.get(origin);
        if (known != null && known) return true;
        long now = level.getGameTime();
        Long last = REAL_CHECKED.get(origin);
        if (known != null && last != null && now - last < 1200) return false;
        int i = nearestLine(x - origin.getX()), j = nearestLine(z - origin.getZ());
        BlockPos key = CityLayout.intersectionKey(origin, i, j);
        if (!level.isLoaded(key)) return known != null && known;
        boolean yes = level.getBlockState(key).is(Blocks.BLACK_CONCRETE);
        REAL.put(origin, yes);
        REAL_CHECKED.put(origin, now);
        return yes;
    }

    private static int nearestLine(double local) {
        int best = 0;
        double bestD = Double.MAX_VALUE;
        for (int k = 0; k < CityLayout.XS.length; k++) {
            double d = Math.abs(CityLayout.XS[k] + CityLayout.ROAD / 2.0 - local);
            if (d < bestD) {
                bestD = d;
                best = k;
            }
        }
        return best;
    }

    /** Origin of the city containing (x, z), built by world generation or by /city; null outside cities. */
    public static BlockPos cityAt(ServerLevel level, double x, double z) {
        if (level.dimension() != Level.OVERWORLD) return null;
        for (Manual m : data(level).manual) if (inCity(m.origin, x, z, 0)) return m.origin;
        BlockPos o = naturalAt(level, x, z, 0);
        return o != null && real(level, o, x, z) ? o : null;
    }

    public static BlockPos cityAt(ServerLevel level, BlockPos pos) {
        return cityAt(level, pos.getX() + 0.5, pos.getZ() + 0.5);
    }

    /** Nearest city site (natural or manual) to a point, searching a few regions out. */
    public static BlockPos nearestCity(ServerLevel level, BlockPos from, int regions) {
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (Manual m : data(level).manual) {
            double d = centre(m.origin).distSqr(from);
            if (d < bestD) {
                bestD = d;
                best = m.origin;
            }
        }
        if (level.dimension() == Level.OVERWORLD) {
            int rx = Math.floorDiv(from.getX(), REGION), rz = Math.floorDiv(from.getZ(), REGION);
            for (int dx = -regions; dx <= regions; dx++) {
                for (int dz = -regions; dz <= regions; dz++) {
                    BlockPos o = naturalSite(level.getSeed(), rx + dx, rz + dz, level.getChunkSource().getGenerator(),
                            level.getChunkSource().randomState(), level);
                    if (o == null) continue;
                    double d = centre(o).distSqr(from);
                    if (d < bestD) {
                        bestD = d;
                        best = o;
                    }
                }
            }
        }
        return best;
    }

    public static BlockPos centre(BlockPos origin) {
        return origin.offset(CityLayout.HALF, 1, CityLayout.HALF);
    }

    public static AABB box(BlockPos origin) {
        return new AABB(origin.getX(), origin.getY() - 8, origin.getZ(),
                origin.getX() + CityLayout.SIZE, origin.getY() + 64, origin.getZ() + CityLayout.SIZE);
    }

    // ------------------------------------------------------------------ the books

    public static final class Books {
        long day = -1;
        int wages, sales, rent, salesYesterday = -1, crimesStopped, arrests, crimesTotal;

        void roll(long d) {
            if (day == d) return;
            if (day >= 0) salesYesterday = sales;
            day = d;
            wages = sales = rent = 0;
        }

        /** Price multiplier for player purchases in this city's shops (0.8 to 1.3). */
        public double priceMultiplier() {
            int s = salesYesterday >= 0 ? (salesYesterday + sales) / 2 : sales;
            return Mth.clamp(0.8 + s / 500.0 * 0.35, 0.8, 1.3);
        }

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putLong("Day", day);
            t.putInt("Wages", wages);
            t.putInt("Sales", sales);
            t.putInt("Rent", rent);
            t.putInt("SalesY", salesYesterday);
            t.putInt("Stopped", crimesStopped);
            t.putInt("Arrests", arrests);
            t.putInt("Crimes", crimesTotal);
            return t;
        }

        static Books load(CompoundTag t) {
            Books b = new Books();
            b.day = t.getLong("Day");
            b.wages = t.getInt("Wages");
            b.sales = t.getInt("Sales");
            b.rent = t.getInt("Rent");
            b.salesYesterday = t.getInt("SalesY");
            b.crimesStopped = t.getInt("Stopped");
            b.arrests = t.getInt("Arrests");
            b.crimesTotal = t.getInt("Crimes");
            return b;
        }
    }

    public static Books books(ServerLevel level, BlockPos origin) {
        Data d = data(level);
        Books b = d.books.computeIfAbsent(origin.asLong(), k -> new Books());
        b.roll(level.getDayTime() / 24000L);
        d.setDirty();
        return b;
    }

    private static Books booksAt(ServerLevel level, BlockPos pos) {
        BlockPos o = cityAt(level, pos);
        return o == null ? null : books(level, o);
    }

    public static void recordWages(ServerLevel level, BlockPos pos, int n) {
        Books b = booksAt(level, pos);
        if (b != null) b.wages += n;
    }

    public static void recordSale(ServerLevel level, BlockPos pos, int n) {
        Books b = booksAt(level, pos);
        if (b != null) b.sales += n;
    }

    public static void recordRent(ServerLevel level, BlockPos pos, int n) {
        Books b = booksAt(level, pos);
        if (b != null) b.rent += n;
    }

    public static void recordCrime(ServerLevel level, BlockPos pos, boolean stopped, boolean arrest) {
        Books b = booksAt(level, pos);
        if (b == null) return;
        b.crimesTotal++;
        if (stopped) b.crimesStopped++;
        if (arrest) b.arrests++;
    }

    public static int pricePercent(ServerLevel level, BlockPos pos) {
        Books b = booksAt(level, pos);
        return b == null ? 100 : (int) Math.round(b.priceMultiplier() * 100);
    }

    /** What the phone's City app shows: a census of who's about, plus the books. */
    public static CompoundTag stats(ServerLevel level, BlockPos near) {
        CompoundTag t = new CompoundTag();
        BlockPos o = cityAt(level, near);
        if (o == null) {
            BlockPos n = nearestCity(level, near, 1);
            if (n != null) {
                t.putInt("NX", centre(n).getX());
                t.putInt("NZ", centre(n).getZ());
            }
            return t;
        }
        AABB box = box(o);
        List<CitizenEntity> people = level.getEntitiesOfClass(CitizenEntity.class, box);
        t.putInt("Population", people.size());
        t.putInt("Employed", (int) people.stream().filter(c -> c.job().wage > 0).count());
        t.putInt("AvgMoney", people.isEmpty() ? 0 : (int) people.stream().mapToInt(CitizenEntity::money).average().orElse(0));
        t.putInt("Cars", level.getEntitiesOfClass(dev.freerun.vehicle.CarEntity.class, box, c -> c.npcDriven()).size());
        t.putInt("Police", level.getEntitiesOfClass(PoliceOfficerEntity.class, box).size());
        Books b = books(level, o);
        t.putInt("Wages", b.wages);
        t.putInt("Sales", b.sales);
        t.putInt("Rent", b.rent);
        t.putInt("SalesYesterday", Math.max(0, b.salesYesterday));
        t.putInt("PriceIndex", (int) Math.round(b.priceMultiplier() * 100));
        t.putInt("Crimes", b.crimesTotal);
        t.putInt("Stopped", b.crimesStopped);
        t.putInt("Arrests", b.arrests);
        t.putInt("OX", o.getX());
        t.putInt("OY", o.getY());
        t.putInt("OZ", o.getZ());
        return t;
    }

    // ------------------------------------------------------------------ saved data

    static final class Manual {
        final BlockPos origin;
        final BitSet built;

        Manual(BlockPos origin, BitSet built) {
            this.origin = origin;
            this.built = built;
        }

        int chunks() {
            return (CityLayout.SIZE + 2 * MARGIN) / 16 + 2;
        }

        int index(ChunkPos cp) {
            int cx = cp.x - ((origin.getX() - MARGIN) >> 4), cz = cp.z - ((origin.getZ() - MARGIN) >> 4);
            int n = chunks();
            return cx < 0 || cz < 0 || cx >= n || cz >= n ? -1 : cz * n + cx;
        }
    }

    public static final class Data extends SavedData {
        final List<Manual> manual = new ArrayList<>();
        final Map<Long, Books> books = new HashMap<>();

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag list = new ListTag();
            for (Manual m : manual) {
                CompoundTag c = new CompoundTag();
                c.putLong("Origin", m.origin.asLong());
                c.putLongArray("Built", m.built.toLongArray());
                list.add(c);
            }
            tag.put("Manual", list);
            ListTag bl = new ListTag();
            for (Map.Entry<Long, Books> e : books.entrySet()) {
                CompoundTag c = e.getValue().save();
                c.putLong("City", e.getKey());
                bl.add(c);
            }
            tag.put("Books", bl);
            return tag;
        }

        static Data load(CompoundTag tag, HolderLookup.Provider registries) {
            Data d = new Data();
            for (Tag t : tag.getList("Manual", Tag.TAG_COMPOUND)) {
                CompoundTag c = (CompoundTag) t;
                d.manual.add(new Manual(BlockPos.of(c.getLong("Origin")), BitSet.valueOf(c.getLongArray("Built"))));
            }
            for (Tag t : tag.getList("Books", Tag.TAG_COMPOUND)) {
                CompoundTag c = (CompoundTag) t;
                d.books.put(c.getLong("City"), Books.load(c));
            }
            return d;
        }
    }

    private static final SavedData.Factory<Data> FACTORY = new SavedData.Factory<>(Data::new, Data::load,
            net.minecraft.util.datafix.DataFixTypes.SAVED_DATA_COMMAND_STORAGE);   // a no-op fixer; our tags carry the current data version

    public static Data data(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(FACTORY, "freerun_cities");
    }

    // ------------------------------------------------------------------ /city: building on load

    private static final LinkedHashSet<Long> QUEUE = new LinkedHashSet<>();

    /** Registers a city at {@code origin} and starts building whatever of it is already loaded. */
    public static void addManual(ServerLevel level, BlockPos origin) {
        Data d = data(level);
        Manual m = new Manual(origin.immutable(), new BitSet());
        d.manual.add(m);
        d.setDirty();
        int x0 = (origin.getX() - MARGIN) >> 4, z0 = (origin.getZ() - MARGIN) >> 4;
        for (int cx = x0; cx < x0 + m.chunks(); cx++) {
            for (int cz = z0; cz < z0 + m.chunks(); cz++) {
                if (level.getChunkSource().getChunkNow(cx, cz) != null) QUEUE.add(ChunkPos.asLong(cx, cz));
            }
        }
    }

    public static boolean overlapsManual(ServerLevel level, BlockPos origin) {
        for (Manual m : data(level).manual) {
            if (Math.abs(m.origin.getX() - origin.getX()) < CityLayout.SIZE + 2 * MARGIN
                    && Math.abs(m.origin.getZ() - origin.getZ()) < CityLayout.SIZE + 2 * MARGIN) return true;
        }
        return false;
    }

    /** Natural city whose footprint (with margin) overlaps a square at {@code origin}, if any. */
    public static BlockPos overlapsNatural(ServerLevel level, BlockPos origin) {
        for (int dx = 0; dx <= 1; dx++) for (int dz = 0; dz <= 1; dz++) {
            double x = origin.getX() + dx * CityLayout.SIZE, z = origin.getZ() + dz * CityLayout.SIZE;
            BlockPos n = naturalAt(level, x, z, MARGIN * 2);
            if (n != null) return n;
        }
        BlockPos n = naturalAt(level, origin.getX() + CityLayout.HALF, origin.getZ() + CityLayout.HALF, CityLayout.HALF + MARGIN * 2);
        return n;
    }

    public static int queued() {
        return QUEUE.size();
    }

    public static void register() {
        ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            if (world.dimension() != Level.OVERWORLD) return;
            Data d = data(world);
            if (d.manual.isEmpty()) return;
            ChunkPos cp = chunk.getPos();
            for (Manual m : d.manual) {
                int idx = m.index(cp);
                if (idx >= 0 && !m.built.get(idx)) QUEUE.add(cp.toLong());
            }
        });
        ServerTickEvents.END_WORLD_TICK.register(level -> {
            if (level.dimension() != Level.OVERWORLD || QUEUE.isEmpty()) return;
            for (int k = 0; k < 2 && !QUEUE.isEmpty(); k++) buildNext(level);
        });
    }

    private static void buildNext(ServerLevel level) {
        // nearest queued chunk to any player first
        long pick = 0;
        double best = Double.MAX_VALUE;
        boolean found = false;
        for (long l : QUEUE) {
            int cx = ChunkPos.getX(l), cz = ChunkPos.getZ(l);
            double d = Double.MAX_VALUE / 4;
            for (ServerPlayer p : level.players()) {
                double dx = (cx << 4) + 8 - p.getX(), dz = (cz << 4) + 8 - p.getZ();
                d = Math.min(d, dx * dx + dz * dz);
            }
            if (!found || d < best) {
                best = d;
                pick = l;
                found = true;
            }
        }
        QUEUE.remove(pick);
        ChunkPos cp = new ChunkPos(pick);
        if (level.getChunkSource().getChunkNow(cp.x, cp.z) == null) return;   // unloaded since; it'll come back on load
        Data d = data(level);
        for (Manual m : d.manual) {
            int idx = m.index(cp);
            if (idx < 0 || m.built.get(idx)) continue;
            CityPlan.buildChunk(level, m.origin, cp, level.getChunkSource().getGenerator(), level.getRandom());
            m.built.set(idx);
            d.setDirty();
        }
    }
}
