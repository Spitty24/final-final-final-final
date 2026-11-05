package dev.freerun.gang;

import dev.freerun.item.ItemRegistry;
import dev.freerun.item.gun.GunType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
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
 * A member of one of the parking-lot crews. They hang around their end of the lot trash-talking
 * the other crew, and when the {@link GangTurfEntity} kicks off a shootout they open fire on each
 * other. They leave you alone unless you start something — then their whole crew comes for you.
 * Stray shots don't care who you are, though.
 */
public class GangsterEntity extends Monster implements RangedAttackMob {
    private static final EntityDataAccessor<Integer> DATA_CREW =
            SynchedEntityData.defineId(GangsterEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_VARIANT =
            SynchedEntityData.defineId(GangsterEntity.class, EntityDataSerializers.INT);

    private GunType gun = GunType.PISTOL;
    private long warUntil;
    private BlockPos home;
    // a hold-up at the Double Quick
    private BlockPos robAt;
    private int robT;
    private int robArrivedAt = -1;
    private boolean robShooting;
    private int stolen;
    private int stuck;
    private net.minecraft.world.phys.Vec3 lastPos = net.minecraft.world.phys.Vec3.ZERO;

    public GangsterEntity(EntityType<? extends GangsterEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 18.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.FOLLOW_RANGE, 28.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CREW, 0);
        builder.define(DATA_VARIANT, 0);
    }

    public Crew crew() {
        return Crew.byId(entityData.get(DATA_CREW));
    }

    public int variant() {
        return entityData.get(DATA_VARIANT);
    }

    public void setup(Crew crew, int variant, GunType gun, BlockPos home) {
        entityData.set(DATA_CREW, crew.ordinal());
        entityData.set(DATA_VARIANT, variant & 1);
        this.gun = gun;
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ItemRegistry.gunItem(gun)));
        setDropChance(EquipmentSlot.MAINHAND, 0.0f);
        setCustomName(Component.literal(crew.color + crew.member));
        setHome(home);
    }

    public void setHome(BlockPos pos) {
        home = pos.immutable();
        restrictTo(home, 14);
    }

    /** Called by the turf while a shootout is on. */
    public void goToWar(long gameTime) {
        warUntil = gameTime + 40;
    }

    public boolean atWar() {
        return level().getGameTime() < warUntil;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 18, 36, 18.0f));
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, GangsterEntity.class, 5, true, false,
                e -> atWar() && e instanceof GangsterEntity g && g.crew() != crew()));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType reason, SpawnGroupData spawnData) {
        SpawnGroupData out = super.finalizeSpawn(level, difficulty, reason, spawnData);
        // Spawn eggs: a random crew and piece.
        setup(Crew.byId(random.nextInt(2)), random.nextInt(2), randomGun(random.nextFloat()), blockPosition());
        return out;
    }

    public static GunType randomGun(float roll) {
        return roll < 0.6f ? GunType.PISTOL : roll < 0.85f ? GunType.SHOTGUN : GunType.RIFLE;
    }

    @Override
    public void performRangedAttack(LivingEntity target, float velocityFactor) {
        if (!(level() instanceof ServerLevel)) return;
        // They spray more than they aim.
        double dx = target.getX() - getX() + random.nextGaussian() * 0.6;
        double dy = target.getEyeY() - 0.3 - (getEyeY() - 0.1) + random.nextGaussian() * 0.4;
        double dz = target.getZ() - getZ() + random.nextGaussian() * 0.6;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        gun.fire(level(), this, new Vec3(dx, dy + horiz * 0.01, dz));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // No friendly fire within a crew, and the Rats never take a shot at their boss (or from him).
        if (source.getEntity() instanceof GangsterEntity g && g.crew() == crew()) return false;
        if (crew() == Crew.RUST_RATS && source.getEntity() instanceof dev.freerun.mobs.EthanEntity) return false;
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && source.getEntity() instanceof Player player && !player.isCreative()) {
            // Start something with one of them and the whole crew answers.
            for (GangsterEntity g : level().getEntitiesOfClass(GangsterEntity.class, getBoundingBox().inflate(20.0),
                    g -> g.crew() == crew() && g.getTarget() == null)) {
                g.setTarget(player);
            }
        }
        return hurt;
    }

    /** Sent to hold up the store whose clerk is at {@code clerkPos}. */
    public void startRobbery(BlockPos clerkPos, boolean shooting) {
        robAt = clerkPos.immutable();
        robT = 0;
        robArrivedAt = -1;
        robShooting = shooting;
        clearRestriction();
    }

    public boolean robbing() {
        return robAt != null;
    }

    private void endRobbery() {
        robAt = null;
        if (home != null) restrictTo(home, 14);
        getNavigation().stop();
    }

    private void tickRobbery() {
        robT++;
        double dx = robAt.getX() + 0.5 - getX();
        double dz = robAt.getZ() + 0.5 - getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (robArrivedAt < 0) {
            if (dist < 5.0) {
                robArrivedAt = robT;
                holdUp();
                return;
            }
            if (robT > 1200) {   // never made it
                endRobbery();
                return;
            }
            if (robT % 10 == 0) {
                if (position().distanceToSqr(lastPos) < 0.04) stuck += 10;
                else stuck = 0;
                lastPos = position();
                if (stuck > 200) {
                    moveTo(robAt.getX() + 0.5 - dx / dist * 3.0, robAt.getY(), robAt.getZ() + 0.5 - dz / dist * 3.0, getYRot(), 0.0f);
                    stuck = 0;
                }
                double hx = dist > 12 ? getX() + dx / dist * 11.0 : robAt.getX() + 0.5;
                double hz = dist > 12 ? getZ() + dz / dist * 11.0 : robAt.getZ() + 0.5;
                getNavigation().moveTo(hx, robAt.getY(), hz, 1.2);
            }
            return;
        }
        int since = robT - robArrivedAt;
        getNavigation().stop();
        if (robShooting && since % (15 + random.nextInt(10)) == 0 && getTarget() == null) {
            // Spraying the storefront.
            double tx = robAt.getX() + 0.5 + random.nextGaussian() * 5.0;
            double ty = robAt.getY() + 0.5 + random.nextDouble() * 2.5;
            double tz = robAt.getZ() + 0.5 + random.nextGaussian() * 5.0;
            getLookControl().setLookAt(tx, ty, tz);
            gun.fire(level(), this, new Vec3(tx - getX(), ty - getEyeY(), tz - getZ()));
        }
        if (since > 400) {
            Component msg = Component.literal("§7The robbers take off back toward the parking lot.");
            for (Player p : level().players()) if (distanceToSqr(p) < 40 * 40) p.displayClientMessage(msg, false);
            endRobbery();
        }
    }

    /** Everybody in the store hands over their cash. */
    private void holdUp() {
        Crew c = crew();
        String who = c.color + "<" + c.member + "> §f";
        Component alarm = Component.literal("§4§l⚠ ROBBERY IN PROGRESS! §r" + who
                + (robShooting ? "EVERYBODY DOWN! *BANG* *BANG*" : "Nobody move! Wallets on the counter!"));
        for (Player p : level().players()) if (distanceToSqr(p) < 64 * 64) p.displayClientMessage(alarm, false);
        level().playSound(null, getX(), getY(), getZ(), net.minecraft.sounds.SoundEvents.BELL_BLOCK,
                net.minecraft.sounds.SoundSource.HOSTILE, 2.0f, 1.6f);
        for (dev.freerun.mobs.RoadhouseNpcEntity clerk : level().getEntitiesOfClass(dev.freerun.mobs.RoadhouseNpcEntity.class,
                getBoundingBox().inflate(8.0), n -> n.getRole() == dev.freerun.mobs.RoadhouseNpcEntity.Role.CLERK)) {
            clerk.shout("§ePlease! Take whatever you want! The register's open!");
        }
        for (Player p : level().players()) {
            if (p.isCreative() || p.isSpectator() || p.distanceToSqr(robAt.getX(), robAt.getY(), robAt.getZ()) > 12 * 12) continue;
            int take = Math.min(25, dev.freerun.economy.Money.balance(p));
            if (take > 0 && dev.freerun.economy.Money.pay(p, take)) {
                stolen += take;
                p.displayClientMessage(Component.literal(who + "Your wallet. NOW. §c(-" + dev.freerun.economy.Money.fmt(take)
                        + ") §7Take him down to get it back."), false);
            } else {
                p.displayClientMessage(Component.literal(who + "You're broke?! Get outta here."), false);
            }
        }
        stolen += 20 + random.nextInt(60);   // and the register
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (robAt != null) {
            tickRobbery();
            return;
        }
        if (home != null && !hasRestriction()) restrictTo(home, 14);
        // Shootout's over: stop fighting the other crew (but keep after any player who started it).
        if (!atWar() && getTarget() instanceof GangsterEntity) setTarget(null);
    }

    public void taunt() {
        Crew c = crew();
        Component msg = Component.literal(c.color + "<" + c.member + "> §f" + c.taunts[random.nextInt(c.taunts.length)]);
        for (Player p : level().players()) {
            if (distanceToSqr(p) < 28 * 28) p.displayClientMessage(msg, false);
        }
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide) {
            if (random.nextFloat() < 0.25f) spawnAtLocation(new ItemStack(ItemRegistry.gunItem(gun)));
            spawnAtLocation(new ItemStack(gun.ammoItem(), 1 + random.nextInt(4)));
            int cash = stolen + 1 + random.nextInt(15);
            for (ItemStack bill : dev.freerun.economy.Money.bills(cash)) spawnAtLocation(bill);
        }
        super.die(source);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Crew", crew().ordinal());
        tag.putInt("Variant", variant());
        tag.putInt("Gun", gun.ordinal());
        if (home != null) {
            tag.putInt("HomeX", home.getX());
            tag.putInt("HomeY", home.getY());
            tag.putInt("HomeZ", home.getZ());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_CREW, Mth.clamp(tag.getInt("Crew"), 0, 1));
        entityData.set(DATA_VARIANT, tag.getInt("Variant") & 1);
        GunType[] guns = GunType.values();
        gun = guns[Mth.clamp(tag.getInt("Gun"), 0, guns.length - 1)];
        if (tag.contains("HomeX")) setHome(new BlockPos(tag.getInt("HomeX"), tag.getInt("HomeY"), tag.getInt("HomeZ")));
    }
}
