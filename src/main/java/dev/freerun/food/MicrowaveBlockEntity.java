package dev.freerun.food;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Whatever is going round on the microwave's turntable. */
public class MicrowaveBlockEntity extends BlockEntity {
    private ItemStack inside = ItemStack.EMPTY;

    public MicrowaveBlockEntity(BlockPos pos, BlockState state) {
        super(FoodRegistry.MICROWAVE_BE, pos, state);
    }

    public ItemStack inside() {
        return inside;
    }

    public void setInside(ItemStack stack) {
        inside = stack;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!inside.isEmpty()) tag.put("Inside", inside.save(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inside = tag.contains("Inside") ? ItemStack.parseOptional(registries, tag.getCompound("Inside")) : ItemStack.EMPTY;
    }
}
