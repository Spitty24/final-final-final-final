package dev.freerun.factory;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Places a forklift on the block you click, facing the way you're looking. */
public class ForkliftItem extends Item {

    public ForkliftItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
        Vec3 at = ctx.getClickedFace() == net.minecraft.core.Direction.UP
                ? ctx.getClickLocation() : Vec3.atBottomCenterOf(pos);
        ForkliftEntity forklift = new ForkliftEntity(FactoryRegistry.FORKLIFT, level);
        Player player = ctx.getPlayer();
        float yaw = player != null ? player.getYRot() : 0.0f;
        forklift.moveTo(at.x, at.y, at.z, yaw, 0.0f);
        forklift.setYBodyRot(yaw);
        forklift.setYHeadRot(yaw);
        if (!level.noCollision(forklift, forklift.getBoundingBox())) return InteractionResult.FAIL;
        level.addFreshEntity(forklift);
        ctx.getItemInHand().consume(1, player);
        return InteractionResult.CONSUME;
    }
}
