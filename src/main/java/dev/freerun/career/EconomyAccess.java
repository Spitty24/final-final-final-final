package dev.freerun.career;

import net.minecraft.world.item.Item;

/** Late lookups for skill icons that live in registries created after the skills class loads. */
final class EconomyAccess {
    private EconomyAccess() {}

    static Item phone() {
        return dev.freerun.economy.EconomyRegistry.SMARTPHONE;
    }
}
