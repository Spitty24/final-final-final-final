package dev.freerun.client.city;

import dev.freerun.Freerun;
import dev.freerun.city.CitizenEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** City residents: eight outfits (two suits, scrubs, work gear, a retiree, a student, casual). */
public class CitizenRenderer extends HumanoidMobRenderer<CitizenEntity, CitizenRenderer.Model> {
    private static final ResourceLocation[] SKINS = new ResourceLocation[8];

    static {
        for (int i = 0; i < SKINS.length; i++) SKINS[i] = Freerun.id("textures/entity/citizen/citizen_" + i + ".png");
    }

    public CitizenRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(CitizenEntity entity) {
        return SKINS[Math.floorMod(entity.variant(), SKINS.length)];
    }

    /** Head down, phone held up, when Blackwire is blowing up their notifications. */
    public static class Model extends HumanoidModel<CitizenEntity> {
        public Model(ModelPart root) {
            super(root);
        }

        @Override
        public void setupAnim(CitizenEntity e, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            super.setupAnim(e, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            if (e.distracted()) {
                rightArm.xRot = -1.25f;
                rightArm.yRot = -0.35f;
                leftArm.xRot = -1.1f;
                leftArm.yRot = 0.4f;
                head.xRot = 0.75f;
                hat.copyFrom(head);
            }
        }
    }
}
