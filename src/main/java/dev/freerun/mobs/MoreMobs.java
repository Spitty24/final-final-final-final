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
    public static EntityType<JohnnySimEntity> JOHNNY_SIM;
    public static EntityType<CoopEntity> COOP;
    public static EntityType<JohnnyJamesPatrickEntity> JOHNNY_JAMES_PATRICK;
    public static EntityType<BigBoudinEntity> BIG_BOUDIN;

    public static Item LIVING_STONE_EGG;
    public static Item ETHAN_EGG;
    public static Item GANGSTER_EGG;
    public static Item PLASMA_CANNON;
    public static Item JOHNNY_SIM_EGG, COOP_EGG, JJP_EGG, BIG_BOUDIN_EGG;

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

        JOHNNY_SIM = Registry.register(BuiltInRegistries.ENTITY_TYPE, Freerun.id("johnny_sim"),
                EntityType.Builder.<JohnnySimEntity>of(JohnnySimEntity::new, MobCategory.CREATURE)
                        .sized(0.6f, 1.95f).clientTrackingRange(10).build("johnny_sim"));
        FabricDefaultAttributeRegistry.register(JOHNNY_SIM, dev.freerun.factory.ForkliftDriverEntity.createAttributes());
        COOP = Registry.register(BuiltInRegistries.ENTITY_TYPE, Freerun.id("coop_student"),
                EntityType.Builder.of(CoopEntity::new, MobCategory.CREATURE)
                        .sized(0.6f, 1.9f).clientTrackingRange(10).build("coop_student"));
        FabricDefaultAttributeRegistry.register(COOP, CoopEntity.createAttributes());
        JOHNNY_JAMES_PATRICK = Registry.register(BuiltInRegistries.ENTITY_TYPE, Freerun.id("johnny_james_patrick"),
                EntityType.Builder.of(JohnnyJamesPatrickEntity::new, MobCategory.CREATURE)
                        .sized(0.6f, 2.9f).eyeHeight(2.55f).clientTrackingRange(10).build("johnny_james_patrick"));
        FabricDefaultAttributeRegistry.register(JOHNNY_JAMES_PATRICK, JohnnyJamesPatrickEntity.createAttributes());
        BIG_BOUDIN = Registry.register(BuiltInRegistries.ENTITY_TYPE, Freerun.id("big_boudin"),
                EntityType.Builder.of(BigBoudinEntity::new, MobCategory.MONSTER)
                        .sized(1.0f, 3.3f).eyeHeight(2.5f).clientTrackingRange(10).build("big_boudin"));
        FabricDefaultAttributeRegistry.register(BIG_BOUDIN, BigBoudinEntity.createAttributes());
        JOHNNY_SIM_EGG = egg("johnny_sim_spawn_egg", new SpawnEggItem(JOHNNY_SIM, 0xd8c39a, 0xf2e600, new Item.Properties()));
        COOP_EGG = egg("coop_student_spawn_egg", new SpawnEggItem(COOP, 0xff7f1e, 0x3355aa, new Item.Properties()));
        JJP_EGG = egg("johnny_james_patrick_spawn_egg", new SpawnEggItem(JOHNNY_JAMES_PATRICK, 0x2b3a5c, 0xf2e600, new Item.Properties()));
        BIG_BOUDIN_EGG = egg("big_boudin_spawn_egg", new SpawnEggItem(BIG_BOUDIN, 0x96523a, 0xf0c41e, new Item.Properties()));

        LIVING_STONE_EGG = egg("living_stone_spawn_egg", new SpawnEggItem(LIVING_STONE, 0x7d7d7d, 0x5a5a5a, new Item.Properties()));
        ETHAN_EGG = egg("ethan_james_patrick_spawn_egg", new SpawnEggItem(ETHAN, 0x2b3a5c, 0xf08a24, new Item.Properties()));
        GANGSTER_EGG = egg("gangster_spawn_egg", new SpawnEggItem(GANGSTER, 0x2a2a2e, 0xd9731e, new Item.Properties()));
        PLASMA_CANNON = egg("plasma_cannon", new dev.freerun.item.PlasmaCannonItem(new Item.Properties().stacksTo(1)
                .rarity(net.minecraft.world.item.Rarity.EPIC)));
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.COMBAT).register(e -> e.accept(PLASMA_CANNON));
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(e -> {
            e.accept(LIVING_STONE_EGG);
            e.accept(ETHAN_EGG);
            e.accept(GANGSTER_EGG);
            e.accept(JOHNNY_SIM_EGG);
            e.accept(COOP_EGG);
            e.accept(JJP_EGG);
            e.accept(BIG_BOUDIN_EGG);
        });
    }

    private static Item egg(String name, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, Freerun.id(name), item);
    }
}
