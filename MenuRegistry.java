package dev.freerun.menu;

import dev.freerun.Freerun;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

/** Registers the big (3x4) crafting menu type. */
public final class MenuRegistry {
    public static MenuType<BigCraftingMenu> BIG_CRAFTING;

    private MenuRegistry() {}

    public static void register() {
        BIG_CRAFTING = Registry.register(BuiltInRegistries.MENU, Freerun.id("big_crafting"),
                new MenuType<>(BigCraftingMenu::new, FeatureFlags.VANILLA_SET));
    }
}
