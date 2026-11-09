package dev.freerun.client.city;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.freerun.city.BusEntity;
import dev.freerun.city.BusRoutes;
import dev.freerun.client.laser.Boxes;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * A city bus, drawn from boxes: white body with the route's colour stripe, a glass band of
 * windows, a big windshield, an LED destination sign, folding front door on the curb side, rows
 * of seats inside (with whoever's riding sat in them), a driver, and lights.
 * Local frame: +z is the front, -x is the curb side, y = 0 is the road.
 */
public class BusRenderer extends EntityRenderer<BusEntity> {
    private static final ResourceLocation WHITE = ResourceLocation.withDefaultNamespace("textures/block/white_concrete.png");
    private static final RenderType GLASS = RenderType.entityTranslucent(WHITE);
    private static final float W = 1.3f, L = 4.5f;
    private static final float DOOR0 = 2.55f, DOOR1 = 3.95f;

    public BusRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        shadowRadius = 2.0f;
    }

    @Override
    public ResourceLocation getTextureLocation(BusEntity bus) {
        return WHITE;
    }

    @Override
    public void render(BusEntity bus, float yaw, float partial, PoseStack ps, MultiBufferSource buffers, int light) {
        BusRoutes.Route route = bus.route();
        int stripe = 0xFF000000 | route.color;
        float y = Mth.rotLerp(partial, bus.yRotO, bus.getYRot());
        boolean open = bus.doorsOpen();
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(-y));

        // ---------------- solid
        VertexConsumer v = buffers.getBuffer(Boxes.SOLID);
        int white = 0xFFF2F2EE, grey = 0xFF8A8F96, dark = 0xFF26282C, black = 0xFF111214;
        // floor and wheel wells
        Boxes.box(ps, v, -W + 0.05f, 0.45f, -L + 0.1f, W - 0.05f, 0.55f, L - 0.1f, grey, light);
        Boxes.box(ps, v, -W, 0.3f, -L, W, 0.45f, L, dark, light);
        // left side (street side), full length
        Boxes.box(ps, v, W - 0.08f, 0.45f, -L, W, 1.35f, L, white, light);
        // right side (curb), with the door opening
        Boxes.box(ps, v, -W, 0.45f, -L, -W + 0.08f, 1.35f, DOOR0, white, light);
        Boxes.box(ps, v, -W, 0.45f, DOOR1, -W + 0.08f, 1.35f, L, white, light);
        // the route stripe
        Boxes.box(ps, v, W, 0.95f, -L, W + 0.02f, 1.2f, L, stripe, light);
        Boxes.box(ps, v, -W - 0.02f, 0.95f, -L, -W, 1.2f, DOOR0, stripe, light);
        Boxes.box(ps, v, -W - 0.02f, 0.95f, DOOR1, -W, 1.2f, L, stripe, light);
        Boxes.box(ps, v, -W, 0.95f, -L - 0.02f, W, 1.2f, -L, stripe, light);
        // window pillars and the band above the windows
        for (float z : new float[]{-L, -2.95f, -1.4f, 0.15f, 1.7f, L - 0.12f}) {
            Boxes.box(ps, v, W - 0.1f, 1.35f, z, W, 2.45f, z + 0.12f, white, light);
            if (z < DOOR0 - 0.2f || z > DOOR1) Boxes.box(ps, v, -W, 1.35f, z, -W + 0.1f, 2.45f, z + 0.12f, white, light);
        }
        Boxes.box(ps, v, -W, 1.35f, DOOR0 - 0.12f, -W + 0.1f, 2.45f, DOOR0, white, light);
        Boxes.box(ps, v, W - 0.08f, 2.45f, -L, W, 2.72f, L, white, light);
        Boxes.box(ps, v, -W, 2.45f, -L, -W + 0.08f, 2.72f, L, white, light);
        // roof and the air-con pod
        Boxes.box(ps, v, -W, 2.72f, -L, W, 2.84f, L, white, light);
        Boxes.box(ps, v, -0.8f, 2.84f, -2.6f, 0.8f, 3.08f, -0.4f, 0xFFC9CCD0, light);
        // front: below the windshield, the bumper, and the sign box over the glass
        Boxes.box(ps, v, -W, 0.45f, L - 0.08f, W, 1.2f, L, white, light);
        Boxes.box(ps, v, -W - 0.02f, 0.3f, L, W + 0.02f, 0.6f, L + 0.1f, black, light);
        Boxes.box(ps, v, -W, 2.28f, L - 0.08f, W, 2.72f, L, white, light);
        Boxes.box(ps, v, -1.1f, 2.33f, L, 1.1f, 2.66f, L + 0.04f, black, light);
        // back: a solid wall with a small rear window band
        Boxes.box(ps, v, -W, 0.45f, -L, W, 1.6f, -L + 0.08f, white, light);
        Boxes.box(ps, v, -W, 2.2f, -L, W, 2.72f, -L + 0.08f, white, light);
        Boxes.box(ps, v, -W - 0.02f, 0.3f, -L - 0.1f, W + 0.02f, 0.55f, -L, black, light);
        Boxes.box(ps, v, -0.5f, 2.3f, -L - 0.04f, 0.5f, 2.6f, -L, black, light);
        // wheels
        for (float z : new float[]{3.0f, -2.6f}) {
            for (int s = -1; s <= 1; s += 2) {
                float x0 = s < 0 ? -W - 0.08f : W - 0.25f, x1 = s < 0 ? -W + 0.25f : W + 0.08f;
                Boxes.box(ps, v, x0, 0.0f, z - 0.42f, x1, 0.8f, z + 0.42f, black, light);
                Boxes.box(ps, v, s < 0 ? x0 - 0.01f : x1 - 0.02f, 0.28f, z - 0.18f, s < 0 ? x0 + 0.02f : x1 + 0.01f, 0.52f, z + 0.18f, 0xFFB0B4BA, light);
            }
        }
        // the door: two folding leaves, open or shut
        float leaf = (DOOR1 - DOOR0) / 2;
        if (open) {
            Boxes.box(ps, v, -W + 0.05f, 0.55f, DOOR0, -W + 0.35f, 2.42f, DOOR0 + 0.06f, dark, light);
            Boxes.box(ps, v, -W + 0.05f, 0.55f, DOOR1 - 0.06f, -W + 0.35f, 2.42f, DOOR1, dark, light);
        } else {
            Boxes.box(ps, v, -W, 0.55f, DOOR0, -W + 0.06f, 0.75f, DOOR1, dark, light);
            Boxes.box(ps, v, -W, 2.3f, DOOR0, -W + 0.06f, 2.42f, DOOR1, dark, light);
            Boxes.box(ps, v, -W, 0.55f, DOOR0 + leaf - 0.04f, -W + 0.06f, 2.42f, DOOR0 + leaf + 0.04f, dark, light);
        }
        // the step well
        Boxes.box(ps, v, -W + 0.08f, 0.3f, DOOR0, -W + 0.6f, 0.45f, DOOR1, 0xFFE0C020, light);
        // inside: seats in pairs down both sides
        int seat = 0xFF2C4C8C;
        for (int row = 0; row < 8; row++) {
            float z = 1.9f - row * 0.85f;
            for (int s = -1; s <= 1; s += 2) {
                float x0 = s < 0 ? -1.15f : 0.35f, x1 = s < 0 ? -0.35f : 1.15f;
                Boxes.box(ps, v, x0, 0.85f, z - 0.25f, x1, 0.97f, z + 0.25f, seat, light);
                Boxes.box(ps, v, x0, 0.97f, z - 0.32f, x1, 1.6f, z - 0.22f, seat, light);
                Boxes.box(ps, v, x0 + 0.1f, 0.55f, z - 0.15f, x0 + 0.18f, 0.85f, z - 0.05f, grey, light);
            }
        }
        // yellow grab poles
        for (float z : new float[]{2.2f, 0.4f, -1.4f}) Boxes.box(ps, v, -0.3f, 0.55f, z, -0.24f, 2.72f, z + 0.06f, 0xFFE8C020, light);
        Boxes.box(ps, v, -0.06f, 2.5f, -L + 0.3f, 0.0f, 2.56f, L - 0.6f, 0xFFE8C020, light);
        // the driver's corner: dash, wheel, fare box, and the driver
        Boxes.box(ps, v, -W + 0.1f, 0.55f, L - 0.55f, W - 0.1f, 1.25f, L - 0.08f, dark, light);
        Boxes.box(ps, v, 0.45f, 1.1f, L - 0.95f, 1.05f, 1.6f, L - 0.88f, black, light);
        Boxes.box(ps, v, -0.15f, 0.55f, L - 1.05f, 0.15f, 1.25f, L - 0.75f, 0xFF4A4E56, light);
        driver(ps, v, light, bus.tickCount + partial);

        // ---------------- glass
        VertexConsumer g = buffers.getBuffer(GLASS);
        int tint = 0x7A2A3C4C;
        Boxes.box(ps, g, W - 0.06f, 1.35f, -L + 0.05f, W - 0.04f, 2.45f, L - 0.05f, tint, light);
        Boxes.box(ps, g, -W + 0.04f, 1.35f, -L + 0.05f, -W + 0.06f, 2.45f, DOOR0 - 0.1f, tint, light);
        Boxes.box(ps, g, -W + 0.04f, 1.35f, DOOR1, -W + 0.06f, 2.45f, L - 0.05f, tint, light);
        Boxes.box(ps, g, -W + 0.05f, 1.2f, L - 0.06f, W - 0.05f, 2.28f, L - 0.04f, 0x602A3C4C, light);
        Boxes.box(ps, g, -W + 0.05f, 1.6f, -L + 0.04f, W - 0.05f, 2.2f, -L + 0.06f, tint, light);
        if (!open) {
            Boxes.box(ps, g, -W + 0.01f, 0.75f, DOOR0 + 0.04f, -W + 0.04f, 2.3f, DOOR0 + leaf - 0.04f, tint, light);
            Boxes.box(ps, g, -W + 0.01f, 0.75f, DOOR0 + leaf + 0.04f, -W + 0.04f, 2.3f, DOOR1 - 0.04f, tint, light);
        }

        // ---------------- lights
        VertexConsumer e = buffers.getBuffer(Boxes.GLOW);
        int full = 0xF000F0;
        boolean night = bus.level().getDayTime() % 24000L > 12500L && bus.level().getDayTime() % 24000L < 23500L;
        for (int s = -1; s <= 1; s += 2) {
            Boxes.box(ps, e, s * 0.95f - 0.22f, 0.65f, L + 0.0f, s * 0.95f + 0.22f, 0.9f, L + 0.04f, night ? 0xFFFFF4D0 : 0xC0E8E4D0, full);
            Boxes.box(ps, e, s * 1.05f - 0.15f, 0.7f, -L - 0.04f, s * 1.05f + 0.15f, 1.1f, -L, bus.braking() ? 0xFFFF2020 : 0xB0A01010, full);
            // amber markers along the roof edge
            Boxes.box(ps, e, s * W - 0.03f, 2.66f, L - 0.3f, s * W + 0.03f, 2.72f, L - 0.2f, 0xFFFFA020, full);
        }
        // the interior strip light
        Boxes.box(ps, e, -0.25f, 2.68f, -L + 0.4f, 0.25f, 2.71f, L - 0.7f, 0x90FFF6DC, full);
        if (open) Boxes.box(ps, e, -W + 0.1f, 2.4f, DOOR0 + 0.2f, -W + 0.14f, 2.45f, DOOR1 - 0.2f, 0xFF40FF60, full);

        // ---------------- the destination signs
        Font font = getFont();
        String front = route.number + "  " + route.name.toUpperCase();
        text(ps, buffers, font, front, 0, 2.495f, L + 0.045f, 0, 0.009f, 0xFFFFA820);
        text(ps, buffers, font, route.number + " " + route.name, -W - 0.025f, 2.1f, 1.0f, -90, 0.008f, 0xFFFFA820);
        text(ps, buffers, font, route.number, 0, 2.45f, -L - 0.045f, 180, 0.02f, 0xFFFFA820);
        ps.popPose();
        super.render(bus, yaw, partial, ps, buffers, light);
    }

    /** Someone in a blue uniform at the wheel. */
    private static void driver(PoseStack ps, VertexConsumer v, int light, float t) {
        float x = 0.75f, z = L - 1.25f;
        int uniform = 0xFF2E4A78, skin = 0xFFB98563, legs = 0xFF23262C;
        Boxes.box(ps, v, x - 0.25f, 0.85f, z - 0.25f, x + 0.25f, 0.97f, z + 0.25f, 0xFF333333, light);   // seat
        Boxes.box(ps, v, x - 0.22f, 0.97f, z - 0.12f, x + 0.22f, 1.6f, z + 0.12f, uniform, light);       // torso
        float sway = Mth.sin(t * 0.05f) * 0.02f;
        Boxes.box(ps, v, x - 0.14f + sway, 1.6f, z - 0.12f, x + 0.14f + sway, 1.88f, z + 0.16f, skin, light);   // head
        Boxes.box(ps, v, x - 0.15f + sway, 1.84f, z - 0.14f, x + 0.15f + sway, 1.92f, z + 0.18f, 0xFF1E2A44, light);   // cap
        Boxes.box(ps, v, x - 0.2f, 0.9f, z + 0.12f, x + 0.2f, 1.0f, z + 0.55f, legs, light);              // thighs
        Boxes.box(ps, v, x - 0.2f, 0.55f, z + 0.45f, x + 0.2f, 0.95f, z + 0.58f, legs, light);            // shins
        Boxes.box(ps, v, x - 0.32f, 1.25f, z + 0.05f, x - 0.22f, 1.35f, z + 0.35f, uniform, light);      // arms to the wheel
        Boxes.box(ps, v, x + 0.22f, 1.25f, z + 0.05f, x + 0.32f, 1.35f, z + 0.35f, uniform, light);
    }

    private static void text(PoseStack ps, MultiBufferSource buffers, Font font, String s, float x, float y, float z, float rot,
                             float scale, int color) {
        ps.pushPose();
        ps.translate(x, y, z);
        ps.mulPose(Axis.YP.rotationDegrees(rot));
        ps.scale(scale, -scale, scale);
        font.drawInBatch(s, -font.width(s) / 2f, -4, color, false, ps.last().pose(), buffers, Font.DisplayMode.POLYGON_OFFSET, 0, 0xF000F0);
        ps.popPose();
    }
}
