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
import net.minecraft.world.phys.Vec3;

/**
 * Ethan James Patrick: as tall and skinny as an enderman (but nowhere near as quick). He wanders
 * the factory floor and asks whoever's nearby whatever's on his mind.
 *
 * He is also, secretly, the leader of the Rust Rats. They never shoot him, they come running if
 * anyone hurts him, and every so often he forgets himself and gets on the walkie-talkie.
 *
 * Every few minutes Supervisor Slater and Safety Nick come down to the floor to yell at him to get
 * off the crane, which ends with him landing on Nick and shaking Big Boudin loose.
 */
public class EthanEntity extends PathfinderMob {
    static final String[] QUESTIONS = {
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
    static final String[] GENIUS = {
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

    private static final String[] WALKIE = {
            "The cheese is in the mousetrap. Repeat, the cheese is in the mousetrap.",
            "Hold position by the dumpster. Act natural.",
            "Nobody touches the Kings' cars until I say so.",
            "Tonight we hit the Double Quick. Bring exact change, we're not animals.",
            "The supervisor suspects nothing. Over.",
            "Remember: if anyone asks, I'm just a guy who works here."
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
    // romance
    private int loveTicks;
    private int breedCooldown;
    // the Slater & Nick visit
    private int visitTimer = 3000 + 1200;
    private int visitStage;
    private int visitT;
    private RoadhouseNpcEntity slater;
    private RoadhouseNpcEntity nick;
    private BlockPos door;
    private boolean ratsRallied;

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
        if (!genius && random.nextInt(12) == 0 && ratsNearby(64.0)) {
            // Forgot the walkie-talkie was on.
            Component a = Component.literal("§6*quietly, into a walkie-talkie* §fRat King to all Rats. "
                    + WALKIE[random.nextInt(WALKIE.length)]);
            Component b = Component.literal("§b<Ethan James Patrick> §f...What? I didn't say anything. What am I supposed to do here?");
            for (Player p : level().players()) {
                if (p == player || distanceToSqr(p) < 10 * 10) {
                    p.displayClientMessage(a, false);
                    p.displayClientMessage(b, false);
                }
            }
            playSound(SoundEvents.NOTE_BLOCK_BIT.value(), 0.6f, 0.5f);
            askCooldown = 700 + random.nextInt(500);
            return;
        }
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
                if (breedCooldown > 0) {
                    player.displayClientMessage(Component.literal(
                            "§b<Ethan James Patrick> §fOh, for me? ...Am I allowed to eat on the floor?"), false);
                } else {
                    player.getItemInHand(hand).consume(1, player);
                    playSound(SoundEvents.GENERIC_EAT, 1.0f, 0.9f);
                    loveTicks = 600;
                    broadcast("*munch* ...This is good boudin. You know who'd love this? Johnny. Where's Johnny?");
                }
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
                if (visitStage == 0) broadcast(rageTicks > 0 ? "Okay, coming down. COMING DOWN." : "Alright. I've seen enough. Coming down.");
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
        if (--climbCooldown > 0 || rageTicks > 0 || loveTicks > 0) return false;
        climbCooldown = 3600 + random.nextInt(3600);   // every 3 to 6 minutes
        dev.freerun.factory.CraneEntity crane = findCrane();
        if (crane == null) return false;
        startRide(crane, 600 + random.nextInt(600));
        return true;
    }

    private dev.freerun.factory.CraneEntity findCrane() {
        return level().getEntitiesOfClass(dev.freerun.factory.CraneEntity.class,
                getBoundingBox().inflate(32.0, 12.0, 32.0)).stream().findFirst().orElse(null);
    }

    private void startRide(dev.freerun.factory.CraneEntity crane, int ticks) {
        if (level() instanceof net.minecraft.server.level.ServerLevel server) {
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, getX(), getY(), getZ(), 12, 0.3, 0.1, 0.3, 0.05);
        }
        playSound(SoundEvents.GOAT_LONG_JUMP, 1.0f, 0.8f);
        riding = crane;
        craneTicks = ticks;
        setNoGravity(true);
        entityData.set(DATA_HANGING, true);
        broadcast("*jumps straight up and grabs the crane* Whoa. You can see EVERYTHING from up here.");
    }

    // ------------------------------------------------------------------ the plasma cannon

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (source.getEntity() instanceof dev.freerun.gang.GangsterEntity g && g.crew() == dev.freerun.gang.Crew.RUST_RATS) {
            return false;   // the Rats would never
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && source.getEntity() instanceof net.minecraft.world.entity.LivingEntity who
                && who != this && !(who instanceof Player p0 && p0.isCreative())) {
            rallyRats(who);
        }
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

    // ------------------------------------------------------------------ the Rust Rats

    private boolean ratsNearby(double range) {
        return !level().getEntitiesOfClass(dev.freerun.gang.GangsterEntity.class, getBoundingBox().inflate(range),
                g -> g.crew() == dev.freerun.gang.Crew.RUST_RATS).isEmpty();
    }

    private void rallyRats(net.minecraft.world.entity.LivingEntity attacker) {
        if (attacker instanceof dev.freerun.gang.GangsterEntity g && g.crew() == dev.freerun.gang.Crew.RUST_RATS) return;
        boolean any = false;
        for (dev.freerun.gang.GangsterEntity g : level().getEntitiesOfClass(dev.freerun.gang.GangsterEntity.class,
                getBoundingBox().inflate(40.0), g -> g.crew() == dev.freerun.gang.Crew.RUST_RATS)) {
            g.setTarget(attacker);
            any = true;
        }
        if (any && !ratsRallied) {
            ratsRallied = true;
            Component msg = Component.literal("§6<Rust Rat> §fSOMEBODY TOUCHED THE BOSS! GET 'EM!");
            for (Player p : level().players()) if (distanceToSqr(p) < 48 * 48) p.displayClientMessage(msg, false);
        }
    }

    @Override
    public void die(net.minecraft.world.damagesource.DamageSource source) {
        if (!level().isClientSide && ratsNearby(64.0)) {
            Component msg = Component.literal("§6*a walkie-talkie crackles somewhere* §7\"Boss? ...Boss? Rats, the King is down. Find who did it.\"");
            for (Player p : level().players()) if (distanceToSqr(p) < 64 * 64) p.displayClientMessage(msg, false);
            if (source.getEntity() instanceof net.minecraft.world.entity.LivingEntity killer) rallyRats(killer);
        }
        super.die(source);
    }

    // ------------------------------------------------------------------ boudin brings people together

    private void tickLove() {
        if (breedCooldown > 0) breedCooldown--;
        if (loveTicks <= 0 || !(level() instanceof net.minecraft.server.level.ServerLevel server)) return;
        loveTicks--;
        if (loveTicks % 10 == 0) server.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART, getX(), getY() + 3.0, getZ(),
                1, 0.3, 0.2, 0.3, 0.0);
        JohnnySimEntity johnny = level().getEntitiesOfClass(JohnnySimEntity.class, getBoundingBox().inflate(32.0),
                JohnnySimEntity::inLove).stream().findFirst().orElse(null);
        if (johnny == null) return;
        if (distanceToSqr(johnny) > 3.5 * 3.5) {
            if (tickCount % 20 == 0) getNavigation().moveTo(johnny.getX(), johnny.getY(), johnny.getZ(), 1.2);
            return;
        }
        loveTicks = 0;
        breedCooldown = 6000;
        johnny.bred();
        JohnnyJamesPatrickEntity baby = new JohnnyJamesPatrickEntity(MoreMobs.JOHNNY_JAMES_PATRICK, server);
        baby.moveTo(getX(), getY(), getZ(), getYRot(), 0.0f);
        baby.makeBaby(home != null ? home : blockPosition());
        server.addFreshEntity(baby);
        server.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART, getX(), getY() + 1.5, getZ(), 12, 0.8, 0.6, 0.8, 0.0);
        playSound(SoundEvents.PLAYER_LEVELUP, 1.0f, 1.2f);
        Component msg = Component.literal("§d♥ §fEthan James Patrick and Johnny Sim had a baby! Say hello to §eJohnny James Patrick§f.");
        for (Player p : level().players()) if (distanceToSqr(p) < 64 * 64) p.displayClientMessage(msg, false);
    }

    // ------------------------------------------------------------------ Slater and Nick come down to the floor

    private void say(RoadhouseNpcEntity who, String line) {
        if (who != null && who.isAlive()) who.shout(line);
    }

    private RoadhouseNpcEntity spawnVisitor(net.minecraft.server.level.ServerLevel server, RoadhouseNpcEntity.Role role, double side) {
        RoadhouseNpcEntity npc = new RoadhouseNpcEntity(SoldierRegistry.ROADHOUSE_NPC, server);
        npc.setRole(role, 0);
        npc.setVisitor(true);
        npc.moveTo(door.getX() + 0.5 + side, door.getY(), door.getZ() + 0.5, 180.0f, 0.0f);
        if (!server.noCollision(npc)) npc.moveTo(getX() + side + 3.0, home.getY(), getZ() + 3.0, 0.0f, 0.0f);
        server.addFreshEntity(npc);
        return npc;
    }

    /** Returns true while the visit has Ethan's full attention. */
    private boolean tickVisit() {
        if (!(level() instanceof net.minecraft.server.level.ServerLevel server)) return false;
        if (visitStage == 0) {
            if (--visitTimer > 0) return false;
            visitTimer = 3600 + random.nextInt(2400);   // every 3 to 5 minutes
            if (home == null || rageTicks > 0 || level().getNearestPlayer(this, 48.0) == null) return false;
            dev.freerun.factory.CraneEntity crane = findCrane();
            if (crane == null) return false;
            if (riding == null) startRide(crane, 2400);
            else craneTicks = Math.max(craneTicks, 2400);
            door = home.offset(0, 0, -14);
            slater = spawnVisitor(server, RoadhouseNpcEntity.Role.SUPERVISOR, -0.7);
            nick = spawnVisitor(server, RoadhouseNpcEntity.Role.SAFETY, 0.7);
            Component msg = Component.literal("§7*The office door bangs open. Supervisor Slater and Safety Nick storm onto the floor.*");
            for (Player p : level().players()) if (distanceToSqr(p) < 64 * 64) p.displayClientMessage(msg, false);
            visitStage = 1;
            visitT = 0;
            return false;
        }
        if (slater == null || nick == null || !slater.isAlive() || !nick.isAlive()) {
            endVisit();
            return false;
        }
        slater.keepVisiting();
        nick.keepVisiting();
        visitT++;
        double floorY = home.getY();
        switch (visitStage) {
            case 1 -> {   // marching over to the crane
                if (riding != null) craneTicks = Math.max(craneTicks, 400);
                double tx = getX(), tz = getZ();
                if (visitT % 20 == 1) {
                    slater.getNavigation().moveTo(tx - 1.2, floorY, tz + 1.6, 1.1);
                    nick.getNavigation().moveTo(tx + 1.2, floorY, tz + 1.6, 1.1);
                }
                boolean there = slater.distanceToSqr(tx - 1.2, slater.getY(), tz + 1.6) < 9.0
                        && nick.distanceToSqr(tx + 1.2, nick.getY(), tz + 1.6) < 9.0;
                if (visitT > 400 && !there) {
                    slater.moveTo(tx - 1.2, floorY, tz + 1.6, 180.0f, 0.0f);
                    nick.moveTo(tx + 1.2, floorY, tz + 1.6, 180.0f, 0.0f);
                    there = true;
                }
                if (there) {
                    visitStage = 2;
                    visitT = 0;
                }
                return false;
            }
            case 2 -> {   // the yelling
                slater.getNavigation().stop();
                nick.getNavigation().stop();
                slater.getLookControl().setLookAt(this, 30.0f, 60.0f);
                nick.getLookControl().setLookAt(this, 30.0f, 60.0f);
                if (riding != null) {
                    craneTicks = Math.max(craneTicks, 100);
                    tickCrane();   // keep hanging from the girder while they yell
                }
                switch (visitT) {
                    case 1 -> say(slater, "§cETHAN! What the hell are you doing up on that crane?!");
                    case 50 -> say(nick, "§cThat is a MAJOR safety violation! Get the hell down from there!");
                    case 100 -> say(slater, "§cHurry up and get down before I fire you! I am NOT kidding this time!");
                    case 140 -> say(nick, "§cYou are NOT tied off! Where is your harness?!");
                    case 170 -> broadcast("Okay okay okay! I'm coming down! Don't fire me!");
                    case 200 -> {   // he lets go... right over Nick
                        riding = null;
                        setNoGravity(false);
                        entityData.set(DATA_HANGING, false);
                        setPos(nick.getX(), nick.getY() + 3.2, nick.getZ());
                        setDeltaMovement(0.0, -0.4, 0.0);
                        playSound(SoundEvents.GOAT_LONG_JUMP, 1.0f, 0.7f);
                    }
                    case 212 -> {
                        nick.hurt(damageSources().mobAttack(this), 2.0f);
                        server.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, nick.getX(), nick.getY() + 1.8, nick.getZ(),
                                16, 0.4, 0.3, 0.4, 0.2);
                        playSound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2f, 0.6f);
                        broadcast("*lands squarely on Nick* ...Oh. Sorry, Nick.");
                    }
                    case 230 -> say(nick, "§cOW! MY BACK! You landed right on—");
                    case 270 -> say(nick, "§e...Wait. Do you smell that? Smoked pork... Rice... Cayenne...");
                    case 310 -> say(nick, "§6§lIT'S BIG BOUDIN! THE PLANT LEGEND IS REAL!");
                    case 320 -> summonBigBoudin(server);
                    case 370 -> say(slater, "...I'm going back to my office. Ethan, you're on thin ice.");
                    case 390 -> {
                        visitStage = 3;
                        visitT = 0;
                    }
                    default -> { }
                }
                return true;
            }
            case 3 -> {   // walking off
                if (visitT % 20 == 1) {
                    slater.getNavigation().moveTo(door.getX() + 0.5, door.getY(), door.getZ() + 0.5, 1.0);
                    nick.getNavigation().moveTo(door.getX() + 0.5, door.getY(), door.getZ() + 0.5, 1.0);
                }
                boolean gone = slater.distanceToSqr(Vec3.atBottomCenterOf(door)) < 4.0 && nick.distanceToSqr(Vec3.atBottomCenterOf(door)) < 4.0;
                if (gone || visitT > 500) endVisit();
                return false;
            }
            default -> {
                endVisit();
                return false;
            }
        }
    }

    private void summonBigBoudin(net.minecraft.server.level.ServerLevel server) {
        if (!server.getEntitiesOfClass(BigBoudinEntity.class, getBoundingBox().inflate(64.0)).isEmpty()) return;
        BigBoudinEntity bb = new BigBoudinEntity(MoreMobs.BIG_BOUDIN, server);
        double a = random.nextDouble() * Math.PI * 2;
        bb.moveTo(nick.getX() + Math.cos(a) * 3.0, nick.getY(), nick.getZ() + Math.sin(a) * 3.0, random.nextFloat() * 360.0f, 0.0f);
        if (!server.noCollision(bb)) bb.moveTo(nick.getX(), nick.getY(), nick.getZ() + 2.5, 0.0f, 0.0f);
        server.addFreshEntity(bb);
        server.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, bb.getX(), bb.getY() + 1.5, bb.getZ(), 40, 0.8, 1.4, 0.8, 0.08);
        server.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, bb.getX(), bb.getY() + 1.5, bb.getZ(), 20, 0.6, 1.0, 0.6, 0.02);
        server.playSound(null, bb.blockPosition(), SoundEvents.RAVAGER_ROAR, net.minecraft.sounds.SoundSource.HOSTILE, 2.0f, 1.4f);
        server.playSound(null, bb.blockPosition(), SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, net.minecraft.sounds.SoundSource.HOSTILE, 2.0f, 0.6f);
    }

    private void endVisit() {
        if (slater != null) slater.discard();
        if (nick != null) nick.discard();
        slater = nick = null;
        visitStage = 0;
        visitT = 0;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (visitStage != 0) endVisit();
        super.remove(reason);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (home != null && !hasRestriction()) restrictTo(home, homeRadius);
        tickRage();
        if (rageTicks <= 0) ratsRallied = false;
        tickLove();
        if (tickVisit()) return;
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
