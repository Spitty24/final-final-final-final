package dev.freerun.mixin.client;

import dev.freerun.client.chaos.ChaosClient;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Tunnel vision while Locked In. */
@Mixin(AbstractClientPlayer.class)
public abstract class FovMixin {
    @Inject(method = "getFieldOfViewModifier", at = @At("RETURN"), cancellable = true, require = 0)
    private void freerun$tunnel(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(ChaosClient.fov(cir.getReturnValueF()));
    }
}
