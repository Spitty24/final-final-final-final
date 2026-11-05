package dev.freerun.client.city;

import dev.freerun.Freerun;
import dev.freerun.city.PoliceOfficerEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** City police: navy uniform, badge, duty belt and cap. Two looks so a squad isn't all twins. */
public class PoliceRenderer extends HumanoidMobRenderer<PoliceOfficerEntity, HumanoidModel<PoliceOfficerEntity>> {
    private static final ResourceLocation[] SKINS = {Freerun.id("textures/entity/police/officer_0.png"),
            Freerun.id("textures/entity/police/officer_1.png")};

    public PoliceRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(PoliceOfficerEntity entity) {
        return SKINS[Math.floorMod(entity.getUUID().hashCode(), SKINS.length)];
    }
}
