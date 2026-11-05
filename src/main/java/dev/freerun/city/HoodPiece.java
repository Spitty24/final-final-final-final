package dev.freerun.city;

import dev.freerun.economy.EconomyRegistry;
import dev.freerun.food.FoodRegistry;
import dev.freerun.gang.Crew;
import dev.freerun.gang.GangTurfEntity;
import dev.freerun.gang.GangsterEntity;
import dev.freerun.mobs.MoreMobs;
import dev.freerun.mobs.RoadhouseNpcEntity;
import dev.freerun.mobs.SoldierRegistry;
import dev.freerun.world.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * The rough side of town (two 38 x 38 halves): old four-storey brick apartment buildings with fire
 * escapes, a corner store, a fenced basketball court, and a vacant lot where the Rust Rats and the
 * Chrome Kings hang out (and, every few minutes, shoot it out). Ordinary residents live in the
 * apartments like anywhere else.
 */
public class HoodPiece extends CityPieceBase {
    /** 0: apartments, the corner store and the court. 1: apartments and the vacant lot. */
    private final int half;

    public HoodPiece(BlockPos city, int lx, int lz, int w, int d, int half) {
        super(CityRegistry.HOOD_PIECE, city, lx, lz, w, 22, d);
        this.half = half;
    }

    public HoodPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(CityRegistry.HOOD_PIECE, tag);
        half = tag.getInt("Half");
    }

    @Override
    protected void save(CompoundTag tag) {
        tag.putInt("Half", half);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        ground(level, box, Blocks.GRAY_CONCRETE.defaultBlockState(), 22);
        for (int x = 0; x < w; x++) for (int z = 0; z < d; z++) {
            if ((x * 7 + z * 13) % 11 == 0) put(level, box, Blocks.CRACKED_STONE_BRICKS, x, 0, z);
        }
        int seed = Math.floorMod(ox * 31 + oz * 7, 97);
        if (half == 0) {
            apartment(level, box, 1, 2, seed, Blocks.BRICKS.defaultBlockState());
            apartment(level, box, 20, 2, seed + 1, Blocks.RED_TERRACOTTA.defaultBlockState());
            cornerStore(level, box, 1, 22);
            court(level, box, 16, 21);
        } else {
            apartment(level, box, 1, 1, seed + 2, Blocks.BROWN_TERRACOTTA.defaultBlockState());
            apartment(level, box, 20, 1, seed + 3, Blocks.BRICKS.defaultBlockState());
            if (Math.floorMod(ox * 7 + oz * 13, 3) == 0) vacantLot(level, box, 1, 18);
            else garden(level, box, 1, 18);
        }
    }

    // ------------------------------------------------------------------ apartments

    private void apartment(WorldGenLevel level, BoundingBox box, int x0, int z0, int seed, BlockState brick) {
        int x1 = x0 + 16, z1 = z0 + 15;
        int floors = 3 + seed % 2;
        for (int f = 0; f < floors; f++) {
            int y0 = f * 4;
            fill(level, box, Blocks.SPRUCE_PLANKS.defaultBlockState(), x0, y0, z0, x1, y0, z1);
            for (int y = y0 + 1; y <= y0 + 3; y++) {
                for (int x = x0; x <= x1; x++) {
                    boolean win = y == y0 + 2 && (x - x0) % 4 == 2;
                    put(level, box, win ? Blocks.GLASS.defaultBlockState() : brick, x, y, z0);
                    put(level, box, win ? Blocks.GLASS.defaultBlockState() : brick, x, y, z1);
                }
                for (int z = z0; z <= z1; z++) {
                    boolean win = y == y0 + 2 && (z - z0) % 4 == 2;
                    put(level, box, win ? Blocks.GLASS.defaultBlockState() : brick, x0, y, z);
                    put(level, box, win ? Blocks.GLASS.defaultBlockState() : brick, x1, y, z);
                }
            }
            // each flat: a bed, a table, a counter, a TV
            for (int side = 0; side < 2; side++) {
                int bx = side == 0 ? x0 + 1 : x0 + 9;
                put(level, box, Blocks.BLUE_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.SOUTH).setValue(BedBlock.PART, BedPart.FOOT), bx + 1, y0 + 1, z1 - 3);
                put(level, box, Blocks.BLUE_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.SOUTH).setValue(BedBlock.PART, BedPart.HEAD), bx + 1, y0 + 1, z1 - 2);
                put(level, box, BlockRegistry.TABLE, bx + 4, y0 + 1, z0 + 7);
                put(level, box, facing(BlockRegistry.CHAIR, Direction.WEST), bx + 5, y0 + 1, z0 + 7);
                put(level, box, facing(FoodRegistry.COUNTER, Direction.SOUTH), bx + 2, y0 + 1, z0 + 1);
                put(level, box, facing(FoodRegistry.MICROWAVE, Direction.SOUTH), bx + 2, y0 + 2, z0 + 1);
                put(level, box, facing(EconomyRegistry.TV, Direction.NORTH).setValue(dev.freerun.economy.StoreBlocks.Tv.ON, (seed + side + f) % 2 == 0), bx + 5, y0 + 1, z1 - 1);
                put(level, box, Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true), bx + 3, y0 + 3, z0 + 8);
            }
        }
        int top = floors * 4;
        fill(level, box, Blocks.SMOOTH_STONE.defaultBlockState(), x0, top, z0, x1, top, z1);
        fill(level, box, brick, x0, top + 1, z0, x1, top + 1, z0);
        // front door, and a ladder up through every floor at the back
        put(level, box, Blocks.AIR, x0 + 8, 1, z0);
        put(level, box, Blocks.AIR, x0 + 8, 2, z0);
        for (int f = 0; f < floors; f++) {
            for (int y = f * 4 + 1; y <= f * 4 + 4; y++) {
                put(level, box, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH), x0 + 8, y, z1 - 1);
                if (y == f * 4 + 4) put(level, box, Blocks.AIR, x0 + 8, y, z1 - 1);
            }
            put(level, box, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH), x0 + 8, f * 4 + 4, z1 - 1);
        }
        put(level, box, Blocks.WHITE_TERRACOTTA, x0 + 8, 1, z1);   // ladder backing
        // fire escape up the outside
        BlockState rail = Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true);
        for (int f = 1; f < floors; f++) {
            int y = f * 4;
            fill(level, box, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), x0 - 1, y, z0 + 5, x0 - 1, y, z0 + 9);
            fill(level, box, rail, x0 - 1, y + 1, z0 + 5, x0 - 1, y + 1, z0 + 9);
        }
        for (int y = 1; y <= (floors - 1) * 4; y++) {
            put(level, box, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.WEST), x0 - 1, y, z0 + 7);
        }
        sign(level, box, x0 + 10, 3, z0 - 1, Direction.NORTH, "", (seed % 2 == 0 ? "MAGNOLIA" : "CYPRESS") + " ARMS", "Apartments", "");
    }

    // ------------------------------------------------------------------ corner store

    private void cornerStore(WorldGenLevel level, BoundingBox box, int x0, int z0) {
        int x1 = x0 + 12, z1 = z0 + 13;
        fill(level, box, Blocks.WHITE_CONCRETE.defaultBlockState(), x0, 0, z0, x1, 0, z1);
        walls(level, box, Blocks.YELLOW_TERRACOTTA.defaultBlockState(), x0, 1, z0, x1, 4, z1);
        fill(level, box, Blocks.GLASS.defaultBlockState(), x0 + 1, 1, z0, x0 + 4, 2, z0);
        fill(level, box, Blocks.GLASS.defaultBlockState(), x0 + 8, 1, z0, x1 - 1, 2, z0);
        fill(level, box, Blocks.AIR.defaultBlockState(), x0 + 6, 1, z0, x0 + 6, 2, z0);
        fill(level, box, Blocks.SMOOTH_STONE.defaultBlockState(), x0, 5, z0, x1, 5, z1);
        put(level, box, Blocks.SEA_LANTERN, x0 + 6, 5, z0 + 6);
        sign(level, box, x0 + 5, 3, z0 - 1, Direction.NORTH, "", "§lCORNER MART", "Snacks · Drinks", "Lotto");
        for (int x = x0 + 2; x <= x0 + 6; x++) put(level, box, facing(FoodRegistry.COUNTER, Direction.SOUTH), x, 1, z0 + 9);
        for (int z = z0 + 2; z <= z0 + 7; z++) {
            put(level, box, facing(FoodRegistry.STORE_SHELF, Direction.EAST), x0 + 9, 1, z);
            put(level, box, facing(FoodRegistry.COOLER, Direction.WEST), x1 - 1, 1, z);
            put(level, box, facing(FoodRegistry.COOLER, Direction.WEST), x1 - 1, 2, z);
        }
        put(level, box, facing(EconomyRegistry.ATM, Direction.EAST), x0 + 1, 1, z0 + 3);
        BlockPos p = at(x0 + 4, 1, z0 + 11);
        if (box.isInside(p)) {
            RoadhouseNpcEntity clerk = new RoadhouseNpcEntity(SoldierRegistry.ROADHOUSE_NPC, server(level));
            clerk.setRole(RoadhouseNpcEntity.Role.CLERK, 0);
            clerk.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(EconomyRegistry.SCRATCH_OFF));
            clerk.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
            clerk.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 180.0f, 0.0f);
            clerk.setYHeadRot(180.0f);
            clerk.setYBodyRot(180.0f);
            level.addFreshEntity(clerk);
        }
    }

    // ------------------------------------------------------------------ basketball court

    private void court(WorldGenLevel level, BoundingBox box, int x0, int z0) {
        int x1 = x0 + 20, z1 = z0 + 16;
        fill(level, box, Blocks.CYAN_TERRACOTTA.defaultBlockState(), x0, 0, z0, x1, 0, z1);
        BlockState line = Blocks.WHITE_CONCRETE.defaultBlockState();
        fill(level, box, line, x0 + 1, 0, z0 + 1, x1 - 1, 0, z0 + 1);
        fill(level, box, line, x0 + 1, 0, z1 - 1, x1 - 1, 0, z1 - 1);
        fill(level, box, line, x0 + 1, 0, z0 + 1, x0 + 1, 0, z1 - 1);
        fill(level, box, line, x1 - 1, 0, z0 + 1, x1 - 1, 0, z1 - 1);
        fill(level, box, line, (x0 + x1) / 2, 0, z0 + 1, (x0 + x1) / 2, 0, z1 - 1);
        int cz = (z0 + z1) / 2;
        for (int end = 0; end < 2; end++) {
            int x = end == 0 ? x0 + 2 : x1 - 2;
            fill(level, box, Blocks.IRON_BARS.defaultBlockState(), x, 1, cz, x, 3, cz);
            fill(level, box, Blocks.WHITE_CONCRETE.defaultBlockState(), x, 4, cz - 1, x, 5, cz + 1);
            put(level, box, Blocks.ORANGE_STAINED_GLASS_PANE, x + (end == 0 ? 1 : -1), 4, cz);
        }
        BlockState fx = Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.EAST, true).setValue(IronBarsBlock.WEST, true);
        BlockState fz = Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true);
        for (int y = 1; y <= 3; y++) {
            for (int x = x0; x <= x1; x++) {
                put(level, box, fx, x, y, z0);
                if (x < x0 + 9 || x > x0 + 11) put(level, box, fx, x, y, z1);
            }
            for (int z = z0 + 1; z < z1; z++) {
                put(level, box, fz, x0, y, z);
                put(level, box, fz, x1, y, z);
            }
        }
    }

    // ------------------------------------------------------------------ community garden

    private void garden(WorldGenLevel level, BoundingBox box, int x0, int z0) {
        int x1 = x0 + 35, z1 = z0 + 17;
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) put(level, box, Blocks.GRASS_BLOCK, x, 0, z);
        Block[] crops = {Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS};
        for (int bed = 0; bed < 6; bed++) {
            int bx = x0 + 2 + bed * 6;
            for (int x = bx; x < bx + 4; x++) for (int z = z0 + 2; z <= z0 + 9; z++) {
                put(level, box, Blocks.FARMLAND.defaultBlockState().setValue(net.minecraft.world.level.block.FarmBlock.MOISTURE, 7), x, 0, z);
                net.minecraft.world.level.block.CropBlock c = (net.minecraft.world.level.block.CropBlock) crops[bed % crops.length];
                put(level, box, c.getStateForAge(Math.min(c.getMaxAge(), (x + z) % 4 + 2)), x, 1, z);
            }
            put(level, box, Blocks.WATER, bx + 4, 0, z0 + 5);
        }
        // a shed, a picnic table, a mural wall at the back
        fill(level, box, Blocks.SPRUCE_PLANKS.defaultBlockState(), x1 - 5, 1, z1 - 5, x1 - 1, 3, z1 - 1);
        fill(level, box, Blocks.AIR.defaultBlockState(), x1 - 4, 1, z1 - 4, x1 - 2, 2, z1 - 2);
        put(level, box, Blocks.AIR, x1 - 3, 1, z1 - 5);
        put(level, box, Blocks.AIR, x1 - 3, 2, z1 - 5);
        put(level, box, Blocks.COMPOSTER, x1 - 2, 1, z1 - 2);
        put(level, box, BlockRegistry.TABLE, x0 + 6, 1, z1 - 4);
        put(level, box, facing(BlockRegistry.CHAIR, Direction.NORTH), x0 + 6, 1, z1 - 3);
        put(level, box, facing(BlockRegistry.CHAIR, Direction.SOUTH), x0 + 6, 1, z1 - 5);
        Block[] mural = {Blocks.YELLOW_CONCRETE, Blocks.ORANGE_CONCRETE, Blocks.LIME_CONCRETE, Blocks.LIGHT_BLUE_CONCRETE, Blocks.MAGENTA_CONCRETE};
        for (int x = x0; x <= x1 - 7; x++) for (int y = 1; y <= 4; y++) {
            put(level, box, mural[Math.floorMod(x / 3 + y, mural.length)], x, y, z1);
        }
        sign(level, box, x0 + 10, 2, z1 - 1, Direction.NORTH, "COMMUNITY", "GARDEN", "Take what you", "need. Plant more.");
    }

    // ------------------------------------------------------------------ the vacant lot (gang turf)

    private void vacantLot(WorldGenLevel level, BoundingBox box, int x0, int z0) {
        int x1 = x0 + 35, z1 = z0 + 17;
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            put(level, box, ((x + z * 3) % 5 == 0 ? Blocks.GRAVEL : Blocks.COARSE_DIRT).defaultBlockState(), x, 0, z);
        }
        // a back wall with each crew's tag on it
        fill(level, box, Blocks.STONE_BRICKS.defaultBlockState(), x0, 1, z1, x1, 5, z1);
        tag(level, box, x0 + 3, z1 - 1, Blocks.ORANGE_CONCRETE.defaultBlockState(), "RR");
        tag(level, box, x1 - 12, z1 - 1, Blocks.PURPLE_CONCRETE.defaultBlockState(), "CK");
        // a burnt-out car, old tyres, a dumpster, burn barrels
        fill(level, box, Blocks.COAL_BLOCK.defaultBlockState(), x0 + 14, 1, z0 + 6, x0 + 16, 1, z0 + 10);
        put(level, box, Blocks.CAMPFIRE, x0 + 15, 2, z0 + 8);
        put(level, box, Blocks.BLACK_CONCRETE, x0 + 22, 1, z0 + 3);
        put(level, box, Blocks.BLACK_CONCRETE, x0 + 22, 2, z0 + 3);
        put(level, box, Blocks.GREEN_TERRACOTTA, x0 + 30, 1, z0 + 12);
        put(level, box, Blocks.GREEN_TERRACOTTA, x0 + 31, 1, z0 + 12);
        put(level, box, Blocks.CAMPFIRE, x0 + 3, 1, z0 + 8);
        put(level, box, Blocks.CAMPFIRE, x1 - 3, 1, z0 + 8);
        put(level, box, Blocks.ORANGE_BANNER, x0 + 2, 1, z0 + 6);
        put(level, box, Blocks.PURPLE_BANNER, x1 - 2, 1, z0 + 6);
        BlockPos homeA = at(x0 + 5, 1, z0 + 8);
        BlockPos homeB = at(x1 - 5, 1, z0 + 8);
        BlockPos centre = at((x0 + x1) / 2, 1, z0 + 8);
        if (box.isInside(centre)) {
            GangTurfEntity turf = new GangTurfEntity(MoreMobs.GANG_TURF, server(level));
            turf.moveTo(centre.getX() + 0.5, centre.getY(), centre.getZ() + 0.5, 0.0f, 0.0f);
            turf.setHomes(homeA, homeB);
            level.addFreshEntity(turf);
        }
        for (int k = 0; k < 3; k++) {
            gangster(level, box, Crew.RUST_RATS, x0 + 4 + k, z0 + 7 + k, homeA, k);
            gangster(level, box, Crew.CHROME_KINGS, x1 - 4 - k, z0 + 7 + k, homeB, k);
        }
    }

    private void gangster(WorldGenLevel level, BoundingBox box, Crew crew, int x, int z, BlockPos home, int k) {
        BlockPos p = at(x, 1, z);
        if (!box.isInside(p)) return;
        GangsterEntity g = new GangsterEntity(MoreMobs.GANGSTER, server(level));
        g.setup(crew, k & 1, GangsterEntity.randomGun(((x * 13 + z * 7) % 100) / 100.0f), home);
        g.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, (x * 37) % 360, 0.0f);
        level.addFreshEntity(g);
    }

    /** Big block letters on the wall facing north (two letters, 5 tall). */
    private void tag(WorldGenLevel level, BoundingBox box, int x0, int z, BlockState paint, String letters) {
        String[][] font = {
                {"RRR ", "R  R", "RRR ", "R R ", "R  R"},   // R
                {" CCC", "C   ", "C   ", "C   ", " CCC"},   // C
                {"K  K", "K K ", "KK  ", "K K ", "K  K"}};  // K
        for (int li = 0; li < letters.length(); li++) {
            char ch = letters.charAt(li);
            String[] g = ch == 'R' ? font[0] : ch == 'C' ? font[1] : font[2];
            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 4; col++) {
                    if (g[row].charAt(col) != ' ') put(level, box, paint, x0 + li * 5 + col, 5 - row, z + 1);
                }
            }
        }
    }
}
