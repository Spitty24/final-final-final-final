package dev.freerun.mixin.client;

import dev.freerun.client.SecondPerson;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Moves the camera for the second-person view. */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Inject(method = "setup", at = @At("TAIL"))
    private void freerun$secondPerson(BlockGetter level, Entity entity, boolean detached, boolean mirrored, float partial,
                                      CallbackInfo ci) {
        double[] c = SecondPerson.camera(Minecraft.getInstance(), partial);
        if (c == null) return;
        setRotation((float) c[3], (float) c[4]);
        setPosition(c[0], c[1], c[2]);
    }
}
