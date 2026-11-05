package dev.freerun.garage;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** The torque wrench: right-click a car to raise its top speed, sneak + right-click to lower it. */
public class WrenchItem extends Item {
    public WrenchItem(Properties p) {
        super(p);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal("Right-click a car: raise top speed").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("Sneak + right-click: lower it (50% to 200%)").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("On the HawkSight aligner: turns the tie rods and cams").withStyle(ChatFormatting.DARK_GRAY));
    }
}
