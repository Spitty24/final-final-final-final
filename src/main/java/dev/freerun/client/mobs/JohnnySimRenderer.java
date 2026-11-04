package dev.freerun.client.mobs;

import dev.freerun.Freerun;
import dev.freerun.factory.ForkliftDriverEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Johnny Sim: a regular-built fella with a horseshoe of grey hair, hi-vis vest and work jeans. */
public class JohnnySimRenderer extends HumanoidMobRenderer<ForkliftDriverEntity, HumanoidModel<ForkliftDriverEntity>> {
    private static final ResourceLocation SKIN = Freerun.id("textures/entity/johnny_sim.png");

    public JohnnySimRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(ForkliftDriverEntity entity) {
        return SKIN;
    }
}
