package dev.freerun.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Point and right-click to fire a nuke shell. Short cooldown; never consumed. */
public class NukeLauncherItem extends Item {

    public NukeLauncherItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.5f, 0.6f);

        if (!level.isClientSide) {
            NukeProjectile shell = new NukeProjectile(level, player);
            shell.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 2.6f, 0.5f);
            level.addFreshEntity(shell);
        }

        player.getCooldowns().addCooldown(this, 40);   // 2s between shots
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
