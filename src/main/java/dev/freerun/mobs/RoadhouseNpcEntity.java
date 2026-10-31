package dev.freerun.mobs;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Restaurant staff and customers. They stay at their spot (no wandering), look at nearby players,
 * and respond when right-clicked:
 *  - HOST greets you.
 *  - WAITER brings a steak and rolls (30s cooldown each).
 *  - COOK hands over a baked potato (30s cooldown each).
 *  - CUSTOMER makes small talk.
 */
public class RoadhouseNpcEntity extends PathfinderMob {
    public enum Role { HOST, WAITER, COOK, CUSTOMER }

    public static final int CUSTOMER_VARIANTS = 3;
    private static final int GIFT_COOLDOWN = 600;   // 30 seconds

    private static final EntityDataAccessor<Integer> DATA_ROLE =
            SynchedEntityData.defineId(RoadhouseNpcEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_VARIANT =
            SynchedEntityData.defineId(RoadhouseNpcEntity.class, EntityDataSerializers.INT);

    private static final String[] HOST_LINES = {
            "Welcome in! Just one today? Grab any open seat.",
            "Howdy, partner! No wait at all right now.",
            "Make yourself at home. Peanuts are by the door!",
            "Table's ready whenever you are."
    };
    private static final String[] CUSTOMER_LINES = {
            "These rolls are unbelievable.",
            "I come here every Friday, no exceptions.",
            "Get the steak. Medium rare. Trust me.",
            "Did you see that Wingus outside?!",
            "Best peanuts in the whole county.",
            "I'm saving room for dessert. Probably.",
            "Is it just me or is the cook singing back there?"
    };

    private long lastGiftTime = -GIFT_COOLDOWN;

    public RoadhouseNpcEntity(EntityType<? extends RoadhouseNpcEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ROLE, Role.CUSTOMER.ordinal());
        builder.define(DATA_VARIANT, 0);
    }

    public Role getRole() {
        Role[] roles = Role.values();
        return roles[Mth.clamp(entityData.get(DATA_ROLE), 0, roles.length - 1)];
    }

    public int getVariant() {
        return entityData.get(DATA_VARIANT);
    }

    public void setRole(Role role, int variant) {
        entityData.set(DATA_ROLE, role.ordinal());
        entityData.set(DATA_VARIANT, variant);
        setCustomName(Component.literal(title(role)));
    }

    private static String title(Role role) {
        return switch (role) {
            case HOST -> "Host";
            case WAITER -> "Server";
            case COOK -> "Cook";
            case CUSTOMER -> "Customer";
        };
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    /** Spawn eggs give a random role. (Roadhouse-placed NPCs set theirs explicitly.) */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType reason, SpawnGroupData spawnData) {
        SpawnGroupData out = super.finalizeSpawn(level, difficulty, reason, spawnData);
        Role[] roles = Role.values();
        setRole(roles[random.nextInt(roles.length)], random.nextInt(CUSTOMER_VARIANTS));
        return out;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!level().isClientSide) respond(player);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    private void respond(Player player) {
        long now = level().getGameTime();
        boolean ready = now - lastGiftTime >= GIFT_COOLDOWN;
        String line;
        switch (getRole()) {
            case HOST -> line = pick(HOST_LINES);
            case WAITER -> {
                if (ready) {
                    give(player, new ItemStack(Items.COOKED_BEEF));
                    give(player, new ItemStack(Items.BREAD, 2));
                    lastGiftTime = now;
                    line = "Here you go: one steak, cooked just how you like it, and some fresh rolls!";
                } else {
                    line = "I'll be right back to check on you, hon!";
                }
            }
            case COOK -> {
                if (ready) {
                    give(player, new ItemStack(Items.BAKED_POTATO));
                    lastGiftTime = now;
                    line = "Order up! One loaded baked potato.";
                } else {
                    line = "Grill's slammed, give me a minute!";
                }
            }
            default -> line = pick(CUSTOMER_LINES);
        }
        getLookControl().setLookAt(player, 30.0f, 30.0f);
        player.displayClientMessage(Component.literal("§6<" + title(getRole()) + "> §f" + line), false);
    }

    private String pick(String[] lines) {
        return lines[random.nextInt(lines.length)];
    }

    private static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    @Override
    public boolean isPushable() {
        return false;   // staff stay at their stations
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Role", getRole().ordinal());
        tag.putInt("Variant", getVariant());
        tag.putLong("LastGift", lastGiftTime);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        Role[] roles = Role.values();
        setRole(roles[Mth.clamp(tag.getInt("Role"), 0, roles.length - 1)], tag.getInt("Variant"));
        lastGiftTime = tag.getLong("LastGift");
    }
}
