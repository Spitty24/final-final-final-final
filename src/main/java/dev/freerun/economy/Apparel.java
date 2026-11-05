package dev.freerun.economy;

import dev.freerun.Freerun;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Clothes and shoes you can buy at the strip mall and wear: parody off-brands (no real logos).
 * Every piece is light armour; the sneakers each come with a small parkour perk.
 */
public final class Apparel {
    private Apparel() {}

    /** A brand: its id (also the armour texture name) and how it's written. */
    public record Brand(String id, String name) {}

    public static final Brand MIKE = new Brand("mike", "Mike"), ABIBAS = new Brand("abibas", "Abibas"),
            VANZ = new Brand("vanz", "Vanz"), CONVERSY = new Brand("conversy", "Conversy"), PUMBA = new Brand("pumba", "Pumba"),
            SUPREMO = new Brand("supremo", "Supremo"), OLD_NAVVY = new Brand("old_navvy", "Old Navvy"),
            UNDER_AMOUR = new Brand("under_amour", "Under Amour"), NORTH_FASE = new Brand("north_fase", "The North Fase");

    public static Item MIKE_AIR_RUNNERS, ABIBAS_CLASSICS, VANZ_SKATE_LOWS, CONVERSY_HIGH_TOPS, PUMBA_SPRINTERS;
    public static Item SUPREMO_CAP, SUPREMO_HOODIE, SUPREMO_JOGGERS;
    public static Item OLD_NAVVY_BUCKET_HAT, OLD_NAVVY_TEE, OLD_NAVVY_JEANS;
    public static Item UNDER_AMOUR_BEANIE, UNDER_AMOUR_SHIRT, UNDER_AMOUR_SHORTS;
    public static Item NORTH_FASE_BEANIE, NORTH_FASE_PUFFER, NORTH_FASE_CARGOS;

    /** Catalogues for the shoe stores and the clothing stores: {item, price}. */
    public static final List<Object[]> SHOE_PRICES = new ArrayList<>(), CLOTHES_PRICES = new ArrayList<>();
    private static final List<Item> TAB = new ArrayList<>();
    private static final Map<String, Holder<ArmorMaterial>> MATERIALS = new java.util.HashMap<>();

    public static void register() {
        MIKE_AIR_RUNNERS = shoe(MIKE, "mike_air_runners", 110, "+7% running speed",
                Attributes.MOVEMENT_SPEED, 0.07, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        ABIBAS_CLASSICS = shoe(ABIBAS, "abibas_classics", 95, "Jump a little higher",
                Attributes.JUMP_STRENGTH, 0.07, AttributeModifier.Operation.ADD_VALUE);
        VANZ_SKATE_LOWS = shoe(VANZ, "vanz_skate_lows", 65, "Fall 3 blocks further without getting hurt",
                Attributes.SAFE_FALL_DISTANCE, 3.0, AttributeModifier.Operation.ADD_VALUE);
        CONVERSY_HIGH_TOPS = shoe(CONVERSY, "conversy_high_tops", 60, "Step straight up curbs and blocks",
                Attributes.STEP_HEIGHT, 0.5, AttributeModifier.Operation.ADD_VALUE);
        PUMBA_SPRINTERS = shoe(PUMBA, "pumba_sprinters", 85, "+4% speed, softer landings",
                Attributes.MOVEMENT_SPEED, 0.04, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

        SUPREMO_CAP = clothes(SUPREMO, "supremo_cap", ArmorItem.Type.HELMET, 45);
        SUPREMO_HOODIE = clothes(SUPREMO, "supremo_hoodie", ArmorItem.Type.CHESTPLATE, 120);
        SUPREMO_JOGGERS = clothes(SUPREMO, "supremo_joggers", ArmorItem.Type.LEGGINGS, 90);
        OLD_NAVVY_BUCKET_HAT = clothes(OLD_NAVVY, "old_navvy_bucket_hat", ArmorItem.Type.HELMET, 12);
        OLD_NAVVY_TEE = clothes(OLD_NAVVY, "old_navvy_tee", ArmorItem.Type.CHESTPLATE, 10);
        OLD_NAVVY_JEANS = clothes(OLD_NAVVY, "old_navvy_jeans", ArmorItem.Type.LEGGINGS, 25);
        UNDER_AMOUR_BEANIE = clothes(UNDER_AMOUR, "under_amour_beanie", ArmorItem.Type.HELMET, 20);
        UNDER_AMOUR_SHIRT = clothes(UNDER_AMOUR, "under_amour_shirt", ArmorItem.Type.CHESTPLATE, 40);
        UNDER_AMOUR_SHORTS = clothes(UNDER_AMOUR, "under_amour_shorts", ArmorItem.Type.LEGGINGS, 30);
        NORTH_FASE_BEANIE = clothes(NORTH_FASE, "north_fase_beanie", ArmorItem.Type.HELMET, 25);
        NORTH_FASE_PUFFER = clothes(NORTH_FASE, "north_fase_puffer", ArmorItem.Type.CHESTPLATE, 150);
        NORTH_FASE_CARGOS = clothes(NORTH_FASE, "north_fase_cargos", ArmorItem.Type.LEGGINGS, 55);

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Freerun.id("apparel"), FabricItemGroup.builder()
                .title(Component.literal("Strip Mall Fashion"))
                .icon(() -> new ItemStack(MIKE_AIR_RUNNERS))
                .displayItems((params, output) -> {
                    for (Item i : TAB) output.accept(i);
                })
                .build());
    }

    /** A full outfit of one clothing brand plus a pair of shoes, for shop mannequins. */
    public static Item[] outfit(int n) {
        Item[][] fits = {
                {SUPREMO_CAP, SUPREMO_HOODIE, SUPREMO_JOGGERS, MIKE_AIR_RUNNERS},
                {OLD_NAVVY_BUCKET_HAT, OLD_NAVVY_TEE, OLD_NAVVY_JEANS, CONVERSY_HIGH_TOPS},
                {UNDER_AMOUR_BEANIE, UNDER_AMOUR_SHIRT, UNDER_AMOUR_SHORTS, PUMBA_SPRINTERS},
                {NORTH_FASE_BEANIE, NORTH_FASE_PUFFER, NORTH_FASE_CARGOS, ABIBAS_CLASSICS},
                {null, OLD_NAVVY_TEE, SUPREMO_JOGGERS, VANZ_SKATE_LOWS}};
        return fits[Math.floorMod(n, fits.length)];
    }

    // ------------------------------------------------------------------ registration

    private static Holder<ArmorMaterial> material(Brand b) {
        return MATERIALS.computeIfAbsent(b.id(), k -> {
            Map<ArmorItem.Type, Integer> def = new EnumMap<>(ArmorItem.Type.class);
            for (ArmorItem.Type t : ArmorItem.Type.values()) def.put(t, t == ArmorItem.Type.CHESTPLATE ? 2 : 1);
            return Registry.registerForHolder(BuiltInRegistries.ARMOR_MATERIAL, Freerun.id(k),
                    new ArmorMaterial(def, 12, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(Items.LEATHER),
                            List.of(new ArmorMaterial.Layer(Freerun.id(k))), 0.0f, 0.0f));
        });
    }

    private static Item shoe(Brand b, String id, int price, String perk, Holder<Attribute> attr, double amount,
                             AttributeModifier.Operation op) {
        ItemAttributeModifiers.Builder mods = ItemAttributeModifiers.builder()
                .add(Attributes.ARMOR, new AttributeModifier(Freerun.id("apparel_feet_armor"), 1.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.FEET)
                .add(attr, new AttributeModifier(Freerun.id("apparel_feet_perk"), amount, op), EquipmentSlotGroup.FEET);
        if (b == PUMBA) {
            mods.add(Attributes.SAFE_FALL_DISTANCE, new AttributeModifier(Freerun.id("apparel_feet_perk2"), 1.5, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.FEET);
        }
        Item item = reg(id, new Wearable(material(b), ArmorItem.Type.BOOTS, b, perk, mods.build()));
        SHOE_PRICES.add(new Object[]{item, 1, price});
        return item;
    }

    private static Item clothes(Brand b, String id, ArmorItem.Type type, int price) {
        int armor = type == ArmorItem.Type.CHESTPLATE ? 2 : 1;
        EquipmentSlotGroup slot = EquipmentSlotGroup.bySlot(type.getSlot());
        ItemAttributeModifiers mods = ItemAttributeModifiers.builder()
                .add(Attributes.ARMOR, new AttributeModifier(Freerun.id("apparel_" + type.getName() + "_armor"), armor,
                        AttributeModifier.Operation.ADD_VALUE), slot)
                .build();
        Item item = reg(id, new Wearable(material(b), type, b, null, mods));
        CLOTHES_PRICES.add(new Object[]{item, 1, price});
        return item;
    }

    private static Item reg(String id, Item item) {
        Item r = Registry.register(BuiltInRegistries.ITEM, Freerun.id(id), item);
        TAB.add(r);
        return r;
    }

    /** A piece of clothing: light armour with its brand (and perk, for shoes) on the tooltip. */
    public static final class Wearable extends ArmorItem {
        private final Brand brand;
        private final String perk;
        private final ItemAttributeModifiers mods;

        public Wearable(Holder<ArmorMaterial> material, Type type, Brand brand, String perk, ItemAttributeModifiers mods) {
            super(material, type, new Item.Properties().stacksTo(1).durability(type.getDurability(12)));
            this.brand = brand;
            this.perk = perk;
            this.mods = mods;
        }

        @Override
        public ItemAttributeModifiers getDefaultAttributeModifiers() {
            return mods;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
            lines.add(Component.literal(brand.name()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            if (perk != null) lines.add(Component.literal(perk).withStyle(ChatFormatting.AQUA));
        }
    }
}
