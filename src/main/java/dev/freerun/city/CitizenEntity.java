package dev.freerun.city;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * A city resident. Everyone has a name, an age, a job, a home and a bank balance, and lives a day:
 *
 *   6–8 AM    commute to work           8 AM–4 PM  at work (paid when the shift ends)
 *   4–7 PM    errands: shopping, the park, the Roadhouse (spends money at the stores)
 *   7 PM–6 AM home (pays rent), stays in
 *
 * Retirees, students and the unemployed keep their own hours. Some citizens drive the city's
 * cars instead and just cruise. Your phone can profile any of them (and, well, hack them).
 */
public class CitizenEntity extends PathfinderMob {
    public enum Job {
        OFFICE_WORKER("Office worker", 140, CityLayout.District.OFFICE),
        ACCOUNTANT("Accountant", 160, CityLayout.District.OFFICE),
        CASHIER("Cashier", 90, CityLayout.District.DOUBLE_QUICK),
        SHOP_CLERK("Shop clerk", 85, CityLayout.District.GENERAL_STORE),
        LINE_COOK("Line cook", 95, CityLayout.District.ROADHOUSE),
        WELDER("Welder", 150, CityLayout.District.FACTORY),
        NURSE("Nurse", 135, CityLayout.District.PARK),
        TEACHER("Teacher", 110, CityLayout.District.PARK),
        STUDENT("Student", 0, CityLayout.District.PARK),
        RETIREE("Retiree", 55, CityLayout.District.PARK),
        RIDESHARE_DRIVER("Rideshare driver", 100, null),
        MUSICIAN("Street musician", 40, CityLayout.District.PARK),
        UNEMPLOYED("Between jobs", 0, CityLayout.District.HOOD),
        IT_TECH("IT technician", 150, CityLayout.District.OFFICE),
        JANITOR("Janitor", 85, CityLayout.District.OFFICE),
        MANAGER("Office manager", 210, CityLayout.District.OFFICE),
        RECEPTIONIST("Receptionist", 95, CityLayout.District.OFFICE),
        SALES_REP("Sales rep", 130, CityLayout.District.OFFICE);

        public static final Job[] OFFICE = {OFFICE_WORKER, ACCOUNTANT, IT_TECH, MANAGER, SALES_REP, OFFICE_WORKER, ACCOUNTANT, RECEPTIONIST, JANITOR};

        public final String title;
        public final int wage;
        public final CityLayout.District work;

        Job(String title, int wage, CityLayout.District work) {
            this.title = title;
            this.wage = wage;
            this.work = work;
        }
    }

    private static final String[] FIRST = {"Marcus", "Dana", "Luis", "Priya", "Tom", "Keisha", "Wei", "Hannah", "Andre", "Sofia",
            "Jake", "Nadia", "Omar", "Grace", "Tyrell", "Molly", "Raj", "Bethany", "Diego", "Aisha", "Cole", "Mei", "Darnell",
            "Rosa", "Ethan", "Lena", "Kofi", "Megan", "Ivan", "Brianna", "Hector", "Joy", "Pierre", "Tasha", "Ben", "Yuki"};
    private static final String[] LAST = {"Thibodeaux", "Nguyen", "Garcia", "Johnson", "Patel", "Landry", "Kim", "Washington",
            "Hebert", "Robinson", "Okafor", "Broussard", "Martinez", "Chen", "Williams", "Guidry", "Lopez", "Davis",
            "Fontenot", "Singh", "Jackson", "Arceneaux", "Rossi", "Brown", "LeBlanc", "Ali", "Miller", "Doucet"};
    public static final String[] FACTS = {
            "Owes $340 in library fines", "Has a 1,200-day streak on a language app", "Secretly can't stand boudin",
            "Collects vintage forklift manuals", "Has been 'about to start a podcast' for 3 years", "Afraid of geese",
            "Won a hot dog eating contest in 2019", "Still uses a flip phone for 'security'", "Has 47 unread voicemails",
            "Runs a fan page for the overhead crane", "Can name every Double Quick location", "Allergic to cats, owns four",
            "Thinks the Rust Rats are a band", "Writes one-star reviews of the weather", "Has never once returned a cart",
            "Practises karaoke in the car every day", "Lost a bet and now has to say 'cher' a lot", "Night owl: 4 hours of sleep",
            "Has a lucky scratch-off they refuse to scratch", "Recently searched: 'how to look busy at work'",
            "Has been mistaken for Ethan James Patrick", "Puts hot sauce on everything", "Volunteers at the animal shelter",
            "Is training for a marathon (has run once)", "Keeps a spare tyre in the living room"};
    private static final String[] GREETINGS = {"Morning!", "Hey there.", "Oh, hi.", "How's it going?", "Can't talk long.", "Hey neighbour."};

    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(CitizenEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_JOB = SynchedEntityData.defineId(CitizenEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_AGE = SynchedEntityData.defineId(CitizenEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_MONEY = SynchedEntityData.defineId(CitizenEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_FACT = SynchedEntityData.defineId(CitizenEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DISTRACTED = SynchedEntityData.defineId(CitizenEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_TYPING = SynchedEntityData.defineId(CitizenEntity.class, EntityDataSerializers.BOOLEAN);

    private enum Plan { WORK, ERRAND, HOME, WANDER }

    private BlockPos city;
    private BlockPos home;
    private BlockPos work;
    private BlockPos goal;
    private Plan plan = Plan.WANDER;
    private int planDay = -1;
    private boolean paidToday, rentToday, shoppedToday;
    private int stuck;
    private Vec3 lastPos = Vec3.ZERO;
    private boolean arrived;
    private boolean errandAtShop;
    private long hackedUntil;
    /** Brought out by the city manager to fill the streets; tidied away again when nobody's near. */
    private boolean ambient;
    /** Set while running from a crime or the police. */
    private int fleeing;

    public CitizenEntity(EntityType<? extends CitizenEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIANT, 0);
        builder.define(DATA_JOB, 0);
        builder.define(DATA_AGE, 30);
        builder.define(DATA_MONEY, 100);
        builder.define(DATA_FACT, 0);
        builder.define(DATA_DISTRACTED, 0);
        builder.define(DATA_TYPING, false);
    }

    // ------------------------------------------------------------------ identity

    public int variant() { return entityData.get(DATA_VARIANT); }
    public Job job() { return Job.values()[Mth.clamp(entityData.get(DATA_JOB), 0, Job.values().length - 1)]; }
    public int age() { return entityData.get(DATA_AGE); }
    public int money() { return entityData.get(DATA_MONEY); }
    public String fact() { return FACTS[Math.floorMod(entityData.get(DATA_FACT), FACTS.length)]; }
    public boolean distracted() { return entityData.get(DATA_DISTRACTED) > 0; }

    public void setMoney(int m) {
        entityData.set(DATA_MONEY, Math.max(0, m));
    }

    /** Makes up a whole person. {@code home} is where they live (world pos), {@code city} the city origin. */
    public void setup(RandomSource r, BlockPos city, BlockPos home, Job job) {
        this.city = city == null ? null : city.immutable();
        this.home = home.immutable();
        entityData.set(DATA_JOB, job.ordinal());
        int age = switch (job) {
            case STUDENT -> 16 + r.nextInt(7);
            case RETIREE -> 66 + r.nextInt(25);
            default -> 21 + r.nextInt(42);
        };
        entityData.set(DATA_AGE, age);
        entityData.set(DATA_MONEY, 40 + r.nextInt(job.wage * 3 + 120));
        entityData.set(DATA_FACT, r.nextInt(FACTS.length));
        int variant = switch (job) {
            case OFFICE_WORKER, ACCOUNTANT, MANAGER, SALES_REP -> r.nextInt(2);   // suits
            case IT_TECH, RECEPTIONIST -> 5 + r.nextInt(3);
            case JANITOR -> 3;
            case NURSE -> 2;
            case WELDER -> 3;
            case RETIREE -> 4;
            case STUDENT -> 5;
            default -> 5 + r.nextInt(3);                             // casual
        };
        entityData.set(DATA_VARIANT, variant);
        setCustomName(Component.literal(FIRST[r.nextInt(FIRST.length)] + " " + LAST[r.nextInt(LAST.length)]));
        if (job.work != null && city != null) {
            List<BlockPos> spots = CityLayout.spotsNear(job.work, home.getX() - city.getX(), home.getZ() - city.getZ(), 180);
            if (spots.isEmpty()) spots = CityLayout.spots(job.work);
            if (!spots.isEmpty()) work = city.offset(spots.get(r.nextInt(spots.size())));
        }
    }

    public BlockPos cityOrigin() {
        return city;
    }

    public BlockPos home() {
        return home;
    }

    public boolean ambient() {
        return ambient;
    }

    public void setAmbient(boolean ambient) {
        this.ambient = ambient;
    }

    /** Run away from {@code from} for a few seconds (a crime, gunfire, a speeding car). */
    public void flee(Vec3 from) {
        if (isPassenger()) return;
        fleeing = 100;
        Vec3 away = position().subtract(from);
        if (away.lengthSqr() < 0.01) away = new Vec3(1, 0, 0);
        away = away.normalize().scale(14.0);
        getNavigation().moveTo(getX() + away.x, getY(), getZ() + away.z, 1.5);
    }

    // ------------------------------------------------------------------ AI

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.4));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7, 0.008f));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, SpawnGroupData data) {
        SpawnGroupData out = super.finalizeSpawn(level, difficulty, reason, data);
        if (home == null) {
            Job[] jobs = Job.values();
            setup(random, CityWorld.cityAt(level.getLevel(), blockPosition()), blockPosition(), jobs[random.nextInt(jobs.length)]);
        }
        return out;
    }

    private Plan planFor(long dayTime) {
        Job j = job();
        long t = dayTime % 24000L;
        boolean night = t >= 13000L;
        if (night) return Plan.HOME;
        if (j == Job.UNEMPLOYED || j == Job.MUSICIAN) return t < 6000L ? Plan.WANDER : Plan.ERRAND;
        if (t < 10000L) return work != null ? Plan.WORK : Plan.WANDER;
        return Plan.ERRAND;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel server)) return;
        if (officeSeat != null) {
            officeDay(server);
            return;
        }
        if (isPassenger()) return;
        if (fleeing > 0) {
            fleeing--;
            return;
        }
        int distracted = entityData.get(DATA_DISTRACTED);
        if (distracted > 0) {
            entityData.set(DATA_DISTRACTED, distracted - 1);
            getNavigation().stop();
            setXRot(40.0f);
            if (distracted % 40 == 0) playSound(SoundEvents.NOTE_BLOCK_CHIME.value(), 0.5f, 1.8f);
            return;
        }
        if (home == null) home = blockPosition();
        long day = level().getDayTime() / 24000L;
        if (day != planDay) {
            planDay = (int) day;
            paidToday = rentToday = shoppedToday = false;
        }
        Plan want = planFor(level().getDayTime());
        if (want != plan) {
            // finishing a shift: get paid
            if (plan == Plan.WORK && !paidToday && job().wage > 0) {
                paidToday = true;
                setMoney(money() + job().wage);
                CityWorld.recordWages(server, blockPosition(), job().wage);
            }
            plan = want;
            arrived = false;
            stuck = 0;
            goal = switch (plan) {
                case WORK -> work;
                case HOME -> home;
                case ERRAND -> errandSpot(server);
                case WANDER -> null;
            };
            clearRestriction();
        }
        if (goal == null) {
            if (!hasRestriction()) restrictTo(home, 24);
            return;
        }
        if (!arrived) {
            if (walk(goal)) {
                arrived = true;
                restrictTo(goal, plan == Plan.HOME ? 3 : 8);
                onArrive(server);
            }
        }
    }

    private static final CityLayout.District[] STORES = {CityLayout.District.DOUBLE_QUICK, CityLayout.District.GENERAL_STORE,
            CityLayout.District.MEGAMART, CityLayout.District.DEPARTMENT, CityLayout.District.STRIP_MALL, CityLayout.District.BURGER,
            CityLayout.District.CHICKEN, CityLayout.District.DESSERT, CityLayout.District.ROADHOUSE};

    private BlockPos errandSpot(ServerLevel server) {
        errandAtShop = false;
        if (city == null) return home;
        double lx = getX() - city.getX(), lz = getZ() - city.getZ();
        if (random.nextInt(3) != 0) {
            List<BlockPos> shops = new java.util.ArrayList<>();
            for (CityLayout.District d : STORES) shops.addAll(CityLayout.spotsNear(d, lx, lz, 150));
            if (!shops.isEmpty()) {
                errandAtShop = true;
                return city.offset(shops.get(random.nextInt(shops.size())));
            }
        }
        List<BlockPos> park = CityLayout.spotsNear(job() == Job.UNEMPLOYED ? CityLayout.District.HOOD : CityLayout.District.PARK, lx, lz, 160);
        return park.isEmpty() ? home : city.offset(park.get(random.nextInt(park.size())));
    }

    private void onArrive(ServerLevel server) {
        if (plan == Plan.ERRAND && !shoppedToday && errandAtShop && money() > 5) {
            shoppedToday = true;
            int spend = Math.min(money(), 5 + random.nextInt(26));
            setMoney(money() - spend);
            CityWorld.recordSale(server, blockPosition(), spend);
        }
        if (plan == Plan.HOME && !rentToday) {
            rentToday = true;
            int rent = Math.min(money(), 15 + random.nextInt(15));
            setMoney(money() - rent);
            CityWorld.recordRent(server, blockPosition(), rent);
        }
    }

    /** One step of a long walk, in short hops so the pathfinder never gives up. */
    private boolean walk(BlockPos to) {
        Vec3 target = Vec3.atBottomCenterOf(to);
        Vec3 here = position();
        double dx = target.x - here.x, dz = target.z - here.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 2.5 && Math.abs(target.y - here.y) < 3) return true;
        if (tickCount % 10 != 0) return false;
        if (here.distanceToSqr(lastPos) < 0.04) stuck += 10;
        else stuck = Math.max(0, stuck - 5);
        lastPos = here;
        if (stuck > 400 && level().getNearestPlayer(this, 24.0) == null) {   // hopelessly stuck and nobody's watching
            moveTo(target.x, target.y, target.z, getYRot(), 0.0f);
            stuck = 0;
            return true;
        }
        Vec3 hop = dist > 14.0 ? new Vec3(here.x + dx / dist * 12.0, here.y, here.z + dz / dist * 12.0) : target;
        if (getNavigation().isDone() || tickCount % 60 == 0) getNavigation().moveTo(hop.x, hop.y, hop.z, 1.0);
        return false;
    }

    // ------------------------------------------------------------------ a day at the office

    private static final String[] OFFICE_LINES = {"Can't talk, I've got a deadline.", "Did you get my email? I sent it twice.",
            "Who schedules a meeting at 4:55 on a Friday?", "Somebody hit reply-all again.", "Have you seen my stapler?",
            "I'm just waiting for this spreadsheet to load. It's been an hour.", "Per my last email...",
            "The coffee in the break room is basically tar today.", "I'm on mute. No wait, I'm not.", "Let's take this offline."};
    private BlockPos officeSeat;
    private BlockPos officeErrand;
    private int officeTimer;

    public boolean typing() {
        return entityData.get(DATA_TYPING);
    }

    /** Gives this person a desk: they sit and work at the chair at {@code chair} until quitting time. */
    public void setOffice(BlockPos chair) {
        officeSeat = chair == null ? null : chair.immutable();
        officeTimer = 200 + random.nextInt(1200);
    }

    public BlockPos officeSeat() {
        return officeSeat;
    }

    private void officeDay(ServerLevel server) {
        long t = level().getDayTime() % 24000L;
        if (t > 11500L) {   // quitting time
            stopRiding();
            entityData.set(DATA_TYPING, false);
            officeSeat = null;
            return;
        }
        boolean seated = getVehicle() instanceof dev.freerun.world.SeatEntity;
        entityData.set(DATA_TYPING, seated && officeErrand == null);
        if (seated) {
            if (tickCount % 30 == 0 && random.nextInt(3) == 0) {
                playSound(SoundEvents.WOODEN_BUTTON_CLICK_ON, 0.15f, 1.8f + random.nextFloat() * 0.2f);   // clack clack
            }
            if (--officeTimer <= 0) {
                officeErrand = errand(server);
                stopRiding();
                officeTimer = 100 + random.nextInt(140);
            }
            return;
        }
        if (officeErrand != null) {
            if (distanceToSqr(Vec3.atBottomCenterOf(officeErrand)) > 2.5 * 2.5 && officeTimer > 0) {
                if (getNavigation().isDone() || tickCount % 40 == 0) getNavigation().moveTo(officeErrand.getX() + 0.5, officeErrand.getY(), officeErrand.getZ() + 0.5, 0.8);
                officeTimer--;
                return;
            }
            getNavigation().stop();
            if (--officeTimer < -80) officeErrand = null;   // done chatting at the cooler
            return;
        }
        // back to the desk
        if (distanceToSqr(Vec3.atBottomCenterOf(officeSeat)) > 1.6 * 1.6) {
            if (getNavigation().isDone() || tickCount % 40 == 0) getNavigation().moveTo(officeSeat.getX() + 0.5, officeSeat.getY(), officeSeat.getZ() + 0.5, 0.8);
            if (++stuck > 400) {
                moveTo(officeSeat.getX() + 0.5, officeSeat.getY(), officeSeat.getZ() + 0.5);
                stuck = 0;
            }
            return;
        }
        stuck = 0;
        var state = level().getBlockState(officeSeat);
        if (state.getBlock() instanceof dev.freerun.life.FurnitureBlock fb && fb.seat()
                && level().getEntitiesOfClass(dev.freerun.world.SeatEntity.class, new net.minecraft.world.phys.AABB(officeSeat)).isEmpty()) {
            dev.freerun.world.SeatEntity seat = dev.freerun.world.SeatEntity.create(level(), officeSeat);
            level().addFreshEntity(seat);
            startRiding(seat, true);
            float yaw = state.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING).toYRot();
            setYRot(yaw);
            setYHeadRot(yaw);
            setYBodyRot(yaw);
            officeTimer = 600 + random.nextInt(1800);
        } else if (!(state.getBlock() instanceof dev.freerun.life.FurnitureBlock)) {
            officeSeat = null;   // their chair is gone
        } else {
            officeTimer = 200;
            officeErrand = errand(server);
        }
    }

    /** Somewhere to wander off to: the water cooler, the printer, the whiteboard, or a coworker's desk. */
    private BlockPos errand(ServerLevel server) {
        List<BlockPos> spots = new java.util.ArrayList<>();
        for (BlockPos q : BlockPos.betweenClosed(blockPosition().offset(-10, -1, -10), blockPosition().offset(10, 1, 10))) {
            var st = server.getBlockState(q);
            if (st.is(dev.freerun.life.LifeRegistry.WATER_COOLER) || st.is(dev.freerun.life.LifeRegistry.PRINTER)
                    || st.is(dev.freerun.life.LifeRegistry.WHITEBOARD) || st.is(dev.freerun.life.LifeRegistry.OFFICE_CHAIR) && random.nextInt(6) == 0) {
                spots.add(q.immutable());
            }
        }
        if (spots.isEmpty()) return officeSeat.offset(random.nextInt(7) - 3, 0, random.nextInt(7) - 3);
        BlockPos at = spots.get(random.nextInt(spots.size()));
        // stand next to it, not in it
        for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            BlockPos side = at.relative(d);
            if (server.getBlockState(side).isAir() && server.getBlockState(side.above()).isAir()) return side;
        }
        return at;
    }

    // ------------------------------------------------------------------ hacks (see Hacking)

    public boolean recentlyHacked() {
        return level().getGameTime() < hackedUntil;
    }

    public int drain(int max) {
        int take = Math.min(money(), max);
        setMoney(money() - take);
        hackedUntil = level().getGameTime() + 6000;   // their bank flags the account for 5 minutes
        return take;
    }

    public void distract(int ticks) {
        entityData.set(DATA_DISTRACTED, ticks);
        getNavigation().stop();
    }

    // ------------------------------------------------------------------ talking

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!level().isClientSide && player instanceof net.minecraft.server.level.ServerPlayer sp && dev.freerun.life.OfficeJobs.talk(sp, this)) {
            return InteractionResult.SUCCESS;
        }
        if (!level().isClientSide && officeSeat != null) {
            player.displayClientMessage(Component.literal("§f<" + getName().getString() + "> " + OFFICE_LINES[random.nextInt(OFFICE_LINES.length)]
                    + " §8(" + job().title + ")"), false);
            return InteractionResult.SUCCESS;
        }
        if (!level().isClientSide) {
            Job j = job();
            String line = GREETINGS[random.nextInt(GREETINGS.length)] + " " + switch (plan) {
                case WORK -> j == Job.STUDENT ? "Late for class!" : "On my way to work. " + j.title + ", if you're wondering.";
                case HOME -> "Heading home. Long day.";
                case ERRAND -> "Just running errands.";
                case WANDER -> j == Job.RETIREE ? "Just enjoying the day. I'm retired, you know." : "Just out for a walk.";
            };
            if (distracted()) line = "*staring at their phone* ...huh? Sorry, my phone's going crazy.";
            player.displayClientMessage(Component.literal("§f<" + getName().getString() + "> " + line), false);
            getLookControl().setLookAt(player, 30.0f, 30.0f);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && source.getEntity() != null) {
            flee(source.getEntity().position());
            if (source.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp) Police.reportAssault(sp, this);
        }
        if (hurt && !level().isClientSide && random.nextInt(2) == 0) {
            String[] yells = {"HEY! What's your problem?!", "Somebody call the cops!", "Ow! Are you crazy?!", "HELP!"};
            Component msg = Component.literal("§f<" + getName().getString() + "> §c" + yells[random.nextInt(yells.length)]);
            for (Player p : level().players()) if (distanceToSqr(p) < 16 * 16) p.displayClientMessage(msg, false);
        }
        return hurt;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        int cash = Math.min(money(), 5 + random.nextInt(25));
        for (net.minecraft.world.item.ItemStack bill : dev.freerun.economy.Money.bills(cash)) spawnAtLocation(bill);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", variant());
        tag.putInt("Job", entityData.get(DATA_JOB));
        tag.putInt("Age", age());
        tag.putInt("Money", money());
        tag.putInt("Fact", entityData.get(DATA_FACT));
        put(tag, "Home", home);
        put(tag, "Work", work);
        put(tag, "City", city);
        tag.putLong("HackedUntil", hackedUntil);
        tag.putBoolean("Ambient", ambient);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_VARIANT, tag.getInt("Variant"));
        entityData.set(DATA_JOB, tag.getInt("Job"));
        entityData.set(DATA_AGE, tag.getInt("Age"));
        entityData.set(DATA_MONEY, tag.getInt("Money"));
        entityData.set(DATA_FACT, tag.getInt("Fact"));
        home = get(tag, "Home");
        work = get(tag, "Work");
        city = get(tag, "City");
        hackedUntil = tag.getLong("HackedUntil");
        ambient = tag.getBoolean("Ambient");
    }

    private static void put(CompoundTag tag, String k, BlockPos p) {
        if (p == null) return;
        tag.putInt(k + "X", p.getX());
        tag.putInt(k + "Y", p.getY());
        tag.putInt(k + "Z", p.getZ());
    }

    private static BlockPos get(CompoundTag tag, String k) {
        return tag.contains(k + "X") ? new BlockPos(tag.getInt(k + "X"), tag.getInt(k + "Y"), tag.getInt(k + "Z")) : null;
    }
}
