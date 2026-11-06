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
        float slap = dev.freerun.client.alien.AlienRenderers.slap(player, partialTicks);
        if (slap >= 0) {
            dev.freerun.client.alien.WatchAnimation.firstPerson(poseStack, buffer, player, light, slap, partialTicks);
            FirstPersonArms.renderingHands = false;
            ci.cancel();
            return;
        }
        dev.freerun.alien.Alien form = dev.freerun.alien.Transformations.alienOf(player);
        if (form != null) {
            dev.freerun.client.alien.AlienRenderers.renderFirstPerson(form, poseStack, buffer, player, light, partialTicks);
            FirstPersonArms.renderingHands = false;
            ci.cancel();
            return;
        }
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
