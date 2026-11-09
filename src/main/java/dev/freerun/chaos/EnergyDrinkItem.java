package dev.freerun.chaos;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/** LOCKD Energy. Forty-five seconds of pure focus; the crash afterwards is your problem. */
public class EnergyDrinkItem extends Item {
    public static final int DURATION = 900;

    public EnergyDrinkItem(Properties p) {
        super(p);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 24;
    }

    @Override
    public SoundEvent getEatingSound() {
        return SoundEvents.GENERIC_DRINK;
    }

    @Override
    public SoundEvent getDrinkingSound() {
        return SoundEvents.GENERIC_DRINK;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal("Zero sugar. Zero chill.").withStyle(ChatFormatting.GREEN, ChatFormatting.ITALIC));
        lines.add(Component.literal("Locked In for 45s, then the crash").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide) {
            entity.removeEffect(ChaosRegistry.BRAIN_FOG);
            entity.addEffect(new MobEffectInstance(ChaosRegistry.LOCKED_IN, DURATION, 0));
            if (entity instanceof ServerPlayer p) p.displayClientMessage(Component.literal("§a§lLOCKED IN."), true);
        }
        return super.finishUsingItem(stack, level, entity);
    }
}
