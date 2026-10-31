package dev.freerun.client.mobs;

import dev.freerun.Freerun;
import dev.freerun.mobs.RoadhouseNpcEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Player-shaped NPCs; the skin depends on role (and variant, for customers). Sits automatically when riding a seat. */
public class RoadhouseNpcRenderer extends MobRenderer<RoadhouseNpcEntity, HumanoidModel<RoadhouseNpcEntity>> {
    private static final ResourceLocation HOST = tex("host");
    private static final ResourceLocation WAITER = tex("waiter");
    private static final ResourceLocation COOK = tex("cook");
    private static final ResourceLocation[] CUSTOMERS = {tex("customer_0"), tex("customer_1"), tex("customer_2")};

    public RoadhouseNpcRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    private static ResourceLocation tex(String name) {
        return Freerun.id("textures/entity/roadhouse/" + name + ".png");
    }

    @Override
    public ResourceLocation getTextureLocation(RoadhouseNpcEntity entity) {
        return switch (entity.getRole()) {
            case HOST -> HOST;
            case WAITER -> WAITER;
            case COOK -> COOK;
            case CUSTOMER -> CUSTOMERS[Math.floorMod(entity.getVariant(), CUSTOMERS.length)];
        };
    }
}
