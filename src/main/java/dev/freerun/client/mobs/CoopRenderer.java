package dev.freerun.client.mobs;

import dev.freerun.Freerun;
import dev.freerun.mobs.CoopEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** The co-op interns: safety vests, lanyards, hard hats one size too big. */
public class CoopRenderer extends HumanoidMobRenderer<CoopEntity, HumanoidModel<CoopEntity>> {
    private static final ResourceLocation[] SKINS = {
            Freerun.id("textures/entity/coop_0.png"), Freerun.id("textures/entity/coop_1.png"), Freerun.id("textures/entity/coop_2.png")};

    public CoopRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(CoopEntity entity) {
        return SKINS[Math.floorMod(entity.variant(), SKINS.length)];
    }
}
