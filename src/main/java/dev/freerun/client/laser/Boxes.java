package dev.freerun.client.laser;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Draws plain tinted boxes (a white texture, coloured per vertex) for the laser's moving parts. */
final class Boxes {
    static final RenderType SOLID = RenderType.entitySolid(ResourceLocation.withDefaultNamespace("textures/block/white_concrete.png"));
    static final RenderType GLOW = RenderType.entityTranslucentEmissive(ResourceLocation.withDefaultNamespace("textures/block/white_concrete.png"));

    private Boxes() {}

    static void box(PoseStack ps, VertexConsumer vc, float x0, float y0, float z0, float x1, float y1, float z1,
                    int argb, int light) {
        PoseStack.Pose p = ps.last();
        int a = argb >>> 24, r = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255;
        // shade the sides a little so the shape reads
        face(p, vc, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, 0, 1, 0, r, g, b, a, light);
        face(p, vc, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1, 0, -1, 0, r * 5 / 10, g * 5 / 10, b * 5 / 10, a, light);
        face(p, vc, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, 0, 0, -1, r * 8 / 10, g * 8 / 10, b * 8 / 10, a, light);
        face(p, vc, x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1, 0, 0, 1, r * 8 / 10, g * 8 / 10, b * 8 / 10, a, light);
        face(p, vc, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0, -1, 0, 0, r * 7 / 10, g * 7 / 10, b * 7 / 10, a, light);
        face(p, vc, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, 1, 0, 0, r * 7 / 10, g * 7 / 10, b * 7 / 10, a, light);
    }

    private static void face(PoseStack.Pose p, VertexConsumer vc, float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz, float nx, float ny, float nz,
                             int r, int g, int b, int a, int light) {
        vc.addVertex(p, ax, ay, az).setColor(r, g, b, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
        vc.addVertex(p, bx, by, bz).setColor(r, g, b, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
        vc.addVertex(p, cx, cy, cz).setColor(r, g, b, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
        vc.addVertex(p, dx, dy, dz).setColor(r, g, b, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
    }
}
