package dev.freerun.food;

import dev.freerun.Freerun;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

import java.util.ArrayList;
import java.util.List;

/**
 * Food and break-room things: the sausage lineup and the stuffer that makes them, the coffee
 * crop and everything from bean to cup, the microwave, counters, and the Chud-Maxing effect.
 */
public final class FoodRegistry {
    public static Holder<MobEffect> CHUD_MAXING;

    // sausages
    public static Item BOUDIN, CHORIZO, BREAKFAST_LINKS, BEEF_FRANK, KIELBASA, BRATWURST,
            CHICKEN_APPLE_SAUSAGE, MERGUEZ, HUNTERS_SAUSAGE, SEAFOOD_SAUSAGE, CHUD_SAUSAGE;
    // coffee
    public static Item COFFEE_BEANS, COFFEE_CHERRIES, ROASTED_COFFEE_BEANS, COFFEE_GROUNDS, COFFEE_MUG, CUP_OF_COFFEE;

    public static Block SAUSAGE_STUFFER, COFFEE_PLANT, COFFEE_MAKER, MICROWAVE, COUNTER, COOLER, STORE_SHELF;
    public static BlockEntityType<SausageStufferBlockEntity> STUFFER_BE;
    public static BlockEntityType<MicrowaveBlockEntity> MICROWAVE_BE;
    public static BlockEntityType<CounterBlockEntity> COUNTER_BE;

    private static final List<Item> FOODS = new ArrayList<>();
    private static final List<Item> MISC = new ArrayList<>();
    private static final List<Item> BLOCK_ITEMS = new ArrayList<>();

    private FoodRegistry() {}

    public static void register() {
        CHUD_MAXING = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Freerun.id("chud_maxing"), new ChudMaxingEffect());

        BOUDIN = sausage("boudin", 8, 0.9f, effect(MobEffects.MOVEMENT_SPEED, 400, 0),
                "Cajun pork and rice. Fuel for a double shift.", null);
        CHORIZO = sausage("chorizo", 7, 0.8f, effect(MobEffects.FIRE_RESISTANCE, 600, 0),
                "Pork and beetroot, fiery red. So spicy you won't feel lava for a bit.", null);
        BREAKFAST_LINKS = sausage("breakfast_links", 6, 0.7f, effect(MobEffects.REGENERATION, 100, 0),
                "Little pork-and-berry links. Most important meal of the shift.", null);
        BEEF_FRANK = sausage("beef_frank", 7, 0.7f, null,
                "An all-beef frank. Ballpark classic.", null);
        KIELBASA = sausage("kielbasa", 9, 0.9f, effect(MobEffects.DAMAGE_RESISTANCE, 600, 0),
                "Beef and potato, smoked. Sticks to your ribs.", null);
        BRATWURST = sausage("bratwurst", 8, 0.8f, effect(MobEffects.ABSORPTION, 1200, 0),
                "Beef and wheat, grilled. Wunderbar.", null);
        CHICKEN_APPLE_SAUSAGE = sausage("chicken_apple_sausage", 7, 0.8f, effect(MobEffects.REGENERATION, 160, 0),
                "Chicken and apple. The healthy one.", null);
        MERGUEZ = sausage("merguez", 8, 0.9f, effect(MobEffects.DAMAGE_BOOST, 600, 0),
                "Spiced mutton and beetroot. Thin, red, strong.", null);
        HUNTERS_SAUSAGE = sausage("hunters_sausage", 7, 0.8f, effect(MobEffects.JUMP, 1200, 0),
                "Rabbit and carrot. Puts a spring in your step.", null);
        SEAFOOD_SAUSAGE = sausage("seafood_sausage", 6, 0.8f, effect(MobEffects.WATER_BREATHING, 1200, 0),
                "Fish and kelp. Somehow it works.", null);
        CHUD_SAUSAGE = sausage("chud_sausage", 10, 1.2f, new MobEffectInstance(CHUD_MAXING, 6000, 0),
                "Rotten flesh and Stone. Do not eat this. You will start chud-maxing.",
                "§6§lYou are now chud-maxing.");

        COFFEE_CHERRIES = misc("coffee_cherries", new Item(new Item.Properties()));
        ROASTED_COFFEE_BEANS = misc("roasted_coffee_beans", new Item(new Item.Properties()));
        COFFEE_GROUNDS = misc("coffee_grounds", new Item(new Item.Properties()));
        COFFEE_MUG = misc("coffee_mug", new Item(new Item.Properties().stacksTo(16)));
        CUP_OF_COFFEE = Registry.register(BuiltInRegistries.ITEM, Freerun.id("cup_of_coffee"), new CoffeeItem(
                new Item.Properties().stacksTo(16).food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3f)
                        .alwaysEdible().usingConvertsTo(COFFEE_MUG)
                        .effect(effect(MobEffects.DIG_SPEED, 2400, 0), 1.0f)
                        .effect(effect(MobEffects.MOVEMENT_SPEED, 600, 0), 1.0f).build())));
        FOODS.add(CUP_OF_COFFEE);

        SAUSAGE_STUFFER = block("sausage_stuffer", new SausageStufferBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                .strength(2.5f).noOcclusion()));
        COFFEE_MAKER = block("coffee_maker", new CoffeeMakerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                .strength(1.5f).noOcclusion()));
        MICROWAVE = block("microwave", new MicrowaveBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                .strength(1.5f).noOcclusion().lightLevel(s -> s.getValue(MicrowaveBlock.RUNNING) ? 7 : 0)));
        COUNTER = block("counter", new CounterBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(2.0f)));
        COOLER = block("drink_cooler", new CounterBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                .strength(2.0f).noOcclusion().lightLevel(st -> 10)));
        STORE_SHELF = block("store_shelf", new CounterBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)
                .strength(2.0f).noOcclusion()));

        COFFEE_PLANT = Registry.register(BuiltInRegistries.BLOCK, Freerun.id("coffee_plant"), new CoffeePlantBlock(
                BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT).pushReaction(PushReaction.DESTROY)));
        COFFEE_BEANS = misc("coffee_beans", new ItemNameBlockItem(COFFEE_PLANT, new Item.Properties()));

        STUFFER_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Freerun.id("sausage_stuffer"),
                BlockEntityType.Builder.of(SausageStufferBlockEntity::new, SAUSAGE_STUFFER).build(null));
        MICROWAVE_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Freerun.id("microwave"),
                BlockEntityType.Builder.of(MicrowaveBlockEntity::new, MICROWAVE).build(null));
        COUNTER_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Freerun.id("counter"),
                BlockEntityType.Builder.of(CounterBlockEntity::new, COUNTER, COOLER, STORE_SHELF).build(null));

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(e -> FOODS.forEach(e::accept));
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(e -> MISC.forEach(e::accept));
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(e -> BLOCK_ITEMS.forEach(e::accept));
    }

    private static MobEffectInstance effect(Holder<MobEffect> effect, int ticks, int amp) {
        return new MobEffectInstance(effect, ticks, amp);
    }

    private static Item sausage(String name, int nutrition, float saturation, MobEffectInstance effect,
                                String flavour, String eatMessage) {
        FoodProperties.Builder food = new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation);
        if (effect != null) food.effect(effect, 1.0f);
        if (eatMessage != null) food.alwaysEdible();
        Item item = Registry.register(BuiltInRegistries.ITEM, Freerun.id(name),
                new SausageItem(new Item.Properties().food(food.build()), flavour, eatMessage));
        FOODS.add(item);
        return item;
    }

    private static Item misc(String name, Item item) {
        Item registered = Registry.register(BuiltInRegistries.ITEM, Freerun.id(name), item);
        MISC.add(registered);
        return registered;
    }

    private static Block block(String name, Block block) {
        Block registered = Registry.register(BuiltInRegistries.BLOCK, Freerun.id(name), block);
        BLOCK_ITEMS.add(Registry.register(BuiltInRegistries.ITEM, Freerun.id(name), new BlockItem(registered, new Item.Properties())));
        return registered;
    }
}
