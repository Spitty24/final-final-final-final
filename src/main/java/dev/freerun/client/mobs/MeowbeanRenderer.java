package dev.freerun.client.mobs;

import dev.freerun.Freerun;
import dev.freerun.mobs.MeowbeanEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class MeowbeanRenderer extends MobRenderer<MeowbeanEntity, MeowbeanModel> {
    private static final ResourceLocation TEXTURE = Freerun.id("textures/entity/meowbean.png");

    public MeowbeanRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new MeowbeanModel(ctx.bakeLayer(MeowbeanModel.LAYER)), 0.4f);
    }

    @Override
    public ResourceLocation getTextureLocation(MeowbeanEntity entity) {
        return TEXTURE;
    }
}
