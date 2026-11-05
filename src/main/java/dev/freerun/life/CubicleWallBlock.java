package dev.freerun.life;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.IronBarsBlock;

/** A fabric cubicle partition. Joins up with its neighbours like glass panes do. */
public class CubicleWallBlock extends IronBarsBlock {
    public static final MapCodec<CubicleWallBlock> CODEC = simpleCodec(CubicleWallBlock::new);

    public CubicleWallBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends IronBarsBlock> codec() {
        return CODEC;
    }
}
