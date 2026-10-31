package dev.freerun.item;

import dev.freerun.Freerun;
import dev.freerun.evil.EvilRegistry;
import dev.freerun.item.gun.GunItem;
import dev.freerun.item.gun.GunType;
import dev.freerun.mobs.SoldierRegistry;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

/** Registers all items: guns, ammo, the nuke launcher, and spawn eggs; sorts them into creative tabs. */
public final class ItemRegistry {
    // guns
    public static Item PISTOL;
    public static Item RIFLE;
    public static Item SHOTGUN;
    // ammo
    public static Item BULLET;
    public static Item SHOTGUN_SHELL;
    // nuke
    public static Item NUKE_LAUNCHER;
    public static EntityType<NukeProjectile> NUKE_PROJECTILE;
    // misc tools
    public static Item GIANT_SHOVEL;
    // spawn eggs
    public static Item PURSUER_EGG;
    public static Item US_SOLDIER_EGG;
    public static Item FRENCH_SOLDIER_EGG;
    public static Item GRUMBUS_EGG;
    public static Item WINGUS_EGG;
    public static Item FLAVUS_EGG;
    public static Item ROADHOUSE_NPC_EGG;

    private ItemRegistry() {}

    public static Item gunItem(GunType type) {
        return switch (type) {
            case PISTOL -> PISTOL;
            case RIFLE -> RIFLE;
            case SHOTGUN -> SHOTGUN;
        };
    }

    public static void register() {
        BULLET = item("bullet", new Item(new Item.Properties()));
        SHOTGUN_SHELL = item("shotgun_shell", new Item(new Item.Properties()));

        PISTOL = item("pistol", new GunItem(GunType.PISTOL, new Item.Properties().stacksTo(1)));
        RIFLE = item("rifle", new GunItem(GunType.RIFLE, new Item.Properties().stacksTo(1)));
        SHOTGUN = item("shotgun", new GunItem(GunType.SHOTGUN, new Item.Properties().stacksTo(1)));

        GIANT_SHOVEL = item("giant_shovel", new GiantShovelItem(new Item.Properties().stacksTo(1)));

        NUKE_LAUNCHER = item("nuke_launcher", new NukeLauncherItem(new Item.Properties().stacksTo(1)));
        NUKE_PROJECTILE = Registry.register(BuiltInRegistries.ENTITY_TYPE, Freerun.id("nuke_projectile"),
                EntityType.Builder.<NukeProjectile>of(NukeProjectile::new, MobCategory.MISC)
                        .sized(0.5f, 0.5f).clientTrackingRange(8).build("nuke_projectile"));

        PURSUER_EGG = item("pursuer_spawn_egg",
                new SpawnEggItem(EvilRegistry.PURSUER, 0x161616, 0xb01010, new Item.Properties()));
        US_SOLDIER_EGG = item("us_soldier_spawn_egg",
                new SpawnEggItem(SoldierRegistry.US_SOLDIER, 0x525836, 0x9aa060, new Item.Properties()));
        FRENCH_SOLDIER_EGG = item("french_soldier_spawn_egg",
                new SpawnEggItem(SoldierRegistry.FRENCH_SOLDIER, 0x48587a, 0x8aa0cc, new Item.Properties()));
        GRUMBUS_EGG = item("grumbus_spawn_egg",
                new SpawnEggItem(SoldierRegistry.GRUMBUS, 0x8f8f94, 0xf0f0f0, new Item.Properties()));
        WINGUS_EGG = item("wingus_spawn_egg",
                new SpawnEggItem(SoldierRegistry.WINGUS, 0x6fb7e8, 0xf4e9ff, new Item.Properties()));
        FLAVUS_EGG = item("flavus_spawn_egg",
                new SpawnEggItem(SoldierRegistry.FLAVUS, 0x1b1b1e, 0xe8b830, new Item.Properties()));
        ROADHOUSE_NPC_EGG = item("roadhouse_npc_spawn_egg",
                new SpawnEggItem(SoldierRegistry.ROADHOUSE_NPC, 0x7a1c1c, 0xe8d7b0, new Item.Properties()));

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.COMBAT).register(entries -> {
            entries.accept(PISTOL);
            entries.accept(RIFLE);
            entries.accept(SHOTGUN);
            entries.accept(NUKE_LAUNCHER);
        });
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries ->
                entries.accept(GIANT_SHOVEL));
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(entries -> {
            entries.accept(BULLET);
            entries.accept(SHOTGUN_SHELL);
        });
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(entries -> {
            entries.accept(PURSUER_EGG);
            entries.accept(US_SOLDIER_EGG);
            entries.accept(FRENCH_SOLDIER_EGG);
            entries.accept(GRUMBUS_EGG);
            entries.accept(WINGUS_EGG);
            entries.accept(FLAVUS_EGG);
            entries.accept(ROADHOUSE_NPC_EGG);
        });
    }

    private static Item item(String name, Item instance) {
        return Registry.register(BuiltInRegistries.ITEM, Freerun.id(name), instance);
    }
}
