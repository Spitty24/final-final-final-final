package dev.freerun.mobs;

import dev.freerun.Freerun;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;

/** Registers the two soldier entities, their attributes, and their natural spawning. */
public final class SoldierRegistry {
    public static EntityType<UsSoldierEntity> US_SOLDIER;
    public static EntityType<FrenchSoldierEntity> FRENCH_SOLDIER;
    public static EntityType<GrumbusEntity> GRUMBUS;
    public static EntityType<WingusEntity> WINGUS;
    public static EntityType<FlavusEntity> FLAVUS;
    public static EntityType<RoadhouseNpcEntity> ROADHOUSE_NPC;
    public static EntityType<dev.freerun.world.SeatEntity> SEAT;

    private SoldierRegistry() {}

    public static void register() {
        ROADHOUSE_NPC = Registry.register(BuiltInRegistries.ENTITY_TYPE, Freerun.id("roadhouse_npc"),
                EntityType.Builder.of(RoadhouseNpcEntity::new, MobCategory.CREATURE)
                        .sized(0.6f, 1.8f).ridingOffset(-0.7f).clientTrackingRange(10).build("roadhouse_npc"));
        FabricDefaultAttributeRegistry.register(ROADHOUSE_NPC, RoadhouseNpcEntity.createAttributes());
        SEAT = Registry.register(BuiltInRegistries.ENTITY_TYPE, Freerun.id("seat"),
                EntityType.Builder.<dev.freerun.world.SeatEntity>of(dev.freerun.world.SeatEntity::new, MobCategory.MISC)
                        .sized(0.5f, 0.2f).clientTrackingRange(10).noSummon().build("seat"));

        FLAVUS = Registry.register(BuiltInRegistries.ENTITY_TYPE, Freerun.id("flavus"),
                EntityType.Builder.of(FlavusEntity::new, MobCategory.CREATURE)
                        .sized(0.6f, 0.95f).clientTrackingRange(10).build("flavus"));
        FabricDefaultAttributeRegistry.register(FLAVUS, FlavusEntity.createAttributes());

        WINGUS = Registry.register(BuiltInRegistries.ENTITY_TYPE, Freerun.id("wingus"),
                EntityType.Builder.of(WingusEntity::new, MobCategory.CREATURE)
                        .sized(0.9f, 0.7f).clientTrackingRange(10).build("wingus"));
        FabricDefaultAttributeRegistry.register(WINGUS, WingusEntity.createAttributes());

        GRUMBUS = Registry.register(BuiltInRegistries.ENTITY_TYPE, Freerun.id("grumbus"),
                EntityType.Builder.of(GrumbusEntity::new, MobCategory.CREATURE)
                        .sized(0.7f, 0.9f).clientTrackingRange(10).build("grumbus"));   // short & wide
        FabricDefaultAttributeRegistry.register(GRUMBUS, GrumbusEntity.createAttributes());

        US_SOLDIER = Registry.register(BuiltInRegistries.ENTITY_TYPE, Freerun.id("us_soldier"),
                EntityType.Builder.of(UsSoldierEntity::new, MobCategory.MONSTER)
                        .sized(0.6f, 1.95f).clientTrackingRange(10).build("us_soldier"));
        FRENCH_SOLDIER = Registry.register(BuiltInRegistries.ENTITY_TYPE, Freerun.id("french_soldier"),
                EntityType.Builder.of(FrenchSoldierEntity::new, MobCategory.MONSTER)
                        .sized(0.6f, 1.95f).clientTrackingRange(10).build("french_soldier"));

        FabricDefaultAttributeRegistry.register(US_SOLDIER, SoldierEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(FRENCH_SOLDIER, SoldierEntity.createAttributes());

        // US soldiers can turn up in any light (so you actually meet your ally); French are night/dark spawners.
        SpawnPlacements.register(US_SOLDIER, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkAnyLightMonsterSpawnRules);
        SpawnPlacements.register(FRENCH_SOLDIER, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);

        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), MobCategory.MONSTER,
                US_SOLDIER, 20, 1, 2);
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), MobCategory.MONSTER,
                FRENCH_SOLDIER, 25, 1, 3);
    }
}
