package dev.freerun.factory;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * What the crane can pick up besides steel: people, animals, cars, forklifts, anything. Hooking
 * someone by the collar does not go over well: they complain the whole way up, and the ones who
 * can fight back remember who was on the pendant.
 */
public final class CraneLoad {
    private CraneLoad() {}

    static final String[] GENERIC = {
            "PUT ME DOWN!", "Hey! HEY! I'm not a pallet!", "This is a safety violation and I'm filing a report!",
            "I did NOT sign up for this!", "My lunch is still down there!", "Who's on the pendant? I'm getting your NAME!",
            "I'm telling Dustin about this!", "Is this a team-building thing? I hate it!", "Oh my god oh my god oh my god",
            "Okay, very funny. PUT. ME. DOWN.", "If you drop me I'm suing the whole plant!", "I can see my car from here. I hate that."};
    static final String[] ETHAN = {"Whoa. WHOA. Is this how the crane feels?", "Am I the load now? Do I count as a part?",
            "This is the best day of my life. Also put me down.", "Can you take me over to Slater's office? I want to wave."};
    static final String[] TARYN = {"Is this the pallet changer?! Am I being changed?!", "Put me down! I left the laser on! ...Did I?",
            "Okay this is way higher than the laser goes!"};
    static final String[] GUILD = {"You are making a career-ending decision.", "I will remember your face. I'm a doctor, I'm good with faces.",
            "Put me down or I'm writing you a prescription for consequences.", "This is assault. With a crane. I'm adding it to your chart."};
    static final String[] COOP = {"Is this part of the internship?!", "Can I put 'operated the crane' on my résumé now?!",
            "My advisor is going to hear about this!"};

    /** Something the hook can grab, and whether it's worth grabbing at all. */
    public static boolean liftable(Entity e) {
        if (e instanceof CraneEntity || e.isPassenger() || e.isSpectator() || !e.isAlive()) return false;
        if (e instanceof net.minecraft.world.entity.item.ItemEntity || e instanceof net.minecraft.world.entity.ExperienceOrb) return false;
        if (e instanceof dev.freerun.garage.LiftEntity || e instanceof net.minecraft.world.entity.decoration.HangingEntity) return false;
        if (e instanceof Player p && p.isCreative()) return true;
        return e instanceof SteelPartEntity || e instanceof LivingEntity || e instanceof net.minecraft.world.entity.vehicle.VehicleEntity;
    }

    /** Where on the entity the hook catches: the top of its box. */
    public static boolean underHook(Entity e, Vec3 tip) {
        AABB bb = e.getBoundingBox();
        boolean under = tip.x >= bb.minX - 0.3 && tip.x <= bb.maxX + 0.3 && tip.z >= bb.minZ - 0.3 && tip.z <= bb.maxZ + 0.3;
        double reach = e instanceof SteelPartEntity ? 0.4 : 0.7;
        return under && tip.y <= bb.maxY + 0.15 && tip.y >= bb.maxY - reach;
    }

    public static String name(Entity e) {
        return e.getName().getString();
    }

    /** The hook just closed on {@code e}. */
    static void grabbed(Entity e) {
        e.setNoGravity(true);
        if (e instanceof Mob m) {
            m.getNavigation().stop();
            m.setTarget(null);
        }
        if (e instanceof LivingEntity l && !(e instanceof dev.freerun.vehicle.CarEntity)) {
            if (e instanceof Player p) p.displayClientMessage(Component.literal("§cYou've been hooked by the overhead crane!"), true);
            else yell(l);
        }
    }

    /** Every tick while held. */
    static void carried(Entity e, Vec3 target, int tick) {
        e.resetFallDistance();
        if (e instanceof dev.freerun.vehicle.CarEntity car) car.suspend();
        Vec3 delta = target.subtract(e.position());
        Vec3 step = delta.lengthSqr() > 1.44 ? delta.normalize().scale(1.2) : delta.scale(0.6);
        Vec3 to = e.position().add(step);
        if (e instanceof ServerPlayer sp) {
            sp.connection.teleport(to.x, to.y, to.z, sp.getYRot(), sp.getXRot());
        } else {
            e.setPos(to.x, to.y, to.z);
            e.setDeltaMovement(Vec3.ZERO);
            e.hurtMarked = true;
        }
        for (Entity p : e.getPassengers()) p.resetFallDistance();
        if (e instanceof Mob m) m.getNavigation().stop();
        if (e instanceof LivingEntity l && !(e instanceof Player) && !(e instanceof dev.freerun.vehicle.CarEntity)) {
            if (tick % 50 == 25) yell(l);
            if (tick % 8 == 0) l.swing(net.minecraft.world.InteractionHand.MAIN_HAND);   // flailing
        }
    }

    /** Set down (or slipped off). Anyone who can hold a grudge now has one. */
    static void released(Entity e, UUID operator) {
        e.setNoGravity(false);
        e.resetFallDistance();
        if (!(e instanceof Mob m) || operator == null) return;
        Player op = e.level().getPlayerByUUID(operator);
        if (op == null || op.isCreative()) return;
        boolean fighter = m instanceof dev.freerun.laser.DrGuildEntity || m instanceof dev.freerun.gang.GangsterEntity
                || m instanceof net.minecraft.world.entity.monster.Enemy || m instanceof net.minecraft.world.entity.NeutralMob;
        if (fighter) m.setTarget(op);
        if (m instanceof LivingEntity l && l.hasCustomName()) {
            say(l, fighter ? "§cYou're going to regret that." : "§7Unbelievable. Un-be-lievable.");
        }
    }

    private static void yell(LivingEntity l) {
        String[] lines = l instanceof dev.freerun.mobs.EthanEntity ? ETHAN
                : l instanceof dev.freerun.laser.TarynEntity ? TARYN
                : l instanceof dev.freerun.laser.DrGuildEntity ? GUILD
                : l instanceof dev.freerun.mobs.CoopEntity ? COOP : GENERIC;
        boolean person = l.hasCustomName() || l instanceof dev.freerun.city.CitizenEntity || l instanceof net.minecraft.world.entity.npc.Npc;
        if (person) say(l, lines[l.getRandom().nextInt(lines.length)]);
        // and the noise they make about it
        net.minecraft.sounds.SoundEvent hurt = SoundEvents.VILLAGER_NO;
        if (!person) {
            l.playSound(SoundEvents.GENERIC_HURT, 0.6f, 1.4f);
        } else {
            l.playSound(hurt, 1.0f, 1.0f);
        }
    }

    private static void say(LivingEntity l, String text) {
        Component msg = Component.literal("§f<" + name(l) + "> §e" + text);
        for (Player p : l.level().players()) if (p.distanceToSqr(l) < 32 * 32) p.displayClientMessage(msg, false);
    }
}
