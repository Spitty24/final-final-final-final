package dev.freerun.garage;

import dev.freerun.Freerun;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.ArrayList;
import java.util.List;

/** The garage: car parts, the chassis, the torque wrench, four-post lifts, runways, the aligner. */
public final class GarageRegistry {
    public static Item CHASSIS, WHEEL, ENGINE, TRANSMISSION, BATTERY, FUEL_TANK, STEERING, SEATS, BODY_PANELS, WINDOWS;
    public static Item WRENCH, LIFT_ITEM;
    public static Block RUNWAY, ALIGNER;
    public static BlockEntityType<AlignerBlockEntity> ALIGNER_BE;
    public static EntityType<LiftEntity> LIFT;
    /** Everything the auto-parts counter sells, with prices: {item, count, price}. */
    public static final List<Object[]> PARTS_PRICES = new ArrayList<>();
    private static final List<Item> TAB = new ArrayList<>();

    private GarageRegistry() {}

    public static void register() {
        CHASSIS = item("car_chassis", new ChassisItem(new Item.Properties().stacksTo(1)), 300);
        WHEEL = part("car_wheel", CarPart.WHEEL_FL, "Tyre on a steel rim. A car takes four.", 45);
        ENGINE = part("car_engine", CarPart.ENGINE, "A four-cylinder block, cast and machined.", 400);
        TRANSMISSION = part("car_transmission", CarPart.TRANSMISSION, "Five speeds and a reverse.", 220);
        BATTERY = part("car_battery", CarPart.BATTERY, "12 volts. Don't lick the terminals.", 80);
        FUEL_TANK = part("car_fuel_tank", CarPart.FUEL_TANK, "Welded steel, 50 litres.", 90);
        STEERING = part("car_steering", CarPart.STEERING, "Column, rack and tie rods.", 120);
        SEATS = part("car_seats", CarPart.SEATS, "Two buckets up front, a bench in the back.", 110);
        BODY_PANELS = part("car_body_panels", CarPart.BODY, "Doors, fenders, hood and trunk lid.", 260);
        WINDOWS = part("car_windows", CarPart.GLASS, "Windshield, side glass and a rear window.", 150);
        WRENCH = item("torque_wrench", new WrenchItem(new Item.Properties().stacksTo(1)), 35);
        LIFT_ITEM = item("four_post_lift", new LiftItem(new Item.Properties().stacksTo(1)), 650);

        RUNWAY = block("runway", new RunwayBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()), 6);
        ALIGNER = block("hawksight_aligner", new AlignerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                .noOcclusion().lightLevel(s -> 6)), 1500);
        ALIGNER_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Freerun.id("hawksight_aligner"),
                BlockEntityType.Builder.of(AlignerBlockEntity::new, ALIGNER).build(null));
        LIFT = Registry.register(BuiltInRegistries.ENTITY_TYPE, Freerun.id("four_post_lift"),
                EntityType.Builder.<LiftEntity>of(LiftEntity::new, MobCategory.MISC)
                        .sized(6.0f, 2.3f).clientTrackingRange(10).updateInterval(2).fireImmune().build("four_post_lift"));

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Freerun.id("garage"), FabricItemGroup.builder()
                .title(Component.literal("Garage"))
                .icon(() -> new ItemStack(WRENCH))
                .displayItems((params, out) -> {
                    for (Item i : TAB) out.accept(i);
                })
                .build());
    }

    private static Item part(String name, CarPart part, String hint, int price) {
        return item(name, new CarPart.PartItem(part, hint, new Item.Properties().stacksTo(part == CarPart.WHEEL_FL ? 4 : 1)), price);
    }

    private static Item item(String name, Item item, int price) {
        Item r = Registry.register(BuiltInRegistries.ITEM, Freerun.id(name), item);
        TAB.add(r);
        PARTS_PRICES.add(new Object[]{r, 1, price});
        return r;
    }

    private static Block block(String name, Block block, int price) {
        Block r = Registry.register(BuiltInRegistries.BLOCK, Freerun.id(name), block);
        item(name, new BlockItem(r, new Item.Properties()), price);
        return r;
    }
}
