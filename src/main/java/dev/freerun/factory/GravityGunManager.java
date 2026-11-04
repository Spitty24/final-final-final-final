package dev.freerun.factory;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Server side of the gravity gun. Each player can hold one thing; every tick we steer it toward a
 * point in front of their eyes with a velocity (so it still collides with the world instead of
 * phasing through walls), and draw a crackling beam to it.
 */
public final class GravityGunManager {
    private static final double RANGE = 20.0;
    private static final double MAX_HOLD_DISTANCE = 24.0;
    private static final double PULL = 0.45;
    private static final double MAX_SPEED = 1.5;

    private static final class Hold {
        final Entity entity;
        final double distance;
        final boolean priorNoGravity;
        int age;

        Hold(Entity entity, double distance, boolean priorNoGravity) {
            this.entity = entity;
            this.distance = distance;
            this.priorNoGravity = priorNoGravity;
        }
    }

    private static final Map<UUID, Hold> HOLDS = new HashMap<>();

    private GravityGunManager() {}

    /** Right-click with the gun. {@code clicked} is the entity under the crosshair if the game already found one. */
    public static void use(ServerPlayer player, Entity clicked) {
        Hold hold = HOLDS.get(player.getUUID());
        if (hold != null) {
            if (player.isShiftKeyDown()) {
                punt(player, hold);
            } else {
                drop(player, hold);
            }
            return;
        }
        Entity target = clicked != null && canGrab(clicked) ? clicked : pick(player);
        if (target == null || isHeld(target)) {
            player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.5f, 1.6f);
            return;
        }
        if (player.isShiftKeyDown()) {
            launch(player, target);   // punt it straight away, no pickup
            beam(player.serverLevel(), player, target, 10);
            return;
        }
        double dist = Mth.clamp(player.getEyePosition().distanceTo(target.getBoundingBox().getCenter()), 2.5, 4.0)
                + target.getBbWidth() * 0.5;
        HOLDS.put(player.getUUID(), new Hold(target, dist, target.isNoGravity()));
        if (!(target instanceof SteelPartEntity)) target.setNoGravity(true);
        if (target instanceof Mob mob) mob.getNavigation().stop();
        player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.6f, 1.9f);
    }

    public static boolean canGrab(Entity e) {
        if (e == null || !e.isAlive() || e.isSpectator() || e.isPassenger()) return false;
        if (e.getPassengers().stream().anyMatch(p -> p instanceof Player)) return false;
        if (e instanceof SteelPartEntity || e instanceof ItemEntity || e instanceof FallingBlockEntity
                || e instanceof PrimedTnt || e instanceof AbstractMinecart || e instanceof Boat) {
            return true;
        }
        return e instanceof LivingEntity && !(e instanceof Player)
                && !(e instanceof EnderDragon) && !(e instanceof WitherBoss);
    }

    private static boolean isHeld(Entity e) {
        for (Hold h : HOLDS.values()) {
            if (h.entity == e) return true;
        }
        return false;
    }

    /** First grabbable thing along the player's view, stopping at walls. Items get a fatter hitbox. */
    private static Entity pick(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0f);
        Vec3 end = eye.add(look.scale(RANGE));
        HitResult wall = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        double maxDist = wall.getType() == HitResult.Type.MISS ? RANGE : wall.getLocation().distanceTo(eye);
        end = eye.add(look.scale(maxDist));

        AABB search = player.getBoundingBox().expandTowards(look.scale(maxDist)).inflate(1.0);
        Entity best = null;
        double bestSq = maxDist * maxDist;
        for (Entity e : player.level().getEntities(player, search, GravityGunManager::canGrab)) {
            AABB bb = e.getBoundingBox().inflate(e instanceof ItemEntity ? 0.4 : 0.15);
            if (bb.contains(eye)) return e;
            Optional<Vec3> hit = bb.clip(eye, end);
            if (hit.isPresent()) {
                double d = eye.distanceToSqr(hit.get());
                if (d < bestSq) {
                    bestSq = d;
                    best = e;
                }
            }
        }
        return best;
    }

    public static void tick(MinecraftServer server) {
        if (HOLDS.isEmpty()) return;
        Iterator<Map.Entry<UUID, Hold>> it = HOLDS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Hold> entry = it.next();
            Hold h = entry.getValue();
            ServerPlayer p = server.getPlayerList().getPlayer(entry.getKey());
            Entity e = h.entity;
            boolean ok = p != null && p.isAlive() && !e.isRemoved() && e.level() == p.level()
                    && holdingGun(p) && p.distanceToSqr(e) < MAX_HOLD_DISTANCE * MAX_HOLD_DISTANCE;
            if (!ok) {
                restore(h);
                it.remove();
                continue;
            }

            Vec3 look = p.getViewVector(1.0f);
            Vec3 target = p.getEyePosition().add(look.scale(h.distance)).subtract(0.0, e.getBbHeight() / 2.0, 0.0);
            Vec3 v = target.subtract(e.position()).scale(PULL);
            if (v.length() > MAX_SPEED) v = v.normalize().scale(MAX_SPEED);
            e.setDeltaMovement(v);
            e.hurtMarked = true;
            e.resetFallDistance();
            if (e instanceof SteelPartEntity part) part.hold();
            if (e instanceof Mob mob) mob.getNavigation().stop();

            if (++h.age % 2 == 0) beam(p.serverLevel(), p, e, 1);
            if (h.age % 40 == 0) {
                p.level().playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.BEACON_AMBIENT,
                        SoundSource.PLAYERS, 0.25f, 2.0f);
            }
        }
    }

    private static boolean holdingGun(Player p) {
        return p.getMainHandItem().getItem() instanceof GravityGunItem
                || p.getOffhandItem().getItem() instanceof GravityGunItem;
    }

    private static void drop(ServerPlayer player, Hold h) {
        HOLDS.remove(player.getUUID());
        restore(h);
        h.entity.setDeltaMovement(h.entity.getDeltaMovement().scale(0.3));
        h.entity.hurtMarked = true;
        player.level().playSound(null, h.entity.getX(), h.entity.getY(), h.entity.getZ(),
                SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.5f, 2.0f);
    }

    private static void punt(ServerPlayer player, Hold h) {
        HOLDS.remove(player.getUUID());
        restore(h);
        launch(player, h.entity);
        beam(player.serverLevel(), player, h.entity, 10);
    }

    private static void launch(ServerPlayer player, Entity e) {
        double power = e instanceof SteelPartEntity part ? 2.2 / Math.sqrt(part.partCount())
                : e instanceof LivingEntity ? 1.6 : 2.0;
        e.setDeltaMovement(player.getViewVector(1.0f).scale(power).add(0.0, 0.15, 0.0));
        e.hurtMarked = true;
        player.level().playSound(null, e.getX(), e.getY(), e.getZ(),
                SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.PLAYERS, 1.0f, 0.7f);
    }

    private static void restore(Hold h) {
        if (!(h.entity instanceof SteelPartEntity) && !h.entity.isRemoved()) {
            h.entity.setNoGravity(h.priorNoGravity);
        }
    }

    /** Sparks along a line from the gun's muzzle to the held thing. */
    private static void beam(ServerLevel level, ServerPlayer p, Entity e, int burst) {
        Vec3 look = p.getViewVector(1.0f);
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 from = p.getEyePosition().add(look.scale(0.7)).add(right.scale(0.3)).add(0.0, -0.25, 0.0);
        Vec3 to = e.getBoundingBox().getCenter();
        int steps = 6;
        for (int i = 1; i <= steps; i++) {
            Vec3 pt = from.lerp(to, i / (double) steps);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pt.x, pt.y, pt.z, burst, 0.03, 0.03, 0.03, 0.02);
        }
    }

    public static void forget(ServerPlayer player) {
        Hold h = HOLDS.remove(player.getUUID());
        if (h != null) restore(h);
    }

    public static void releaseAll() {
        for (Hold h : HOLDS.values()) restore(h);
        HOLDS.clear();
    }
}
