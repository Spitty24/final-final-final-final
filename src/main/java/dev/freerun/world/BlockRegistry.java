package dev.freerun.world;

import dev.freerun.Freerun;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * "Pointless Ore": a block that is a full copy of stone (same look, same mining), generates in
 * stone underground, and has no use whatsoever. Its worldgen feature is defined in JSON under
 * data/freerun/worldgen; this just registers the block, its item, and adds the feature to biomes.
 */
public final class BlockRegistry {
    public static Block POINTLESS_ORE;
    public static Block TEXAS_ROADHOUSE_SIGN;
    public static Block CHECKERED_TABLECLOTH;
    public static Block TABLE;
    public static Block GRILL;
    public static BlockEntityType<GrillBlockEntity> GRILL_BE;

    private static final ResourceKey<PlacedFeature> POINTLESS_ORE_PLACED =
            ResourceKey.create(Registries.PLACED_FEATURE, Freerun.id("pointless_ore"));

    private BlockRegistry() {}

    public static void register() {
        POINTLESS_ORE = block("pointless_ore",
                new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)), CreativeModeTabs.NATURAL_BLOCKS);

        // Decorative blocks for the Texas Roadhouse.
        TEXAS_ROADHOUSE_SIGN = block("texas_roadhouse_sign",
                new TexasRoadhouseSignBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)),
                CreativeModeTabs.FUNCTIONAL_BLOCKS);
        CHECKERED_TABLECLOTH = block("checkered_tablecloth",
                new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_WOOL)),
                CreativeModeTabs.BUILDING_BLOCKS);
        TABLE = block("table",
                new TableBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion()),
                CreativeModeTabs.FUNCTIONAL_BLOCKS);
        GRILL = block("grill",
                new GrillBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                        .lightLevel(s -> s.getValue(BlockStateProperties.LIT) ? 13 : 0)),
                CreativeModeTabs.FUNCTIONAL_BLOCKS);

        GRILL_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Freerun.id("grill"),
                BlockEntityType.Builder.of(GrillBlockEntity::new, GRILL).build(null));

        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.UNDERGROUND_ORES, POINTLESS_ORE_PLACED);
    }

    /** Registers a block + its item and drops the item in the given creative tab. */
    private static Block block(String name, Block block, net.minecraft.resources.ResourceKey<net.minecraft.world.item.CreativeModeTab> tab) {
        Block registered = Registry.register(BuiltInRegistries.BLOCK, Freerun.id(name), block);
        Item item = Registry.register(BuiltInRegistries.ITEM, Freerun.id(name),
                new BlockItem(registered, new Item.Properties()));
        ItemGroupEvents.modifyEntriesEvent(tab).register(entries -> entries.accept(item));
        return registered;
    }
}
