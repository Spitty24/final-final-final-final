package dev.freerun.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.freerun.client.FirstPersonArms;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void freerun$parkourArms(float partialTicks, PoseStack poseStack, MultiBufferSource.BufferSource buffer,
                                     LocalPlayer player, int light, CallbackInfo ci) {
        FirstPersonArms.renderingHands = true;
        if (FirstPersonArms.render(poseStack, buffer, player, light, partialTicks)) {
            FirstPersonArms.renderingHands = false;
            ci.cancel();
        }
    }

    @Inject(method = "renderHandsWithItems", at = @At("RETURN"))
    private void freerun$done(float partialTicks, PoseStack poseStack, MultiBufferSource.BufferSource buffer,
                              LocalPlayer player, int light, CallbackInfo ci) {
        FirstPersonArms.renderingHands = false;
    }
}
