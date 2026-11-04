package dev.freerun.client.factory;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.freerun.Freerun;
import dev.freerun.factory.PartType;
import dev.freerun.factory.SteelPartEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws each part of a steel assembly at its offset and rotation. Models are built in pixels with
 * Y up (no flip), so a cube from y=0 to y=1 is one pixel thick sitting on the entity's feet.
 */
public class SteelPartRenderer extends EntityRenderer<SteelPartEntity> {
    private static final ResourceLocation TEXTURE = Freerun.id("textures/entity/steel_part.png");

    public static final ModelLayerLocation SHEET = layer("steel_sheet");
    public static final ModelLayerLocation PLATE = layer("steel_plate");
    public static final ModelLayerLocation BRACKET = layer("steel_bracket");
    public static final ModelLayerLocation CHANNEL = layer("steel_channel");

    private final ModelPart[] models = new ModelPart[PartType.values().length];

    public SteelPartRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        shadowRadius = 0.45f;
        shadowStrength = 0.6f;
        models[PartType.SHEET.ordinal()] = ctx.bakeLayer(SHEET);
        models[PartType.PLATE.ordinal()] = ctx.bakeLayer(PLATE);
        models[PartType.BRACKET.ordinal()] = ctx.bakeLayer(BRACKET);
        models[PartType.CHANNEL.ordinal()] = ctx.bakeLayer(CHANNEL);
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(Freerun.id(name), "main");
    }

    // Texture is 64x64 (shipped at 128x128 for a finer brushed finish).

    public static LayerDefinition sheetLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("sheet",
                CubeListBuilder.create().texOffs(0, 0).addBox(-8, 0, -8, 16, 1, 16), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition plateLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("plate",
                CubeListBuilder.create().texOffs(0, 17).addBox(-8, 0, -4, 16, 1, 8), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** L angle: a flat flange plus an upright flange along the back (-Z) edge. */
    public static LayerDefinition bracketLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("bracket", CubeListBuilder.create()
                .texOffs(0, 26).addBox(-8, 0, -4, 16, 1, 8)
                .texOffs(0, 35).addBox(-8, 1, -4, 16, 7, 1), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** U channel: a base and two upright walls. */
    public static LayerDefinition channelLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("channel", CubeListBuilder.create()
                .texOffs(0, 44).addBox(-8, 0, -3, 16, 1, 6)
                .texOffs(0, 51).addBox(-8, 1, -3, 16, 5, 1)
                .texOffs(0, 51).addBox(-8, 1, 2, 16, 5, 1), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void render(SteelPartEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        for (SteelPartEntity.Part p : entity.getParts()) {
            poseStack.pushPose();
            poseStack.translate(p.dx(), p.dy(), p.dz());
            poseStack.mulPose(Axis.YP.rotationDegrees(-90.0f * p.rot()));
            models[p.type().ordinal()].render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(SteelPartEntity entity) {
        return TEXTURE;
    }
}
