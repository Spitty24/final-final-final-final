package dev.freerun.mobs;

import dev.freerun.economy.BreakTrip;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * The three co-op students interning at the factory this semester. Lanyards, safety vests, hard hats
 * a size too big. They wander the floor in a loose clump, follow Ethan around because he seems like
 * he knows things, and ask everybody questions.
 */
public class CoopEntity extends PathfinderMob {
    public static final String[] NAMES = {"Co-op Tyler", "Co-op Brayden", "Co-op Kaitlyn"};

    private static final String[] QUESTIONS = {
            "Is this going to be on the end-of-term report?",
            "Do we get paid for orientation?",
            "My advisor said I'd be doing CAD. Is the CAD in here somewhere?",
            "Where do I turn in my timesheet?",
            "Is it okay if I just watch?",
            "Should I be taking notes on this?",
            "Is Ethan our mentor? He said he didn't know.",
            "What does a press brake actually brake?",
            "Can I drive the forklift? I have my learner's permit.",
            "Are we allowed to use the break room or is that just for real employees?",
            "Do you have any tips for my exit presentation?",
            "Is the parking lot always like that?",
            "What's the difference between a supervisor and a superintendent?",
            "How many sheets make a plate? Wait, is it the other way?",
            "Is it normal that I haven't been given anything to do yet?",
            "Can I put 'operated overhead crane' on my résumé if I just watched?",
            "Does anybody know where the bathroom is? I've been here three weeks."
    };

    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(CoopEntity.class, EntityDataSerializers.INT);

    private final BreakTrip breakTrip = new BreakTrip();
    private BlockPos home;
    private int homeRadius = 14;
    private int askCooldown = 200;
    private int followTicks;

    public CoopEntity(EntityType<? extends CoopEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 16.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIANT, 0);
    }

    public int variant() {
        return entityData.get(DATA_VARIANT);
    }

    public void setVariant(int v) {
        int i = Mth.clamp(v, 0, NAMES.length - 1);
        entityData.set(DATA_VARIANT, i);
        setCustomName(Component.literal(NAMES[i]));
    }

    public void setHome(BlockPos pos, int radius) {
        home = pos.immutable();
        homeRadius = radius;
        restrictTo(home, radius);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.9, 0.03f));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        SpawnGroupData spawnData) {
        SpawnGroupData out = super.finalizeSpawn(level, difficulty, reason, spawnData);
        setVariant(random.nextInt(NAMES.length));
        setHome(blockPosition(), 16);
        return out;
    }

    private void ask(Player player) {
        Component msg = Component.literal("§a<" + NAMES[variant()] + "> §f" + QUESTIONS[random.nextInt(QUESTIONS.length)]);
        for (Player p : level().players()) if (p == player || distanceToSqr(p) < 10 * 10) p.displayClientMessage(msg, false);
        playSound(SoundEvents.VILLAGER_AMBIENT, 0.8f, 1.35f);
        getLookControl().setLookAt(player, 30.0f, 30.0f);
        askCooldown = 600 + random.nextInt(600);
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
        if (breakTrip.tick(this)) {
            clearRestriction();   // off to the Double Quick
            return;
        }
        if (home != null && !hasRestriction()) restrictTo(home, homeRadius);

        // Every so often, trail after Ethan for a while.
        if (followTicks > 0) {
            followTicks--;
            EthanEntity ethan = level().getEntitiesOfClass(EthanEntity.class, getBoundingBox().inflate(24.0)).stream()
                    .findFirst().orElse(null);
            if (ethan != null && !ethan.isHanging() && distanceToSqr(ethan) > 9.0 && tickCount % 20 == 0) {
                getNavigation().moveTo(ethan, 1.0);
            }
        } else if (random.nextInt(2400) == 0) {
            followTicks = 400 + random.nextInt(400);
        }

        if (--askCooldown > 0) return;
        Player near = level().getNearestPlayer(this, 6.0);
        if (near != null) ask(near);
        else askCooldown = 80;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", variant());
        if (home != null) {
            tag.putInt("HomeX", home.getX());
            tag.putInt("HomeY", home.getY());
            tag.putInt("HomeZ", home.getZ());
        }
        breakTrip.save(tag, registryAccess());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setVariant(tag.getInt("Variant"));
        if (tag.contains("HomeX")) setHome(new BlockPos(tag.getInt("HomeX"), tag.getInt("HomeY"), tag.getInt("HomeZ")), 14);
        breakTrip.load(tag, registryAccess());
    }
}
