package dev.freerun.mixin.client;

import dev.freerun.client.FirstPersonArms;
import dev.freerun.client.ThirdPersonAnimator;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void freerun$animate(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                                 float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (FirstPersonArms.renderingHands || !(entity instanceof Player player)) return;
        ThirdPersonAnimator.applyLimbs((PlayerModel<?>) (Object) this, player, ageInTicks - player.tickCount);
        float slap = dev.freerun.client.alien.AlienRenderers.slap(player, ageInTicks - player.tickCount);
        if (slap >= 0) dev.freerun.client.alien.WatchAnimation.slapPose((PlayerModel<?>) (Object) this, slap);
    }
}
