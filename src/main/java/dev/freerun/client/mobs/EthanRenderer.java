package dev.freerun.client.mobs;

import dev.freerun.Freerun;
import dev.freerun.mobs.EthanEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class EthanRenderer extends MobRenderer<EthanEntity, EthanModel> {
    private static final ResourceLocation TEXTURE = Freerun.id("textures/entity/ethan_james_patrick.png");

    public EthanRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new EthanModel(ctx.bakeLayer(EthanModel.LAYER)), 0.4f);
        addLayer(new net.minecraft.client.renderer.entity.layers.ItemInHandLayer<>(this, ctx.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(EthanEntity entity) {
        return TEXTURE;
    }
}
