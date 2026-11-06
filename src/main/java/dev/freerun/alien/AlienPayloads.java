package dev.freerun.alien;

import dev.freerun.Freerun;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Swampwatch packets: the server tells everyone what each player is; the client asks to change. */
public final class AlienPayloads {
    public static final int SELECT = 0, ABILITY = 1, REVERT = 2;

    private AlienPayloads() {}

    /** S2C: this player is now this form (alien -1 = human). */
    public record Sync(int entityId, int alien, int phase, long phaseStart, long abilityStart, long timeoutAt) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(Freerun.id("alien_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Sync> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Sync::entityId, ByteBufCodecs.VAR_INT, Sync::alien, ByteBufCodecs.VAR_INT, Sync::phase,
                ByteBufCodecs.VAR_LONG, Sync::phaseStart, ByteBufCodecs.VAR_LONG, Sync::abilityStart, ByteBufCodecs.VAR_LONG, Sync::timeoutAt,
                Sync::new);

        public static Sync of(int entityId, Form f) {
            return new Sync(entityId, f.alien() == null ? -1 : f.alien().ordinal(), f.phase(), f.phaseStart(), f.abilityStart(), f.timeoutAt());
        }

        public Form form() {
            return new Form(Alien.byOrdinal(alien), phase, phaseStart, abilityStart, timeoutAt);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** C2S: pick a creature on the dial, press the ability key, or turn back. */
    public record Action(int kind, int alien) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(Freerun.id("alien_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Action> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Action::kind, ByteBufCodecs.VAR_INT, Action::alien, Action::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    static void register() {
        PayloadTypeRegistry.playS2C().register(Sync.TYPE, Sync.CODEC);
        PayloadTypeRegistry.playC2S().register(Action.TYPE, Action.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Action.TYPE, (payload, ctx) -> ctx.player().server.execute(() -> {
            switch (payload.kind()) {
                case SELECT -> Transformations.select(ctx.player(), Alien.byOrdinal(payload.alien()));
                case ABILITY -> Abilities.use(ctx.player());
                case REVERT -> Transformations.revert(ctx.player(), false);
                default -> {}
            }
        }));
    }
}
