package dev.freerun.client.alien;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.freerun.alien.SwampwatchItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;

/**
 * Using the watch, before the change: the left wrist comes up across the chest, the dial springs
 * open, the right hand comes down on it, and then the light takes over.
 *
 *   0.00 - 0.45  left forearm swings up and across, palm in, watch facing you
 *   0.30 - 0.70  right hand lifts high
 *   0.70 - 0.80  slap
 *   0.80 - 1.00  both hands hold as the dial flares
 */
public final class WatchAnimation {
    private WatchAnimation() {}

    private static float seg(float t, float a, float b) {
        return Mth.clamp((t - a) / (b - a), 0, 1);
    }

    private static float ease(float x) {
        return 1 - (1 - x) * (1 - x) * (1 - x);
    }

    /** Third person: pose the arms on the player model. */
    public static void slapPose(PlayerModel<?> m, float t) {
        float up = ease(seg(t, 0.0f, 0.45f));
        m.leftArm.xRot = Mth.lerp(up, m.leftArm.xRot, -1.45f);
        m.leftArm.yRot = Mth.lerp(up, m.leftArm.yRot, 0.75f);
        m.leftArm.zRot = Mth.lerp(up, m.leftArm.zRot, 0.0f);
        float lift = ease(seg(t, 0.3f, 0.7f)), hit = ease(seg(t, 0.7f, 0.8f));
        m.rightArm.xRot = Mth.lerp(lift, m.rightArm.xRot, -2.6f) + hit * 1.15f;
        m.rightArm.yRot = Mth.lerp(lift, m.rightArm.yRot, -0.55f);
        m.rightArm.zRot = 0;
        m.leftSleeve.copyFrom(m.leftArm);
        m.rightSleeve.copyFrom(m.rightArm);
        m.head.xRot = Mth.lerp(up, m.head.xRot, 0.45f);   // looking down at the dial
        m.hat.copyFrom(m.head);
    }

    /** First person: both arms in front of the camera doing the same thing. */
    public static void firstPerson(PoseStack ps, MultiBufferSource buffers, LocalPlayer p, int light, float t, float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.getEntityRenderDispatcher().getRenderer(p) instanceof PlayerRenderer r)) return;
        float up = ease(seg(t, 0.0f, 0.45f));
        float lift = ease(seg(t, 0.3f, 0.7f)), hit = ease(seg(t, 0.7f, 0.8f));
        // the left forearm, rising across the view with the watch toward the camera
        ps.pushPose();
        ps.translate(Mth.lerp(up, -0.9f, -0.18f), Mth.lerp(up, -1.2f, -0.45f), -0.75f);
        ps.mulPose(Axis.XP.rotationDegrees(-15));
        ps.mulPose(Axis.YP.rotationDegrees(Mth.lerp(up, 0, 70)));
        ps.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(up, 0, -80)));
        r.renderLeftHand(ps, buffers, light, (AbstractClientPlayer) p);
        // the watch on that arm (arm space: pixels, rotated like the model arm)
        ps.pushPose();
        ps.translate(0.33f, 0.05f, 0.0f);   // to the arm's pivot, as renderLeftHand sets it
        ps.scale(1 / 16f, 1 / 16f, 1 / 16f);
        ps.mulPose(Axis.XP.rotationDegrees(180));
        ps.translate(1, -2, 0);
        AlienRenderers.watchOnArm(ps, buffers, 0xF000F0, t, true, p.tickCount + partial);
        ps.popPose();
        ps.popPose();
        // the right hand: up, then down onto the dial
        ps.pushPose();
        float y = Mth.lerp(lift, -1.2f, -0.05f) - hit * 0.35f;
        ps.translate(Mth.lerp(lift, 0.9f, 0.25f) - hit * 0.12f, y, -0.85f + hit * 0.1f);
        ps.mulPose(Axis.XP.rotationDegrees(Mth.lerp(lift, 0, -60) + hit * 40));
        ps.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(lift, 0, 25)));
        r.renderRightHand(ps, buffers, light, (AbstractClientPlayer) p);
        ps.popPose();
    }

    /** Whether this player wears the watch (anywhere in their inventory). */
    public static boolean wearing(AbstractClientPlayer p) {
        return !SwampwatchItem.find(p).isEmpty();
    }
}
