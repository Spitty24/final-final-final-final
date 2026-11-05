package dev.freerun.laser;

import dev.freerun.economy.EconomyRegistry;
import dev.freerun.economy.Money;
import dev.freerun.item.ItemRegistry;
import dev.freerun.item.gun.GunType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Dr. Guild, the plant's occupational-health doctor. Fleece over scrubs, a stethoscope, a can of
 * diet cola that never runs out, and a pistol he says is "for liability reasons". He's very good at
 * medicine and very bad at people. Right-click him when you're hurt and he patches you up for a $40
 * copay (and makes sure you feel stupid about it). Hit him and he shoots back.
 */
public class DrGuildEntity extends PathfinderMob implements RangedAttackMob {
    public static final String NAME = "Dr. Guild";
    public static final int COPAY = 40;

    static final String[] HEALTHY = {
            "You're not sick. You're just like this.",
            "I went to medical school for eleven years so I could be interrupted by you. Worth it.",
            "Is this an appointment, or did you wander in again?",
            "Your vitals are fine. Your decisions are the concern.",
            "I'd tell you to get a second opinion, but you'd just come back here.",
            "Unless you're bleeding, I'm on lunch. I'm always on lunch.",
            "WebMD is not a colleague. Stop quoting it to me.",
            "I see you've diagnosed yourself again. Bold.",
            "There's no cure for what you've got. It's called personality.",
            "Wash your hands. No, longer. No. Longer. Ugh. Fine.",
            "You know they make you sign a waiver to talk to me, right? You signed it. Didn't read it, did you.",
            "Diet cola is a food group. I'm a doctor, so that's medical advice."
    };

    static final String[] HEALED = {
            "There. Try not to fall off anything for at least an hour. I know. It'll be hard.",
            "Patched. That's a $40 copay, and the lecture is free.",
            "You're fine. You were basically fine. You just like attention.",
            "All better. If you come back with the same thing, I'm charging double.",
            "Done. I've seen co-ops with better survival instincts.",
            "Stitched up. Next time, wear the PPE. It's literally on the wall.",
            "Healed. Don't thank me. Seriously, don't, it's weird."
    };

    static final String[] BROKE = {
            "No copay, no care. Walk it off.",
            "You can't afford me. Few can.",
            "Insurance doesn't cover 'being an idiot'. Come back with $40.",
            "Have you tried rubbing some dirt on it? It's free. Like your opinions."
    };

    static final String[] AMBIENT = {
            "*sips diet cola*",
            "Who left a boudin wrapper in my exam room. WHO.",
            "If one more person asks me to look at a rash before 9 AM...",
            "Ethan asked me if he has a pulse. I said I'd check later. I won't.",
            "Taryn says she looked at the laser with her 'good eye'. I've started a file.",
            "Dustin booked a 'wellness check' for himself. I'm not going.",
            "The vending machine's out of diet cola. This is a medical emergency.",
            "Safety glasses are not optional. Neither is my contempt."
    };

    static final String[] ANGRY = {
            "Oh, you want to do this? I took an oath. It had loopholes.",
            "First, do no harm. Second, return fire.",
            "That's going in your chart.",
            "You just made a very expensive mistake."
    };

    private BlockPos home;
    private int talkCooldown = 600;
    private int calmTicks;

    public DrGuildEntity(EntityType<? extends DrGuildEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setCustomName(Component.literal(NAME));
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ItemRegistry.gunItem(GunType.PISTOL)));
        setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(EconomyRegistry.FIZZ_COLA));
        setDropChance(EquipmentSlot.MAINHAND, 0.0f);
        setDropChance(EquipmentSlot.OFFHAND, 0.0f);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    public void setHome(BlockPos pos) {
        home = pos.immutable();
        restrictTo(home, 5);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new RangedAttackGoal(this, 1.0, 16, 28, 16.0f));
        goalSelector.addGoal(3, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.6, 0.01f));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    private void say(String text) {
        Component msg = Component.literal("§b<" + NAME + "> §f" + text);
        for (Player p : level().players()) if (distanceToSqr(p) < 12 * 12) p.displayClientMessage(msg, false);
    }

    private String pick(String[] lines) {
        return lines[random.nextInt(lines.length)];
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level().isClientSide) return InteractionResult.SUCCESS;
        getLookControl().setLookAt(player, 30.0f, 30.0f);
        talkCooldown = 600 + random.nextInt(600);
        if (getTarget() == player) {
            say("Oh, NOW you want a check-up?");
            return InteractionResult.CONSUME;
        }
        boolean hurt = player.getHealth() < player.getMaxHealth() - 0.5f || !player.getActiveEffects().isEmpty() && hasHarmful(player);
        if (!hurt) {
            say(pick(HEALTHY));
            return InteractionResult.CONSUME;
        }
        if (!player.getAbilities().instabuild && !Money.pay(player, COPAY)) {
            say(pick(BROKE));
            return InteractionResult.CONSUME;
        }
        player.setHealth(player.getMaxHealth());
        for (var e : java.util.List.copyOf(player.getActiveEffects())) {
            if (e.getEffect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL) player.removeEffect(e.getEffect());
        }
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HEART, player.getX(), player.getEyeY() + 0.3, player.getZ(), 4, 0.3, 0.2, 0.3, 0.0);
        }
        playSound(SoundEvents.PLAYER_LEVELUP, 0.5f, 1.6f);
        say(pick(HEALED));
        return InteractionResult.CONSUME;
    }

    private static boolean hasHarmful(Player p) {
        for (var e : p.getActiveEffects()) {
            if (e.getEffect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL) return true;
        }
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean wasCalm = getTarget() == null;
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && source.getEntity() instanceof LivingEntity attacker) {
            if (wasCalm) say(pick(ANGRY));
            setTarget(attacker);
            calmTicks = 400;
        }
        return hurt;
    }

    @Override
    public void performRangedAttack(LivingEntity target, float velocityFactor) {
        if (!(level() instanceof ServerLevel)) return;
        // annoyingly, he's a good shot
        double dx = target.getX() - getX() + random.nextGaussian() * 0.25;
        double dy = target.getEyeY() - 0.3 - (getEyeY() - 0.1) + random.nextGaussian() * 0.2;
        double dz = target.getZ() - getZ() + random.nextGaussian() * 0.25;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        GunType.PISTOL.fire(level(), this, new Vec3(dx, dy + horiz * 0.01, dz));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (home != null && !hasRestriction()) restrictTo(home, 5);
        if (getTarget() != null) {
            if (--calmTicks <= 0 || !getTarget().isAlive() || distanceToSqr(getTarget()) > 30 * 30) {
                setTarget(null);
                say("Fine. I'm billing you for the bullets.");
                heal(getMaxHealth());
            }
            return;
        }
        if (--talkCooldown > 0) return;
        Player near = level().getNearestPlayer(this, 7.0);
        if (near != null) {
            getLookControl().setLookAt(near, 30.0f, 30.0f);
            say(pick(random.nextInt(3) == 0 ? HEALTHY : AMBIENT));
            talkCooldown = 900 + random.nextInt(900);
        } else {
            talkCooldown = 120;
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (home != null) tag.putLong("Home", home.asLong());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Home")) setHome(BlockPos.of(tag.getLong("Home")));
    }
}
