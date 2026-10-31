package dev.freerun.world;

import dev.freerun.mobs.SoldierRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * An invisible, tiny entity that sits inside a chair so players and NPCs can ride it (i.e. sit).
 * Removes itself as soon as nobody is sitting on it, or if its chair is broken.
 */
public class SeatEntity extends Entity {

    public SeatEntity(EntityType<? extends SeatEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    /** A seat positioned on the cushion of the chair at {@code pos}. */
    public static SeatEntity create(Level level, BlockPos pos) {
        SeatEntity seat = new SeatEntity(SoldierRegistry.SEAT, level);
        seat.setPos(pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5);
        return seat;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            boolean chairGone = !(level().getBlockState(blockPosition()).getBlock() instanceof ChairBlock);
            if (getPassengers().isEmpty() || chairGone) {
                ejectPassengers();
                discard();
            }
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
