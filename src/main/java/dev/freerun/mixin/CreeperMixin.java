package dev.freerun.mixin;

import dev.freerun.chaos.CreeperPodcast;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Creepers have a podcast now. They interview you instead of exploding (mostly). */
@Mixin(Creeper.class)
public abstract class CreeperMixin {
    @Inject(method = "explodeCreeper", at = @At("HEAD"), cancellable = true)
    private void freerun$podcast(CallbackInfo ci) {
        Creeper self = (Creeper) (Object) this;
        if (!CreeperPodcast.mayExplode(self) && CreeperPodcast.intercept(self)) ci.cancel();
    }
}
