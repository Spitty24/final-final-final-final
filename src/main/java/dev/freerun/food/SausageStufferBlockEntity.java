package dev.freerun.food;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Holds the filler (crop) loaded into a sausage stuffer's hopper. */
public class SausageStufferBlockEntity extends BlockEntity {
    private ItemStack filler = ItemStack.EMPTY;

    public SausageStufferBlockEntity(BlockPos pos, BlockState state) {
        super(FoodRegistry.STUFFER_BE, pos, state);
    }

    public ItemStack filler() {
        return filler;
    }

    public void setFiller(ItemStack stack) {
        filler = stack;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!filler.isEmpty()) tag.put("Filler", filler.save(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        filler = tag.contains("Filler") ? ItemStack.parseOptional(registries, tag.getCompound("Filler")) : ItemStack.EMPTY;
    }
}
