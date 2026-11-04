package dev.freerun.client.factory;

import dev.freerun.Freerun;
import dev.freerun.factory.ForkliftDriverEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class ForkliftDriverRenderer extends MobRenderer<ForkliftDriverEntity, ForkliftDriverModel> {
    private static final ResourceLocation TEXTURE = Freerun.id("textures/entity/forklift_driver.png");

    public ForkliftDriverRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new ForkliftDriverModel(ctx.bakeLayer(ForkliftDriverModel.LAYER)), 0.6f);
    }

    @Override
    public ResourceLocation getTextureLocation(ForkliftDriverEntity entity) {
        return TEXTURE;
    }
}
