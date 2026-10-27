package dev.freerun.net;

import dev.freerun.Freerun;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> other clients: "player with this entity id is now in this state" (drives their animations). */
public record SyncPayload(int entityId, int state) implements CustomPacketPayload {
    public static final Type<SyncPayload> TYPE = new Type<>(Freerun.id("sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SyncPayload::entityId,
            ByteBufCodecs.VAR_INT, SyncPayload::state,
            SyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
