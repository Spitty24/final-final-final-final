package dev.freerun.client.factory;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.freerun.Freerun;
import dev.freerun.factory.ForkliftEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Draws the forklift, with the fork carriage at its current height and a flashing beacon. */
public class ForkliftRenderer extends EntityRenderer<ForkliftEntity> {
    private static final ResourceLocation TEXTURE = Freerun.id("textures/entity/forklift.png");
    private final ForkliftModel model;

    public ForkliftRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        shadowRadius = 1.0f;
        shadowStrength = 0.7f;
        model = new ForkliftModel(ctx.bakeLayer(ForkliftModel.LAYER));
    }

    @Override
    public void render(ForkliftEntity entity, float entityYaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-entityYaw));
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        int overlay = OverlayTexture.NO_OVERLAY;
        model.body.render(pose, vc, light, overlay);

        boolean running = entity.isVehicle();
        boolean flash = running && (entity.tickCount / 4) % 2 == 0;
        model.beacon.render(pose, vc, flash ? LightTexture.FULL_BRIGHT : light, overlay);

        pose.pushPose();
        pose.translate(0.0f, entity.forkHeight(partialTick), 0.0f);
        model.forks.render(pose, vc, light, overlay);
        if (entity.hasPallet()) model.pallet.render(pose, vc, light, overlay);
        if (entity.hasPallet() && entity.isLoaded()) model.load.render(pose, vc, light, overlay);
        pose.popPose();

        pose.popPose();
        super.render(entity, entityYaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(ForkliftEntity entity) {
        return TEXTURE;
    }
}
