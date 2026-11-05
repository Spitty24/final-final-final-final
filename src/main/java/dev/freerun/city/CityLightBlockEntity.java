package dev.freerun.city;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Drives a traffic light head or a street lamp: twice a second it works out what it should be
 * showing and updates its block state. A traffic light remembers which intersection it belongs to.
 */
public class CityLightBlockEntity extends BlockEntity {
    private BlockPos intersection;

    public CityLightBlockEntity(BlockPos pos, BlockState state) {
        super(CityRegistry.CITY_LIGHT_BE, pos, state);
    }

    public void setIntersection(BlockPos key) {
        intersection = key.immutable();
        setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CityLightBlockEntity be) {
        if (level.isClientSide || Math.floorMod(level.getGameTime() + pos.hashCode(), 10L) != 0) return;
        BlockState next = state;
        if (state.getBlock() instanceof TrafficLightBlock) {
            boolean alongX = state.getValue(TrafficLightBlock.FACING).getAxis() == net.minecraft.core.Direction.Axis.X;
            TrafficSystem.Signal s = TrafficSystem.blackedOut(level, pos) ? TrafficSystem.Signal.RED
                    : TrafficSystem.signal(level, be.intersection != null ? be.intersection : pos, alongX);
            next = state.setValue(TrafficLightBlock.SIGNAL, s.ordinal());
        } else if (state.getBlock() instanceof StreetLampBlock) {
            next = state.setValue(StreetLampBlock.LIT, TrafficSystem.night(level) && !TrafficSystem.blackedOut(level, pos));
        }
        if (next != state) level.setBlock(pos, next, Block.UPDATE_CLIENTS);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (intersection != null) {
            tag.putInt("IX", intersection.getX());
            tag.putInt("IY", intersection.getY());
            tag.putInt("IZ", intersection.getZ());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("IX")) intersection = new BlockPos(tag.getInt("IX"), tag.getInt("IY"), tag.getInt("IZ"));
    }

    public BlockPos intersection() {
        return intersection;
    }
}
