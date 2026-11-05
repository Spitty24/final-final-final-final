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
            else officeFloor(level, box, x0, y0, z0, x1, z1, seed + f, f == floors - 1);
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

    // ------------------------------------------------------------------ furnishing

    private static BlockState lf(net.minecraft.world.level.block.Block b, Direction d) {
        return b.defaultBlockState().setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, d);
    }

    /** A desk with a computer on it and a chair; {@code toChair} points from the desk to where you sit. */
    private void workstation(WorldGenLevel level, BoundingBox box, int x, int y, int z, Direction toChair, boolean on) {
        put(level, box, lf(dev.freerun.life.LifeRegistry.OFFICE_DESK, toChair), x, y, z);
        put(level, box, lf(dev.freerun.life.LifeRegistry.COMPUTER, toChair).setValue(dev.freerun.life.ComputerBlock.SCREEN, on ? 1 : 0), x, y + 1, z);
        put(level, box, lf(dev.freerun.life.LifeRegistry.OFFICE_CHAIR, toChair.getOpposite()), x + toChair.getStepX(), y, z + toChair.getStepZ());
    }

    private void cubicleWall(WorldGenLevel level, BoundingBox box, int xa, int y, int za, int xb, int zb) {
        BlockState wall = dev.freerun.life.LifeRegistry.CUBICLE_WALL.defaultBlockState();
        boolean alongX = za == zb;
        BlockState s = alongX ? wall.setValue(net.minecraft.world.level.block.IronBarsBlock.EAST, true).setValue(net.minecraft.world.level.block.IronBarsBlock.WEST, true)
                : wall.setValue(net.minecraft.world.level.block.IronBarsBlock.NORTH, true).setValue(net.minecraft.world.level.block.IronBarsBlock.SOUTH, true);
        fill(level, box, s, Math.min(xa, xb), y, Math.min(za, zb), Math.max(xa, xb), y, Math.max(za, zb));
    }

    /** A pod of four cubicles (7 x 6): two on each side of a centre partition, entered from the sides. */
    private void cubiclePod(WorldGenLevel level, BoundingBox box, int px, int y, int pz, int seed) {
        cubicleWall(level, box, px, y, pz, px + 6, pz);
        cubicleWall(level, box, px, y, pz + 5, px + 6, pz + 5);
        cubicleWall(level, box, px + 3, y, pz, px + 3, pz + 5);
        cubicleWall(level, box, px, y, pz, px, pz + 1);
        cubicleWall(level, box, px, y, pz + 4, px, pz + 5);
        cubicleWall(level, box, px + 6, y, pz, px + 6, pz + 1);
        cubicleWall(level, box, px + 6, y, pz + 4, px + 6, pz + 5);
        workstation(level, box, px + 1, y, pz + 1, Direction.SOUTH, (seed & 1) == 0);
        workstation(level, box, px + 5, y, pz + 1, Direction.SOUTH, (seed & 2) == 0);
        workstation(level, box, px + 1, y, pz + 4, Direction.NORTH, (seed & 4) == 0);
        workstation(level, box, px + 5, y, pz + 4, Direction.NORTH, true);
        put(level, box, lf(dev.freerun.life.LifeRegistry.FILING_CABINET, Direction.SOUTH), px + 2, y, pz + 1);
        put(level, box, Blocks.POTTED_FERN, px + 4, y, pz + 4);
        put(level, box, lf(dev.freerun.life.LifeRegistry.DESK_PHONE, Direction.NORTH), px + 2, y, pz + 4);
    }

    private void lobby(WorldGenLevel level, BoundingBox box, int x0, int z0, int x1, int z1, String company) {
        int mid = (x0 + x1) / 2;
        for (int x = mid - 1; x <= mid + 1; x++) for (int y = 1; y <= 3; y++) put(level, box, Blocks.AIR, x, y, z0);
        put(level, box, Blocks.GRAY_CARPET, mid, 1, z0 + 1);
        sign(level, box, mid, 4, z0 - 1, Direction.NORTH, "", company, "", "");
        // the reception desk: a long counter with the receptionist's computer and phone
        for (int x = mid - 3; x <= mid + 3; x++) put(level, box, facing(FoodRegistry.COUNTER, Direction.NORTH), x, 1, z0 + 6);
        put(level, box, lf(dev.freerun.life.LifeRegistry.COMPUTER, Direction.SOUTH), mid - 1, 2, z0 + 6);
        put(level, box, lf(dev.freerun.life.LifeRegistry.DESK_PHONE, Direction.NORTH), mid + 1, 2, z0 + 6);
        put(level, box, lf(dev.freerun.life.LifeRegistry.OFFICE_CHAIR, Direction.NORTH), mid - 1, 1, z0 + 7);
        put(level, box, lf(dev.freerun.life.LifeRegistry.OFFICE_CHAIR, Direction.NORTH), mid + 1, 1, z0 + 7);
        // waiting area, the job board, plants, ATM, vending, water cooler
        for (int x = x0 + 2; x <= x0 + 4; x++) put(level, box, facing(BlockRegistry.CHAIR, Direction.EAST), x, 1, z0 + 3);
        put(level, box, lf(dev.freerun.life.LifeRegistry.COFFEE_TABLE, Direction.NORTH), x0 + 3, 1, z0 + 4);
        put(level, box, lf(dev.freerun.life.LifeRegistry.JOB_BOARD, Direction.NORTH), mid + 4, 2, z0 + 11);
        fill(level, box, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), mid + 3, 1, z0 + 12, mid + 5, 3, z0 + 12);
        sign(level, box, mid + 4, 3, z0 + 11, Direction.NORTH, "§lNOW HIRING", "Janitor · IT", "Accounting", "Managers · Front desk");
        put(level, box, lf(dev.freerun.life.LifeRegistry.BIG_PLANT, Direction.NORTH), x0 + 1, 1, z0 + 1);
        put(level, box, lf(dev.freerun.life.LifeRegistry.BIG_PLANT, Direction.NORTH), x1 - 1, 1, z0 + 1);
        put(level, box, facing(EconomyRegistry.ATM, Direction.SOUTH), x0 + 2, 1, z1 - 1);
        put(level, box, facing(EconomyRegistry.VENDING_MACHINE, Direction.SOUTH), x0 + 4, 1, z1 - 1);
        put(level, box, lf(dev.freerun.life.LifeRegistry.WATER_COOLER, Direction.SOUTH), x0 + 6, 1, z1 - 1);
        sign(level, box, mid + 3, 2, z1 - 1, Direction.NORTH, "DIRECTORY", "1F Lobby & HR", "2F+ Offices", "Top: Executive");
    }

    private void officeFloor(WorldGenLevel level, BoundingBox box, int x0, int y0, int z0, int x1, int z1, int seed, boolean top) {
        int y = y0 + 1;
        // every floor: carpet, the water cooler and printer by the stairs, a whiteboard, the janitor's closet
        for (int x = x0 + 1; x < x1; x++) for (int z = z0 + 1; z < z1; z++) put(level, box, (seed % 2 == 0 ? Blocks.GRAY_CARPET : Blocks.LIGHT_GRAY_CARPET), x, y, z);
        put(level, box, lf(dev.freerun.life.LifeRegistry.WATER_COOLER, Direction.WEST), x1 - 1, y, z0 + 10);
        put(level, box, lf(dev.freerun.life.LifeRegistry.PRINTER, Direction.WEST), x1 - 1, y, z0 + 12);
        put(level, box, lf(dev.freerun.life.LifeRegistry.WHITEBOARD, Direction.WEST), x1 - 1, y + 1, z0 + 15);
        fill(level, box, Blocks.WHITE_CONCRETE.defaultBlockState(), x1 - 3, y, z1 - 3, x1 - 1, y + 2, z1 - 1);
        fill(level, box, Blocks.AIR.defaultBlockState(), x1 - 2, y, z1 - 2, x1 - 1, y + 2, z1 - 1);
        put(level, box, Blocks.AIR, x1 - 3, y, z1 - 2);
        put(level, box, Blocks.AIR, x1 - 3, y + 1, z1 - 2);
        put(level, box, Blocks.CAULDRON, x1 - 1, y, z1 - 1);
        put(level, box, Blocks.BARREL, x1 - 2, y, z1 - 1);
        sign(level, box, x1 - 3, y + 2, z1 - 2, Direction.WEST, "", "JANITOR", "", "");
        int type = top ? 3 : Math.floorMod(seed, 4);
        int w = x1 - x0;
        switch (type) {
            case 0, 1 -> {   // cubicle farm
                for (int px = x0 + 2; px + 6 <= x1 - 6; px += 8) {
                    for (int pz = z0 + 2; pz + 5 <= z1 - 2; pz += 7) cubiclePod(level, box, px, y, pz, seed + px * 3 + pz);
                }
            }
            case 2 -> {      // IT and accounting
                // the server room behind glass
                int sx = x0 + 1, sz = z1 - 7;
                fill(level, box, Blocks.GLASS.defaultBlockState(), sx, y, sz - 1, sx + 9, y + 3, sz - 1);
                put(level, box, Blocks.AIR, sx + 4, y, sz - 1);
                put(level, box, Blocks.AIR, sx + 4, y + 1, sz - 1);
                for (int x = sx + 1; x <= sx + 8; x += 2) {
                    for (int z = sz + 1; z <= sz + 4; z++) put(level, box, lf(dev.freerun.life.LifeRegistry.SERVER_RACK, Direction.EAST), x, y, z);
                }
                sign(level, box, sx + 6, y + 2, sz - 2, Direction.NORTH, "", "SERVER ROOM", "Authorized", "staff only");
                // the IT help desk
                workstation(level, box, sx + 12, y, sz + 2, Direction.NORTH, true);
                workstation(level, box, sx + 14, y, sz + 2, Direction.NORTH, true);
                put(level, box, lf(dev.freerun.life.LifeRegistry.DESK_PHONE, Direction.NORTH), sx + 13, y, sz + 2);
                sign(level, box, sx + 13, y + 2, sz + 3, Direction.NORTH, "", "IT HELP DESK", "Have you tried", "turning it off?");
                // accounting: rows of desks with filing cabinets between
                for (int z = z0 + 3; z <= z0 + 10; z += 4) {
                    for (int x = x0 + 2; x <= x0 + w - 8; x += 3) {
                        workstation(level, box, x, y, z, Direction.SOUTH, (x + z + seed) % 3 != 0);
                        put(level, box, lf(dev.freerun.life.LifeRegistry.FILING_CABINET, Direction.SOUTH), x + 1, y, z);
                    }
                }
                sign(level, box, x0 + 2, y + 2, z0 + 1, Direction.SOUTH, "", "ACCOUNTING", "", "");
            }
            default -> {     // management: corner offices round a conference room
                int[][] corners = {{x0 + 1, z0 + 1}, {x0 + 1, z1 - 7}, {x1 - 13, z1 - 7}};
                String[] titles = {"DIRECTOR", "VP, SALES", "CFO"};
                for (int k = 0; k < corners.length; k++) {
                    int cx = corners[k][0], cz = corners[k][1];
                    BlockState pane = Blocks.GLASS_PANE.defaultBlockState();
                    boolean north = cz == z0 + 1;
                    int wallZ = north ? cz + 6 : cz;
                    for (int x = cx; x <= cx + 6; x++) for (int yy = y; yy <= y + 2; yy++) {
                        put(level, box, pane.setValue(net.minecraft.world.level.block.IronBarsBlock.EAST, true).setValue(net.minecraft.world.level.block.IronBarsBlock.WEST, true), x, yy, wallZ);
                    }
                    for (int z = cz; z <= cz + 6; z++) for (int yy = y; yy <= y + 2; yy++) {
                        put(level, box, pane.setValue(net.minecraft.world.level.block.IronBarsBlock.NORTH, true).setValue(net.minecraft.world.level.block.IronBarsBlock.SOUTH, true), cx + 6, yy, z);
                    }
                    put(level, box, Blocks.AIR, cx + 6, y, cz + 3);
                    put(level, box, Blocks.AIR, cx + 6, y + 1, cz + 3);
                    fill(level, box, Blocks.RED_CARPET.defaultBlockState(), cx + 1, y, cz + 1, cx + 5, y, cz + 5);
                    // executive desk (two wide), bookshelf, plant, lamp, armchairs for visitors
                    Direction toChair = north ? Direction.NORTH : Direction.SOUTH;
                    int dz = north ? cz + 3 : cz + 3;
                    workstation(level, box, cx + 2, y, dz, toChair, true);
                    put(level, box, lf(dev.freerun.life.LifeRegistry.OFFICE_DESK, toChair), cx + 3, y, dz);
                    put(level, box, lf(dev.freerun.life.LifeRegistry.DESK_PHONE, toChair), cx + 3, y + 1, dz);
                    put(level, box, lf(dev.freerun.life.LifeRegistry.ARMCHAIR, toChair), cx + 2, y, dz - toChair.getStepZ() * 2);
                    put(level, box, lf(dev.freerun.life.LifeRegistry.ARMCHAIR, toChair), cx + 3, y, dz - toChair.getStepZ() * 2);
                    put(level, box, Blocks.BOOKSHELF, cx + 5, y, north ? cz + 1 : cz + 5);
                    put(level, box, Blocks.BOOKSHELF, cx + 5, y + 1, north ? cz + 1 : cz + 5);
                    put(level, box, lf(dev.freerun.life.LifeRegistry.BIG_PLANT, Direction.NORTH), cx + 1, y, north ? cz + 1 : cz + 5);
                    put(level, box, lf(dev.freerun.life.LifeRegistry.FILING_CABINET, Direction.EAST), cx + 1, y, dz);
                    sign(level, box, cx + 6, y + 2, cz + 2, Direction.EAST, "", titles[k], "", "");
                }
                // conference room: a long table, chairs down both sides, whiteboard and TV
                int tx = x0 + 9, tz = z0 + 9;
                for (int x = tx; x <= tx + 6; x++) {
                    put(level, box, lf(dev.freerun.life.LifeRegistry.OFFICE_DESK, Direction.NORTH), x, y, tz);
                    put(level, box, lf(dev.freerun.life.LifeRegistry.OFFICE_DESK, Direction.SOUTH), x, y, tz + 1);
                    if ((x - tx) % 2 == 0) {
                        put(level, box, lf(dev.freerun.life.LifeRegistry.OFFICE_CHAIR, Direction.SOUTH), x, y, tz - 1);
                        put(level, box, lf(dev.freerun.life.LifeRegistry.OFFICE_CHAIR, Direction.NORTH), x, y, tz + 2);
                    }
                }
                put(level, box, lf(dev.freerun.life.LifeRegistry.WHITEBOARD, Direction.EAST), tx - 2, y + 1, tz);
                put(level, box, facing(EconomyRegistry.TV, Direction.WEST).setValue(dev.freerun.economy.StoreBlocks.Tv.ON, true), tx + 8, y + 1, tz + 1);
                put(level, box, Blocks.LIGHT_GRAY_CONCRETE, tx + 8, y, tz + 1);
                // the executive assistant
                workstation(level, box, x0 + 12, y, z0 + 3, Direction.SOUTH, true);
                put(level, box, lf(dev.freerun.life.LifeRegistry.DESK_PHONE, Direction.SOUTH), x0 + 13, y, z0 + 3);
            }
        }
    }

    private void worker(WorldGenLevel level, BlockPos p) {
        CitizenEntity c = new CitizenEntity(CityRegistry.CITIZEN, server(level));
        c.setup(level.getRandom(), city, p, level.getRandom().nextBoolean() ? CitizenEntity.Job.OFFICE_WORKER : CitizenEntity.Job.ACCOUNTANT);
        c.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0.0f, 0.0f);
        level.addFreshEntity(c);
    }
}
