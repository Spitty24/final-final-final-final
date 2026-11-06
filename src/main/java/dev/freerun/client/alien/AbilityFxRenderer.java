package dev.freerun.client.alien;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.freerun.alien.AbilityFx;
import dev.freerun.client.laser.Boxes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Draws every Swampwatch power effect. Solid geometry first, glowing geometry second, never
 * interleaved (fetching a second buffer ends the first).
 */
public class AbilityFxRenderer extends EntityRenderer<AbilityFx> {
    private static final int FULL = 0xF000F0;

    public AbilityFxRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        shadowRadius = 0;
    }

    @Override
    public void render(AbilityFx fx, float yaw, float partial, PoseStack ps, MultiBufferSource buffers, int light) {
        float t = fx.tickCount + partial;
        float life = fx.life();
        float k = Mth.clamp(t / life, 0, 1);   // 0 → 1 over the effect's life
        switch (fx.kind()) {
            case AbilityFx.ROOTS -> roots(fx, t, life, ps, buffers, light);
            case AbilityFx.SHOCKWAVE -> shockwave(fx, t, k, ps, buffers, light);
            case AbilityFx.ARC -> arc(fx, t, k, ps, buffers);
            case AbilityFx.DRAIN -> drain(fx, t, k, ps, buffers);
            case AbilityFx.SNAP -> snap(fx, t, k, ps, buffers, light);
            case AbilityFx.MIST -> mist(fx, k, ps, buffers);
            case AbilityFx.DUST -> dust(fx, t, k, ps, buffers, light);
            case AbilityFx.SPLAT -> splat(fx, t, k, ps, buffers, light);
            case AbilityFx.AFTERIMAGE -> afterimage(fx, k, ps, buffers);
            case AbilityFx.SLAG -> slag(fx, t, ps, buffers);
            default -> {}
        }
        super.render(fx, yaw, partial, ps, buffers, light);
    }

    // ------------------------------------------------------------------ Cypress: Root Snare

    /**
     * Five roots burst out of the ground round the target, curl up and over it in a cage, hold, and
     * pull back down at the end. Each root is a chain of shrinking, twisting segments.
     */
    private static void roots(AbilityFx fx, float t, float life, PoseStack ps, MultiBufferSource buffers, int light) {
        float grow = ease(Mth.clamp(t / 8f, 0, 1));
        float shrink = ease(Mth.clamp((life - t) / 10f, 0, 1));
        float g = Math.min(grow, shrink);
        if (g <= 0) return;
        Entity target = fx.target();
        float h = target != null ? target.getBbHeight() + 0.4f : 2.0f;
        float r = target != null ? target.getBbWidth() * 0.5f + 0.35f : 0.7f;
        VertexConsumer vc = buffers.getBuffer(Boxes.SOLID);
        RandomSource rnd = RandomSource.create(fx.getId() * 31L);
        int bark = 0xFF5A3E24, bark2 = 0xFF3E2A18, moss = 0xFF6A8A3A;
        // a mound of churned earth
        Boxes.box(ps, vc, -r - 0.35f, -0.02f, -r - 0.35f, r + 0.35f, 0.08f * g, r + 0.35f, 0xFF4A3420, light);
        int segs = 9;
        for (int i = 0; i < 5; i++) {
            float base = i * Mth.TWO_PI / 5 + rnd.nextFloat() * 0.4f;
            float twist = 0.9f + rnd.nextFloat() * 0.6f;
            int shown = (int) Math.ceil(segs * g);
            for (int s = 0; s < shown; s++) {
                float f = s / (float) segs;
                float ang = base + f * twist + Mth.sin(t * 0.08f + i) * 0.05f;
                float rr = r * (1.25f - 0.6f * f * f) + 0.15f;     // curls inward toward the top
                float y = f * h;
                float x = Mth.cos(ang) * rr, z = Mth.sin(ang) * rr;
                float w = 0.16f * (1 - f * 0.65f);
                if (s == shown - 1) w *= Mth.clamp(segs * g - s, 0.2f, 1);
                Boxes.box(ps, vc, x - w, y, z - w, x + w, y + h / segs + 0.04f, z + w, s % 3 == 0 ? bark2 : bark, light);
                if (s % 3 == 1) {   // little side shoots and moss
                    float sx = Mth.cos(ang + 1.2f) * 0.18f, sz = Mth.sin(ang + 1.2f) * 0.18f;
                    Boxes.box(ps, vc, x + sx - 0.04f, y + 0.05f, z + sz - 0.04f, x + sx * 1.8f + 0.04f, y + 0.12f, z + sz * 1.8f + 0.04f, moss, light);
                }
            }
        }
        // the cage's top knot once they've met
        if (g > 0.95f) Boxes.box(ps, vc, -0.12f, h - 0.05f, -0.12f, 0.12f, h + 0.15f, 0.12f, bark2, light);
        // a faint green glow at the roots' base: the snare's draining the target
        VertexConsumer gl = buffers.getBuffer(Boxes.GLOW);
        int a = (int) (90 * g * (0.6f + 0.4f * Mth.sin(t * 0.3f)));
        Boxes.box(ps, gl, -r - 0.3f, 0.09f, -r - 0.3f, r + 0.3f, 0.11f, r + 0.3f, a << 24 | 0x80E040, FULL);
    }

    // ------------------------------------------------------------------ Mossback: Shell Slam

    private static void shockwave(AbilityFx fx, float t, float k, PoseStack ps, MultiBufferSource buffers, int light) {
        float R = fx.radius();
        VertexConsumer vc = buffers.getBuffer(Boxes.SOLID);
        RandomSource rnd = RandomSource.create(fx.getId() * 17L);
        // a crater: a cracked disc of dark earth
        float crater = Math.min(1, k * 4) * (1 - Mth.clamp((k - 0.7f) / 0.3f, 0, 1));
        Boxes.box(ps, vc, -1.4f * crater, -0.02f, -1.4f * crater, 1.4f * crater, 0.03f, 1.4f * crater, 0xFF2A2018, light);
        for (int i = 0; i < 8; i++) {   // cracks radiating out
            float ang = i * Mth.TWO_PI / 8 + rnd.nextFloat() * 0.3f;
            float len = (1.2f + rnd.nextFloat() * 2.5f) * Math.min(1, k * 3) * crater;
            ps.pushPose();
            ps.mulPose(Axis.YP.rotation(ang));
            Boxes.box(ps, vc, 0, 0.0f, -0.05f, len, 0.04f, 0.05f, 0xFF18120C, light);
            ps.popPose();
        }
        // chunks of ground thrown up along the ring
        float ring = ease(k) * R;
        for (int i = 0; i < 20; i++) {
            float ang = i * Mth.TWO_PI / 20 + rnd.nextFloat() * 0.2f;
            float up = Mth.sin(Mth.clamp(k * 1.6f - rnd.nextFloat() * 0.3f, 0, 1) * Mth.PI) * (0.4f + rnd.nextFloat() * 0.8f);
            float x = Mth.cos(ang) * ring, z = Mth.sin(ang) * ring, s = 0.12f + rnd.nextFloat() * 0.14f;
            ps.pushPose();
            ps.translate(x, up, z);
            ps.mulPose(Axis.XP.rotation(t * 0.3f + i));
            Boxes.box(ps, vc, -s, -s, -s, s, s, s, i % 3 == 0 ? 0xFF5A4A30 : 0xFF6A6058, light);
            ps.popPose();
        }
        // the ring of force itself
        VertexConsumer g = buffers.getBuffer(Boxes.GLOW);
        int a = (int) (180 * (1 - k));
        for (int i = 0; i < 32; i++) {
            float ang = i * Mth.TWO_PI / 32;
            float x = Mth.cos(ang) * ring, z = Mth.sin(ang) * ring;
            Boxes.box(ps, g, x - 0.18f, 0.05f, z - 0.18f, x + 0.18f, 0.05f + 0.5f * (1 - k), z + 0.18f, a << 24 | 0xD8C890, FULL);
        }
    }

    // ------------------------------------------------------------------ Voltbelly: Arc

    /** A jagged bolt from this point to point B, re-forked every tick, with a white core and a blue sheath. */
    private static void arc(AbilityFx fx, float t, float k, PoseStack ps, MultiBufferSource buffers) {
        Vec3 a = Vec3.ZERO, b = fx.pointB().subtract(fx.position());
        VertexConsumer g = buffers.getBuffer(Boxes.GLOW);
        RandomSource rnd = RandomSource.create(fx.getId() * 7L + (long) t);
        int n = Math.max(6, (int) (b.length() * 3));
        Vec3 prev = a;
        float fade = 1 - k;
        for (int i = 1; i <= n; i++) {
            Vec3 p = a.lerp(b, i / (double) n);
            if (i < n) p = p.add((rnd.nextFloat() - 0.5) * 0.6, (rnd.nextFloat() - 0.5) * 0.6, (rnd.nextFloat() - 0.5) * 0.6);
            segment(ps, g, prev, p, 0.09f, (int) (110 * fade) << 24 | 0x60A0FF);
            segment(ps, g, prev, p, 0.035f, (int) (255 * fade) << 24 | 0xF0F8FF);
            if (rnd.nextInt(4) == 0) {   // a little fork
                Vec3 f = p.add((rnd.nextFloat() - 0.5) * 1.0, (rnd.nextFloat() - 0.5) * 1.0, (rnd.nextFloat() - 0.5) * 1.0);
                segment(ps, g, p, f, 0.02f, (int) (200 * fade) << 24 | 0xC0E0FF);
            }
            prev = p;
        }
        // a flash where it lands
        float s = 0.4f * fade + 0.1f;
        ps.pushPose();
        ps.translate(b.x, b.y, b.z);
        Boxes.box(ps, g, -s, -s, -s, s, s, s, (int) (160 * fade) << 24 | 0xC0E0FF, FULL);
        ps.popPose();
    }

    // ------------------------------------------------------------------ Skeeter: Drain Bite

    private static void drain(AbilityFx fx, float t, float k, PoseStack ps, MultiBufferSource buffers) {
        Vec3 from = Vec3.ZERO, to = fx.pointB().subtract(fx.position());
        Entity target = fx.target();
        if (target != null) to = target.getEyePosition().subtract(0, 0.2, 0).subtract(fx.position());
        VertexConsumer g = buffers.getBuffer(Boxes.GLOW);
        // drops of red running from the bite back to the Skeeter, along a sagging curve
        for (int i = 0; i < 10; i++) {
            float f = ((i / 10f) + t * 0.08f) % 1f;
            Vec3 p = from.lerp(to, 1 - f).add(0, -Mth.sin(f * Mth.PI) * 0.3f, 0);
            float s = 0.06f + 0.03f * Mth.sin(t + i);
            ps.pushPose();
            ps.translate(p.x, p.y, p.z);
            Boxes.box(ps, g, -s, -s, -s, s, s, s, (int) (230 * (1 - k)) << 24 | 0xD01818, FULL);
            ps.popPose();
        }
    }

    // ------------------------------------------------------------------ Mudbug: Pincer Snap

    /** Two huge ghostly claws in front of the Mudbug that swing in and clap. */
    private static void snap(AbilityFx fx, float t, float k, PoseStack ps, MultiBufferSource buffers, int light) {
        VertexConsumer g = buffers.getBuffer(Boxes.GLOW);
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(-fx.fxYaw()));
        ps.translate(0, 0.9, 1.6);
        float close = ease(Mth.clamp(k * 2.5f, 0, 1));
        int col = (int) (200 * (1 - k * 0.8f)) << 24 | 0xFF6040;
        for (int side = -1; side <= 1; side += 2) {
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(side * (50 - 48 * close)));
            Boxes.box(ps, g, side < 0 ? -0.15f : 0, -0.25f, 0, side < 0 ? 0 : 0.15f, 0.25f, 1.6f, col, FULL);
            Boxes.box(ps, g, side < 0 ? -0.35f : 0.0f, -0.2f, 1.2f, side < 0 ? 0.0f : 0.35f, 0.2f, 1.6f, col, FULL);
            ps.popPose();
        }
        if (close > 0.95f) {   // the impact star
            float s = 0.5f * (1 - k);
            Boxes.box(ps, g, -s, -0.05f, 1.3f - 0.05f, s, 0.05f, 1.3f + 0.05f, (int) (255 * (1 - k)) << 24 | 0xFFF0C0, FULL);
            Boxes.box(ps, g, -0.05f, -s, 1.3f - 0.05f, 0.05f, s, 1.3f + 0.05f, (int) (255 * (1 - k)) << 24 | 0xFFF0C0, FULL);
        }
        ps.popPose();
    }

    // ------------------------------------------------------------------ Foglet: Mist Dash trail

    private static void mist(AbilityFx fx, float k, PoseStack ps, MultiBufferSource buffers) {
        VertexConsumer g = buffers.getBuffer(Boxes.GLOW);
        float s = 0.4f + 0.5f * k;
        int a = (int) (140 * (1 - k));
        RandomSource rnd = RandomSource.create(fx.getId());
        for (int i = 0; i < 4; i++) {
            float x = (rnd.nextFloat() - 0.5f) * 0.6f, y = 0.4f + rnd.nextFloat() * 1.0f, z = (rnd.nextFloat() - 0.5f) * 0.6f;
            Boxes.box(ps, g, x - s * 0.5f, y - s * 0.4f, z - s * 0.5f, x + s * 0.5f, y + s * 0.4f, z + s * 0.5f, a << 24 | 0xE0F0FF, FULL);
        }
    }

    // ------------------------------------------------------------------ Bayou Bull: Stampede dust

    private static void dust(AbilityFx fx, float t, float k, PoseStack ps, MultiBufferSource buffers, int light) {
        VertexConsumer vc = buffers.getBuffer(Boxes.GLOW);
        RandomSource rnd = RandomSource.create(fx.getId());
        for (int i = 0; i < 6; i++) {
            float x = (rnd.nextFloat() - 0.5f) * 1.4f, z = (rnd.nextFloat() - 0.5f) * 1.4f;
            float s = 0.2f + 0.4f * k + rnd.nextFloat() * 0.2f;
            float y = 0.1f + k * 0.6f + rnd.nextFloat() * 0.3f;
            Boxes.box(ps, vc, x - s, y - s * 0.6f, z - s, x + s, y + s * 0.6f, z + s, (int) (120 * (1 - k)) << 24 | 0x9A7A5A, light);
        }
        // a hoof-print scorch
        Boxes.box(ps, vc, -0.2f, 0.01f, -0.25f, 0.2f, 0.02f, 0.25f, (int) (160 * (1 - k)) << 24 | 0x3A2A1A, light);
    }

    // ------------------------------------------------------------------ Gumbo: Splat

    /** A wave of gumbo rolling outward, with chunks in it; small ones are the puddles stuck to each target. */
    private static void splat(AbilityFx fx, float t, float k, PoseStack ps, MultiBufferSource buffers, int light) {
        float R = fx.radius();
        VertexConsumer vc = buffers.getBuffer(Boxes.SOLID);
        RandomSource rnd = RandomSource.create(fx.getId() * 13L);
        int brown = 0xFF7A4A1E, dark = 0xFF5A3010;
        if (R < 1.5f) {   // a puddle round someone's feet that slowly sinks away
            float s = R * (1 - Mth.clamp((k - 0.8f) / 0.2f, 0, 1));
            Boxes.box(ps, vc, -s, 0.0f, -s, s, 0.06f, s, brown, light);
            for (int i = 0; i < 4; i++) {
                float x = (rnd.nextFloat() - 0.5f) * s * 1.4f, z = (rnd.nextFloat() - 0.5f) * s * 1.4f;
                float h = 0.1f + 0.1f * Mth.sin(t * 0.2f + i);
                Boxes.box(ps, vc, x - 0.08f, 0.04f, z - 0.08f, x + 0.08f, 0.04f + h, z + 0.08f, i % 2 == 0 ? 0xFFC84A2A : 0xFF3A7A30, light);
            }
            return;
        }
        float ring = ease(Math.min(1, k * 1.6f)) * R;
        float wave = Mth.sin(Math.min(1, k * 1.6f) * Mth.PI) * 0.7f;
        int segs = 28;
        for (int i = 0; i < segs; i++) {
            float ang = i * Mth.TWO_PI / segs;
            float x = Mth.cos(ang) * ring, z = Mth.sin(ang) * ring;
            float w = 0.45f;
            ps.pushPose();
            ps.translate(x, 0, z);
            ps.mulPose(Axis.YP.rotation(-ang));
            Boxes.box(ps, vc, -0.15f, 0, -w, 0.15f, wave + 0.05f, w, i % 2 == 0 ? brown : dark, light);
            ps.popPose();
            if (i % 4 == 0) {   // chunks riding the wave
                float h = wave + 0.15f + rnd.nextFloat() * 0.3f;
                Boxes.box(ps, vc, x - 0.1f, h, z - 0.1f, x + 0.1f, h + 0.18f, z + 0.1f, i % 8 == 0 ? 0xFFC84A2A : 0xFF3A7A30, light);
            }
        }
        // what's left in the middle
        float pool = Math.min(1, k * 3) * 1.4f * (1 - Mth.clamp((k - 0.6f) / 0.4f, 0, 1));
        Boxes.box(ps, vc, -pool, 0.0f, -pool, pool, 0.05f, pool, brown, light);
    }

    // ------------------------------------------------------------------ Clockroach: Scurry afterimages

    private static void afterimage(AbilityFx fx, float k, PoseStack ps, MultiBufferSource buffers) {
        VertexConsumer g = buffers.getBuffer(Boxes.GLOW);
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(-fx.fxYaw()));
        int a = (int) (130 * (1 - k));
        Boxes.box(ps, g, -0.2f, 0.05f, -0.28f, 0.2f, 0.25f, 0.28f, a << 24 | 0xC08040, FULL);
        for (int i = -1; i <= 1; i++) {   // speed lines out behind it
            Boxes.box(ps, g, i * 0.12f - 0.01f, 0.15f, -0.28f - 0.9f * (1 - k), i * 0.12f + 0.01f, 0.17f, -0.28f, a << 24 | 0xFFE0A0, FULL);
        }
        ps.popPose();
    }

    // ------------------------------------------------------------------ Slagworm: the glob of slag on its fireball

    private static void slag(AbilityFx fx, float t, PoseStack ps, MultiBufferSource buffers) {
        VertexConsumer g = buffers.getBuffer(Boxes.GLOW);
        float s = 0.32f + 0.04f * Mth.sin(t * 0.8f);
        ps.pushPose();
        ps.mulPose(Axis.YP.rotation(t * 0.4f));
        ps.mulPose(Axis.XP.rotation(t * 0.3f));
        Boxes.box(ps, g, -s, -s, -s, s, s, s, 0xFFFF7A10, FULL);
        Boxes.box(ps, g, -s * 0.6f, -s * 0.6f, -s * 0.6f, s * 0.6f, s * 0.6f, s * 0.6f, 0xFFFFE060, FULL);
        Boxes.box(ps, g, -s * 1.4f, -s * 1.4f, -s * 1.4f, s * 1.4f, s * 1.4f, s * 1.4f, 0x50FF4010, FULL);
        ps.popPose();
    }

    // ------------------------------------------------------------------ helpers

    /** A box stretched between two points. */
    private static void segment(PoseStack ps, VertexConsumer vc, Vec3 a, Vec3 b, float w, int argb) {
        Vec3 d = b.subtract(a);
        double len = d.length();
        if (len < 1.0E-4) return;
        ps.pushPose();
        ps.translate(a.x, a.y, a.z);
        float yaw = (float) Mth.atan2(d.x, d.z);
        float pitch = (float) Math.atan2(Math.sqrt(d.x * d.x + d.z * d.z), d.y);
        ps.mulPose(Axis.YP.rotation(yaw));
        ps.mulPose(Axis.XP.rotation(pitch));
        Boxes.box(ps, vc, -w, 0, -w, w, (float) len, w, argb, FULL);
        ps.popPose();
    }

    private static float ease(float x) {
        return 1 - (1 - x) * (1 - x) * (1 - x);
    }

    @Override
    public ResourceLocation getTextureLocation(AbilityFx fx) {
        return ResourceLocation.withDefaultNamespace("textures/block/white_concrete.png");
    }
}
