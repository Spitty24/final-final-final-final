package dev.freerun.career;

import dev.freerun.Freerun;
import dev.freerun.gang.GangsterEntity;
import dev.freerun.net.CareerPayloads;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server side of careers: storage (a Fabric data attachment on the player, kept through death),
 * XP and levelling, unlocking skills, and applying what they do.
 *
 * Levelling: you earn career XP for doing the job. Pressing parts +5, welding +8, cutting welds
 * +2, crane lifts +4 (set-downs +2), forklift lifts +4, making boudin +3, beating a hostile mob
 * +4 (a gangster +6), every 6 blocks mined +1, and +2 for every minute on the clock. Each level
 * needs 30 + 15 x level XP, up to level 20, and gives one skill point.
 */
public final class Careers {
    public static AttachmentType<CareerData> DATA;

    private static final Map<UUID, int[]> COUNTERS = new ConcurrentHashMap<>();   // {seconds online, blocks mined}
    private static final Map<UUID, Integer> PENDING_PICKER = new ConcurrentHashMap<>();

    private Careers() {}

    public static void register() {
        DATA = AttachmentRegistry.<CareerData>builder()
                .persistent(CareerData.CODEC)
                .copyOnDeath()
                .initializer(CareerData::fresh)
                .buildAndRegister(Freerun.id("career"));

        PayloadTypeRegistry.playS2C().register(CareerPayloads.Sync.TYPE, CareerPayloads.Sync.CODEC);
        PayloadTypeRegistry.playS2C().register(CareerPayloads.OpenSelect.TYPE, CareerPayloads.OpenSelect.CODEC);
        PayloadTypeRegistry.playC2S().register(CareerPayloads.Choose.TYPE, CareerPayloads.Choose.CODEC);
        PayloadTypeRegistry.playC2S().register(CareerPayloads.Unlock.TYPE, CareerPayloads.Unlock.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(CareerPayloads.Choose.TYPE,
                (payload, ctx) -> choose(ctx.player(), Career.byId(payload.career())));
        ServerPlayNetworking.registerGlobalReceiver(CareerPayloads.Unlock.TYPE,
                (payload, ctx) -> unlock(ctx.player(), payload.skill()));
        PayloadTypeRegistry.playC2S().register(CareerPayloads.Prestige.TYPE, CareerPayloads.Prestige.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(CareerPayloads.Prestige.TYPE, (payload, ctx) -> prestige(ctx.player()));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer p = handler.player;
            apply(p);
            sync(p, 0, "");
            CareerData d = get(p);
            if (d.careerType() == Career.NONE) {
                PENDING_PICKER.put(p.getUUID(), 60);   // give the world a few seconds to load first
            } else {
                p.displayClientMessage(Component.literal("§7Career: §f" + d.careerType().title + " §7(Level " + d.level()
                        + "). Press §fP§7 for your skill tree."), false);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            COUNTERS.remove(handler.player.getUUID());
            PENDING_PICKER.remove(handler.player.getUUID());
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            apply(newPlayer);
            sync(newPlayer, 0, "");
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            boolean second = server.getTickCount() % 20 == 0;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                Integer wait = PENDING_PICKER.get(p.getUUID());
                if (wait != null) {
                    if (wait <= 0) {
                        PENDING_PICKER.remove(p.getUUID());
                        if (get(p).careerType() == Career.NONE && ServerPlayNetworking.canSend(p, CareerPayloads.OpenSelect.TYPE)) {
                            ServerPlayNetworking.send(p, new CareerPayloads.OpenSelect(0));
                        }
                    } else {
                        PENDING_PICKER.put(p.getUUID(), wait - 1);
                    }
                }
                CareerData d = get(p);
                if (d.has("golden_parachute") && !p.onGround() && p.fallDistance > 3.5f && !p.getAbilities().flying) {
                    Skills.keep(p, MobEffects.SLOW_FALLING, 0, 40);
                }
                if (second) everySecond(p, d);
            }
        });

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayer sp)) return;
            int[] c = COUNTERS.computeIfAbsent(sp.getUUID(), k -> new int[2]);
            if (++c[1] % 6 == 0) addXp(sp, 1, "mining");
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (source.getEntity() instanceof ServerPlayer sp && entity instanceof Enemy) {
                addXp(sp, entity instanceof GangsterEntity ? 6 : 4, "combat");
            }
        });
    }

    // ------------------------------------------------------------------ data access

    public static CareerData get(Player p) {
        return ((AttachmentTarget) p).getAttachedOrCreate(DATA);
    }

    private static void set(ServerPlayer p, CareerData d) {
        ((AttachmentTarget) p).setAttached(DATA, d);
    }

    /** Works on both sides (the client only knows about its own player). */
    public static boolean has(Player p, String skill) {
        if (p.level().isClientSide) return CareerClientState.has(p, skill);
        return get(p).has(skill);
    }

    public static void sync(ServerPlayer p, int gained, String reason) {
        if (!ServerPlayNetworking.canSend(p, CareerPayloads.Sync.TYPE)) return;
        CareerData d = get(p);
        ServerPlayNetworking.send(p, new CareerPayloads.Sync(d.career() | (d.completed() << 8), d.level(), d.xp(),
                d.skills(), gained, reason == null ? "" : reason));
    }

    // ------------------------------------------------------------------ progress

    public static void addXp(ServerPlayer p, int base, String reason) {
        CareerData d = get(p);
        if (d.careerType() == Career.NONE || base <= 0) return;
        int gain = Math.max(1, (int) Math.round(base * Skills.xpMultiplier(d)));
        int level = d.level();
        int xp = d.xp() + gain;
        boolean leveled = false;
        while (level < CareerData.MAX_LEVEL && xp >= CareerData.xpToNext(level)) {
            xp -= CareerData.xpToNext(level);
            level++;
            leveled = true;
        }
        if (level >= CareerData.MAX_LEVEL) xp = 0;
        set(p, new CareerData(d.career(), level, xp, d.skills(), d.completed()));
        sync(p, gain, reason);
        if (leveled) {
            p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.2f);
            p.displayClientMessage(Component.literal("§6§lLevel up! §r§eYou're a Level " + level + " "
                    + d.careerType().title + ". §7Press P to spend your skill point."), false);
        }
    }

    public static void choose(ServerPlayer p, Career career) {
        if (career == Career.NONE) return;
        CareerData d = get(p);
        if (d.careerType() == career) return;
        if (d.isCompleted(career)) {
            p.displayClientMessage(Component.literal("§cYou've already mastered " + career.title + "."), true);
            return;
        }
        boolean switching = d.careerType() != Career.NONE;
        // Switching sideways drops the skills of the unfinished career; mastered careers stay.
        List<String> kept = d.skills().stream().filter(id -> {
            Skill s = Skills.get(id);
            return s != null && d.isCompleted(s.career());
        }).toList();
        set(p, new CareerData(career.ordinal(), d.level(), d.xp(), kept, d.completed()));
        apply(p);
        sync(p, 0, "");
        p.displayClientMessage(Component.literal("§6You're now in " + career.title + "."
                + (switching ? " §7Your unfinished skills were reset; you kept your level." : "")
                + " §7Press §fP§7 to open your skill tree."), false);
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.5f, 1.2f);
    }

    /**
     * Promotion: with every skill in your tree unlocked you move up to the next career at level 1,
     * keeping every skill you own. The finished career is marked with a prestige star.
     */
    public static void prestige(ServerPlayer p) {
        CareerData d = get(p);
        if (!d.canPrestige()) {
            p.displayClientMessage(Component.literal("§cUnlock every skill in your tree to get promoted."), true);
            return;
        }
        Career from = d.careerType();
        Career to = d.nextCareer();
        int completed = d.completed() | (1 << from.ordinal());
        set(p, new CareerData(to.ordinal(), 1, 0, d.skills(), completed));
        apply(p);
        sync(p, 0, "");
        p.displayClientMessage(Component.literal("§6§lPROMOTED! §r§eYou're now in " + to.title
                + " §7(prestige " + Integer.bitCount(completed) + "). §fEvery " + from.title
                + " skill stays with you."), false);
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.PLAYERS, 1.0f, 1.0f);
        if (p.level() instanceof net.minecraft.server.level.ServerLevel server) {
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.TOTEM_OF_UNDYING,
                    p.getX(), p.getY() + 1.0, p.getZ(), 60, 0.5, 0.8, 0.5, 0.4);
        }
    }

    public static void unlock(ServerPlayer p, String id) {
        Skill s = Skills.get(id);
        CareerData d = get(p);
        if (s == null || d.has(id)) return;
        String problem = null;
        if (s.career() != d.careerType()) problem = "That skill isn't in your career's tree.";
        else if (d.level() < s.requiredLevel()) problem = "You need to be level " + s.requiredLevel() + " for that.";
        else if (s.tier() > 1 && Skills.forCareer(s.career()).stream().noneMatch(o -> o.tier() == s.tier() - 1 && d.has(o.id())))
            problem = "Unlock a tier " + (s.tier() - 1) + " skill first.";
        else if (d.points() <= 0) problem = "No skill points left. Level up to earn more.";
        if (problem != null) {
            p.displayClientMessage(Component.literal("§c" + problem), true);
            return;
        }
        set(p, d.withSkill(id));
        apply(p);
        sync(p, 0, "");
        p.displayClientMessage(Component.literal("§aUnlocked: §f" + s.name()), true);
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8f, 1.2f);
    }

    // ------------------------------------------------------------------ effects

    private static ResourceLocation modId(Skill s, int i) {
        return Freerun.id("skill/" + s.id() + "/" + i);
    }

    /** Removes every skill bonus and re-adds the ones this player owns. */
    public static void apply(ServerPlayer p) {
        CareerData d = get(p);
        for (Skill s : Skills.all()) {
            for (int i = 0; i < s.mods().size(); i++) {
                Skill.Mod m = s.mods().get(i);
                AttributeInstance inst = p.getAttribute(m.attribute());
                if (inst == null) continue;
                ResourceLocation id = modId(s, i);
                if (d.has(s.id())) inst.addOrUpdateTransientModifier(new AttributeModifier(id, m.amount(), m.op()));
                else inst.removeModifier(id);
            }
        }
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    private static void everySecond(ServerPlayer p, CareerData d) {
        int[] c = COUNTERS.computeIfAbsent(p.getUUID(), k -> new int[2]);
        int seconds = ++c[0];
        if (d.careerType() == Career.NONE) return;
        if (seconds % 60 == 0) addXp(p, d.has("overtime") ? 4 : 2, "on the clock");
        for (String id : d.skills()) {
            Skill s = Skills.get(id);
            if (s != null && s.everySecond() != null) s.everySecond().accept(p, seconds);
        }
    }
}
