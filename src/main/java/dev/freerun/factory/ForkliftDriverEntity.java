package dev.freerun.factory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * The factory's forklift drivers: big, cheerful Cajun fellas who never stop talking, in a thick
 * bayou patter that nobody on the floor can actually follow. They drive their forklift's route on
 * their own, honk at anyone in the way, and climb back aboard an empty route forklift if they're
 * ever knocked off.
 */
public class ForkliftDriverEntity extends PathfinderMob {
    private static final String[] NAMES = {"Boudreaux", "Thibodeaux", "Arceneaux", "Guidry", "Fontenot"};

    private static final String[] SYLLABLES = {
            "ba", "boo", "dee", "doo", "gah", "la", "mah", "nuh", "shoo", "zah", "bree", "tee", "yah",
            "hoo", "mm", "gum", "bo", "sha", "wee", "ree", "bay", "nonc", "rah", "fee", "too", "dah"
    };
    private static final String[] CAJUN = {
            "mais yeah", "cher", "I garontee", "ça c'est bon", "laissez les bons temps rouler",
            "hoo-wee", "mais la", "ayee", "mon ami", "bon appétit", "comment ça va", "allons"
    };
    private static final String[] ENDINGS = {"!", "!!", "?", "...", ", yeah?", ", mais oui!"};

    private static final EntityDataAccessor<Integer> DATA_NAME =
            SynchedEntityData.defineId(ForkliftDriverEntity.class, EntityDataSerializers.INT);

    private int chatterCooldown;
    private int remountScan;

    public ForkliftDriverEntity(EntityType<? extends ForkliftDriverEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.18);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_NAME, 0);
    }

    public void setNameIndex(int i) {
        entityData.set(DATA_NAME, Math.floorMod(i, NAMES.length));
        setCustomName(Component.literal(NAMES[Math.floorMod(i, NAMES.length)] + " §7(Forklift)"));
    }

    public String driverName() {
        return NAMES[Math.floorMod(entityData.get(DATA_NAME), NAMES.length)];
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType reason, SpawnGroupData spawnData) {
        SpawnGroupData out = super.finalizeSpawn(level, difficulty, reason, spawnData);
        setNameIndex(random.nextInt(NAMES.length));
        return out;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 10.0f));
        goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    /** A line of his patter: mostly gibberish, with some real Cajun French mixed in. */
    public static String gibberish(RandomSource r) {
        StringBuilder sb = new StringBuilder();
        int words = 3 + r.nextInt(5);
        for (int w = 0; w < words; w++) {
            if (w > 0) sb.append(' ');
            if (r.nextInt(4) == 0) {
                sb.append(CAJUN[r.nextInt(CAJUN.length)]);
                continue;
            }
            int syl = 1 + r.nextInt(3);
            for (int i = 0; i < syl; i++) {
                if (i > 0 && r.nextInt(3) == 0) sb.append('-');
                sb.append(SYLLABLES[r.nextInt(SYLLABLES.length)]);
            }
        }
        sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
        sb.append(ENDINGS[r.nextInt(ENDINGS.length)]);
        return sb.toString();
    }

    /** What he says next. Most drivers speak pure bayou gibberish. */
    protected String line() {
        return gibberish(random);
    }

    protected String speaker() {
        return driverName();
    }

    protected boolean speaksGibberish() {
        return true;
    }

    /** Somebody handed him boudin (after he's eaten it). */
    protected void fedBoudin(Player player) {
    }

    protected void say(Player to, String line) {
        Component msg = Component.literal("§6<" + speaker() + "> §f" + line);
        if (to != null) {
            to.displayClientMessage(msg, false);
        } else {
            for (Player p : level().players()) {
                if (distanceToSqr(p) < 14 * 14) p.displayClientMessage(msg, false);
            }
        }
        playSound(SoundEvents.VILLAGER_AMBIENT, 1.0f, 0.65f + random.nextFloat() * 0.15f);
    }

    /** Right-clicked (directly, or via his forklift). Hand him boudin and he lights right up. */
    public void chatter(Player player) {
        getLookControl().setLookAt(player, 30.0f, 30.0f);
        net.minecraft.world.item.ItemStack held = player.getMainHandItem();
        if (held.is(dev.freerun.food.FoodRegistry.BOUDIN)) {
            held.consume(1, player);
            say(player, speaksGibberish() ? "BOUDIN?! Mais cher, ça c'est bon! " + gibberish(random)
                    : "Boudin?! Oh cher, you didn't have to do that. Now I'm all warm inside.");
            if (level() instanceof net.minecraft.server.level.ServerLevel server) {
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART,
                        getX(), getY() + 2.1, getZ(), 6, 0.4, 0.3, 0.4, 0.0);
            }
            playSound(SoundEvents.GENERIC_EAT, 1.0f, 0.8f);
            chatterCooldown = 200;
            fedBoudin(player);
            return;
        }
        say(player, line());
        if (speaksGibberish() && random.nextInt(3) == 0) {
            player.displayClientMessage(Component.literal("§7§o(You didn't catch a single word of that.)"), false);
        }
        chatterCooldown = 200;
    }

    /** Somebody is standing in front of his forklift. */
    public void yellAtObstacle() {
        if (random.nextInt(2) == 0) say(null, "AYEE! " + line());
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!level().isClientSide) chatter(player);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        // Talks to anybody who comes near, whether they asked or not.
        if (chatterCooldown > 0) {
            chatterCooldown--;
        } else if (random.nextInt(140) == 0) {
            Player near = level().getNearestPlayer(this, 9.0);
            if (near != null) {
                say(null, line());
                chatterCooldown = 240 + random.nextInt(200);
            }
        }
        // Knocked off his forklift? Climb back on the nearest empty route forklift.
        if (!isPassenger() && --remountScan <= 0) {
            remountScan = 40;
            level().getEntitiesOfClass(ForkliftEntity.class, getBoundingBox().inflate(6.0),
                            f -> f.hasRoute() && f.getPassengers().isEmpty()).stream()
                    .findFirst().ifPresent(f -> startRiding(f, true));
        }
    }

    @Override
    public boolean isPushable() {
        return !isPassenger();
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("DriverName", entityData.get(DATA_NAME));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setNameIndex(Mth.clamp(tag.getInt("DriverName"), 0, NAMES.length - 1));
    }
}
