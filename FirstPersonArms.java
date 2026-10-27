package dev.freerun.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.freerun.FreerunConfig;
import dev.freerun.ParkourState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

/**
 * First-person body awareness: draws both of the player's arms (with their skin) animated
 * per move, replacing the held-item view while a move plays.
 *
 * Poses are written for the RIGHT arm and mirrored for the left:
 *   dx > 0 = outward, dy > 0 = up the screen, dz < 0 = away from the camera,
 *   xr > 0 = tip the hand up, yr < 0 = turn outward, zr = roll (degrees).
 */
public final class FirstPersonArms {
    /** True while the game is drawing first-person hands, so third-person poses don't leak into them. */
    public static boolean renderingHands;

    private record ArmPose(float dx, float dy, float dz, float xr, float yr, float zr) {
        static final ArmPose REST = new ArmPose(0f, -0.7f, 0f, 0f, 0f, 0f);

        ArmPose lerp(ArmPose o, float t) {
            return new ArmPose(Mth.lerp(t, dx, o.dx), Mth.lerp(t, dy, o.dy), Mth.lerp(t, dz, o.dz),
                    Mth.lerp(t, xr, o.xr), Mth.lerp(t, yr, o.yr), Mth.lerp(t, zr, o.zr));
        }
    }

    private FirstPersonArms() {}

    /** @return true if we drew the hands (vanilla hand/item rendering should be skipped). */
    public static boolean render(PoseStack ps, MultiBufferSource.BufferSource buffer, LocalPlayer player,
                                 int light, float partialTick) {
        FreerunConfig cfg = FreerunConfig.get();
        if (!cfg.enabled || !cfg.firstPersonArms) return false;

        AnimState a = AnimState.get(player.getUUID());
        float now = ClientClock.now(partialTick);
        float blend = a.blend(now);
        boolean emptyHands = player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty();
        boolean pump = cfg.sprintArmPump && emptyHands && player.isSprinting();
        boolean cur = a.state.showsArms;
        boolean prev = a.prev.showsArms;
        if (!cur && !pump && !(prev && blend < 1f)) return false;

        float walk = Mth.lerp(partialTick, player.walkDistO, player.walkDist);
        for (HumanoidArm arm : HumanoidArm.values()) {
            boolean right = arm == HumanoidArm.RIGHT;
            ArmPose idle = pump ? run(walk, right) : ArmPose.REST;
            ArmPose target = cur ? pose(a.state, a.age(now), right, walk) : idle;
            if (blend < 1f) {
                ArmPose from = prev ? pose(a.prev, a.prevAge(), right, walk) : (pump ? idle : ArmPose.REST);
                target = from.lerp(target, blend);
            }
            drawArm(ps, buffer, light, player, arm, target);
        }
        buffer.endBatch();
        return true;
    }

    private static ArmPose run(float walk, boolean right) {
        float s = Mth.sin(walk * Mth.PI) * (right ? 1f : -1f);
        return new ArmPose(0f, -0.18f + 0.1f * s, 0.05f * s, 12f * s, 0f, 0f);
    }

    private static ArmPose pose(ParkourState s, float age, boolean right, float walk) {
        float prog = AnimState.progress(s, age);
        return switch (s) {
            case WALLRUN_RIGHT, WALLRUN_LEFT -> {
                boolean wallSide = (s == ParkourState.WALLRUN_RIGHT) == right;
                yield wallSide
                        ? new ArmPose(0.25f, 0.05f, -0.05f, 20f, -35f, -15f)   // palm on the wall
                        : run(age * 0.3f, right);
            }
            case WALL_CLIMB -> {
                float alt = Mth.sin(age * 1.2f) * (right ? 1f : -1f);
                yield new ArmPose(-0.1f, 0.25f + 0.12f * alt, -0.1f, 45f + 12f * alt, 0f, 8f);
            }
            case LEDGE_HANG -> {
                float sway = Mth.sin(age * 0.15f) * 0.01f;
                yield new ArmPose(-0.08f, 0.42f + sway, -0.12f, 65f, 5f, 5f);
            }
            case MANTLE -> {
                float e = AnimState.ease(prog);
                yield new ArmPose(-0.08f, Mth.lerp(e, 0.42f, -0.2f), Mth.lerp(e, -0.12f, 0.05f),
                        Mth.lerp(e, 65f, -30f), 5f, 5f);
            }
            case VAULT -> right
                    ? new ArmPose(0.35f, -0.1f, 0f, 10f, -40f, -20f)                     // out for balance
                    : new ArmPose(-0.25f, -0.05f - 0.15f * prog, -0.05f, -25f, 10f, 0f); // planted
            case SLIDE -> right
                    ? new ArmPose(-0.05f, -0.05f, -0.1f, 25f, 10f, 0f)
                    : new ArmPose(0.2f, -0.35f, 0.05f, -40f, -10f, -10f);   // trailing hand toward the floor
            case ROLL_READY -> new ArmPose(-0.1f, -0.12f, -0.12f, -10f, 5f, 0f);
            case ROLL -> new ArmPose(-0.05f, -0.55f, 0f, -30f, 0f, 0f);
            case WALL_JUMP -> new ArmPose(0.3f, 0f, 0f, 25f, -25f, -25f);
            case NONE -> ArmPose.REST;
        };
    }

    private static void drawArm(PoseStack ps, MultiBufferSource buffer, int light, LocalPlayer player,
                                HumanoidArm arm, ArmPose pose) {
        float f = arm == HumanoidArm.RIGHT ? 1f : -1f;
        ps.pushPose();

        // our offset, rotating around the arm's on-screen shoulder
        float px = f * 0.45f, py = -0.55f, pz = -0.6f;
        ps.translate(px + f * pose.dx, py + pose.dy, pz + pose.dz);
        ps.mulPose(Axis.YP.rotationDegrees(f * pose.yr));
        ps.mulPose(Axis.XP.rotationDegrees(pose.xr));
        ps.mulPose(Axis.ZP.rotationDegrees(f * pose.zr));
        ps.translate(-px, -py, -pz);

        // vanilla empty-hand placement (ItemInHandRenderer#renderPlayerArm, no swing/equip)
        ps.translate(f * 0.64000005f, -0.6f, -0.71999997f);
        ps.mulPose(Axis.YP.rotationDegrees(f * 45f));
        ps.translate(f * -1.0f, 3.6f, 3.5f);
        ps.mulPose(Axis.ZP.rotationDegrees(f * 120f));
        ps.mulPose(Axis.XP.rotationDegrees(200f));
        ps.mulPose(Axis.YP.rotationDegrees(f * -135f));
        ps.translate(f * 5.6f, 0f, 0f);

        PlayerRenderer renderer = (PlayerRenderer) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
        if (f > 0) renderer.renderRightHand(ps, buffer, light, player);
        else renderer.renderLeftHand(ps, buffer, light, player);
        ps.popPose();
    }
}
