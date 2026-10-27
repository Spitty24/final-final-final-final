package dev.freerun.mixin;

import dev.freerun.menu.BigCraftingMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Makes the vanilla crafting table open the 3x4 crafting menu instead of the normal 3x3 one. */
@Mixin(CraftingTableBlock.class)
public abstract class CraftingTableBlockMixin {

    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void freerun$openBigCrafting(BlockState state, Level level, BlockPos pos, Player player,
                                         BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (!level.isClientSide) {
            player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new BigCraftingMenu(id, inv, ContainerLevelAccess.create(level, pos)),
                    Component.translatable("container.crafting")));
            player.awardStat(Stats.INTERACT_WITH_CRAFTING_TABLE);
        }
        cir.setReturnValue(InteractionResult.sidedSuccess(level.isClientSide()));
    }
}
