package dev.freerun.city;

import dev.freerun.vehicle.CarEntity;
import dev.freerun.vehicle.VehicleRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * The old per-city marker entity. Cities now keep their books in the world's saved data and the
 * population is managed around players (see {@link CityManager}), so any hub left in an older
 * world simply removes itself. The car-spawning helper lives on here because everything uses it.
 */
public class CityHubEntity extends Entity {
    public CityHubEntity(EntityType<? extends CityHubEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
        setInvisible(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) discard();
    }

    /** A car in a lane with a resident at the wheel. Returns the car, or null if the spot was blocked. */
    public static CarEntity spawnDrivenCar(net.minecraft.world.level.ServerLevelAccessor level, BlockPos city, double x, double y, double z,
                                           int heading, RandomSource r) {
        ServerLevel server = level.getLevel();
        CarEntity car = new CarEntity(VehicleRegistry.CAR, server);
        float yaw = CityLayout.yaw(heading);
        car.moveTo(x, y, z, yaw, 0.0f);
        car.setYBodyRot(yaw);
        car.setYHeadRot(yaw);
        car.randomColor();
        car.addFuel(100.0f);
        if (!level.noCollision(car)) return null;
        car.setCity(city);
        CitizenEntity driver = new CitizenEntity(CityRegistry.CITIZEN, server);
        CitizenEntity.Job[] jobs = CitizenEntity.Job.values();
        CitizenEntity.Job job = r.nextInt(3) == 0 ? CitizenEntity.Job.RIDESHARE_DRIVER : jobs[r.nextInt(jobs.length)];
        driver.setup(r, city, BlockPos.containing(x, y, z), job);
        driver.moveTo(x, y, z, yaw, 0.0f);
        driver.startRiding(car, true);
        level.addFreshEntityWithPassengers(car);
        return car;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
