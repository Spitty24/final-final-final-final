package dev.freerun.city;

import dev.freerun.career.Careers;
import dev.freerun.economy.Money;
import dev.freerun.vehicle.CarEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Dispatch and the wanted list. Hurting residents in a city makes you wanted (one star for a
 * fight, two for a killing, three for shooting at police); the nearest officer comes for you.
 * At one star they arrest you (a fine, and it's over); at two or more they're armed and so are
 * you, as far as they're concerned. Stay out of sight of any officer for a minute and they lose you.
 * Upper Management's Bodyguard Detail skill means your security handles it and nobody calls the cops.
 */
public final class Police {
    private static final class Wanted {
        int level;
        long last;
        long lastSeen;
    }

    private static final Map<UUID, Wanted> WANTED = new HashMap<>();

    private Police() {}

    public static int wanted(ServerPlayer p) {
        Wanted w = WANTED.get(p.getUUID());
        return w == null ? 0 : w.level;
    }

    private static void raise(ServerPlayer p, int to, String why) {
        if (p.isCreative() || p.isSpectator()) return;
        if (CityWorld.cityAt(p.serverLevel(), p.blockPosition()) == null) return;
        if (Careers.has(p, "bodyguard_detail")) {
            if (p.getRandom().nextInt(4) == 0) p.displayClientMessage(Component.literal("§8Your security detail makes the problem go away."), true);
            return;
        }
        Wanted w = WANTED.computeIfAbsent(p.getUUID(), k -> new Wanted());
        boolean fresh = w.level == 0;
        int before = w.level;
        w.level = Math.min(3, Math.max(w.level, to));
        w.last = w.lastSeen = p.serverLevel().getGameTime();
        if (w.level > before) {
            p.displayClientMessage(Component.literal("§c" + "★".repeat(w.level) + "§7" + "☆".repeat(3 - w.level)
                    + " §cWANTED §7— " + why), false);
        }
        if (fresh || w.level > before) dispatch(p.serverLevel(), p, p.blockPosition());
    }

    public static void reportAssault(ServerPlayer p, CitizenEntity victim) {
        raise(p, victim.isDeadOrDying() ? 2 : 1, victim.isDeadOrDying() ? "a resident was killed." : "assaulting a resident.");
    }

    public static void reportKill(ServerPlayer p) {
        raise(p, 2, "a resident was killed.");
    }

    public static void reportAttackOnOfficer(ServerPlayer p, PoliceOfficerEntity officer) {
        raise(p, 3, "attacking a police officer!");
        officer.respond(p);
    }

    /** Sends the nearest free officer (or a fresh cruiser, if none is close) after {@code suspect}. */
    public static PoliceOfficerEntity dispatch(ServerLevel level, LivingEntity suspect, BlockPos at) {
        AABB box = new AABB(at).inflate(140.0, 40.0, 140.0);
        PoliceOfficerEntity cop = level.getEntitiesOfClass(PoliceOfficerEntity.class, box, o -> !o.busy() && o.isAlive())
                .stream().min(Comparator.comparingDouble(o -> o.distanceToSqr(at.getCenter()))).orElse(null);
        if (cop == null) {
            BlockPos city = CityWorld.cityAt(level, at);
            if (city != null) cop = CityManager.spawnCruiser(level, city, at, 45, 80, true);
        }
        if (cop != null) cop.respond(suspect);
        return cop;
    }

    public static void arrestPlayer(ServerPlayer p, PoliceOfficerEntity officer) {
        Wanted w = WANTED.remove(p.getUUID());
        int fine = 50 * (w == null ? 1 : w.level);
        int paid = Math.min(fine, Money.balance(p));
        if (paid > 0) Money.pay(p, paid);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3));
        p.displayClientMessage(Component.literal("§9<" + officer.getName().getString() + "> §fYou're under arrest. "
                + "§7You pay a fine of " + Money.fmt(paid) + " and get a stern talking-to. Behave."), false);
        p.serverLevel().playSound(null, p.blockPosition(), dev.freerun.economy.ModSounds.CUFFS, net.minecraft.sounds.SoundSource.NEUTRAL, 1.0f, 1.0f);
    }

    /** Message players near a spot (radius in blocks). Players with Police Radio hear the whole city. */
    public static void announce(ServerLevel level, BlockPos at, String text, int radius) {
        Component msg = Component.literal(text);
        BlockPos city = CityWorld.cityAt(level, at);
        for (ServerPlayer p : level.players()) {
            boolean near = p.distanceToSqr(at.getCenter()) < (double) radius * radius;
            boolean radio = city != null && Careers.has(p, "police_radio") && city.equals(CityWorld.cityAt(level, p.blockPosition()));
            if (near || radio) p.displayClientMessage(msg, false);
        }
    }

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(level -> {
            if (level.getGameTime() % 40 != 0 || WANTED.isEmpty()) return;
            long now = level.getGameTime();
            WANTED.entrySet().removeIf(e -> {
                ServerPlayer p = level.getServer().getPlayerList().getPlayer(e.getKey());
                if (p == null) return true;
                if (p.serverLevel() != level) return false;
                Wanted w = e.getValue();
                boolean seen = !level.getEntitiesOfClass(PoliceOfficerEntity.class, p.getBoundingBox().inflate(40.0, 16.0, 40.0),
                        o -> o.hasLineOfSight(p) || o.getVehicle() instanceof CarEntity).isEmpty();
                if (seen) w.lastSeen = now;
                if (now - w.lastSeen > 20 * 60 || CityWorld.cityAt(level, p.blockPosition()) == null && now - w.last > 20 * 20) {
                    p.displayClientMessage(Component.literal("§a You lost the police. §7Wanted level cleared."), false);
                    for (PoliceOfficerEntity o : level.getEntitiesOfClass(PoliceOfficerEntity.class, p.getBoundingBox().inflate(200.0),
                            o -> o.suspect() == p)) o.standDown();
                    return true;
                }
                p.displayClientMessage(Component.literal("§c" + "★".repeat(w.level) + "§7" + "☆".repeat(3 - w.level)
                        + (seen ? " §cThe police are on you!" : " §7Lay low...")), true);
                return false;
            });
        });
    }
}
