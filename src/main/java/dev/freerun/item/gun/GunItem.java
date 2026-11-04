package dev.freerun.item.gun;

import dev.freerun.economy.ModSounds;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A player-usable gun with a magazine. Right-click fires (the rifle keeps firing while you hold it);
 * when the magazine runs dry it reloads from the ammo in your inventory, which takes a moment.
 * Sneak + right-click to top up the magazine early. Each shot kicks your view up a little.
 */
public class GunItem extends Item {
    private final GunType type;

    public GunItem(GunType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public GunType getType() {
        return type;
    }

    public static int loaded(ItemStack stack) {
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        if (d == null || !d.copyTag().contains("Mag")) return -1;   // never loaded: treat as full
        return d.copyTag().getInt("Mag");
    }

    private int rounds(ItemStack stack) {
        int n = loaded(stack);
        return n < 0 ? type.magazine : n;
    }

    private static void setRounds(ItemStack stack, int n) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putInt("Mag", n));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (rounds(stack) < type.magazine && reload(level, player, stack)) {
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
            return InteractionResultHolder.fail(stack);
        }
        if (type.automatic) {
            player.startUsingItem(hand);
            shoot(level, player, stack);
            return InteractionResultHolder.consume(stack);
        }
        shoot(level, player, stack);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** Full-auto: one round every {@link GunType#cooldown} ticks while the button's held. */
    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!(entity instanceof Player player) || player.getCooldowns().isOnCooldown(this)) return;
        if ((getUseDuration(stack, entity) - remaining) % type.cooldown == 0) shoot(level, player, stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return type.automatic ? UseAnim.BOW : UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return type.automatic ? 72000 : 0;
    }

    private void shoot(Level level, Player player, ItemStack stack) {
        if (player.getCooldowns().isOnCooldown(this)) return;
        boolean creative = player.getAbilities().instabuild;
        int mag = rounds(stack);
        if (mag <= 0 && !creative) {
            if (!reload(level, player, stack)) {
                level.playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.7f, 1.6f);
                if (!level.isClientSide) player.displayClientMessage(Component.literal("§c*click* §7Out of "
                        + (type.ammo == GunType.Ammo.SHELL ? "shells" : "bullets") + "."), true);
                player.getCooldowns().addCooldown(this, 10);
                player.stopUsingItem();
            }
            return;
        }
        if (!level.isClientSide) {
            type.fire(level, player, player.getLookAngle());
            if (!creative) setRounds(stack, mag - 1);
            player.displayClientMessage(Component.literal("§7" + type.id.toUpperCase() + " §f" + Math.max(0, mag - (creative ? 0 : 1))
                    + "§7/" + type.magazine + "  §8(" + countAmmo(player) + " spare)"), true);
        } else {
            // Recoil: the muzzle climbs, with a little sideways wander.
            player.setXRot(player.getXRot() - type.recoil());
            player.setYRot(player.getYRot() + (player.getRandom().nextFloat() - 0.5f) * type.recoil() * 0.4f);
        }
        player.getCooldowns().addCooldown(this, type.cooldown);
        player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(this));
    }

    /** Loads from inventory ammo. Returns false if there's none. */
    private boolean reload(Level level, Player player, ItemStack stack) {
        if (player.getCooldowns().isOnCooldown(this)) return true;
        int have = rounds(stack);
        int need = type.magazine - have;
        if (need <= 0) return false;
        int got = level.isClientSide ? Math.min(need, countAmmo(player)) : takeAmmo(player, need);
        if (got <= 0) return false;
        if (!level.isClientSide) {
            setRounds(stack, have + got);
            level.playSound(null, player.blockPosition(), ModSounds.GUN_RELOAD, SoundSource.PLAYERS, 1.0f,
                    type == GunType.SHOTGUN ? 0.85f : 1.0f);
            player.displayClientMessage(Component.literal("§eReloading... §7" + (have + got) + "/" + type.magazine), true);
        }
        player.getCooldowns().addCooldown(this, type.reloadTicks);
        player.stopUsingItem();
        return true;
    }

    private int countAmmo(Player player) {
        int n = 0;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).is(type.ammoItem())) n += inv.getItem(i).getCount();
        return n;
    }

    private int takeAmmo(Player player, int want) {
        int got = 0;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize() && got < want; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.is(type.ammoItem())) continue;
            int take = Math.min(want - got, s.getCount());
            s.shrink(take);
            got += take;
        }
        return got;
    }

    /** Don't play the re-equip bob every time the magazine count changes. */
    @Override
    public boolean allowComponentsUpdateAnimation(Player player, InteractionHand hand, ItemStack oldStack, ItemStack newStack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7Magazine: §f" + rounds(stack) + "/" + type.magazine
                + (type.automatic ? " §8· full auto (hold)" : "")));
        tooltip.add(Component.literal("§8Sneak + use to reload. Uses " + (type.ammo == GunType.Ammo.SHELL ? "shotgun shells." : "bullets.")));
    }
}
