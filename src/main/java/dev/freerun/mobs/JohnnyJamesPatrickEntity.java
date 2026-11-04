package dev.freerun.mobs;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Johnny James Patrick: what you get when Ethan James Patrick and Johnny Sim both really like the
 * same boudin. Tall and gangly like Ethan, balding like Johnny, dressed in half of each of their
 * outfits, and he talks in a mix of the two: Ethan's questions in Johnny's voice. Born small, he
 * grows up over 20 minutes.
 */
public class JohnnyJamesPatrickEntity extends PathfinderMob {
    private static final int GROW_TICKS = 24000;
    private static final net.minecraft.resources.ResourceLocation BABY = dev.freerun.Freerun.id("jjp_baby");
    private static final String[] ENDINGS = {", cher?", ", I guarantee.", "? Mais, who knows.", ", no?", "? Lord have mercy.",
            ", you hear me?", "... pass me a boudin and I'll think about it."};
    private static final String[] BABY_LINES = {"*gurgles* ...forklift?", "Wha' am I s'posed to do here, cher?",
            "Boudin!", "*tries to climb your leg like it's a crane*", "Mais... what?"};

    private int age;
    private BlockPos home;
    private int askCooldown = 200;

    public JohnnyJamesPatrickEntity(EntityType<? extends JohnnyJamesPatrickEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setCustomName(Component.literal("Johnny James Patrick"));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.SCALE, 0.85)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    public boolean isYoung() {
        return age < GROW_TICKS;
    }

    /** Fresh out of... wherever he came from. */
    public void makeBaby(BlockPos home) {
        age = 0;
        this.home = home.immutable();
        restrictTo(this.home, 14);
        applyScale();
    }

    private void applyScale() {
        AttributeInstance scale = getAttribute(Attributes.SCALE);
        if (scale == null) return;
        scale.removeModifier(BABY);
        if (isYoung()) scale.addPermanentModifier(new AttributeModifier(BABY, -0.45, AttributeModifier.Operation.ADD_VALUE));
    }

    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level,
            net.minecraft.world.DifficultyInstance difficulty, net.minecraft.world.entity.MobSpawnType reason,
            net.minecraft.world.entity.SpawnGroupData data) {
        net.minecraft.world.entity.SpawnGroupData out = super.finalizeSpawn(level, difficulty, reason, data);
        if (reason != net.minecraft.world.entity.MobSpawnType.BREEDING) {
            age = GROW_TICKS;   // spawn eggs give you the grown-up
            home = blockPosition();
            applyScale();
        }
        return out;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0, 0.02f));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    private String nextLine() {
        if (isYoung() && random.nextInt(2) == 0) return BABY_LINES[random.nextInt(BABY_LINES.length)];
        if (random.nextInt(10) == 0) {
            return "§d§o*suddenly, perfectly lucid* §r§f" + EthanEntity.GENIUS[random.nextInt(EthanEntity.GENIUS.length)]
                    + " Mais, I learned that from my daddy.";
        }
        if (random.nextInt(3) == 0) return JohnnySimEntity.LINES[random.nextInt(JohnnySimEntity.LINES.length)];
        String q = EthanEntity.QUESTIONS[random.nextInt(EthanEntity.QUESTIONS.length)];
        if (q.endsWith("?")) q = q.substring(0, q.length() - 1);
        return q + ENDINGS[random.nextInt(ENDINGS.length)];
    }

    private void ask(Player player) {
        Component msg = Component.literal("§e<Johnny James Patrick> §f" + nextLine());
        for (Player p : level().players()) if (p == player || distanceToSqr(p) < 10 * 10) p.displayClientMessage(msg, false);
        playSound(SoundEvents.VILLAGER_TRADE, 1.0f, isYoung() ? 1.4f : 0.85f);
        getLookControl().setLookAt(player, 30.0f, 30.0f);
        askCooldown = 500 + random.nextInt(500);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!level().isClientSide) ask(player);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (home != null && !hasRestriction()) restrictTo(home, 14);
        if (isYoung() && ++age >= GROW_TICKS) {
            applyScale();
            Component msg = Component.literal("§eJohnny James Patrick is all grown up. §7He's already asking what he's supposed to do here.");
            for (Player p : level().players()) if (distanceToSqr(p) < 48 * 48) p.displayClientMessage(msg, false);
        }
        if (--askCooldown > 0) return;
        Player near = level().getNearestPlayer(this, 7.0);
        if (near != null) ask(near);
        else askCooldown = 60;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("GrowAge", age);
        if (home != null) {
            tag.putInt("HomeX", home.getX());
            tag.putInt("HomeY", home.getY());
            tag.putInt("HomeZ", home.getZ());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        age = tag.contains("GrowAge") ? tag.getInt("GrowAge") : GROW_TICKS;
        if (tag.contains("HomeX")) home = new BlockPos(tag.getInt("HomeX"), tag.getInt("HomeY"), tag.getInt("HomeZ"));
        applyScale();
    }
}
