package dev.freerun.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Second-person view, the fourth stop on F5 (first person, behind, in front, then this): you see
 * yourself through the eyes of whoever is nearest and can see you. Your controls stay your own,
 * so walking toward the camera means walking toward them. With nobody around, an imaginary
 * onlooker stands a few blocks away.
 */
public final class SecondPerson {
    private static boolean active;
    private static CameraType last = CameraType.FIRST_PERSON;
    private static Entity watcher;
    private static int pickCooldown;
    private static Vec3 smoothed;

    private SecondPerson() {}

    public static boolean active() {
        return active;
    }

    /** Called every client tick: slots the extra mode into the F5 cycle. */
    public static void tick(Minecraft mc) {
        if (mc.player == null) {
            active = false;
            return;
        }
        CameraType now = mc.options.getCameraType();
        if (!active && last == CameraType.THIRD_PERSON_FRONT && now == CameraType.FIRST_PERSON) {
            active = true;   // after "in front", go second person instead of back to first
            watcher = null;
            smoothed = null;
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            now = CameraType.THIRD_PERSON_BACK;
        } else if (active && now != CameraType.THIRD_PERSON_BACK) {
            active = false;  // F5 again: back round to first person
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            now = CameraType.FIRST_PERSON;
        }
        last = now;
        if (!active) return;
        if (--pickCooldown <= 0 || watcher == null || !watcher.isAlive() || watcher.distanceTo(mc.player) > 32) {
            watcher = pick(mc);
            pickCooldown = 40;
        }
    }

    private static Entity pick(Minecraft mc) {
        Player me = mc.player;
        Entity best = null;
        double bd = Double.MAX_VALUE;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e == me || !(e instanceof LivingEntity) || e.isSpectator() || e.isInvisible()) continue;
            double d = e.distanceToSqr(me);
            if (d > 24 * 24 || d < 1.5 * 1.5) continue;
            Vec3 eye = e.getEyePosition();
            var hit = mc.level.clip(new ClipContext(eye, me.getEyePosition(), ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, e));
            if (hit.getType() != HitResult.Type.MISS) continue;
            if (d < bd) {
                bd = d;
                best = e;
            }
        }
        return best;
    }

    /** Where the camera goes and what it looks at, or null to leave vanilla's camera alone. */
    public static double[] camera(Minecraft mc, float partial) {
        if (!active || mc.player == null) return null;
        Player me = mc.player;
        Vec3 target = me.getPosition(partial).add(0, me.getBbHeight() * 0.6, 0);
        Vec3 eye;
        if (watcher != null && watcher.isAlive()) {
            eye = watcher.getEyePosition(partial);
            // just in front of their face, so their own head doesn't block the view
            Vec3 toMe = target.subtract(eye);
            if (toMe.lengthSqr() > 1.0E-4) eye = eye.add(toMe.normalize().scale(0.35));
        } else {
            // an imaginary onlooker: four blocks ahead of you, a little to the side, at head height
            float yaw = me.getViewYRot(partial) * Mth.DEG_TO_RAD;
            Vec3 fwd = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
            Vec3 side = new Vec3(-fwd.z, 0, fwd.x);
            eye = target.add(fwd.scale(4.0)).add(side.scale(1.5)).add(0, 0.6, 0);
            var hit = mc.level.clip(new ClipContext(target, eye, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, me));
            if (hit.getType() != HitResult.Type.MISS) eye = hit.getLocation().add(target.subtract(eye).normalize().scale(0.3));
        }
        smoothed = smoothed == null ? eye : smoothed.lerp(eye, 0.35);
        Vec3 d = target.subtract(smoothed);
        double h = Math.sqrt(d.x * d.x + d.z * d.z);
        float yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90.0f;
        float pitch = (float) -(Mth.atan2(d.y, h) * Mth.RAD_TO_DEG);
        return new double[]{smoothed.x, smoothed.y, smoothed.z, yaw, pitch};
    }

    public static void hud(GuiGraphics g, Minecraft mc) {
        if (!active || mc.options.hideGui) return;
        String who = watcher != null && watcher.isAlive() ? watcher.getName().getString() : "a stranger";
        String text = "§72nd person: §fyou, as seen by " + who;
        g.drawString(mc.font, text, (g.guiWidth() - mc.font.width(text)) / 2, 6, 0xFFFFFF, true);
    }
}
