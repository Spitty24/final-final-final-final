package dev.freerun.client.laser;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.freerun.laser.LaserCutterBlockEntity;
import dev.freerun.laser.LaserShape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import java.util.List;

/**
 * The laser cutter's moving parts, over the 3 x 3 bed: gantry rails, the bridge sliding along
 * them, the cutting head on the bridge, the sheet on the bed, the kerf the beam leaves behind it,
 * and the beam itself while it cuts.
 */
public class LaserCutterRenderer implements BlockEntityRenderer<LaserCutterBlockEntity> {
    private static final float BED = 0.76f;

    public LaserCutterRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(LaserCutterBlockEntity be, float partial, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        VertexConsumer vc = buffers.getBuffer(Boxes.SOLID);
        int grey = 0xFF3C4046, dark = 0xFF24272B, yellow = 0xFFE8B21C, steel = 0xFFB4BAC2;
        // rails along both sides of the bed, on posts
        Boxes.box(ps, vc, -1.08f, 1.05f, -1.0f, -0.92f, 1.2f, 2.0f, grey, light);
        Boxes.box(ps, vc, 1.92f, 1.05f, -1.0f, 2.08f, 1.2f, 2.0f, grey, light);
        for (float z : new float[]{-1.0f, 1.86f}) {
            Boxes.box(ps, vc, -1.08f, 0.0f, z, -0.92f, 1.05f, z + 0.14f, dark, light);
            Boxes.box(ps, vc, 1.92f, 0.0f, z, 2.08f, 1.05f, z + 0.14f, dark, light);
        }
        double hx = LaserShape.N / 2.0, hy = LaserShape.N - 1.0;   // parked at the back
        boolean cutting = false;
        List<int[]> path = be.path();
        double t = 0;
        if (be.busy() && Minecraft.getInstance().level != null) {
            t = (Minecraft.getInstance().level.getGameTime() + partial - be.start()) / Math.max(1, be.duration());
            t = Math.max(0, Math.min(1, t));
            double[] h = LaserCutterBlockEntity.headAt(path, t);
            hx = h[0];
            hy = h[1];
            cutting = t < 1;
        }
        float wx = (float) (-1 + hx * 3.0 / LaserShape.N), wz = (float) (-1 + hy * 3.0 / LaserShape.N);
        // the bridge (spans x) rides along z, the head rides along the bridge
        Boxes.box(ps, vc, -1.1f, 1.2f, wz - 0.12f, 2.1f, 1.42f, wz + 0.12f, yellow, light);
        Boxes.box(ps, vc, wx - 0.13f, 0.98f, wz - 0.16f, wx + 0.13f, 1.48f, wz + 0.16f, 0xFF2A2E33, light);
        Boxes.box(ps, vc, wx - 0.05f, 0.86f, wz - 0.05f, wx + 0.05f, 0.98f, wz + 0.05f, 0xFFC08A3A, light);   // copper nozzle

        if (!be.busy()) return;
        // Solid parts first, then everything that glows. Fetching a second buffer from the
        // buffer source ends the first one, so the two must never be interleaved.
        Boxes.box(ps, vc, -1.0f, BED, -1.0f, 2.0f, BED + 0.03f, 2.0f, be.gas() == LaserCutterBlockEntity.NITROGEN ? steel : 0xFF9A9488, light);
        int done = Math.min(path.size(), (int) (t * path.size()));
        float c = 3.0f / LaserShape.N;
        int hotFrom = Math.max(0, done - 12);   // the last stretch of the kerf is still glowing
        for (int i = 0; i < hotFrom; i++) {
            int[] p = path.get(i);
            float x0 = -1 + p[0] * c, z0 = -1 + p[1] * c;
            Boxes.box(ps, vc, x0, BED + 0.031f, z0, x0 + c, BED + 0.033f, z0 + c, 0xFF1A1A1A, light);
        }
        VertexConsumer glow = buffers.getBuffer(Boxes.GLOW);
        for (int i = hotFrom; i < done; i++) {
            int[] p = path.get(i);
            float x0 = -1 + p[0] * c, z0 = -1 + p[1] * c;
            Boxes.box(ps, glow, x0, BED + 0.031f, z0, x0 + c, BED + 0.034f, z0 + c, 0xFFFF7A20, 0xF000F0);
        }
        if (cutting) {
            int beam = be.gas() == LaserCutterBlockEntity.NITROGEN ? 0xDDB8E6FF : 0xDDFF5A1A;
            Boxes.box(ps, glow, wx - 0.012f, BED + 0.03f, wz - 0.012f, wx + 0.012f, 0.86f, wz + 0.012f, beam, 0xF000F0);
            Boxes.box(ps, glow, wx - 0.04f, BED + 0.035f, wz - 0.04f, wx + 0.04f, BED + 0.045f, wz + 0.04f, 0xFFFFF0B0, 0xF000F0);
        }
    }

    @Override
    public boolean shouldRenderOffScreen(LaserCutterBlockEntity be) {
        return true;
    }
}
