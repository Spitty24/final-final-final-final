package dev.freerun.client.skate;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.freerun.skate.SkatePayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Draws skaters: the board under their feet, the stance, the poses, and the skate camera. */
public final class SkateRender {
    /** Other players' boards, from the server relay. */
    public static final Map<Integer, SkatePayloads.State> REMOTE = new ConcurrentHashMap<>();
    /** How far the rider stands above the ground on the deck. */
    public static final float LIFT = 0.19f;

    private static float camYaw = Float.NaN, camYawO;
    private static double camDist = 3.4;
    private static long lastNs;

    private SkateRender() {}

    public static SkatePayloads.State state(Player p) {
        Minecraft mc = Minecraft.getInstance();
        if (p == mc.player) return SkateClient.active() ? SkateClient.localState(p.getId()) : null;
        SkatePayloads.State s = REMOTE.get(p.getId());
        return s != null && s.on() ? s : null;
    }

    // ------------------------------------------------------------------ the board

    /** At the start of PlayerRenderer#render: the board at the feet (the pose stack is at the feet). */
    public static boolean renderBoard(Player p, float partial, PoseStack ps, MultiBufferSource buffers, int light) {
        SkatePayloads.State s = state(p);
        if (s == null) return false;
        boolean bail = s.pose() == SkatePayloads.BAIL;
        ps.pushPose();
        if (bail && p == Minecraft.getInstance().player) {
            // the board skitters off by itself
            Vec3 at = SkateClient.bailBoard(partial);
            Vec3 me = p.getPosition(partial);
            ps.translate(at.x - me.x, at.y - me.y, at.z - me.z);
            ps.mulPose(Axis.YP.rotationDegrees(180 - s.heading() + SkateClient.bailBoardSpin()));
            ps.mulPose(Axis.ZP.rotationDegrees(SkateClient.bailTicks() * 23 % 360 > 180 ? 180 : 0));
        } else if (!bail) {
            float roll = s.roll(), spin = s.spin();
            ps.mulPose(Axis.YP.rotationDegrees(180 - s.heading() - spin));
            // a flipping board rises up to the feet and spins on its long axis around its middle
            if (roll != 0) {
                ps.translate(0, 0.3f * Mth.sin(Math.abs(roll) % 360 / 360f * Mth.PI), 0);
                ps.translate(0, SkateboardModel.HEIGHT * 0.5f, 0);
                ps.mulPose(Axis.ZP.rotationDegrees(roll));
                ps.translate(0, -SkateboardModel.HEIGHT * 0.5f, 0);
            }
            if (s.pitch() != 0) {
                // manuals pivot on the back (or front) wheels
                float pivot = s.pitch() > 0 ? 0.19f : -0.19f;
                ps.translate(0, 0, pivot);
                ps.mulPose(Axis.XP.rotationDegrees(s.pitch()));
                ps.translate(0, 0, -pivot);
            }
            // carving: the deck leans on its trucks
            ps.mulPose(Axis.ZP.rotationDegrees(-s.lean() * 6));
        } else {
            ps.popPose();
            return true;   // someone else's bail: their board is lying somewhere; just don't draw it on their feet
        }
        SkateboardModel.render(ps, buffers::getBuffer, s.deck(), light, 0);
        ps.popPose();
        return true;
    }

    /** After the board: stand the rider on the deck. */
    public static void lift(Player p, PoseStack ps) {
        SkatePayloads.State s = state(p);
        if (s == null || s.pose() == SkatePayloads.BAIL) return;
        float y = LIFT;
        if (s.pitch() != 0) y += 0.05f;
        ps.translate(0, y, 0);
    }

    /** After PlayerRenderer#setupRotations: turn sideways on the board, and with any air spin. */
    public static void body(Player p, PoseStack ps, float scale) {
        SkatePayloads.State s = state(p);
        if (s == null) return;
        // setupRotations has already turned us to yBodyRot (= heading); stand side-on (regular: left foot forward)
        ps.mulPose(Axis.YP.rotationDegrees(-90 - s.body()));
        switch (s.pose()) {
            case SkatePayloads.CROUCH, SkatePayloads.AIR, SkatePayloads.GRAB_L, SkatePayloads.GRAB_R, SkatePayloads.GRAB_BOTH ->
                    ps.translate(0, -0.18f * scale, 0);
            case SkatePayloads.GRIND, SkatePayloads.BOARDSLIDE, SkatePayloads.POWERSLIDE -> ps.translate(0, -0.1f * scale, 0);
            case SkatePayloads.BAIL -> {
                // tumble
                float t = p == Minecraft.getInstance().player ? SkateClient.bailTicks() : 0;
                ps.translate(0, 0.4f * scale, 0);
                ps.mulPose(Axis.XP.rotationDegrees(t > 22 ? (34 - t) * 7.5f : 90));
                ps.translate(0, -0.4f * scale, 0);
                ps.translate(0, t > 22 ? 0 : -0.55f * scale, 0);
            }
            default -> {
            }
        }
        // lean into the carve (the body leans across its own width, which is along the board now)
        if (s.pose() != SkatePayloads.BAIL) ps.mulPose(Axis.XP.rotationDegrees(s.lean() * 10));
    }

    // ------------------------------------------------------------------ poses

    public static void limbs(PlayerModel<?> m, Player p, float ageInTicks) {
        SkatePayloads.State s = state(p);
        if (s == null) return;
        // the board runs along the model's X axis: feet apart, toes on one side
        m.rightLeg.xRot = 0;
        m.leftLeg.xRot = 0;
        m.rightLeg.yRot = 0;
        m.leftLeg.yRot = 0;
        m.rightLeg.zRot = 0.28f;
        m.leftLeg.zRot = -0.28f;
        m.rightArm.xRot = 0;
        m.leftArm.xRot = 0;
        m.rightArm.zRot = 0.55f + 0.05f * Mth.sin(ageInTicks * 0.1f);
        m.leftArm.zRot = -0.55f - 0.05f * Mth.sin(ageInTicks * 0.1f);
        m.body.xRot = 0;
        // look where we're going: the head turns back toward the nose (the model's left)
        m.head.yRot = 1.2f;
        m.head.xRot = 0.15f;
        switch (s.pose()) {
            case SkatePayloads.PUSH -> {
                // back foot off the board, kicking
                float k = Mth.sin(ageInTicks * 0.45f);
                m.rightLeg.zRot = 0.05f;
                m.rightLeg.xRot = 0.6f * k;
                m.rightLeg.yRot = 0;
                m.body.xRot = 0.15f;
                m.leftArm.zRot = -0.3f;
                m.rightArm.xRot = -0.4f * k;
            }
            case SkatePayloads.CROUCH -> crouch(m, 0.5f);
            case SkatePayloads.AIR -> {
                crouch(m, 0.7f);
                m.rightArm.zRot = 1.0f;
                m.leftArm.zRot = -1.0f;
            }
            case SkatePayloads.GRAB_L, SkatePayloads.GRAB_R, SkatePayloads.GRAB_BOTH -> {
                crouch(m, 0.9f);
                if (s.pose() != SkatePayloads.GRAB_R) {   // indy: back hand to the toe edge
                    m.rightArm.xRot = -0.9f;
                    m.rightArm.zRot = 0.2f;
                } else m.rightArm.zRot = 1.3f;
                if (s.pose() != SkatePayloads.GRAB_L) {   // melon: front hand to the heel edge
                    m.leftArm.xRot = 0.9f;
                    m.leftArm.zRot = -0.2f;
                } else m.leftArm.zRot = -1.3f;
            }
            case SkatePayloads.MANUAL -> {
                m.rightLeg.zRot = 0.1f;
                m.leftLeg.zRot = -0.45f;
                m.body.zRot = -0.1f;
                m.rightArm.zRot = 1.2f;
                m.leftArm.zRot = -0.9f;
            }
            case SkatePayloads.NOSE_MANUAL -> {
                m.leftLeg.zRot = -0.1f;
                m.rightLeg.zRot = 0.45f;
                m.body.zRot = 0.1f;
                m.rightArm.zRot = 0.9f;
                m.leftArm.zRot = -1.2f;
            }
            case SkatePayloads.GRIND, SkatePayloads.BOARDSLIDE -> {
                crouch(m, 0.4f);
                m.rightArm.zRot = 1.35f;
                m.leftArm.zRot = -1.35f;
                m.rightArm.xRot = -0.2f;
                m.leftArm.xRot = 0.2f;
            }
            case SkatePayloads.POWERSLIDE -> {
                crouch(m, 0.5f);
                m.body.xRot = -0.2f;
                m.rightArm.xRot = -0.6f;
                m.leftArm.xRot = -0.6f;
            }
            case SkatePayloads.BAIL -> {
                m.rightArm.xRot = -2.6f;
                m.leftArm.xRot = -2.4f;
                m.rightArm.zRot = 0.4f;
                m.leftArm.zRot = -0.4f;
                m.rightLeg.xRot = 0.5f * Mth.sin(ageInTicks * 0.9f);
                m.leftLeg.xRot = -0.5f * Mth.sin(ageInTicks * 0.9f);
                m.head.yRot = 0;
            }
            default -> {
            }
        }
        m.hat.copyFrom(m.head);
        m.rightSleeve.copyFrom(m.rightArm);
        m.leftSleeve.copyFrom(m.leftArm);
        m.rightPants.copyFrom(m.rightLeg);
        m.leftPants.copyFrom(m.leftLeg);
        m.jacket.copyFrom(m.body);
    }

    /** Knees bent: no knees on a Minecraft leg, so splay the legs and bend at the waist. */
    private static void crouch(PlayerModel<?> m, float k) {
        m.body.xRot = 0.35f * k;
        m.rightLeg.zRot = 0.28f + 0.3f * k;
        m.leftLeg.zRot = -0.28f - 0.3f * k;
        m.head.xRot = 0.15f - 0.25f * k;
    }

    // ------------------------------------------------------------------ the camera

    /** A low chase cam that swings round behind the board. Returns {x, y, z, yaw, pitch} or null. */
    public static double[] camera(Minecraft mc, float partial) {
        if (!SkateClient.camera() || mc.player == null || mc.options.getCameraType().isFirstPerson()) {
            camYaw = Float.NaN;
            return null;
        }
        Player p = mc.player;
        float target = p.getYRot();
        if (Float.isNaN(camYaw)) camYaw = camYawO = target;
        long now = System.nanoTime();
        float dt = lastNs == 0 ? 0.016f : Math.min(0.1f, (now - lastNs) / 1e9f);
        lastNs = now;
        float yaw = camYaw + Mth.wrapDegrees(target - camYaw) * (1 - (float) Math.exp(-dt * 7));
        camYawO = camYaw;
        camYaw = yaw;
        boolean front = mc.options.getCameraType() == net.minecraft.client.CameraType.THIRD_PERSON_FRONT;
        float useYaw = front ? yaw + 180 : yaw;
        Vec3 feet = p.getPosition(partial);
        Vec3 focus = feet.add(0, 1.1, 0);
        double want = 3.2 + Math.min(1.5, SkateClient.speed() * 2.5);
        camDist = Mth.lerp(1 - Math.exp(-dt * 4), camDist, want);
        Vec3 back = SkateClient.dir(useYaw).scale(-camDist);
        Vec3 eye = focus.add(back.x, 0.55, back.z);
        HitResult hit = mc.level.clip(new ClipContext(focus, eye, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, p));
        if (hit.getType() != HitResult.Type.MISS) eye = hit.getLocation().add(focus.subtract(hit.getLocation()).normalize().scale(0.2));
        Vec3 d = focus.subtract(eye);
        float pitch = (float) (-Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * Mth.RAD_TO_DEG) + 4;
        return new double[]{eye.x, eye.y, eye.z, useYaw, pitch};
    }
}
