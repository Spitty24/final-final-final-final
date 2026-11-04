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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Everything for the welding factory: steel parts, the machines, the torch and the gravity gun. */
public final class FactoryRegistry {
    public static EntityType<SteelPartEntity> STEEL_PART;

    public static Item STEEL_SHEET;
    public static Item STEEL_PLATE;
    public static Item STEEL_BRACKET;
    public static Item STEEL_CHANNEL;
    public static Item WELDING_TORCH;
    public static Item GRAVITY_GUN;

    public static Block WELDER;
    public static Block PRESS_BRAKE;

    private FactoryRegistry() {}

    public static void register() {
        STEEL_PART = Registry.register(BuiltInRegistries.ENTITY_TYPE, Freerun.id("steel_part"),
                EntityType.Builder.<SteelPartEntity>of(SteelPartEntity::new, MobCategory.MISC)
                        .sized(1.0f, 0.0625f).clientTrackingRange(8).updateInterval(1).fireImmune()
                        .build("steel_part"));

        STEEL_SHEET = item("steel_sheet", new PartItem(PartType.SHEET, new Item.Properties()));
        STEEL_PLATE = item("steel_plate", new PartItem(PartType.PLATE, new Item.Properties()));
        STEEL_BRACKET = item("steel_bracket", new PartItem(PartType.BRACKET, new Item.Properties()));
        STEEL_CHANNEL = item("steel_channel", new PartItem(PartType.CHANNEL, new Item.Properties()));
        WELDING_TORCH = item("welding_torch", new WeldingTorchItem(new Item.Properties().stacksTo(1)));
        GRAVITY_GUN = item("gravity_gun", new GravityGunItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

        WELDER = block("welder", new WelderBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                .noOcclusion().lightLevel(s -> 4)));
        PRESS_BRAKE = block("press_brake", new PressBrakeBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                .noOcclusion()));

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(e -> {
            e.accept(STEEL_SHEET);
            e.accept(STEEL_PLATE);
            e.accept(STEEL_BRACKET);
            e.accept(STEEL_CHANNEL);
        });
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(e -> {
            e.accept(WELDING_TORCH);
            e.accept(GRAVITY_GUN);
        });
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(e -> {
            e.accept(WELDER);
            e.accept(PRESS_BRAKE);
        });

        ServerTickEvents.END_SERVER_TICK.register(GravityGunManager::tick);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> GravityGunManager.releaseAll());
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            GravityGunManager.forget(handler.player);
            WeldingTorchItem.forget(handler.player.getUUID());
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
