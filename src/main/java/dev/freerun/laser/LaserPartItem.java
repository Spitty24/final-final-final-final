package dev.freerun.laser;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

/** A part cut on the laser. It carries its own shape, which is how it's drawn in your hand. */
public class LaserPartItem extends Item {
    public LaserPartItem(Properties p) {
        super(p);
    }

    public static ItemStack make(long[] shape, int gas) {
        ItemStack s = new ItemStack(LaserRegistry.LASER_PART);
        CompoundTag t = new CompoundTag();
        t.putLongArray("Shape", LaserShape.copy(shape));
        t.putInt("Gas", gas);
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        return s;
    }

    public static long[] shape(ItemStack stack) {
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        if (d == null) return null;
        CompoundTag t = d.copyTag();
        return t.contains("Shape") ? LaserShape.copy(t.getLongArray("Shape")) : null;
    }

    public static int gas(ItemStack stack) {
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        return d == null ? 0 : d.copyTag().getInt("Gas");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        long[] s = shape(stack);
        if (s == null) {
            lines.add(Component.literal("Blank").withStyle(ChatFormatting.GRAY));
            return;
        }
        int pieces = LaserShape.pierces(s);
        lines.add(Component.literal(LaserShape.count(s) + " cells, " + pieces + (pieces == 1 ? " piece" : " pieces")).withStyle(ChatFormatting.GRAY));
        lines.add(gas(stack) == LaserCutterBlockEntity.NITROGEN
                ? Component.literal("Nitrogen cut: bright, oxide-free edge").withStyle(ChatFormatting.AQUA)
                : Component.literal("Oxygen cut: dark oxide edge").withStyle(ChatFormatting.GOLD));
    }
}
