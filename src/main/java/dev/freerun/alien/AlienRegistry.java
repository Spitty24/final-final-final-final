package dev.freerun.alien;

import dev.freerun.Freerun;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/** The Swampwatch and its sounds. */
public final class AlienRegistry {
    public static Item SWAMPWATCH;
    public static SoundEvent TRANSFORM, REVERT, BEEP;
    public static net.minecraft.world.entity.EntityType<AbilityFx> FX;

    private AlienRegistry() {}

    public static void register() {
        SWAMPWATCH = Registry.register(BuiltInRegistries.ITEM, Freerun.id("swampwatch"),
                new SwampwatchItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
        TRANSFORM = sound("swampwatch_transform");
        REVERT = sound("swampwatch_revert");
        BEEP = sound("swampwatch_beep");
        FX = Registry.register(BuiltInRegistries.ENTITY_TYPE, Freerun.id("ability_fx"),
                net.minecraft.world.entity.EntityType.Builder.<AbilityFx>of(AbilityFx::new, net.minecraft.world.entity.MobCategory.MISC)
                        .sized(0.5f, 0.5f).clientTrackingRange(8).updateInterval(1).noSave().fireImmune().build("ability_fx"));
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(e -> e.accept(SWAMPWATCH));
        Transformations.register();
    }

    private static SoundEvent sound(String name) {
        return Registry.register(BuiltInRegistries.SOUND_EVENT, Freerun.id(name), SoundEvent.createVariableRangeEvent(Freerun.id(name)));
    }
}
