package dev.freerun.city;

import dev.freerun.life.LifeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * A back alley in the 4-block gap between two downtown or hood lots: two climbable brick walls
 * a two-block corridor apart (wall-run along them, kick between them, climb them and run along
 * the tops), with dumpsters and crates to vault, AC units and fire-escape landings to hang from,
 * drainpipes to climb, a clothesline with a lamp, and a doorway halfway along each side.
 */
public class AlleyPiece extends CityPieceBase {
    /** true: the alley runs north-south (4 wide in x); false: east-west (4 deep in z). */
    private final boolean alongZ;
    private final boolean hood;

    public AlleyPiece(BlockPos city, int lx, int lz, int w, int d, boolean alongZ, boolean hood) {
        super(CityRegistry.ALLEY_PIECE, city, lx, lz, w, 12, d);
        this.alongZ = alongZ;
        this.hood = hood;
    }

    public AlleyPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(CityRegistry.ALLEY_PIECE, tag);
        alongZ = tag.getBoolean("AlongZ");
        hood = tag.getBoolean("Hood");
    }

    @Override
    protected void save(CompoundTag tag) {
        tag.putBoolean("AlongZ", alongZ);
        tag.putBoolean("Hood", hood);
    }

    private int len() {
        return alongZ ? d : w;
    }

    /** Puts a block at (across, y, along) in alley coordinates. */
    private void at(WorldGenLevel level, BoundingBox box, BlockState s, int a, int y, int l) {
        if (alongZ) put(level, box, s, a, y, l);
        else put(level, box, s, l, y, a);
    }

    /** The horizontal direction pointing from across-position 0 toward 3. */
    private Direction acrossDir() {
        return alongZ ? Direction.EAST : Direction.SOUTH;
    }

    private int hash(int l, int salt) {
        int h = (ox * 31 + oz * 17 + l * 7919 + salt * 104729);
        h ^= h >>> 11;
        return Math.floorMod(h * 0x2545F491, 1000);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        int len = len();
        int wallH = hood ? 7 : 9;
        BlockState air = Blocks.AIR.defaultBlockState();
        // clear and pave
        fill(level, box, air, 0, 1, 0, w - 1, 11, d - 1);
        for (int l = 0; l < len; l++) for (int a = 0; a < 4; a++) {
            int h = hash(l * 4 + a, 1);
            at(level, box, (h % 9 == 0 ? Blocks.CRACKED_STONE_BRICKS : h % 5 == 0 ? Blocks.GRAVEL : Blocks.GRAY_CONCRETE).defaultBlockState(), a, 0, l);
        }
        support(level, box, 0, 0, w - 1, d - 1);

        // the walls (open at both ends, and a doorway halfway along each side)
        for (int side = 0; side < 2; side++) {
            int a = side == 0 ? 0 : 3;
            for (int l = 0; l < len; l++) {
                for (int y = 1; y <= wallH; y++) {
                    int h = hash(l * 16 + y, side + 2);
                    BlockState s;
                    if (hood) s = (h % 7 == 0 ? Blocks.CRACKED_STONE_BRICKS : h % 11 == 0 ? Blocks.MOSSY_STONE_BRICKS : Blocks.BRICKS).defaultBlockState();
                    else s = (h % 6 == 0 ? Blocks.MOSSY_STONE_BRICKS : h % 9 == 0 ? Blocks.CRACKED_STONE_BRICKS : Blocks.STONE_BRICKS).defaultBlockState();
                    at(level, box, s, a, y, l);
                }
                at(level, box, Blocks.STONE_BRICK_SLAB.defaultBlockState(), a, wallH + 1, l);
            }
            int door = len / 2 + (side == 0 ? -3 : 3);
            for (int y = 1; y <= 2; y++) at(level, box, air, a, y, door);
            at(level, box, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false), a, wallH + 1, door);
        }

        // what's in the corridor (across 1 and 2), every few blocks
        Direction toWall0 = acrossDir().getOpposite(), toWall3 = acrossDir();
        for (int l = 3; l < len - 3; l += 4) {
            int h = hash(l, 9);
            int a = h % 2 == 0 ? 1 : 2;                       // which side it's against
            Direction wallward = a == 1 ? toWall0 : toWall3;
            switch ((h / 2) % 7) {
                case 0 -> {   // dumpster: two long, lid on top. Vault it.
                    at(level, box, Blocks.GREEN_TERRACOTTA.defaultBlockState(), a, 1, l);
                    at(level, box, Blocks.GREEN_TERRACOTTA.defaultBlockState(), a, 1, l + 1);
                    BlockState lid = Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.BOTTOM);
                    at(level, box, lid, a, 2, l);
                    at(level, box, lid, a, 2, l + 1);
                }
                case 1 -> {   // crates stacked two high, and a lone one to step up with
                    at(level, box, Blocks.BARREL.defaultBlockState(), a, 1, l);
                    at(level, box, Blocks.SPRUCE_PLANKS.defaultBlockState(), a, 2, l);
                    at(level, box, Blocks.BARREL.defaultBlockState(), a, 1, l + 1);
                }
                case 2 -> {   // fire-escape landing with a ladder up to it
                    BlockState deck = Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.TOP);
                    for (int k = 0; k < 3; k++) at(level, box, deck, a, 4, l + k);
                    for (int y = 1; y <= 4; y++) {
                        at(level, box, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, wallward.getOpposite()), a, y, l - 1);
                    }
                    at(level, box, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), a, 3, l + 2);
                }
                case 3 -> {   // AC units sticking out of the wall: handholds
                    at(level, box, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), a, 3, l);
                    at(level, box, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), 3 - a, 6, l + 2);
                }
                case 4 -> {   // drainpipe running up the wall
                    for (int y = 1; y <= wallH; y++) at(level, box, Blocks.CHAIN.defaultBlockState(), a, y, l);
                    at(level, box, Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.BOTTOM), a, 1, l + 1);
                }
                case 5 -> {   // trash bags piled by a back door
                    at(level, box, LifeRegistry.TRASH_BAG.defaultBlockState(), a, 1, l);
                    at(level, box, LifeRegistry.TRASH_BAG.defaultBlockState(), a, 1, l + 1);
                    at(level, box, Blocks.COMPOSTER.defaultBlockState(), a, 1, l + 2);
                }
                default -> {  // clothesline across, with a lamp hanging from it
                    BlockState line = Blocks.CHAIN.defaultBlockState().setValue(ChainBlock.AXIS, acrossDir().getAxis());
                    at(level, box, line, 1, wallH - 1, l);
                    at(level, box, line, 2, wallH - 1, l);
                    at(level, box, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), a, wallH - 2, l);
                }
            }
        }
    }
}
