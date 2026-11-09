package dev.freerun.skate;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** A skateboard. Carry one and press the board key (B) to ride it. */
public class SkateboardItem extends Item {
    public final int deck;
    private final String graphic;

    public SkateboardItem(int deck, String graphic, Properties p) {
        super(p);
        this.deck = deck;
        this.graphic = graphic;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal(graphic).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        lines.add(Component.literal("Press B to drop it and ride").withStyle(ChatFormatting.AQUA));
        lines.add(Component.literal("Mouse is the flick stick: pull back, flick forward to pop").withStyle(ChatFormatting.DARK_GRAY));
    }

    /** The board the player rides: the selected one if it's a board, else the first in the inventory. */
    public static ItemStack find(net.minecraft.world.entity.player.Player p) {
        if (p.getMainHandItem().getItem() instanceof SkateboardItem) return p.getMainHandItem();
        for (ItemStack s : p.getInventory().items) if (s.getItem() instanceof SkateboardItem) return s;
        if (p.getOffhandItem().getItem() instanceof SkateboardItem) return p.getOffhandItem();
        return ItemStack.EMPTY;
    }
}
