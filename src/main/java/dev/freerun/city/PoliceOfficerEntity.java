package dev.freerun.city;

import dev.freerun.item.ItemRegistry;
import dev.freerun.item.gun.GunType;
import dev.freerun.vehicle.CarEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * A city police officer. Most of them patrol in a cruiser (driving with the rest of the traffic);
 * a few work out of the precinct. When a crime is called in, the nearest one hits the lights and
 * siren, drives straight there, and goes after the suspect on foot: an unarmed suspect gets
 * tackled and cuffed, an armed one gets a warning and then the officer's sidearm. They'll come
 * for you too if you hurt people.
 */
public class PoliceOfficerEntity extends PathfinderMob implements RangedAttackMob {
    private static final String[] NAMES = {"Ofc. Broussard", "Ofc. Landry", "Ofc. Guidry", "Ofc. Hebert", "Sgt. Thibodeaux",
            "Ofc. Fontenot", "Ofc. Castille", "Sgt. Richard", "Ofc. Breaux", "Ofc. Savoie", "Ofc. Mouton", "Lt. Arceneaux"};
    private static final String[] CHATTER = {"Evening. Keep it safe out there.", "Quiet shift so far. Let's keep it that way.",
            "If you see anything, call it in.", "Drive the speed limit and we'll get along fine.",
            "Coffee's terrible at the precinct. Don't tell the sergeant.", "Nice day for it."};

    private LivingEntity suspect;
    private UUID suspectId;
    private UUID carId;
    private BlockPos post;
    private boolean ambient;
    private int onFoot;
    private int warned;

    public PoliceOfficerEntity(EntityType<? extends PoliceOfficerEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.ARMOR, 4.0);
    }

    public void setup(int seed, BlockPos post) {
        setCustomName(Component.literal(NAMES[Math.floorMod(seed, NAMES.length)]));
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ItemRegistry.gunItem(GunType.PISTOL)));
        setDropChance(EquipmentSlot.MAINHAND, 0.0f);
        this.post = post == null ? null : post.immutable();
        if (this.post != null) restrictTo(this.post, 12);
    }

    public boolean ambient() {
        return ambient;
    }

    public void setAmbient(boolean ambient) {
        this.ambient = ambient;
    }

    public void setCar(CarEntity car) {
        carId = car == null ? null : car.getUUID();
    }

    public boolean busy() {
        return suspect != null && suspect.isAlive();
    }

    public LivingEntity suspect() {
        return suspect;
    }

    /** Respond to a call: go after {@code who}. */
    public void respond(LivingEntity who) {
        suspect = who;
        suspectId = who.getUUID();
        warned = 0;
        clearRestriction();
        if (getVehicle() instanceof CarEntity car) {
            car.setSiren(true);
            car.autopilot().chase = who.position();
            car.autopilot().chaseStopAt = 7.0;
        }
    }

    public void standDown() {
        suspect = null;
        suspectId = null;
        setTarget(null);
        if (getVehicle() instanceof CarEntity car) {
            car.setSiren(false);
            car.autopilot().chase = null;
        }
        if (post != null) restrictTo(post, 12);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new RangedAttackGoal(this, 1.15, 16, 30, 22.0f));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6, 0.01f));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel server)) return;
        if (suspect == null && suspectId != null && tickCount % 20 == 0 && server.getEntity(suspectId) instanceof LivingEntity l) suspect = l;
        if (suspect != null && (!suspect.isAlive() || suspect.distanceToSqr(this) > 200 * 200
                || suspect instanceof CriminalEntity c && c.cuffed()
                || suspect instanceof ServerPlayer sp && Police.wanted(sp) == 0)) {
            standDown();
        }
        if (suspect == null) {
            setTarget(null);
            if (getVehicle() == null && tickCount % 40 == 0) backToCar(server);
            return;
        }
        // in the car: drive to the suspect, then get out
        if (getVehicle() instanceof CarEntity car) {
            car.autopilot().chase = suspect.position();
            boolean suspectDriving = suspect.getVehicle() instanceof CarEntity;
            if (distanceToSqr(suspect) < (suspectDriving ? 6 * 6 : 10 * 10) && Math.abs(car.speed()) < 0.12) {
                stopRiding();
                onFoot = 0;
                say(suspect instanceof Player ? "Police! Stop right there!" : "Police! Don't move!");
            } else if (car.autopilot().stuckTicks() > 80) {
                stopRiding();
            }
            return;
        }
        onFoot++;
        // armed (or shooting at us): warn once, then return fire
        boolean dangerous = suspect instanceof CriminalEntity c ? c.armed() && !c.surrendered()
                : suspect instanceof ServerPlayer sp && Police.wanted(sp) >= 2;
        if (dangerous) {
            if (warned++ == 0) say("Drop the weapon! NOW!");
            if (warned > 30) setTarget(suspect);
        } else {
            setTarget(null);
            getNavigation().moveTo(suspect, 1.3);
            if (distanceToSqr(suspect) < 2.6 * 2.6) arrest(server);
        }
    }

    private void arrest(ServerLevel server) {
        if (suspect instanceof CriminalEntity c) {
            c.arrestBy(this);
            CityWorld.recordCrime(server, blockPosition(), true, true);
            Police.announce(server, blockPosition(), "§9🚔 " + getName().getString() + ": suspect in custody.", 120);
            standDown();
        } else if (suspect instanceof ServerPlayer sp) {
            Police.arrestPlayer(sp, this);
            standDown();
        }
    }

    private void backToCar(ServerLevel server) {
        if (carId == null) return;
        if (!(server.getEntity(carId) instanceof CarEntity car) || !car.isAlive()) {
            carId = null;
            return;
        }
        if (distanceToSqr(car) < 3.0 * 3.0 && car.getPassengers().isEmpty()) {
            startRiding(car, true);
            car.setSiren(false);
            car.autopilot().chase = null;
            car.autopilot().reset();
        } else {
            getNavigation().moveTo(car, 1.0);
        }
    }

    @Override
    public void performRangedAttack(LivingEntity target, float velocityFactor) {
        // Trained: much steadier than a gang member.
        double dx = target.getX() - getX() + random.nextGaussian() * 0.25;
        double dy = target.getEyeY() - 0.4 - (getEyeY() - 0.1) + random.nextGaussian() * 0.2;
        double dz = target.getZ() - getZ() + random.nextGaussian() * 0.25;
        GunType.PISTOL.fire(level(), this, new Vec3(dx, dy, dz));
    }

    private void say(String line) {
        Component msg = Component.literal("§9<" + getName().getString() + "> §f" + line);
        for (Player p : level().players()) if (p.distanceToSqr(this) < 28 * 28) p.displayClientMessage(msg, false);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!level().isClientSide) {
            String line = busy() ? "Not now, I'm on a call!" : player instanceof ServerPlayer sp && Police.wanted(sp) > 0
                    ? "You. Hands where I can see them." : CHATTER[random.nextInt(CHATTER.length)];
            player.displayClientMessage(Component.literal("§9<" + getName().getString() + "> §f" + line), false);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof PoliceOfficerEntity) return false;
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && source.getEntity() instanceof ServerPlayer sp && !sp.isCreative()) {
            Police.reportAttackOnOfficer(sp, this);
        } else if (hurt && !level().isClientSide && source.getEntity() instanceof LivingEntity l && !(l instanceof Player)) {
            respond(l);
        }
        return hurt;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Ambient", ambient);
        if (carId != null) tag.putUUID("Car", carId);
        if (post != null) tag.putLong("Post", post.asLong());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ambient = tag.getBoolean("Ambient");
        if (tag.hasUUID("Car")) carId = tag.getUUID("Car");
        if (tag.contains("Post")) {
            post = BlockPos.of(tag.getLong("Post"));
            restrictTo(post, 12);
        }
    }
}
