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

    private AlienRegistry() {}

    public static void register() {
        SWAMPWATCH = Registry.register(BuiltInRegistries.ITEM, Freerun.id("swampwatch"),
                new SwampwatchItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
        TRANSFORM = sound("swampwatch_transform");
        REVERT = sound("swampwatch_revert");
        BEEP = sound("swampwatch_beep");
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(e -> e.accept(SWAMPWATCH));
        Transformations.register();
    }

    private static SoundEvent sound(String name) {
        return Registry.register(BuiltInRegistries.SOUND_EVENT, Freerun.id(name), SoundEvent.createVariableRangeEvent(Freerun.id(name)));
    }
}
