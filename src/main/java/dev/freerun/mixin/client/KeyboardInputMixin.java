package dev.freerun.mixin.client;

import dev.freerun.client.skate.SkateClient;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** On the board the keys are read by the skate controller, not walked with. */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {
    @Inject(method = "tick", at = @At("TAIL"))
    private void freerun$skate(boolean slow, float slowFactor, CallbackInfo ci) {
        if (!SkateClient.active()) return;
        up = down = left = right = false;
        forwardImpulse = 0;
        leftImpulse = 0;
        jumping = false;
        shiftKeyDown = false;
    }
}
