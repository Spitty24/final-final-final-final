package dev.freerun.life;

import dev.freerun.Freerun;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/**
 * City life: office fixtures, home furniture, the bar and the arcade, the janitor's mop and the
 * IT toolkit, arcade tickets and prizes.
 */
public final class LifeRegistry {
    // office
    public static Block COMPUTER, OFFICE_DESK, OFFICE_CHAIR, CUBICLE_WALL, FILING_CABINET, PRINTER, WATER_COOLER, SERVER_RACK,
            WHITEBOARD, DESK_PHONE, JOB_BOARD, SPILL, TRASH_BAG;
    // home
    public static Block SOFA, ARMCHAIR, COFFEE_TABLE, FLOOR_LAMP, FRIDGE, STOVE, KITCHEN_SINK, TOILET, BATHTUB, WARDROBE,
            NIGHTSTAND, FIREPLACE, PIANO, GRANDFATHER_CLOCK, BIG_PLANT, RUG, MAILBOX;
    // fun
    public static Block ARCADE_CABINET, CLAW_MACHINE, DART_BOARD, POOL_TABLE, BAR_STOOL, BAR_TAP;
    public static Item MOP, IT_TOOLKIT, TICKET, GATOR_PLUSH, PELICAN_PLUSH, RUBBER_DUCK;
    public static BlockEntityType<FurnitureStorageBlockEntity> STORAGE_BE;

    /** Everything a furniture store sells, in catalogue order. */
    public static final List<Block> HOME_FURNITURE = new ArrayList<>();
    public static final List<Block> OFFICE_FURNITURE = new ArrayList<>();
    private static final List<Item> TAB = new ArrayList<>();

    private LifeRegistry() {}

    private static VoxelShape box(double... b) {
        VoxelShape s = Shapes.empty();
        for (int i = 0; i + 5 < b.length; i += 6) s = Shapes.or(s, Block.box(b[i], b[i + 1], b[i + 2], b[i + 3], b[i + 4], b[i + 5]));
        return s;
    }

    private static BlockBehaviour.Properties wood() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(1.5f).noOcclusion();
    }

    private static BlockBehaviour.Properties metal() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.0f).noOcclusion();
    }

    private static BlockBehaviour.Properties cloth() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_WOOL).strength(0.8f).noOcclusion();
    }

    private static BlockBehaviour.Properties lit(BlockBehaviour.Properties p, int level) {
        return p.lightLevel(s -> s.hasProperty(FurnitureBlock.LIT) && s.getValue(FurnitureBlock.LIT) ? level : 0);
    }

    private static Block f(String name, BlockBehaviour.Properties p, FurnitureBlock.Use use, VoxelShape shape, boolean solid, List<Block> catalogue) {
        Block b = block(name, use == FurnitureBlock.Use.STORAGE ? new FurnitureBlock.Storage(p, shape) : new FurnitureBlock(p, use, shape, solid));
        if (catalogue != null) catalogue.add(b);
        return b;
    }

    public static void register() {
        // ---- office
        COMPUTER = block("computer", new ComputerBlock(metal().strength(1.0f).lightLevel(s -> s.getValue(ComputerBlock.SCREEN) > 0 ? 5 : 0)));
        OFFICE_FURNITURE.add(COMPUTER);
        OFFICE_DESK = f("office_desk", wood(), FurnitureBlock.Use.DESK, box(0, 13, 0, 16, 16, 16, 0, 0, 12, 16, 13, 16, 0, 0, 0, 2, 13, 12, 14, 0, 0, 16, 13, 12), true, OFFICE_FURNITURE);
        OFFICE_CHAIR = f("office_chair", cloth(), FurnitureBlock.Use.SEAT, box(3, 0, 3, 13, 9, 13, 3, 9, 11, 13, 18, 13), true, OFFICE_FURNITURE);
        CUBICLE_WALL = block("cubicle_wall", new CubicleWallBlock(cloth().strength(1.0f)));
        OFFICE_FURNITURE.add(CUBICLE_WALL);
        FILING_CABINET = f("filing_cabinet", metal(), FurnitureBlock.Use.STORAGE, box(2, 0, 1, 14, 16, 15), true, OFFICE_FURNITURE);
        PRINTER = f("printer", metal(), FurnitureBlock.Use.PRINTER, box(1, 0, 2, 15, 10, 14), true, OFFICE_FURNITURE);
        WATER_COOLER = f("water_cooler", metal(), FurnitureBlock.Use.WATER_COOLER, box(4, 0, 4, 12, 16, 12), true, OFFICE_FURNITURE);
        SERVER_RACK = f("server_rack", metal().lightLevel(s -> 4), FurnitureBlock.Use.SERVER, box(1, 0, 1, 15, 16, 15), true, OFFICE_FURNITURE);
        WHITEBOARD = f("whiteboard", metal(), FurnitureBlock.Use.WHITEBOARD, box(0, 2, 14, 16, 15, 16), true, OFFICE_FURNITURE);
        DESK_PHONE = f("desk_phone", metal().strength(0.5f), FurnitureBlock.Use.PHONE, box(4, 0, 5, 12, 4, 12), false, OFFICE_FURNITURE);
        JOB_BOARD = f("job_board", wood(), FurnitureBlock.Use.JOB_BOARD, box(1, 1, 14, 15, 15, 16), true, null);
        SPILL = block("coffee_spill", new SpillBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BROWN_CARPET).strength(0.2f).noOcclusion().noCollission()));
        TRASH_BAG = block("trash_bag", new SpillBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BLACK_WOOL).strength(0.2f).noOcclusion().noCollission()));

        // ---- home
        SOFA = f("sofa", cloth(), FurnitureBlock.Use.SEAT, box(0, 0, 0, 16, 8, 16, 0, 8, 12, 16, 16, 16), true, HOME_FURNITURE);
        ARMCHAIR = f("armchair", cloth(), FurnitureBlock.Use.SEAT, box(0, 0, 0, 16, 8, 16, 0, 8, 12, 16, 16, 16, 0, 8, 0, 3, 12, 12, 13, 8, 0, 16, 12, 12), true, HOME_FURNITURE);
        COFFEE_TABLE = f("coffee_table", wood(), FurnitureBlock.Use.NONE, box(1, 0, 2, 15, 7, 14), true, HOME_FURNITURE);
        FLOOR_LAMP = f("floor_lamp", lit(metal().strength(0.8f), 15), FurnitureBlock.Use.LAMP, box(5, 0, 5, 11, 16, 11), true, HOME_FURNITURE);
        FRIDGE = f("fridge", metal(), FurnitureBlock.Use.STORAGE, box(1, 0, 1, 15, 16, 15), true, HOME_FURNITURE);
        STOVE = f("stove", lit(metal(), 4), FurnitureBlock.Use.STOVE, box(0, 0, 1, 16, 14, 15), true, HOME_FURNITURE);
        KITCHEN_SINK = f("kitchen_sink", metal(), FurnitureBlock.Use.SINK, box(0, 0, 1, 16, 14, 15), true, HOME_FURNITURE);
        TOILET = f("toilet", BlockBehaviour.Properties.ofFullCopy(Blocks.QUARTZ_BLOCK).strength(1.0f).noOcclusion(), FurnitureBlock.Use.TOILET,
                box(3, 0, 2, 13, 9, 13, 2, 0, 12, 14, 16, 16), true, HOME_FURNITURE);
        BATHTUB = f("bathtub", BlockBehaviour.Properties.ofFullCopy(Blocks.QUARTZ_BLOCK).strength(1.0f).noOcclusion(), FurnitureBlock.Use.SEAT,
                box(0, 0, 0, 16, 9, 16), true, HOME_FURNITURE);
        WARDROBE = f("wardrobe", wood(), FurnitureBlock.Use.STORAGE, box(1, 0, 2, 15, 16, 16), true, HOME_FURNITURE);
        NIGHTSTAND = f("nightstand", wood(), FurnitureBlock.Use.STORAGE, box(2, 0, 2, 14, 12, 14), true, HOME_FURNITURE);
        FIREPLACE = f("fireplace", lit(BlockBehaviour.Properties.ofFullCopy(Blocks.BRICKS).strength(2.0f).noOcclusion(), 13),
                FurnitureBlock.Use.LAMP, box(0, 0, 4, 16, 16, 16), true, HOME_FURNITURE);
        PIANO = f("piano", wood(), FurnitureBlock.Use.PIANO, box(0, 0, 4, 16, 16, 16), true, HOME_FURNITURE);
        GRANDFATHER_CLOCK = f("grandfather_clock", wood(), FurnitureBlock.Use.CLOCK, box(3, 0, 4, 13, 16, 12), true, HOME_FURNITURE);
        BIG_PLANT = f("big_plant", BlockBehaviour.Properties.ofFullCopy(Blocks.FLOWER_POT).strength(0.5f).noOcclusion(),
                FurnitureBlock.Use.PLANT, box(3, 0, 3, 13, 16, 13), false, HOME_FURNITURE);
        RUG = f("rug", BlockBehaviour.Properties.ofFullCopy(Blocks.RED_CARPET).noOcclusion(), FurnitureBlock.Use.NONE, box(0, 0, 0, 16, 1, 16), false, HOME_FURNITURE);
        MAILBOX = f("mailbox", metal(), FurnitureBlock.Use.MAILBOX, box(5, 0, 5, 11, 14, 11), true, null);

        // ---- the bar and the arcade
        ARCADE_CABINET = block("arcade_cabinet", new ArcadeBlock(metal().lightLevel(s -> 9)));
        CLAW_MACHINE = f("claw_machine", metal().lightLevel(s -> 8), FurnitureBlock.Use.CLAW, box(1, 0, 1, 15, 16, 15), true, null);
        DART_BOARD = f("dart_board", wood(), FurnitureBlock.Use.DARTS, box(2, 2, 14, 14, 14, 16), true, null);
        POOL_TABLE = f("pool_table", wood(), FurnitureBlock.Use.POOL, box(0, 0, 0, 16, 13, 16), true, null);
        BAR_STOOL = f("bar_stool", wood(), FurnitureBlock.Use.SEAT, box(4, 0, 4, 12, 12, 12), true, null);
        BAR_TAP = f("bar_tap", metal(), FurnitureBlock.Use.TAP, box(3, 0, 5, 13, 12, 11), true, null);

        STORAGE_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Freerun.id("furniture_storage"),
                BlockEntityType.Builder.of(FurnitureStorageBlockEntity::new, FILING_CABINET, FRIDGE, WARDROBE, NIGHTSTAND).build(null));

        // ---- tools, tickets, prizes
        MOP = item("mop", new JobToolItem(new Item.Properties().stacksTo(1), true));
        IT_TOOLKIT = item("it_toolkit", new JobToolItem(new Item.Properties().stacksTo(1), false));
        TICKET = item("arcade_ticket", new Item(new Item.Properties()));
        GATOR_PLUSH = item("gator_plush", new Item(new Item.Properties().stacksTo(16)));
        PELICAN_PLUSH = item("pelican_plush", new Item(new Item.Properties().stacksTo(16)));
        RUBBER_DUCK = item("rubber_duck", new Item(new Item.Properties().stacksTo(16)));

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Freerun.id("city_life"), FabricItemGroup.builder()
                .title(Component.literal("City Life: Office, Home & Fun"))
                .icon(() -> new ItemStack(COMPUTER))
                .displayItems((params, output) -> {
                    for (Item i : TAB) output.accept(i);
                })
                .build());

        LifePayloads.register();
        OfficeJobs.register();
        OfficeLife.register();
        Rentals.register();
        Arcade.register();
    }

    private static Block block(String name, Block block) {
        Block reg = Registry.register(BuiltInRegistries.BLOCK, Freerun.id(name), block);
        item(name, new BlockItem(reg, new Item.Properties()));
        return reg;
    }

    private static Item item(String name, Item item) {
        Item reg = Registry.register(BuiltInRegistries.ITEM, Freerun.id(name), item);
        TAB.add(reg);
        return reg;
    }
}
