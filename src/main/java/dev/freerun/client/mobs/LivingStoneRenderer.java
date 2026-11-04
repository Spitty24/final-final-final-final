package dev.freerun.client.mobs;

import dev.freerun.Freerun;
import dev.freerun.mobs.LivingStoneEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class LivingStoneRenderer extends MobRenderer<LivingStoneEntity, LivingStoneModel> {
    private static final ResourceLocation TEXTURE = Freerun.id("textures/entity/living_stone.png");

    public LivingStoneRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new LivingStoneModel(ctx.bakeLayer(LivingStoneModel.LAYER)), 0.45f);
    }

    @Override
    public ResourceLocation getTextureLocation(LivingStoneEntity entity) {
        return TEXTURE;
    }
}
