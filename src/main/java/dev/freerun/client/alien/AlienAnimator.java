package dev.freerun.client.alien;

import dev.freerun.alien.Alien;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Procedural animation for the ten creatures: an idle, a walk cycle and the ability move for each.
 * {@code ability} runs 0 → 1 over the second after the ability key, 0 when nothing is happening.
 */
public final class AlienAnimator {
    private AlienAnimator() {}

    private static void rot(ModelPart p, float x, float y, float z) {
        if (p == null) return;
        p.xRot += x;
        p.yRot += y;
        p.zRot += z;
    }

    private static void move(ModelPart p, float x, float y, float z) {
        if (p == null) return;
        p.x += x;
        p.y += y;
        p.z += z;
    }

    /** A pulse that rises fast and settles: 0 at a=0 and a=1, peaks near a=0.3. */
    private static float pulse(float a) {
        if (a <= 0 || a >= 1) return 0;
        return a < 0.3f ? Mth.sin(a / 0.3f * Mth.HALF_PI) : Mth.cos((a - 0.3f) / 0.7f * Mth.HALF_PI);
    }

    public static void pose(AlienModel m, float limbSwing, float limbAmount, float age, float headYaw, float headPitch,
                            float ability, boolean crouching, boolean flying) {
        m.reset();
        Alien a = m.alien;
        float walk = Mth.cos(limbSwing * 0.6662f) * 1.2f * limbAmount;
        float walkB = Mth.cos(limbSwing * 0.6662f + Mth.PI) * 1.2f * limbAmount;
        float breathe = Mth.sin(age * 0.09f);
        float ab = pulse(ability);
        ModelPart head = m.part("head"), body = m.part("body"), armL = m.part("armL"), armR = m.part("armR"),
                legL = m.part("legL"), legR = m.part("legR"), tail = m.part("tail");
        // look where the player looks (not the worm or the eel: their whole body turns)
        if (head != null && a != Alien.SLAGWORM && a != Alien.CLOCKROACH) {
            head.yRot = headYaw * Mth.DEG_TO_RAD * 0.8f;
            head.xRot = headPitch * Mth.DEG_TO_RAD * 0.7f;
        }
        switch (a) {
            case MOSSBACK -> {
                rot(legL, walk * 0.6f, 0, 0);
                rot(legR, walkB * 0.6f, 0, 0);
                rot(armL, walkB * 0.5f, 0, -0.15f);
                rot(armR, walk * 0.5f, 0, 0.15f);
                move(body, 0, breathe * 0.3f, 0);
                move(m.part("shell"), 0, breathe * 0.3f, 0);
                rot(tail, 0, Mth.sin(age * 0.15f) * 0.2f, 0);
                if (ab > 0) {   // shell slam: arms up, head tucked, then a crouch
                    rot(armL, -2.6f * ab, 0, -0.5f * ab);
                    rot(armR, -2.6f * ab, 0, 0.5f * ab);
                    rot(head, 0.6f * ab, 0, 0);
                    move(head, 0, 2.5f * ab, 1.5f * ab);
                    rot(legL, -0.8f * ab, 0, 0.2f * ab);
                    rot(legR, -0.8f * ab, 0, -0.2f * ab);
                }
            }
            case SKEETER -> {
                float buzz = Mth.sin(age * 2.4f);
                rot(m.part("wingL"), 0, 0, 0.6f + buzz * 0.9f);
                rot(m.part("wingR"), 0, 0, -0.6f - buzz * 0.9f);
                for (String n : new String[]{"armL", "armR", "legL", "legR", "legL2", "legR2"}) {
                    rot(m.part(n), 0.35f + (flying ? 0.5f : walk * 0.4f), 0, n.endsWith("L") || n.endsWith("L2") ? 0.25f : -0.25f);
                }
                move(body, 0, breathe * 0.5f + (flying ? Mth.sin(age * 0.5f) : 0), 0);
                rot(m.part("abdomen"), -0.2f + breathe * 0.1f, 0, 0);
                if (ab > 0) {   // drain bite: lunge the head and proboscis forward
                    move(head, 0, 1.5f * ab, -4.0f * ab);
                    rot(head, 0.5f * ab, 0, 0);
                    rot(body, 0.3f * ab, 0, 0);
                }
            }
            case MUDBUG -> {
                rot(legL, walk * 0.7f, 0, 0);
                rot(legR, walkB * 0.7f, 0, 0);
                rot(m.part("sideL"), walkB * 0.6f, 0, 0.3f);
                rot(m.part("sideR"), walk * 0.6f, 0, -0.3f);
                rot(m.part("antL"), -0.3f + Mth.sin(age * 0.3f) * 0.15f, 0.25f, 0);
                rot(m.part("antR"), -0.3f + Mth.sin(age * 0.3f + 1) * 0.15f, -0.25f, 0);
                rot(armL, -0.4f + walkB * 0.2f, -0.3f, 0);
                rot(armR, -0.4f + walk * 0.2f, 0.3f, 0);
                rot(m.part("pincerL"), -0.9f, -0.3f, 0);
                rot(m.part("pincerR"), -0.9f, 0.3f, 0);
                rot(tail, Mth.sin(age * 0.2f) * 0.1f, 0, 0);
                if (ab > 0) {   // pincer snap: both claws swing in and clap
                    rot(armL, -1.2f * ab, 0.9f * ab, 0);
                    rot(armR, -1.2f * ab, -0.9f * ab, 0);
                    rot(m.part("pincerL"), -0.6f * ab, 1.2f * ab, 0);
                    rot(m.part("pincerR"), -0.6f * ab, -1.2f * ab, 0);
                    rot(body, 0.25f * ab, 0, 0);
                }
            }
            case VOLTBELLY -> {
                float s = Mth.sin(age * 0.25f + limbSwing * 0.5f);
                rot(body, 0, 0, s * 0.08f + walk * 0.1f);
                rot(m.part("tail1"), 0, 0, -s * 0.15f + walkB * 0.15f);
                rot(m.part("tail2"), 0, Mth.sin(age * 0.2f) * 0.2f + walk * 0.3f, s * 0.1f);
                rot(armL, 0, 0, -0.4f + Mth.sin(age * 0.4f) * 0.3f);
                rot(armR, 0, 0, 0.4f - Mth.sin(age * 0.4f) * 0.3f);
                rot(m.part("crest"), Mth.sin(age * 0.3f) * 0.1f, 0, 0);
                rot(m.part("finBack"), Mth.sin(age * 0.5f) * 0.2f, 0, 0);
                if (ab > 0) {   // arc: rear back, fins flared, then thrust the head forward
                    rot(body, -0.4f * ab, 0, 0);
                    rot(head, 0.9f * ab, 0, 0);
                    rot(armL, 0, 0, -1.4f * ab);
                    rot(armR, 0, 0, 1.4f * ab);
                    rot(m.part("crest"), -0.8f * ab, 0, 0);
                }
            }
            case CYPRESS -> {
                rot(legL, walk * 0.4f, 0, 0);
                rot(legR, walkB * 0.4f, 0, 0);
                float sway = Mth.sin(age * 0.05f) * 0.04f;
                rot(body, 0, 0, sway);
                rot(m.part("canopy"), Mth.sin(age * 0.07f) * 0.05f, Mth.sin(age * 0.03f) * 0.1f, sway * 2);
                rot(armL, walkB * 0.2f, 0, -0.4f + Mth.sin(age * 0.06f) * 0.05f);
                rot(armR, walk * 0.2f, 0, 0.4f - Mth.sin(age * 0.06f) * 0.05f);
                if (ab > 0) {   // root snare: arms thrown down and out, canopy shakes
                    rot(armL, 0.4f * ab, 0, -1.6f * ab);
                    rot(armR, 0.4f * ab, 0, 1.6f * ab);
                    rot(m.part("canopy"), 0, 0, Mth.sin(age * 2.0f) * 0.2f * ab);
                    move(body, 0, 2.0f * ab, 0);
                    move(head, 0, 2.0f * ab, 0);
                    move(m.part("canopy"), 0, 2.0f * ab, 0);
                }
            }
            case FOGLET -> {
                float drift = Mth.sin(age * 0.11f);
                move(body, Mth.sin(age * 0.07f) * 0.5f, drift * 0.8f, 0);
                move(head, Mth.sin(age * 0.07f + 0.5f) * 0.5f, drift * 1.2f, 0);
                rot(armL, walkB * 0.3f, 0, -0.5f + drift * 0.1f);
                rot(armR, walk * 0.3f, 0, 0.5f - drift * 0.1f);
                rot(tail, walk * 0.2f, Mth.sin(age * 0.13f) * 0.3f, 0);
                move(tail, 0, drift * 0.5f, 0);
                if (ab > 0) {   // mist dash: stretch forward and thin out
                    move(body, 0, 0, -3.0f * ab);
                    move(head, 0, 0, -5.0f * ab);
                    rot(armL, -1.2f * ab, 0, 0);
                    rot(armR, -1.2f * ab, 0, 0);
                    move(tail, 0, 0, 4.0f * ab);
                }
            }
            case CLOCKROACH -> {
                float fast = limbSwing * 2.0f;
                for (int i = 0; i < 3; i++) {
                    String l = i == 0 ? "armL" : i == 1 ? "legL" : "legL2", r = i == 0 ? "armR" : i == 1 ? "legR" : "legR2";
                    float ph = i * 2.1f;
                    rot(m.part(l), 0, Mth.cos(fast + ph) * 0.5f * limbAmount, 0);
                    rot(m.part(r), 0, Mth.cos(fast + ph + Mth.PI) * 0.5f * limbAmount, 0);
                }
                rot(m.part("antL"), Mth.sin(age * 0.5f) * 0.3f + 0.4f, 0.3f, 0);
                rot(m.part("antR"), Mth.sin(age * 0.5f + 1.5f) * 0.3f + 0.4f, -0.3f, 0);
                rot(head, Mth.sin(age * 0.3f) * 0.1f, headYaw * Mth.DEG_TO_RAD * 0.3f, 0);
                if (ab > 0) {   // scurry: body low, legs a blur
                    move(body, 0, 1.0f * ab, 0);
                    for (String n : new String[]{"armL", "armR", "legL", "legR", "legL2", "legR2"}) rot(m.part(n), 0, Mth.sin(age * 4f) * 0.8f * ab, 0);
                }
            }
            case BAYOU_BULL -> {
                rot(legL, walk * 0.7f, 0, 0);
                rot(legR, walkB * 0.7f, 0, 0);
                rot(armL, walkB * 0.6f, 0, -0.1f);
                rot(armR, walk * 0.6f, 0, 0.1f);
                move(body, 0, breathe * 0.4f, 0);
                move(m.part("belly"), 0, breathe * 0.4f, 0);
                rot(tail, 0.3f, Mth.sin(age * 0.2f) * 0.3f, 0);
                rot(head, Mth.sin(age * 0.08f) * 0.04f, 0, 0);
                if (ab > 0) {   // stampede: head down, horns forward, arms pumping
                    rot(head, 0.7f * ab, 0, 0);
                    move(head, 0, 2.0f * ab, -2.0f * ab);
                    rot(m.part("hornL"), 0.7f * ab, 0, 0);
                    rot(m.part("hornR"), 0.7f * ab, 0, 0);
                    rot(body, 0.35f * ab, 0, 0);
                    rot(armL, Mth.sin(age * 1.5f) * 1.0f * ab, 0, 0);
                    rot(armR, -Mth.sin(age * 1.5f) * 1.0f * ab, 0, 0);
                }
            }
            case SLAGWORM -> {
                float s = Mth.sin(age * 0.18f + limbSwing * 0.6f);
                rot(m.part("tail1"), s * 0.06f + walk * 0.08f, 0, Mth.sin(age * 0.12f) * 0.05f);
                rot(body, s * 0.1f + walk * 0.12f, headYaw * Mth.DEG_TO_RAD * 0.3f, 0);
                rot(head, -s * 0.12f + headPitch * Mth.DEG_TO_RAD * 0.5f, headYaw * Mth.DEG_TO_RAD * 0.5f, 0);
                rot(m.part("jaw"), 0.1f + Mth.max(0, Mth.sin(age * 0.3f)) * 0.2f, 0, 0);
                rot(armL, Mth.sin(age * 0.3f) * 0.2f, 0, -0.5f);
                rot(armR, Mth.sin(age * 0.3f + 2) * 0.2f, 0, 0.5f);
                if (ab > 0) {   // slag spit: rear back then lunge, jaw wide
                    float back = ability < 0.3f ? ab : 0;
                    rot(body, -0.5f * back + 0.4f * (ab - back), 0, 0);
                    rot(head, -0.4f * back + 0.5f * (ab - back), 0, 0);
                    rot(m.part("jaw"), 1.0f * ab, 0, 0);
                }
            }
            case GUMBO -> {
                float wobble = Mth.sin(age * 0.3f);
                rot(legL, walk * 0.5f, 0, 0);
                rot(legR, walkB * 0.5f, 0, 0);
                rot(armL, walkB * 0.4f, 0, -0.3f + wobble * 0.05f);
                rot(armR, walk * 0.4f, 0, 0.3f - wobble * 0.05f);
                float squash = crouching ? 1.0f : 0.0f;
                // squash and stretch: it jiggles with every step
                float jig = Mth.sin(limbSwing * 1.33f) * limbAmount * 0.08f;
                body.yScale = 1 + jig - squash * 0.55f;
                body.xScale = body.zScale = 1 - jig * 0.5f + squash * 0.25f;
                move(body, 0, squash * 6.0f, 0);
                move(head, 0, squash * 10.0f, 0);
                move(m.part("bits"), 0, squash * 6.0f, 0);
                move(m.part("drip"), 0, Mth.max(0, wobble) * 1.5f, 0);
                if (ab > 0) {   // splat: squash flat and spread
                    body.yScale *= 1 - 0.5f * ab;
                    body.xScale *= 1 + 0.6f * ab;
                    body.zScale *= 1 + 0.6f * ab;
                    move(body, 0, 5.0f * ab, 0);
                    move(head, 0, 8.0f * ab, 0);
                    rot(armL, 0, 0, -1.3f * ab);
                    rot(armR, 0, 0, 1.3f * ab);
                }
            }
        }
    }
}
