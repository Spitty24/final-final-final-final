package dev.freerun.alien;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The ability key: one active power per creature, each on its own cooldown. */
public final class Abilities {
    private static final Map<UUID, Long> READY = new HashMap<>();
    /** Charges and dashes still running: player to ticks left. */
    private static final Map<UUID, int[]> RUNNING = new HashMap<>();

    private Abilities() {}

    public static void use(ServerPlayer p) {
        Form f = Transformations.form(p);
        if (!f.active()) return;
        long now = p.serverLevel().getGameTime();
        long ready = READY.getOrDefault(p.getUUID(), 0L);
        if (now < ready) {
            p.displayClientMessage(Component.literal("§7" + f.alien().activeName() + " in " + ((ready - now) / 20 + 1) + "s"), true);
            return;
        }
        if (!fire(p, f.alien(), now)) return;
        READY.put(p.getUUID(), now + f.alien().cooldownTicks);
        Transformations.markAbility(p, now);
    }

    private static LivingEntity lookTarget(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1.0f), end = eye.add(look.scale(range));
        AABB box = p.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(p, eye, end, box, e -> e instanceof LivingEntity && e != p && e.isAlive(), range * range);
        return hit != null && hit.getEntity() instanceof LivingEntity l ? l : null;
    }

    private static boolean fire(ServerPlayer p, Alien a, long now) {
        ServerLevel level = p.serverLevel();
        Vec3 pos = p.position();
        switch (a) {
            case MOSSBACK -> {
                // up, then the slam lands when you come down (see tick)
                p.setDeltaMovement(p.getDeltaMovement().x, 0.9, p.getDeltaMovement().z);
                p.hurtMarked = true;
                RUNNING.put(p.getUUID(), new int[]{0, 40});
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 0.6f, 1.4f);
            }
            case SKEETER -> {
                LivingEntity t = lookTarget(p, 4.0);
                if (t == null) {
                    p.displayClientMessage(Component.literal("§7Nothing to bite in reach."), true);
                    return false;
                }
                t.hurt(level.damageSources().playerAttack(p), 6.0f);
                p.heal(6.0f);
                AbilityFx.spawn(level, AbilityFx.DRAIN, p.getEyePosition().subtract(0, 0.3, 0), t, t.getEyePosition(), 1, 16, 0);
                level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, t.getX(), t.getY() + t.getBbHeight() * 0.6, t.getZ(), 8, 0.2, 0.2, 0.2, 0.1);
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BEE_STING, SoundSource.PLAYERS, 1.0f, 0.8f);
            }
            case MUDBUG -> {
                Vec3 look = p.getViewVector(1.0f);
                int hits = 0;
                AbilityFx.spawn(level, AbilityFx.SNAP, pos, null, null, 1, 10, p.getYRot());
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(3.5), e -> e != p)) {
                    Vec3 d = e.position().subtract(pos).normalize();
                    if (d.dot(look) < 0.3) continue;
                    e.hurt(level.damageSources().playerAttack(p), 10.0f);
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3));
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 1));
                    hits++;
                }
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5f, 1.8f);
                level.sendParticles(ParticleTypes.CRIT, pos.x + look.x * 1.5, pos.y + 1, pos.z + look.z * 1.5, 15, 0.6, 0.4, 0.6, 0.2);
                if (hits == 0) p.displayClientMessage(Component.literal("§7Snap! Nothing in the pincers."), true);
            }
            case VOLTBELLY -> {
                LivingEntity first = lookTarget(p, 14.0);
                if (first == null) {
                    p.displayClientMessage(Component.literal("§7No target for the arc."), true);
                    return false;
                }
                List<LivingEntity> chain = new ArrayList<>();
                chain.add(first);
                LivingEntity from = first;
                for (int k = 0; k < 2; k++) {
                    LivingEntity next = null;
                    double bd = 36;
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, from.getBoundingBox().inflate(6), e -> e != p && !chain.contains(e))) {
                        double d = e.distanceToSqr(from);
                        if (d < bd) {
                            bd = d;
                            next = e;
                        }
                    }
                    if (next == null) break;
                    chain.add(next);
                    from = next;
                }
                Vec3 a0 = p.getEyePosition();
                for (LivingEntity e : chain) {
                    e.hurt(level.damageSources().lightningBolt(), 8.0f);
                    Vec3 a1 = e.position().add(0, e.getBbHeight() * 0.5, 0);
                    AbilityFx.spawn(level, AbilityFx.ARC, a0, null, a1, 1, 9, 0);
                    AbilityFx.spawn(level, AbilityFx.ARC, a0, null, a1, 1, 6, 0);   // a second fork, so it crackles
                    for (int i = 0; i <= 12; i++) {
                        Vec3 q = a0.lerp(a1, i / 12.0);
                        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, q.x, q.y, q.z, 2, 0.1, 0.1, 0.1, 0.05);
                    }
                    a0 = a1;
                }
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.6f, 1.6f);
            }
            case CYPRESS -> {
                int n = 0;
                AbilityFx.spawn(level, AbilityFx.SHOCKWAVE, pos, null, null, 7.0f, 18, 0);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(7), e -> e != p)) {
                    AbilityFx.spawn(level, AbilityFx.ROOTS, e.position(), e, null, 1, 100, 0);
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 9));
                    e.addEffect(new MobEffectInstance(MobEffects.JUMP, 100, 128));   // can't jump out either
                    e.hurt(level.damageSources().cactus(), 3.0f);
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, e.getX(), e.getY(), e.getZ(), 10, 0.4, 0.5, 0.4, 0.0);
                    for (int i = 0; i < 6; i++) {
                        level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.ROOTED_DIRT.defaultBlockState()),
                                e.getX(), e.getY() + 0.2, e.getZ(), 4, 0.4, 0.2, 0.4, 0.1);
                    }
                    n++;
                }
                p.heal(2.0f + n);
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ROOTED_DIRT_BREAK, SoundSource.PLAYERS, 1.0f, 0.6f);
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.AZALEA_LEAVES_PLACE, SoundSource.PLAYERS, 1.0f, 0.7f);
            }
            case FOGLET -> {
                RUNNING.put(p.getUUID(), new int[]{1, 10});
                p.noPhysics = true;
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 1.0f, 1.5f);
            }
            case CLOCKROACH -> {
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 3, false, true, true));
                RUNNING.put(p.getUUID(), new int[]{3, 80});
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.SILVERFISH_AMBIENT, SoundSource.PLAYERS, 1.0f, 1.6f);
            }
            case BAYOU_BULL -> {
                RUNNING.put(p.getUUID(), new int[]{2, 30});
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 0.8f, 0.8f);
            }
            case SLAGWORM -> {
                Vec3 look = p.getViewVector(1.0f);
                SmallFireball ball = new SmallFireball(level, p, look.scale(0.6));
                ball.setPos(p.getEyePosition().add(look.scale(1.2)));
                level.addFreshEntity(ball);
                AbilityFx.spawn(level, AbilityFx.SLAG, ball.position(), ball, null, 1, 80, 0);
                level.sendParticles(ParticleTypes.LAVA, ball.getX(), ball.getY(), ball.getZ(), 8, 0.2, 0.2, 0.2, 0.0);
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.8f, 0.8f);
            }
            case GUMBO -> {
                AbilityFx.spawn(level, AbilityFx.SPLAT, pos, null, null, 6.0f, 26, 0);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(6), e -> e != p)) {
                    AbilityFx.spawn(level, AbilityFx.SPLAT, e.position(), e, null, Math.max(0.6f, e.getBbWidth() * 0.9f), 120, 0);
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 3));
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0));
                    e.hurt(level.damageSources().playerAttack(p), 3.0f);
                }
                for (int i = 0; i < 40; i++) {
                    double ang = i * Math.PI / 20;
                    level.sendParticles(ParticleTypes.ITEM_SLIME, pos.x + Math.cos(ang) * 2, pos.y + 0.5, pos.z + Math.sin(ang) * 2, 2, 0.3, 0.3, 0.3, 0.2);
                }
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1.0f, 0.6f);
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 0.8f, 0.7f);
            }
        }
        return true;
    }

    /** Running powers: the slam's landing, the mist dash, the stampede. Called every server tick. */
    public static void tick(ServerPlayer p) {
        int[] r = RUNNING.get(p.getUUID());
        if (r == null) return;
        ServerLevel level = p.serverLevel();
        Alien a = Transformations.alienOf(p);
        if (a == null) {
            RUNNING.remove(p.getUUID());
            p.noPhysics = false;
            return;
        }
        r[1]--;
        switch (r[0]) {
            case 0 -> {   // Mossback: slam when you land
                if (p.onGround() && r[1] < 36 || r[1] <= 0) {
                    RUNNING.remove(p.getUUID());
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(5), e -> e != p)) {
                        e.hurt(level.damageSources().playerAttack(p), 8.0f);
                        Vec3 d = e.position().subtract(p.position());
                        double len = Math.max(0.5, d.horizontalDistance());
                        e.push(d.x / len * 1.2, 0.6, d.z / len * 1.2);
                        e.hurtMarked = true;
                    }
                    level.sendParticles(ParticleTypes.EXPLOSION, p.getX(), p.getY() + 0.2, p.getZ(), 3, 1.5, 0.1, 1.5, 0.0);
                    AbilityFx.spawn(level, AbilityFx.SHOCKWAVE, p.position(), null, null, 5.0f, 22, 0);
                    level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(p.blockPosition().below())),
                            p.getX(), p.getY() + 0.2, p.getZ(), 60, 2.0, 0.3, 2.0, 0.3);
                    level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7f, 0.9f);
                    level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.8f, 0.5f);
                }
            }
            case 1 -> {   // Foglet: eight blocks straight ahead, through anything thin
                Vec3 look = p.getViewVector(1.0f);
                Vec3 step = new Vec3(look.x, look.y * 0.5, look.z).normalize().scale(0.8);
                Vec3 to = p.position().add(step);
                p.setDeltaMovement(Vec3.ZERO);
                p.connection.teleport(to.x, to.y, to.z, p.getYRot(), p.getXRot());
                p.resetFallDistance();
                level.sendParticles(ParticleTypes.CLOUD, to.x, to.y + 0.8, to.z, 6, 0.3, 0.5, 0.3, 0.02);
                AbilityFx.spawn(level, AbilityFx.MIST, p.position(), null, null, 1, 24, 0);
                if (r[1] <= 0) {
                    RUNNING.remove(p.getUUID());
                    // don't leave them inside a wall: back up until the body fits
                    Vec3 back = step.scale(-1);
                    Vec3 at = p.position();
                    for (int i = 0; i < 12 && !level.noCollision(p, p.getDimensions(p.getPose()).makeBoundingBox(at)); i++) at = at.add(back);
                    p.connection.teleport(at.x, at.y, at.z, p.getYRot(), p.getXRot());
                    p.noPhysics = false;
                }
            }
            case 2 -> {   // Bayou Bull: stampede
                Vec3 look = p.getViewVector(1.0f);
                p.setDeltaMovement(look.x * 1.0, p.getDeltaMovement().y, look.z * 1.0);
                p.hurtMarked = true;
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(1.2), e -> e != p && !e.getTags().contains("bulled"))) {
                    e.hurt(level.damageSources().playerAttack(p), 10.0f);
                    Vec3 side = new Vec3(-look.z, 0, look.x).scale(level.random.nextBoolean() ? 1 : -1);
                    e.push(side.x * 1.3 + look.x * 0.6, 0.7, side.z * 1.3 + look.z * 0.6);
                    e.hurtMarked = true;
                    e.addTag("bulled");
                }
                if (r[1] % 4 == 0) level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.HORSE_GALLOP, SoundSource.PLAYERS, 1.0f, 0.7f);
                level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, p.getX(), p.getY() + 0.3, p.getZ(), 2, 0.4, 0.1, 0.4, 0.01);
                if (r[1] % 2 == 0) AbilityFx.spawn(level, AbilityFx.DUST, p.position(), null, null, 1, 18, 0);
                if (r[1] <= 0) {
                    RUNNING.remove(p.getUUID());
                    for (Entity e : level.getEntities(p, p.getBoundingBox().inflate(16), e -> e.getTags().contains("bulled"))) e.removeTag("bulled");
                }
            }
            case 3 -> {   // Clockroach: scurry afterimages
                if (r[1] % 2 == 0 && p.getDeltaMovement().horizontalDistanceSqr() > 0.01) {
                    AbilityFx.spawn(level, AbilityFx.AFTERIMAGE, p.position(), null, null, 1, 12, p.getYRot());
                }
                if (r[1] <= 0) RUNNING.remove(p.getUUID());
            }
            default -> RUNNING.remove(p.getUUID());
        }
    }

    public static int cooldownLeft(Player p, long now) {
        long r = READY.getOrDefault(p.getUUID(), 0L);
        return (int) Math.max(0, r - now);
    }

    public static void forget(UUID id) {
        READY.remove(id);
        RUNNING.remove(id);
    }
}
