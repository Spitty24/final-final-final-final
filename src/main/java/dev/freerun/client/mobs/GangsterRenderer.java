package dev.freerun.client.mobs;

import dev.freerun.Freerun;
import dev.freerun.gang.Crew;
import dev.freerun.gang.GangsterEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Crew members: hoodie in the crew's colour, face covered, piece in hand. */
public class GangsterRenderer extends HumanoidMobRenderer<GangsterEntity, HumanoidModel<GangsterEntity>> {
    private static final ResourceLocation[][] SKINS = {
            {tex("rust_rat_0"), tex("rust_rat_1")},
            {tex("chrome_king_0"), tex("chrome_king_1")}
    };

    public GangsterRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    private static ResourceLocation tex(String name) {
        return Freerun.id("textures/entity/gang/" + name + ".png");
    }

    @Override
    public ResourceLocation getTextureLocation(GangsterEntity entity) {
        return SKINS[entity.crew() == Crew.RUST_RATS ? 0 : 1][entity.variant() & 1];
    }
}
