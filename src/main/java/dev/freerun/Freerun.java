package dev.freerun;

import dev.freerun.evil.EvilConfig;
import dev.freerun.evil.EvilRegistry;
import dev.freerun.evil.PursuerEntity;
import dev.freerun.evil.PursuerManager;
import dev.freerun.net.CaughtPayload;
import dev.freerun.net.StatePayload;
import dev.freerun.net.SyncPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Common/server side. Movement itself is client-driven (like vanilla), the server only:
 *  - relays each player's state to everyone tracking them so third-person animations show,
 *  - cancels fall damage for safe moves and softens it for landing rolls.
 */
public final class Freerun implements ModInitializer {
    public static final String MOD_ID = "freerun";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Server-side view of each player's current move. */
    public static final class ServerEntry {
        public volatile ParkourState state = ParkourState.NONE;
        public volatile long rollArmedAt = Long.MIN_VALUE / 2;
    }

    public static final Map<UUID, ServerEntry> SERVER_STATES = new ConcurrentHashMap<>();
    private static final Map<UUID, PursuerManager> PURSUERS = new ConcurrentHashMap<>();
    private static final Map<UUID, dev.freerun.mobs.GrumbusManager> GRUMBI = new ConcurrentHashMap<>();
    private static final Map<UUID, dev.freerun.mobs.WingusManager> WINGI = new ConcurrentHashMap<>();

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        FreerunConfig.load();
        EvilConfig.load();
        dev.freerun.evil.ai.AiConfig.load();
        EvilRegistry.register();
        FabricDefaultAttributeRegistry.register(EvilRegistry.PURSUER, PursuerEntity.createAttributes());
        dev.freerun.mobs.SoldierRegistry.register();
        dev.freerun.world.StructureRegistry.register();
        dev.freerun.world.BlockRegistry.register();
        dev.freerun.item.ItemRegistry.register();
        dev.freerun.menu.MenuRegistry.register();

        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, env) -> dev.freerun.world.RoadhouseCommand.register(dispatcher));

        PayloadTypeRegistry.playC2S().register(StatePayload.TYPE, StatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncPayload.TYPE, SyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CaughtPayload.TYPE, CaughtPayload.CODEC);

        // Per-player Pursuer brain, ticked each server tick.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                PURSUERS.computeIfAbsent(player.getUUID(), k -> new PursuerManager()).tick(player);
                GRUMBI.computeIfAbsent(player.getUUID(), k -> new dev.freerun.mobs.GrumbusManager()).tick(player);
                WINGI.computeIfAbsent(player.getUUID(), k -> new dev.freerun.mobs.WingusManager()).tick(player);
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(StatePayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            ParkourState state = ParkourState.byId(payload.state());
            ServerEntry entry = SERVER_STATES.computeIfAbsent(player.getUUID(), k -> new ServerEntry());
            entry.state = state;
            if (state == ParkourState.ROLL_READY || state == ParkourState.ROLL) {
                entry.rollArmedAt = player.level().getGameTime();
            }
            if (state.safe) player.resetFallDistance();

            SyncPayload sync = new SyncPayload(player.getId(), state.ordinal());
            for (ServerPlayer other : PlayerLookup.tracking(player)) {
                if (other != player && ServerPlayNetworking.canSend(other, SyncPayload.TYPE)) {
                    ServerPlayNetworking.send(other, sync);
                }
            }
        });

        // Someone walks into view mid-move: tell them what that player is doing.
        EntityTrackingEvents.START_TRACKING.register((tracked, viewer) -> {
            if (!(tracked instanceof ServerPlayer target)) return;
            ServerEntry entry = SERVER_STATES.get(target.getUUID());
            if (entry != null && entry.state != ParkourState.NONE
                    && ServerPlayNetworking.canSend(viewer, SyncPayload.TYPE)) {
                ServerPlayNetworking.send(viewer, new SyncPayload(target.getId(), entry.state.ordinal()));
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            SERVER_STATES.remove(handler.player.getUUID());
            PURSUERS.remove(handler.player.getUUID());
            GRUMBI.remove(handler.player.getUUID());
            WINGI.remove(handler.player.getUUID());
        });
    }
}
