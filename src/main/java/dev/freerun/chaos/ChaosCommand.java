package dev.freerun.chaos;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * /chaos — set off the weird stuff on demand, for testing:
 * mom, podcast, lockedin, crash, credits (anyone); reverse, dragon (needs cheats / op).
 */
public final class ChaosCommand {
    private ChaosCommand() {}

    static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> build(dispatcher));
    }

    private static void build(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("chaos")
                .then(Commands.literal("mom").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    MomCalls.callNow(p);
                    return ok(c.getSource(), "§dYour phone is about to ring...");
                }))
                .then(Commands.literal("podcast").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Vec3 at = p.position().add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(3));
                    Creeper cr = EntityType.CREEPER.create(p.serverLevel());
                    if (cr == null) return 0;
                    cr.moveTo(at.x, p.getY(), at.z, p.getYRot() + 180, 0);
                    cr.finalizeSpawn(p.serverLevel(), p.serverLevel().getCurrentDifficultyAt(cr.blockPosition()), MobSpawnType.COMMAND, null);
                    p.serverLevel().addFreshEntity(cr);
                    cr.setTarget(p);
                    CreeperPodcast.intercept(cr);
                    return ok(c.getSource(), "§aYou're on the Creeper Cast.");
                }))
                .then(Commands.literal("lockedin").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    p.removeEffect(ChaosRegistry.BRAIN_FOG);
                    p.addEffect(new MobEffectInstance(ChaosRegistry.LOCKED_IN, EnergyDrinkItem.DURATION, 0));
                    return ok(c.getSource(), "§a§lLOCKED IN.");
                }))
                .then(Commands.literal("crash").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    p.removeEffect(ChaosRegistry.LOCKED_IN);
                    p.addEffect(new MobEffectInstance(ChaosRegistry.BRAIN_FOG, 900, 0));
                    return ok(c.getSource(), "§7Brain fog.");
                }))
                .then(Commands.literal("credits").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    ChaosPayloads.send(p, ChaosPayloads.DRAGON_CREDITS, 0, p.getName().getString(), List.of());
                    return 1;
                }))
                .then(Commands.literal("reverse").requires(s -> s.hasPermission(2)).executes(c -> {
                    ReverseMode.toggle(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("dragon").requires(s -> s.hasPermission(2)).executes(c -> {
                    ReverseMode.spawnDragon(c.getSource().getPlayerOrException());
                    return 1;
                })));
    }

    private static int ok(CommandSourceStack s, String msg) {
        s.sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }
}
