package dev.freerun.world;

import dev.freerun.mobs.RoadhouseNpcEntity;
import dev.freerun.mobs.RoadhouseNpcEntity.Role;
import dev.freerun.mobs.SoldierRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * The roadhouse restaurant, 25 wide x 21 deep, laid out front (z=0) to back:
 *
 *   z 0       front wall: 3-wide entrance with the 5x2 steakhouse sign above it
 *   z 1-4     LOBBY: host stand + host, waiting chairs, peanut barrels, plants
 *   z 5       half-wall with a 5-wide opening
 *   z 6-12    DINING: 8 tables, 4 chairs each, seated customers, two servers
 *   z 13      kitchen wall with a 3-wide, unobstructed doorway
 *   z 14-19   KITCHEN: grill/smoker line, side counters, sink, pantry, two islands, two cooks
 *
 * Built with local coords via placeBlock; orientation SOUTH maps local x/z straight to world x/z.
 * NPCs are only spawned if their spot is inside the chunk box being generated, so each spawns once.
 */
public class RoadhousePiece extends StructurePiece {
    private static final int MAX_X = 24;          // 25 wide
    private static final int MAX_Z = 20;          // 21 deep
    private static final int WALL = 5;            // ceiling height; interior is y 1..4
    private static final int LOBBY_WALL = 5;
    private static final int KITCHEN_WALL = 13;

    public RoadhousePiece(BlockPos origin) {
        super(StructureRegistry.ROADHOUSE_PIECE, 0, new BoundingBox(
                origin.getX(), origin.getY(), origin.getZ(),
                origin.getX() + MAX_X, origin.getY() + WALL + 2, origin.getZ() + MAX_Z));
        // A concrete orientation so placeBlock() has a non-null rotation (SOUTH = no rotation).
        setOrientation(Direction.SOUTH);
    }

    public RoadhousePiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(StructureRegistry.ROADHOUSE_PIECE, tag);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        // bounding box + orientation are saved by the base class
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator chunkGenerator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        buildShell(level, box);
        buildSign(level, box);
        buildLobby(level, box);
        buildDining(level, box);
        buildKitchen(level, box);
    }

    // ------------------------------------------------------------------ shell

    private void buildShell(WorldGenLevel level, BoundingBox box) {
        BlockState planks = Blocks.SPRUCE_PLANKS.defaultBlockState();
        BlockState log = Blocks.SPRUCE_LOG.defaultBlockState();
        BlockState red = Blocks.RED_TERRACOTTA.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState glass = Blocks.GLASS_PANE.defaultBlockState();
        BlockState glow = Blocks.GLOWSTONE.defaultBlockState();

        // Floors: dark oak lobby, spruce dining, checkered kitchen tile
        for (int x = 0; x <= MAX_X; x++) {
            for (int z = 0; z <= MAX_Z; z++) {
                BlockState floor;
                boolean kitchen = z > KITCHEN_WALL && z < MAX_Z && x > 0 && x < MAX_X;
                if (kitchen) {
                    floor = ((x + z) % 2 == 0) ? Blocks.WHITE_CONCRETE.defaultBlockState()
                                               : Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
                } else if (z < LOBBY_WALL) {
                    floor = Blocks.DARK_OAK_PLANKS.defaultBlockState();
                } else {
                    floor = planks;
                }
                placeBlock(level, floor, x, 0, z, box);
            }
        }

        // Clear the interior
        for (int x = 1; x < MAX_X; x++)
            for (int z = 1; z < MAX_Z; z++)
                for (int y = 1; y < WALL; y++)
                    placeBlock(level, air, x, y, z, box);

        // Outer walls with a red accent band and windows
        for (int x = 0; x <= MAX_X; x++) {
            for (int z = 0; z <= MAX_Z; z++) {
                boolean edge = x == 0 || x == MAX_X || z == 0 || z == MAX_Z;
                if (!edge) continue;
                boolean corner = (x == 0 || x == MAX_X) && (z == 0 || z == MAX_Z);
                for (int y = 1; y <= WALL; y++) {
                    BlockState s = corner ? log : (y == 2 ? red : planks);
                    placeBlock(level, s, x, y, z, box);
                }
                if (!corner && isWindow(x, z)) placeBlock(level, glass, x, 3, z, box);
            }
        }

        // 3-wide, 3-tall entrance
        for (int x = 11; x <= 13; x++)
            for (int y = 1; y <= 3; y++)
                placeBlock(level, air, x, y, 0, box);

        // Ceiling with a grid of glowstone lights
        for (int x = 1; x < MAX_X; x++)
            for (int z = 1; z < MAX_Z; z++)
                placeBlock(level, (x % 4 == 2 && z % 4 == 2) ? glow : planks, x, WALL, z, box);

        // Red roof, two tiers
        for (int x = 0; x <= MAX_X; x++)
            for (int z = 0; z <= MAX_Z; z++)
                placeBlock(level, red, x, WALL + 1, z, box);
        for (int x = 2; x <= MAX_X - 2; x++)
            for (int z = 2; z <= MAX_Z - 2; z++)
                placeBlock(level, red, x, WALL + 2, z, box);
    }

    private static boolean isWindow(int x, int z) {
        if (z == 0) return x == 3 || x == 4 || x == 6 || x == 7 || x == 17 || x == 18 || x == 20 || x == 21;
        if (z == MAX_Z) return x == 6 || x == 7 || x == 17 || x == 18;
        if (x == 0 || x == MAX_X) return z == 2 || z == 3 || z == 7 || z == 8 || z == 10 || z == 11 || z == 16 || z == 17;
        return false;
    }

    // ------------------------------------------------------------------ sign

    /** The 5x2 sign above the entrance, facing out (north). Seen from outside, part 0 is top-left. */
    private void buildSign(WorldGenLevel level, BoundingBox box) {
        BlockState panel = BlockRegistry.SIGN_PANEL.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
        for (int row = 0; row < 2; row++) {
            int y = row == 0 ? WALL : WALL - 1;
            for (int col = 0; col < 5; col++) {
                int x = 14 - col;    // viewer outside faces south, so their left is +x
                placeBlock(level, panel.setValue(SignPanelBlock.PART, row * 5 + col), x, y, 0, box);
            }
        }
    }

    // ------------------------------------------------------------------ lobby

    private void buildLobby(WorldGenLevel level, BoundingBox box) {
        BlockState planks = Blocks.SPRUCE_PLANKS.defaultBlockState();
        BlockState fence = Blocks.SPRUCE_FENCE.defaultBlockState();

        // Half-wall between lobby and dining, with a 5-wide opening in the middle
        for (int x = 1; x < MAX_X; x++) {
            if (x >= 10 && x <= 14) continue;
            placeBlock(level, planks, x, 1, LOBBY_WALL, box);
            placeBlock(level, fence, x, 2, LOBBY_WALL, box);
        }

        // Host stand
        for (int x = 16; x <= 18; x++) placeBlock(level, Blocks.DARK_OAK_PLANKS.defaultBlockState(), x, 1, 2, box);
        placeBlock(level, Blocks.LANTERN.defaultBlockState(), 18, 2, 2, box);

        // Waiting benches along both side walls
        for (int z = 1; z <= 3; z++) {
            chair(level, box, 1, z, Direction.EAST);
            chair(level, box, MAX_X - 1, z, Direction.WEST);
        }

        // Peanut barrels by the door, plants in the corners
        placeBlock(level, Blocks.BARREL.defaultBlockState(), 9, 1, 1, box);
        placeBlock(level, Blocks.BARREL.defaultBlockState(), 15, 1, 1, box);
        placeBlock(level, Blocks.POTTED_FERN.defaultBlockState(), 1, 1, 4, box);
        placeBlock(level, Blocks.POTTED_FERN.defaultBlockState(), MAX_X - 1, 1, 4, box);

        spawnNpc(level, box, 17, 3, Direction.NORTH, Role.HOST, 0);
        seatNpc(level, box, 1, 2, Direction.EAST, Role.CUSTOMER, 0);   // someone waiting for a table
    }

    // ------------------------------------------------------------------ dining

    private void buildDining(WorldGenLevel level, BoundingBox box) {
        BlockState table = BlockRegistry.TABLE.defaultBlockState();
        int[] tableX = {4, 9, 15, 20};
        int[] tableZ = {7, 11};
        for (int tx : tableX) {
            for (int tz : tableZ) {
                placeBlock(level, table, tx, 1, tz, box);
                chair(level, box, tx, tz - 1, Direction.SOUTH);   // all chairs face the table
                chair(level, box, tx, tz + 1, Direction.NORTH);
                chair(level, box, tx - 1, tz, Direction.EAST);
                chair(level, box, tx + 1, tz, Direction.WEST);
                placeBlock(level, hangingLantern(), tx, WALL - 1, tz, box);
            }
        }

        // Customers enjoying their meals
        seatNpc(level, box, 4, 6, Direction.SOUTH, Role.CUSTOMER, 1);
        seatNpc(level, box, 5, 7, Direction.WEST, Role.CUSTOMER, 2);
        seatNpc(level, box, 9, 12, Direction.NORTH, Role.CUSTOMER, 0);
        seatNpc(level, box, 15, 10, Direction.SOUTH, Role.CUSTOMER, 2);
        seatNpc(level, box, 14, 11, Direction.EAST, Role.CUSTOMER, 1);
        seatNpc(level, box, 20, 8, Direction.NORTH, Role.CUSTOMER, 0);
        seatNpc(level, box, 21, 7, Direction.WEST, Role.CUSTOMER, 1);

        // Servers working the aisles
        spawnNpc(level, box, 6, 9, Direction.EAST, Role.WAITER, 0);
        spawnNpc(level, box, 17, 9, Direction.WEST, Role.WAITER, 0);
    }

    // ------------------------------------------------------------------ kitchen

    private void buildKitchen(WorldGenLevel level, BoundingBox box) {
        BlockState planks = Blocks.SPRUCE_PLANKS.defaultBlockState();
        BlockState counter = Blocks.SMOOTH_QUARTZ.defaultBlockState();
        BlockState grill = BlockRegistry.GRILL.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
        BlockState smoker = Blocks.SMOKER.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);

        // Kitchen wall with a 3-wide, 3-tall doorway — nothing placed in front of it
        for (int x = 1; x < MAX_X; x++) {
            for (int y = 1; y < WALL; y++) {
                if (x >= 11 && x <= 13 && y <= 3) continue;
                placeBlock(level, planks, x, y, KITCHEN_WALL, box);
            }
        }

        // Cooking line along the back wall, facing into the kitchen
        for (int x = 2; x <= MAX_X - 2; x++) {
            placeBlock(level, (x % 2 == 0) ? grill : smoker, x, 1, MAX_Z - 1, box);
        }

        // West side: prep counter. East side: pantry, sink, counter.
        for (int z = KITCHEN_WALL + 1; z <= MAX_Z - 2; z++) placeBlock(level, counter, 1, 1, z, box);
        placeBlock(level, Blocks.BARREL.defaultBlockState(), MAX_X - 1, 1, 14, box);
        placeBlock(level, Blocks.BARREL.defaultBlockState(), MAX_X - 1, 1, 15, box);
        placeBlock(level, Blocks.WATER_CAULDRON.defaultBlockState(), MAX_X - 1, 1, 16, box);
        placeBlock(level, counter, MAX_X - 1, 1, 17, box);
        placeBlock(level, counter, MAX_X - 1, 1, 18, box);

        // Two prep islands, leaving a clear aisle (x 9..15) straight from the doorway
        for (int z = 16; z <= 17; z++) {
            for (int x = 5; x <= 8; x++) placeBlock(level, counter, x, 1, z, box);
            for (int x = 16; x <= 19; x++) placeBlock(level, counter, x, 1, z, box);
        }

        spawnNpc(level, box, 7, 18, Direction.SOUTH, Role.COOK, 0);
        spawnNpc(level, box, 17, 18, Direction.SOUTH, Role.COOK, 0);
    }

    // ------------------------------------------------------------------ helpers

    private void chair(WorldGenLevel level, BoundingBox box, int x, int z, Direction facing) {
        placeBlock(level, BlockRegistry.CHAIR.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, facing), x, 1, z, box);
    }

    private static BlockState hangingLantern() {
        return Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, Boolean.TRUE);
    }

    private void spawnNpc(WorldGenLevel level, BoundingBox box, int x, int z, Direction facing,
                          Role role, int variant) {
        BlockPos pos = getWorldPos(x, 1, z).immutable();
        if (!box.isInside(pos)) return;
        ServerLevel server = level.getLevel();
        RoadhouseNpcEntity npc = new RoadhouseNpcEntity(SoldierRegistry.ROADHOUSE_NPC, server);
        npc.setRole(role, variant);
        float yaw = facing.toYRot();
        npc.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0);
        npc.setYHeadRot(yaw);
        npc.setYBodyRot(yaw);
        level.addFreshEntityWithPassengers(npc);
    }

    /** Puts an NPC in the chair at (x, z) by seating it on a SeatEntity. */
    private void seatNpc(WorldGenLevel level, BoundingBox box, int x, int z, Direction facing,
                         Role role, int variant) {
        BlockPos pos = getWorldPos(x, 1, z).immutable();
        if (!box.isInside(pos)) return;
        ServerLevel server = level.getLevel();
        SeatEntity seat = SeatEntity.create(server, pos);
        RoadhouseNpcEntity npc = new RoadhouseNpcEntity(SoldierRegistry.ROADHOUSE_NPC, server);
        npc.setRole(role, variant);
        float yaw = facing.toYRot();
        npc.moveTo(seat.getX(), seat.getY(), seat.getZ(), yaw, 0);
        npc.setYHeadRot(yaw);
        npc.setYBodyRot(yaw);
        npc.startRiding(seat, true);
        level.addFreshEntityWithPassengers(seat);
    }
}
