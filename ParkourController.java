package dev.freerun.client;

import dev.freerun.Freerun;
import dev.freerun.FreerunConfig;
import dev.freerun.ParkourState;
import dev.freerun.client.evil.CursedParkour;
import dev.freerun.evil.EvilConfig;
import dev.freerun.net.StatePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import static dev.freerun.ParkourState.*;

/**
 * Drives the local player's movement. Runs at the start of each client tick and overrides
 * the player's velocity while a move is active; vanilla physics then applies it.
 *
 * Controls (Mirror's Edge style, two buttons): JUMP = "up" actions, SNEAK = "down" actions.
 */
public final class ParkourController {
    private static final ResourceLocation MOMENTUM_ID = Freerun.id("momentum");
    /** Feet sit this far below a ledge while hanging (hands at the edge). */
    private static final double HANG_OFFSET = 2.0;

    private ParkourState state = NONE;
    private int t;
    private Vec3 wallDir = Vec3.ZERO;   // unit axis vector pointing INTO the wall
    private Vec3 along = Vec3.ZERO;     // wall-run direction
    private Vec3 moveDir = Vec3.ZERO;   // slide / roll direction
    private double speed;
    private double ledgeTop;
    private boolean wallRunAvailable = true;
    private boolean climbAvailable = true;
    private int grabCooldown;
    private int slideCooldown;
    private int airTicks;
    private int momentumTicks;
    private int turnTicks;
    private float turnPerTick;
    private boolean prevJump;
    private boolean prevSneak;
    private ParkourState lastSent = NONE;

    public ParkourState state() {
        return state;
    }

    public void reset() {
        state = NONE;
        lastSent = NONE;
        t = 0;
    }

    // ------------------------------------------------------------------ main loop

    public void tick(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || mc.isPaused()) return;
        FreerunConfig cfg = FreerunConfig.get();
        Options o = mc.options;

        boolean jumpDown = o.keyJump.isDown();
        boolean sneakDown = o.keyShift.isDown();
        Input in = new Input(jumpDown && !prevJump, sneakDown && !prevSneak, jumpDown, sneakDown,
                o.keyUp.isDown(), o.keyDown.isDown(), o.keyLeft.isDown(), o.keyRight.isDown());
        prevJump = jumpDown;
        prevSneak = sneakDown;

        if (!cfg.enabled || !canParkour(p)) {
            setState(p, NONE);
            applyMomentum(p, cfg, false);
            sync(p);
            return;
        }

        if (grabCooldown > 0) grabCooldown--;
        if (slideCooldown > 0) slideCooldown--;
        if (p.onGround()) {
            wallRunAvailable = true;
            climbAvailable = true;
            airTicks = 0;
        } else {
            airTicks++;
        }
        if (turnTicks > 0) {
            p.setYRot(p.getYRot() + turnPerTick);
            turnTicks--;
        }

        t++;
        switch (state) {
            case NONE -> tickNone(p, cfg, in);
            case WALLRUN_LEFT, WALLRUN_RIGHT -> tickWallRun(p, cfg, in);
            case WALL_CLIMB -> tickClimb(p, cfg, in);
            case LEDGE_HANG -> tickHang(p, in);
            case MANTLE -> tickMantle(p);
            case VAULT -> tickVault(p);
            case SLIDE -> tickSlide(p, in);
            case ROLL_READY -> tickRollReady(p);
            case ROLL -> tickRoll(p);
            case WALL_JUMP -> {
                if (t >= WALL_JUMP.duration) setState(p, NONE);
                else if (t > 2) tryAirMoves(p, cfg, in);
            }
        }

        boolean running = p.isSprinting() && in.forward && state == NONE;
        applyMomentum(p, cfg, running);
        applyGrudge(p);
        sync(p);
    }

    private int stillTicks;
    private float prevFall;

    /** Gravity's grudge: standing still builds an extra downward pull; a hard landing makes you limp. */
    private void applyGrudge(LocalPlayer p) {
        EvilConfig evil = EvilConfig.get();

        // limp detection: fall distance resets to 0 on landing
        if (prevFall > 0 && p.fallDistance == 0f && p.onGround()) {
            CursedParkour.maybeLimp(p, prevFall);
        }
        prevFall = p.fallDistance;

        if (!evil.evilMode || !evil.gravityGrudge) {
            stillTicks = 0;
            return;
        }
        boolean moving = horizontal(p.getDeltaMovement()).lengthSqr() > 0.004 || state != NONE;
        stillTicks = moving ? 0 : stillTicks + 1;
        if (stillTicks > evil.grudgeGraceTicks) {
            double growth = (stillTicks - evil.grudgeGraceTicks) / 40.0;
            Vec3 v = p.getDeltaMovement();
            p.setDeltaMovement(v.x, v.y - evil.grudgePull * Math.min(growth, 4.0), v.z);
        }
    }

    private record Input(boolean jumpPressed, boolean sneakPressed, boolean jump, boolean sneak,
                         boolean forward, boolean back, boolean left, boolean right) {}

    private static boolean canParkour(LocalPlayer p) {
        return !p.isSpectator() && !p.getAbilities().flying && !p.isInWater() && !p.isInLava()
                && !p.onClimbable() && !p.isFallFlying() && !p.isPassenger() && !p.isSleeping();
    }

    // ------------------------------------------------------------------ starting moves

    private void tickNone(LocalPlayer p, FreerunConfig cfg, Input in) {
        Vec3 fwd = axis(forward(p));
        double hs = horizontal(p.getDeltaMovement()).length();

        if (p.onGround()) {
            // Vault: waist-high obstacle while running
            if (cfg.vault && in.forward && ((cfg.autoVault && p.isSprinting()) || in.jumpPressed)
                    && blocked(p, fwd, 0.5, 0.1, 0.9)) {
                double top = findLedge(p, p.position(), fwd, 0.5, 1.3);
                if (!Double.isNaN(top)) {
                    wallDir = fwd;
                    ledgeTop = top;
                    speed = Math.max(hs, 0.3);
                    setState(p, VAULT);
                    return;
                }
            }
            // Wall climb: jump into a wall
            if (cfg.wallClimb && climbAvailable && in.jumpPressed && in.forward && blocked(p, fwd, 0.35, 0.5, 1.6)) {
                startClimb(p, fwd);
                return;
            }
            // Slide: crouch while sprinting
            if (cfg.slide && in.sneakPressed && slideCooldown == 0 && p.isSprinting() && hs > 0.15) {
                moveDir = horizontal(p.getDeltaMovement()).normalize();
                speed = Math.max(hs * 1.3, 0.42);
                setState(p, SLIDE);
            }
            return;
        }
        tryAirMoves(p, cfg, in);
    }

    private void tryAirMoves(LocalPlayer p, FreerunConfig cfg, Input in) {
        Vec3 fwd = axis(forward(p));
        Vec3 vel = p.getDeltaMovement();
        double hs = horizontal(vel).length();

        // Ledge grab: falling (or at the top of a jump) next to a ledge you're facing
        if (cfg.ledgeGrab && grabCooldown == 0 && in.forward && vel.y < 0.15 && blocked(p, fwd, 0.35, 0.6, 1.5)) {
            double top = findLedge(p, p.position(), fwd, 1.3, 2.4);
            if (!Double.isNaN(top)) {
                wallDir = fwd;
                ledgeTop = top;
                setState(p, LEDGE_HANG);
                return;
            }
        }
        // Wall climb from a jump that hits a wall while still rising
        if (cfg.wallClimb && climbAvailable && state == NONE && airTicks <= 10 && vel.y > 0 && in.forward
                && (p.isSprinting() || hs > 0.2) && blocked(p, fwd, 0.35, 0.5, 1.6)
                && forwardDotAxis(p, fwd) > 0.8) {
            startClimb(p, fwd);
            return;
        }
        // Wall run: wall beside you while running through the air
        if (cfg.wallRun && wallRunAvailable && grabCooldown == 0 && in.forward && hs > 0.18 && vel.y > -0.6) {
            Vec3 right = right(p);
            int side = blocked(p, axis(right), 0.3, 0.2, 1.5) ? 1
                    : blocked(p, axis(right.scale(-1)), 0.3, 0.2, 1.5) ? -1 : 0;
            if (side != 0) {
                Vec3 n = axis(right.scale(side));
                Vec3 f = forward(p);
                Vec3 al = f.subtract(n.scale(f.dot(n)));
                if (al.lengthSqr() > 0.3) {
                    wallDir = n;
                    along = al.normalize();
                    speed = Math.max(hs, 0.32);
                    setState(p, side > 0 ? WALLRUN_RIGHT : WALLRUN_LEFT);
                    return;
                }
            }
        }
        // Arm a landing roll
        if (cfg.roll && state == NONE && in.sneakPressed && vel.y < -0.3) {
            setState(p, ROLL_READY);
        }
    }

    private void startClimb(LocalPlayer p, Vec3 dir) {
        wallDir = dir;
        climbAvailable = false;
        setState(p, WALL_CLIMB);
    }

    // ------------------------------------------------------------------ active moves

    private void tickWallRun(LocalPlayer p, FreerunConfig cfg, Input in) {
        if (CursedParkour.shouldFail(state)) {   // the wall betrays you
            p.setDeltaMovement(wallDir.scale(0.1).add(0, -0.1, 0));
            wallRunAvailable = false;
            grabCooldown = 4;
            setState(p, NONE);
            return;
        }
        if (in.jumpPressed) {
            // Kick off the wall, keeping forward speed; another wall run is allowed afterwards
            Vec3 v = wallDir.scale(-0.42).add(along.scale(speed * 0.85));
            p.setDeltaMovement(v.x, 0.5, v.z);
            wallRunAvailable = true;
            grabCooldown = 4;
            setState(p, WALL_JUMP);
            return;
        }
        if (p.onGround() || !in.forward || in.sneak || t > cfg.wallRunTicks
                || !blocked(p, wallDir, 0.3, 0.2, 1.5)) {
            wallRunAvailable = false;
            grabCooldown = 4;
            setState(p, NONE);
            return;
        }
        double prog = t / (double) cfg.wallRunTicks;
        double dy = 0.1 - 0.3 * prog;        // slight rise, then an arc downwards
        Vec3 v = along.scale(speed).add(wallDir.scale(0.04));
        p.setDeltaMovement(v.x, dy, v.z);
        p.setSprinting(true);
        p.resetFallDistance();
    }

    private void tickClimb(LocalPlayer p, FreerunConfig cfg, Input in) {
        if (CursedParkour.shouldFail(state)) {   // grip fails
            p.setDeltaMovement(wallDir.scale(-0.15).add(0, -0.1, 0));
            grabCooldown = 6;
            setState(p, NONE);
            return;
        }
        if (in.sneak || (t > 2 && !in.forward)) {
            grabCooldown = 6;
            setState(p, NONE);
            return;
        }
        if (FreerunConfig.get().ledgeGrab) {
            double top = findLedge(p, p.position(), wallDir, 0.4, 2.3);
            if (!Double.isNaN(top)) {
                ledgeTop = top;
                setState(p, MANTLE);
                return;
            }
        }
        if (in.jumpPressed && t > 3) {
            wallKick(p);
            return;
        }
        if (t > cfg.wallClimbTicks || !blocked(p, wallDir, 0.35, 0.3, 1.7)) {
            grabCooldown = 3;
            setState(p, NONE);
            return;
        }
        double prog = t / (double) cfg.wallClimbTicks;
        double dy = 0.34 - 0.24 * prog;
        setVelocityCancellingInput(p, wallDir.x * 0.05, dy, wallDir.z * 0.05);
        p.resetFallDistance();
    }

    private void tickHang(LocalPlayer p, Input in) {
        // Cursed: the ledge crumbles after a short grace, or your grip randomly slips.
        if (t > CursedParkour.ledgeLifetime() || CursedParkour.shouldFail(state)) {
            p.setDeltaMovement(wallDir.scale(-0.12).add(0, -0.2, 0));
            grabCooldown = 12;
            setState(p, NONE);
            return;
        }
        if (in.sneakPressed) {                       // let go
            grabCooldown = 12;
            setState(p, NONE);
            return;
        }
        if (in.jumpPressed) {
            if (in.back) wallKick(p);                // back + jump: turn and leap away
            else setState(p, MANTLE);                // jump: climb up
            return;
        }

        // Shimmy sideways along the ledge
        Vec3 shift = Vec3.ZERO;
        int strafe = (in.right ? 1 : 0) - (in.left ? 1 : 0);
        if (strafe != 0) {
            Vec3 side = new Vec3(-wallDir.z, 0, wallDir.x).scale(strafe);
            Vec3 probe = p.position().add(side.scale(0.4));
            double top = findLedge(p, probe, wallDir, 1.6, 2.4);
            if (!Double.isNaN(top) && Math.abs(top - ledgeTop) < 0.01) shift = side.scale(0.08);
        }

        double targetY = ledgeTop - HANG_OFFSET;
        double dy = Mth.clamp((targetY - p.getY()) * 0.6, -0.3, 0.3);
        setVelocityCancellingInput(p, wallDir.x * 0.03 + shift.x, dy, wallDir.z * 0.03 + shift.z);
        p.resetFallDistance();
    }

    private void tickMantle(LocalPlayer p) {
        int rise = 6;
        double vx, vy, vz;
        if (t <= rise) {
            vy = Mth.clamp((ledgeTop + 0.05 - p.getY()) / (rise - t + 1), 0.0, 0.55);
            vx = wallDir.x * 0.02;
            vz = wallDir.z * 0.02;
        } else {
            vy = 0.0;
            vx = wallDir.x * 0.2;
            vz = wallDir.z * 0.2;
        }
        if (t >= MANTLE.duration) {
            p.setDeltaMovement(wallDir.x * 0.15, 0, wallDir.z * 0.15);
            setState(p, NONE);
            return;
        }
        setVelocityCancellingInput(p, vx, vy, vz);
        p.resetFallDistance();
    }

    private void tickVault(LocalPlayer p) {
        int rise = 3;
        if (t >= VAULT.duration) {
            p.setDeltaMovement(wallDir.x * speed, 0, wallDir.z * speed);
            setState(p, NONE);
            return;
        }
        if (t <= rise) {
            double vy = Mth.clamp((ledgeTop + 0.1 - p.getY()) / (rise - t + 1), 0.0, 0.6);
            p.setDeltaMovement(wallDir.x * 0.1, vy, wallDir.z * 0.1);
        } else {
            p.setDeltaMovement(wallDir.x * Math.max(speed, 0.4), 0.0, wallDir.z * Math.max(speed, 0.4));
        }
        p.resetFallDistance();
    }

    private void tickSlide(LocalPlayer p, Input in) {
        if (in.jumpPressed) {                        // jump out of a slide keeps the speed
            slideCooldown = 10;
            setState(p, NONE);
            return;
        }
        speed *= 0.95;
        if ((t > 3 && !p.onGround()) || (t > 6 && !in.sneak) || speed < 0.13 || t > 24) {
            slideCooldown = 10;
            setState(p, NONE);
            return;
        }
        p.setDeltaMovement(moveDir.x * speed, p.getDeltaMovement().y, moveDir.z * speed);
    }

    private void tickRollReady(LocalPlayer p) {
        if (p.onGround()) {
            Vec3 h = horizontal(p.getDeltaMovement());
            moveDir = h.lengthSqr() > 1.0E-4 ? h.normalize() : horizontal(forward(p)).normalize();
            speed = Math.max(h.length(), 0.28);
            setState(p, ROLL);
        } else if (t > 60) {
            setState(p, NONE);
        }
    }

    private void tickRoll(LocalPlayer p) {
        if (t >= ROLL.duration) {
            setState(p, NONE);
            return;
        }
        double s = speed * (1.0 - 0.4 * t / ROLL.duration);
        p.setDeltaMovement(moveDir.x * s, p.getDeltaMovement().y, moveDir.z * s);
    }

    /** Push off the wall you're facing and turn 180 degrees over a few ticks. */
    private void wallKick(LocalPlayer p) {
        Vec3 v = wallDir.scale(-0.4);
        p.setDeltaMovement(v.x, 0.45, v.z);
        turnTicks = 6;
        turnPerTick = 30f;
        climbAvailable = false;
        wallRunAvailable = true;
        grabCooldown = 8;
        setState(p, WALL_JUMP);
    }

    // ------------------------------------------------------------------ helpers

    private void setState(LocalPlayer p, ParkourState s) {
        if (s == state) return;
        state = s;
        t = 0;
        AnimState.get(p.getUUID()).set(s, ClientClock.now());
    }

    private void sync(LocalPlayer p) {
        if (state != lastSent && ClientPlayNetworking.canSend(StatePayload.TYPE)) {
            ClientPlayNetworking.send(new StatePayload(state.ordinal()));
            lastSent = state;
        }
    }

    private void applyMomentum(LocalPlayer p, FreerunConfig cfg, boolean running) {
        if (running && p.onGround()) momentumTicks = Math.min(momentumTicks + 1, 40);
        else if (p.onGround() && !p.isSprinting()) momentumTicks = 0;

        AttributeInstance attr = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr == null) return;
        double amount = cfg.enabled && cfg.momentum ? cfg.momentumBonus * (momentumTicks / 40.0) : 0.0;
        if (amount > 0.001) {
            attr.addOrUpdateTransientModifier(
                    new AttributeModifier(MOMENTUM_ID, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        } else {
            attr.removeModifier(MOMENTUM_ID);
        }
    }

    /**
     * Sets velocity and subtracts the small air-control push vanilla adds from WASD this tick,
     * so locked moves (hanging, climbing) don't drift.
     */
    private static void setVelocityCancellingInput(LocalPlayer p, double x, double y, double z) {
        Vec3 rel = new Vec3(p.xxa, 0, p.zza);
        if (rel.lengthSqr() > 1.0E-7) {
            if (rel.lengthSqr() > 1) rel = rel.normalize();
            rel = rel.scale(p.isSprinting() ? 0.026 : 0.02);
            float sin = Mth.sin(p.getYRot() * Mth.DEG_TO_RAD);
            float cos = Mth.cos(p.getYRot() * Mth.DEG_TO_RAD);
            x -= rel.x * cos - rel.z * sin;
            z -= rel.z * cos + rel.x * sin;
        }
        p.setDeltaMovement(x, y, z);
    }

    private static Vec3 forward(LocalPlayer p) {
        float yaw = p.getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
    }

    private static Vec3 right(LocalPlayer p) {
        float yaw = p.getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
    }

    private static double forwardDotAxis(LocalPlayer p, Vec3 axis) {
        return forward(p).dot(axis);
    }

    private static Vec3 horizontal(Vec3 v) {
        return new Vec3(v.x, 0, v.z);
    }

    /** Snaps a horizontal direction to the nearest block axis (walls are axis-aligned). */
    private static Vec3 axis(Vec3 v) {
        return Math.abs(v.x) > Math.abs(v.z)
                ? new Vec3(Math.signum(v.x), 0, 0)
                : new Vec3(0, 0, Math.signum(v.z));
    }

    /** Is there something solid {@code dist} blocks in {@code dir}, between y0 and y1 above the feet? */
    private static boolean blocked(LocalPlayer p, Vec3 dir, double dist, double y0, double y1) {
        AABB b = p.getBoundingBox();
        AABB probe = new AABB(b.minX, b.minY + y0, b.minZ, b.maxX, b.minY + y1, b.maxZ)
                .move(dir.x * dist, 0, dir.z * dist);
        return !p.level().noCollision(p, probe);
    }

    /**
     * Finds the top of a climbable edge in front of {@code origin}, between minRel and maxRel blocks
     * above the feet, with room to stand (or crouch) on top and a clear path straight up.
     * Returns the absolute Y of the edge, or NaN.
     */
    private static double findLedge(LocalPlayer p, Vec3 origin, Vec3 dir, double minRel, double maxRel) {
        Level level = p.level();
        double px = origin.x + dir.x * 0.8;
        double pz = origin.z + dir.z * 0.8;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int y0 = Mth.floor(origin.y + minRel) - 1;
        int y1 = Mth.floor(origin.y + maxRel);

        for (int y = y0; y <= y1; y++) {
            pos.set(Mth.floor(px), y, Mth.floor(pz));
            VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
            if (shape.isEmpty()) continue;
            double top = y + shape.max(Direction.Axis.Y);
            double rel = top - origin.y;
            if (rel < minRel || rel > maxRel) continue;

            AABB stand = p.getDimensions(Pose.STANDING).makeBoundingBox(px, top + 0.01, pz).deflate(0.01);
            AABB crouch = p.getDimensions(Pose.CROUCHING).makeBoundingBox(px, top + 0.01, pz).deflate(0.01);
            if (!level.noCollision(p, stand) && !level.noCollision(p, crouch)) continue;

            AABB b = p.getBoundingBox();
            double ox = origin.x - p.getX(), oz = origin.z - p.getZ();
            AABB column = new AABB(b.minX + ox, origin.y + 0.05, b.minZ + oz,
                    b.maxX + ox, top + 1.5, b.maxZ + oz).deflate(0.02, 0, 0.02);
            if (!level.noCollision(p, column)) continue;

            return top;
        }
        return Double.NaN;
    }
}
