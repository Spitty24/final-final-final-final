package dev.freerun.client.laser;

import dev.freerun.Freerun;
import dev.freerun.laser.DrGuildEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Dr. Guild: messy hair, glasses, olive fleece over blue scrubs, stethoscope, ID badge. */
public class DrGuildRenderer extends HumanoidMobRenderer<DrGuildEntity, HumanoidModel<DrGuildEntity>> {
    private static final ResourceLocation SKIN = Freerun.id("textures/entity/dr_guild.png");

    public DrGuildRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(DrGuildEntity entity) {
        return SKIN;
    }
}
