package dev.freerun.chaos;

import dev.freerun.Freerun;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/** One packet each way for the silly stuff: podcasts, Mom's calls, and the dragon's offer. */
public final class ChaosPayloads {
    // server -> client
    public static final int PODCAST_QUESTION = 0, PODCAST_END = 1, MOM_CALL = 2, MOM_QUEST = 3, MOM_DONE = 4, DRAGON_ASK = 5,
            DRAGON_CREDITS = 6, PODCAST_LINE = 7;
    // client -> server
    public static final int PODCAST_ANSWER = 0, MOM_ANSWER = 1, DRAGON_ANSWER = 2;

    private ChaosPayloads() {}

    public record ToClient(int kind, int id, String text, List<String> lines) implements CustomPacketPayload {
        public static final Type<ToClient> TYPE = new Type<>(Freerun.id("chaos_s2c"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ToClient> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, ToClient::kind, ByteBufCodecs.VAR_INT, ToClient::id, ByteBufCodecs.STRING_UTF8, ToClient::text,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), ToClient::lines, ToClient::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ToServer(int kind, int id, int value) implements CustomPacketPayload {
        public static final Type<ToServer> TYPE = new Type<>(Freerun.id("chaos_c2s"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ToServer> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, ToServer::kind, ByteBufCodecs.VAR_INT, ToServer::id, ByteBufCodecs.VAR_INT, ToServer::value, ToServer::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void send(ServerPlayer p, int kind, int id, String text, List<String> lines) {
        if (ServerPlayNetworking.canSend(p, ToClient.TYPE)) ServerPlayNetworking.send(p, new ToClient(kind, id, text, lines));
    }

    static void register() {
        PayloadTypeRegistry.playS2C().register(ToClient.TYPE, ToClient.CODEC);
        PayloadTypeRegistry.playC2S().register(ToServer.TYPE, ToServer.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ToServer.TYPE, (payload, ctx) -> ctx.player().server.execute(() -> {
            ServerPlayer p = ctx.player();
            switch (payload.kind()) {
                case PODCAST_ANSWER -> CreeperPodcast.answer(p, payload.id(), payload.value());
                case MOM_ANSWER -> MomCalls.answer(p, payload.value() == 1);
                case DRAGON_ANSWER -> DragonDivorce.answer(p, payload.value() == 1);
                default -> {
                }
            }
        }));
    }
}
