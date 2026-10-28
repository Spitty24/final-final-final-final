package dev.freerun.client.mobs;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.freerun.Freerun;
import dev.freerun.mobs.FlavusEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Renders Flavus Florbington at about 3 feet tall. */
public class FlavusRenderer extends MobRenderer<FlavusEntity, FlavusModel> {
    private static final ResourceLocation TEXTURE = Freerun.id("textures/entity/flavus.png");

    public FlavusRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new FlavusModel(ctx.bakeLayer(FlavusModel.LAYER)), 0.35f);
    }

    @Override
    public ResourceLocation getTextureLocation(FlavusEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(FlavusEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(0.85f, 0.85f, 0.85f);   // ~0.9 blocks tall, including the hair
    }
}
