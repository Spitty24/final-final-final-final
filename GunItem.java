package dev.freerun.item.gun;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A player-usable gun. Right-click fires; consumes one matching ammo unless in creative. */
public class GunItem extends Item {
    private final GunType type;

    public GunItem(GunType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public GunType getType() {
        return type;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean creative = player.getAbilities().instabuild;

        if (!creative && !consumeAmmo(player)) {
            level.playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL,
                    SoundSource.PLAYERS, 0.7f, 1.3f);   // click: out of ammo
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide) {
            type.fire(level, player, player.getLookAngle());
        }
        player.getCooldowns().addCooldown(this, type.cooldown);
        player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private boolean consumeAmmo(Player player) {
        Item ammo = type.ammoItem();
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(ammo)) {
                s.shrink(1);
                return true;
            }
        }
        return false;
    }
}
