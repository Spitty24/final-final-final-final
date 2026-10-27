package dev.freerun.client.evil;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.freerun.Freerun;
import dev.freerun.evil.PursuerEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders the Pursuer as a near-black translucent humanoid — a running shadow of the player.
 * (Minecraft 1.21.1 renderer API: no render-state objects.)
 */
public class PursuerRenderer extends MobRenderer<PursuerEntity, HumanoidModel<PursuerEntity>> {
    private static final ResourceLocation TEXTURE = Freerun.id("textures/entity/pursuer.png");

    public PursuerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(PursuerEntity entity) {
        return TEXTURE;
    }

    @Override
    protected RenderType getRenderType(PursuerEntity entity, boolean bodyVisible, boolean translucent, boolean glowing) {
        // Always translucent so it reads as a shadow, not a solid mob.
        return RenderType.entityTranslucent(TEXTURE);
    }

    @Override
    protected void scale(PursuerEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(1.02f, 1.02f, 1.02f);
    }

    @Override
    protected int getBlockLightLevel(PursuerEntity entity, BlockPos pos) {
        return 7; // self-lit a little, so it's visible in the dark it prefers
    }
}
