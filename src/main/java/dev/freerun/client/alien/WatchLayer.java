package dev.freerun.client.alien;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.freerun.alien.Transformations;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

/** The Swampwatch on the left wrist of any player carrying one. */
public class WatchLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public WatchLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buffers, int light, AbstractClientPlayer p, float limbSwing, float limbAmount,
                       float partial, float age, float headYaw, float headPitch) {
        if (!WatchAnimation.wearing(p) || p.isInvisible()) return;
        ps.pushPose();
        getParentModel().leftArm.translateAndRotate(ps);
        ps.scale(1 / 16f, 1 / 16f, 1 / 16f);
        boolean charged = !Transformations.form(p).transformed();
        AlienRenderers.watchOnArm(ps, buffers, light, AlienRenderers.slap(p, partial), charged, age);
        ps.popPose();
    }
}
