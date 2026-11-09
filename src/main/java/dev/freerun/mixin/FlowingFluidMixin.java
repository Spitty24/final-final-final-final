package dev.freerun.mixin;

import dev.freerun.chaos.ReverseMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.WaterFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Reverse Mode: a lone pool or a bucket of water runs up instead of down (a few blocks, then spills over). */
@Mixin(FlowingFluid.class)
public abstract class FlowingFluidMixin {
    @Redirect(method = "spread(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/material/FluidState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;below()Lnet/minecraft/core/BlockPos;"), require = 0)
    private BlockPos freerun$flowUp(BlockPos pos, Level level, BlockPos origin, FluidState state) {
        if ((Object) this instanceof WaterFluid && ReverseMode.flowsUp(level, pos)) return pos.above();
        return pos.below();
    }

    @Redirect(method = "getNewLiquid(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/level/material/FluidState;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;above()Lnet/minecraft/core/BlockPos;"), require = 0)
    private BlockPos freerun$fedFromBelow(BlockPos pos, Level level, BlockPos origin, BlockState state) {
        if ((Object) this instanceof WaterFluid && ReverseMode.fedFromBelow(level, pos) > 0) return pos.below();
        return pos.above();
    }
}
