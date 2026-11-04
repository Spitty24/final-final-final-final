package dev.freerun.world;

import dev.freerun.mobs.LivingStoneEntity;
import dev.freerun.mobs.MoreMobs;
import net.minecraft.core.BlockPos;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The fake "Stone": looks and mines exactly like stone and does nothing — except that one time in
 * ten, the block you just broke gets up and follows you around.
 */
public class PointlessOreBlock extends Block {
    public static final com.mojang.serialization.MapCodec<PointlessOreBlock> CODEC = simpleCodec(PointlessOreBlock::new);

    public PointlessOreBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity,
                              ItemStack tool) {
        if (!level.isClientSide && level.random.nextInt(10) == 0) {
            player.awardStat(Stats.BLOCK_MINED.get(this));
            player.causeFoodExhaustion(0.005f);
            comeAlive(level, pos, player);
            return;   // it walked off, so nothing drops
        }
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }

    /** Creative mode skips playerDestroy, so roll here for creative players. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative() && level.random.nextInt(10) == 0) comeAlive(level, pos, player);
        return super.playerWillDestroy(level, pos, state, player);
    }

    private static void comeAlive(Level level, BlockPos pos, Player player) {
        LivingStoneEntity stone = new LivingStoneEntity(MoreMobs.LIVING_STONE, level);
        stone.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYRot() + 180.0f, 0.0f);
        stone.setOwner(player);
        level.addFreshEntity(stone);
        stone.say(player, "...Chud.");
    }
}
