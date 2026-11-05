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

    /** Solid ground under a local rectangle wherever the terrain falls away below street level. */
    protected void support(WorldGenLevel level, BoundingBox box, int x0, int z0, int x1, int z1) {
        int ax = Math.max(x0, box.minX() - ox), bx = Math.min(x1, box.maxX() - ox);
        int az = Math.max(z0, box.minZ() - oz), bz = Math.min(z1, box.maxZ() - oz);
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

    /** A parked (empty) car at local x/z, if that spot is in the chunk being generated. */
    protected void parkedCar(WorldGenLevel level, BoundingBox box, double x, double z, float yaw) {
        BlockPos p = at((int) Math.floor(x), 1, (int) Math.floor(z));
        if (!box.isInside(p)) return;
        dev.freerun.vehicle.CarEntity car = new dev.freerun.vehicle.CarEntity(dev.freerun.vehicle.VehicleRegistry.CAR, server(level));
        car.moveTo(ox + x, oy + 1, oz + z, yaw, 0.0f);
        car.setYBodyRot(yaw);
        car.setYHeadRot(yaw);
        car.randomColor();
        car.setCity(city);
        level.addFreshEntity(car);
    }

    /** A store worker at local x/z (feet at y=1), selling from {@code shop} if given. */
    protected dev.freerun.mobs.RoadhouseNpcEntity staff(WorldGenLevel level, BoundingBox box, int x, int z, float yaw,
                                                         dev.freerun.mobs.RoadhouseNpcEntity.Role role,
                                                         dev.freerun.economy.Shops.Shop shop, net.minecraft.world.item.ItemStack held) {
        return staff(level, box, x, 1, z, yaw, role, shop, held);
    }

    protected dev.freerun.mobs.RoadhouseNpcEntity staff(WorldGenLevel level, BoundingBox box, int x, int y, int z, float yaw,
                                                         dev.freerun.mobs.RoadhouseNpcEntity.Role role,
                                                         dev.freerun.economy.Shops.Shop shop, net.minecraft.world.item.ItemStack held) {
        BlockPos p = at(x, y, z);
        if (!box.isInside(p)) return null;
        dev.freerun.mobs.RoadhouseNpcEntity npc = new dev.freerun.mobs.RoadhouseNpcEntity(dev.freerun.mobs.SoldierRegistry.ROADHOUSE_NPC, server(level));
        npc.setRole(role, 0);
        if (shop != null) npc.setShop(shop);
        npc.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, held);
        npc.setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 0.0f);
        npc.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, yaw, 0.0f);
        npc.setYHeadRot(yaw);
        npc.setYBodyRot(yaw);
        npc.setPersistenceRequired();
        level.addFreshEntity(npc);
        return npc;
    }

    // ------------------------------------------------------------------ big block lettering

    private static final java.util.Map<Character, String[]> FONT = new java.util.HashMap<>();

    static {
        String[][] glyphs = {
                {"A", " ## ", "#  #", "####", "#  #", "#  #"}, {"B", "### ", "#  #", "### ", "#  #", "### "},
                {"C", " ###", "#   ", "#   ", "#   ", " ###"}, {"D", "### ", "#  #", "#  #", "#  #", "### "},
                {"E", "####", "#   ", "### ", "#   ", "####"}, {"F", "####", "#   ", "### ", "#   ", "#   "},
                {"G", " ###", "#   ", "# ##", "#  #", " ###"}, {"H", "#  #", "#  #", "####", "#  #", "#  #"},
                {"I", "###", " # ", " # ", " # ", "###"}, {"K", "#  #", "# # ", "##  ", "# # ", "#  #"},
                {"L", "#   ", "#   ", "#   ", "#   ", "####"}, {"M", "#   #", "## ##", "# # #", "#   #", "#   #"},
                {"N", "#  #", "## #", "# ##", "#  #", "#  #"}, {"O", " ## ", "#  #", "#  #", "#  #", " ## "},
                {"P", "### ", "#  #", "### ", "#   ", "#   "}, {"Q", " ## ", "#  #", "#  #", "# ##", " ###"},
                {"R", "### ", "#  #", "### ", "# # ", "#  #"}, {"S", " ###", "#   ", " ## ", "   #", "### "},
                {"T", "###", " # ", " # ", " # ", " # "}, {"U", "#  #", "#  #", "#  #", "#  #", " ## "},
                {"V", "#   #", "#   #", " # # ", " # # ", "  #  "}, {"W", "#   #", "#   #", "# # #", "## ##", "#   #"},
                {"Y", "#   #", " # # ", "  #  ", "  #  ", "  #  "}, {"&", " #  ", "# # ", " #  ", "# ##", " ## "},
                {" ", "  ", "  ", "  ", "  ", "  "}, {"'", "#", "#", " ", " ", " "},
        };
        for (String[] g : glyphs) FONT.put(g[0].charAt(0), java.util.Arrays.copyOfRange(g, 1, 6));
    }

    /** Width in blocks of {@link #bigText} for {@code text}. */
    protected static int textWidth(String text) {
        int w = 0;
        for (char c : text.toUpperCase().toCharArray()) {
            String[] g = FONT.getOrDefault(c, FONT.get(' '));
            w += g[0].length() + 1;
        }
        return Math.max(0, w - 1);
    }

    /**
     * Five-block-tall letters in {@code paint} on the plane of local (x, z), readable by someone
     * standing on the {@code face} side; {@code (x, y)} is the text's top-left as they see it.
     */
    protected void bigText(WorldGenLevel level, BoundingBox box, int x, int y, int z, Direction face, String text, BlockState paint) {
        int ux = face == Direction.NORTH ? -1 : face == Direction.SOUTH ? 1 : 0;
        int uz = face == Direction.EAST ? -1 : face == Direction.WEST ? 1 : 0;
        int col = 0;
        for (char c : text.toUpperCase().toCharArray()) {
            String[] g = FONT.getOrDefault(c, FONT.get(' '));
            for (int row = 0; row < 5; row++) {
                for (int i = 0; i < g[row].length(); i++) {
                    if (g[row].charAt(i) != ' ') put(level, box, paint, x + ux * (col + i), y - row, z + uz * (col + i));
                }
            }
            col += g[0].length() + 1;
        }
    }

    /** Centred {@link #bigText} across a wall that runs from a to b (local, along the text direction). */
    protected void bigTextCentred(WorldGenLevel level, BoundingBox box, int a, int b, int y, int depth, Direction face, String text, BlockState paint) {
        int w = textWidth(text);
        int lo = Math.min(a, b), hi = Math.max(a, b);
        int start = (lo + hi + 1) / 2 - w / 2;   // leftmost along +axis
        int end = start + w - 1;
        switch (face) {
            case NORTH -> bigText(level, box, end, y, depth, face, text, paint);
            case SOUTH -> bigText(level, box, start, y, depth, face, text, paint);
            case EAST -> bigText(level, box, depth, y, end, face, text, paint);
            default -> bigText(level, box, depth, y, start, face, text, paint);
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
