package dev.freerun.client.mobs;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.freerun.Freerun;
import dev.freerun.mobs.OfficeSkit;
import dev.freerun.mobs.RoadhouseNpcEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Player-shaped NPCs; the skin depends on role (and variant, for customers). Sits automatically
 * when riding a seat. The office pair get 3D hair, and Safety Nick lies down and rolls around
 * during his meltdown.
 */
public class RoadhouseNpcRenderer extends MobRenderer<RoadhouseNpcEntity, RoadhouseNpcModel> {
    private static final ResourceLocation HOST = tex("host");
    private static final ResourceLocation WAITER = tex("waiter");
    private static final ResourceLocation COOK = tex("cook");
    private static final ResourceLocation SLATER = tex("slater");
    private static final ResourceLocation NICK = tex("nick");
    private static final ResourceLocation WELDER = tex("welder");
    private static final ResourceLocation PRESS_OPERATOR = tex("press_operator");
    private static final ResourceLocation SAUSAGE_MAKER = tex("sausage_maker");
    private static final ResourceLocation CLERK = tex("clerk");
    private static final ResourceLocation SHOPKEEPER = tex("shopkeeper");
    private static final ResourceLocation[] CUSTOMERS = {tex("customer_0"), tex("customer_1"), tex("customer_2")};

    public RoadhouseNpcRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new RoadhouseNpcModel(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5f);
        addLayer(new NpcHairLayer(this, ctx.getModelSet()));
        addLayer(new net.minecraft.client.renderer.entity.layers.ItemInHandLayer<>(this, ctx.getItemInHandRenderer()));
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
            case SUPERVISOR -> SLATER;
            case SAFETY -> NICK;
            case WELDER -> WELDER;
            case PRESS_OPERATOR -> PRESS_OPERATOR;
            case SAUSAGE_MAKER -> SAUSAGE_MAKER;
            case CLERK -> CLERK;
            case SHOPKEEPER -> SHOPKEEPER;
            case CUSTOMER -> CUSTOMERS[Math.floorMod(entity.getVariant(), CUSTOMERS.length)];
        };
    }

    @Override
    protected void setupRotations(RoadhouseNpcEntity entity, PoseStack poseStack, float bob, float yBodyRot,
                                  float partialTick, float scale) {
        super.setupRotations(entity, poseStack, bob, yBodyRot, partialTick, scale);
        if (entity.getRole() == RoadhouseNpcEntity.Role.SAFETY && !entity.isPassenger() && !entity.isVisitor()
                && OfficeSkit.nickDown(entity.level().getGameTime())) {
            // Flat on the floor, rolling back and forth along his own body.
            float t = entity.level().getGameTime() + partialTick;
            float roll = Mth.sin(t * 0.45f) * 70.0f;
            poseStack.translate(0.0f, 0.35f, 0.0f);
            poseStack.mulPose(Axis.ZP.rotationDegrees(90.0f));
            poseStack.mulPose(Axis.YP.rotationDegrees(roll));
        }
    }
}
