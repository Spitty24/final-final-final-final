package dev.freerun.laser;

import dev.freerun.Freerun;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** The laser bay: the laser cutter, its bed and CNC terminal, Taryn, and the work phone. */
public final class LaserRegistry {
    public static Block LASER_CUTTER, LASER_TABLE, CNC_TERMINAL;
    public static BlockEntityType<LaserCutterBlockEntity> LASER_CUTTER_BE;
    public static Item LASER_PART, WORK_PHONE, TARYN_EGG;
    public static EntityType<TarynEntity> TARYN;
    public static EntityType<DrGuildEntity> DR_GUILD;
    public static Item DR_GUILD_EGG;
    public static SoundEvent LASER_START, LASER_CUT, TEAMS_PING;

    private LaserRegistry() {}

    public static void register() {
        LASER_CUTTER = block("laser_cutter", new LaserCutterBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()));
        LASER_TABLE = block("laser_table", new LaserTableBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()));
        CNC_TERMINAL = block("cnc_terminal", new CncTerminalBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                .noOcclusion().lightLevel(s -> 5)));
        LASER_CUTTER_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Freerun.id("laser_cutter"),
                BlockEntityType.Builder.of(LaserCutterBlockEntity::new, LASER_CUTTER).build(null));
        LASER_PART = item("laser_cut_part", new LaserPartItem(new Item.Properties().stacksTo(16)));
        WORK_PHONE = item("work_phone", new WorkPhoneItem(new Item.Properties().stacksTo(1)));

        TARYN = Registry.register(BuiltInRegistries.ENTITY_TYPE, Freerun.id("taryn"),
                EntityType.Builder.of(TarynEntity::new, MobCategory.CREATURE)
                        .sized(0.6f, 1.8f).ridingOffset(-0.7f).clientTrackingRange(10).build("taryn"));
        FabricDefaultAttributeRegistry.register(TARYN, TarynEntity.createAttributes());
        DR_GUILD = Registry.register(BuiltInRegistries.ENTITY_TYPE, Freerun.id("dr_guild"),
                EntityType.Builder.of(DrGuildEntity::new, MobCategory.CREATURE)
                        .sized(0.6f, 1.95f).ridingOffset(-0.7f).clientTrackingRange(10).build("dr_guild"));
        FabricDefaultAttributeRegistry.register(DR_GUILD, DrGuildEntity.createAttributes());
        DR_GUILD_EGG = item("dr_guild_spawn_egg", new SpawnEggItem(DR_GUILD, 0x4a4a3a, 0x5a8ad0, new Item.Properties()));
        TARYN_EGG = item("taryn_spawn_egg", new SpawnEggItem(TARYN, 0x2a3f6e, 0x7cff5a, new Item.Properties()));

        LASER_START = sound("laser_start");
        LASER_CUT = sound("laser_cut");
        TEAMS_PING = sound("teams_ping");

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(e -> {
            e.accept(LASER_CUTTER);
            e.accept(LASER_TABLE);
            e.accept(CNC_TERMINAL);
        });
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(e -> e.accept(WORK_PHONE));
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(e -> e.accept(LASER_PART));
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(e -> {
            e.accept(TARYN_EGG);
            e.accept(DR_GUILD_EGG);
        });
    }

    private static SoundEvent sound(String name) {
        return Registry.register(BuiltInRegistries.SOUND_EVENT, Freerun.id(name), SoundEvent.createVariableRangeEvent(Freerun.id(name)));
    }

    private static Item item(String name, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, Freerun.id(name), item);
    }

    private static Block block(String name, Block block) {
        Block reg = Registry.register(BuiltInRegistries.BLOCK, Freerun.id(name), block);
        item(name, new BlockItem(reg, new Item.Properties()));
        return reg;
    }
}
