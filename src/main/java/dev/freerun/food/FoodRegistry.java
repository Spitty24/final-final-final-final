package dev.freerun.food;

import dev.freerun.Freerun;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Boudin and the sausage stuffer that makes it. */
public final class FoodRegistry {
    public static Item BOUDIN;
    public static Block SAUSAGE_STUFFER;

    private FoodRegistry() {}

    public static void register() {
        // Filling, and a little burst of speed: fuel for a double shift.
        BOUDIN = Registry.register(BuiltInRegistries.ITEM, Freerun.id("boudin"), new Item(new Item.Properties()
                .food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.9f)
                        .effect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 0), 1.0f).build())));
        SAUSAGE_STUFFER = Registry.register(BuiltInRegistries.BLOCK, Freerun.id("sausage_stuffer"),
                new SausageStufferBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.5f).noOcclusion()));
        Item stufferItem = Registry.register(BuiltInRegistries.ITEM, Freerun.id("sausage_stuffer"),
                new BlockItem(SAUSAGE_STUFFER, new Item.Properties()));

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(e -> e.accept(BOUDIN));
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(e -> e.accept(stufferItem));
    }
}
