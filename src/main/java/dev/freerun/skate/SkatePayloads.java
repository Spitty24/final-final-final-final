package dev.freerun.skate;

import dev.freerun.Freerun;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * What a skater looks like this tick, sent by the skater's client and relayed to everyone who can
 * see them. {@code event} is a one-off sound: 0 none, 1 pop, 2 land, 3 bail, 4 grind start, 5 push.
 */
public record SkatePayloads() {
    public record State(int entityId, boolean on, int deck, float heading, float roll, float spin, float body, float pitch, float tilt,
                        int pose, float lean, int event) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(Freerun.id("skate_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC = StreamCodec.of((buf, s) -> {
            buf.writeVarInt(s.entityId);
            buf.writeBoolean(s.on);
            buf.writeVarInt(s.deck);
            buf.writeFloat(s.heading);
            buf.writeFloat(s.roll);
            buf.writeFloat(s.spin);
            buf.writeFloat(s.body);
            buf.writeFloat(s.pitch);
            buf.writeFloat(s.tilt);
            buf.writeVarInt(s.pose);
            buf.writeFloat(s.lean);
            buf.writeVarInt(s.event);
        }, buf -> new State(buf.readVarInt(), buf.readBoolean(), buf.readVarInt(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
                buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readVarInt(), buf.readFloat(), buf.readVarInt()));

        public static final State OFF = new State(0, false, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

        public State withId(int id) {
            return new State(id, on, deck, heading, roll, spin, body, pitch, tilt, pose, lean, event);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // poses, shared by the controller and the renderer
    public static final int RIDE = 0, PUSH = 1, CROUCH = 2, AIR = 3, GRAB_L = 4, GRAB_R = 5, MANUAL = 6, NOSE_MANUAL = 7,
            GRIND = 8, BOARDSLIDE = 9, POWERSLIDE = 10, BAIL = 11, GRAB_BOTH = 12;
    public static final int EV_NONE = 0, EV_POP = 1, EV_LAND = 2, EV_BAIL = 3, EV_GRIND = 4, EV_PUSH = 5;
}
