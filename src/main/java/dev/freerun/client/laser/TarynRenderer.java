package dev.freerun.client.laser;

import dev.freerun.Freerun;
import dev.freerun.laser.TarynEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Taryn: navy coveralls, hi-vis vest, green-tinted laser safety glasses, ponytail. */
public class TarynRenderer extends HumanoidMobRenderer<TarynEntity, HumanoidModel<TarynEntity>> {
    private static final ResourceLocation SKIN = Freerun.id("textures/entity/taryn.png");

    public TarynRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_SLIM)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(TarynEntity entity) {
        return SKIN;
    }
}
