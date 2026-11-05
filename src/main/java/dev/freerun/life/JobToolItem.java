package dev.freerun.life;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** The janitor's mop and the IT department's toolkit. */
public class JobToolItem extends Item {
    private final boolean mop;

    public JobToolItem(Properties properties, boolean mop) {
        super(properties);
        this.mop = mop;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (ctx.getLevel().isClientSide) return InteractionResult.SUCCESS;
        if (!(ctx.getPlayer() instanceof ServerPlayer sp)) return InteractionResult.PASS;
        BlockState state = ctx.getLevel().getBlockState(ctx.getClickedPos());
        if (mop) {
            var pos = state.getBlock() instanceof SpillBlock ? ctx.getClickedPos() : ctx.getClickedPos().above();
            if (ctx.getLevel().getBlockState(pos).getBlock() instanceof SpillBlock) {
                OfficeJobs.clean(sp, pos, true);
                return InteractionResult.CONSUME;
            }
            sp.displayClientMessage(Component.literal("§7You mop. The floor was already clean, but it's cleaner now."), true);
            return InteractionResult.CONSUME;
        }
        return OfficeJobs.toolkit(sp, ctx.getClickedPos(), state) ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(mop ? "§7Right-click spills and trash to clean them up." : "§7Right-click crashed computers, jammed printers and server alarms."));
    }
}
