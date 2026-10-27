package dev.freerun.mobs;

import dev.freerun.item.ItemRegistry;
import dev.freerun.item.gun.GunType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

/**
 * Base for the two soldier mobs. Faction is fixed per subclass (so it's safe to read while the
 * goals are being built in the constructor); the gun is rolled on spawn and saved to NBT.
 * The soldier holds (and drops) the real gun item, and fires via the shared {@link GunType} logic.
 */
public abstract class SoldierEntity extends Monster implements RangedAttackMob {

    public enum Faction { US, FRENCH }

    private GunType gun = GunType.PISTOL;

    protected SoldierEntity(EntityType<? extends SoldierEntity> type, Level level) {
        super(type, level);
    }

    public abstract Faction getFaction();

    public GunType getGun() {
        return gun;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 25, 50, 20.0f));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        if (getFaction() == Faction.US) {
            // Friendly: shoots hostile monsters (zombies, skeletons, enemy soldiers), never players.
            targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
                    e -> !(e instanceof SoldierEntity s && s.getFaction() == Faction.US)));
        } else {
            // Hostile: hunts the player, and fights US soldiers.
            targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
            targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, SoldierEntity.class, 10, true, false,
                    e -> e instanceof SoldierEntity s && s.getFaction() == Faction.US));
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType reason, SpawnGroupData spawnData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnData);
        GunType[] guns = GunType.values();
        gun = guns[this.random.nextInt(guns.length)];
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ItemRegistry.gunItem(gun)));
        setDropChance(EquipmentSlot.MAINHAND, 0.0f);   // we handle drops ourselves in die()
        return data;
    }

    // --- shooting ---

    @Override
    public void performRangedAttack(LivingEntity target, float velocityFactor) {
        if (!(level() instanceof ServerLevel)) return;
        double dx = target.getX() - this.getX();
        double dy = target.getEyeY() - (this.getEyeY() - 0.1);
        double dz = target.getZ() - this.getZ();
        double horiz = Math.sqrt(dx * dx + dz * dz);
        gun.fire(level(), this, new Vec3(dx, dy + horiz * 0.12, dz));
    }

    // --- loot: drop the gun and some ammo on death ---

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide) {
            spawnAtLocation(new ItemStack(ItemRegistry.gunItem(gun)));
            int ammoCount = 2 + this.random.nextInt(5);
            spawnAtLocation(new ItemStack(gun.ammoItem(), ammoCount));
        }
        super.die(source);
    }

    // --- persistence ---

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Gun", gun.ordinal());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        int idx = tag.getInt("Gun");
        GunType[] guns = GunType.values();
        if (idx >= 0 && idx < guns.length) gun = guns[idx];
    }
}
