package dev.freerun.mobs;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.Comparator;
import java.util.List;

/**
 * Flavus Florbington: a buff, 3-foot-tall, gold-obsessed little guy.
 *
 * - Follows any nearby player holding gold, and grabs gold items lying on the ground.
 * - Right-click him with gold to hand it over.
 * - Once he's collected {@link #GOLD_TO_TAME} gold, he calls the nearest Wingus over, hops on,
 *   and rides it around from then on.
 */
public class FlavusEntity extends PathfinderMob {
    public static final int GOLD_TO_TAME = 5;

    private int goldFed;
    private boolean wantsWingus;
    private int scanTimer;

    public FlavusEntity(EntityType<? extends FlavusEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setCustomName(Component.translatable("entity.freerun.flavus"));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    public static boolean isGold(ItemStack s) {
        return s.is(Items.GOLD_INGOT) || s.is(Items.GOLD_NUGGET) || s.is(Items.RAW_GOLD) || s.is(Items.GOLD_BLOCK);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    // --- handing him gold ---

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isGold(stack)) {
            if (!level().isClientSide) {
                if (!player.getAbilities().instabuild) stack.shrink(1);
                feedGold(1);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    private void feedGold(int amount) {
        if (!(level() instanceof ServerLevel server)) return;
        goldFed += amount;
        server.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 1.0, getZ(), 8, 0.3, 0.3, 0.3, 0.0);
        playSound(SoundEvents.PIGLIN_ADMIRING_ITEM, 1.0f, 1.3f);

        String msg = wantsWingus || isPassenger()
                ? "§6Flavus Florbington pockets the gold. Can never have too much."
                : "§6Flavus Florbington grabs the gold greedily. (" + Math.min(goldFed, GOLD_TO_TAME) + "/" + GOLD_TO_TAME + ")";
        if (!wantsWingus && !isPassenger() && goldFed >= GOLD_TO_TAME) {
            wantsWingus = true;
            msg = "§6Flavus Florbington: \"That's enough gold! Time to find me a Wingus.\"";
        }
        tellNearby(msg);
    }

    private void tellNearby(String msg) {
        for (Player p : level().players()) {
            if (distanceToSqr(p) < 32 * 32) p.displayClientMessage(Component.literal(msg), true);
        }
    }

    // --- per-tick behaviour ---

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (isPassenger()) return;

        // Follow players who are holding gold.
        Player tempter = level().getNearestPlayer(this, 10);
        if (tempter != null && (isGold(tempter.getMainHandItem()) || isGold(tempter.getOffhandItem()))) {
            getLookControl().setLookAt(tempter, 30.0f, 30.0f);
            if (distanceTo(tempter) > 2.0f) getNavigation().moveTo(tempter, 1.15);
            else getNavigation().stop();
        }

        if (--scanTimer <= 0) {
            scanTimer = 10;
            grabNearbyGold();
        }

        if (wantsWingus) callWingus();
    }

    private void grabNearbyGold() {
        List<ItemEntity> items = level().getEntitiesOfClass(ItemEntity.class,
                getBoundingBox().inflate(8), e -> isGold(e.getItem()));
        items.stream().min(Comparator.comparingDouble(this::distanceToSqr)).ifPresent(item -> {
            if (distanceTo(item) < 1.5f) {
                int count = item.getItem().getCount();
                item.discard();
                feedGold(count);
            } else {
                getNavigation().moveTo(item, 1.15);
            }
        });
    }

    private void callWingus() {
        List<WingusEntity> wingi = level().getEntitiesOfClass(WingusEntity.class,
                getBoundingBox().inflate(48), w -> w.isAlive() && w.getPassengers().isEmpty());
        WingusEntity wingus = wingi.stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        if (wingus == null) return;

        wingus.beckon(this);
        getLookControl().setLookAt(wingus, 30.0f, 30.0f);
        if (distanceTo(wingus) < 2.5f) {
            if (startRiding(wingus, true)) {
                wingus.tameBy(this);
                wantsWingus = false;
                tellNearby("§6Flavus Florbington: \"YEEHAW!\" §7(he tamed a Wingus)");
            }
        }
    }

    // --- toughness: he's buff ---

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.is(DamageTypes.IN_WALL) || super.isInvulnerableTo(source);   // Wingus flies through hills
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("GoldFed", goldFed);
        tag.putBoolean("WantsWingus", wantsWingus);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        goldFed = tag.getInt("GoldFed");
        wantsWingus = tag.getBoolean("WantsWingus");
    }
}
