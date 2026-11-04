package dev.freerun.client.factory;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.freerun.Freerun;
import dev.freerun.factory.CraneEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Draws the whole crane from the trolley entity: the bridge girder spanning the bay (one segment
 * per block, lit by the block it's in), the end trucks on the rails, the hoist, the wire rope down
 * to the hook block.
 */
public class CraneRenderer extends EntityRenderer<CraneEntity> {
    private static final ResourceLocation TEXTURE = Freerun.id("textures/entity/crane.png");
    private final CraneModel model;

    public CraneRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        shadowRadius = 0.0f;
        model = new CraneModel(ctx.bakeLayer(CraneModel.LAYER));
    }

    @Override
    public void render(CraneEntity entity, float entityYaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        int overlay = OverlayTexture.NO_OVERLAY;
        double ex = Mth.lerp(partialTick, entity.xOld, entity.getX());
        double ez = Mth.lerp(partialTick, entity.zOld, entity.getZ());
        double ey = entity.getY();
        float min = entity.beamMin();
        float max = entity.beamMax();

        if (max > min) {
            for (int bx = Mth.floor(min); bx < Mth.ceil(max); bx++) {
                int segLight = LevelRenderer.getLightColor(entity.level(), BlockPos.containing(bx + 0.5, ey + 0.3, ez));
                pose.pushPose();
                pose.translate(bx + 0.5 - ex, 0.0, 0.0);
                model.beam.render(pose, vc, segLight, overlay);
                pose.popPose();
            }
            for (double endX : new double[]{min + 0.5, max - 0.5}) {
                pose.pushPose();
                pose.translate(endX - ex, 0.0, 0.0);
                model.end_truck.render(pose, vc, LevelRenderer.getLightColor(entity.level(),
                        BlockPos.containing(endX, ey + 0.3, ez)), overlay);
                pose.popPose();
            }
        }

        model.trolley.render(pose, vc, light, overlay);

        float drop = entity.drop(partialTick);
        float top = (float) -CraneEntity.TROLLEY_DEPTH;
        int whole = Mth.floor(drop);
        for (int i = 0; i < whole; i++) {
            pose.pushPose();
            pose.translate(0.0f, top - i, 0.0f);
            model.cable.render(pose, vc, light, overlay);
            pose.popPose();
        }
        float rest = drop - whole;
        if (rest > 0.01f) {
            pose.pushPose();
            pose.translate(0.0f, top - whole, 0.0f);
            pose.scale(1.0f, rest, 1.0f);
            model.cable.render(pose, vc, light, overlay);
            pose.popPose();
        }
        pose.pushPose();
        pose.translate(0.0f, top - drop, 0.0f);
        model.hook.render(pose, vc, light, overlay);
        pose.popPose();

        super.render(entity, entityYaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(CraneEntity entity) {
        return TEXTURE;
    }
}
