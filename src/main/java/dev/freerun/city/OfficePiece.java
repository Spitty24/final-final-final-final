package dev.freerun.city;

import dev.freerun.economy.EconomyRegistry;
import dev.freerun.food.FoodRegistry;
import dev.freerun.world.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Downtown: glass office towers (one per block, two on the wide blocks) on a paved plaza with
 * planters and benches. Each tower has a lobby with a reception desk and the company's name, a
 * staircase up the back, open-plan floors of desks with monitors and chairs, a break corner with
 * a coffee maker, ceiling lights, and a roof with HVAC units (and a helipad on the tallest).
 */
public class OfficePiece extends CityPieceBase {
    private static final String[] COMPANIES = {"Bayou Analytics", "Delta Synergy Group", "Crawfish Capital", "Gator Logistics",
            "Pelican Mutual Insurance", "Magnolia Tech", "Cypress & Pine LLP", "Riverbend Holdings", "Levee Data Systems",
            "Spanish Moss Media", "Second Line Software", "Cane Field Consulting"};
    private static final int FLOOR = 5;
    public static final int MAX_FLOORS = 10;

    public OfficePiece(BlockPos city, int lx, int lz, int w, int d) {
        super(CityRegistry.OFFICE_PIECE, city, lx, lz, w, MAX_FLOORS * FLOOR + 6, d);
    }

    public OfficePiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(CityRegistry.OFFICE_PIECE, tag);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        ground(level, box, Blocks.SMOOTH_STONE.defaultBlockState(), MAX_FLOORS * FLOOR + 6);
        // plaza paving
        for (int x = 0; x < w; x++) for (int z = 0; z < d; z++) {
            if ((x + z) % 2 == 0) put(level, box, Blocks.POLISHED_ANDESITE, x, 0, z);
        }
        if (w > 60) {
            tower(level, box, 5, 7, 30, 24, 0);
            tower(level, box, w - 35, 7, 30, 24, 1);
        } else {
            tower(level, box, 7, 7, 24, 24, 0);
        }
        // planters with trees and benches along the street
        for (int x = 3; x < w - 3; x += 10) {
            planter(level, box, x, 3);
            put(level, box, Blocks.SMOOTH_QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH), x + 4, 1, 2);
        }
    }

    private void planter(WorldGenLevel level, BoundingBox box, int x, int z) {
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            put(level, box, (dx == 0 && dz == 0) ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState(), x + dx, 0, z + dz);
            if (dx != 0 || dz != 0) put(level, box, Blocks.STONE_BRICK_SLAB, x + dx, 1, z + dz);
        }
        for (int y = 1; y <= 3; y++) put(level, box, Blocks.BIRCH_LOG, x, y, z);
        BlockState leaves = Blocks.BIRCH_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) for (int y = 3; y <= 5; y++) {
            if (Math.abs(dx) + Math.abs(dz) + (y == 5 ? 1 : 0) > 2 || (dx == 0 && dz == 0 && y < 5)) continue;
            put(level, box, leaves, x + dx, y, z + dz);
        }
    }

    private void tower(WorldGenLevel level, BoundingBox box, int x0, int z0, int tw, int td, int index) {
        int seed = Math.floorMod((ox + x0) * 73 + (oz + z0) * 19, 1009);
        int floors = 5 + seed % 6;
        String company = COMPANIES[seed % COMPANIES.length];
        BlockState column = (seed % 2 == 0 ? Blocks.LIGHT_GRAY_CONCRETE : Blocks.SMOOTH_QUARTZ).defaultBlockState();
        BlockState glass = (seed % 3 == 0 ? Blocks.LIGHT_BLUE_STAINED_GLASS : seed % 3 == 1 ? Blocks.GLASS : Blocks.CYAN_STAINED_GLASS).defaultBlockState();
        BlockState band = Blocks.GRAY_CONCRETE.defaultBlockState();
        int x1 = x0 + tw - 1, z1 = z0 + td - 1;
        int top = floors * FLOOR;
        for (int f = 0; f < floors; f++) {
            int y0 = f * FLOOR;
            fill(level, box, f == 0 ? Blocks.POLISHED_DIORITE.defaultBlockState() : Blocks.SMOOTH_STONE.defaultBlockState(), x0, y0, z0, x1, y0, z1);
            fill(level, box, band, x0, y0, z0, x1, y0, z0);
            fill(level, box, band, x0, y0, z1, x1, y0, z1);
            fill(level, box, band, x0, y0, z0, x0, y0, z1);
            fill(level, box, band, x1, y0, z0, x1, y0, z1);
            for (int y = y0 + 1; y < y0 + FLOOR; y++) {
                for (int x = x0; x <= x1; x++) {
                    BlockState s = ((x - x0) % 4 == 0 || x == x1) ? column : glass;
                    put(level, box, s, x, y, z0);
                    put(level, box, s, x, y, z1);
                }
                for (int z = z0 + 1; z < z1; z++) {
                    BlockState s = ((z - z0) % 4 == 0) ? column : glass;
                    put(level, box, s, x0, y, z);
                    put(level, box, s, x1, y, z);
                }
            }
            if (f == 0) lobby(level, box, x0, z0, x1, z1, company);
            else officeFloor(level, box, x0, y0, z0, x1, z1, seed + f);
        }
        // roof
        fill(level, box, Blocks.GRAY_CONCRETE.defaultBlockState(), x0, top, z0, x1, top, z1);
        // ceiling lights and the staircase, now that every slab is down
        for (int f = 0; f < floors; f++) {
            int y0 = f * FLOOR;
            for (int x = x0 + 3; x < x1 - 4; x += 5) for (int z = z0 + 3; z < z1 - 1; z += 5) put(level, box, Blocks.SEA_LANTERN, x, y0 + FLOOR, z);
            if (f == floors - 1) continue;
            for (int x = x1 - 3; x <= x1 - 2; x++) {
                for (int k = 0; k < 4; k++) put(level, box, Blocks.AIR, x, y0 + FLOOR, z0 + 3 + k);
                for (int k = 0; k <= 4; k++) {
                    put(level, box, Blocks.SMOOTH_QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH), x, y0 + 1 + k, z0 + 3 + k);
                }
            }
            fill(level, box, Blocks.IRON_BARS.defaultBlockState(), x1 - 4, y0 + FLOOR + 1, z0 + 3, x1 - 4, y0 + FLOOR + 1, z0 + 6);
        }
        fill(level, box, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), x0, top + 1, z0, x1, top + 1, z0);
        fill(level, box, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), x0, top + 1, z1, x1, top + 1, z1);
        fill(level, box, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), x0, top + 1, z0, x0, top + 1, z1);
        fill(level, box, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), x1, top + 1, z0, x1, top + 1, z1);
        fill(level, box, Blocks.IRON_BLOCK.defaultBlockState(), x0 + 3, top + 1, z0 + 3, x0 + 5, top + 2, z0 + 5);
        fill(level, box, Blocks.IRON_BLOCK.defaultBlockState(), x0 + 3, top + 1, z1 - 5, x0 + 6, top + 2, z1 - 3);
        put(level, box, Blocks.IRON_TRAPDOOR, x0 + 4, top + 3, z0 + 4);
        if (floors >= 9) {   // helipad
            int cx = (x0 + x1) / 2, cz = (z0 + z1) / 2;
            fill(level, box, Blocks.YELLOW_CONCRETE.defaultBlockState(), cx - 2, top, cz - 3, cx - 2, top, cz + 3);
            fill(level, box, Blocks.YELLOW_CONCRETE.defaultBlockState(), cx + 2, top, cz - 3, cx + 2, top, cz + 3);
            fill(level, box, Blocks.YELLOW_CONCRETE.defaultBlockState(), cx - 2, top, cz, cx + 2, top, cz);
        } else {
            put(level, box, Blocks.LIGHTNING_ROD, x1 - 2, top + 1, z1 - 2);
            put(level, box, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(net.minecraft.world.level.block.RedstoneLampBlock.LIT, true), x1 - 2, top + 2, z1 - 2);
        }
    }

    private void lobby(WorldGenLevel level, BoundingBox box, int x0, int z0, int x1, int z1, String company) {
        int mid = (x0 + x1) / 2;
        for (int x = mid - 1; x <= mid + 1; x++) for (int y = 1; y <= 3; y++) put(level, box, Blocks.AIR, x, y, z0);
        put(level, box, Blocks.GRAY_CARPET, mid, 1, z0 + 1);
        sign(level, box, mid, 4, z0 - 1, Direction.NORTH, "", company, "", "");
        // reception desk, waiting chairs, plants, an ATM and the directory
        for (int x = mid - 3; x <= mid + 3; x++) put(level, box, facing(FoodRegistry.COUNTER, Direction.NORTH), x, 1, z0 + 6);
        put(level, box, facing(EconomyRegistry.TV, Direction.NORTH).setValue(dev.freerun.economy.StoreBlocks.Tv.ON, true), mid, 2, z0 + 6);
        for (int x = x0 + 2; x <= x0 + 4; x++) put(level, box, facing(BlockRegistry.CHAIR, Direction.EAST), x, 1, z0 + 3);
        put(level, box, Blocks.FLOWERING_AZALEA, x0 + 1, 1, z0 + 1);
        put(level, box, Blocks.FLOWERING_AZALEA, x1 - 1, 1, z0 + 1);
        put(level, box, facing(EconomyRegistry.ATM, Direction.SOUTH), x0 + 2, 1, z1 - 1);
        put(level, box, facing(EconomyRegistry.VENDING_MACHINE, Direction.SOUTH), x0 + 4, 1, z1 - 1);
        sign(level, box, mid + 3, 2, z1 - 1, Direction.NORTH, "DIRECTORY", "1F Lobby", "2F+ Offices", "Roof: no.");
        BlockPos desk = at(mid, 1, z0 + 8);
        if (box.isInside(desk)) worker(level, desk);
    }

    private void officeFloor(WorldGenLevel level, BoundingBox box, int x0, int y0, int z0, int x1, int z1, int seed) {
        // rows of desks: table + monitor + chair
        for (int z = z0 + 4; z <= z1 - 4; z += 4) {
            for (int x = x0 + 2; x <= x1 - 6; x += 3) {
                put(level, box, BlockRegistry.TABLE, x, y0 + 1, z);
                put(level, box, facing(EconomyRegistry.TV, Direction.SOUTH).setValue(dev.freerun.economy.StoreBlocks.Tv.ON, (x + z + seed) % 3 != 0), x, y0 + 2, z);
                put(level, box, facing(BlockRegistry.CHAIR, Direction.NORTH), x, y0 + 1, z + 1);
            }
        }
        // break corner
        put(level, box, facing(FoodRegistry.COUNTER, Direction.NORTH), x0 + 1, y0 + 1, z1 - 1);
        put(level, box, facing(FoodRegistry.COFFEE_MAKER, Direction.NORTH), x0 + 1, y0 + 2, z1 - 1);
        put(level, box, facing(FoodRegistry.COUNTER, Direction.NORTH), x0 + 2, y0 + 1, z1 - 1);
        put(level, box, facing(FoodRegistry.MICROWAVE, Direction.NORTH), x0 + 2, y0 + 2, z1 - 1);
        put(level, box, Blocks.POTTED_BAMBOO, x1 - 1, y0 + 1, z1 - 1);
    }

    private void worker(WorldGenLevel level, BlockPos p) {
        CitizenEntity c = new CitizenEntity(CityRegistry.CITIZEN, server(level));
        c.setup(level.getRandom(), city, p, level.getRandom().nextBoolean() ? CitizenEntity.Job.OFFICE_WORKER : CitizenEntity.Job.ACCOUNTANT);
        c.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0.0f, 0.0f);
        level.addFreshEntity(c);
    }
}
