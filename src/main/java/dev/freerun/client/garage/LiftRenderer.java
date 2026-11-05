package dev.freerun.client.garage;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.freerun.client.laser.Boxes;
import dev.freerun.garage.LiftEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * The four-post lift: red columns with ladder-slot safety locks, yellow cross-beams riding up the
 * columns, and two diamond-plate runways with ramps on the drive-on end and wheel stops at the far end.
 */
public class LiftRenderer extends EntityRenderer<LiftEntity> {
    public LiftRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        shadowRadius = 0f;
    }

    @Override
    public void render(LiftEntity lift, float yaw, float partial, PoseStack ps, MultiBufferSource buffers, int light) {
        ps.pushPose();
        if (lift.alongX()) ps.mulPose(Axis.YP.rotationDegrees(90));
        VertexConsumer vc = buffers.getBuffer(Boxes.SOLID);
        float h = lift.height(partial);
        float W = (float) LiftEntity.HALF_W, L = (float) LiftEntity.HALF_L, D = (float) LiftEntity.DECK;
        int red = 0xFFB8262A, yellow = 0xFFE8B21C, plate = 0xFF8A9098, dark = 0xFF3A3E44;
        float top = (float) LiftEntity.HIGH + D + 0.35f;
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) {
            float x = sx * (W + 0.05f), z = sz * (L + 0.12f);
            Boxes.box(ps, vc, x - 0.16f, 0, z - 0.16f, x + 0.16f, top, z + 0.16f, red, light);
            Boxes.box(ps, vc, x - 0.25f, 0, z - 0.25f, x + 0.25f, 0.06f, z + 0.25f, dark, light);            // base plate
            for (float y = 0.4f; y < top - 0.2f; y += 0.25f) {                                                      // lock slots
                Boxes.box(ps, vc, x - 0.17f * sx * 0 - 0.06f, y, z - sz * 0.17f - 0.01f, x + 0.06f, y + 0.1f, z - sz * 0.165f, dark, light);
            }
        }
        // the cross-beams at each end, riding at deck height
        for (int sz = -1; sz <= 1; sz += 2) {
            float z = sz * (L + 0.12f);
            Boxes.box(ps, vc, -W - 0.2f, h - 0.05f, z - 0.2f, W + 0.2f, h + D + 0.12f, z + 0.2f, yellow, light);
        }
        // the runways under each wheel track, a ramp at the front, wheel stops at the back
        for (int sx = -1; sx <= 1; sx += 2) {
            float x0 = sx < 0 ? -W + 0.1f : 0.25f, x1 = sx < 0 ? -0.25f : W - 0.1f;
            Boxes.box(ps, vc, x0, h, -L, x1, h + D, L, plate, light);
            for (float z = -L + 0.3f; z < L; z += 0.45f) {                                                          // diamond plate
                Boxes.box(ps, vc, x0 + 0.1f, h + D, z, x0 + 0.2f, h + D + 0.01f, z + 0.1f, 0xFFB0B6BE, light);
                Boxes.box(ps, vc, x1 - 0.2f, h + D, z + 0.2f, x1 - 0.1f, h + D + 0.01f, z + 0.3f, 0xFFB0B6BE, light);
            }
            Boxes.box(ps, vc, x0, h + D, -L, x0 + 0.06f, h + D + 0.1f, L, yellow, light);                     // edge lips
            Boxes.box(ps, vc, x1 - 0.06f, h + D, -L, x1, h + D + 0.1f, L, yellow, light);
            Boxes.box(ps, vc, x0, h + D, L - 0.15f, x1, h + D + 0.25f, L, dark, light);                        // wheel stop
            if (h < 0.4f) Boxes.box(ps, vc, x0, 0, -L - 0.6f, x1, 0.12f, -L, plate, light);                    // approach ramp
        }
        ps.popPose();
        super.render(lift, yaw, partial, ps, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(LiftEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/block/white_concrete.png");
    }
}
