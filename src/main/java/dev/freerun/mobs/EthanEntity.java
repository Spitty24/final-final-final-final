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

    /** One in ten things he says is, somehow, the smartest thing anyone in the building has ever said. */
    private static final String[] GENIUS = {
            "Press brakes overbend a little on purpose: steel springs back once the ram lets go, so you aim past 90 to land on 90.",
            "MIG welding feeds a wire electrode through the torch and shields the puddle with gas, usually 75% argon and 25% CO2, so oxygen can't get in.",
            "Steel is just iron with under about two percent carbon. More carbon makes it harder, but more brittle.",
            "A forklift's rated capacity assumes a load center, usually 24 inches. Carry it further out on the forks and the real capacity drops.",
            "The crane's hook block runs the rope through several falls, so each line only carries part of the load. Mechanical advantage.",
            "Coffee beans aren't beans. They're the seeds of a fruit called a coffee cherry.",
            "Boudin is pork, rice, onions, peppers and spices stuffed in a casing. Ours uses wheat for the rice, which is technically a crime.",
            "Never microwave metal: the field piles up charge on sharp edges until it arcs. That's the sparks.",
            "The second law of thermodynamics says entropy never goes down in a closed system. That's why this floor is never clean.",
            "A conveyor's real throughput is belt speed times how closely you can space the parts. Gaps are lost money."
    };

    private BlockPos home;
    private int climbCooldown = 1200 + 600;
    private int craneTicks;
    private dev.freerun.factory.CraneEntity riding;
    private int rageTicks;
    private int shotCooldown;
    private net.minecraft.world.entity.LivingEntity rageTarget;
    private int homeRadius = 13;
    private int askCooldown = 100;
    private int lastQuestion = -1;

    public EthanEntity(EntityType<? extends EthanEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setCustomName(Component.literal("Ethan James Patrick"));
    }

    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> DATA_HANGING =
            net.minecraft.network.syncher.SynchedEntityData.defineId(EthanEntity.class,
                    net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HANGING, false);
    }

    /** Hanging off the overhead crane (the model raises his arms). */
    public boolean isHanging() {
        return entityData.get(DATA_HANGING);
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
        boolean genius = random.nextInt(10) == 0;
        Component msg = Component.literal(genius
                ? "§b<Ethan James Patrick> §d§o*suddenly, perfectly lucid* §r§f" + GENIUS[random.nextInt(GENIUS.length)]
                : "§b<Ethan James Patrick> §f" + QUESTIONS[i]);
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

    private void broadcast(String line) {
        Component msg = Component.literal("§b<Ethan James Patrick> §f" + line);
        for (Player p : level().players()) if (distanceToSqr(p) < 24 * 24) p.displayClientMessage(msg, false);
    }

    // ------------------------------------------------------------------ the crane

    /** Every few minutes he leaps up onto the overhead crane's bridge and rides it for a while. */
    private boolean tickCrane() {
        if (riding != null) {
            if (!riding.isAlive() || --craneTicks <= 0 || rageTicks > 0) {
                riding = null;
                setNoGravity(false);
                entityData.set(DATA_HANGING, false);
                setDeltaMovement(0.0, 0.3, 0.0);
                broadcast(rageTicks > 0 ? "Okay, coming down. COMING DOWN." : "Alright. I've seen enough. Coming down.");
                return false;
            }
            // Stand on top of the girder and ride it wherever the crane goes.
            double x = net.minecraft.util.Mth.clamp(getX(), riding.beamMin() + 1.0, riding.beamMax() - 1.0);
            setPos(x, riding.getY() - 3.6, riding.getZ());   // hanging from the girder by his hands
            setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            getNavigation().stop();
            fallDistance = 0;
            return true;
        }
        if (--climbCooldown > 0 || rageTicks > 0) return false;
        climbCooldown = 3600 + random.nextInt(3600);   // every 3 to 6 minutes
        dev.freerun.factory.CraneEntity crane = level().getEntitiesOfClass(dev.freerun.factory.CraneEntity.class,
                getBoundingBox().inflate(32.0, 12.0, 32.0)).stream().findFirst().orElse(null);
        if (crane == null || !(level() instanceof net.minecraft.server.level.ServerLevel server)) return false;
        server.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, getX(), getY(), getZ(), 12, 0.3, 0.1, 0.3, 0.05);
        playSound(SoundEvents.GOAT_LONG_JUMP, 1.0f, 0.8f);
        riding = crane;
        craneTicks = 600 + random.nextInt(600);
        setNoGravity(true);
        entityData.set(DATA_HANGING, true);
        broadcast("*jumps straight up and grabs the crane* Whoa. You can see EVERYTHING from up here.");
        return true;
    }

    // ------------------------------------------------------------------ the plasma cannon

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && isAlive() && source.getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker
                && attacker != this && !(attacker instanceof Player p && p.isCreative())) {
            if (rageTicks <= 0) {
                broadcast("§cWHY WOULD YOU DO THAT?! §f*pulls out a plasma cannon*");
                setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new net.minecraft.world.item.ItemStack(MoreMobs.PLASMA_CANNON));
                setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 1.0f);
                shotCooldown = 10;
            }
            rageTarget = attacker;
            rageTicks = 300;   // 15 seconds of plasma
        }
        return hurt;
    }

    /** While enraged he shoots whoever hurt him, and any factory workers who happen to be nearby. */
    private void tickRage() {
        if (rageTicks <= 0) return;
        if (--rageTicks <= 0) {
            setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, net.minecraft.world.item.ItemStack.EMPTY);
            broadcast("...Sorry. What was I supposed to be doing?");
            rageTarget = null;
            return;
        }
        if (--shotCooldown > 0) return;
        shotCooldown = 16 + random.nextInt(10);
        net.minecraft.world.entity.LivingEntity target = rageTarget;
        boolean targetOk = target != null && target.isAlive() && distanceToSqr(target) < 28 * 28
                && !(target instanceof Player p && (p.isCreative() || p.isSpectator()));
        if (!targetOk || random.nextInt(3) == 0) {
            java.util.List<net.minecraft.world.entity.LivingEntity> workers = level().getEntitiesOfClass(
                    net.minecraft.world.entity.LivingEntity.class, getBoundingBox().inflate(16.0),
                    e -> e.isAlive() && (e instanceof RoadhouseNpcEntity || e instanceof dev.freerun.factory.ForkliftDriverEntity));
            if (!workers.isEmpty()) target = workers.get(random.nextInt(workers.size()));
            else if (!targetOk) target = null;
        }
        if (target == null) return;
        getLookControl().setLookAt(target, 60.0f, 60.0f);
        getNavigation().stop();
        net.minecraft.world.phys.Vec3 eye = getEyePosition();
        net.minecraft.world.phys.Vec3 aim = target.getBoundingBox().getCenter().subtract(eye)
                .add(random.nextGaussian() * 0.4, random.nextGaussian() * 0.3, random.nextGaussian() * 0.4);
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        dev.freerun.item.PlasmaCannonItem.fire(level(), this, eye.add(aim.normalize().scale(0.8)), aim, 6.0f);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (home != null && !hasRestriction()) restrictTo(home, homeRadius);
        tickRage();
        if (tickCrane()) return;
        if (rageTicks > 0) return;
        if (--askCooldown > 0) return;
        Player near = level().getNearestPlayer(this, 7.0);
        if (near != null) ask(near);
        else askCooldown = 60;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
        return false;   // long legs
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
