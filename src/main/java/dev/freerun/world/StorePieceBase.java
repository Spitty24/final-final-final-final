package dev.freerun.world;

import dev.freerun.mobs.RoadhouseNpcEntity;
import dev.freerun.mobs.SoldierRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

/**
 * Shared helpers for the store pieces. Local x/z map straight onto world x/z (+x east, +z south),
 * and blocks are written exactly as given (no structure mirroring), so a block placed facing
 * NORTH really faces north.
 */
abstract class StorePieceBase extends StructurePiece {
    protected final int ox, oy, oz;

    protected StorePieceBase(StructurePieceType type, BlockPos origin, int w, int h, int d) {
        super(type, 0, new BoundingBox(origin.getX(), origin.getY() - 1, origin.getZ(),
                origin.getX() + w - 1, origin.getY() + h, origin.getZ() + d - 1));
        ox = origin.getX();
        oy = origin.getY();
        oz = origin.getZ();
    }

    protected StorePieceBase(StructurePieceType type, CompoundTag tag) {
        super(type, tag);
        ox = boundingBox.minX();
        oy = boundingBox.minY() + 1;
        oz = boundingBox.minZ();
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
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

    protected void fill(WorldGenLevel level, BoundingBox box, BlockState state, int x0, int y0, int z0, int x1, int y1, int z1) {
        for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) put(level, box, state, x, y, z);
    }

    protected void fill(WorldGenLevel level, BoundingBox box, Block block, int x0, int y0, int z0, int x1, int y1, int z1) {
        fill(level, box, block.defaultBlockState(), x0, y0, z0, x1, y1, z1);
    }

    protected static BlockState facing(Block b, Direction d) {
        return b.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, d);
    }

    protected void sign(WorldGenLevel level, BoundingBox box, int x, int y, int z, Direction facing, String... lines) {
        BlockPos p = at(x, y, z);
        if (!box.isInside(p)) return;
        level.setBlock(p, Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, facing), Block.UPDATE_CLIENTS);
        AnnexHelpers.signText(level, p, lines);
    }

    /** Clears the footprint up to the roof and lays a floor. */
    protected void clear(WorldGenLevel level, BoundingBox box, int w, int h, int d, BlockState ground) {
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                put(level, box, Blocks.STONE.defaultBlockState(), x, -1, z);
                put(level, box, ground, x, 0, z);
                for (int y = 1; y <= h; y++) put(level, box, Blocks.AIR.defaultBlockState(), x, y, z);
            }
        }
    }

    protected void staff(WorldGenLevel level, BoundingBox box, int x, int z, float yaw, RoadhouseNpcEntity.Role role, ItemStack held) {
        BlockPos p = at(x, 1, z);
        if (!box.isInside(p)) return;
        RoadhouseNpcEntity npc = new RoadhouseNpcEntity(SoldierRegistry.ROADHOUSE_NPC, level.getLevel());
        npc.setRole(role, 0);
        npc.setItemSlot(EquipmentSlot.MAINHAND, held);
        npc.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
        npc.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, yaw, 0.0f);
        npc.setYHeadRot(yaw);
        npc.setYBodyRot(yaw);
        level.addFreshEntityWithPassengers(npc);
    }

    protected void car(WorldGenLevel level, BoundingBox box, double x, double z, float yaw) {
        BlockPos p = at((int) Math.floor(x), 1, (int) Math.floor(z));
        if (!box.isInside(p)) return;
        dev.freerun.vehicle.CarEntity car = new dev.freerun.vehicle.CarEntity(dev.freerun.vehicle.VehicleRegistry.CAR, level.getLevel());
        car.moveTo(ox + x, oy + 1, oz + z, yaw, 0.0f);
        car.setYBodyRot(yaw);
        car.setYHeadRot(yaw);
        car.setColor(new int[]{0xB3202A, 0x1E4FA8, 0xEDEDED, 0x1C1C1E, 0x9EA3A8, 0x2F6B3A}[Math.floorMod((int) (x * 7 + z * 13), 6)]);
        level.addFreshEntity(car);
    }
}
