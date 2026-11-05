package dev.freerun.client.laser;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.freerun.laser.LaserCutterBlockEntity;
import dev.freerun.laser.LaserPartItem;
import dev.freerun.laser.LaserShape;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Draws a laser-cut part as the actual shape you cut: a thin steel plate, one box per run of cells. */
public final class LaserPartRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer {
    @Override
    public void render(ItemStack stack, ItemDisplayContext mode, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        long[] s = LaserPartItem.shape(stack);
        VertexConsumer vc = buffers.getBuffer(Boxes.SOLID);
        boolean n2 = LaserPartItem.gas(stack) == LaserCutterBlockEntity.NITROGEN;
        int face = n2 ? 0xFFC9D0D8 : 0xFF8E887E;
        int n = LaserShape.N;
        float c = 1.0f / n, z0 = 0.47f, z1 = 0.53f;
        if (s == null || LaserShape.empty(s)) {   // a blank: a plain square plate
            Boxes.box(ps, vc, 0.1f, 0.1f, z0, 0.9f, 0.9f, z1, face, light);
            return;
        }
        for (int y = 0; y < n; y++) {
            int x = 0;
            while (x < n) {
                if (!LaserShape.get(s, x, y)) {
                    x++;
                    continue;
                }
                int start = x;
                while (x < n && LaserShape.get(s, x, y)) x++;
                float top = 1 - y * c;
                Boxes.box(ps, vc, start * c, top - c, z0, x * c, top, z1, face, light);
            }
        }
    }
}
