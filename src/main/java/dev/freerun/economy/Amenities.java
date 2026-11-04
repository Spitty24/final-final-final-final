package dev.freerun.economy;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** The modern conveniences sold at the Double Quick and the General Store. */
public final class Amenities {
    private Amenities() {}

    private static void tip(List<Component> tooltip, String... lines) {
        for (String l : lines) tooltip.add(Component.literal("§7" + l));
    }

    // ------------------------------------------------------------------ smartphone

    /** Clock, balance, next break, location, and whatever notification just came in. */
    public static class Smartphone extends Item {
        private static final String[] NOTIFICATIONS = {
                "Mom: call me when you get a chance",
                "Your package has been delivered (to the wrong house)",
                "Supervisor Slater: where are you",
                "Double Quick Rewards: 2-for-$3 roller dogs today only!",
                "Low storage. Delete some of your 4,000 photos of the crane.",
                "Ethan James Patrick sent you a message: \"what am i supposed to do here\"",
                "Weather alert: it's humid. It's always humid.",
                "Bayou Federal Credit Union: direct deposit received",
                "Johnny Sim: who parked in front of my forklift",
                "You have 1 new voicemail from an unknown number (probably your car's warranty)",
                "Screen time is up 400% this week",
                "Co-op group chat (37): did anybody figure out what we're supposed to do"
        };

        public Smartphone(Properties p) {
            super(p);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!level.isClientSide) {
                long day = level.getDayTime() % 24000L;
                int hours = (int) ((day / 1000L + 6L) % 24L);
                int minutes = (int) (day % 1000L * 60L / 1000L);
                String clock = String.format("%d:%02d %s", hours % 12 == 0 ? 12 : hours % 12, minutes, hours < 12 ? "AM" : "PM");
                int until = BreakTime.ticksUntilBreak(level);
                String br = BreakTime.isBreak(level) ? "§aon break now!" : "next break in " + (until / 1200) + "m " + (until % 1200 / 20) + "s";
                player.displayClientMessage(Component.literal("§8┌ §f📱 " + clock + " §8· §a" + Money.fmt(Money.balance(player))
                        + " §8· §7" + br), false);
                player.displayClientMessage(Component.literal("§8│ §7GPS: " + player.blockPosition().getX() + ", "
                        + player.blockPosition().getY() + ", " + player.blockPosition().getZ()), false);
                player.displayClientMessage(Component.literal("§8└ §e🔔 " + NOTIFICATIONS[level.random.nextInt(NOTIFICATIONS.length)]), false);
                level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.5f, 1.8f);
            }
            player.getCooldowns().addCooldown(this, 20);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            tip(tooltip, "Use: time, balance, next break, GPS", "and your latest notification.");
        }
    }

    // ------------------------------------------------------------------ flashlight

    public static class Flashlight extends Item {
        public Flashlight(Properties p) {
            super(p);
        }

        public static boolean isOn(ItemStack stack) {
            CustomData d = stack.get(DataComponents.CUSTOM_DATA);
            return d != null && d.copyTag().getBoolean("On");
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            boolean on = !isOn(stack);
            CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putBoolean("On", on));
            level.playSound(null, player.blockPosition(), SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.PLAYERS, 0.6f, on ? 1.6f : 1.2f);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        @Override
        public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
            if (level.isClientSide || !(entity instanceof LivingEntity living) || !isOn(stack)) return;
            boolean held = selected || living.getOffhandItem() == stack;
            if (held && entity.tickCount % 20 == 0) {
                living.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false, true));
            }
        }

        @Override
        public boolean isFoil(ItemStack stack) {
            return isOn(stack);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            tip(tooltip, isOn(stack) ? "§eOn §7- lights things up while held" : "Off. Use to switch on.");
        }
    }

    // ------------------------------------------------------------------ scratch-off

    public static class ScratchOff extends Item {
        public ScratchOff(Properties p) {
            super(p);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!level.isClientSide) {
                float r = level.random.nextFloat();
                int win = r < 0.001f ? 1000 : r < 0.01f ? 100 : r < 0.05f ? 20 : r < 0.15f ? 5 : r < 0.40f ? 2 : 0;
                level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 1.4f);
                if (win > 0) {
                    Money.give(player, win);
                    player.displayClientMessage(Component.literal(win >= 100
                            ? "§6§l★ LUCKY 7s ★ §r§6You won " + Money.fmt(win) + "!!!"
                            : "§aWinner! " + Money.fmt(win) + "."), false);
                    level.playSound(null, player.blockPosition(), win >= 100 ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE
                            : SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.0f);
                } else {
                    String[] lose = {"Not a winner.", "So close. (It wasn't close.)", "Better luck next time.", "Nope."};
                    player.displayClientMessage(Component.literal("§7" + lose[level.random.nextInt(lose.length)]), true);
                }
                stack.consume(1, player);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            tip(tooltip, "Use to scratch. Top prize $1,000!", "§8Odds of winning anything: about 2 in 5.");
        }
    }

    // ------------------------------------------------------------------ first aid

    public static class Bandage extends Item {
        public Bandage(Properties p) {
            super(p);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            if (player.getHealth() >= player.getMaxHealth()) return InteractionResultHolder.pass(player.getItemInHand(hand));
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(player.getItemInHand(hand));
        }

        @Override
        public UseAnim getUseAnimation(ItemStack stack) {
            return UseAnim.BOW;
        }

        @Override
        public int getUseDuration(ItemStack stack, LivingEntity entity) {
            return 30;
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
            if (!level.isClientSide) {
                entity.heal(6.0f);
                level.playSound(null, entity.blockPosition(), SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1.0f, 1.3f);
                stack.consume(1, entity);
            }
            return stack;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            tip(tooltip, "Hold use to patch yourself up (+3 hearts).");
        }
    }

    public static class Painkiller extends Item {
        public Painkiller(Properties p) {
            super(p);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!level.isClientSide) {
                for (MobEffectInstance e : List.copyOf(player.getActiveEffects())) {
                    if (e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) player.removeEffect(e.getEffect());
                }
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
                player.displayClientMessage(Component.literal("§7*takes two with a sip of water* §aMuch better."), true);
                level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.6f, 1.4f);
                stack.consume(1, player);
            }
            player.getCooldowns().addCooldown(this, 100);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            tip(tooltip, "Clears harmful effects and gives a little regeneration.");
        }
    }

    // ------------------------------------------------------------------ sunglasses

    public static class Sunglasses extends Item implements Equipable {
        public Sunglasses(Properties p) {
            super(p);
        }

        @Override
        public EquipmentSlot getEquipmentSlot() {
            return EquipmentSlot.HEAD;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            return swapWithEquipmentSlot(this, level, player, hand);
        }

        @Override
        public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
            if (level.isClientSide || !(entity instanceof LivingEntity living)) return;
            if (living.getItemBySlot(EquipmentSlot.HEAD) != stack) return;
            if (living.hasEffect(MobEffects.BLINDNESS)) living.removeEffect(MobEffects.BLINDNESS);
            if (living.hasEffect(MobEffects.DARKNESS)) living.removeEffect(MobEffects.DARKNESS);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            tip(tooltip, "Wear on your head. Blindness and Darkness", "can't touch you. Neither can anyone, really.");
        }
    }

    // ------------------------------------------------------------------ umbrella

    public static class Umbrella extends Item {
        public Umbrella(Properties p) {
            super(p);
        }

        @Override
        public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
            if (level.isClientSide || !(entity instanceof LivingEntity living)) return;
            boolean held = selected || living.getOffhandItem() == stack;
            if (held && !living.onGround() && living.getDeltaMovement().y < -0.2) {
                living.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 10, 0, true, false, false));
            }
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            tip(tooltip, "Hold it while falling to float down gently.");
        }
    }

    // ------------------------------------------------------------------ air horn

    public static class AirHorn extends Item {
        public AirHorn(Properties p) {
            super(p);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level instanceof ServerLevel server) {
                server.playSound(null, player.blockPosition(), ModSounds.CAR_HORN, SoundSource.PLAYERS, 3.0f, 1.25f);
                server.playSound(null, player.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.PLAYERS, 1.5f, 1.6f);
                // Everything nearby jumps out of its skin and backs off.
                for (Mob mob : server.getEntitiesOfClass(Mob.class, new AABB(player.blockPosition()).inflate(10.0), m -> m.isAlive())) {
                    double dx = mob.getX() - player.getX();
                    double dz = mob.getZ() - player.getZ();
                    double d = Math.max(0.5, Math.sqrt(dx * dx + dz * dz));
                    mob.push(dx / d * 0.8, 0.35, dz / d * 0.8);
                }
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            }
            player.getCooldowns().addCooldown(this, 40);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            tip(tooltip, "Extremely loud. Startles everything nearby away from you.");
        }
    }

    // ------------------------------------------------------------------ duct tape

    public static class DuctTape extends Item {
        public DuctTape(Properties p) {
            super(p);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack tape = player.getItemInHand(hand);
            ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            if (!other.isDamaged()) {
                if (!level.isClientSide) player.displayClientMessage(Component.literal(
                        "§7Hold something damaged in your other hand to tape it up."), true);
                return InteractionResultHolder.fail(tape);
            }
            if (!level.isClientSide) {
                int fix = Math.max(1, other.getMaxDamage() / 4);
                other.setDamageValue(Math.max(0, other.getDamageValue() - fix));
                tape.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
                level.playSound(null, player.blockPosition(), SoundEvents.WOOL_BREAK, SoundSource.PLAYERS, 1.0f, 0.7f);
                player.displayClientMessage(Component.literal("§aGood as new. §7(Not really. But close enough.)"), true);
            }
            return InteractionResultHolder.sidedSuccess(tape, level.isClientSide);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            tip(tooltip, "Repairs a quarter of whatever's damaged", "in your other hand.");
        }
    }

    // ------------------------------------------------------------------ gas can

    public static class GasCan extends Item {
        public GasCan(Properties p) {
            super(p);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            tip(tooltip, "Right-click a car to add 40% fuel.");
        }
    }
}
