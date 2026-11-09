package dev.freerun.client.chaos;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.freerun.client.laser.Boxes;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.monster.Creeper;

/** Headphones and a microphone on a creeper that's recording its podcast. */
public class PodcastGearLayer extends RenderLayer<Creeper, CreeperModel<Creeper>> {
    public PodcastGearLayer(RenderLayerParent<Creeper, CreeperModel<Creeper>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buffers, int light, Creeper c, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!ChaosClient.PODCASTERS.contains(c.getId())) return;
        ps.pushPose();
        getParentModel().root().getChild("head").translateAndRotate(ps);
        VertexConsumer v = buffers.getBuffer(Boxes.SOLID);
        float s = 1 / 16f;
        int band = 0xFF1E1E24, cup = 0xFF2B2B33, pad = 0xFFE04848, mic = 0xFF111111, grille = 0xFF8A8F96;
        Boxes.box(ps, v, -4.6f * s, -9.0f * s, -1.0f * s, 4.6f * s, -8.0f * s, 1.0f * s, band, light);
        Boxes.box(ps, v, -5.0f * s, -8.0f * s, -0.8f * s, -4.0f * s, -5.0f * s, 0.8f * s, band, light);
        Boxes.box(ps, v, 4.0f * s, -8.0f * s, -0.8f * s, 5.0f * s, -5.0f * s, 0.8f * s, band, light);
        Boxes.box(ps, v, -5.4f * s, -5.5f * s, -1.8f * s, -4.0f * s, -1.5f * s, 1.8f * s, cup, light);
        Boxes.box(ps, v, 4.0f * s, -5.5f * s, -1.8f * s, 5.4f * s, -1.5f * s, 1.8f * s, cup, light);
        Boxes.box(ps, v, -5.6f * s, -4.8f * s, -1.2f * s, -5.4f * s, -2.2f * s, 1.2f * s, pad, light);
        Boxes.box(ps, v, 5.4f * s, -4.8f * s, -1.2f * s, 5.6f * s, -2.2f * s, 1.2f * s, pad, light);
        // the boom mic, round to the mouth
        Boxes.box(ps, v, 4.4f * s, -2.0f * s, -5.0f * s, 4.9f * s, -1.5f * s, -1.8f * s, mic, light);
        Boxes.box(ps, v, 0.5f * s, -2.0f * s, -5.0f * s, 4.9f * s, -1.5f * s, -4.5f * s, mic, light);
        Boxes.box(ps, v, -1.0f * s, -2.6f * s, -5.6f * s, 1.0f * s, -0.6f * s, -4.4f * s, grille, light);
        ps.popPose();
    }
}
