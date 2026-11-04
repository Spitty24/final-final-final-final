package dev.freerun.net;

import dev.freerun.Freerun;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: the forklift driver's fork keys. +1 raising, -1 lowering, 0 neither. */
public record ForkPayload(int dir) implements CustomPacketPayload {
    public static final Type<ForkPayload> TYPE = new Type<>(Freerun.id("fork"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ForkPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, ForkPayload::dir, ForkPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
