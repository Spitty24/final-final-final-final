package dev.freerun.chaos;

import dev.freerun.Freerun;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/**
 * The weird stuff: Lock-In energy drinks (and the crash), the Creeper podcast, Mom's calls, Reverse
 * Mode with its homesick dragon, and the Ender Dragon's divorce.
 */
public final class ChaosRegistry {
    public static Holder<MobEffect> LOCKED_IN, BRAIN_FOG;
    public static Item ENERGY_DRINK, EMPTY_CAN, MEATLOAF, BACKWARDS_CLOCK;
    public static EntityType<HomesickDragon> HOMESICK_DRAGON;
    public static SoundEvent LOFI, PHONE_RING, SAD_PIANO, PODCAST_JINGLE, CRASH;

    private ChaosRegistry() {}

    static final class Effect extends MobEffect {
        Effect(MobEffectCategory cat, int color) {
            super(cat, color);
        }
    }

    public static void register() {
        LOCKED_IN = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Freerun.id("locked_in"),
                new Effect(MobEffectCategory.BENEFICIAL, 0x7CFF4A)
                        .addAttributeModifier(Attributes.BLOCK_BREAK_SPEED, Freerun.id("effect.locked_in.mining"), 1.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
                        .addAttributeModifier(Attributes.MOVEMENT_SPEED, Freerun.id("effect.locked_in.speed"), 0.12, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        BRAIN_FOG = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Freerun.id("brain_fog"),
                new Effect(MobEffectCategory.HARMFUL, 0x9A9AB0)
                        .addAttributeModifier(Attributes.BLOCK_BREAK_SPEED, Freerun.id("effect.brain_fog.mining"), -0.45, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                        .addAttributeModifier(Attributes.MOVEMENT_SPEED, Freerun.id("effect.brain_fog.speed"), -0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

        EMPTY_CAN = Registry.register(BuiltInRegistries.ITEM, Freerun.id("empty_can"), new Item(new Item.Properties()));
        ENERGY_DRINK = Registry.register(BuiltInRegistries.ITEM, Freerun.id("energy_drink"), new EnergyDrinkItem(new Item.Properties().stacksTo(16)
                .food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.1f).alwaysEdible().usingConvertsTo(EMPTY_CAN).build())));
        MEATLOAF = Registry.register(BuiltInRegistries.ITEM, Freerun.id("moms_meatloaf"), new Item(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)
                .food(new FoodProperties.Builder().nutrition(12).saturationModifier(1.4f)
                        .effect(new MobEffectInstance(MobEffects.REGENERATION, 400, 1), 1.0f)
                        .effect(new MobEffectInstance(MobEffects.ABSORPTION, 2400, 1), 1.0f).build())));
        BACKWARDS_CLOCK = Registry.register(BuiltInRegistries.ITEM, Freerun.id("backwards_clock"), new BackwardsClockItem(
                new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

        HOMESICK_DRAGON = Registry.register(BuiltInRegistries.ENTITY_TYPE, Freerun.id("homesick_dragon"),
                EntityType.Builder.<HomesickDragon>of(HomesickDragon::new, MobCategory.MISC).sized(16.0f, 8.0f).fireImmune()
                        .clientTrackingRange(16).updateInterval(2).build("homesick_dragon"));
        FabricDefaultAttributeRegistry.register(HOMESICK_DRAGON, EnderDragon.createAttributes());

        LOFI = sound("lofi_beat");
        PHONE_RING = sound("phone_ring");
        SAD_PIANO = sound("sad_piano");
        PODCAST_JINGLE = sound("podcast_jingle");
        CRASH = sound("energy_crash");

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(e -> {
            e.accept(ENERGY_DRINK);
            e.accept(MEATLOAF);
        });
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(e -> e.accept(BACKWARDS_CLOCK));

        ChaosPayloads.register();
        CreeperPodcast.register();
        MomCalls.register();
        ReverseMode.register();
        DragonDivorce.register();
        ChaosCommand.register();

        // the crash: when Locked In runs out, the brain fog rolls in
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                MobEffectInstance in = p.getEffect(LOCKED_IN);
                if (in != null && in.getDuration() == 1) {
                    p.addEffect(new MobEffectInstance(BRAIN_FOG, 900, 0));
                    p.displayClientMessage(Component.literal("§7...and there's the crash. §8Brain fog."), true);
                    p.level().playSound(null, p.blockPosition(), CRASH, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
                }
            }
        });
    }

    private static SoundEvent sound(String name) {
        return Registry.register(BuiltInRegistries.SOUND_EVENT, Freerun.id(name), SoundEvent.createVariableRangeEvent(Freerun.id(name)));
    }
}
