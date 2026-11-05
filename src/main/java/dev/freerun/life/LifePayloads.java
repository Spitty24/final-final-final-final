package dev.freerun.life;

import dev.freerun.Freerun;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Two packets for all the city-life screens: the server opens one (job board, a work task, an
 * arcade game) with its data, and the client answers with what the player did.
 */
public final class LifePayloads {
    public static final int BOARD = 0, TASK = 1, ARCADE = 2;

    private LifePayloads() {}

    public record Open(int screen, CompoundTag data) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Freerun.id("life_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Open> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Open::screen, ByteBufCodecs.COMPOUND_TAG, Open::data, Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Action(int screen, CompoundTag data) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(Freerun.id("life_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Action> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Action::screen, ByteBufCodecs.COMPOUND_TAG, Action::data, Action::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    static void register() {
        PayloadTypeRegistry.playS2C().register(Open.TYPE, Open.CODEC);
        PayloadTypeRegistry.playC2S().register(Action.TYPE, Action.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Action.TYPE, (payload, ctx) -> ctx.player().server.execute(() -> {
            ServerPlayer p = ctx.player();
            switch (payload.screen()) {
                case BOARD -> OfficeJobs.pick(p, payload.data());
                case TASK -> OfficeJobs.answer(p, payload.data());
                case ARCADE -> Arcade.result(p, payload.data());
                default -> {}
            }
        }));
    }

    public static void open(ServerPlayer p, int screen, CompoundTag data) {
        if (ServerPlayNetworking.canSend(p, Open.TYPE)) ServerPlayNetworking.send(p, new Open(screen, data));
    }
}
