package dev.freerun.city;

import dev.freerun.mobs.RoadhouseNpcEntity;
import dev.freerun.vehicle.CarEntity;
import dev.freerun.vehicle.VehicleRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * An invisible marker at the middle of a city that keeps it alive: it tops up the population
 * (new residents move in, new cars come onto the roads, always out of sight), knows where the
 * shops are, and keeps the city's books: wages paid, rent, and what people spend in the stores.
 * Shop prices follow demand: a busy day pushes prices up, a dead one brings sales.
 */
public class CityHubEntity extends Entity {
    private static final int MIN_CITIZENS = 28;
    private static final int MIN_CARS = 9;

    private BlockPos origin;
    private final List<BlockPos> shops = new ArrayList<>();
    private int scan;
    // books
    private long day = -1;
    private int wagesToday, salesToday, rentToday;
    private int salesYesterday = -1;
    // last census
    private int population, employed, avgMoney, cars;

    public CityHubEntity(EntityType<? extends CityHubEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
        setInvisible(true);
    }

    public void setOrigin(BlockPos origin) {
        this.origin = origin.immutable();
    }

    public BlockPos origin() {
        return origin != null ? origin : blockPosition().offset(-CityLayout.HALF, 0, -CityLayout.HALF);
    }

    public AABB cityBox() {
        BlockPos o = origin();
        return new AABB(o.getX(), o.getY() - 8, o.getZ(), o.getX() + CityLayout.SIZE, o.getY() + 64, o.getZ() + CityLayout.SIZE);
    }

    /** The hub of the city containing (or nearest to) {@code pos}, within 200 blocks. */
    public static CityHubEntity nearest(ServerLevel level, BlockPos pos) {
        return level.getEntitiesOfClass(CityHubEntity.class, new AABB(pos).inflate(200.0, 96.0, 200.0)).stream()
                .min(java.util.Comparator.comparingDouble(h -> h.distanceToSqr(pos.getCenter()))).orElse(null);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    // ------------------------------------------------------------------ the books

    public void recordWages(int n) {
        wagesToday += n;
    }

    public void recordSale(int n) {
        salesToday += n;
    }

    public void recordRent(int n) {
        rentToday += n;
    }

    /** Price multiplier for player purchases in this city's shops (0.8 to 1.3). */
    public double priceMultiplier() {
        int expected = Math.max(1, population * 9);
        int sales = salesYesterday >= 0 ? (salesYesterday + salesToday) / 2 : salesToday;
        double demand = sales / (double) expected;
        return Mth.clamp(0.8 + demand * 0.35, 0.8, 1.3);
    }

    public CompoundTag stats() {
        CompoundTag t = new CompoundTag();
        t.putInt("Population", population);
        t.putInt("Employed", employed);
        t.putInt("AvgMoney", avgMoney);
        t.putInt("Cars", cars);
        t.putInt("Wages", wagesToday);
        t.putInt("Sales", salesToday);
        t.putInt("Rent", rentToday);
        t.putInt("SalesYesterday", Math.max(0, salesYesterday));
        t.putInt("PriceIndex", (int) Math.round(priceMultiplier() * 100));
        t.putInt("OX", origin().getX());
        t.putInt("OY", origin().getY());
        t.putInt("OZ", origin().getZ());
        return t;
    }

    // ------------------------------------------------------------------ shops

    public BlockPos randomShop(RandomSource r) {
        return shops.isEmpty() ? null : shops.get(r.nextInt(shops.size()));
    }

    public boolean isShop(BlockPos pos) {
        return pos != null && shops.contains(pos);
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;
        long d = level().getDayTime() / 24000L;
        if (day != d) {
            if (day >= 0) salesYesterday = salesToday;
            day = d;
            wagesToday = salesToday = rentToday = 0;
        }
        if (--scan > 0) return;
        scan = 200;
        AABB box = cityBox();
        // census
        List<CitizenEntity> people = server.getEntitiesOfClass(CitizenEntity.class, box);
        population = people.size();
        employed = (int) people.stream().filter(c -> c.job().wage > 0).count();
        avgMoney = people.isEmpty() ? 0 : (int) people.stream().mapToInt(CitizenEntity::money).average().orElse(0);
        List<CarEntity> all = server.getEntitiesOfClass(CarEntity.class, box);
        cars = (int) all.stream().filter(c -> c.getFirstPassenger() instanceof CitizenEntity).count();
        // where the registers are
        shops.clear();
        for (RoadhouseNpcEntity n : server.getEntitiesOfClass(RoadhouseNpcEntity.class, box,
                n -> n.shop() != null || n.getRole() == RoadhouseNpcEntity.Role.HOST)) {
            shops.add(n.blockPosition().immutable());
        }
        // move-ins, out of sight
        if (population < MIN_CITIZENS) spawnCitizen(server);
        if (cars < MIN_CARS) spawnCar(server);
    }

    private boolean watched(ServerLevel server, BlockPos p) {
        return server.getNearestPlayer(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 28.0, false) != null
                || !server.isLoaded(p) || !server.shouldTickBlocksAt(p);
    }

    private void spawnCitizen(ServerLevel server) {
        CityLayout.District d = random.nextInt(3) == 0 ? CityLayout.District.HOOD : CityLayout.District.SUBURB;
        List<BlockPos> spots = CityLayout.spots(d);
        if (spots.isEmpty()) return;
        BlockPos at = origin().offset(spots.get(random.nextInt(spots.size()))).offset(random.nextInt(9) - 4, 0, random.nextInt(9) - 4);
        if (watched(server, at)) return;
        CitizenEntity c = new CitizenEntity(CityRegistry.CITIZEN, server);
        CitizenEntity.Job[] jobs = CitizenEntity.Job.values();
        c.setup(random, origin(), at, jobs[random.nextInt(jobs.length)]);
        c.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360f, 0f);
        if (server.noCollision(c)) server.addFreshEntity(c);
    }

    private void spawnCar(ServerLevel server) {
        boolean horizontal = random.nextBoolean();
        int line = random.nextInt(CityLayout.XS.length);
        int seg = random.nextInt(CityLayout.cols());
        int h;
        double lx, lz;
        if (horizontal) {
            h = random.nextBoolean() ? CityLayout.EAST : CityLayout.WEST;
            lx = (CityLayout.XS[seg] + CityLayout.ROAD + CityLayout.XS[seg + 1]) / 2.0;
            lz = CityLayout.laneCoord(h, line);
        } else {
            h = random.nextBoolean() ? CityLayout.SOUTH : CityLayout.NORTH;
            lz = (CityLayout.ZS[seg] + CityLayout.ROAD + CityLayout.ZS[seg + 1]) / 2.0;
            lx = CityLayout.laneCoord(h, line);
        }
        BlockPos at = origin().offset((int) lx, 1, (int) lz);
        if (watched(server, at)) return;
        spawnDrivenCar(server, origin(), origin().getX() + lx, origin().getY() + 1, origin().getZ() + lz, h, random);
    }

    /** A car in a lane with a resident at the wheel. Used by the hub and by road generation. */
    public static void spawnDrivenCar(net.minecraft.world.level.ServerLevelAccessor level, BlockPos city, double x, double y, double z,
                                      int heading, RandomSource r) {
        ServerLevel server = level.getLevel();
        CarEntity car = new CarEntity(VehicleRegistry.CAR, server);
        float yaw = CityLayout.yaw(heading);
        car.moveTo(x, y, z, yaw, 0.0f);
        car.setYBodyRot(yaw);
        car.setYHeadRot(yaw);
        car.randomColor();
        car.addFuel(100.0f);
        if (!level.noCollision(car)) return;
        car.setCity(city);
        CitizenEntity driver = new CitizenEntity(CityRegistry.CITIZEN, server);
        CitizenEntity.Job[] jobs = CitizenEntity.Job.values();
        CitizenEntity.Job job = r.nextInt(3) == 0 ? CitizenEntity.Job.RIDESHARE_DRIVER : jobs[r.nextInt(jobs.length)];
        driver.setup(r, city, BlockPos.containing(x, y, z), job);
        driver.moveTo(x, y, z, yaw, 0.0f);
        driver.startRiding(car, true);
        level.addFreshEntityWithPassengers(car);
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
        if (tag.contains("OX")) origin = new BlockPos(tag.getInt("OX"), tag.getInt("OY"), tag.getInt("OZ"));
        day = tag.contains("Day") ? tag.getLong("Day") : -1;
        wagesToday = tag.getInt("Wages");
        salesToday = tag.getInt("Sales");
        rentToday = tag.getInt("Rent");
        salesYesterday = tag.contains("SalesY") ? tag.getInt("SalesY") : -1;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        BlockPos o = origin();
        tag.putInt("OX", o.getX());
        tag.putInt("OY", o.getY());
        tag.putInt("OZ", o.getZ());
        tag.putLong("Day", day);
        tag.putInt("Wages", wagesToday);
        tag.putInt("Sales", salesToday);
        tag.putInt("Rent", rentToday);
        tag.putInt("SalesY", salesYesterday);
    }
}
