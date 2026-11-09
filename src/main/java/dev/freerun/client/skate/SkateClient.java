package dev.freerun.client.skate;

import com.mojang.blaze3d.platform.InputConstants;
import dev.freerun.skate.GrindRailBlock;
import dev.freerun.skate.SkatePayloads;
import dev.freerun.skate.SkateRampBlock;
import dev.freerun.skate.SkateboardItem;
import dev.freerun.skate.Skating;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Skating, the way Skate 3 does it, on mouse and keyboard.
 *
 * <p><b>The mouse is the right stick ("Flick-It").</b> In board mode the mouse doesn't turn the
 * camera; it moves a virtual stick that springs back to the middle. Pull it back (mouse toward you)
 * to crouch, then flick it forward to pop. The direction of the flick is the trick:
 * <pre>
 *   straight up ............ Ollie          up-left / up-right ..... Kickflip / Heelflip
 *   left / right ........... Pop Shove-it / FS Pop Shove-it
 *   down-left / down-right . Varial Kickflip / Varial Heelflip (flick out sideways and low)
 *   a half circle round ..... 360 Flip (via the left) / Laser Flip (via the right)
 *   a fast, hard flick ...... Double Kickflip / Double Heelflip
 *   forward first, then back  the Nollie version of any of them
 * </pre>
 * Hold the stick about halfway back for a Manual, halfway forward for a Nose Manual, and steer the
 * mouse left and right to keep your balance.
 *
 * <p><b>Keys:</b> W push · S foot-brake · A/D carve (in the air: spin) · Shift + A/D powerslide ·
 * Shift alone tuck · left / right mouse in the air: Indy / Melon grab (both: Crossbone) · Space a
 * plain ollie · Left Ctrl / Left Alt manual / nose manual · B get on or off the board.
 *
 * <p>Land on a rail, fence, wall, bar or ledge and you lock into a grind; tilt the stick to change
 * it (5-0, Nosegrind, Crooked, Smith), flick to pop out. Land with the board still flipping, a
 * grab still held, sideways, or from too high, and you bail.
 */
public final class SkateClient {
    // ------------------------------------------------------------------ tuning
    static final double MAX_PUSH = 0.44, MAX_SPEED = 0.95, PUSH_GAIN = 0.085;
    static final float SENS = 0.011f, STICK_RETURN = 0.74f;

    // ------------------------------------------------------------------ state
    private static boolean active;
    private static int deck;
    private static float heading;           // direction of travel (yaw)
    private static float facing;            // the board's nose direction (= heading, or heading + 180 riding fakie)
    private static boolean fakie;
    private static double speed;
    private static int pose = SkatePayloads.RIDE;
    private static float lean, leanO;
    private static CameraType savedCamera;

    // trick in progress
    private static String trickName;
    private static float rollTarget, spinTarget;
    private static int trickDuration, trickAge;
    private static float boardRoll, boardSpin, boardPitch;
    private static float airSpin;           // body+board spin from A/D in the air
    private static int airTicks;
    private static double lastVy, prevY;
    private static boolean lateFlipUsed;

    // grind
    private static boolean grinding;
    private static Direction.Axis grindAxis;
    private static double grindLine, grindTop;
    private static int grindDir;
    private static String grindName;
    private static int grindTicks;

    // manual
    private static int manualTicks, manualHold;
    private static boolean manualNose;
    private static float balance;

    // grabs, pushes, slides
    private static int grabTicks;
    private static int grabKind;            // 0 none, 1 indy, 2 melon, 3 crossbone
    private static int pushTicks;
    private static boolean powersliding;
    private static int slideTicks;

    // bail
    private static int bailTicks;
    private static Vec3 bailBoardPos = Vec3.ZERO, bailBoardVel = Vec3.ZERO;
    private static float bailBoardSpin;

    // the flick stick
    private static double pendingDx, pendingDy;
    private static float sx, sy, psx, psy;
    private static int prep;                // 0 none, 1 pulled back, 2 pushed forward (nollie)
    private static int prepAge;
    private static float travel, lastAng, flickSpeed;
    private static int popCooldown;

    // combo
    static final List<String> line = new ArrayList<>();
    static int linePoints, lineMult = 1;
    private static int idleTicks;
    static long totalScore;
    static final List<Popup> popups = new ArrayList<>();

    record Popup(String text, int color, int[] ticks) {}

    private static int event = SkatePayloads.EV_NONE;
    private static SkatePayloads.State lastSent = SkatePayloads.State.OFF;
    private static boolean wasOnGround = true;

    // ramps
    private static boolean onRamp, vertAir;
    private static double rampSn, rampC = 1;
    private static float rampPitch;
    /**
     * How far the rider and board are pitched, relative to the direction of travel: + leaning back
     * (going up a ramp, or a backflip), - leaning forward. It eases to match the ground or the ramp,
     * stays put through the air (so you come off a quarter pipe the way you went up it), and W / S
     * rotate it in the air for front and back flips.
     */
    private static float tilt, tiltO, flipTotal;
    /** W / S have been let go since we left the ground (so holding W to push doesn't flip you). */
    private static boolean flipKeysFree;

    // stalls on the coping
    private static boolean stalling;
    private static Direction stallDrop;
    private static boolean slopeRail;

    private SkateClient() {}

    // ------------------------------------------------------------------ queries for the renderers and mixins

    public static boolean active() {
        return active;
    }

    /** True while the camera should be the low skate cam. */
    public static boolean camera() {
        return active;
    }

    public static float stickX() {
        return sx;
    }

    public static float stickY() {
        return sy;
    }

    public static int prepState() {
        return prep;
    }

    public static float balance() {
        return manualTicks > 0 ? balance : Float.NaN;
    }

    public static double speed() {
        return speed;
    }

    public static boolean grinding() {
        return grinding;
    }

    /** What the local player looks like right now (the renderers read this; others get theirs from the server). */
    public static SkatePayloads.State localState(int entityId) {
        float body = airSpin + (fakie ? 180 : 0);
        float roll = boardRoll, spin = boardSpin + body, pitch = boardPitch;
        if (powersliding) {
            spin += 80 * Math.signum(lean == 0 ? 1 : lean);
            body += 60 * Math.signum(lean == 0 ? 1 : lean);
        }
        if (grinding && "Boardslide".equals(grindName)) {
            spin += 90;
            body += 90;
        }
        return new SkatePayloads.State(entityId, active, deck, heading, roll, spin, body, pitch, tilt, pose, lean, 0);
    }

    public static Vec3 bailBoard(float partial) {
        return bailBoardPos.add(bailBoardVel.scale(partial));
    }

    public static float bailBoardSpin() {
        return bailBoardSpin;
    }

    public static int bailTicks() {
        return bailTicks;
    }

    /** Mouse movement in board mode, from the Entity.turn mixin (raw turn amounts). */
    public static void mouse(double dYaw, double dPitch) {
        pendingDx += dYaw;
        pendingDy += dPitch;
    }

    // ------------------------------------------------------------------ getting on and off

    public static void toggle() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return;
        if (active) {
            stop(mc, "§7Off the board.");
            return;
        }
        ItemStack board = SkateboardItem.find(p);
        if (board.isEmpty()) {
            p.displayClientMessage(Component.literal("§cYou need a skateboard in your inventory."), true);
            return;
        }
        if (p.isPassenger() || p.isInWater() || p.getAbilities().flying || dev.freerun.alien.Transformations.form(p).transformed()) {
            p.displayClientMessage(Component.literal("§cCan't drop in from here."), true);
            return;
        }
        active = true;
        deck = ((SkateboardItem) board.getItem()).deck;
        heading = facing = p.getYRot();
        fakie = false;
        speed = Math.min(MAX_PUSH, p.getDeltaMovement().horizontalDistance());
        resetTrick();
        grinding = false;
        manualTicks = 0;
        bailTicks = 0;
        pose = SkatePayloads.RIDE;
        savedCamera = mc.options.getCameraType();
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        line.clear();
        linePoints = 0;
        p.playSound(Skating.LAND, 0.7f, 1.2f);
        popup("§bDROP IN", 0x40D8FF);
        p.displayClientMessage(Component.literal("§7Mouse = flick stick · W push · A/D carve · B to get off"), true);
        RollSound.start(mc);
    }

    private static void stop(Minecraft mc, String msg) {
        active = false;
        grinding = false;
        if (savedCamera != null) mc.options.setCameraType(savedCamera);
        savedCamera = null;
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal(msg), true);
            mc.player.setYRot(heading);
        }
        send(mc.player, true);
    }

    // ------------------------------------------------------------------ the tick

    public static void tick(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) {
            active = false;
            return;
        }
        for (int i = popups.size() - 1; i >= 0; i--) if (--popups.get(i).ticks()[0] <= 0) popups.remove(i);
        if (!active || mc.isPaused()) return;
        if (SkateboardItem.find(p).isEmpty() || p.isPassenger() || p.isSpectator() || p.getAbilities().flying
                || dev.freerun.alien.Transformations.form(p).transformed() || p.isDeadOrDying()) {
            stop(mc, "§7Off the board.");
            return;
        }
        if (p.isInWater()) {
            bail(p, "Splashdown");
            stop(mc, "§9Into the water.");
            return;
        }
        leanO = lean;
        tiltO = tilt;
        event = SkatePayloads.EV_NONE;
        if (popCooldown > 0) popCooldown--;
        updateStick();

        if (bailTicks > 0) {
            tickBail(p);
            send(p, false);
            return;
        }

        Options o = mc.options;
        long win = mc.getWindow().getWindow();
        boolean w = o.keyUp.isDown(), s = o.keyDown.isDown(), a = o.keyLeft.isDown(), d = o.keyRight.isDown();
        boolean shift = o.keyShift.isDown(), space = o.keyJump.isDown();
        boolean lmb = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean rmb = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        boolean ctrl = InputConstants.isKeyDown(win, GLFW.GLFW_KEY_LEFT_CONTROL);
        boolean alt = InputConstants.isKeyDown(win, GLFW.GLFW_KEY_LEFT_ALT);
        int steer = (a ? 1 : 0) - (d ? 1 : 0);   // + = left (yaw decreases), matching vanilla A/D

        boolean ground = p.onGround();
        double rh = grinding ? Double.NaN : rampHeight(p.level(), p.getX(), p.getZ(), p.getY());
        double vy0 = p.getDeltaMovement().y;
        boolean rampContact = !Double.isNaN(rh) && (onRamp ? Math.abs(p.getY() - rh) < 0.6
                : p.getY() - rh < 0.15 + Math.max(0, -vy0) && p.getY() - rh > -1.2 && vy0 <= 0.02);
        if (grinding) tickGrind(p, steer, w, s);
        else if (rampContact) {
            tickRamp(p, rh, w, s, steer, space, lmb || rmb);
            ground = onRamp;
        } else {
            onRamp = false;
            if (ground) tickGround(p, w, s, steer, shift, space, ctrl, alt);
            else tickAir(p, steer, lmb, rmb, w, s, shift);
            if (ground) tilt = ease(tilt, 0, 0.3f);
        }

        // landing
        if (!grinding && !onRamp && ground && !wasOnGround && airTicks > 1) land(p, lmb || rmb);
        if (ground || grinding) {
            vertAir = false;
            flipKeysFree = false;
        }
        wasOnGround = ground || grinding;
        if (!ground && !grinding) airTicks++;
        else if (!grinding) airTicks = 0;
        lastVy = p.getDeltaMovement().y;
        prevY = p.getY();

        // the line ends once you've rolled a moment without doing anything
        if (ground && !grinding && manualTicks == 0 && trickName == null && !line.isEmpty()) {
            if (++idleTicks > 22) bankLine(p);
        } else {
            idleTicks = 0;
        }

        p.setYRot(heading);
        p.setYHeadRot(heading);
        p.yBodyRot = heading;
        p.setXRot(12);
        p.resetFallDistance();
        send(p, false);
    }

    // ------------------------------------------------------------------ the flick stick

    private static void updateStick() {
        psx = sx;
        psy = sy;
        sx = sx * STICK_RETURN + (float) (pendingDx * SENS);
        sy = sy * STICK_RETURN - (float) (pendingDy * SENS);   // mouse up (pitch down) = stick forward
        pendingDx = pendingDy = 0;
        float m = Mth.sqrt(sx * sx + sy * sy);
        if (m > 1) {
            sx /= m;
            sy /= m;
        }
    }

    private static float stickAngle() {
        return (float) (Math.atan2(sx, sy) * Mth.RAD_TO_DEG);   // 0 = forward, -90 = left, +90 = right, ±180 = back
    }

    /**
     * Watches the stick for a pull-back-and-flick (or push-and-pull for a nollie). Once the stick
     * leaves the "loaded" zone, it watches for {@link #FLICK_WINDOW} ticks and reads the trick from
     * where the flick went furthest. Returns true if it popped.
     */
    private static boolean gesture(LocalPlayer p, boolean canPop) {
        float mag = Mth.sqrt(sx * sx + sy * sy);
        float ang = stickAngle();
        if (prep == 0) {
            if (!canPop || popCooldown > 0) return false;
            if (sy < -0.62f) startPrep(1, ang);
            else if (sy > 0.62f && speed > 0.05) startPrep(2, ang);
            return false;
        }
        prepAge++;
        if (mag > 0.4f) {
            travel += Mth.wrapDegrees(ang - lastAng);
            lastAng = ang;
        }
        flickSpeed = Math.max(flickSpeed, Mth.sqrt((sx - psx) * (sx - psx) + (sy - psy) * (sy - psy)));
        boolean loaded = prep == 1 ? sy < -0.4f && Math.abs(sx) < 0.55f : sy > 0.4f && Math.abs(sx) < 0.55f;
        if (flickAge == 0 && !loaded && mag > 0.45f) flickAge = 1;
        if (flickAge > 0) {
            if (mag >= peakMag) {
                peakMag = mag;
                peakAng = ang;
            }
            if (flickAge++ >= FLICK_WINDOW || Math.abs(travel) >= 240) {
                if (!canPop) {
                    prep = 0;
                    return false;
                }
                pop(p, classify());
                return true;
            }
            return false;
        }
        if (prepAge > 40 || (prepAge > 6 && mag < 0.25f)) prep = 0;   // let go without flicking: nothing
        return false;
    }

    static final int FLICK_WINDOW = 2;
    private static int flickAge;
    private static float peakMag, peakAng;

    private static void startPrep(int kind, float ang) {
        prep = kind;
        prepAge = 0;
        travel = 0;
        lastAng = ang;
        flickSpeed = 0;
        flickAge = 0;
        peakMag = 0;
        peakAng = ang;
    }

    /** The trick a flick means. Angles are measured from "forward" (or from "back" for a nollie). */
    private static Trick classify() {
        boolean nollie = prep == 2;
        float a = peakAng;
        if (nollie) a = Mth.wrapDegrees(a - 180);
        if (Math.abs(travel) >= 240) return travel > 0 ? Trick.TRE_FLIP.nollie(nollie) : Trick.LASER_FLIP.nollie(nollie);
        boolean hard = flickSpeed > 1.15f;
        Trick t;
        float abs = Math.abs(a);
        boolean left = a < 0;
        if (abs <= 22) t = Trick.OLLIE;
        else if (abs <= 67) t = left ? (hard ? Trick.DOUBLE_KICKFLIP : Trick.KICKFLIP) : (hard ? Trick.DOUBLE_HEELFLIP : Trick.HEELFLIP);
        else if (abs <= 112) t = left ? Trick.SHOVE_IT : Trick.FS_SHOVE_IT;
        else t = left ? Trick.VARIAL_KICKFLIP : Trick.VARIAL_HEELFLIP;
        return t.nollie(nollie);
    }

    /** The trick set: board roll (flip) and spin (shove) in degrees, how long it takes, what it's worth. */
    record Trick(String name, float roll, float spin, int ticks, int points) {
        static final Trick OLLIE = new Trick("Ollie", 0, 0, 0, 100);
        static final Trick KICKFLIP = new Trick("Kickflip", 360, 0, 12, 250);
        static final Trick HEELFLIP = new Trick("Heelflip", -360, 0, 12, 250);
        static final Trick DOUBLE_KICKFLIP = new Trick("Double Kickflip", 720, 0, 18, 550);
        static final Trick DOUBLE_HEELFLIP = new Trick("Double Heelflip", -720, 0, 18, 550);
        static final Trick SHOVE_IT = new Trick("Pop Shove-it", 0, 180, 10, 200);
        static final Trick FS_SHOVE_IT = new Trick("FS Pop Shove-it", 0, -180, 10, 200);
        static final Trick VARIAL_KICKFLIP = new Trick("Varial Kickflip", 360, 180, 14, 400);
        static final Trick VARIAL_HEELFLIP = new Trick("Varial Heelflip", -360, -180, 14, 400);
        static final Trick TRE_FLIP = new Trick("360 Flip", 360, 360, 16, 650);
        static final Trick LASER_FLIP = new Trick("Laser Flip", -360, -360, 16, 700);

        Trick nollie(boolean n) {
            if (!n) return this;
            return new Trick(name.equals("Ollie") ? "Nollie" : "Nollie " + name, roll, -spin, ticks, points * 6 / 5);
        }

        Trick named(String n) {
            return new Trick(n, roll, spin, ticks, points);
        }
    }

    // ------------------------------------------------------------------ popping

    private static void pop(LocalPlayer p, Trick t) {
        prep = 0;
        popCooldown = 6;
        if (fakie && !t.name().startsWith("Nollie")) t = t.named((t.name().equals("Ollie") ? "Fakie " : "Switch ") + t.name());
        double vy = 0.44 + 0.1 * Math.min(1, flickSpeed);
        if (onRamp) {
            vy += Math.max(0, speed * rampSn);
            speed *= rampC;
            onRamp = false;
        }
        if (grinding) {
            endGrind(p, true);
            vy = 0.4;
        }
        if (manualTicks > 0) endManual(p);
        Vec3 v = p.getDeltaMovement();
        p.setDeltaMovement(v.x, vy, v.z);
        p.hurtMarked = true;
        startTrick(t);
        airTicks = 1;
        wasOnGround = false;
        pose = SkatePayloads.AIR;
        sound(p, Skating.POP, 1.0f, 0.95f + p.getRandom().nextFloat() * 0.1f);
        event = SkatePayloads.EV_POP;
        lateFlipUsed = false;
    }

    private static void startTrick(Trick t) {
        trickName = t.name();
        rollTarget = t.roll();
        spinTarget = t.spin();
        trickDuration = Math.max(1, t.ticks());
        trickAge = 0;
        boardRoll = boardSpin = 0;
        addToLine(t.name(), t.points());
    }

    private static void resetTrick() {
        trickName = null;
        rollTarget = spinTarget = 0;
        boardRoll = boardSpin = boardPitch = 0;
        airSpin = 0;
        trickAge = 0;
        grabTicks = 0;
        grabKind = 0;
    }

    /** How far through its flip the board is: 1 = caught. */
    private static float trickProgress() {
        if (rollTarget == 0 && spinTarget == 0) return 1;
        return Mth.clamp(trickAge / (float) trickDuration, 0, 1);
    }

    // ------------------------------------------------------------------ on the ground

    private static void tickGround(LocalPlayer p, boolean w, boolean s, int steer, boolean shift, boolean space, boolean ctrl, boolean alt) {
        Vec3 fwd = dir(heading);
        // slopes and stairs: rolling down speeds you up, rolling up slows you
        double dy = p.getY() - prevY;
        if (Math.abs(dy) < 1.0) speed = Mth.clamp(speed - dy * 0.1, 0, MAX_SPEED);

        // pushing
        if (pushTicks > 0) pushTicks--;
        if (w && pushTicks == 0 && speed < MAX_PUSH && manualTicks == 0) {
            pushTicks = 16;
            event = SkatePayloads.EV_PUSH;
            sound(p, Skating.PUSH, 0.6f, 1.0f);
        }
        if (pushTicks == 9) speed = Math.min(MAX_PUSH, speed + PUSH_GAIN + (speed < 0.05 ? 0.05 : 0));
        // braking with a foot
        if (s && manualTicks == 0) {
            speed = Math.max(0, speed - 0.018);
            if (speed > 0.08 && p.tickCount % 3 == 0) dust(p, 1);
        }
        // carving and powerslides
        float turn = (float) Math.max(2.2, 6.0 - speed * 6.5);
        powersliding = shift && steer != 0 && speed > 0.16;
        if (powersliding) {
            if (slideTicks++ == 0) addToLine("Powerslide", 150);
            speed *= 0.935;
            heading -= steer * turn * 0.4f;
            dust(p, 3);
            if (p.tickCount % 4 == 0) sound(p, Skating.GRIND, 0.3f, 0.6f);
        } else {
            slideTicks = 0;
            heading -= steer * turn;
        }
        lean = Mth.lerp(0.3f, lean, steer * (float) Math.min(1, speed * 3));
        facing = heading;
        // friction: tucked (shift, no steer) rolls further
        boolean tuck = shift && steer == 0;
        speed *= tuck ? 0.9985 : 0.995;
        speed = Math.max(0, speed - 0.0006);

        // walls
        if (p.horizontalCollision) {
            if (speed > 0.28) {
                bail(p, "Slammed");
                return;
            }
            speed *= 0.2;
        }

        // manuals: the stick halfway back (or forward), or Ctrl / Alt
        boolean inZone = prep == 0 && ((sy < -0.22f && sy > -0.58f) || (sy > 0.22f && sy < 0.58f)) && Math.abs(sx) < 0.5f;
        manualHold = inZone ? manualHold + 1 : 0;
        boolean keepZone = manualTicks > 0 && ((sy < -0.12f && sy > -0.62f) || (sy > 0.12f && sy < 0.62f));
        boolean manualStick = (manualHold > 5 || keepZone) && speed > 0.04;
        boolean manualKey = (ctrl || alt) && speed > 0.04;
        if ((manualStick || manualKey) && prep == 0) {
            boolean nose = manualKey ? alt : sy > 0;
            if (manualTicks == 0) {
                manualNose = nose;
                balance = (p.getRandom().nextFloat() - 0.5f) * 0.2f;
            }
            manualTicks++;
            // the balance drifts, and drifts faster the further off it is; steer the mouse (or A/D) to hold it
            balance += (p.getRandom().nextFloat() - 0.5f) * 0.05f + balance * 0.035f + (float) speed * 0.01f * Mth.sin(manualTicks * 0.2f);
            balance -= (sx * 0.12f + steer * -0.04f);
            if (Math.abs(balance) > 1) {
                // tip over the back (or front): you put the wheels down; too far and you're off
                endManual(p);
                if (Math.abs(balance) > 1.3f) {
                    bail(p, manualNose ? "Nose-dived" : "Looped out");
                    return;
                }
            } else {
                pose = manualNose ? SkatePayloads.NOSE_MANUAL : SkatePayloads.MANUAL;
                boardPitch = manualNose ? -11 : 11;
                linePoints += 2;
            }
        } else if (manualTicks > 0) {
            endManual(p);
        }

        // popping: the flick, or the space bar for a plain ollie
        boolean popped = gesture(p, true);
        if (!popped && space && popCooldown == 0) {
            flickSpeed = 0.5f;
            pop(p, Trick.OLLIE);
            popped = true;
        }
        if (popped) return;

        if (manualTicks == 0) {
            boardPitch = 0;
            if (prep != 0) pose = SkatePayloads.CROUCH;
            else if (powersliding) pose = SkatePayloads.POWERSLIDE;
            else if (pushTicks > 0) pose = SkatePayloads.PUSH;
            else pose = tuck ? SkatePayloads.CROUCH : SkatePayloads.RIDE;
        }
        Vec3 v = p.getDeltaMovement();
        p.setDeltaMovement(fwd.x * speed, v.y, fwd.z * speed);
    }

    private static void endManual(LocalPlayer p) {
        if (manualTicks > 6) addToLine((manualNose ? "Nose Manual" : "Manual") + String.format(" %.1fs", manualTicks / 20f), manualTicks * 8);
        manualTicks = 0;
        boardPitch = 0;
        balance = 0;
    }

    // ------------------------------------------------------------------ in the air

    private static void tickAir(LocalPlayer p, int steer, boolean lmb, boolean rmb, boolean w, boolean s, boolean shift) {
        trickAge++;
        float k = trickProgress();
        float e = 1 - (1 - k) * (1 - k);
        boardRoll = rollTarget * e;
        boardSpin = spinTarget * e;
        if (k >= 1 && trickName != null && (rollTarget != 0 || spinTarget != 0)) {
            boardRoll = boardSpin = 0;
            rollTarget = spinTarget = 0;   // caught
        }
        // body spins
        if (steer != 0) airSpin -= steer * 13f;
        // grabs
        int g = lmb && rmb ? 3 : lmb ? 1 : rmb ? 2 : 0;
        if (g != 0) {
            if (grabKind != g && grabTicks > 4) finishGrab();
            if (grabKind != g) grabTicks = 0;
            grabKind = g;
            grabTicks++;
            pose = g == 1 ? SkatePayloads.GRAB_L : g == 2 ? SkatePayloads.GRAB_R : SkatePayloads.GRAB_BOTH;
            linePoints += 3;
        } else {
            if (grabKind != 0) finishGrab();
            pose = SkatePayloads.AIR;
        }
        // a late flip: flick again after a plain ollie
        if (!lateFlipUsed && "Ollie".equals(trickName) && airTicks < 14 && airTicks > 2 && rollTarget == 0) {
            float mag = Mth.sqrt(sx * sx + sy * sy);
            if (mag > 0.75f && Math.abs(sx) > 0.5f) {
                lateFlipUsed = true;
                startTrick((sx < 0 ? Trick.KICKFLIP : Trick.HEELFLIP).named("Late " + (sx < 0 ? "Kickflip" : "Heelflip")));
                sound(p, Skating.POP, 0.5f, 1.4f);
            }
        }
        gesture(p, false);
        // keep momentum; a touch of air control
        Vec3 v = p.getDeltaMovement();
        Vec3 fwd = dir(heading);
        // flips: W rolls you forward, S back; let go and you hold that angle (a flat air slowly levels out)
        if (!w && !s) flipKeysFree = true;
        if (w != s && flipKeysFree) {
            float d = w ? -11f : 11f;
            tilt += d;
            flipTotal += d;
        } else if (!vertAir && Math.abs(Mth.wrapDegrees(tilt)) < 70) {
            tilt = ease(tilt, 0, 0.06f);
        }
        if (vertAir) {
            // straight up out of the pipe: drift back over the ramp, or hold Shift to come down on the coping
            double drift = shift ? 0.07 : -0.035;
            p.setDeltaMovement(fwd.x * drift, v.y, fwd.z * drift);
        } else {
            p.setDeltaMovement(fwd.x * speed, v.y, fwd.z * speed);
        }
        // look for something to grind on the way down
        if (v.y <= 0.05 && airTicks > 2) tryGrind(p);
    }

    private static void finishGrab() {
        String n = switch (grabKind) {
            case 1 -> "Indy";
            case 2 -> "Melon";
            default -> "Crossbone";
        };
        if (grabTicks > 3) addToLine(n + (grabTicks > 14 ? String.format(" %.1fs", grabTicks / 20f) : ""), 100 + grabTicks * 12);
        grabKind = 0;
        grabTicks = 0;
    }

    // ------------------------------------------------------------------ landing and bailing

    /** The tilt the surface we're landing on wants (set just before a ramp landing; 0 for flat ground). */
    private static float landTilt;

    private static float ease(float from, float to, float k) {
        return from + Mth.wrapDegrees(to - from) * k;
    }

    private static void land(LocalPlayer p, boolean grabHeld) {
        float want = landTilt;
        landTilt = 0;
        float tiltOff = Math.abs(Mth.wrapDegrees(tilt - want));
        int flips = Math.round(Math.abs(flipTotal) / 360f);
        boolean back = flipTotal > 0;
        flipTotal = 0;
        if (tiltOff > 55) {
            String why = Mth.wrapDegrees(tilt - want) > 0 ? "Over-rotated" : "Under-rotated";
            tilt = 0;
            bail(p, why);
            return;
        }
        tilt = Mth.wrapDegrees(tilt);
        if (flips > 0) {
            String n = (flips == 2 ? "Double " : flips >= 3 ? "Triple " : "") + (back ? "Backflip" : "Frontflip");
            addToLine(n, 900 * flips + 300 * (flips - 1));
        }
        float k = trickProgress();
        if (k < 0.8f) {
            bail(p, "Didn't catch it");
            return;
        }
        if (grabHeld && grabKind != 0) {
            bail(p, "Still grabbing");
            return;
        }
        if (lastVy < -1.25) {
            bail(p, "Too high");
            return;
        }
        // which way round are we? within 40° of either end of the board is a landing
        float spin = Mth.wrapDegrees(airSpin);
        float off = Math.abs(spin);
        if (off > 40 && off < 140) {
            bail(p, "Landed sideways");
            return;
        }
        if (Math.abs(airSpin) >= 140) {
            int halves = Math.round(Math.abs(airSpin) / 180f);
            String dir = airSpin < 0 ? "FS " : "BS ";
            addToLine(dir + (halves * 180), halves * 300);
        }
        if (off >= 140) fakie = !fakie;   // came round 180 (or 540): riding the other way now
        if (grabKind != 0) finishGrab();
        float sketch = Math.abs(Mth.wrapDegrees(spin - (off >= 140 ? 180 : 0)));
        resetTrick();
        pose = SkatePayloads.RIDE;
        sound(p, Skating.LAND, 1.0f, 0.9f + p.getRandom().nextFloat() * 0.15f);
        event = SkatePayloads.EV_LAND;
        speed *= sketch > 25 ? 0.75 : 0.97;
        if (sketch > 25) popup("§eSketchy", 0xE0C040);
        dust(p, 6);
    }

    private static void bail(LocalPlayer p, String why) {
        if (grinding) grinding = false;
        stalling = false;
        slopeRail = false;
        onRamp = false;
        vertAir = false;
        tilt = tiltO = flipTotal = 0;
        bailTicks = 34;
        Vec3 v = p.getDeltaMovement();
        bailBoardPos = p.position();
        bailBoardVel = new Vec3(v.x * 1.4 + (p.getRandom().nextFloat() - 0.5) * 0.3, 0.35, v.z * 1.4 + (p.getRandom().nextFloat() - 0.5) * 0.3);
        bailBoardSpin = 0;
        line.clear();
        linePoints = 0;
        lineMult = 1;
        manualTicks = 0;
        prep = 0;
        resetTrick();
        pose = SkatePayloads.BAIL;
        sound(p, Skating.BAIL, 1.0f, 1.0f);
        event = SkatePayloads.EV_BAIL;
        popup("§c§lBAILED §r§7" + why, 0xFF4040);
        dev.freerun.client.StreamChat.skateBailed();
        speed *= 0.3;
    }

    private static void tickBail(LocalPlayer p) {
        bailTicks--;
        pose = SkatePayloads.BAIL;
        // the board skitters off on its own
        bailBoardPos = bailBoardPos.add(bailBoardVel);
        bailBoardVel = new Vec3(bailBoardVel.x * 0.9, Math.max(-0.5, bailBoardVel.y - 0.06), bailBoardVel.z * 0.9);
        if (bailBoardPos.y < p.getY()) {
            bailBoardPos = new Vec3(bailBoardPos.x, p.getY(), bailBoardPos.z);
            bailBoardVel = new Vec3(bailBoardVel.x, -bailBoardVel.y * 0.3, bailBoardVel.z);
        }
        bailBoardSpin += (float) (bailBoardVel.horizontalDistance() * 90);
        Vec3 v = p.getDeltaMovement();
        speed *= 0.85;
        Vec3 fwd = dir(heading);
        p.setDeltaMovement(fwd.x * speed, v.y, fwd.z * speed);
        if (bailTicks == 0) {
            pose = SkatePayloads.RIDE;
            speed = 0;
            fakie = false;
            popup("§7Back on", 0x909090);
        }
    }

    // ------------------------------------------------------------------ grinding

    private static boolean rail(BlockState s) {
        return s.getBlock() instanceof FenceBlock || s.getBlock() instanceof WallBlock || s.getBlock() instanceof IronBarsBlock
                || s.getBlock() instanceof GrindRailBlock && s.getValue(GrindRailBlock.SLOPE) == 0;
    }

    /** Height of the top of whatever's at {@code pos}, or NaN. */
    private static double top(LocalPlayer p, BlockPos pos) {
        BlockState s = p.level().getBlockState(pos);
        VoxelShape shape = s.getCollisionShape(p.level(), pos);
        if (shape.isEmpty()) return Double.NaN;
        return pos.getY() + shape.max(Direction.Axis.Y);
    }

    /** A drop beside a ledge: air, or a ramp falling away (the coping of a quarter pipe). */
    private static boolean drop(LocalPlayer p, BlockPos pos, double ledgeTop) {
        BlockState s = p.level().getBlockState(pos);
        if (s.getBlock() instanceof SkateRampBlock) return true;
        double t = top(p, pos);
        return Double.isNaN(t) || t < ledgeTop - 0.45;
    }

    /** A sloped handrail block somewhere around the feet: its top here, or NaN. */
    private static double slopedRailTop(LocalPlayer p, double x, double z, double nearY, BlockPos[] found) {
        int bx = Mth.floor(x), bz = Mth.floor(z);
        for (int by = Mth.floor(nearY + 0.6); by >= Mth.floor(nearY - 1.6); by--) {
            BlockPos bp = new BlockPos(bx, by, bz);
            BlockState st = p.level().getBlockState(bp);
            if (st.getBlock() instanceof GrindRailBlock && st.getValue(GrindRailBlock.SLOPE) != 0) {
                double t = by + GrindRailBlock.top(st, x - bx, z - bz);
                if (Math.abs(t - nearY) < 1.0) {
                    if (found != null) found[0] = bp;
                    return t;
                }
            }
        }
        return Double.NaN;
    }

    private static void tryGrind(LocalPlayer p) {
        Vec3 feet = p.position();
        Vec3 v = p.getDeltaMovement();
        // a handrail down some stairs
        BlockPos[] hr = new BlockPos[1];
        double ht = slopedRailTop(p, feet.x, feet.z, feet.y, hr);
        if (!Double.isNaN(ht) && feet.y > ht - 0.2 && feet.y < ht + 0.5 + Math.max(0, -v.y)) {
            BlockState st = p.level().getBlockState(hr[0]);
            Direction.Axis ax = st.getValue(GrindRailBlock.AXIS);
            slopeRail = true;
            startGrind(p, ax, ax == Direction.Axis.X ? hr[0].getZ() + 0.5 : hr[0].getX() + 0.5, ht, false);
            return;
        }
        BlockPos below = BlockPos.containing(feet.x, feet.y - 0.6, feet.z);
        for (BlockPos bp : new BlockPos[]{below, below.above()}) {
            BlockState st = p.level().getBlockState(bp);
            if (st.getBlock() instanceof SkateRampBlock) continue;
            double t = top(p, bp);
            if (Double.isNaN(t)) continue;
            if (feet.y < t - 0.15 || feet.y > t + 0.5 + Math.max(0, -v.y)) continue;
            if (rail(st)) {
                Direction.Axis ax = railAxis(st);
                slopeRail = false;
                startGrind(p, ax, ax == Direction.Axis.X ? bp.getZ() + 0.5 : bp.getX() + 0.5, t, false);
                return;
            }
            // a ledge: a solid top with a drop on one side, and we're over that edge
            if (t - bp.getY() < 0.45) continue;   // carpets, slabs on the floor: no
            double fx = feet.x - bp.getX(), fz = feet.z - bp.getZ();
            Direction best = null;
            double bestD = 0.35;
            for (Direction side : Direction.Plane.HORIZONTAL) {
                double d = switch (side) {
                    case WEST -> fx;
                    case EAST -> 1 - fx;
                    case NORTH -> fz;
                    default -> 1 - fz;
                };
                if (d < bestD && drop(p, bp.relative(side), t)) {
                    bestD = d;
                    best = side;
                }
            }
            if (best == null) continue;
            Direction.Axis ax = best.getAxis() == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
            double lineAt = switch (best) {
                case WEST -> bp.getX() + 0.15;
                case EAST -> bp.getX() + 0.85;
                case NORTH -> bp.getZ() + 0.15;
                default -> bp.getZ() + 0.85;
            };
            slopeRail = false;
            Vec3 axisVec = ax == Direction.Axis.X ? new Vec3(1, 0, 0) : new Vec3(0, 0, 1);
            if (Math.abs(dir(heading).dot(axisVec)) < 0.4 && (vertAir || speed < 0.2)) {
                startStall(p, ax, lineAt, t, best);
                return;
            }
            startGrind(p, ax, lineAt, t, true);
            return;
        }
    }

    /** Came straight down on the coping: an axle stall, then drop back in. */
    private static void startStall(LocalPlayer p, Direction.Axis axis, double lineAt, double top, Direction dropSide) {
        if (trickProgress() < 0.8f) {
            bail(p, "Didn't catch it");
            return;
        }
        grinding = true;
        stalling = true;
        stallDrop = dropSide;
        grindAxis = axis;
        grindLine = lineAt;
        grindTop = top;
        grindTicks = 0;
        grindName = "Axle Stall";
        speed = 0;
        vertAir = false;
        resetTrick();
        if (grabKind != 0) finishGrab();
        pose = SkatePayloads.GRIND;
        sound(p, Skating.GRIND, 0.8f, 0.7f);
        event = SkatePayloads.EV_GRIND;
    }

    private static void tickStall(LocalPlayer p, boolean w, boolean s) {
        grindTicks++;
        pose = SkatePayloads.GRIND;
        Vec3 pos = p.position();
        double nx = grindAxis == Direction.Axis.Z ? Mth.lerp(0.5, pos.x, grindLine) : pos.x;
        double nz = grindAxis == Direction.Axis.X ? Mth.lerp(0.5, pos.z, grindLine) : pos.z;
        p.setPos(nx, grindTop, nz);
        p.setDeltaMovement(0, 0, 0);
        linePoints += 2;
        float mag = Mth.sqrt(sx * sx + sy * sy);
        if (grindTicks > 6 && (mag > 0.6f || w || s || grindTicks > 30)) {
            // back in: the way we came, riding the other way round
            addToLine(grindTicks > 20 ? "Rock to Fakie" : "Axle Stall", 150 + grindTicks * 6);
            grinding = false;
            stalling = false;
            heading = stallDrop.toYRot();
            fakie = !fakie;
            tilt = -tilt;
            facing = heading;
            p.setPos(nx + stallDrop.getStepX() * 0.45, grindTop - 0.1, nz + stallDrop.getStepZ() * 0.45);
            p.setDeltaMovement(stallDrop.getStepX() * 0.05, -0.1, stallDrop.getStepZ() * 0.05);
            speed = 0.05;
            pose = SkatePayloads.AIR;
            airTicks = 1;
            wasOnGround = false;
            prep = 0;
        }
    }

    private static Direction.Axis railAxis(BlockState s) {
        boolean ns = false, ew = false;
        if (s.getBlock() instanceof GrindRailBlock) return s.getValue(GrindRailBlock.AXIS);
        if (s.hasProperty(FenceBlock.NORTH)) {
            ns = s.getValue(FenceBlock.NORTH) || s.getValue(FenceBlock.SOUTH);
            ew = s.getValue(FenceBlock.EAST) || s.getValue(FenceBlock.WEST);
        }
        if (ns && !ew) return Direction.Axis.Z;
        if (ew && !ns) return Direction.Axis.X;
        float h = Mth.wrapDegrees(heading);
        return Math.abs(h) < 45 || Math.abs(h) > 135 ? Direction.Axis.Z : Direction.Axis.X;
    }

    private static void startGrind(LocalPlayer p, Direction.Axis axis, double line_, double top, boolean ledge) {
        if (trickProgress() < 0.8f) {
            bail(p, "Didn't catch it");
            return;
        }
        grinding = true;
        grindAxis = axis;
        grindLine = line_;
        grindTop = top;
        grindTicks = 0;
        Vec3 axisVec = axis == Direction.Axis.X ? new Vec3(1, 0, 0) : new Vec3(0, 0, 1);
        Vec3 f = dir(facing);
        double along = dir(heading).dot(axisVec);
        grindDir = along >= 0 ? 1 : -1;
        double angle = Math.toDegrees(Math.acos(Math.min(1, Math.abs(f.dot(axisVec)))));
        grindName = angle > 50 ? "Boardslide" : ledge ? "50-50" : "50-50";
        speed = Math.max(0.16, speed * Math.max(0.5, Math.abs(along)));
        heading = axis == Direction.Axis.X ? (grindDir > 0 ? -90 : 90) : (grindDir > 0 ? 0 : 180);
        if (angle <= 50) facing = heading;
        resetTrick();
        if (grabKind != 0) finishGrab();
        pose = grindName.equals("Boardslide") ? SkatePayloads.BOARDSLIDE : SkatePayloads.GRIND;
        sound(p, Skating.GRIND, 1.0f, 1.0f);
        event = SkatePayloads.EV_GRIND;
    }

    private static void tickGrind(LocalPlayer p, int steer, boolean w, boolean s) {
        if (stalling) {
            tickStall(p, w, s);
            return;
        }
        grindTicks++;
        // tilt the stick to change grinds (boardslides stay boardslides)
        if (!grindName.equals("Boardslide")) {
            String n = "50-50";
            boolean tail = sy < -0.35f || s, nose = sy > 0.35f || w, tilt = Math.abs(sx) > 0.4f || steer != 0;
            if (tail) n = tilt ? "Smith" : "5-0";
            else if (nose) n = tilt ? "Crooked" : "Nosegrind";
            if (!n.equals(grindName) && grindTicks > 4) {
                addToLine(grindName, grindTicks * 6);
                grindTicks = 0;
            }
            grindName = n;
            boardPitch = n.equals("5-0") || n.equals("Smith") ? 10 : n.equals("Nosegrind") || n.equals("Crooked") ? -10 : 0;
        }
        pose = grindName.equals("Boardslide") ? SkatePayloads.BOARDSLIDE : SkatePayloads.GRIND;
        if (!slopeRail) tilt = ease(tilt, 0, 0.4f);
        speed *= 0.995;
        linePoints += 2;
        // ride along the rail
        Vec3 axisVec = grindAxis == Direction.Axis.X ? new Vec3(1, 0, 0) : new Vec3(0, 0, 1);
        Vec3 pos = p.position();
        double nx = grindAxis == Direction.Axis.X ? pos.x : Mth.lerp(0.5, pos.x, grindLine);
        double nz = grindAxis == Direction.Axis.Z ? pos.z : Mth.lerp(0.5, pos.z, grindLine);
        p.setPos(nx, grindTop, nz);
        p.setDeltaMovement(axisVec.x * speed * grindDir, 0, axisVec.z * speed * grindDir);
        // sparks off the trucks
        if (p.tickCount % 2 == 0) {
            p.level().addParticle(ParticleTypes.ELECTRIC_SPARK, p.getX(), grindTop + 0.05, p.getZ(),
                    -axisVec.x * grindDir * 0.2 + (p.getRandom().nextFloat() - 0.5) * 0.1, 0.12, -axisVec.z * grindDir * 0.2);
        }
        if (p.tickCount % 6 == 0) sound(p, Skating.GRIND, 0.45f, 0.9f + (float) speed * 0.4f);
        // does the rail go on?
        Vec3 ahead = new Vec3(nx, grindTop, nz).add(axisVec.scale(grindDir * 0.45));
        boolean goes;
        if (slopeRail) {
            // down (or up) the handrail: follow its slope, and gravity with it
            double here = slopedRailTop(p, nx, nz, grindTop, null);
            if (!Double.isNaN(here)) {
                tilt = ease(tilt, here < grindTop - 0.005 ? -45 : here > grindTop + 0.005 ? 45 : tilt, 0.4f);
                speed += (grindTop - here) * 0.08;
                grindTop = here;
                p.setPos(nx, grindTop, nz);
            }
            double next = slopedRailTop(p, ahead.x, ahead.z, grindTop, null);
            goes = !Double.isNaN(next) && Math.abs(next - grindTop) < 0.7;
            speed = Math.min(speed + 0.004, MAX_SPEED);
        } else {
            BlockPos bp = BlockPos.containing(ahead.x, grindTop - 0.3, ahead.z);
            double t = top(p, bp);
            goes = !Double.isNaN(t) && Math.abs(t - grindTop) < 0.15;
        }
        if (!goes || speed < 0.06 || p.horizontalCollision) {
            endGrind(p, false);
            return;
        }
        gesture(p, true);
    }

    private static void endGrind(LocalPlayer p, boolean popped) {
        grinding = false;
        stalling = false;
        slopeRail = false;
        if (grindTicks > 2) addToLine(grindName, 80 + grindTicks * 8);
        boardPitch = 0;
        pose = SkatePayloads.AIR;
        airTicks = 1;
        wasOnGround = false;
        if (!popped) {
            Vec3 v = p.getDeltaMovement();
            p.setDeltaMovement(v.x, 0.08, v.z);   // a little hop off the end
        }
    }

    // ------------------------------------------------------------------ ramps

    /**
     * Height of the skate ramp surface under (x, z), looking down from a little above {@code y},
     * or NaN if there's no ramp there (or something solid sits over it).
     */
    static double rampHeight(net.minecraft.world.level.Level level, double x, double z, double y) {
        int bx = Mth.floor(x), bz = Mth.floor(z);
        int from = Mth.floor(y + 0.6);
        for (int by = from; by >= from - 4; by--) {
            BlockPos bp = new BlockPos(bx, by, bz);
            BlockState st = level.getBlockState(bp);
            if (st.getBlock() instanceof SkateRampBlock) {
                double h = SkateRampBlock.surface(st, x - bx, z - bz);
                if (h > 1e-4) return by + h;
                continue;
            }
            if (!st.getCollisionShape(level, bp).isEmpty()) return Double.NaN;
        }
        return Double.NaN;
    }

    /** Rise per block of travel along {@code yaw} at (x, z). */
    private static double rampSlope(net.minecraft.world.level.Level level, double x, double z, double h, float yaw) {
        Vec3 f = dir(yaw);
        double e = 0.15;
        double a = rampHeight(level, x + f.x * e, z + f.z * e, h + 0.3), b = rampHeight(level, x - f.x * e, z - f.z * e, h + 0.3);
        if (Double.isNaN(a) && Double.isNaN(b)) return 0;
        if (Double.isNaN(a)) return (h - b) / e;
        if (Double.isNaN(b)) return (a - h) / e;
        return (a - b) / (2 * e);
    }

    /** Which way is straight down the ramp here, as a yaw. */
    private static float downhill(net.minecraft.world.level.Level level, double x, double z, double h) {
        double gx = rampSlope(level, x, z, h, -90), gz = rampSlope(level, x, z, h, 0);   // rise toward +x, toward +z
        return (float) (Mth.atan2(-gz, -gx) * Mth.RAD_TO_DEG) - 90.0f;
    }

    private static void tickRamp(LocalPlayer p, double rh, boolean w, boolean s, int steer, boolean space, boolean grabHeld) {
        var level = p.level();
        if (!onRamp && airTicks > 1) {
            // coming down onto the ramp: pick up the line of travel and the speed along the surface
            Vec3 v = p.getDeltaMovement();
            float newHeading = v.horizontalDistance() > 0.06 ? (float) (Mth.atan2(v.z, v.x) * Mth.RAD_TO_DEG) - 90.0f : downhill(level, p.getX(), p.getZ(), rh);
            if (Math.abs(Mth.wrapDegrees(newHeading - heading)) > 90) {   // coming back the way we went up
                fakie = !fakie;
                tilt = -tilt;
                flipTotal = -flipTotal;
            }
            heading = newHeading;
            double sl = rampSlope(level, p.getX(), p.getZ(), rh, heading);
            landTilt = (float) Math.toDegrees(Math.atan(sl));
            double c = 1 / Math.sqrt(1 + sl * sl), sn = sl * c;
            double along = v.horizontalDistance() * c + v.y * sn;
            if (sl < -0.4) lastVy = 0;   // a transition takes the drop
            vertAir = false;
            land(p, grabHeld);
            if (bailTicks > 0) return;
            speed = Math.max(0.04, along);
        }
        onRamp = true;
        airTicks = 0;
        double sl = rampSlope(level, p.getX(), p.getZ(), rh, heading);
        double c = 1 / Math.sqrt(1 + sl * sl), sn = sl * c;
        // gravity along the slope; pumping (W) on the way down; S drags a foot
        speed -= 0.05 * sn;
        if (w && sl < -0.15) speed += 0.011;
        if (s) speed = Math.max(0, speed - 0.012);
        speed *= 0.997;
        if (speed < 0) {   // ran out of speed going up: roll back down fakie
            heading += 180;
            fakie = !fakie;
            tilt = -tilt;
            speed = -speed;
            sl = -sl;
            sn = -sn;
        }
        speed = Math.min(speed, 1.05);
        heading -= steer * (float) Math.max(1.5, 4.5 - speed * 4) * (float) c;
        lean = Mth.lerp(0.3f, lean, steer * (float) Math.min(1, speed * 3));
        facing = heading;
        rampSn = sn;
        rampC = c;
        rampPitch = (float) Mth.clamp(Math.toDegrees(Math.atan(sl)), -80, 80);
        tilt = ease(tilt, rampPitch, 0.5f);
        Vec3 fwd = dir(heading);
        // up the wall: something solid just ahead at the top, or the ramp ends while it's steep, go straight up
        if (sl > 1.3) {
            BlockPos ahead = BlockPos.containing(p.getX() + fwd.x * 0.45, p.getY() + 0.6, p.getZ() + fwd.z * 0.45);
            BlockState as = level.getBlockState(ahead);
            boolean wall = !(as.getBlock() instanceof SkateRampBlock) && !as.getCollisionShape(level, ahead).isEmpty();
            boolean lip = Double.isNaN(rampHeight(level, p.getX() + fwd.x * 0.45, p.getZ() + fwd.z * 0.45, p.getY() + 0.5));
            if (wall || lip || p.horizontalCollision) {
                launchVert(p, fwd);
                return;
            }
        } else if (p.horizontalCollision && speed > 0.3) {
            bail(p, "Slammed");
            return;
        }
        // tricks off the ramp
        boolean popped = gesture(p, true);
        if (!popped && space && popCooldown == 0) {
            flickSpeed = 0.5f;
            pop(p, Trick.OLLIE);
            popped = true;
        }
        if (popped) {
            Vec3 v = p.getDeltaMovement();
            p.setDeltaMovement(fwd.x * speed, v.y, fwd.z * speed);
            return;
        }
        pose = prep != 0 ? SkatePayloads.CROUCH : sl < -0.4 ? SkatePayloads.CROUCH : SkatePayloads.RIDE;
        manualTicks = 0;
        // step along the surface
        double hd = speed * c;
        double nx = p.getX() + fwd.x * hd, nz = p.getZ() + fwd.z * hd;
        double nh = rampHeight(level, nx, nz, rh + Math.max(0.3, speed * Math.abs(sn) + 0.3));
        if (Double.isNaN(nh)) {
            onRamp = false;
            // onto flat ground at the same height (the top of a funbox), or off the lip into the air
            double gt = top(p, BlockPos.containing(nx, rh - 0.05, nz));
            if (!Double.isNaN(gt) && Math.abs(gt - rh) < 0.15) {
                p.setDeltaMovement(fwd.x * hd, gt - p.getY() + 0.01, fwd.z * hd);
                speed = hd;
                rampPitch = 0;
                return;
            }
            p.setDeltaMovement(fwd.x * hd, speed * sn, fwd.z * hd);
            speed = hd;
            rampPitch = 0;
            if (sn > 0.25) {
                airTicks = 2;
                wasOnGround = false;
                pose = SkatePayloads.AIR;
            }
            return;
        }
        p.setDeltaMovement(nx - p.getX(), nh - p.getY() + 0.001, nz - p.getZ());
        if (speed > 0.08 && p.tickCount % 3 == 0 && Math.abs(sl) < 0.2) dust(p, 1);
    }

    /** Off the top of a quarter pipe: straight up, to come back down into the same wall. */
    private static void launchVert(LocalPlayer p, Vec3 fwd) {
        double vy = Math.min(1.25, speed * 1.25);
        onRamp = false;
        vertAir = true;
        airTicks = 2;
        wasOnGround = false;
        pose = SkatePayloads.AIR;
        rampPitch = 0;
        p.setDeltaMovement(-fwd.x * 0.03, vy, -fwd.z * 0.03);
        if (vy > 0.5) popup("§bAIR", 0x40D8FF);
        if (vy > 0.35) addToLine("Air", (int) (vy * 200));
    }

    // ------------------------------------------------------------------ scoring

    private static void addToLine(String name, int points) {
        line.add(name);
        linePoints += points;
        lineMult = (int) line.stream().distinct().count();
        idleTicks = 0;
    }

    private static void bankLine(LocalPlayer p) {
        int total = linePoints * Math.max(1, lineMult);
        totalScore += total;
        dev.freerun.client.StreamChat.skateLanded(total);
        String text = line.size() == 1 ? line.get(0) : line.size() + " tricks";
        popup("§a§l+" + String.format("%,d", total) + " §r§f" + text, 0x60F080);
        if (total > 4000) sound(p, net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, 0.4f, 1.6f);
        line.clear();
        linePoints = 0;
        lineMult = 1;
    }

    static void popup(String text, int color) {
        popups.add(new Popup(text, color, new int[]{50}));
        while (popups.size() > 4) popups.remove(0);
    }

    // ------------------------------------------------------------------ helpers

    static Vec3 dir(float yaw) {
        float r = yaw * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(r), 0, Mth.cos(r));
    }

    private static void dust(LocalPlayer p, int n) {
        for (int i = 0; i < n; i++) {
            p.level().addParticle(ParticleTypes.POOF, p.getX() + (p.getRandom().nextFloat() - 0.5) * 0.6, p.getY() + 0.05,
                    p.getZ() + (p.getRandom().nextFloat() - 0.5) * 0.6, 0, 0.02, 0);
        }
    }

    private static void sound(LocalPlayer p, SoundEvent s, float vol, float pitch) {
        p.playSound(s, vol, pitch);
    }

    private static void send(LocalPlayer p, boolean force) {
        if (p == null || !ClientPlayNetworking.canSend(SkatePayloads.State.TYPE)) return;
        SkatePayloads.State s = localState(p.getId());
        s = new SkatePayloads.State(s.entityId(), active, s.deck(), s.heading(), s.roll(), s.spin(), s.body(), s.pitch(), s.tilt(), s.pose(), s.lean(), event);
        boolean changed = s.on() != lastSent.on() || s.pose() != lastSent.pose() || event != 0
                || Math.abs(s.roll() - lastSent.roll()) > 1 || Math.abs(s.spin() - lastSent.spin()) > 1
                || Math.abs(s.heading() - lastSent.heading()) > 2 || Math.abs(s.pitch() - lastSent.pitch()) > 1
                || Math.abs(s.tilt() - lastSent.tilt()) > 1.5;
        if (!force && !changed && p.tickCount % 10 != 0) return;
        ClientPlayNetworking.send(s);
        lastSent = s;
    }

    public static float tiltLerp(float partial) {
        return tiltO + Mth.wrapDegrees(tilt - tiltO) * partial;
    }

    public static float leanLerp(float partial) {
        return Mth.lerp(partial, leanO, lean);
    }

    public static int pushTicks() {
        return pushTicks;
    }
}
