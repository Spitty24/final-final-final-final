package dev.freerun.world;

import dev.freerun.gang.Crew;
import dev.freerun.gang.GangTurfEntity;
import dev.freerun.gang.GangsterEntity;
import dev.freerun.mobs.MoreMobs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * The factory's parking lot, in front of the building (local z=16 touches the factory's front
 * wall). Two rows of striped stalls with parked cars (one still burning), light poles, a
 * sidewalk with a crosswalk to the front door, and the two crews' hangouts at either end —
 * plus the invisible {@link GangTurfEntity} that runs the gang activity.
 *
 *   z 0       far edge
 *   z 1..5    stall row 1
 *   z 6..8    driving aisle (crew hangouts at each end)
 *   z 9..13   stall row 2
 *   z 14      light poles, crosswalk
 *   z 15..16  sidewalk along the factory
 */
public class ParkingLotPiece extends StructurePiece {
    static final int MAX_X = FactoryPiece.MAX_X;
    static final int DEPTH = 17;
    private static final int[] WEST_STALLS = {1, 5, 9, 13};
    private static final int[] EAST_STALLS = {25, 29, 33, 37};

    /** {@code factoryOrigin} is the factory's own origin; the lot sits just in front of it. */
    public ParkingLotPiece(BlockPos factoryOrigin) {
        super(StructureRegistry.PARKING_LOT_PIECE, 0, new BoundingBox(
                factoryOrigin.getX(), factoryOrigin.getY(), factoryOrigin.getZ() - DEPTH,
                factoryOrigin.getX() + MAX_X, factoryOrigin.getY() + 6, factoryOrigin.getZ() - 1));
        setOrientation(Direction.SOUTH);
    }

    public ParkingLotPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(StructureRegistry.PARKING_LOT_PIECE, tag);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator chunkGenerator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState asphalt = Blocks.BLACK_CONCRETE.defaultBlockState();
        BlockState crack = Blocks.GRAY_CONCRETE.defaultBlockState();
        BlockState paint = Blocks.WHITE_CONCRETE.defaultBlockState();
        BlockState walk = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
        for (int x = 0; x <= MAX_X; x++) {
            for (int z = 0; z < DEPTH; z++) {
                BlockState ground;
                boolean stallRow = (z >= 1 && z <= 5) || (z >= 9 && z <= 13);
                boolean line = stallRow && (x <= 16 || x >= 24) && x % 4 == 0;
                if (z >= 15) ground = walk;
                else if (line) ground = paint;
                else if (z == 14 && x >= 17 && x <= 23 && x % 2 == 1) ground = paint;   // crosswalk
                else if ((x * 31 + z * 17) % 23 == 0) ground = crack;
                else ground = asphalt;
                placeBlock(level, ground, x, 0, z, box);
                for (int y = 1; y <= 6; y++) placeBlock(level, air, x, y, z, box);
            }
        }

        // Parked cars, one burnt out and still smoking from the last shootout
        car(level, box, WEST_STALLS[0], 1, Blocks.RED_CONCRETE.defaultBlockState(), false);
        car(level, box, WEST_STALLS[2], 1, Blocks.BLUE_CONCRETE.defaultBlockState(), false);
        car(level, box, EAST_STALLS[0], 1, Blocks.WHITE_CONCRETE.defaultBlockState(), false);
        car(level, box, EAST_STALLS[3], 1, Blocks.GRAY_CONCRETE.defaultBlockState(), false);
        car(level, box, WEST_STALLS[1], 9, Blocks.LIME_CONCRETE.defaultBlockState(), false);
        car(level, box, WEST_STALLS[3], 9, Blocks.BLACK_CONCRETE.defaultBlockState(), true);
        car(level, box, EAST_STALLS[1], 9, Blocks.YELLOW_CONCRETE.defaultBlockState(), false);
        car(level, box, EAST_STALLS[2], 9, Blocks.ORANGE_CONCRETE.defaultBlockState(), false);

        for (int x : new int[]{4, 12, 28, 36}) {   // light poles
            for (int y = 1; y <= 4; y++) placeBlock(level, Blocks.IRON_BARS.defaultBlockState(), x, y, 14, box);
            placeBlock(level, Blocks.SEA_LANTERN.defaultBlockState(), x, 5, 14, box);
        }

        // Dumpster on the sidewalk
        placeBlock(level, Blocks.GREEN_TERRACOTTA.defaultBlockState(), 0, 1, 15, box);
        placeBlock(level, Blocks.GREEN_TERRACOTTA.defaultBlockState(), 1, 1, 15, box);
        placeBlock(level, Blocks.IRON_TRAPDOOR.defaultBlockState(), 0, 2, 15, box);
        placeBlock(level, Blocks.IRON_TRAPDOOR.defaultBlockState(), 1, 2, 15, box);

        // Crew hangouts: banners, a burn barrel fire, crates to sit on
        hangout(level, box, 0, 2, Blocks.ORANGE_BANNER.defaultBlockState().setValue(BlockStateProperties.ROTATION_16, 12));
        hangout(level, box, MAX_X, MAX_X - 2, Blocks.PURPLE_BANNER.defaultBlockState().setValue(BlockStateProperties.ROTATION_16, 4));

        BlockPos homeA = getWorldPos(3, 1, 7).immutable();
        BlockPos homeB = getWorldPos(MAX_X - 3, 1, 7).immutable();
        BlockPos center = getWorldPos(20, 1, 7).immutable();
        if (box.isInside(center)) {
            GangTurfEntity turf = new GangTurfEntity(MoreMobs.GANG_TURF, level.getLevel());
            turf.moveTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5, 0.0f, 0.0f);
            turf.setHomes(homeA, homeB);
            level.addFreshEntity(turf);
        }
        for (int i = 0; i < 3; i++) {
            gangster(level, box, Crew.RUST_RATS, 3 + i, 6 + i, homeA, random);
            gangster(level, box, Crew.CHROME_KINGS, MAX_X - 3 - i, 6 + i, homeB, random);
        }
    }

    private void hangout(WorldGenLevel level, BoundingBox box, int edgeX, int fireX, BlockState banner) {
        BlockState barrel = Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP);
        placeBlock(level, barrel, edgeX, 1, 6, box);
        placeBlock(level, barrel, edgeX, 1, 8, box);
        placeBlock(level, banner, edgeX, 1, 7, box);
        placeBlock(level, Blocks.CAMPFIRE.defaultBlockState(), fireX, 1, 7, box);
    }

    /** A 3 x 5 block car facing the aisle: wheels at the corners, lights, a tinted cabin. */
    private void car(WorldGenLevel level, BoundingBox box, int x0, int z0, BlockState body, boolean burnt) {
        BlockState wheel = Blocks.BLACK_CONCRETE.defaultBlockState();
        BlockState head = burnt ? Blocks.COAL_BLOCK.defaultBlockState() : Blocks.WHITE_CONCRETE.defaultBlockState();
        BlockState tail = burnt ? Blocks.COAL_BLOCK.defaultBlockState() : Blocks.RED_CONCRETE.defaultBlockState();
        BlockState glass = Blocks.BLACK_STAINED_GLASS.defaultBlockState();
        if (burnt) body = Blocks.COAL_BLOCK.defaultBlockState();
        for (int dz = 0; dz < 5; dz++) {
            for (int dx = 0; dx < 3; dx++) {
                boolean side = dx != 1;
                BlockState s = body;
                if (side && (dz == 1 || dz == 3)) s = wheel;
                else if (side && dz == 4) s = head;   // nose faces the aisle
                else if (side && dz == 0) s = tail;
                placeBlock(level, s, x0 + dx, 1, z0 + dz, box);
                if (dz >= 1 && dz <= 3) {
                    placeBlock(level, burnt ? Blocks.AIR.defaultBlockState() : glass, x0 + dx, 2, z0 + dz, box);
                }
            }
        }
        if (burnt) placeBlock(level, Blocks.CAMPFIRE.defaultBlockState(), x0 + 1, 2, z0 + 2, box);
    }

    private void gangster(WorldGenLevel level, BoundingBox box, Crew crew, int x, int z, BlockPos home, RandomSource random) {
        BlockPos pos = getWorldPos(x, 1, z).immutable();
        if (!box.isInside(pos)) return;
        GangsterEntity g = new GangsterEntity(MoreMobs.GANGSTER, level.getLevel());
        g.setup(crew, random.nextInt(2), GangsterEntity.randomGun(random.nextFloat()), home);
        g.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360.0f, 0.0f);
        level.addFreshEntityWithPassengers(g);
    }
}
