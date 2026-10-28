package dev.freerun.client.mobs;

import dev.freerun.Freerun;
import dev.freerun.mobs.WingusEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class WingusRenderer extends MobRenderer<WingusEntity, WingusModel> {
    private static final ResourceLocation TEXTURE = Freerun.id("textures/entity/wingus.png");

    public WingusRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new WingusModel(ctx.bakeLayer(WingusModel.LAYER)), 0.4f);
    }

    @Override
    public ResourceLocation getTextureLocation(WingusEntity entity) {
        return TEXTURE;
    }
}
