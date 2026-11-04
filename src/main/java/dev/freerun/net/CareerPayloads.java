package dev.freerun.net;

import dev.freerun.Freerun;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/** Network messages for the career / skill tree system. */
public final class CareerPayloads {
    private CareerPayloads() {}

    /** Server -> client: your career data (and the XP you just earned, for the HUD). The low 8 bits of
     *  {@code career} are the career; the bits above are the completed-careers mask. */
    public record Sync(int career, int level, int xp, List<String> skills, int gained, String reason)
            implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(Freerun.id("career_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Sync> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Sync::career,
                ByteBufCodecs.VAR_INT, Sync::level,
                ByteBufCodecs.VAR_INT, Sync::xp,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Sync::skills,
                ByteBufCodecs.VAR_INT, Sync::gained,
                ByteBufCodecs.STRING_UTF8, Sync::reason,
                Sync::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server -> client: show the career picker (first time in the world). */
    public record OpenSelect(int unused) implements CustomPacketPayload {
        public static final Type<OpenSelect> TYPE = new Type<>(Freerun.id("career_open_select"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenSelect> CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, OpenSelect::unused, OpenSelect::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: I pick this career. */
    public record Choose(int career) implements CustomPacketPayload {
        public static final Type<Choose> TYPE = new Type<>(Freerun.id("career_choose"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Choose> CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, Choose::career, Choose::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: get promoted to the next career. */
    public record Prestige(int unused) implements CustomPacketPayload {
        public static final Type<Prestige> TYPE = new Type<>(Freerun.id("career_prestige"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Prestige> CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, Prestige::unused, Prestige::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: spend a point on this skill. */
    public record Unlock(String skill) implements CustomPacketPayload {
        public static final Type<Unlock> TYPE = new Type<>(Freerun.id("career_unlock"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Unlock> CODEC =
                StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Unlock::skill, Unlock::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
