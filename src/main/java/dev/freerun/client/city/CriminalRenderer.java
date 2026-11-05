package dev.freerun.client.city;

import dev.freerun.Freerun;
import dev.freerun.city.CriminalEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Suspects: a dark hoodie, a track jacket, or a grey work jacket. Hands go up when they give up. */
public class CriminalRenderer extends HumanoidMobRenderer<CriminalEntity, CriminalRenderer.Model> {
    private static final ResourceLocation[] SKINS = {Freerun.id("textures/entity/criminal/suspect_0.png"),
            Freerun.id("textures/entity/criminal/suspect_1.png"), Freerun.id("textures/entity/criminal/suspect_2.png")};

    public CriminalRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(CriminalEntity entity) {
        return SKINS[Math.floorMod(entity.variant(), SKINS.length)];
    }

    public static class Model extends HumanoidModel<CriminalEntity> {
        public Model(ModelPart root) {
            super(root);
        }

        @Override
        public void setupAnim(CriminalEntity e, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            super.setupAnim(e, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            if (e.surrendered()) {
                rightArm.xRot = (float) Math.PI;
                leftArm.xRot = (float) Math.PI;
                rightArm.zRot = 0.2f;
                leftArm.zRot = -0.2f;
            }
        }
    }
}
