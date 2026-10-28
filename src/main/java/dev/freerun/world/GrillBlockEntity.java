package dev.freerun.world;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SmokerMenu;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Grill's block entity. Cooks food like a smoker (uses smoking recipes), but 25% faster —
 * the speed boost is applied by {@code AbstractFurnaceBlockEntityMixin}. Reuses the vanilla
 * smoker menu/screen so no custom GUI is needed.
 */
public class GrillBlockEntity extends AbstractFurnaceBlockEntity {
    public GrillBlockEntity(BlockPos pos, BlockState state) {
        super(BlockRegistry.GRILL_BE, pos, state, RecipeType.SMOKING);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.freerun.grill");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inv) {
        return new SmokerMenu(id, inv, this, this.dataAccess);
    }
}
