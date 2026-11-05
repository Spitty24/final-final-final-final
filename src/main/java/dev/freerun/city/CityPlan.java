package dev.freerun.city;

import dev.freerun.economy.Shops;
import dev.freerun.world.CityAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Every piece of a city with its north-west corner (street level) at {@code city}, and the code
 * that builds one 16 x 16 chunk of it: level the terrain (cut hills down, fill hollows and rivers,
 * slope the edges off), then let each piece that touches the chunk draw its part.
 */
public final class CityPlan {
    private record Index(List<StructurePiece> all, Map<Long, List<StructurePiece>> byChunk) {}

    private static final Map<BlockPos, Index> CACHE = new ConcurrentHashMap<>();

    private CityPlan() {}

    private static Index index(BlockPos city) {
        Index idx = CACHE.get(city);
        if (idx != null) return idx;
        if (CACHE.size() > 12) CACHE.clear();
        return CACHE.computeIfAbsent(city.immutable(), c -> {
            List<StructurePiece> all = pieces(c);
            Map<Long, List<StructurePiece>> by = new HashMap<>();
            for (StructurePiece p : all) {
                BoundingBox b = p.getBoundingBox();
                for (int cx = b.minX() >> 4; cx <= b.maxX() >> 4; cx++) {
                    for (int cz = b.minZ() >> 4; cz <= b.maxZ() >> 4; cz++) {
                        by.computeIfAbsent(ChunkPos.asLong(cx, cz), k -> new ArrayList<>()).add(p);
                    }
                }
            }
            return new Index(Collections.unmodifiableList(all), by);
        });
    }

    public static List<StructurePiece> pieces(BlockPos city) {
        List<StructurePiece> out = new ArrayList<>();
        // streets first, then the blocks
        for (int j = 0; j < CityLayout.ZS.length; j++) for (int i = 0; i < CityLayout.XS.length; i++) out.add(new IntersectionPiece(city, i, j));
        for (int line = 0; line < CityLayout.ZS.length; line++) for (int seg = 0; seg < CityLayout.cols(); seg++) out.add(new RoadPiece(city, true, line, seg));
        for (int line = 0; line < CityLayout.XS.length; line++) for (int seg = 0; seg < CityLayout.rows(); seg++) out.add(new RoadPiece(city, false, line, seg));
        for (int r = 0; r < CityLayout.rows(); r++) {
            for (int c = 0; c < CityLayout.cols(); c++) {
                List<CityLayout.Lot> lots = CityLayout.lotsIn(c, r);
                int lx = CityLayout.lotX(c), lz = CityLayout.lotZ(r), w = CityLayout.lotW(c), d = CityLayout.lotD(r);
                if (lots.size() != 1 || lots.get(0).w() != w || lots.get(0).d() != d) {
                    // the gaps between lots: service lanes joining the parking lots, lawns, or paving
                    out.add(new LotPiece(city, lx, lz, w, d, 6, baseKind(lots)));
                }
                for (CityLayout.Lot lot : lots) lot(out, city, lot);
            }
        }
        return out;
    }

    private static int baseKind(List<CityLayout.Lot> lots) {
        boolean commercial = false, hood = false, office = false;
        for (CityLayout.Lot l : lots) {
            switch (l.district()) {
                case SUBURB, PARK -> {}
                case HOOD -> hood = true;
                case OFFICE, CITY_HALL -> office = true;
                default -> commercial = true;
            }
        }
        return commercial ? LotPiece.PAVED : hood ? LotPiece.CONCRETE : office ? LotPiece.PLAZA : LotPiece.GRASS;
    }

    private static void lot(List<StructurePiece> out, BlockPos city, CityLayout.Lot l) {
        int lx = l.lx(), lz = l.lz(), w = l.w(), d = l.d();
        BlockPos at = city.offset(lx, 0, lz);
        switch (l.district()) {
            case SUBURB -> out.add(new SuburbPiece(city, lx, lz, w, d));
            case OFFICE -> out.add(new OfficePiece(city, lx, lz, w, d));
            case PARK -> out.add(new ParkPiece(city, lx, lz, w, d, ParkPiece.PARK_ONLY));
            case CITY_HALL -> out.add(new ParkPiece(city, lx, lz, w, d, ParkPiece.HALL_AND_PARK));
            case HOOD -> out.add(new HoodPiece(city, lx, lz, w, d, l.variant() % 2));
            case PARKING -> out.add(new LotPiece(city, lx, lz, w, d, 8, LotPiece.PARKING));
            case ROADHOUSE -> {
                out.add(new LotPiece(city, lx, lz, w, 25, 16, LotPiece.GRASS));
                out.add(new LotPiece(city, lx, lz + 25, w, d - 25, 8, LotPiece.PARKING));
                out.add(CityAccess.roadhouse(at.offset(6, 0, 2)));
            }
            case GENERAL_STORE -> {
                out.add(new LotPiece(city, lx, lz, w, 25, 16, LotPiece.GRASS));
                out.add(new LotPiece(city, lx, lz + 25, w, d - 25, 8, LotPiece.PARKING));
                out.add(CityAccess.generalStore(at.offset(10, 0, 3)));
            }
            case DOUBLE_QUICK -> {
                out.add(new LotPiece(city, lx, lz, w, 27, 16, LotPiece.GRASS));
                out.add(new LotPiece(city, lx, lz + 27, w, d - 27, 8, LotPiece.PARKING));
                out.add(CityAccess.doubleQuick(at.offset(6, 0, 2)));
            }
            case FACTORY -> {
                out.add(new LotPiece(city, lx, lz, w, d, 20, LotPiece.GRASS));
                out.addAll(CityAccess.factory(at.offset(20, 0, 3 + CityAccess.LOT_DEPTH)));
            }
            case MEGAMART -> out.add(new MegaMartPiece(city, lx, lz, w, d));
            case DEPARTMENT -> out.add(new DepartmentStorePiece(city, lx, lz, w, d));
            case STRIP_MALL -> out.add(new StripMallPiece(city, lx, lz, w, d, l.variant()));
            case BURGER -> out.add(new FastFoodPiece(city, lx, lz, w, d, Shops.Shop.BURGER));
            case CHICKEN -> out.add(new FastFoodPiece(city, lx, lz, w, d, Shops.Shop.CHICKEN));
            case DESSERT -> out.add(new DessertShopPiece(city, lx, lz, w, d));
            case POLICE -> out.add(new PoliceStationPiece(city, lx, lz, w, d));
            case ARCADE -> out.add(new FunPiece(city, lx, lz, w, d, FunPiece.ARCADE));
            case BAR -> out.add(new FunPiece(city, lx, lz, w, d, FunPiece.BAR));
            case FURNITURE -> out.add(new FunPiece(city, lx, lz, w, d, FunPiece.FURNITURE));
        }
    }

    // ------------------------------------------------------------------ building a chunk

    /** Builds whatever part of the city at {@code city} lies in chunk {@code cp}. */
    public static void buildChunk(WorldGenLevel level, BlockPos city, ChunkPos cp, ChunkGenerator generator, RandomSource random) {
        terrain(level, city, cp);
        BoundingBox box = new BoundingBox(cp.getMinBlockX(), level.getMinBuildHeight(), cp.getMinBlockZ(),
                cp.getMaxBlockX(), level.getMaxBuildHeight() - 1, cp.getMaxBlockZ());
        List<StructurePiece> here = index(city).byChunk().get(cp.toLong());
        if (here == null) return;
        var structures = level.getLevel().structureManager();
        for (StructurePiece p : here) {
            synchronized (p) {   // a piece spans chunks that may generate on different threads at once
                p.postProcess(level, structures, generator, random, box, cp, city);
            }
        }
    }

    /**
     * Levels the ground: inside the city everything above the street is cleared and everything
     * below it made solid; around the edge the cut and the fill both taper off one block per block.
     */
    private static void terrain(WorldGenLevel level, BlockPos city, ChunkPos cp) {
        int street = city.getY();
        int max = Math.min(level.getMaxBuildHeight() - 1, street + 160);
        int min = Math.max(level.getMinBuildHeight() + 1, street - 48);
        BlockState air = Blocks.AIR.defaultBlockState(), stone = Blocks.STONE.defaultBlockState(),
                dirt = Blocks.DIRT.defaultBlockState(), grass = Blocks.GRASS_BLOCK.defaultBlockState();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = cp.getMinBlockX(); x <= cp.getMaxBlockX(); x++) {
            for (int z = cp.getMinBlockZ(); z <= cp.getMaxBlockZ(); z++) {
                int lx = x - city.getX(), lz = z - city.getZ();
                int out = Math.max(Math.max(-lx, lx - (CityLayout.SIZE - 1)), Math.max(-lz, lz - (CityLayout.SIZE - 1)));
                out = Math.max(0, out);
                if (out > CityWorld.MARGIN) continue;
                int ceiling = street + out, floor = street - out;
                int top = Math.min(max, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) + 1);
                boolean cut = false;
                for (int y = top; y > ceiling; y--) {
                    m.set(x, y, z);
                    if (!level.getBlockState(m).isAir()) {
                        level.setBlock(m, air, Block.UPDATE_CLIENTS);
                        cut = true;
                    }
                }
                boolean filled = false;
                for (int y = out == 0 ? street : floor; y >= min; y--) {
                    m.set(x, y, z);
                    BlockState st = level.getBlockState(m);
                    if (!st.isAir() && !st.liquid() && !st.canBeReplaced()) break;
                    level.setBlock(m, y >= floor - 2 ? dirt : stone, Block.UPDATE_CLIENTS);
                    filled = true;
                }
                if (out > 0 && (cut || filled)) {
                    // give the new slope a grassy top
                    for (int y = ceiling; y >= floor; y--) {
                        m.set(x, y, z);
                        BlockState st = level.getBlockState(m);
                        if (st.isAir()) continue;
                        if (st.is(Blocks.DIRT) || st.is(Blocks.STONE) || st.is(Blocks.GRAVEL) || st.is(Blocks.SAND)) level.setBlock(m, grass, Block.UPDATE_CLIENTS);
                        break;
                    }
                }
            }
        }
    }
}
