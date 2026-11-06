package dev.freerun.alien;

import dev.freerun.Freerun;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Who is what. The server owns the state and tells every client that can see the player; the
 * client keeps a copy per entity id for rendering, hitboxes and the HUD.
 */
public final class Transformations {
    private static final Map<UUID, Form> SERVER = new HashMap<>();
    private static final Map<UUID, Long> READY_AT = new HashMap<>();
    /** Client-side: entity id to form, filled by {@link AlienPayloads.Sync}. */
    public static final Map<Integer, Form> CLIENT = new ConcurrentHashMap<>();

    private static final ResourceLocation MOD_ID = Freerun.id("swampwatch");

    private Transformations() {}

    // ------------------------------------------------------------------ lookup (both sides)

    public static Form form(Player p) {
        if (p.level().isClientSide) return CLIENT.getOrDefault(p.getId(), Form.HUMAN);
        return SERVER.getOrDefault(p.getUUID(), Form.HUMAN);
    }

    public static Alien alienOf(Player p) {
        Form f = form(p);
        return f.transformed() ? f.alien() : null;
    }

    /** Seconds until the watch can be used again, 0 if it's charged. */
    public static int rechargeLeft(ServerPlayer p) {
        long at = READY_AT.getOrDefault(p.getUUID(), 0L);
        return (int) Math.max(0, (at - p.serverLevel().getGameTime()) / 20);
    }

    // ------------------------------------------------------------------ server

    public static void register() {
        AlienPayloads.register();
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                Form f = SERVER.get(p.getUUID());
                if (f != null && f.transformed()) tick(p, f);
                Abilities.tick(p);
            }
        });
        EntityTrackingEvents.START_TRACKING.register((tracked, viewer) -> {
            if (tracked instanceof ServerPlayer target) {
                Form f = SERVER.get(target.getUUID());
                if (f != null && f.transformed() && ServerPlayNetworking.canSend(viewer, AlienPayloads.Sync.TYPE)) {
                    ServerPlayNetworking.send(viewer, AlienPayloads.Sync.of(target.getId(), f));
                }
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            Form f = SERVER.remove(handler.player.getUUID());
            if (f != null && f.transformed()) strip(handler.player, f.alien());
            Abilities.forget(handler.player.getUUID());
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer p && form(p).transformed()) revert(p, true);
        });
    }

    private static void set(ServerPlayer p, Form f) {
        SERVER.put(p.getUUID(), f);
        p.refreshDimensions();
        AlienPayloads.Sync msg = AlienPayloads.Sync.of(p.getId(), f);
        ServerPlayNetworking.send(p, msg);
        for (ServerPlayer v : PlayerLookup.tracking(p)) if (v != p) ServerPlayNetworking.send(v, msg);
    }

    /** The dial was pressed. */
    public static void select(ServerPlayer p, Alien alien) {
        if (alien == null) return;
        Form cur = form(p);
        long now = p.serverLevel().getGameTime();
        if (cur.transformed()) {
            p.displayClientMessage(Component.literal("§cYou're already " + cur.alien().title + ". Turn back first."), true);
            return;
        }
        if (SwampwatchItem.find(p).isEmpty() && !p.isCreative()) return;
        int left = rechargeLeft(p);
        if (left > 0 && !p.isCreative()) {
            p.displayClientMessage(Component.literal("§cThe Swampwatch is recharging: " + left + "s"), true);
            p.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1.0f, 0.5f);
            return;
        }
        p.stopRiding();
        set(p, new Form(alien, Form.MORPH_IN, now, 0, now + Form.MORPH_IN_TICKS + Form.DURATION));
        ServerLevel level = p.serverLevel();
        level.playSound(null, p.getX(), p.getY(), p.getZ(), AlienRegistry.TRANSFORM, SoundSource.PLAYERS, 1.0f, 1.0f);
        level.sendParticles(ParticleTypes.GLOW, p.getX(), p.getY() + 1, p.getZ(), 40, 0.4, 0.8, 0.4, 0.05);
        level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 20, 0.3, 0.8, 0.3, 0.02);
    }

    /** Back to human: the watch timing out, the player asking, or dying. */
    public static void revert(ServerPlayer p, boolean instant) {
        Form f = form(p);
        if (!f.transformed()) return;
        long now = p.serverLevel().getGameTime();
        strip(p, f.alien());
        if (instant) {
            SERVER.remove(p.getUUID());
            set(p, Form.HUMAN);
        } else {
            set(p, f.with(Form.MORPH_OUT, now));
            p.serverLevel().playSound(null, p.getX(), p.getY(), p.getZ(), AlienRegistry.REVERT, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
        READY_AT.put(p.getUUID(), now + Form.RECHARGE);
        if (!p.isCreative()) p.displayClientMessage(Component.literal("§7Swampwatch recharging: 30s"), true);
    }

    /** The ability key was used: everyone sees the animation. */
    static void markAbility(ServerPlayer p, long now) {
        Form f = form(p);
        if (f.transformed()) set(p, f.ability(now));
    }

    private static void tick(ServerPlayer p, Form f) {
        long now = p.serverLevel().getGameTime();
        long t = now - f.phaseStart();
        switch (f.phase()) {
            case Form.MORPH_IN -> {
                p.setDeltaMovement(p.getDeltaMovement().multiply(0.2, 1, 0.2));
                if (t >= Form.MORPH_IN_TICKS) {
                    dress(p, f.alien());
                    set(p, f.with(Form.ACTIVE, now));
                    p.displayClientMessage(Component.literal("§a" + f.alien().title + "! §7" + f.alien().activeName() + " on the ability key."), true);
                }
            }
            case Form.ACTIVE -> {
                passives(p, f.alien(), now);
                long left = f.timeoutAt() - now;
                if (left == 200 || left == 100 || left == 60 || left == 40 || left == 20) {
                    p.playNotifySound(AlienRegistry.BEEP, SoundSource.PLAYERS, 1.0f, left <= 60 ? 1.3f : 1.0f);
                    p.displayClientMessage(Component.literal("§c" + (left / 20) + "s left"), true);
                }
                if (left <= 0) revert(p, false);
            }
            case Form.MORPH_OUT -> {
                if (t >= Form.MORPH_OUT_TICKS) {
                    SERVER.remove(p.getUUID());
                    set(p, Form.HUMAN);
                }
            }
            default -> {}
        }
    }

    // ------------------------------------------------------------------ what each form does to the player

    private static void mod(ServerPlayer p, Holder<Attribute> attr, String name, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = p.getAttribute(attr);
        if (inst == null) return;
        ResourceLocation id = MOD_ID.withSuffix("/" + name);
        inst.removeModifier(id);
        inst.addTransientModifier(new AttributeModifier(id, amount, op));
    }

    private static final String[] MODS = {"health", "armor", "speed", "damage", "jump", "step", "fall", "knockback", "reach", "breach"};

    /** Puts the form's stats on the player. */
    static void dress(ServerPlayer p, Alien a) {
        var add = AttributeModifier.Operation.ADD_VALUE;
        var mul = AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
        float hp = p.getHealth();
        switch (a) {
            case MOSSBACK -> {
                mod(p, Attributes.MAX_HEALTH, "health", 40, add);
                mod(p, Attributes.ARMOR, "armor", 12, add);
                mod(p, Attributes.MOVEMENT_SPEED, "speed", -0.25, mul);
                mod(p, Attributes.KNOCKBACK_RESISTANCE, "knockback", 1.0, add);
                mod(p, Attributes.ATTACK_DAMAGE, "damage", 3, add);
            }
            case SKEETER -> {
                mod(p, Attributes.MAX_HEALTH, "health", -10, add);
                mod(p, Attributes.MOVEMENT_SPEED, "speed", 0.2, mul);
                p.getAbilities().mayfly = true;
                p.getAbilities().flying = true;
                p.getAbilities().setFlyingSpeed(0.08f);
                p.onUpdateAbilities();
            }
            case MUDBUG -> {
                mod(p, Attributes.ARMOR, "armor", 14, add);
                mod(p, Attributes.ATTACK_DAMAGE, "damage", 4, add);
                mod(p, Attributes.MAX_HEALTH, "health", 10, add);
            }
            case VOLTBELLY -> {
                mod(p, Attributes.MOVEMENT_SPEED, "speed", 0.1, mul);
                mod(p, Attributes.ARMOR, "armor", 4, add);
            }
            case CYPRESS -> {
                mod(p, Attributes.ARMOR, "armor", 8, add);
                mod(p, Attributes.MAX_HEALTH, "health", 20, add);
                mod(p, Attributes.ENTITY_INTERACTION_RANGE, "reach", 2.0, add);
                mod(p, Attributes.BLOCK_INTERACTION_RANGE, "breach", 2.0, add);
                mod(p, Attributes.STEP_HEIGHT, "step", 0.5, add);
                mod(p, Attributes.MOVEMENT_SPEED, "speed", -0.1, mul);
            }
            case FOGLET -> {
                mod(p, Attributes.SAFE_FALL_DISTANCE, "fall", 200, add);
                mod(p, Attributes.MOVEMENT_SPEED, "speed", 0.15, mul);
            }
            case CLOCKROACH -> {
                mod(p, Attributes.MOVEMENT_SPEED, "speed", 0.6, mul);
                mod(p, Attributes.MAX_HEALTH, "health", -8, add);
                mod(p, Attributes.SAFE_FALL_DISTANCE, "fall", 10, add);
                mod(p, Attributes.ATTACK_DAMAGE, "damage", -2, add);
            }
            case BAYOU_BULL -> {
                mod(p, Attributes.MAX_HEALTH, "health", 20, add);
                mod(p, Attributes.ATTACK_DAMAGE, "damage", 6, add);
                mod(p, Attributes.ARMOR, "armor", 6, add);
                mod(p, Attributes.KNOCKBACK_RESISTANCE, "knockback", 1.0, add);
                mod(p, Attributes.STEP_HEIGHT, "step", 0.5, add);
            }
            case SLAGWORM -> {
                mod(p, Attributes.ARMOR, "armor", 6, add);
                mod(p, Attributes.MAX_HEALTH, "health", 10, add);
                mod(p, Attributes.MOVEMENT_SPEED, "speed", -0.15, mul);
            }
            case GUMBO -> {
                mod(p, Attributes.SAFE_FALL_DISTANCE, "fall", 200, add);
                mod(p, Attributes.JUMP_STRENGTH, "jump", 0.3, add);
                mod(p, Attributes.MAX_HEALTH, "health", 6, add);
            }
        }
        // the extra hearts come full
        p.setHealth(Math.min(p.getMaxHealth(), hp + Math.max(0, p.getMaxHealth() - 20)));
    }

    /** Takes the stats off again. */
    private static final java.util.List<Holder<Attribute>> ATTRS = java.util.List.of(Attributes.MAX_HEALTH, Attributes.ARMOR,
            Attributes.MOVEMENT_SPEED, Attributes.ATTACK_DAMAGE, Attributes.JUMP_STRENGTH, Attributes.STEP_HEIGHT, Attributes.SAFE_FALL_DISTANCE,
            Attributes.KNOCKBACK_RESISTANCE, Attributes.ENTITY_INTERACTION_RANGE, Attributes.BLOCK_INTERACTION_RANGE);

    static void strip(ServerPlayer p, Alien a) {
        for (String m : MODS) {
            ResourceLocation id = MOD_ID.withSuffix("/" + m);
            for (Holder<Attribute> attr : ATTRS) {
                AttributeInstance inst = p.getAttribute(attr);
                if (inst != null) inst.removeModifier(id);
            }
        }
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
        if (a == Alien.SKEETER && !p.isCreative() && !p.isSpectator()) {
            p.getAbilities().mayfly = false;
            p.getAbilities().flying = false;
            p.getAbilities().setFlyingSpeed(0.05f);
            p.onUpdateAbilities();
        }
        for (Holder<net.minecraft.world.effect.MobEffect> effect : java.util.List.of(MobEffects.FIRE_RESISTANCE, MobEffects.WATER_BREATHING,
                MobEffects.NIGHT_VISION, MobEffects.DOLPHINS_GRACE)) {
            MobEffectInstance inst = p.getEffect(effect);
            if (inst != null && inst.isAmbient()) p.removeEffect(effect);
        }
        p.noPhysics = false;
    }

    private static void hidden(ServerPlayer p, Holder<net.minecraft.world.effect.MobEffect> e, int amp) {
        p.addEffect(new MobEffectInstance(e, 60, amp, true, false, false));
    }

    /** Every tick while active. */
    private static void passives(ServerPlayer p, Alien a, long now) {
        ServerLevel level = p.serverLevel();
        switch (a) {
            case SKEETER -> {
                if (!p.getAbilities().mayfly) {
                    p.getAbilities().mayfly = true;
                    p.onUpdateAbilities();
                }
            }
            case MUDBUG, VOLTBELLY -> {
                hidden(p, MobEffects.WATER_BREATHING, 0);
                hidden(p, MobEffects.DOLPHINS_GRACE, 0);
                if (a == Alien.VOLTBELLY && p.isInWater() && now % 10 == 0) {
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(1.2), e -> e != p && e.isInWater())) {
                        e.hurt(level.damageSources().lightningBolt(), 2.0f);
                        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, e.getX(), e.getY() + 1, e.getZ(), 6, 0.3, 0.4, 0.3, 0.1);
                    }
                }
            }
            case CYPRESS -> {
                if ((level.isRainingAt(p.blockPosition()) || p.isInWater()) && now % 20 == 0) p.heal(1.0f);
            }
            case CLOCKROACH -> {
                hidden(p, MobEffects.NIGHT_VISION, 0);
                // scurry up walls: push against one and you climb it
                if (p.horizontalCollision && !p.isShiftKeyDown() && p.zza > 0) {
                    p.setDeltaMovement(p.getDeltaMovement().x, 0.22, p.getDeltaMovement().z);
                    p.resetFallDistance();
                }
            }
            case SLAGWORM -> {
                hidden(p, MobEffects.FIRE_RESISTANCE, 0);
                if (now % 10 == 0) {
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(2.0), e -> e != p && !(e instanceof Player))) {
                        e.igniteForSeconds(3);
                    }
                    level.sendParticles(ParticleTypes.LAVA, p.getX(), p.getY() + 0.5, p.getZ(), 1, 0.4, 0.3, 0.4, 0.0);
                }
            }
            case FOGLET -> {
                if (now % 4 == 0) level.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.3, p.getZ(), 1, 0.3, 0.2, 0.3, 0.0);
            }
            default -> {}
        }
    }

    /** Damage rules for forms: Foglet shrugs off projectiles, Gumbo's hits stick. */
    public static boolean blocksDamage(Player p, net.minecraft.world.damagesource.DamageSource source) {
        Alien a = alienOf(p);
        if (a == null) return false;
        if (a == Alien.FOGLET && source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)) return true;
        return (a == Alien.FOGLET || a == Alien.GUMBO) && source.is(net.minecraft.tags.DamageTypeTags.IS_FALL);
    }

    /** The player (as a form) just hit something. */
    public static void onHit(Player p, LivingEntity target) {
        Alien a = alienOf(p);
        if (a == Alien.GUMBO) target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
        if (a == Alien.SLAGWORM) target.igniteForSeconds(4);
        if (a == Alien.SKEETER && !(target instanceof Player)) p.heal(1.0f);
    }

    static AABB reach(Player p, double r) {
        return p.getBoundingBox().inflate(r);
    }
}
