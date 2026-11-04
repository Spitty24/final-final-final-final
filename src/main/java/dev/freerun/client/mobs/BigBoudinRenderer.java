package dev.freerun.client.mobs;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.freerun.Freerun;
import dev.freerun.mobs.BigBoudinEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Big Boudin waddling along, swinging his spatula when he attacks. */
public class BigBoudinRenderer extends MobRenderer<BigBoudinEntity, BigBoudinRenderer.Model> {
    private static final ResourceLocation TEXTURE = Freerun.id("textures/entity/big_boudin.png");

    public BigBoudinRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(new BigBoudinModel(ctx.bakeLayer(BigBoudinModel.LAYER))), 0.9f);
    }

    @Override
    public ResourceLocation getTextureLocation(BigBoudinEntity entity) {
        return TEXTURE;
    }

    public static class Model extends EntityModel<BigBoudinEntity> {
        private final BigBoudinModel m;

        Model(BigBoudinModel m) {
            this.m = m;
        }

        @Override
        public void setupAnim(BigBoudinEntity e, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            float walk = Mth.cos(limbSwing * 0.5f) * 0.9f * limbSwingAmount;
            m.right_leg.xRot = walk;
            m.left_leg.xRot = -walk;
            // a side-to-side waddle
            m.body.zRot = Mth.sin(limbSwing * 0.5f) * 0.12f * limbSwingAmount;
            m.hat.zRot = m.body.zRot;
            m.left_arm.xRot = -walk * 0.6f;
            m.left_arm.zRot = -0.25f;
            float swing = attackTime > 0 ? Mth.sin(attackTime * (float) Math.PI) : 0.0f;
            m.right_arm.xRot = -0.4f - swing * 1.6f + walk * 0.3f;
            m.right_arm.zRot = 0.2f;
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            m.body.render(pose, vc, light, overlay, color);
            m.hat.render(pose, vc, light, overlay, color);
            m.right_arm.render(pose, vc, light, overlay, color);
            m.left_arm.render(pose, vc, light, overlay, color);
            m.right_leg.render(pose, vc, light, overlay, color);
            m.left_leg.render(pose, vc, light, overlay, color);
        }
    }
}
