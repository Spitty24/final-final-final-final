package dev.freerun.evil;

import dev.freerun.evil.ai.AiConfig;
import dev.freerun.evil.ai.HunterBrain;
import dev.freerun.evil.ai.HunterDecision;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Server-side brain for one player's Pursuer: decides when it spawns, ramps its speed with
 * survival time, and detects a "catch" to hand off to {@link CaughtHandler}.
 */
public final class PursuerManager {
    private int runTicks;
    private int surviveTicks;
    private PursuerEntity pursuer;

    private final HunterBrain brain = new HunterBrain();
    private int thinkTimer;
    private String lastTaunt = "";

    public void tick(ServerPlayer player) {
        EvilConfig cfg = EvilConfig.get();
        if (!cfg.evilMode || !cfg.pursuer) {
            despawn();
            runTicks = 0;
            surviveTicks = 0;
            return;
        }

        ServerLevel level = (ServerLevel) player.level();
        boolean moving = player.getDeltaMovement().horizontalDistanceSqr() > 0.01;
        runTicks = moving ? runTicks + 1 : Math.max(0, runTicks - 2);

        if (pursuer == null || !pursuer.isAlive()) {
            if (runTicks >= cfg.pursuerSpawnDelay) spawn(level, player, cfg);
            return;
        }

        surviveTicks++;
        double ramp = Mth.clamp(surviveTicks / (double) cfg.pursuerRampTicks, 0, 1);
        double speed = Mth.lerp(ramp, cfg.pursuerBaseSpeed, cfg.pursuerMaxSpeed);

        // --- AI brain (optional, only when a key is configured) ---
        AiConfig ai = AiConfig.get();
        if (ai.usable()) {
            double dist = pursuer.distanceTo(player);
            if (dist <= ai.thinkRange && ++thinkTimer >= ai.thinkIntervalTicks && brain.canCall(ai)) {
                brain.requestAsync(ai, situation(player, dist));
                thinkTimer = 0;
            }
            HunterDecision d = brain.latest();
            speed *= 0.6 + 0.8 * Mth.clamp((float) d.aggression, 0f, 1f);   // aggression scales speed
            if (ai.showTaunts && d.taunt != null && !d.taunt.isBlank() && !d.taunt.equals(lastTaunt)) {
                player.displayClientMessage(Component.literal("§c" + d.taunt), false);
                lastTaunt = d.taunt;
            }
            // "cutoff": head to the predicted intercept point instead of straight at the player.
            if ("cutoff".equals(d.strategy) && d.hasPoint()) {
                pursuer.getNavigation().moveTo(d.targetX, d.targetY, d.targetZ, 1.3);
            } else {
                pursuer.setTarget(player);
            }
        } else {
            pursuer.setTarget(player);
        }

        pursuer.setRampedSpeed(speed);

        if (pursuer.distanceTo(player) <= 1.6) {
            CaughtHandler.onCaught(player, pursuer);
            despawn();
            surviveTicks = 0;
            runTicks = 0;
        }
    }

    private void spawn(ServerLevel level, ServerPlayer player, EvilConfig cfg) {
        Vec3 behind = player.position().subtract(player.getLookAngle().scale(cfg.pursuerSpawnDistance));
        PursuerEntity e = new PursuerEntity(EvilRegistry.PURSUER, level);
        e.moveTo(behind.x, player.getY(), behind.z, player.getYRot(), 0);
        e.setTarget(player);
        level.addFreshEntity(e);
        pursuer = e;
        player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§cSomething is running after you."), true);
    }

    /** Compact snapshot of the chase, as JSON, for the model to reason over. */
    private String situation(ServerPlayer player, double dist) {
        Vec3 pv = player.getDeltaMovement();
        StringBuilder b = new StringBuilder(256);
        b.append('{');
        b.append("\"player\":{\"x\":").append(r(player.getX()))
                .append(",\"y\":").append(r(player.getY()))
                .append(",\"z\":").append(r(player.getZ()))
                .append(",\"yaw\":").append(r(player.getYRot()))
                .append(",\"vx\":").append(r(pv.x)).append(",\"vy\":").append(r(pv.y)).append(",\"vz\":").append(r(pv.z))
                .append(",\"sprinting\":").append(player.isSprinting())
                .append(",\"onGround\":").append(player.onGround())
                .append(",\"health\":").append(r(player.getHealth())).append('}');
        b.append(",\"hunter\":{\"x\":").append(r(pursuer.getX()))
                .append(",\"y\":").append(r(pursuer.getY()))
                .append(",\"z\":").append(r(pursuer.getZ())).append('}');
        b.append(",\"distance\":").append(r(dist));
        b.append(",\"surviveSeconds\":").append(surviveTicks / 20);
        b.append(",\"timeOfDay\":").append(player.level().getDayTime() % 24000);
        b.append('}');
        return b.toString();
    }

    private static double r(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private void despawn() {
        if (pursuer != null) {
            pursuer.discard();
            pursuer = null;
        }
    }
}
