package dev.freerun.skate;

import dev.freerun.Freerun;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The server side of skating: the boards, relaying each skater's state to everyone watching, the
 * sounds other people hear, and no fall damage for a landed trick (a bail still hurts if it's high).
 */
public final class Skating {
    public static final Item[] BOARDS = new Item[4];
    public static SoundEvent ROLL, POP, LAND, GRIND, BAIL, PUSH;
    private static final Map<UUID, SkatePayloads.State> STATE = new HashMap<>();
    private static final Map<UUID, Long> LAST_BAIL = new HashMap<>();
    private static final String[] NAMES = {"skateboard", "skateboard_gator", "skateboard_bolt", "skateboard_crawfish"};
    private static final String[] GRAPHICS = {"Bayou deck: moon over the cypress", "Gator deck: mind the teeth",
            "Bolt deck: purple with a lightning strike", "Crawfish deck: a mudbug on cream"};

    private Skating() {}

    public static void register() {
        for (int i = 0; i < 4; i++) {
            BOARDS[i] = Registry.register(BuiltInRegistries.ITEM, Freerun.id(NAMES[i]),
                    new SkateboardItem(i, GRAPHICS[i], new Item.Properties().stacksTo(1)));
        }
        ROLL = sound("skate_roll");
        POP = sound("skate_pop");
        LAND = sound("skate_land");
        GRIND = sound("skate_grind");
        BAIL = sound("skate_bail");
        PUSH = sound("skate_push");
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(e -> {
            for (Item b : BOARDS) e.accept(b);
        });
        PayloadTypeRegistry.playC2S().register(SkatePayloads.State.TYPE, SkatePayloads.State.CODEC);
        PayloadTypeRegistry.playS2C().register(SkatePayloads.State.TYPE, SkatePayloads.State.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(SkatePayloads.State.TYPE, (payload, ctx) -> ctx.player().server.execute(() -> receive(ctx.player(), payload)));
        ServerPlayConnectionEvents.DISCONNECT.register((h, s) -> STATE.remove(h.player.getUUID()));
        // a landed trick doesn't hurt; a bail from up high still does
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayer p) || !source.is(DamageTypeTags.IS_FALL)) return true;
            SkatePayloads.State s = STATE.get(p.getUUID());
            if (s == null || !s.on()) return true;
            long bail = LAST_BAIL.getOrDefault(p.getUUID(), -100L);
            return p.serverLevel().getGameTime() - bail < 40 && amount > 4.0f;   // bailed off something tall
        });
    }

    private static SoundEvent sound(String name) {
        return Registry.register(BuiltInRegistries.SOUND_EVENT, Freerun.id(name), SoundEvent.createVariableRangeEvent(Freerun.id(name)));
    }

    public static boolean skating(ServerPlayer p) {
        SkatePayloads.State s = STATE.get(p.getUUID());
        return s != null && s.on();
    }

    private static void receive(ServerPlayer p, SkatePayloads.State s) {
        if (s.on() && SkateboardItem.find(p).isEmpty() && !p.isCreative()) s = SkatePayloads.State.OFF;
        if (dev.freerun.alien.Transformations.form(p).transformed()) s = SkatePayloads.State.OFF;
        s = s.withId(p.getId());
        STATE.put(p.getUUID(), s);
        if (s.event() == SkatePayloads.EV_BAIL) LAST_BAIL.put(p.getUUID(), p.serverLevel().getGameTime());
        // the sounds everyone else hears (the skater plays their own)
        SoundEvent snd = switch (s.event()) {
            case SkatePayloads.EV_POP -> POP;
            case SkatePayloads.EV_LAND -> LAND;
            case SkatePayloads.EV_BAIL -> BAIL;
            case SkatePayloads.EV_GRIND -> GRIND;
            case SkatePayloads.EV_PUSH -> PUSH;
            default -> null;
        };
        if (snd != null) p.serverLevel().playSound(p, p.getX(), p.getY(), p.getZ(), snd, SoundSource.PLAYERS, 0.8f, 1.0f);
        for (ServerPlayer v : PlayerLookup.tracking(p)) if (v != p) ServerPlayNetworking.send(v, s);
    }
}
