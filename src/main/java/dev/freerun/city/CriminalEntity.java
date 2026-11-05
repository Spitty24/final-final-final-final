package dev.freerun.city;

import dev.freerun.economy.Money;
import dev.freerun.economy.ModSounds;
import dev.freerun.item.ItemRegistry;
import dev.freerun.item.gun.GunType;
import dev.freerun.vehicle.CarEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Somebody up to no good. Crimes are rare (see {@link CrimeManager}) and come in three kinds:
 * a purse snatching, an armed mugging, and a car theft. The suspect does the deed, then runs (or
 * drives off). Catch them first and they give up once they've taken a beating: right-click a
 * suspect with their hands up to cuff them and collect the reward. Or let the police handle it.
 */
public class CriminalEntity extends PathfinderMob implements RangedAttackMob {
    public enum Kind {
        SNATCHER("Purse snatcher", false), MUGGER("Armed mugger", true), CAR_THIEF("Car thief", false);

        public final String title;
        public final boolean armed;

        Kind(String title, boolean armed) {
            this.title = title;
            this.armed = armed;
        }
    }

    private enum State { STALK, ROB, FLEE, SURRENDER, CUFFED }

    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(CriminalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STATE = SynchedEntityData.defineId(CriminalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(CriminalEntity.class, EntityDataSerializers.INT);

    private LivingEntity victim;
    private CarEntity targetCar;
    private int loot;
    private int t;
    private int life;
    private boolean fightingBack;

    public CriminalEntity(EntityType<? extends CriminalEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 22.0)
                .add(Attributes.MOVEMENT_SPEED, 0.33)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_KIND, 0);
        builder.define(DATA_STATE, 0);
        builder.define(DATA_VARIANT, 0);
    }

    public Kind kind() {
        return Kind.values()[Mth.clamp(entityData.get(DATA_KIND), 0, Kind.values().length - 1)];
    }

    private State state() {
        return State.values()[Mth.clamp(entityData.get(DATA_STATE), 0, State.values().length - 1)];
    }

    private void setState(State s) {
        entityData.set(DATA_STATE, s.ordinal());
        t = 0;
    }

    public int variant() {
        return entityData.get(DATA_VARIANT);
    }

    public boolean surrendered() {
        return state() == State.SURRENDER || state() == State.CUFFED;
    }

    public boolean cuffed() {
        return state() == State.CUFFED;
    }

    public boolean armed() {
        return kind().armed;
    }

    public boolean fleeing() {
        return state() == State.FLEE;
    }

    /** Sets the crime up: who they're after, and (for a car thief) which car. */
    public void setup(Kind kind, LivingEntity victim, CarEntity car) {
        entityData.set(DATA_KIND, kind.ordinal());
        entityData.set(DATA_VARIANT, random.nextInt(3));
        this.victim = victim;
        this.targetCar = car;
        setCustomName(Component.literal("§cSuspect"));
        if (kind.armed) {
            setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ItemRegistry.gunItem(GunType.PISTOL)));
            setDropChance(EquipmentSlot.MAINHAND, 0.0f);
        }
        setState(State.STALK);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 20, 40, 16.0f) {
            @Override
            public boolean canUse() {
                return fightingBack && armed() && !surrendered() && super.canUse();
            }
        });
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel server)) return;
        t++;
        life++;
        if (life > 20 * 60 * 4 && !surrendered() && server.getNearestPlayer(this, 48.0) == null) {
            discard();   // got away
            return;
        }
        switch (state()) {
            case STALK -> stalk(server);
            case ROB -> rob(server);
            case FLEE -> flee(server);
            case SURRENDER -> {
                getNavigation().stop();
                setTarget(null);
                if (t > 20 * 90) {   // nobody came for them: they slip off
                    setState(State.FLEE);
                }
            }
            case CUFFED -> {
                getNavigation().stop();
                if (t > 100) {
                    server.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, getX(), getY() + 1, getZ(), 10, 0.3, 0.5, 0.3, 0.02);
                    discard();   // taken into custody
                }
            }
        }
    }

    private void stalk(ServerLevel server) {
        if (kind() == Kind.CAR_THIEF) {
            if (targetCar == null || !targetCar.isAlive()) {
                setState(State.FLEE);
                return;
            }
            getNavigation().moveTo(targetCar, 1.2);
            if (distanceToSqr(targetCar) < 3.0 * 3.0) {
                Entity driver = targetCar.getFirstPassenger();
                if (driver != null) {
                    driver.stopRiding();
                    if (driver instanceof CitizenEntity c) {
                        c.flee(position());
                        shout(c, "HEY! That's my car! Somebody stop him!");
                    }
                }
                startRiding(targetCar, true);
                targetCar.joyride(20 * 120);
                CrimeManager.committed(server, this, kind(), 0);
                setState(State.FLEE);
            }
            if (t > 400) setState(State.FLEE);
            return;
        }
        if (victim == null || !victim.isAlive()) {
            setState(State.FLEE);
            return;
        }
        getNavigation().moveTo(victim, kind() == Kind.SNATCHER ? 1.25 : 1.0);
        if (distanceToSqr(victim) < 2.2 * 2.2) setState(State.ROB);
        if (t > 400) setState(State.FLEE);
    }

    private void rob(ServerLevel server) {
        if (victim == null) {
            setState(State.FLEE);
            return;
        }
        getLookControl().setLookAt(victim, 30, 30);
        if (kind() == Kind.MUGGER) {
            getNavigation().stop();
            if (t == 1) shoutNear("§c<Suspect> Wallet. Now. Don't be a hero.");
            if (victim instanceof CitizenEntity c) c.distract(40);
            if (t < 80) return;
        }
        int took = victim instanceof CitizenEntity c ? Math.max(8, c.drain(40 + random.nextInt(60))) : 20;
        loot += took;
        if (victim instanceof CitizenEntity c) {
            c.flee(position());
            shout(c, kind() == Kind.SNATCHER ? "HEY! Stop! He took my purse!" : "Help! I just got robbed!");
        }
        CrimeManager.committed(server, this, kind(), took);
        setState(State.FLEE);
    }

    private void flee(ServerLevel server) {
        if (getVehicle() instanceof CarEntity car) {
            // driving off in the stolen car: bail out if the law gets close or the car's stopped
            boolean cornered = !level().getEntitiesOfClass(PoliceOfficerEntity.class, getBoundingBox().inflate(9.0)).isEmpty()
                    || server.getNearestPlayer(this, 5.0) != null && Math.abs(car.speed()) < 0.08;
            if (cornered || t > 20 * 100) {
                stopRiding();
                car.stall(20 * 10);
                t = 0;
            }
            return;
        }
        Entity chaser = level().getNearestPlayer(this, 24.0);
        PoliceOfficerEntity cop = level().getEntitiesOfClass(PoliceOfficerEntity.class, getBoundingBox().inflate(24.0)).stream().findFirst().orElse(null);
        if (cop != null && (chaser == null || cop.distanceToSqr(this) < chaser.distanceToSqr(this))) chaser = cop;
        Vec3 from = chaser != null ? chaser.position() : victim != null ? victim.position() : position().add(1, 0, 0);
        if (t % 20 == 1 || getNavigation().isDone()) {
            Vec3 away = position().subtract(from);
            if (away.lengthSqr() < 0.01) away = new Vec3(random.nextGaussian(), 0, random.nextGaussian());
            away = away.normalize().scale(16.0).add(random.nextGaussian() * 4, 0, random.nextGaussian() * 4);
            getNavigation().moveTo(getX() + away.x, getY(), getZ() + away.z, 1.35);
        }
        // a cop right on top of an unarmed suspect: it's over
        if (cop != null && cop.distanceToSqr(this) < 3.0 * 3.0 && !armed()) surrender(cop);
    }

    private void surrender(Entity to) {
        if (surrendered()) return;
        setState(State.SURRENDER);
        setTarget(null);
        fightingBack = false;
        getNavigation().stop();
        setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        shoutNear("§c<Suspect> §fOkay! Okay! I give up! Don't shoot!");
    }

    /** An officer caught up with them. */
    public void arrestBy(PoliceOfficerEntity officer) {
        if (cuffed()) return;
        setState(State.CUFFED);
        level().playSound(null, blockPosition(), ModSounds.CUFFS, SoundSource.NEUTRAL, 1.0f, 1.0f);
        returnLoot();
    }

    private void returnLoot() {
        if (victim instanceof CitizenEntity c && c.isAlive()) c.setMoney(c.money() + loot);
        loot = 0;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (state() == State.SURRENDER && player instanceof ServerPlayer sp) {
            setState(State.CUFFED);
            level().playSound(null, blockPosition(), ModSounds.CUFFS, SoundSource.NEUTRAL, 1.0f, 1.0f);
            int reward = CrimeManager.reward(sp, kind(), loot);
            returnLoot();
            CityWorld.recordCrime(sp.serverLevel(), blockPosition(), true, false);
            sp.displayClientMessage(Component.literal("§a🚔 You cuffed the " + kind().title.toLowerCase()
                    + "! The city pays a reward of " + Money.fmt(reward) + "."), false);
            Police.announce(sp.serverLevel(), blockPosition(), "§9🚔 Dispatch: citizen " + sp.getName().getString()
                    + " detained the suspect. Units, stand down.", 160);
            for (PoliceOfficerEntity cop : level().getEntitiesOfClass(PoliceOfficerEntity.class, getBoundingBox().inflate(160.0),
                    c -> c.suspect() == this)) cop.standDown();
            return InteractionResult.CONSUME;
        }
        if (!surrendered()) {
            player.displayClientMessage(Component.literal("§7They won't come quietly. Take them down first."), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (cuffed()) return false;
        boolean hurt = super.hurt(source, amount);
        if (!hurt || level().isClientSide) return hurt;
        if (getHealth() < getMaxHealth() * 0.4f && isAlive()) {
            surrender(source.getEntity());
        } else if (armed() && source.getEntity() instanceof LivingEntity l && !surrendered()) {
            fightingBack = true;
            setTarget(l);
        } else if (state() == State.STALK || state() == State.ROB) {
            setState(State.FLEE);
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel server && source.getEntity() instanceof ServerPlayer sp && !cuffed()) {
            int reward = CrimeManager.reward(sp, kind(), 0, 3);
            sp.displayClientMessage(Component.literal("§eSuspect down. The city pays a reduced reward: " + Money.fmt(reward)
                    + ". §7(Next time, let them surrender and cuff them.)"), false);
            CityWorld.recordCrime(server, blockPosition(), true, false);
        }
        if (loot > 0) for (ItemStack bill : Money.bills(loot)) spawnAtLocation(bill);
        loot = 0;
    }

    @Override
    public void performRangedAttack(LivingEntity target, float velocityFactor) {
        double dx = target.getX() - getX() + random.nextGaussian() * 0.7;
        double dy = target.getEyeY() - 0.3 - (getEyeY() - 0.1) + random.nextGaussian() * 0.5;
        double dz = target.getZ() - getZ() + random.nextGaussian() * 0.7;
        GunType.PISTOL.fire(level(), this, new Vec3(dx, dy, dz));
    }

    private void shout(LivingEntity who, String line) {
        Component msg = Component.literal("§f<" + who.getName().getString() + "> §c" + line);
        for (Player p : level().players()) if (p.distanceToSqr(who) < 40 * 40) p.displayClientMessage(msg, false);
    }

    private void shoutNear(String text) {
        Component msg = Component.literal(text);
        for (Player p : level().players()) if (p.distanceToSqr(this) < 24 * 24) p.displayClientMessage(msg, false);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Kind", entityData.get(DATA_KIND));
        tag.putInt("Loot", loot);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_KIND, tag.getInt("Kind"));
        loot = tag.getInt("Loot");
        // a reloaded suspect has lost track of whatever they were doing: they just run
        entityData.set(DATA_STATE, State.FLEE.ordinal());
    }
}
