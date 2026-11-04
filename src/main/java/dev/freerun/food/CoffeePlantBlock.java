package dev.freerun.food;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;

/** Coffee: plant coffee beans on farmland; when it's fully grown and red with cherries, harvest it. */
public class CoffeePlantBlock extends CropBlock {
    public static final MapCodec<CoffeePlantBlock> CODEC = simpleCodec(CoffeePlantBlock::new);

    public CoffeePlantBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends CropBlock> codec() {
        return CODEC;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return FoodRegistry.COFFEE_BEANS;
    }
}
