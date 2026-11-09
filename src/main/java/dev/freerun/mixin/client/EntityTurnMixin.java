package dev.freerun.mixin.client;

import dev.freerun.client.skate.SkateClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** On the board the mouse is the flick stick: it moves the stick instead of the camera. */
@Mixin(Entity.class)
public abstract class EntityTurnMixin {
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void freerun$flickStick(double yRot, double xRot, CallbackInfo ci) {
        if (!SkateClient.active() || (Object) this != Minecraft.getInstance().player) return;
        SkateClient.mouse(yRot, xRot);
        ci.cancel();
    }
}
