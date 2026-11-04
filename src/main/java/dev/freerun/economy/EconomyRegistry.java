package dev.freerun.economy;

import dev.freerun.Freerun;
import dev.freerun.food.FoodRegistry;
import dev.freerun.item.ItemRegistry;
import dev.freerun.net.ShopPayloads;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/** Money, store food and drink, the modern conveniences, and the store fixtures. */
public final class EconomyRegistry {
    public static final int[] DENOMINATIONS_ASC = {1, 5, 20, 100};

    public static Item ONE_DOLLAR, FIVE_DOLLARS, TWENTY_DOLLARS, HUNDRED_DOLLARS, WALLET;
    // snacks
    public static Item POTATO_CHIPS, CHEESE_PUFFS, CANDY_BAR, GUMMY_WORMS, BEEF_JERKY, SUNFLOWER_SEEDS, BOILED_PEANUTS,
            PORK_CRACKLINS, HONEY_BUN, GLAZED_DONUT, FROZEN_BURRITO, HOT_BURRITO, INSTANT_RAMEN, ROLLER_HOT_DOG, CORN_DOG,
            PIZZA_SLICE, CHICKEN_TENDERS, NACHOS, SAUSAGE_BISCUIT, ICE_CREAM_SANDWICH, BIG_PICKLE;
    // drinks
    public static Item FIZZ_COLA, LEMON_LIME_SODA, ROOT_BEER, ENERGY_DRINK, SPORTS_DRINK, BOTTLED_WATER, SWEET_TEA,
            CHOCOLATE_MILK, BLUE_SLUSHIE, CHERRY_SLUSHIE, ICED_COFFEE, EMPTY_CAN;
    // amenities
    public static Item SMARTPHONE, FLASHLIGHT, SCRATCH_OFF, LIGHTER, BANDAGE, PAINKILLERS, SUNGLASSES, UMBRELLA, AIR_HORN,
            DUCT_TAPE, GAS_CAN, EMPTY_GAS_CAN;
    // fixtures
    public static Block ATM, VENDING_MACHINE, SLUSHIE_MACHINE, ROLLER_GRILL, GAS_PUMP, TV;

    /** What the vending machine can drop. */
    public static final List<Item> VENDING = new ArrayList<>();
    /** Snacks the workers buy on break. */
    public static final List<Item> BREAK_SNACKS = new ArrayList<>();

    private static final List<Item> TAB = new ArrayList<>();

    private EconomyRegistry() {}

    public static Item bill(int value) {
        return switch (value) {
            case 100 -> HUNDRED_DOLLARS;
            case 20 -> TWENTY_DOLLARS;
            case 5 -> FIVE_DOLLARS;
            default -> ONE_DOLLAR;
        };
    }

    public static void register() {
        ModSounds.register();
        BreakTime.register();

        ONE_DOLLAR = item("one_dollar_bill", new CashItem(1, new Item.Properties()));
        FIVE_DOLLARS = item("five_dollar_bill", new CashItem(5, new Item.Properties()));
        TWENTY_DOLLARS = item("twenty_dollar_bill", new CashItem(20, new Item.Properties()));
        HUNDRED_DOLLARS = item("hundred_dollar_bill", new CashItem(100, new Item.Properties().rarity(Rarity.UNCOMMON)));
        WALLET = item("wallet", new WalletItem(new Item.Properties().stacksTo(1)));
        EMPTY_CAN = item("empty_can", new Item(new Item.Properties()));

        // ---- snacks (name, hunger, saturation, effects, flavour)
        POTATO_CHIPS = snack("potato_chips", 3, 0.3f, false, 24, "Salt & vinegar. The bag is 80% air.", null, null);
        CHEESE_PUFFS = snack("cheese_puffs", 3, 0.2f, false, 24, "Your fingers will be orange until Thursday.", null,
                (l, e) -> msg(e, "§6Your fingers are now permanently orange."));
        CANDY_BAR = snack("candy_bar", 4, 0.3f, false, 24, "Peanuts, nougat, caramel. A meal, technically.", null, null,
                eff(MobEffects.MOVEMENT_SPEED, 300, 0));
        GUMMY_WORMS = snack("gummy_worms", 2, 0.1f, false, 20, "Sour. Bouncy.", null, null, eff(MobEffects.JUMP, 400, 1));
        BEEF_JERKY = snack("beef_jerky", 6, 0.8f, false, 40, "Teriyaki. Takes forever to chew.", null, null);
        SUNFLOWER_SEEDS = snack("sunflower_seeds", 2, 0.2f, false, 30, "Spit the shells. Not on the floor.", null,
                (l, e) -> msg(e, "§7*ptoo*"));
        BOILED_PEANUTS = snack("boiled_peanuts", 4, 0.5f, false, 32, "Cajun spiced, from the crock pot by the register.", null,
                null, eff(MobEffects.FIRE_RESISTANCE, 300, 0));
        PORK_CRACKLINS = snack("pork_cracklins", 5, 0.5f, false, 32, "Fried pork skin with the fat still on. Crunchy.", null, null);
        HONEY_BUN = snack("honey_bun", 5, 0.4f, false, 28, "Glazed. Sticky. Breakfast.", null, null, eff(MobEffects.ABSORPTION, 600, 0));
        GLAZED_DONUT = snack("glazed_donut", 4, 0.3f, false, 24, "From this morning. Probably.", null, null,
                eff(MobEffects.MOVEMENT_SPEED, 200, 0));
        FROZEN_BURRITO = snack("frozen_burrito", 1, 0.1f, false, 40, "Still frozen solid. Microwave it first.", null,
                (l, e) -> {
                    msg(e, "§bIt's still frozen in the middle. Why would you do that.");
                    e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
                });
        HOT_BURRITO = snack("hot_burrito", 7, 0.7f, false, 32, "Lava on the outside, ice in the middle.", null, null);
        INSTANT_RAMEN = snack("instant_ramen", 6, 0.5f, true, 32, "Just add hot water. They have it by the coffee.", null, null);
        ROLLER_HOT_DOG = snack("roller_hot_dog", 6, 0.6f, false, 28, "It's been rolling since 6 AM. That's flavour.", null, null);
        CORN_DOG = snack("corn_dog", 6, 0.6f, false, 28, "Mustard's free.", null, null);
        PIZZA_SLICE = snack("pizza_slice", 7, 0.5f, false, 32, "Gas station pizza. A gamble every time.", null,
                (l, e) -> {
                    if (l.random.nextInt(5) == 0) {
                        msg(e, "§2...that one was a mistake.");
                        e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0));
                    }
                });
        CHICKEN_TENDERS = snack("chicken_tenders", 8, 0.8f, false, 36, "Hot case. Comes with a little cup of sauce.", null, null);
        NACHOS = snack("nachos", 6, 0.5f, false, 32, "Cheese from the pump. Jalapeños from a jar.", null, null,
                eff(MobEffects.DAMAGE_BOOST, 300, 0));
        SAUSAGE_BISCUIT = snack("sausage_biscuit", 7, 0.7f, false, 32, "Buttermilk biscuit, sausage patty, wrapped in foil.", null, null);
        ICE_CREAM_SANDWICH = snack("ice_cream_sandwich", 4, 0.3f, false, 24, "Melting already.", null, null,
                eff(MobEffects.FIRE_RESISTANCE, 600, 0));
        BIG_PICKLE = snack("big_pickle", 3, 0.2f, false, 28, "A pickle the size of your forearm, in a pouch of brine.", null, null,
                eff(MobEffects.REGENERATION, 100, 0));

        // ---- drinks
        Supplier<Item> can = () -> EMPTY_CAN;
        FIZZ_COLA = snack("fizz_cola", 2, 0.1f, true, 32, "Ice cold. Mostly sugar.", can, null, eff(MobEffects.MOVEMENT_SPEED, 400, 0));
        LEMON_LIME_SODA = snack("lemon_lime_soda", 2, 0.1f, true, 32, "Clear, so it's healthy.", can, null, eff(MobEffects.JUMP, 400, 0));
        ROOT_BEER = snack("root_beer", 2, 0.1f, true, 32, "Brewed in Louisiana since forever.", can, null, eff(MobEffects.REGENERATION, 80, 0));
        ENERGY_DRINK = snack("energy_drink", 1, 0.1f, true, 28, "VOLTAGE. 300 mg of caffeine. Not a beverage, a decision.", can,
                (l, e) -> {
                    if (l.random.nextInt(6) == 0) {
                        msg(e, "§eYou can hear your own heartbeat.");
                        e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
                    }
                }, eff(MobEffects.MOVEMENT_SPEED, 900, 1), eff(MobEffects.DIG_SPEED, 900, 1));
        SPORTS_DRINK = snack("sports_drink", 2, 0.3f, true, 32, "Electrolytes. It's what plants crave.", null, null,
                eff(MobEffects.REGENERATION, 120, 0), eff(MobEffects.DAMAGE_RESISTANCE, 300, 0));
        BOTTLED_WATER = snack("bottled_water", 1, 0.1f, true, 24, "Clears up most of what ails you.", null,
                (l, e) -> clearHarmful(e));
        SWEET_TEA = snack("sweet_tea", 3, 0.2f, true, 32, "A gallon of sugar with some tea in it. Perfect.", null, null,
                eff(MobEffects.MOVEMENT_SPEED, 300, 0), eff(MobEffects.REGENERATION, 60, 0));
        CHOCOLATE_MILK = snack("chocolate_milk", 3, 0.3f, true, 32, "Works like milk: clears every effect.", null,
                (l, e) -> e.removeAllEffects());
        BLUE_SLUSHIE = snack("blue_raspberry_slushie", 2, 0.1f, true, 40, "Blue isn't a flavour, but here we are.", null,
                (l, e) -> brainFreeze(e), eff(MobEffects.FIRE_RESISTANCE, 600, 0));
        CHERRY_SLUSHIE = snack("cherry_slushie", 2, 0.1f, true, 40, "Turns your whole mouth red.", null,
                (l, e) -> brainFreeze(e), eff(MobEffects.FIRE_RESISTANCE, 600, 0));
        ICED_COFFEE = snack("iced_coffee", 2, 0.2f, true, 32, "In a can. Shake well.", can, null, eff(MobEffects.DIG_SPEED, 1200, 0));

        // ---- amenities
        SMARTPHONE = item("smartphone", new Amenities.Smartphone(new Item.Properties().stacksTo(1)));
        FLASHLIGHT = item("flashlight", new Amenities.Flashlight(new Item.Properties().stacksTo(1)));
        SCRATCH_OFF = item("scratch_off", new Amenities.ScratchOff(new Item.Properties()));
        LIGHTER = item("lighter", new FlintAndSteelItem(new Item.Properties().durability(64)));
        BANDAGE = item("bandage", new Amenities.Bandage(new Item.Properties().stacksTo(16)));
        PAINKILLERS = item("painkillers", new Amenities.Painkiller(new Item.Properties().stacksTo(16)));
        SUNGLASSES = item("sunglasses", new Amenities.Sunglasses(new Item.Properties().stacksTo(1)));
        UMBRELLA = item("umbrella", new Amenities.Umbrella(new Item.Properties().stacksTo(1)));
        AIR_HORN = item("air_horn", new Amenities.AirHorn(new Item.Properties().durability(40)));
        DUCT_TAPE = item("duct_tape", new Amenities.DuctTape(new Item.Properties().durability(8)));
        GAS_CAN = item("gas_can", new Amenities.GasCan(new Item.Properties().stacksTo(1)));
        EMPTY_GAS_CAN = item("empty_gas_can", new Item(new Item.Properties().stacksTo(1)));

        // ---- fixtures
        ATM = block("atm", new StoreBlocks.Atm(props(Blocks.IRON_BLOCK).lightLevel(s -> 6)));
        VENDING_MACHINE = block("vending_machine", new StoreBlocks.VendingMachine(props(Blocks.IRON_BLOCK).lightLevel(s -> 8)));
        SLUSHIE_MACHINE = block("slushie_machine", new StoreBlocks.SlushieMachine(props(Blocks.IRON_BLOCK).lightLevel(s -> 5)));
        ROLLER_GRILL = block("roller_grill", new StoreBlocks.RollerGrill(props(Blocks.IRON_BLOCK)));
        GAS_PUMP = block("gas_pump", new StoreBlocks.GasPump(props(Blocks.IRON_BLOCK).lightLevel(s -> 4)));
        TV = block("tv", new StoreBlocks.Tv(props(Blocks.IRON_BLOCK).strength(1.0f)
                .lightLevel(s -> s.getValue(StoreBlocks.Tv.ON) ? 9 : 0)));

        VENDING.addAll(List.of(POTATO_CHIPS, CHEESE_PUFFS, CANDY_BAR, GUMMY_WORMS, HONEY_BUN, FIZZ_COLA, LEMON_LIME_SODA,
                ROOT_BEER, ENERGY_DRINK, BOTTLED_WATER, ICED_COFFEE));
        BREAK_SNACKS.addAll(List.of(ROLLER_HOT_DOG, CORN_DOG, POTATO_CHIPS, CANDY_BAR, FIZZ_COLA, SWEET_TEA, ENERGY_DRINK,
                PIZZA_SLICE, CHICKEN_TENDERS, BOILED_PEANUTS, HONEY_BUN, SAUSAGE_BISCUIT, NACHOS, BLUE_SLUSHIE));

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Freerun.id("double_quick"), FabricItemGroup.builder()
                .title(Component.literal("Double Quick & General Store"))
                .icon(() -> new ItemStack(ROLLER_HOT_DOG))
                .displayItems((params, output) -> {
                    for (Item i : TAB) output.accept(i);
                    if (dev.freerun.vehicle.VehicleRegistry.CAR_ITEM != null) output.accept(dev.freerun.vehicle.VehicleRegistry.CAR_ITEM);
                })
                .build());

        PayloadTypeRegistry.playS2C().register(ShopPayloads.Open.TYPE, ShopPayloads.Open.CODEC);
        PayloadTypeRegistry.playC2S().register(ShopPayloads.Trade.TYPE, ShopPayloads.Trade.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ShopPayloads.Trade.TYPE, (payload, ctx) -> ctx.player().server.execute(() -> {
            Entity clerk = ctx.player().level().getEntity(payload.entityId());
            if (!(clerk instanceof dev.freerun.mobs.RoadhouseNpcEntity npc) || npc.shop() == null) return;
            if (payload.sell()) Shops.sell(ctx.player(), clerk, npc.shop(), payload.index(), payload.times());
            else Shops.buy(ctx.player(), clerk, npc.shop(), payload.index(), payload.times());
        }));
    }

    /** Called last, once every item that appears on a shelf exists. */
    public static void buildShops() {
        List<Shops.Offer> dq = new ArrayList<>();
        for (Object[] o : new Object[][]{
                {ROLLER_HOT_DOG, 1, 2}, {CORN_DOG, 1, 3}, {PIZZA_SLICE, 1, 3}, {CHICKEN_TENDERS, 1, 6}, {NACHOS, 1, 4},
                {SAUSAGE_BISCUIT, 1, 3}, {FoodRegistry.BOUDIN, 1, 5}, {BOILED_PEANUTS, 1, 3}, {PORK_CRACKLINS, 1, 3},
                {POTATO_CHIPS, 1, 2}, {CHEESE_PUFFS, 1, 2}, {CANDY_BAR, 1, 2}, {GUMMY_WORMS, 1, 2}, {BEEF_JERKY, 1, 5},
                {SUNFLOWER_SEEDS, 1, 1}, {HONEY_BUN, 1, 2}, {GLAZED_DONUT, 1, 1}, {FROZEN_BURRITO, 1, 3}, {INSTANT_RAMEN, 1, 1},
                {ICE_CREAM_SANDWICH, 1, 2}, {BIG_PICKLE, 1, 2},
                {FIZZ_COLA, 1, 2}, {LEMON_LIME_SODA, 1, 2}, {ROOT_BEER, 1, 2}, {ENERGY_DRINK, 1, 4}, {SPORTS_DRINK, 1, 3},
                {BOTTLED_WATER, 1, 1}, {SWEET_TEA, 1, 2}, {CHOCOLATE_MILK, 1, 2}, {BLUE_SLUSHIE, 1, 2}, {CHERRY_SLUSHIE, 1, 2},
                {ICED_COFFEE, 1, 3}, {FoodRegistry.CUP_OF_COFFEE, 1, 2},
                {SCRATCH_OFF, 1, 2}, {LIGHTER, 1, 1}, {BANDAGE, 1, 4}, {PAINKILLERS, 1, 5}, {SUNGLASSES, 1, 15},
                {GAS_CAN, 1, 14}, {EMPTY_GAS_CAN, 1, 8}, {WALLET, 1, 10}, {AIR_HORN, 1, 6}}) {
            dq.add(Shops.of((Item) o[0], (Integer) o[1], (Integer) o[2]));
        }
        List<Shops.Offer> dqSell = List.of(Shops.of(EMPTY_CAN, 4, 1), Shops.of(FoodRegistry.BOUDIN, 1, 3),
                Shops.of(FoodRegistry.COFFEE_GROUNDS, 2, 1), Shops.of(FoodRegistry.COFFEE_CHERRIES, 4, 1));

        List<Shops.Offer> gs = new ArrayList<>();
        for (Object[] o : new Object[][]{
                {SMARTPHONE, 1, 150}, {FLASHLIGHT, 1, 12}, {UMBRELLA, 1, 10}, {DUCT_TAPE, 1, 3}, {AIR_HORN, 1, 6},
                {SUNGLASSES, 1, 15}, {WALLET, 1, 10}, {LIGHTER, 1, 1}, {BANDAGE, 1, 4}, {PAINKILLERS, 1, 5},
                {EMPTY_GAS_CAN, 1, 8}, {dev.freerun.vehicle.VehicleRegistry.CAR_ITEM, 1, 400},
                {ItemRegistry.BULLET, 16, 10}, {ItemRegistry.SHOTGUN_SHELL, 8, 10},
                {FoodRegistry.COFFEE_BEANS, 4, 3}, {FoodRegistry.COFFEE_MUG, 2, 2}, {Items.WHEAT_SEEDS, 8, 1},
                {Items.CARROT, 4, 2}, {Items.POTATO, 4, 2}, {Items.BREAD, 2, 1}, {Items.TORCH, 16, 2},
                {Items.OAK_LOG, 16, 8}, {Items.IRON_INGOT, 1, 4}, {Items.LANTERN, 1, 3}, {Items.BUCKET, 1, 3},
                {Items.FISHING_ROD, 1, 6}, {Items.SHEARS, 1, 4}, {Items.IRON_PICKAXE, 1, 18}, {Items.IRON_AXE, 1, 15},
                {Items.SADDLE, 1, 20}, {Items.NAME_TAG, 1, 8},
                {FoodRegistry.COFFEE_MAKER.asItem(), 1, 40}, {FoodRegistry.MICROWAVE.asItem(), 1, 60},
                {FoodRegistry.COUNTER.asItem(), 1, 20}, {TV.asItem(), 1, 120}, {VENDING_MACHINE.asItem(), 1, 250},
                {ATM.asItem(), 1, 300}, {GAS_PUMP.asItem(), 1, 200}}) {
            if (o[0] != null) gs.add(Shops.of((Item) o[0], (Integer) o[1], (Integer) o[2]));
        }
        List<Shops.Offer> gsSell = new ArrayList<>(List.of(
                Shops.of(dev.freerun.factory.FactoryRegistry.STEEL_SHEET, 1, 3),
                Shops.of(dev.freerun.factory.FactoryRegistry.STEEL_PLATE, 1, 2),
                Shops.of(dev.freerun.factory.FactoryRegistry.STEEL_BRACKET, 1, 4),
                Shops.of(dev.freerun.factory.FactoryRegistry.STEEL_CHANNEL, 1, 5),
                Shops.of(FoodRegistry.BOUDIN, 1, 3), Shops.of(FoodRegistry.CHUD_SAUSAGE, 1, 1),
                Shops.of(Items.WHEAT, 8, 2), Shops.of(Items.CARROT, 8, 2), Shops.of(Items.POTATO, 8, 2),
                Shops.of(Items.COAL, 4, 1), Shops.of(Items.IRON_INGOT, 1, 2), Shops.of(Items.COPPER_INGOT, 4, 1),
                Shops.of(Items.GOLD_INGOT, 1, 8), Shops.of(Items.EMERALD, 1, 10), Shops.of(Items.DIAMOND, 1, 40),
                Shops.of(Items.LEATHER, 2, 1), Shops.of(Items.COD, 2, 1), Shops.of(Items.SALMON, 2, 2),
                Shops.of(EMPTY_CAN, 4, 1)));
        for (Item s : new Item[]{FoodRegistry.CHORIZO, FoodRegistry.KIELBASA, FoodRegistry.BRATWURST, FoodRegistry.MERGUEZ,
                FoodRegistry.BREAKFAST_LINKS, FoodRegistry.BEEF_FRANK, FoodRegistry.CHICKEN_APPLE_SAUSAGE,
                FoodRegistry.HUNTERS_SAUSAGE, FoodRegistry.SEAFOOD_SAUSAGE}) {
            gsSell.add(Shops.of(s, 1, 3));
        }
        Shops.build(dq, dqSell, gs, gsSell);
    }

    // ------------------------------------------------------------------ helpers

    private static MobEffectInstance eff(Holder<MobEffect> e, int ticks, int amp) {
        return new MobEffectInstance(e, ticks, amp);
    }

    private static void msg(LivingEntity e, String text) {
        if (e instanceof Player p) p.displayClientMessage(Component.literal(text), true);
    }

    private static void clearHarmful(LivingEntity e) {
        for (MobEffectInstance i : List.copyOf(e.getActiveEffects())) {
            if (i.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) e.removeEffect(i.getEffect());
        }
    }

    private static void brainFreeze(LivingEntity e) {
        if (e.level().random.nextInt(3) == 0) {
            msg(e, "§b§lBRAIN FREEZE");
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
        }
    }

    private static Item snack(String name, int hunger, float sat, boolean drink, int useTicks, String flavour,
                              Supplier<Item> leftover, BiConsumer<Level, LivingEntity> onFinish, MobEffectInstance... effects) {
        FoodProperties.Builder food = new FoodProperties.Builder().nutrition(hunger).saturationModifier(sat);
        if (drink) food.alwaysEdible();
        for (MobEffectInstance e : effects) food.effect(e, 1.0f);
        return item(name, new SnackItem(new Item.Properties().food(food.build()), drink, useTicks, flavour, leftover, onFinish));
    }

    private static BlockBehaviour.Properties props(Block copy) {
        return BlockBehaviour.Properties.ofFullCopy(copy).strength(2.0f).noOcclusion();
    }

    private static Item item(String name, Item item) {
        Item reg = Registry.register(BuiltInRegistries.ITEM, Freerun.id(name), item);
        TAB.add(reg);
        return reg;
    }

    private static Block block(String name, Block block) {
        Block reg = Registry.register(BuiltInRegistries.BLOCK, Freerun.id(name), block);
        item(name, new BlockItem(reg, new Item.Properties()));
        return reg;
    }
}
