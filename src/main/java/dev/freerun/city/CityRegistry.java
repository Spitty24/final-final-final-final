package dev.freerun.city;

import dev.freerun.Freerun;
import dev.freerun.net.PhonePayloads;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

/** The city: its structure and pieces, street furniture, residents, and the phone's network. */
public final class CityRegistry {
    public static StructureType<CityStructure> CITY;
    public static StructurePieceType ROAD_PIECE, INTERSECTION_PIECE, LOT_PIECE, SUBURB_PIECE, OFFICE_PIECE, HOOD_PIECE, PARK_PIECE;
    public static Block TRAFFIC_LIGHT, STOP_SIGN, STREET_LAMP;
    public static BlockEntityType<CityLightBlockEntity> CITY_LIGHT_BE;
    public static EntityType<CitizenEntity> CITIZEN;
    public static EntityType<CityHubEntity> CITY_HUB;
    public static Item CITIZEN_EGG;

    private CityRegistry() {}

    public static void register() {
        CITY = Registry.register(BuiltInRegistries.STRUCTURE_TYPE, Freerun.id("city"), () -> CityStructure.CODEC);
        ROAD_PIECE = piece("city_road", RoadPiece::new);
        INTERSECTION_PIECE = piece("city_intersection", IntersectionPiece::new);
        LOT_PIECE = piece("city_lot", LotPiece::new);
        SUBURB_PIECE = piece("city_suburb", SuburbPiece::new);
        OFFICE_PIECE = piece("city_office", OfficePiece::new);
        HOOD_PIECE = piece("city_hood", HoodPiece::new);
        PARK_PIECE = piece("city_park", ParkPiece::new);

        TRAFFIC_LIGHT = block("traffic_light", new TrafficLightBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                .strength(2.0f).noOcclusion().lightLevel(s -> 6)));
        STOP_SIGN = block("stop_sign", new StopSignBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                .strength(1.5f).noOcclusion()));
        STREET_LAMP = block("street_lamp", new StreetLampBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                .strength(1.5f).noOcclusion().lightLevel(s -> s.getValue(StreetLampBlock.LIT) ? 15 : 0)));
        CITY_LIGHT_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Freerun.id("city_light"),
                BlockEntityType.Builder.of(CityLightBlockEntity::new, TRAFFIC_LIGHT, STREET_LAMP).build(null));

        CITIZEN = Registry.register(BuiltInRegistries.ENTITY_TYPE, Freerun.id("citizen"),
                EntityType.Builder.of(CitizenEntity::new, MobCategory.CREATURE).sized(0.6f, 1.95f).clientTrackingRange(10).build("citizen"));
        FabricDefaultAttributeRegistry.register(CITIZEN, CitizenEntity.createAttributes());
        CITY_HUB = Registry.register(BuiltInRegistries.ENTITY_TYPE, Freerun.id("city_hub"),
                EntityType.Builder.<CityHubEntity>of(CityHubEntity::new, MobCategory.MISC).sized(0.5f, 0.5f)
                        .clientTrackingRange(4).updateInterval(100).noSummon().fireImmune().build("city_hub"));
        CITIZEN_EGG = Registry.register(BuiltInRegistries.ITEM, Freerun.id("citizen_spawn_egg"),
                new SpawnEggItem(CITIZEN, 0x3a5a8c, 0xd9b38c, new Item.Properties()));
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(e -> e.accept(CITIZEN_EGG));
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(e -> {
            e.accept(TRAFFIC_LIGHT);
            e.accept(STOP_SIGN);
            e.accept(STREET_LAMP);
        });

        PayloadTypeRegistry.playC2S().register(PhonePayloads.Request.TYPE, PhonePayloads.Request.CODEC);
        PayloadTypeRegistry.playC2S().register(PhonePayloads.Hack.TYPE, PhonePayloads.Hack.CODEC);
        PayloadTypeRegistry.playS2C().register(PhonePayloads.Data.TYPE, PhonePayloads.Data.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(PhonePayloads.Request.TYPE, (payload, ctx) -> ctx.player().server.execute(() -> {
            CityHubEntity hub = CityHubEntity.nearest(ctx.player().serverLevel(), ctx.player().blockPosition());
            CompoundTag tag = hub != null ? hub.stats() : new CompoundTag();
            if (ServerPlayNetworking.canSend(ctx.player(), PhonePayloads.Data.TYPE)) {
                ServerPlayNetworking.send(ctx.player(), new PhonePayloads.Data(tag));
            }
        }));
        ServerPlayNetworking.registerGlobalReceiver(PhonePayloads.Hack.TYPE, (payload, ctx) ->
                ctx.player().server.execute(() -> Hacking.handle(ctx.player(), payload)));
    }

    private static StructurePieceType piece(String name, StructurePieceType type) {
        return Registry.register(BuiltInRegistries.STRUCTURE_PIECE, Freerun.id(name), type);
    }

    private static Block block(String name, Block block) {
        Block reg = Registry.register(BuiltInRegistries.BLOCK, Freerun.id(name), block);
        Registry.register(BuiltInRegistries.ITEM, Freerun.id(name), new BlockItem(reg, new Item.Properties()));
        return reg;
    }
}
