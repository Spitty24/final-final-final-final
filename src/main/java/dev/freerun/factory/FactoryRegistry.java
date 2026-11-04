package dev.freerun.factory;

import dev.freerun.Freerun;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.phys.Vec3;
import dev.freerun.net.ForkPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Everything for the welding factory: steel parts, the machines, the torch and the gravity gun. */
public final class FactoryRegistry {
    public static EntityType<SteelPartEntity> STEEL_PART;
    public static EntityType<CraneEntity> CRANE;
    public static EntityType<ForkliftEntity> FORKLIFT;
    public static EntityType<ForkliftDriverEntity> FORKLIFT_DRIVER;

    public static Item STEEL_SHEET;
    public static Item STEEL_PLATE;
    public static Item STEEL_BRACKET;
    public static Item STEEL_CHANNEL;
    public static Item WELDING_TORCH;
    public static Item GRAVITY_GUN;
    public static Item CRANE_PENDANT;
    public static Item FORKLIFT_ITEM;
    public static Item FORKLIFT_DRIVER_EGG;

    public static Block WELDER;
    public static Block PRESS_BRAKE;
    public static Block CONVEYOR;

    private FactoryRegistry() {}

    public static void register() {
        STEEL_PART = Registry.register(BuiltInRegistries.ENTITY_TYPE, Freerun.id("steel_part"),
                EntityType.Builder.<SteelPartEntity>of(SteelPartEntity::new, MobCategory.MISC)
                        .sized(1.0f, 0.0625f).clientTrackingRange(8).updateInterval(1).fireImmune()
                        .build("steel_part"));

        CRANE = Registry.register(BuiltInRegistries.ENTITY_TYPE, Freerun.id("overhead_crane"),
                EntityType.Builder.<CraneEntity>of(CraneEntity::new, MobCategory.MISC)
                        .sized(0.9f, 0.4f).clientTrackingRange(12).updateInterval(1).fireImmune()
                        .build("overhead_crane"));
        FORKLIFT = Registry.register(BuiltInRegistries.ENTITY_TYPE, Freerun.id("forklift"),
                EntityType.Builder.<ForkliftEntity>of(ForkliftEntity::new, MobCategory.MISC)
                        .sized(1.4f, 2.45f).passengerAttachments(new Vec3(0.0, 0.95, -0.4))
                        .clientTrackingRange(10).fireImmune().build("forklift"));
        FabricDefaultAttributeRegistry.register(FORKLIFT, ForkliftEntity.createAttributes());
        FORKLIFT_DRIVER = Registry.register(BuiltInRegistries.ENTITY_TYPE, Freerun.id("forklift_driver"),
                EntityType.Builder.of(ForkliftDriverEntity::new, MobCategory.CREATURE)
                        .sized(0.9f, 2.0f).ridingOffset(-0.65f).clientTrackingRange(10).build("forklift_driver"));
        FabricDefaultAttributeRegistry.register(FORKLIFT_DRIVER, ForkliftDriverEntity.createAttributes());

        STEEL_SHEET = item("steel_sheet", new PartItem(PartType.SHEET, new Item.Properties()));
        STEEL_PLATE = item("steel_plate", new PartItem(PartType.PLATE, new Item.Properties()));
        STEEL_BRACKET = item("steel_bracket", new PartItem(PartType.BRACKET, new Item.Properties()));
        STEEL_CHANNEL = item("steel_channel", new PartItem(PartType.CHANNEL, new Item.Properties()));
        WELDING_TORCH = item("welding_torch", new WeldingTorchItem(new Item.Properties().stacksTo(1)));
        GRAVITY_GUN = item("gravity_gun", new GravityGunItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
        CRANE_PENDANT = item("crane_pendant", new CranePendantItem(new Item.Properties().stacksTo(1)));
        FORKLIFT_ITEM = item("forklift", new ForkliftItem(new Item.Properties().stacksTo(1)));
        FORKLIFT_DRIVER_EGG = item("forklift_driver_spawn_egg",
                new SpawnEggItem(FORKLIFT_DRIVER, 0xa8201e, 0x3a5680, new Item.Properties()));

        WELDER = block("welder", new WelderBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                .noOcclusion().lightLevel(s -> 4)));
        PRESS_BRAKE = block("press_brake", new PressBrakeBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                .noOcclusion()));
        CONVEYOR = block("conveyor", new ConveyorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                .strength(2.0f).noOcclusion()));

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(e -> {
            e.accept(STEEL_SHEET);
            e.accept(STEEL_PLATE);
            e.accept(STEEL_BRACKET);
            e.accept(STEEL_CHANNEL);
        });
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(e -> {
            e.accept(WELDING_TORCH);
            e.accept(GRAVITY_GUN);
            e.accept(CRANE_PENDANT);
            e.accept(FORKLIFT_ITEM);
        });
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(e -> e.accept(FORKLIFT_DRIVER_EGG));
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(e -> {
            e.accept(WELDER);
            e.accept(PRESS_BRAKE);
            e.accept(CONVEYOR);
        });

        ServerTickEvents.END_SERVER_TICK.register(GravityGunManager::tick);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> GravityGunManager.releaseAll());
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            GravityGunManager.forget(handler.player);
            WeldingTorchItem.forget(handler.player.getUUID());
            CraneEntity.forget(handler.player.getUUID());
        });

        PayloadTypeRegistry.playC2S().register(ForkPayload.TYPE, ForkPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ForkPayload.TYPE, (payload, context) -> {
            if (context.player().getVehicle() instanceof ForkliftEntity forklift) {
                forklift.setForkInput(context.player(), payload.dir());
            }
        });
    }

    private static Item item(String name, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, Freerun.id(name), item);
    }

    private static Block block(String name, Block block) {
        Block registered = Registry.register(BuiltInRegistries.BLOCK, Freerun.id(name), block);
        Registry.register(BuiltInRegistries.ITEM, Freerun.id(name), new BlockItem(registered, new Item.Properties()));
        return registered;
    }
}
