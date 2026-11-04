package dev.freerun.vehicle;

import dev.freerun.Freerun;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;

/** Cars. */
public final class VehicleRegistry {
    public static EntityType<CarEntity> CAR;
    public static Item CAR_ITEM;

    private VehicleRegistry() {}

    public static void register() {
        CAR = Registry.register(BuiltInRegistries.ENTITY_TYPE, Freerun.id("car"),
                EntityType.Builder.of(CarEntity::new, MobCategory.MISC)
                        .sized(2.3f, 1.55f).eyeHeight(1.2f).clientTrackingRange(10).build("car"));
        FabricDefaultAttributeRegistry.register(CAR, CarEntity.createAttributes());
        CAR_ITEM = Registry.register(BuiltInRegistries.ITEM, Freerun.id("car"), new CarItem(new Item.Properties().stacksTo(1)));
    }
}
