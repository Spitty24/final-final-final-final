package dev.freerun.client.mobs;

import dev.freerun.Freerun;
import dev.freerun.mobs.JohnnyJamesPatrickEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Ethan's long, skinny frame with Johnny's head and half of each of their outfits. */
public class JohnnyJamesPatrickRenderer extends MobRenderer<JohnnyJamesPatrickEntity, JohnnyJamesPatrickRenderer.Model> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Freerun.id("johnny_james_patrick"), "main");
    private static final ResourceLocation SKIN = Freerun.id("textures/entity/johnny_james_patrick.png");

    public JohnnyJamesPatrickRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(LAYER)), 0.4f);
    }

    @Override
    public ResourceLocation getTextureLocation(JohnnyJamesPatrickEntity entity) {
        return SKIN;
    }

    /** Same mesh and posing as {@link EthanModel} (minus the crane hang). */
    public static class Model extends HumanoidModel<JohnnyJamesPatrickEntity> {
        public Model(ModelPart root) {
            super(root);
        }

        @Override
        public void setupAnim(JohnnyJamesPatrickEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                              float netHeadYaw, float headPitch) {
            super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            rightArm.xRot *= 0.5f;
            leftArm.xRot *= 0.5f;
            rightLeg.xRot *= 0.5f;
            leftLeg.xRot *= 0.5f;
            head.y = -14.0f;
            head.z = 0.0f;
            body.y = -14.0f;
            body.z = 0.0f;
            rightArm.setPos(-4.0f, -13.0f, 0.0f);
            leftArm.setPos(4.0f, -13.0f, 0.0f);
            rightLeg.setPos(-1.2f, -2.0f, 0.0f);
            leftLeg.setPos(1.2f, -2.0f, 0.0f);
            head.zRot = (float) Math.sin(ageInTicks * 0.07f) * 0.15f;
            hat.copyFrom(head);
        }
    }
}
