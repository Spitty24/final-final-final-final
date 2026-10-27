package dev.freerun.client.mobs;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.freerun.Freerun;
import dev.freerun.mobs.GrumbusEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders Grumbus small and, crucially, at full brightness so he ignores world lighting/shadow and
 * "pops" like a more-3D-rendered object dropped into Minecraft.
 */
public class GrumbusRenderer extends MobRenderer<GrumbusEntity, GrumbusModel> {
    private static final ResourceLocation TEXTURE = Freerun.id("textures/entity/grumbus.png");

    public GrumbusRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new GrumbusModel(ctx.bakeLayer(ModelLayers.PLAYER)), 0.4f);
    }

    @Override
    public ResourceLocation getTextureLocation(GrumbusEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(GrumbusEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(0.5f, 0.5f, 0.5f);   // roughly half a player tall
    }

    @Override
    protected int getBlockLightLevel(GrumbusEntity entity, BlockPos pos) {
        return 15;   // full-bright: makes him look rendered-in rather than part of the world
    }
}
