package dev.freerun.city;

import dev.freerun.economy.Amenities;
import dev.freerun.economy.EconomyRegistry;
import dev.freerun.economy.Money;
import dev.freerun.economy.StoreBlocks;
import dev.freerun.gang.GangsterEntity;
import dev.freerun.mobs.RoadhouseNpcEntity;
import dev.freerun.net.PhonePayloads;
import dev.freerun.vehicle.CarEntity;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Blackwire, the phone's hacking app (server side). Look at something with your phone and press
 * H (or Shift+H for the second option). Every hack costs phone battery, which recharges on its own.
 *
 *   people        H: transfer from their bank account   Shift+H: blow up their phone with notifications
 *   NPC cars      H: kill the engine                    Shift+H: floor it, ignoring every light
 *   parked cars   H/Shift+H: set off the alarm
 *   gangsters     H: overload their phone (zap)         Shift+H: leak their location to the other crew
 *   shop clerks   H: skim the register (once every 5 min)
 *   traffic light H: every direction green              Shift+H: every direction red
 *   street light  H: black out the block for 30 s
 *   ATM           H: dispense cash (once every 10 min per ATM)
 *   gas pump      H: overload it (boom; once every 5 min)
 *   vending machine / TV: free snack / change the channel
 */
public final class Hacking {
    private static final Map<UUID, Long> COOLDOWN = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Long> BLOCK_LOCK = new ConcurrentHashMap<>();
    private static final Map<Integer, Long> CLERK_LOCK = new ConcurrentHashMap<>();
    private static final double RANGE = 48.0;

    private Hacking() {}

    public static ItemStack phone(ServerPlayer p) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.getItem() instanceof Amenities.Smartphone) return s;
        }
        return ItemStack.EMPTY;
    }

    private static void say(ServerPlayer p, String text) {
        p.displayClientMessage(Component.literal("§3[Blackwire] §f" + text), true);
    }

    private static boolean spend(ServerPlayer p, ItemStack phone, int cost) {
        if (p.getAbilities().instabuild) return true;
        float b = Amenities.Smartphone.battery(phone);
        if (b < cost) {
            say(p, "§cNot enough battery (" + (int) b + "% / " + cost + "% needed).");
            return false;
        }
        Amenities.Smartphone.setBattery(phone, b - cost);
        return true;
    }

    private static void beep(ServerPlayer p) {
        p.level().playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.PLAYERS, 0.6f, 1.8f);
    }

    private static void zap(ServerLevel level, Vec3 at) {
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 16, 0.3, 0.3, 0.3, 0.2);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.NEUTRAL, 0.6f, 2.0f);
    }

    public static void handle(ServerPlayer p, PhonePayloads.Hack h) {
        ServerLevel level = p.serverLevel();
        ItemStack phone = phone(p);
        if (phone.isEmpty()) return;
        long now = level.getGameTime();
        Long last = COOLDOWN.get(p.getUUID());
        if (last != null && now - last < 10) return;
        COOLDOWN.put(p.getUUID(), now);
        if (h.kind() == 0) {
            Entity e = level.getEntity(h.entityId());
            if (e == null || e.distanceTo(p) > RANGE || !(e instanceof LivingEntity || e instanceof CarEntity)) return;
            if (e instanceof LivingEntity le && !p.hasLineOfSight(le)) {
                say(p, "§cNo line of sight.");
                return;
            }
            hackEntity(p, level, phone, e, h.alt(), now);
        } else {
            BlockPos pos = h.pos();
            if (pos.distToCenterSqr(p.position()) > RANGE * RANGE) return;
            hackBlock(p, level, phone, pos, level.getBlockState(pos), h.alt(), now);
        }
    }

    private static void hackEntity(ServerPlayer p, ServerLevel level, ItemStack phone, Entity e, boolean alt, long now) {
        Vec3 at = e.getBoundingBox().getCenter();
        if (e instanceof CitizenEntity c) {
            if (alt) {
                if (!spend(p, phone, 10)) return;
                c.distract(200);
                say(p, "Blew up " + c.getName().getString() + "'s phone. They're not going anywhere for a bit.");
            } else {
                if (c.recentlyHacked()) {
                    say(p, "§e" + c.getName().getString() + "'s bank froze the account. Try again later.");
                    return;
                }
                if (!spend(p, phone, 25)) return;
                int amount = c.drain(10 + level.random.nextInt(51));
                if (amount <= 0) {
                    say(p, c.getName().getString() + " is completely broke. $0.");
                } else {
                    Money.give(p, amount);
                    say(p, "§aTransferred " + Money.fmt(amount) + " from " + c.getName().getString() + "'s account.");
                    p.level().playSound(null, p.blockPosition(), dev.freerun.economy.ModSounds.CASH_REGISTER, SoundSource.PLAYERS, 0.5f, 1.4f);
                }
            }
            zap(level, at);
        } else if (e instanceof CarEntity car) {
            if (car.npcDriven()) {
                if (!spend(p, phone, alt ? 30 : 20)) return;
                if (alt) {
                    car.joyride(240);
                    say(p, "Overrode the throttle. That car isn't stopping for anything.");
                } else {
                    car.stall(240);
                    say(p, "Killed the engine.");
                }
            } else {
                if (!spend(p, phone, 5)) return;
                for (int i = 0; i < 3; i++) car.honk();
                level.playSound(null, car.blockPosition(), SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.NEUTRAL, 2.0f, 1.5f);
                say(p, "Car alarm triggered.");
            }
            zap(level, at);
        } else if (e instanceof GangsterEntity g) {
            if (alt) {
                if (!spend(p, phone, 20)) return;
                int n = 0;
                for (GangsterEntity r : level.getEntitiesOfClass(GangsterEntity.class, g.getBoundingBox().inflate(40.0), r -> r.crew() != g.crew())) {
                    r.setTarget(g);
                    n++;
                }
                say(p, "Leaked their location to the " + (g.crew().rival().member) + "s. (" + n + " got the message.)");
            } else {
                if (!spend(p, phone, 30)) return;
                g.hurt(level.damageSources().indirectMagic(p, p), 5.0f);
                g.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3));
                say(p, "Overloaded their phone. That's gotta sting.");
            }
            zap(level, at);
        } else if (e instanceof RoadhouseNpcEntity n && n.shop() != null) {
            Long locked = CLERK_LOCK.get(n.getId());
            if (locked != null && now < locked) {
                say(p, "§eThat register's already been skimmed. Give it a few minutes.");
                return;
            }
            if (!spend(p, phone, 30)) return;
            CLERK_LOCK.put(n.getId(), now + 6000);
            int amount = 10 + level.random.nextInt(31);
            Money.give(p, amount);
            say(p, "§aSkimmed " + Money.fmt(amount) + " from the register's card reader.");
            zap(level, at);
        } else if (e instanceof LivingEntity le) {
            if (!spend(p, phone, 30)) return;
            le.hurt(level.damageSources().indirectMagic(p, p), 3.0f);
            say(p, "Zapped it.");
            zap(level, at);
        }
        beep(p);
    }

    private static boolean locked(BlockPos pos, long now, int ticks) {
        Long until = BLOCK_LOCK.get(pos);
        if (until != null && now < until) return true;
        BLOCK_LOCK.put(pos.immutable(), now + ticks);
        return false;
    }

    private static void hackBlock(ServerPlayer p, ServerLevel level, ItemStack phone, BlockPos pos, BlockState state, boolean alt, long now) {
        Block b = state.getBlock();
        Vec3 at = pos.getCenter();
        if (b instanceof TrafficLightBlock) {
            if (!spend(p, phone, 25)) return;
            BlockPos key = level.getBlockEntity(pos) instanceof CityLightBlockEntity be && be.intersection() != null ? be.intersection() : pos;
            TrafficSystem.force(level, key, alt ? TrafficSystem.Override.ALL_RED : TrafficSystem.Override.ALL_GREEN, 300);
            say(p, alt ? "Every light at this intersection is red. Gridlock." : "§eEvery direction is green. Stand back.");
        } else if (b instanceof StreetLampBlock) {
            if (!spend(p, phone, 30)) return;
            TrafficSystem.blackout(level, pos, 40.0, 600);
            say(p, "Blacked out the block for 30 seconds.");
        } else if (state.is(EconomyRegistry.ATM)) {
            if (locked(pos, now, 12000)) {
                say(p, "§eThis ATM's security patch kicked in. Try another one.");
                return;
            }
            if (!spend(p, phone, 35)) return;
            int amount = 10 + level.random.nextInt(31);
            for (ItemStack bill : Money.bills(amount)) Block.popResource(level, pos.above(), bill);
            level.playSound(null, pos, dev.freerun.economy.ModSounds.CASH_REGISTER, SoundSource.BLOCKS, 1.0f, 1.2f);
            say(p, "§aThe ATM spits out " + Money.fmt(amount) + ".");
        } else if (state.is(EconomyRegistry.GAS_PUMP)) {
            if (locked(pos, now, 6000)) {
                say(p, "§eThe pump's safety cut-off is still engaged.");
                return;
            }
            if (!spend(p, phone, 50)) return;
            level.explode(null, at.x, at.y + 0.5, at.z, 2.5f, false, Level.ExplosionInteraction.NONE);
            say(p, "§6Overloaded the pump.");
        } else if (state.is(EconomyRegistry.VENDING_MACHINE)) {
            if (!spend(p, phone, 10)) return;
            Block.popResourceFromFace(level, pos, state.getValue(StoreBlocks.VendingMachine.FACING),
                    new ItemStack(EconomyRegistry.VENDING.get(level.random.nextInt(EconomyRegistry.VENDING.size()))));
            level.playSound(null, pos, SoundEvents.DISPENSER_DISPENSE, SoundSource.BLOCKS, 1.0f, 0.8f);
            say(p, "Free snack. Nobody saw that.");
        } else if (state.is(EconomyRegistry.TV)) {
            if (!spend(p, phone, 5)) return;
            level.setBlock(pos, state.cycle(StoreBlocks.Tv.ON), Block.UPDATE_ALL);
            say(p, "Changed the channel.");
        } else {
            say(p, "§7Nothing to hack there.");
            return;
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 12, 0.3, 0.3, 0.3, 0.2);
        beep(p);
    }
}
