package dev.freerun.economy;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Anything you buy at the counter and eat or drink: chips, candy, roller-grill food, sodas, energy
 * drinks, slushies. Drinks use the drinking animation, cans leave an Empty Can behind, and some have
 * an extra effect when finished (brain freeze, clearing your effects, and so on).
 */
public class SnackItem extends Item {
    private final boolean drink;
    private final String flavour;
    private final Supplier<Item> leftover;
    private final BiConsumer<Level, LivingEntity> onFinish;
    private final int useTicks;

    public SnackItem(Properties properties, boolean drink, int useTicks, String flavour, Supplier<Item> leftover,
                     BiConsumer<Level, LivingEntity> onFinish) {
        super(properties);
        this.drink = drink;
        this.useTicks = useTicks;
        this.flavour = flavour;
        this.leftover = leftover;
        this.onFinish = onFinish;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return drink ? UseAnim.DRINK : UseAnim.EAT;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return useTicks;
    }

    @Override
    public SoundEvent getDrinkingSound() {
        return SoundEvents.GENERIC_DRINK;
    }

    @Override
    public SoundEvent getEatingSound() {
        return drink ? SoundEvents.GENERIC_DRINK : SoundEvents.GENERIC_EAT;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        Item left = leftover == null ? null : leftover.get();
        boolean creative = entity instanceof Player p && p.getAbilities().instabuild;
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide) {
            if (onFinish != null) onFinish.accept(level, entity);
            if (entity instanceof ServerPlayer sp) dev.freerun.career.Careers.addXp(sp, 1, "snack break");
        }
        if (left != null && !creative) {
            if (result.isEmpty()) return new ItemStack(left);
            if (entity instanceof Player p && !p.getInventory().add(new ItemStack(left))) p.drop(new ItemStack(left), false);
        }
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (flavour != null) tooltip.add(Component.literal("§7§o" + flavour));
    }
}
