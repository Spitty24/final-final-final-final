package dev.freerun.client.skate;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.freerun.skate.SkateboardItem;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** The board as an item: the same 3D model you ride. */
public final class SkateboardItemRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer {
    @Override
    public void render(ItemStack stack, ItemDisplayContext mode, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        int deck = stack.getItem() instanceof SkateboardItem b ? b.deck : 0;
        ps.pushPose();
        ps.translate(0.5f, 0.4f, 0.5f);
        if (mode == ItemDisplayContext.GUI) {
            ps.mulPose(Axis.YP.rotationDegrees(45));
            ps.scale(1.15f, 1.15f, 1.15f);
        }
        SkateboardModel.render(ps, buffers::getBuffer, deck, light, 0);
        ps.popPose();
    }
}
