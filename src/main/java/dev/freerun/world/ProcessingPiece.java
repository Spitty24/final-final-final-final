package dev.freerun.world;

import dev.freerun.factory.FactoryRegistry;
import dev.freerun.food.FoodRegistry;
import dev.freerun.mobs.RoadhouseNpcEntity.Role;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * The sausage processing plant, built onto the back of the press department (local z=0 is the
 * factory's back wall, with the door in it). Produce bins of fillers on the west wall, a walk-in
 * cooler full of meat, a line of six sausage stuffers with sausage makers at work, a conveyor to
 * the packed-sausage bin, smokers and grills, packing counters, a hand-wash station, and recipe
 * signs on the wall.
 */
public class ProcessingPiece extends StructurePiece {
    static final int OFFSET_Z = FactoryPiece.MAX_Z;
    private static final int W = 22;
    private static final int D = 18;
    private static final int CEIL = 6;

    public ProcessingPiece(BlockPos factoryOrigin) {
        super(StructureRegistry.PROCESSING_PIECE, 0, new BoundingBox(
                factoryOrigin.getX(), factoryOrigin.getY(), factoryOrigin.getZ() + OFFSET_Z,
                factoryOrigin.getX() + W, factoryOrigin.getY() + CEIL + 1, factoryOrigin.getZ() + OFFSET_Z + D));
        setOrientation(Direction.SOUTH);
    }

    public ProcessingPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(StructureRegistry.PROCESSING_PIECE, tag);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
    }

    private BlockState facing(Block b, Direction d) {
        return b.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, d);
    }

    private void barrel(WorldGenLevel level, BoundingBox box, int x, int z, Direction d, ItemStack... contents) {
        placeBlock(level, Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, d), x, 1, z, box);
        BlockPos p = getWorldPos(x, 1, z).immutable();
        if (box.isInside(p)) AnnexHelpers.fill(level, p, contents);
    }

    private void sign(WorldGenLevel level, BoundingBox box, int x, int y, int z, String... lines) {
        placeBlock(level, Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, Direction.SOUTH), x, y, z, box);
        BlockPos p = getWorldPos(x, y, z).immutable();
        if (box.isInside(p)) AnnexHelpers.signText(level, p, lines);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator chunkGenerator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        // ---- shell: food-grade white tile, white walls, bright lights
        for (int x = 0; x <= W; x++) {
            for (int z = 1; z <= D; z++) {
                boolean edge = x == 0 || x == W || z == D;
                placeBlock(level, (x % 4 == 0 || z % 4 == 0 ? Blocks.LIGHT_GRAY_CONCRETE : Blocks.WHITE_CONCRETE).defaultBlockState(), x, 0, z, box);
                for (int y = 1; y < CEIL; y++) {
                    BlockState s;
                    if (!edge) s = Blocks.AIR.defaultBlockState();
                    else if (y <= 2) s = Blocks.WHITE_TERRACOTTA.defaultBlockState();
                    else if (y == 3 && z == D && x % 3 == 1) s = Blocks.GLASS.defaultBlockState();
                    else s = Blocks.WHITE_CONCRETE.defaultBlockState();
                    placeBlock(level, s, x, y, z, box);
                }
                placeBlock(level, (!edge && x % 4 == 2 && z % 4 == 2 ? Blocks.SEA_LANTERN : Blocks.WHITE_CONCRETE).defaultBlockState(), x, CEIL, z, box);
                placeBlock(level, Blocks.GRAY_CONCRETE.defaultBlockState(), x, CEIL + 1, z, box);
            }
        }
        for (int x = 3; x <= 4; x++) {   // doorway through the factory wall
            for (int y = 1; y <= 3; y++) placeBlock(level, Blocks.AIR.defaultBlockState(), x, y, 0, box);
        }

        // ---- signs: the rules and the recipes
        sign(level, box, 5, 3, 1, "SAUSAGE PLANT", "Hairnets", "required.", "No chuds.");
        sign(level, box, 9, 3, 1, "PORK", "alone: Boudin", "+beet: Chorizo", "+berry: Brkfst");
        sign(level, box, 10, 3, 1, "BEEF", "alone: Frank", "+potato: Kielb.", "+wheat: Brat");
        sign(level, box, 11, 3, 1, "chicken+apple", "mutton+beet", "rabbit+carrot", "fish+kelp");
        sign(level, box, 12, 3, 1, "rotten flesh +", "Stone =", "CHUD SAUSAGE", "(please don't)");
        sign(level, box, 13, 3, 1, "HOW TO:", "1) crop in the", "   hopper", "2) then the meat");

        // ---- hand-wash station by the door
        placeBlock(level, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3), 6, 1, 1, box);
        placeBlock(level, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3), 7, 1, 1, box);

        // ---- produce bins (the fillers) on the west wall
        Item stone = BlockRegistry.POINTLESS_ORE.asItem();
        Item[] produce = {Items.WHEAT, Items.BEETROOT, Items.SWEET_BERRIES, Items.POTATO, Items.APPLE, Items.CARROT, Items.DRIED_KELP, stone};
        for (int i = 0; i < produce.length; i++) {
            barrel(level, box, 1, 2 + i, Direction.EAST, new ItemStack(produce[i], produce[i] == stone ? 8 : 24));
        }

        // ---- walk-in cooler in the south-west corner
        for (int z = 11; z <= D - 1; z++) {
            for (int y = 1; y < CEIL; y++) placeBlock(level, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), 7, y, z, box);
        }
        for (int x = 1; x <= 7; x++) {
            for (int y = 1; y < CEIL; y++) {
                boolean doorway = x == 4 && y <= 2;
                placeBlock(level, (doorway ? Blocks.AIR : Blocks.LIGHT_GRAY_CONCRETE).defaultBlockState(), x, y, 11, box);
            }
        }
        for (int x = 1; x <= 6; x++) {
            for (int z = 12; z <= D - 1; z++) placeBlock(level, Blocks.PACKED_ICE.defaultBlockState(), x, 0, z, box);
        }
        for (int x = 2; x <= 5; x++) placeBlock(level, Blocks.CHAIN.defaultBlockState(), x, 5, 14, box);
        barrel(level, box, 1, 12, Direction.EAST, new ItemStack(Items.PORKCHOP, 32));
        barrel(level, box, 1, 13, Direction.EAST, new ItemStack(Items.BEEF, 24));
        barrel(level, box, 1, 14, Direction.EAST, new ItemStack(Items.CHICKEN, 16));
        barrel(level, box, 1, 15, Direction.EAST, new ItemStack(Items.MUTTON, 16));
        barrel(level, box, 1, 16, Direction.EAST, new ItemStack(Items.RABBIT, 12));
        barrel(level, box, 1, 17, Direction.EAST, new ItemStack(Items.COD, 12), new ItemStack(Items.SALMON, 8));
        barrel(level, box, 5, 17, Direction.NORTH, new ItemStack(Items.ROTTEN_FLESH, 8));

        // ---- the stuffing line, with a conveyor carrying finished links to the bin
        for (int x = 8; x <= 18; x += 2) {
            placeBlock(level, facing(FoodRegistry.SAUSAGE_STUFFER, Direction.NORTH), x, 1, 7, box);
        }
        for (int x = 8; x <= 18; x++) {
            placeBlock(level, facing(FactoryRegistry.CONVEYOR, Direction.EAST), x, 1, 8, box);
        }
        Item[] sausages = {FoodRegistry.BOUDIN, FoodRegistry.CHORIZO, FoodRegistry.BREAKFAST_LINKS, FoodRegistry.BEEF_FRANK,
                FoodRegistry.KIELBASA, FoodRegistry.BRATWURST, FoodRegistry.CHICKEN_APPLE_SAUSAGE, FoodRegistry.MERGUEZ,
                FoodRegistry.HUNTERS_SAUSAGE, FoodRegistry.SEAFOOD_SAUSAGE};
        ItemStack[] packed = new ItemStack[sausages.length];
        for (int i = 0; i < sausages.length; i++) packed[i] = new ItemStack(sausages[i], 4);
        barrel(level, box, 19, 8, Direction.UP, packed);
        for (int x = 8; x <= 16; x += 4) {
            BlockPos w = getWorldPos(x, 1, 6).immutable();
            if (box.isInside(w)) AnnexHelpers.standingWorker(level, w, Direction.SOUTH, Role.SAUSAGE_MAKER, new ItemStack(Items.PORKCHOP));
        }

        // ---- smokehouse corner and grills on the east wall
        for (int z = 2; z <= 8; z += 2) {
            placeBlock(level, facing(Blocks.SMOKER, Direction.WEST).setValue(BlockStateProperties.LIT, true), W - 1, 1, z, box);
        }
        placeBlock(level, facing(BlockRegistry.GRILL, Direction.WEST), W - 1, 1, 10, box);
        placeBlock(level, facing(BlockRegistry.GRILL, Direction.WEST), W - 1, 1, 12, box);

        // ---- packing counters along the back, a packing table in the middle
        for (int x = 9; x <= W - 2; x++) {
            placeBlock(level, facing(FoodRegistry.COUNTER, Direction.NORTH), x, 1, D - 1, box);
        }
        BlockPos sample = getWorldPos(9, 1, D - 1).immutable();
        if (box.isInside(sample)) {
            ItemStack[] samples = new ItemStack[sausages.length + 1];
            for (int i = 0; i < sausages.length; i++) samples[i] = new ItemStack(sausages[i], 2);
            samples[sausages.length] = new ItemStack(FoodRegistry.CHUD_SAUSAGE, 1);
            AnnexHelpers.fill(level, sample, samples);
        }
        for (int x = 13; x <= 15; x++) placeBlock(level, BlockRegistry.TABLE.defaultBlockState(), x, 1, 13, box);
    }
}
