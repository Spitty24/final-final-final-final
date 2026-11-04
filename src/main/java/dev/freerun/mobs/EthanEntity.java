package dev.freerun.mobs;

import dev.freerun.food.FoodRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
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
 * Ethan James Patrick: as tall and skinny as an enderman (but nowhere near as quick). He wanders
 * the factory floor and asks whoever's nearby whatever's on his mind.
 */
public class EthanEntity extends PathfinderMob {
    private static final String[] QUESTIONS = {
            "What am I supposed to do here?",
            "Is this my shift or yours?",
            "Who decides which way the conveyor goes?",
            "If you weld two welders together, is that a weldest?",
            "Does the forklift know where it's going, or just the guy driving it?",
            "Why's it called a press brake if it never stops anything?",
            "What's the crane looking at up there?",
            "Am I on break right now? How would I know?",
            "Wait, does a sheet make two plates, or do two plates make a sheet?",
            "Where does all the steel go after it goes in the barrel?",
            "Do the forklift guys understand each other?",
            "If the factory makes the parts, who made the factory?",
            "Is Slater coming in today?",
            "What happens if I press the big red button on the crane remote?",
            "Do you ever hear the conveyor when you close your eyes?",
            "How do the parking lot guys afford all those cars?",
            "Is boudin a sausage or a lifestyle?",
            "Why do they put the welding curtains in red specifically?",
            "If I stand under the crane long enough, will it pick me up?",
            "Do you think the rocks around here can hear us?",
            "Who's in charge of the person who's in charge?",
            "Should I be wearing a hard hat? Do they make them in my size?"
    };

    private BlockPos home;
    private int homeRadius = 13;
    private int askCooldown = 100;
    private int lastQuestion = -1;

    public EthanEntity(EntityType<? extends EthanEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setCustomName(Component.literal("Ethan James Patrick"));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.18)       // an enderman is 0.3
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0, 0.02f));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    /** Keeps him wandering around one building. */
    public void setHome(BlockPos pos, int radius) {
        home = pos.immutable();
        homeRadius = radius;
        restrictTo(home, radius);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType reason, SpawnGroupData spawnData) {
        SpawnGroupData out = super.finalizeSpawn(level, difficulty, reason, spawnData);
        setHome(blockPosition(), 16);
        return out;
    }

    private void ask(Player player) {
        int i;
        do {
            i = random.nextInt(QUESTIONS.length);
        } while (i == lastQuestion && QUESTIONS.length > 1);
        lastQuestion = i;
        Component msg = Component.literal("§b<Ethan James Patrick> §f" + QUESTIONS[i]);
        for (Player p : level().players()) {
            if (p == player || distanceToSqr(p) < 10 * 10) p.displayClientMessage(msg, false);
        }
        playSound(SoundEvents.VILLAGER_TRADE, 1.0f, 0.75f);
        getLookControl().setLookAt(player, 30.0f, 30.0f);
        askCooldown = 500 + random.nextInt(500);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!level().isClientSide) {
            if (player.getItemInHand(hand).is(FoodRegistry.BOUDIN)) {
                player.displayClientMessage(Component.literal(
                        "§b<Ethan James Patrick> §fOh, for me? ...Am I allowed to eat on the floor?"), false);
                askCooldown = 400;
            } else {
                ask(player);
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (home != null && !hasRestriction()) restrictTo(home, homeRadius);
        if (--askCooldown > 0) return;
        Player near = level().getNearestPlayer(this, 7.0);
        if (near != null) ask(near);
        else askCooldown = 60;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (home != null) {
            tag.putInt("HomeX", home.getX());
            tag.putInt("HomeY", home.getY());
            tag.putInt("HomeZ", home.getZ());
            tag.putInt("HomeR", homeRadius);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("HomeX")) setHome(new BlockPos(tag.getInt("HomeX"), tag.getInt("HomeY"), tag.getInt("HomeZ")),
                tag.contains("HomeR") ? tag.getInt("HomeR") : 13);
    }
}
