package dev.freerun.client.mobs;

import dev.freerun.Freerun;
import dev.freerun.mobs.SoldierEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

/** Humanoid renderer for the soldiers, with their held weapon drawn in hand. */
public class SoldierRenderer extends MobRenderer<SoldierEntity, HumanoidModel<SoldierEntity>> {
    private static final ResourceLocation US = Freerun.id("textures/entity/us_soldier.png");
    private static final ResourceLocation FRENCH = Freerun.id("textures/entity/french_soldier.png");

    public SoldierRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.5f);
        addLayer(new ItemInHandLayer<>(this, ctx.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(SoldierEntity entity) {
        return entity.getFaction() == SoldierEntity.Faction.US ? US : FRENCH;
    }
}
