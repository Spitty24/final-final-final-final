package dev.freerun.evil;

import dev.freerun.Freerun;
import dev.freerun.net.CaughtPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What happens when the Pursuer catches you. Everything here stays inside the game:
 * screen effects, status effects, item scatter, respawn. Nothing touches the real machine.
 */
public final class CaughtHandler {

    public static void onCaught(ServerPlayer player, PursuerEntity pursuer) {
        EvilConfig cfg = EvilConfig.get();
        ServerLevel level = (ServerLevel) player.level();
        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 1.2f, 0.5f);

        EvilConfig.CaughtMode mode = cfg.caughtMode();

        // Every catch (on modes that leave you alive) steals items and takes some hearts.
        if (mode == EvilConfig.CaughtMode.FAKE_OUT || mode == EvilConfig.CaughtMode.CURSED_RESPAWN) {
            if (cfg.pursuerStealsItems) stealItems(player, cfg.pursuerStealCount);
            if (cfg.pursuerCatchHearts > 0) {
                player.hurt(level.damageSources().mobAttack(pursuer), (float) (cfg.pursuerCatchHearts * 2.0));
            }
        }

        // World deletion only makes sense for a single-player save and must be explicitly armed.
        // On a dedicated/LAN server the save is shared, so fall back to the hardcore scatter instead.
        if (mode == EvilConfig.CaughtMode.DELETE_WORLD) {
            boolean singlePlayer = player.server.isSingleplayerOwner(player.getGameProfile());
            if (cfg.allowWorldDeletion && singlePlayer && !player.server.isDedicatedServer()) {
                deleteWorld(player);
                return;
            }
            mode = EvilConfig.CaughtMode.HARDCORE_SCATTER;
        }

        switch (mode) {
            case FAKE_OUT -> fakeOut(player);
            case HARDCORE_SCATTER -> scatter(player, level);
            case CURSED_RESPAWN -> curse(player);
            default -> fakeOut(player);
        }
    }

    /** The Pursuer pockets a few random stacks from the player's inventory — they're just gone. */
    private static void stealItems(ServerPlayer player, int count) {
        var inv = player.getInventory();
        List<Integer> filled = new ArrayList<>();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (!inv.getItem(i).isEmpty()) filled.add(i);
        }
        Collections.shuffle(filled);
        int taken = 0;
        for (int slot : filled) {
            if (taken >= count) break;
            inv.removeItemNoUpdate(slot);
            taken++;
        }
        if (taken > 0) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§8It took something from you."), true);
        }
    }

    /** Tell the client to tear down and delete its own single-player save (the save lives on the client). */
    private static void deleteWorld(ServerPlayer player) {
        if (ServerPlayNetworking.canSend(player, CaughtPayload.TYPE)) {
            ServerPlayNetworking.send(player, new CaughtPayload(CaughtPayload.DELETE_WORLD));
        }
        Freerun.LOGGER.info("Pursuer caught {} — signalling DELETE_WORLD", player.getGameProfile().getName());
    }

    /** Looks like the game glitched out; costs nothing real. */
    private static void fakeOut(ServerPlayer player) {
        if (ServerPlayNetworking.canSend(player, CaughtPayload.TYPE)) {
            ServerPlayNetworking.send(player, new CaughtPayload(CaughtPayload.FAKE_OUT));
        }
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
        player.displayClientMessage(net.minecraft.network.chat.Component.literal("§8§kXX§r §7caught you."), false);
    }

    /** Hardcore-style: big hit, items scatter, world remembers the spot. */
    private static void scatter(ServerPlayer player, ServerLevel level) {
        if (ServerPlayNetworking.canSend(player, CaughtPayload.TYPE)) {
            ServerPlayNetworking.send(player, new CaughtPayload(CaughtPayload.SCATTER));
        }
        player.getInventory().dropAll();
        player.hurt(level.damageSources().magic(), Float.MAX_VALUE);
        BlockPos p = player.blockPosition();
        Freerun.LOGGER.info("Pursuer caught {} at {}", player.getGameProfile().getName(), p);
    }

    /** Survive, but cursed: lingering weakness until cleansed. */
    private static void curse(ServerPlayer player) {
        if (ServerPlayNetworking.canSend(player, CaughtPayload.TYPE)) {
            ServerPlayNetworking.send(player, new CaughtPayload(CaughtPayload.CURSE));
        }
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 2400, 1));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 2400, 1));
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
    }
}
