package dev.freerun.net;

import dev.freerun.Freerun;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Store packets: the server opens a shop screen; the client buys or sells a line. */
public final class ShopPayloads {
    private ShopPayloads() {}

    /** Server -> client: open {@code shop} for the clerk with this entity id. */
    public record Open(int entityId, int shop) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Freerun.id("shop_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Open> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Open::entityId, ByteBufCodecs.VAR_INT, Open::shop, Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: buy (sell=false) or sell line {@code index}, {@code times} times. */
    public record Trade(int entityId, int index, boolean sell, int times) implements CustomPacketPayload {
        public static final Type<Trade> TYPE = new Type<>(Freerun.id("shop_trade"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Trade> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Trade::entityId, ByteBufCodecs.VAR_INT, Trade::index,
                ByteBufCodecs.BOOL, Trade::sell, ByteBufCodecs.VAR_INT, Trade::times, Trade::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
