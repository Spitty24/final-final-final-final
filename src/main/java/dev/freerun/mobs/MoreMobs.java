package dev.freerun.mobs;

import dev.freerun.Freerun;
import dev.freerun.gang.GangTurfEntity;
import dev.freerun.gang.GangsterEntity;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

/** The Living Stone, Ethan James Patrick, and the parking-lot crews. */
public final class MoreMobs {
    public static EntityType<LivingStoneEntity> LIVING_STONE;
    public static EntityType<EthanEntity> ETHAN;
    public static EntityType<GangsterEntity> GANGSTER;
    public static EntityType<GangTurfEntity> GANG_TURF;

    public static Item LIVING_STONE_EGG;
    public static Item ETHAN_EGG;
    public static Item GANGSTER_EGG;

    private MoreMobs() {}

    public static void register() {
        LIVING_STONE = Registry.register(BuiltInRegistries.ENTITY_TYPE, Freerun.id("living_stone"),
                EntityType.Builder.of(LivingStoneEntity::new, MobCategory.CREATURE)
                        .sized(0.8f, 0.95f).clientTrackingRange(10).build("living_stone"));
        FabricDefaultAttributeRegistry.register(LIVING_STONE, LivingStoneEntity.createAttributes());

        ETHAN = Registry.register(BuiltInRegistries.ENTITY_TYPE, Freerun.id("ethan_james_patrick"),
                EntityType.Builder.of(EthanEntity::new, MobCategory.CREATURE)
                        .sized(0.6f, 2.9f).eyeHeight(2.55f).clientTrackingRange(10).build("ethan_james_patrick"));
        FabricDefaultAttributeRegistry.register(ETHAN, EthanEntity.createAttributes());

        GANGSTER = Registry.register(BuiltInRegistries.ENTITY_TYPE, Freerun.id("gangster"),
                EntityType.Builder.of(GangsterEntity::new, MobCategory.MONSTER)
                        .sized(0.6f, 1.95f).clientTrackingRange(10).build("gangster"));
        FabricDefaultAttributeRegistry.register(GANGSTER, GangsterEntity.createAttributes());

        GANG_TURF = Registry.register(BuiltInRegistries.ENTITY_TYPE, Freerun.id("gang_turf"),
                EntityType.Builder.<GangTurfEntity>of(GangTurfEntity::new, MobCategory.MISC)
                        .sized(0.5f, 0.5f).clientTrackingRange(4).updateInterval(100).noSummon().fireImmune()
                        .build("gang_turf"));

        LIVING_STONE_EGG = egg("living_stone_spawn_egg", new SpawnEggItem(LIVING_STONE, 0x7d7d7d, 0x5a5a5a, new Item.Properties()));
        ETHAN_EGG = egg("ethan_james_patrick_spawn_egg", new SpawnEggItem(ETHAN, 0x2b3a5c, 0xf08a24, new Item.Properties()));
        GANGSTER_EGG = egg("gangster_spawn_egg", new SpawnEggItem(GANGSTER, 0x2a2a2e, 0xd9731e, new Item.Properties()));
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(e -> {
            e.accept(LIVING_STONE_EGG);
            e.accept(ETHAN_EGG);
            e.accept(GANGSTER_EGG);
        });
    }

    private static Item egg(String name, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, Freerun.id(name), item);
    }
}
