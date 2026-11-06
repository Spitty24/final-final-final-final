package dev.freerun.client.alien;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.freerun.Freerun;
import dev.freerun.alien.Alien;
import dev.freerun.alien.Form;
import dev.freerun.alien.Transformations;
import dev.freerun.client.laser.Boxes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.EnumMap;
import java.util.Map;

/**
 * Draws a transformed player: the creature in place of the player model, with the morph in and
 * out (a twist of light, the body growing out of it), the wrist watch blinking as time runs out,
 * and first-person arms. Also the spinning preview on the dial.
 */
public final class AlienRenderers {
    private static final Map<Alien, AlienModel> MODELS = new EnumMap<>(Alien.class);
    private static final ResourceLocation WHITE = ResourceLocation.withDefaultNamespace("textures/block/white_concrete.png");
    public static final int TEAL = 0x40E8C8, COPPER = 0xC87A3A;

    private AlienRenderers() {}

    public static AlienModel model(Alien a) {
        return MODELS.computeIfAbsent(a, k -> new AlienModel(k, Minecraft.getInstance().getEntityModels().bakeLayer(AlienModel.layer(k))));
    }

    public static ResourceLocation skin(Alien a) {
        return Freerun.id("textures/entity/alien/" + a.id() + ".png");
    }

    private static RenderType layer(Alien a) {
        return a == Alien.FOGLET || a == Alien.GUMBO ? RenderType.entityTranslucent(skin(a)) : RenderType.entityCutoutNoCull(skin(a));
    }

    /** Third person. Called from the PlayerRenderer mixin in place of the vanilla model. */
    public static void render(AbstractClientPlayer p, Form f, float partial, PoseStack ps, MultiBufferSource buffers, int light) {
        Alien a = f.alien();
        AlienModel m = model(a);
        long now = p.level().getGameTime();
        float t = now - f.phaseStart() + partial;
        float morph = 1.0f;   // 0 = no creature yet, 1 = fully formed
        boolean morphing = f.phase() != Form.ACTIVE;
        if (f.phase() == Form.MORPH_IN) morph = Mth.clamp((t - 6) / (Form.MORPH_IN_TICKS - 6), 0, 1);
        else if (f.phase() == Form.MORPH_OUT) morph = 1 - Mth.clamp(t / (Form.MORPH_OUT_TICKS - 4), 0, 1);
        float bodyYaw = Mth.rotLerp(partial, p.yBodyRotO, p.yBodyRot);
        float headYaw = Mth.rotLerp(partial, p.yHeadRotO, p.yHeadRot) - bodyYaw;
        float pitch = Mth.lerp(partial, p.xRotO, p.getXRot());
        float limb = 0, limbAmount = 0;
        if (!p.isPassenger()) {
            limbAmount = Math.min(1, p.walkAnimation.speed(partial));
            limb = p.walkAnimation.position(partial);
        }
        float age = p.tickCount + partial;
        float ability = f.abilityStart() > 0 ? (now - f.abilityStart() + partial) / 20.0f : 0;
        if (ability > 1) ability = 0;

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(180.0f - bodyYaw));
        if (morphing) {
            // twist of light the creature forms inside
            column(ps, buffers, t, f.phase() == Form.MORPH_IN ? Mth.clamp(t / 8f, 0, 1) * (1 - morph * 0.7f) : (1 - morph) * 0.8f, a);
        }
        if (morph > 0) {
            float s = morphing ? 0.2f + 0.8f * ease(morph) : 1.0f;
            float over = morphing && f.phase() == Form.MORPH_IN ? 1 + 0.15f * Mth.sin(morph * Mth.PI) : 1;
            ps.scale(s * over, s / over, s * over);
            ps.scale(-1.0f, -1.0f, 1.0f);
            ps.translate(0.0f, -1.501f, 0.0f);
            AlienAnimator.pose(m, limb, limbAmount, age, headYaw, pitch, ability, p.isCrouching(), p.getAbilities().flying);
            int color = a == Alien.FOGLET ? 0x99FFFFFF : 0xFFFFFFFF;
            int hurtOverlay = OverlayTexture.pack(0, p.hurtTime > 0);
            m.render(ps, buffers.getBuffer(layer(a)), light, hurtOverlay, color);
            if (morphing) {
                // the whole body lit teal while it forms
                int glow = (int) (200 * Mth.sin(morph * Mth.PI)) << 24 | TEAL;
                m.render(ps, buffers.getBuffer(RenderType.entityTranslucentEmissive(WHITE)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, glow);
            }
            wristwatch(m, f, now, ps, buffers);
        }
        ps.popPose();
        if (morphing && p.level().random.nextInt(2) == 0) {
            p.level().addParticle(ParticleTypes.GLOW, p.getRandomX(0.8), p.getY() + p.level().random.nextDouble() * 2, p.getRandomZ(0.8), 0, 0.08, 0);
        }
    }

    private static float ease(float x) {
        return x < 0.5f ? 4 * x * x * x : 1 - (float) Math.pow(-2 * x + 2, 3) / 2;
    }

    /** A spinning column of teal and copper light, the Swampwatch's signature. */
    private static void column(PoseStack ps, MultiBufferSource buffers, float t, float strength, Alien a) {
        if (strength <= 0.01f) return;
        VertexConsumer g = buffers.getBuffer(Boxes.GLOW);
        float h = Math.max(1.2f, a.height) + 0.4f;
        for (int i = 0; i < 6; i++) {
            float ang = t * 0.35f + i * Mth.TWO_PI / 6;
            float r = 0.55f + 0.15f * Mth.sin(t * 0.5f + i);
            float x = Mth.cos(ang) * r, z = Mth.sin(ang) * r;
            int c = (int) (160 * strength) << 24 | (i % 2 == 0 ? TEAL : COPPER);
            Boxes.box(ps, g, x - 0.05f, 0, z - 0.05f, x + 0.05f, h, z + 0.05f, c, 0xF000F0);
        }
        // and a floor ring
        for (int i = 0; i < 16; i++) {
            float ang = -t * 0.2f + i * Mth.TWO_PI / 16;
            float x = Mth.cos(ang) * 0.9f, z = Mth.sin(ang) * 0.9f;
            Boxes.box(ps, g, x - 0.08f, 0.02f, z - 0.08f, x + 0.08f, 0.06f, z + 0.08f, (int) (200 * strength) << 24 | TEAL, 0xF000F0);
        }
    }

    /** The watch stays on the left wrist, whatever the wrist looks like; it blinks red near the end. */
    private static void wristwatch(AlienModel m, Form f, long now, PoseStack ps, MultiBufferSource buffers) {
        ModelPart arm = m.part("armL");
        if (arm == null || f.alien() == Alien.FOGLET) return;
        long left = f.timeoutAt() - now;
        boolean warn = f.phase() == Form.ACTIVE && left < 200 && (now / 5) % 2 == 0;
        ps.pushPose();
        arm.translateAndRotate(ps);
        ps.scale(1 / 16f, 1 / 16f, 1 / 16f);
        float w = Math.max(1.5f, armWidth(m.alien) / 2 + 0.3f), y = armLength(m.alien) * 0.7f;
        VertexConsumer vc = buffers.getBuffer(Boxes.SOLID);
        Boxes.box(ps, vc, -w, y - 1.2f, -w, w, y + 1.2f, w, 0xFF2A3034, 0xF000F0);                   // strap
        VertexConsumer g = buffers.getBuffer(Boxes.GLOW);
        Boxes.box(ps, g, w - 0.2f, y - 1.0f, -1.0f, w + 0.6f, y + 1.0f, 1.0f, 0xFF000000 | (warn ? 0xFF3030 : TEAL), 0xF000F0);   // dial
        ps.popPose();
    }

    private static float armWidth(Alien a) {
        return switch (a) {
            case MOSSBACK -> 5; case BAYOU_BULL -> 7; case MUDBUG, GUMBO, CYPRESS, SLAGWORM -> 3; case SKEETER, CLOCKROACH -> 1; default -> 3;
        };
    }

    private static float armLength(Alien a) {
        return switch (a) {
            case MOSSBACK -> 12; case BAYOU_BULL -> 13; case CYPRESS -> 15; case SKEETER, CLOCKROACH -> 6; case SLAGWORM -> 5; default -> 8;
        };
    }

    /** First person: the creature's own arms in front of the camera. */
    public static void renderFirstPerson(Alien a, PoseStack ps, MultiBufferSource buffers, Player p, int light, float partial) {
        AlienModel m = model(a);
        AlienAnimator.pose(m, 0, 0, p.tickCount + partial, 0, 0, 0, false, false);
        float bob = Mth.sin((p.tickCount + partial) * 0.1f) * 0.01f;
        VertexConsumer vc = buffers.getBuffer(layer(a));
        for (int side = 0; side < 2; side++) {
            ModelPart arm = m.part(side == 0 ? "armR" : "armL");
            if (arm == null) continue;
            ps.pushPose();
            float sx = side == 0 ? 1 : -1;
            ps.translate(sx * 0.55f, -0.45f + bob, -0.7f);
            ps.mulPose(Axis.XP.rotationDegrees(-100.0f));
            ps.mulPose(Axis.YP.rotationDegrees(sx * 15.0f));
            ps.scale(-1.0f, -1.0f, 1.0f);
            ps.scale(1.1f, 1.1f, 1.1f);
            ps.translate(-arm.x / 16f, -arm.y / 16f, -arm.z / 16f);
            arm.xRot = 0;
            arm.yRot = 0;
            arm.zRot = 0;
            arm.render(ps, vc, light, OverlayTexture.NO_OVERLAY, a == Alien.FOGLET ? 0x99FFFFFF : 0xFFFFFFFF);
            ps.popPose();
        }
    }

    /** The dial's preview: the creature turning slowly on a pedestal of light. */
    public static void renderPreview(Alien a, PoseStack ps, MultiBufferSource buffers, float spin, float age) {
        AlienModel m = model(a);
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(spin));
        ps.scale(-1.0f, -1.0f, 1.0f);
        ps.translate(0.0f, -1.501f, 0.0f);
        AlienAnimator.pose(m, age * 0.5f, 0.35f, age, 0, 0, (age % 80) / 20f < 1 ? (age % 80) / 20f : 0, false, a == Alien.SKEETER);
        m.render(ps, buffers.getBuffer(layer(a)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, a == Alien.FOGLET ? 0x99FFFFFF : 0xFFFFFFFF);
        ps.popPose();
    }

    /** Is this player drawn as a creature right now? */
    public static Form formOf(Player p) {
        Form f = Transformations.form(p);
        return f.transformed() ? f : null;
    }
}
