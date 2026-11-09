package dev.freerun.mixin;

import dev.freerun.chaos.ReverseMode;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reverse Mode: saplings grow upside down, leaves first. */
@Mixin(SaplingBlock.class)
public abstract class SaplingBlockMixin {
    @Inject(method = "advanceTree(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void freerun$upsideDown(ServerLevel level, BlockPos pos, BlockState state, RandomSource random, CallbackInfo ci) {
        if (state.getValue(SaplingBlock.STAGE) == 1 && ReverseMode.growUpsideDown(level, pos, state)) ci.cancel();
    }
}
