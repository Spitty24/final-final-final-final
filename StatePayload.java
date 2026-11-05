package dev.freerun.net;

import dev.freerun.Freerun;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: "I am now in this parkour state." */
public record StatePayload(int state) implements CustomPacketPayload {
    public static final Type<StatePayload> TYPE = new Type<>(Freerun.id("state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StatePayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, StatePayload::state, StatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
