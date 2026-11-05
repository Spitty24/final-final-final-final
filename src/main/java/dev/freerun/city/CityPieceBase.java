package dev.freerun.city;

import dev.freerun.world.CityAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

/**
 * Base for the city's own pieces. Local x/z map straight onto world x/z, y=0 is street level, and
 * blocks are written exactly as given (no mirroring). Fills are clipped to the chunk being
 * generated so big pieces stay cheap.
 */
public abstract class CityPieceBase extends StructurePiece {
    protected static final int BELOW = 6;
    protected int ox, oy, oz, w, h, d;
    /** World position of the whole city's origin (for things that need the street plan). */
    protected BlockPos city;

    protected CityPieceBase(StructurePieceType type, BlockPos city, int lx, int lz, int w, int h, int d) {
        super(type, 0, new BoundingBox(city.getX() + lx, city.getY() - BELOW, city.getZ() + lz,
                city.getX() + lx + w - 1, city.getY() + h, city.getZ() + lz + d - 1));
        this.city = city.immutable();
        this.ox = city.getX() + lx;
        this.oy = city.getY();
        this.oz = city.getZ() + lz;
        this.w = w;
        this.h = h;
        this.d = d;
    }

    protected CityPieceBase(StructurePieceType type, CompoundTag tag) {
        super(type, tag);
        city = new BlockPos(tag.getInt("CX"), tag.getInt("CY"), tag.getInt("CZ"));
        ox = boundingBox.minX();
        oy = boundingBox.minY() + BELOW;
        oz = boundingBox.minZ();
        w = boundingBox.getXSpan();
        h = boundingBox.maxY() - oy;
        d = boundingBox.getZSpan();
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putInt("CX", city.getX());
        tag.putInt("CY", city.getY());
        tag.putInt("CZ", city.getZ());
        save(tag);
    }

    protected void save(CompoundTag tag) {
    }

    protected BlockPos at(int x, int y, int z) {
        return new BlockPos(ox + x, oy + y, oz + z);
    }

    protected void put(WorldGenLevel level, BoundingBox box, BlockState state, int x, int y, int z) {
        BlockPos p = at(x, y, z);
        if (box.isInside(p)) level.setBlock(p, state, Block.UPDATE_CLIENTS);
    }

    protected void put(WorldGenLevel level, BoundingBox box, Block block, int x, int y, int z) {
        put(level, box, block.defaultBlockState(), x, y, z);
    }

    /** Fill a local box, clipped to the chunk being generated. */
    protected void fill(WorldGenLevel level, BoundingBox box, BlockState state, int x0, int y0, int z0, int x1, int y1, int z1) {
        int ax = Math.max(Math.min(x0, x1), box.minX() - ox), bx = Math.min(Math.max(x0, x1), box.maxX() - ox);
        int ay = Math.max(Math.min(y0, y1), box.minY() - oy), by = Math.min(Math.max(y0, y1), box.maxY() - oy);
        int az = Math.max(Math.min(z0, z1), box.minZ() - oz), bz = Math.min(Math.max(z0, z1), box.maxZ() - oz);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = ax; x <= bx; x++) for (int z = az; z <= bz; z++) for (int y = ay; y <= by; y++) {
            level.setBlock(m.set(ox + x, oy + y, oz + z), state, Block.UPDATE_CLIENTS);
        }
    }

    protected void fill(WorldGenLevel level, BoundingBox box, Block block, int x0, int y0, int z0, int x1, int y1, int z1) {
        fill(level, box, block.defaultBlockState(), x0, y0, z0, x1, y1, z1);
    }

    /** Hollow box walls (no floor/ceiling). */
    protected void walls(WorldGenLevel level, BoundingBox box, BlockState state, int x0, int y0, int z0, int x1, int y1, int z1) {
        fill(level, box, state, x0, y0, z0, x1, y1, z0);
        fill(level, box, state, x0, y0, z1, x1, y1, z1);
        fill(level, box, state, x0, y0, z0, x0, y1, z1);
        fill(level, box, state, x1, y0, z0, x1, y1, z1);
    }

    /**
     * Flatten the whole footprint: air from street level up to {@code clearTo}, the given ground
     * block at y=0, and stone underneath wherever the terrain falls away.
     */
    protected void ground(WorldGenLevel level, BoundingBox box, BlockState top, int clearTo) {
        fill(level, box, Blocks.AIR.defaultBlockState(), 0, 1, 0, w - 1, clearTo, d - 1);
        fill(level, box, top, 0, 0, 0, w - 1, 0, d - 1);
        int ax = Math.max(0, box.minX() - ox), bx = Math.min(w - 1, box.maxX() - ox);
        int az = Math.max(0, box.minZ() - oz), bz = Math.min(d - 1, box.maxZ() - oz);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = ax; x <= bx; x++) {
            for (int z = az; z <= bz; z++) {
                for (int y = -1; y >= -BELOW; y--) {
                    m.set(ox + x, oy + y, oz + z);
                    if (!box.isInside(m)) break;
                    BlockState st = level.getBlockState(m);
                    if (!st.isAir() && !st.liquid() && !st.canBeReplaced()) break;
                    level.setBlock(m, (y == -1 ? Blocks.DIRT : Blocks.STONE).defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    protected static BlockState facing(Block b, Direction dir) {
        return b.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, dir);
    }

    protected boolean inside(BoundingBox box, int x, int y, int z) {
        return box.isInside(at(x, y, z));
    }

    protected net.minecraft.server.level.ServerLevel server(WorldGenLevel level) {
        return level.getLevel();
    }

    /** Sign helper shared with the store pieces. */
    protected void sign(WorldGenLevel level, BoundingBox box, int x, int y, int z, Direction facing, String... lines) {
        BlockPos p = at(x, y, z);
        if (!box.isInside(p)) return;
        level.setBlock(p, Blocks.OAK_WALL_SIGN.defaultBlockState()
                .setValue(net.minecraft.world.level.block.WallSignBlock.FACING, facing), Block.UPDATE_CLIENTS);
        CityAccess.signText(level, p, lines);
    }
}
