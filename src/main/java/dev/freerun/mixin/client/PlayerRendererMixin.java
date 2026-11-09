package dev.freerun.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.freerun.alien.Form;
import dev.freerun.client.alien.AlienRenderers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
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

    /** A player wearing a Swampwatch form is drawn as the creature instead. */
    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), cancellable = true)
    private void freerun$renderForm(AbstractClientPlayer player, float entityYaw, float partialTick, PoseStack poseStack,
                                    MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        Form f = AlienRenderers.formOf(player);
        if (f == null) return;
        AlienRenderers.render(player, f, partialTick, poseStack, buffer, packedLight);
        var mc = net.minecraft.client.Minecraft.getInstance();
        boolean self = mc.getCameraEntity() == player && !mc.options.getCameraType().isFirstPerson();
        boolean show = net.minecraft.client.Minecraft.renderNames() && player != mc.getCameraEntity() && !player.isInvisibleTo(mc.player)
                && player.distanceToSqr(mc.getCameraEntity()) < 64 * 64;
        if (show || self && dev.freerun.client.SecondPerson.active()) renderNameTag(player, player.getDisplayName(), poseStack, buffer, packedLight, partialTick);
        ci.cancel();
    }

    @Inject(method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V",
            at = @At("TAIL"))
    private void freerun$bodyMotion(AbstractClientPlayer player, PoseStack poseStack, float bob, float yBodyRot,
                                    float partialTick, float scale, CallbackInfo ci) {
        if (dev.freerun.client.skate.SkateRender.state(player) != null) {
            dev.freerun.client.skate.SkateRender.body(player, poseStack, scale, partialTick);
            return;
        }
        ThirdPersonAnimator.applyBody(player, poseStack, partialTick, scale);
    }

    @org.spongepowered.asm.mixin.Unique
    private boolean freerun$skatePushed;

    /** A skater: the board under the feet, and the rider stood on it. */
    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"))
    private void freerun$skateBoard(AbstractClientPlayer player, float entityYaw, float partialTick, PoseStack poseStack,
                                    MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        freerun$skatePushed = false;
        if (AlienRenderers.formOf(player) != null) return;
        if (!dev.freerun.client.skate.SkateRender.renderBoard(player, partialTick, poseStack, buffer, packedLight)) return;
        poseStack.pushPose();
        dev.freerun.client.skate.SkateRender.lift(player, poseStack);
        freerun$skatePushed = true;
    }

    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN"))
    private void freerun$skateBoardEnd(AbstractClientPlayer player, float entityYaw, float partialTick, PoseStack poseStack,
                                       MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (freerun$skatePushed) poseStack.popPose();
        freerun$skatePushed = false;
    }
}
