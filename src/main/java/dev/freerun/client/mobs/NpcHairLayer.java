package dev.freerun.client.mobs;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.freerun.Freerun;
import dev.freerun.mobs.RoadhouseNpcEntity;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * 3D hair for the office pair, attached to the head so it turns and tilts with it.
 *  - Supervisor Slater: long hair, down past the shoulders.
 *  - Safety Nick: an early-2010s side-swept swoop fringe.
 */
public class NpcHairLayer extends RenderLayer<RoadhouseNpcEntity, RoadhouseNpcModel> {
    public static final ModelLayerLocation SLATER = new ModelLayerLocation(Freerun.id("slater_hair"), "main");
    public static final ModelLayerLocation NICK = new ModelLayerLocation(Freerun.id("nick_hair"), "main");
    private static final ResourceLocation SLATER_TEX = Freerun.id("textures/entity/roadhouse/slater_hair.png");
    private static final ResourceLocation NICK_TEX = Freerun.id("textures/entity/roadhouse/nick_hair.png");

    private final ModelPart slaterHair;
    private final ModelPart nickHair;

    public NpcHairLayer(RenderLayerParent<RoadhouseNpcEntity, RoadhouseNpcModel> parent, EntityModelSet models) {
        super(parent);
        this.slaterHair = models.bakeLayer(SLATER);
        this.nickHair = models.bakeLayer(NICK);
    }

    /** Head-relative coords: the head cube spans x/z -4..4 and y -8..0. */
    public static LayerDefinition slaterLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("hair", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.5f, -8.6f, -4.5f, 9, 2, 9)        // top
                .texOffs(0, 11).addBox(-4.5f, -8.0f, 3.6f, 9, 15, 1.5f)    // long back, past the shoulders
                .texOffs(36, 0).addBox(-4.7f, -8.0f, -3.5f, 1.2f, 11, 7)   // left side, past the jaw
                .texOffs(36, 0).addBox(3.5f, -8.0f, -3.5f, 1.2f, 11, 7)    // right side
                .texOffs(0, 0).addBox(-4.5f, -8.3f, -4.8f, 9, 1.5f, 1),    // fringe
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    public static LayerDefinition nickLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("hair", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.4f, -8.8f, -4.4f, 8.8f, 1.8f, 8.8f)  // volume on top
                .texOffs(0, 12).addBox(-4.4f, -8.3f, -5.0f, 6.5f, 2.6f, 1.4f) // swoop across the forehead
                .texOffs(0, 12).addBox(1.6f, -7.4f, -5.2f, 2.6f, 2.0f, 1.2f)  // swoop tip, sweeping down to one side
                .texOffs(0, 18).addBox(-4.6f, -8.0f, -4.0f, 1, 4, 8)          // short sides
                .texOffs(0, 18).addBox(3.6f, -8.0f, -4.0f, 1, 4, 8)
                .texOffs(0, 18).addBox(-4.4f, -8.0f, 3.5f, 8.8f, 5, 1),       // back
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight, RoadhouseNpcEntity entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        ModelPart hair;
        ResourceLocation tex;
        switch (entity.getRole()) {
            case SUPERVISOR -> { hair = slaterHair; tex = SLATER_TEX; }
            case SAFETY -> { hair = nickHair; tex = NICK_TEX; }
            default -> { return; }
        }
        poseStack.pushPose();
        getParentModel().head.translateAndRotate(poseStack);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(tex));
        hair.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
