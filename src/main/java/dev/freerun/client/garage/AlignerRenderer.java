package dev.freerun.client.garage;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.freerun.client.laser.Boxes;
import dev.freerun.garage.AlignerBlockEntity;
import dev.freerun.vehicle.CarEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * The HawkSight's moving parts: the camera tower beside the console with its beam spanning the
 * front of the lift (two camera pods per end, one for each axle) that rides up and down to the
 * targets, and the four targets themselves, which swing out of the stand and clamp onto the
 * tyres, sit level through the rolling compensation, and come off again at the end.
 */
public class AlignerRenderer implements BlockEntityRenderer<AlignerBlockEntity> {
    public AlignerRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(AlignerBlockEntity be, float partial, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        long now = level.getGameTime();
        float t = (now - be.phaseStart()) + partial;
        int phase = be.phase();
        VertexConsumer vc = buffers.getBuffer(Boxes.SOLID);
        int frame = 0xFF2A2D33, blue = 0xFF1C5FB0, cam = 0xFF101214;

        // ---- the tower and beam, in the console's frame (cameras look along +Z = "facing")
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(-be.facing().toYRot()));
        float beam = be.beam();
        if (phase == AlignerBlockEntity.MEASURE) beam = beam + (float) Math.sin(t * 0.6) * 0.01f;   // hunting for the targets
        Boxes.box(ps, vc, 0.5f, 0, 0.1f, 0.74f, 3.4f, 0.34f, frame, light);                        // tower post beside the console
        Boxes.box(ps, vc, 0.45f, 3.4f, 0.05f, 0.79f, 3.5f, 0.39f, blue, light);                     // tower cap
        Boxes.box(ps, vc, -2.4f, beam - 0.1f, 0.36f, 2.4f, beam + 0.1f, 0.5f, frame, light);          // the beam
        Boxes.box(ps, vc, 0.4f, beam - 0.16f, 0.32f, 0.84f, beam + 0.16f, 0.54f, blue, light);       // carriage on the post
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int k = 0; k < 2; k++) {                                                               // two cameras per pod
                float x = sx * (2.2f - k * 0.3f);
                Boxes.box(ps, vc, x - 0.12f, beam - 0.12f, 0.5f, x + 0.12f, beam + 0.12f, 0.8f, cam, light);
                Boxes.box(ps, vc, x - 0.13f, beam + 0.12f, 0.49f, x + 0.13f, beam + 0.16f, 0.81f, blue, light);
            }
        }
        ps.popPose();

        // ---- targets on the car
        Entity e = be.carId() >= 0 ? level.getEntity(be.carId()) : null;
        if (!(e instanceof CarEntity car) || phase == AlignerBlockEntity.IDLE) {
            glow(be, buffers, ps, phase, t, null);
            return;
        }
        Vec3 origin = Vec3.atLowerCornerOf(be.getBlockPos());
        Vec3 cp = car.getPosition(partial);
        float yaw = Mth.lerp(partial, car.yRotO, car.getYRot());
        for (int w = 0; w < 4; w++) {
            float on;   // 0 = still on the stand, 1 = clamped to the tyre
            if (phase == AlignerBlockEntity.MOUNT) on = Mth.clamp((t - w * 15) / 15f, 0, 1);
            else if (phase == AlignerBlockEntity.REMOVE) on = 1 - Mth.clamp((t - w * 10) / 10f, 0, 1);
            else on = 1;
            if (on <= 0) continue;
            boolean left = w % 2 == 0, front = w < 2;
            // wheel hub in the car's frame (+X left, +Z forward), out past the tyre
            float lx = (left ? 1 : -1) * (12.9f / 16 + 0.28f + (1 - on) * 0.9f), lz = (front ? 1 : -1) * 23f / 16;
            float ly = 6f / 16 + (1 - on) * 0.6f;
            ps.pushPose();
            ps.translate(cp.x - origin.x, cp.y - origin.y, cp.z - origin.z);
            ps.mulPose(Axis.YP.rotationDegrees(-yaw));
            ps.translate(lx, ly, lz);
            float s = 0.32f;
            // clamp arms to the tread, a hub, and the target plate (black with a white dot grid) facing the cameras
            float in = left ? -1 : 1;
            Boxes.box(ps, vc, Math.min(0, in * 0.28f), -0.03f, -0.03f, Math.max(0, in * 0.28f), 0.03f, 0.03f, 0xFF7A7E86, light);
            Boxes.box(ps, vc, -0.03f, -0.3f * on, -0.03f, 0.03f, 0.3f * on, 0.03f, 0xFF7A7E86, light);
            Boxes.box(ps, vc, -0.05f, -0.05f, -0.05f, 0.05f, 0.05f, 0.05f, 0xFF2A2A2A, light);
            float fz = 0.06f;
            ps.translate(0, 0.32f, 0);
            Boxes.box(ps, vc, -s, -s, fz, s, s, fz + 0.04f, 0xFF0E0E10, light);
            for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) {
                if ((i + j + w) % 2 != 0 && !(i == 0 && j == 0)) continue;
                float d = 0.12f;
                Boxes.box(ps, vc, i * d - 0.035f, j * d - 0.035f, fz + 0.04f, i * d + 0.035f, j * d + 0.035f, fz + 0.05f, 0xFFF4F4F4, light);
            }
            Boxes.box(ps, vc, -s, s, fz, s, s + 0.03f, fz + 0.05f, 0xFF1C5FB0, light);   // the blue top rail
            ps.popPose();
        }
        glow(be, buffers, ps, phase, t, car);
    }

    /** Camera status LEDs (green when locked on) and, while measuring, faint sight lines to the targets. */
    private static void glow(AlignerBlockEntity be, MultiBufferSource buffers, PoseStack ps, int phase, float t, CarEntity car) {
        VertexConsumer g = buffers.getBuffer(Boxes.GLOW);
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(-be.facing().toYRot()));
        float beam = be.beam();
        boolean locked = phase == AlignerBlockEntity.LIVE;
        boolean blink = phase == AlignerBlockEntity.MEASURE && ((int) t / 4) % 2 == 0;
        int led = locked ? 0xFF40FF60 : blink ? 0xFFFF4030 : 0xFF305030;
        for (int sx = -1; sx <= 1; sx += 2) for (int k = 0; k < 2; k++) {
            float x = sx * (2.2f - k * 0.3f);
            Boxes.box(ps, g, x - 0.05f, beam - 0.05f, 0.8f, x + 0.05f, beam + 0.05f, 0.82f, led, 0xF000F0);
        }
        // the console's status lamp on top
        int lamp = phase == AlignerBlockEntity.IDLE ? 0xFF1C3A60 : locked ? 0xFF2AC040 : 0xFF2A8AF0;
        Boxes.box(ps, g, -0.08f, 1.0f, -0.08f, 0.08f, 1.08f, 0.08f, lamp, 0xF000F0);
        ps.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(AlignerBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
