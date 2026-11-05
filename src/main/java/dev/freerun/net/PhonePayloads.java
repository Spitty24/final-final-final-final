package dev.freerun.net;

import dev.freerun.Freerun;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Smartphone traffic: the phone asks for city info, and the Blackwire app sends hacks. */
public final class PhonePayloads {
    private PhonePayloads() {}

    /** Client -> server: "what city am I in?" */
    public record Request(int unused) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(Freerun.id("phone_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, Request::unused, Request::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server -> client: the nearest city's stats (empty tag: no signal). */
    public record Data(CompoundTag tag) implements CustomPacketPayload {
        public static final Type<Data> TYPE = new Type<>(Freerun.id("phone_data"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Data> CODEC =
                StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, Data::tag, Data::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: hack the entity (kind 0) or block (kind 1) the player is looking at. */
    public record Hack(int kind, int entityId, BlockPos pos, boolean alt) implements CustomPacketPayload {
        public static final Type<Hack> TYPE = new Type<>(Freerun.id("phone_hack"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Hack> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Hack::kind, ByteBufCodecs.VAR_INT, Hack::entityId,
                BlockPos.STREAM_CODEC, Hack::pos, ByteBufCodecs.BOOL, Hack::alt, Hack::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
