package dev.freerun.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.freerun.FreerunConfig;
import dev.freerun.ParkourState;
import dev.freerun.client.AnimState;
import dev.freerun.client.ClientClock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** First-person camera effects: tilt away from the wall while wall running, forward flip on a roll. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = "bobHurt", at = @At("TAIL"))
    private void freerun$camera(PoseStack poseStack, float partialTicks, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        FreerunConfig cfg = FreerunConfig.get();
        if (mc.player == null || !cfg.enabled || !mc.options.getCameraType().isFirstPerson()) return;

        AnimState a = AnimState.get(mc.player.getUUID());
        float now = ClientClock.now(partialTicks);
        float blend = a.blend(now);
        float roll = blend < 1f
                ? net.minecraft.util.Mth.lerp(blend, tilt(a.prev, cfg), tilt(a.state, cfg))
                : tilt(a.state, cfg);
        if (roll != 0f) poseStack.mulPose(Axis.ZP.rotationDegrees(roll));

        if (cfg.cameraRollFlip && a.state == ParkourState.ROLL) {
            float p = AnimState.ease(AnimState.progress(ParkourState.ROLL, a.age(now)));
            poseStack.mulPose(Axis.XP.rotationDegrees(360f * p));
        }
    }

    private static float tilt(ParkourState s, FreerunConfig cfg) {
        return switch (s) {
            case WALLRUN_RIGHT -> -cfg.cameraTilt;
            case WALLRUN_LEFT -> cfg.cameraTilt;
            default -> 0f;
        };
    }
}
