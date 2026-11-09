package dev.freerun.mixin.client;

import dev.freerun.client.skate.SkateClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** On the board the mouse buttons are grabs, so they don't punch or place. */
@Mixin(Minecraft.class)
public abstract class MinecraftSkateMixin {
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void freerun$noAttack(CallbackInfoReturnable<Boolean> cir) {
        if (SkateClient.active()) cir.setReturnValue(false);
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void freerun$noUse(CallbackInfo ci) {
        if (SkateClient.active()) ci.cancel();
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void freerun$noMine(boolean leftClick, CallbackInfo ci) {
        if (SkateClient.active()) ci.cancel();
    }
}
