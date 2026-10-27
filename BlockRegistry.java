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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * "Pointless Ore": a block that is a full copy of stone (same look, same mining), generates in
 * stone underground, and has no use whatsoever. Its worldgen feature is defined in JSON under
 * data/freerun/worldgen; this just registers the block, its item, and adds the feature to biomes.
 */
public final class BlockRegistry {
    public static Block POINTLESS_ORE;

    private static final ResourceKey<PlacedFeature> POINTLESS_ORE_PLACED =
            ResourceKey.create(Registries.PLACED_FEATURE, Freerun.id("pointless_ore"));

    private BlockRegistry() {}

    public static void register() {
        POINTLESS_ORE = Registry.register(BuiltInRegistries.BLOCK, Freerun.id("pointless_ore"),
                new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)));

        Item item = Registry.register(BuiltInRegistries.ITEM, Freerun.id("pointless_ore"),
                new BlockItem(POINTLESS_ORE, new Item.Properties()));

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.NATURAL_BLOCKS)
                .register(entries -> entries.accept(item));

        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.UNDERGROUND_ORES, POINTLESS_ORE_PLACED);
    }
}
