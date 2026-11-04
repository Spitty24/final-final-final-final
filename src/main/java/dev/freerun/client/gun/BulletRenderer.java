package dev.freerun.client.gun;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.freerun.Freerun;
import dev.freerun.item.gun.BulletEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** A bullet's tracer: a thin glowing streak stretched back along its path, drawn as two crossed quads. */
public class BulletRenderer extends EntityRenderer<BulletEntity> {
    private static final ResourceLocation TEXTURE = Freerun.id("textures/entity/tracer.png");

    public BulletRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(BulletEntity bullet, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        Vec3 v = bullet.getDeltaMovement();
        double len = v.length();
        if (len < 1.0E-3) return;
        Vec3 dir = v.scale(1.0 / len);
        float tail = (float) Math.min(2.2, len * 0.45);
        Vec3 side = dir.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 1.0E-4) side = new Vec3(1, 0, 0);
        side = side.normalize();
        Vec3 up = side.cross(dir).normalize();
        float w = 0.025f;
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
        Matrix4f m = pose.last().pose();
        Vec3 back = dir.scale(-tail);
        quad(vc, m, pose, side.scale(w), back);
        quad(vc, m, pose, up.scale(w), back);
    }

    private static void quad(VertexConsumer vc, Matrix4f m, PoseStack pose, Vec3 off, Vec3 back) {
        int full = LightTexture.FULL_BRIGHT;
        int ov = OverlayTexture.NO_OVERLAY;
        vert(vc, m, pose, off.x, off.y, off.z, 0, 0, full, ov);
        vert(vc, m, pose, -off.x, -off.y, -off.z, 0, 1, full, ov);
        vert(vc, m, pose, back.x - off.x, back.y - off.y, back.z - off.z, 1, 1, full, ov);
        vert(vc, m, pose, back.x + off.x, back.y + off.y, back.z + off.z, 1, 0, full, ov);
        // and the back face
        vert(vc, m, pose, back.x + off.x, back.y + off.y, back.z + off.z, 1, 0, full, ov);
        vert(vc, m, pose, back.x - off.x, back.y - off.y, back.z - off.z, 1, 1, full, ov);
        vert(vc, m, pose, -off.x, -off.y, -off.z, 0, 1, full, ov);
        vert(vc, m, pose, off.x, off.y, off.z, 0, 0, full, ov);
    }

    private static void vert(VertexConsumer vc, Matrix4f m, PoseStack pose, double x, double y, double z, float u, float v,
                             int light, int overlay) {
        vc.addVertex(m, (float) x, (float) y, (float) z).setColor(255, 236, 170, 230).setUv(u, v)
                .setOverlay(overlay).setLight(light).setNormal(pose.last(), 0.0f, 1.0f, 0.0f);
    }

    @Override
    public ResourceLocation getTextureLocation(BulletEntity bullet) {
        return TEXTURE;
    }
}
