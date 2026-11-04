package dev.freerun.food;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** A sausage. Just food, plus a line of flavour text (and a warning, for one of them). */
public class SausageItem extends Item {
    private final String flavour;
    private final String eatMessage;

    public SausageItem(Properties properties, String flavour, String eatMessage) {
        super(properties);
        this.flavour = flavour;
        this.eatMessage = eatMessage;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && eatMessage != null && entity instanceof Player player) {
            player.displayClientMessage(Component.literal(eatMessage), false);
        }
        return super.finishUsingItem(stack, level, entity);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (flavour != null) tooltip.add(Component.literal("§7" + flavour));
    }
}
