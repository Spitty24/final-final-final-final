package dev.freerun.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.freerun.alien.Form;
import dev.freerun.client.alien.AlienRenderers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Shadow;
import dev.freerun.client.ThirdPersonAnimator;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
    @Shadow
    protected abstract void renderNameTag(AbstractClientPlayer entity, Component displayName, PoseStack poseStack, MultiBufferSource buffer, int packedLight, float partialTick);

    @Shadow
    protected abstract boolean shouldShowName(LivingEntity entity);

    /** A player wearing a Swampwatch form is drawn as the creature instead. */
    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), cancellable = true)
    private void freerun$renderForm(AbstractClientPlayer player, float entityYaw, float partialTick, PoseStack poseStack,
                                    MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        Form f = AlienRenderers.formOf(player);
        if (f == null) return;
        AlienRenderers.render(player, f, partialTick, poseStack, buffer, packedLight);
        if (shouldShowName(player)) renderNameTag(player, player.getDisplayName(), poseStack, buffer, packedLight, partialTick);
        ci.cancel();
    }

    @Inject(method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V",
            at = @At("TAIL"))
    private void freerun$bodyMotion(AbstractClientPlayer player, PoseStack poseStack, float bob, float yBodyRot,
                                    float partialTick, float scale, CallbackInfo ci) {
        ThirdPersonAnimator.applyBody(player, poseStack, partialTick, scale);
    }
}
