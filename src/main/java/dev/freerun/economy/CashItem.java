package dev.freerun.economy;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** A dollar bill of some denomination. */
public class CashItem extends Item {
    private final int value;

    public CashItem(int value, Properties properties) {
        super(properties);
        this.value = value;
    }

    public int value() {
        return value;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§a" + Money.fmt(value) + (stack.getCount() > 1 ? " §7× " + stack.getCount()
                + " = §a" + Money.fmt(value * stack.getCount()) : "")));
        tooltip.add(Component.literal("§8Spend it at the Double Quick or the General Store."));
    }
}
