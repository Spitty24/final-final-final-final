package dev.freerun.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.freerun.ParkourState;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * Third-person animation: per-limb poses (applied after vanilla setupAnim) plus whole-body
 * transforms (tilts, the landing roll, leaning back into a slide).
 *
 * Limb angles are radians in model space: xRot negative = swing forward/up,
 * right arm zRot positive = out to the side (left arm mirrored).
 * Tweak the numbers in {@link #limbPose} to change how moves look.
 */
public final class ThirdPersonAnimator {
    private static final int RA = 0, LA = 3, RL = 6, LL = 9, BODY = 12;

    private ThirdPersonAnimator() {}

    // ------------------------------------------------------------------ limbs

    public static void applyLimbs(PlayerModel<?> m, Player player, float partialTick) {
        AnimState a = AnimState.peek(player.getUUID());
        if (a == null) return;
        float now = ClientClock.now(partialTick);
        float blend = a.blend(now);
        if (a.state == ParkourState.NONE && blend >= 1f) return;

        float[] vanilla = capture(m);
        float[] out = limbPose(a.state, a.age(now), vanilla);
        if (blend < 1f) out = lerp(limbPose(a.prev, a.prevAge(), vanilla), out, blend);
        write(m, out);

        // keep the outer skin layer glued to the limbs
        m.rightSleeve.copyFrom(m.rightArm);
        m.leftSleeve.copyFrom(m.leftArm);
        m.rightPants.copyFrom(m.rightLeg);
        m.leftPants.copyFrom(m.leftLeg);
        m.jacket.copyFrom(m.body);
    }

    private static float[] limbPose(ParkourState s, float age, float[] vanilla) {
        float[] p = vanilla.clone();
        float prog = AnimState.progress(s, age);
        float arc = Mth.sin(prog * Mth.PI);
        switch (s) {
            case NONE -> { }
            case WALLRUN_RIGHT, WALLRUN_LEFT -> {
                float c = Mth.sin(age * 0.9f);
                boolean right = s == ParkourState.WALLRUN_RIGHT;
                // wall-side arm reaches out to the wall, the other pumps
                float[] wallArm = {-0.5f, 0f, right ? 1.3f : -1.3f};
                float[] freeArm = {c * 1.0f - 0.2f, 0f, right ? -0.2f : 0.2f};
                set(p, RA, right ? wallArm : freeArm);
                set(p, LA, right ? freeArm : wallArm);
                set(p, RL, c * 1.1f - 0.3f, 0f, 0f);
                set(p, LL, -c * 1.1f - 0.3f, 0f, 0f);
                set(p, BODY, 0.25f, 0f, 0f);
            }
            case WALL_CLIMB -> {
                float c = Mth.sin(age * 1.2f);
                set(p, RA, -2.6f + 0.35f * c, 0f, 0.15f);
                set(p, LA, -2.6f - 0.35f * c, 0f, -0.15f);
                set(p, RL, -0.5f - 0.5f * c, 0f, 0f);
                set(p, LL, -0.5f + 0.5f * c, 0f, 0f);
                set(p, BODY, 0.1f, 0f, 0f);
            }
            case LEDGE_HANG -> {
                float sway = Mth.sin(age * 0.15f) * 0.08f;
                set(p, RA, -2.95f, 0f, 0.2f);
                set(p, LA, -2.95f, 0f, -0.2f);
                set(p, RL, 0.1f + sway, 0f, 0.05f);
                set(p, LL, 0.1f - sway, 0f, -0.05f);
                set(p, BODY, 0f, 0f, 0f);
            }
            case MANTLE -> {
                float e = AnimState.ease(prog);
                set(p, RA, Mth.lerp(e, -2.95f, -0.2f), 0f, 0.25f);
                set(p, LA, Mth.lerp(e, -2.95f, -0.2f), 0f, -0.25f);
                set(p, RL, -1.6f * arc, 0f, 0f);
                set(p, LL, -0.4f * arc, 0f, 0f);
                set(p, BODY, 0.6f * arc, 0f, 0f);
            }
            case VAULT -> {
                set(p, LA, -1.0f + 0.6f * prog, 0f, -0.25f);   // planted hand
                set(p, RA, -0.6f, 0f, 1.2f);                    // balance
                set(p, RL, -1.4f, 0f, 0.3f);
                set(p, LL, -1.2f, 0f, 0.3f);
                set(p, BODY, 0.3f, 0f, 0f);
            }
            case SLIDE -> {
                set(p, RA, -1.1f, 0f, 0.3f);
                set(p, LA, 0.4f, 0f, -0.5f);
                set(p, RL, -0.25f, 0f, 0.05f);
                set(p, LL, -0.6f, 0f, -0.1f);
                set(p, BODY, 0f, 0f, 0f);
            }
            case ROLL_READY -> {
                set(p, RA, -0.9f, 0f, 0.4f);
                set(p, LA, -0.9f, 0f, -0.4f);
                set(p, RL, -0.6f, 0f, 0f);
                set(p, LL, -0.3f, 0f, 0f);
                set(p, BODY, 0.2f, 0f, 0f);
            }
            case ROLL -> {
                set(p, RA, -1.5f, 0f, 0.2f);
                set(p, LA, -1.5f, 0f, -0.2f);
                set(p, RL, -1.8f, 0f, 0f);
                set(p, LL, -1.8f, 0f, 0f);
                set(p, BODY, 0.6f, 0f, 0f);
            }
            case WALL_JUMP -> {
                set(p, RA, -0.8f, 0f, 1.0f);
                set(p, LA, -0.8f, 0f, -1.0f);
                set(p, RL, -0.9f, 0f, 0f);
                set(p, LL, 0.5f, 0f, 0f);
                set(p, BODY, 0.15f, 0f, 0f);
            }
        }
        return p;
    }

    // ------------------------------------------------------------------ whole body

    /** Called after vanilla PlayerRenderer#setupRotations. Local space: +X = player's right, -Z = forward, origin at the feet. */
    public static void applyBody(Player player, PoseStack ps, float partialTick, float scale) {
        AnimState a = AnimState.peek(player.getUUID());
        if (a == null) return;
        float now = ClientClock.now(partialTick);
        float blend = a.blend(now);
        if (a.state == ParkourState.NONE && blend >= 1f) return;

        float[] cur = bodyParams(a.state, a.age(now));
        float[] b = blend < 1f ? lerp(bodyParams(a.prev, a.prevAge()), cur, blend) : cur;
        float rx = b[0], rz = b[1], pivot = b[2], drop = b[3];
        if (rx == 0f && rz == 0f && drop == 0f) return;

        ps.translate(0f, (pivot + drop) * scale, 0f);
        ps.mulPose(Axis.XP.rotation(rx));   // + leans back, - pitches forward
        ps.mulPose(Axis.ZP.rotation(rz));   // + tilts the top to the left
        ps.translate(0f, -pivot * scale, 0f);
    }

    /** {pitch, roll, pivotHeight, verticalDrop} */
    private static float[] bodyParams(ParkourState s, float age) {
        float prog = AnimState.progress(s, age);
        float arc = Mth.sin(prog * Mth.PI);
        return switch (s) {
            case WALLRUN_RIGHT -> new float[]{0f, 0.35f, 0f, 0f};    // lean away from the wall
            case WALLRUN_LEFT -> new float[]{0f, -0.35f, 0f, 0f};
            case WALL_CLIMB -> new float[]{-0.08f, 0f, 0f, 0f};
            case MANTLE -> new float[]{-0.3f * arc, 0f, 0f, 0f};
            case VAULT -> new float[]{0f, 0.35f * arc, 0.5f, 0f};
            case SLIDE -> new float[]{1.0f, 0f, 0f, 0f};
            case ROLL -> new float[]{prog >= 1f ? 0f : -(float) (Math.PI * 2) * AnimState.ease(prog), 0f, 0.6f, -0.3f * arc};
            default -> new float[]{0f, 0f, 0f, 0f};
        };
    }

    // ------------------------------------------------------------------ plumbing

    private static float[] capture(PlayerModel<?> m) {
        return new float[]{
                m.rightArm.xRot, m.rightArm.yRot, m.rightArm.zRot,
                m.leftArm.xRot, m.leftArm.yRot, m.leftArm.zRot,
                m.rightLeg.xRot, m.rightLeg.yRot, m.rightLeg.zRot,
                m.leftLeg.xRot, m.leftLeg.yRot, m.leftLeg.zRot,
                m.body.xRot, m.body.yRot, m.body.zRot};
    }

    private static void write(PlayerModel<?> m, float[] p) {
        rot(m.rightArm, p, RA);
        rot(m.leftArm, p, LA);
        rot(m.rightLeg, p, RL);
        rot(m.leftLeg, p, LL);
        rot(m.body, p, BODY);
    }

    private static void rot(ModelPart part, float[] p, int i) {
        part.xRot = p[i];
        part.yRot = p[i + 1];
        part.zRot = p[i + 2];
    }

    private static void set(float[] p, int i, float x, float y, float z) {
        p[i] = x;
        p[i + 1] = y;
        p[i + 2] = z;
    }

    private static void set(float[] p, int i, float[] xyz) {
        set(p, i, xyz[0], xyz[1], xyz[2]);
    }

    private static float[] lerp(float[] a, float[] b, float t) {
        float[] r = new float[a.length];
        for (int i = 0; i < a.length; i++) r[i] = Mth.lerp(t, a[i], b[i]);
        return r;
    }
}
