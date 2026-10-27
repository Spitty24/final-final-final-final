package dev.freerun.net;

import dev.freerun.Freerun;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: play a "caught" screen effect of the given kind. */
public record CaughtPayload(int kind) implements CustomPacketPayload {
    public static final int FAKE_OUT = 0;
    public static final int SCATTER = 1;
    public static final int CURSE = 2;
    public static final int DELETE_WORLD = 3;

    public static final Type<CaughtPayload> TYPE = new Type<>(Freerun.id("caught"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CaughtPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, CaughtPayload::kind, CaughtPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
