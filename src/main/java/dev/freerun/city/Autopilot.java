package dev.freerun.city;

import dev.freerun.vehicle.CarEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * How a resident drives: keep to the right-hand lane, slow for intersections, stop at red lights
 * and stop signs (and actually wait at the sign, taking turns with the other drivers), brake for
 * anything in front of the car, don't pull into an intersection with nowhere to go, then pick a
 * random way to go at each intersection and steer through the turn into the new lane. It works
 * out throttle and steering; the car's own physics does the rest.
 *
 * Getting unstuck, in order: oncoming traffic and cars waiting at the other stop lines are
 * ignored when judging the road ahead; after eight seconds wedged, the car "ghosts" through other
 * cars for a few seconds; {@link TrafficControl} spots jams of several cars and ghosts them all;
 * and, as before, a car stuck for twenty seconds with nobody watching is put back in its lane.
 *
 * Police cars can also be sent straight to a spot ({@link #chase}), lights and sirens, ignoring
 * the lanes and the signals.
 */
public class Autopilot {
    private static final double CRUISE = 0.42;
    private static final double STOP_AT = 3.2;    // car centre this far before the intersection edge

    private enum Stage { FOLLOW, TURNING }

    private Stage stage = Stage.FOLLOW;
    private int heading = -1;
    private int road;          // the road line we're on (z index if driving E/W, x index if N/S)
    private int nextI, nextJ;  // the intersection ahead
    private int turnTo = -1;
    private boolean stopped;   // came to a full stop at this stop sign and it's our turn
    private int waited;
    private int stuck;
    private int boxWait;
    private int honkCooldown;
    private BlockPos claimed;
    public int stallTicks;
    public int joyrideTicks;
    /** World position to drive straight to (police response), or null. */
    public Vec3 chase;
    public double chaseStopAt = 6.0;

    public float throttle, steer;

    // ------------------------------------------------------------------ geometry helpers

    private static Vec3 local(CarEntity car, BlockPos city) {
        return car.position().subtract(city.getX(), city.getY(), city.getZ());
    }

    /** Works out heading, road and next intersection from wherever the car is. */
    private boolean locate(CarEntity car, Vec3 p) {
        float yaw = Mth.wrapDegrees(car.getYRot());
        int h = yaw >= -135 && yaw < -45 ? CityLayout.EAST : yaw >= -45 && yaw < 45 ? CityLayout.SOUTH
                : yaw >= 45 && yaw < 135 ? CityLayout.WEST : CityLayout.NORTH;
        int k = -1;
        if (h == CityLayout.EAST || h == CityLayout.WEST) {
            for (int j = 0; j < CityLayout.ZS.length; j++) if (p.z >= CityLayout.ZS[j] - 1 && p.z < CityLayout.ZS[j] + CityLayout.ROAD + 1) k = j;
        } else {
            for (int i = 0; i < CityLayout.XS.length; i++) if (p.x >= CityLayout.XS[i] - 1 && p.x < CityLayout.XS[i] + CityLayout.ROAD + 1) k = i;
        }
        if (k < 0) {
            // facing across a road we're sitting on: turn onto it instead
            for (int dh = 1; dh <= 3 && k < 0; dh += 2) {
                int h2 = (h + dh) % 4;
                if (h2 == CityLayout.EAST || h2 == CityLayout.WEST) {
                    for (int j = 0; j < CityLayout.ZS.length; j++) if (p.z >= CityLayout.ZS[j] && p.z < CityLayout.ZS[j] + CityLayout.ROAD) k = j;
                } else {
                    for (int i = 0; i < CityLayout.XS.length; i++) if (p.x >= CityLayout.XS[i] && p.x < CityLayout.XS[i] + CityLayout.ROAD) k = i;
                }
                if (k >= 0) h = h2;
            }
        }
        if (k < 0) return false;
        heading = h;
        road = k;
        return findNext(p);
    }

    private boolean findNext(Vec3 p) {
        switch (heading) {
            case CityLayout.EAST -> {
                nextI = -1;
                for (int i = 0; i < CityLayout.XS.length; i++) if (CityLayout.XS[i] + 2 > p.x) { nextI = i; break; }
                nextJ = road;
            }
            case CityLayout.WEST -> {
                nextI = -1;
                for (int i = CityLayout.XS.length - 1; i >= 0; i--) if (CityLayout.XS[i] + CityLayout.ROAD - 2 < p.x) { nextI = i; break; }
                nextJ = road;
            }
            case CityLayout.SOUTH -> {
                nextJ = -1;
                for (int j = 0; j < CityLayout.ZS.length; j++) if (CityLayout.ZS[j] + 2 > p.z) { nextJ = j; break; }
                nextI = road;
            }
            default -> {
                nextJ = -1;
                for (int j = CityLayout.ZS.length - 1; j >= 0; j--) if (CityLayout.ZS[j] + CityLayout.ROAD - 2 < p.z) { nextJ = j; break; }
                nextI = road;
            }
        }
        return nextI >= 0 && nextJ >= 0;
    }

    /** Distance from the car's centre to the near edge of the next intersection, along the heading. */
    private double toEdge(Vec3 p) {
        return switch (heading) {
            case CityLayout.EAST -> CityLayout.XS[nextI] - p.x;
            case CityLayout.WEST -> p.x - (CityLayout.XS[nextI] + CityLayout.ROAD);
            case CityLayout.SOUTH -> CityLayout.ZS[nextJ] - p.z;
            default -> p.z - (CityLayout.ZS[nextJ] + CityLayout.ROAD);
        };
    }

    private int chooseTurn(CarEntity car) {
        int[] options = {heading, (heading + 1) % 4, (heading + 3) % 4};   // straight, right, left
        int[] weights = {5, 3, 3};
        int total = 0;
        for (int i = 0; i < 3; i++) if (CityLayout.canLeave(nextI, nextJ, options[i])) total += weights[i];
        if (total == 0) return (heading + 2) % 4;   // dead end: turn around
        int roll = car.getRandom().nextInt(total);
        for (int i = 0; i < 3; i++) {
            if (!CityLayout.canLeave(nextI, nextJ, options[i])) continue;
            if ((roll -= weights[i]) < 0) return options[i];
        }
        return heading;
    }

    // ------------------------------------------------------------------ driving

    /** Decide this tick's throttle and steering. */
    public void step(CarEntity car, BlockPos city) {
        throttle = 0;
        steer = 0;
        if (honkCooldown > 0) honkCooldown--;
        Vec3 p = local(car, city);
        if (stallTicks > 0) {
            stallTicks--;
            if (stallTicks % 30 == 0) honk(car);
            brakeTo(car, 0.0);
            return;
        }
        if (chase != null) {
            chaseStep(car, p, chase.subtract(city.getX(), city.getY(), city.getZ()));
            return;
        }
        if (heading < 0 && !locate(car, p)) {
            backToRoad(car, p);
            return;
        }
        boolean wild = joyrideTicks > 0;
        if (wild) joyrideTicks--;

        Vec3 aim;
        double target = wild ? 0.95 : CRUISE;
        boolean waitingForSignal = false;
        if (stage == Stage.FOLLOW) {
            double edge = toEdge(p);
            if (edge < -CityLayout.ROAD) {   // somehow drove past it: re-read the map
                heading = -1;
                return;
            }
            if (turnTo < 0 && edge < 22) turnTo = chooseTurn(car);
            boolean turning = turnTo >= 0 && turnTo != heading;
            if (edge < 14) target = Math.min(target, wild ? target : turning ? 0.22 : 0.34);
            // signals
            BlockPos key = CityLayout.intersectionKey(city, nextI, nextJ);
            boolean mustStop = false;
            if (!wild) {
                if (CityLayout.hasLights(nextI, nextJ)) {
                    boolean alongX = heading == CityLayout.EAST || heading == CityLayout.WEST;
                    TrafficSystem.Signal s = TrafficSystem.signal(car.level(), key, alongX);
                    if (s == TrafficSystem.Signal.RED || (s == TrafficSystem.Signal.YELLOW && edge > STOP_AT + 1.5)) mustStop = edge > STOP_AT - 1.2;
                } else if (!stopped) {
                    mustStop = edge > STOP_AT - 1.2;
                    if (edge <= STOP_AT + 0.8 && Math.abs(car.speed()) < 0.03) {
                        // full stop, then wait our turn: first to the line goes first
                        if (++waited > 20 && TrafficControl.claim(car.level(), key, car)) {
                            stopped = true;
                            claimed = key;
                            waited = 0;
                        }
                    }
                }
                // don't block the box: only pull in if there's room on the other side
                if (!mustStop && edge < STOP_AT + 1.0 && edge > -0.5 && turnTo >= 0 && exitBlocked(car, city)) {
                    if (++boxWait < 160) mustStop = true;
                } else if (edge > STOP_AT + 4.0) {
                    boxWait = 0;
                }
            }
            if (mustStop) {
                target = Math.min(target, Math.max(0.0, (edge - STOP_AT) * 0.09));
                waitingForSignal = true;
            }
            // lane following: aim at a point a few blocks down our lane
            double lane = CityLayout.laneCoord(heading, road);
            double ahead = 5.0 + Math.abs(car.speed()) * 6.0;
            aim = switch (heading) {
                case CityLayout.EAST -> new Vec3(p.x + ahead, 0, lane);
                case CityLayout.WEST -> new Vec3(p.x - ahead, 0, lane);
                case CityLayout.SOUTH -> new Vec3(lane, 0, p.z + ahead);
                default -> new Vec3(lane, 0, p.z - ahead);
            };
            if (edge < 0.8 && !mustStop) {
                stage = Stage.TURNING;
                if (turnTo < 0) turnTo = chooseTurn(car);
            }
        } else {
            // through the intersection toward the exit lane
            Vec3 exit = CityLayout.exitPoint(nextI, nextJ, turnTo, 3.0);
            aim = exit;
            if (turnTo != heading) target = Math.min(target, wild ? 0.5 : 0.22);
            double past = switch (turnTo) {
                case CityLayout.EAST -> p.x - (CityLayout.XS[nextI] + CityLayout.ROAD);
                case CityLayout.WEST -> CityLayout.XS[nextI] - p.x;
                case CityLayout.SOUTH -> p.z - (CityLayout.ZS[nextJ] + CityLayout.ROAD);
                default -> CityLayout.ZS[nextJ] - p.z;
            };
            if (past > 0.5) {
                if (claimed != null) TrafficControl.release(car.level(), claimed, car);
                claimed = null;
                heading = turnTo;
                road = (heading == CityLayout.EAST || heading == CityLayout.WEST) ? nextJ : nextI;
                turnTo = -1;
                stopped = false;
                boxWait = 0;
                stage = Stage.FOLLOW;
                if (!findNext(p)) heading = -1;
            }
        }

        // anything in front of us?
        if (!wild && !car.ghosting()) {
            double gap = clearAhead(car, city);
            if (gap < 9.0) {
                target = Math.min(target, Math.max(0.0, (gap - 4.0) * 0.07));
                if (gap < 5.0 && honkCooldown <= 0 && car.getRandom().nextInt(60) == 0) honk(car);
            }
        }

        steerToward(car, p, aim, wild ? 1.0f : 1.35f);
        brakeTo(car, target);
        unstick(car, city, target, waitingForSignal);
    }

    /** Wedged with somewhere to go: first ghost through other cars, then (unwatched) hop back into the lane. */
    private void unstick(CarEntity car, BlockPos city, double target, boolean waitingForSignal) {
        if (waitingForSignal) {   // queued at a light or a stop sign is normal
            stuck = Math.max(0, stuck - 1);
            return;
        }
        if (Math.abs(car.speed()) < 0.02) {
            stuck++;
            if (stuck == 160) car.ghost(70);
            if (stuck > 400 && car.level().getNearestPlayer(car, 24.0) == null && heading >= 0) {
                int h2 = turnTo >= 0 ? turnTo : heading;
                if (CityLayout.canLeave(nextI, nextJ, h2)) {
                    Vec3 out = CityLayout.exitPoint(nextI, nextJ, h2, 4.0);
                    car.moveTo(city.getX() + out.x, car.getY(), city.getZ() + out.z, CityLayout.yaw(h2), 0.0f);
                    reset();
                }
                stuck = 0;
            }
        } else {
            stuck = Math.max(0, stuck - 4);
        }
    }

    public int stuckTicks() {
        return stuck;
    }

    /** The intersection this car is heading for (world key), or null. */
    public BlockPos nextKey(BlockPos city) {
        return heading < 0 ? null : CityLayout.intersectionKey(city, nextI, nextJ);
    }

    /** Off the road (after a chase, or knocked off): head for the nearest street and pick it up. */
    private void backToRoad(CarEntity car, Vec3 p) {
        double best = Double.MAX_VALUE;
        Vec3 aim = p;
        for (int x : CityLayout.XS) {
            double d = Math.abs(p.x - (x + 5));
            if (d < best) {
                best = d;
                aim = new Vec3(x + 5, 0, p.z);
            }
        }
        for (int z : CityLayout.ZS) {
            double d = Math.abs(p.z - (z + 5));
            if (d < best) {
                best = d;
                aim = new Vec3(p.x, 0, z + 5);
            }
        }
        steerToward(car, p, aim, 1.35f);
        brakeTo(car, 0.18);
        if (Math.abs(car.speed()) < 0.02 && ++stuck > 200) {
            if (car.level().getNearestPlayer(car, 24.0) == null) {
                car.moveTo(car.getX() + (aim.x - p.x), car.getY(), car.getZ() + (aim.z - p.z));
            }
            stuck = 0;
        }
    }

    /** Lights and siren: straight at the target, slowing to a stop {@link #chaseStopAt} blocks short. */
    private void chaseStep(CarEntity car, Vec3 p, Vec3 target) {
        double dx = target.x - p.x, dz = target.z - p.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        double want = dist < chaseStopAt ? 0.0 : Math.min(0.85, (dist - chaseStopAt) * 0.05 + 0.15);
        steerToward(car, p, target, 1.35f);
        if (Math.abs(Mth.wrapDegrees((float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0f - car.getYRot())) > 70) want = Math.min(want, 0.25);
        double gap = clearAhead(car, null);
        if (gap < 6.0) want = Math.min(want, Math.max(0.0, (gap - 3.5) * 0.08));
        brakeTo(car, want);
        if (want > 0.1 && Math.abs(car.speed()) < 0.02) {
            if (++stuck == 60) car.ghost(60);
        } else {
            stuck = 0;
        }
        heading = -1;   // re-read the map once the chase is over
    }

    private void steerToward(CarEntity car, Vec3 p, Vec3 aim, float authority) {
        double dx = aim.x - p.x, dz = aim.z - p.z;
        float desired = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0f;
        float diff = Mth.wrapDegrees(desired - car.getYRot());
        // positive steer turns left (yaw decreasing)
        steer = Mth.clamp(-diff / 18.0f, -authority, authority);
    }

    private void brakeTo(CarEntity car, double target) {
        double v = car.speed();
        throttle = (float) Mth.clamp((target - v) * 8.0, -1.0, 1.0);
        if (target <= 0.001 && v < 0.02) throttle = 0;
    }

    /**
     * Distance to whatever's really in our path, up to ~10 blocks. Oncoming cars, cars waiting
     * at the other stop lines, and people on the sidewalk don't count.
     */
    private double clearAhead(CarEntity car, BlockPos city) {
        Vec3 f = car.forwardVec();
        Vec3 c = car.position();
        AABB box = car.getBoundingBox().inflate(0.2).expandTowards(f.scale(10.0));
        double best = 99.0;
        List<Entity> found = car.level().getEntities(car, box, e -> e.isAlive() && !e.isPassengerOfSameVehicle(car)
                && (e instanceof CarEntity || e instanceof LivingEntity) && e.getVehicle() == null);
        // buses are long: check their nose, middle and tail
        for (BusEntity bus : car.level().getEntitiesOfClass(BusEntity.class, box.inflate(5.0, 0, 5.0))) {
            Vec3 bf = bus.forward().scale(BusRoutes.HALF_LENGTH - 0.3);
            for (Vec3 q : new Vec3[]{bus.position(), bus.position().add(bf), bus.position().subtract(bf)}) {
                Vec3 d = q.subtract(c);
                double along = d.dot(f), side = Math.abs(d.x * f.z - d.z * f.x);
                if (along <= 0.5) continue;
                double facing = bus.forward().dot(f);
                if (facing < -0.5 && side > 1.0) continue;   // oncoming
                if (side < 2.3) best = Math.min(best, along - 0.8);
            }
        }
        for (Entity e : found) {
            Vec3 d = e.position().subtract(c);
            double along = d.dot(f);
            double side = Math.abs(d.x * f.z - d.z * f.x);
            if (along <= 0.5) continue;
            if (e instanceof CarEntity other) {
                if (other.ghosting()) continue;
                double facing = other.forwardVec().dot(f);
                if (facing < -0.5 && side > 1.0) continue;                         // oncoming, in its own lane
                if (Math.abs(facing) < 0.5 && Math.abs(other.speed()) < 0.03
                        && !(along < 3.8 && side < 1.5)) continue;                   // waiting at a cross street
                if (side < 2.0) best = Math.min(best, along);
            } else {
                if (city != null) {
                    double lx = e.getX() - city.getX(), lz = e.getZ() - city.getZ();
                    if (!CityLayout.onRoad(lx, lz) || onSidewalk(lx, lz)) continue;
                }
                if (side < 1.4) best = Math.min(best, along);
            }
        }
        return best;
    }

    private static boolean onSidewalk(double lx, double lz) {
        for (int x : CityLayout.XS) {
            double o = lx - x;
            if (o >= 0 && o < CityLayout.ROAD && (o < 1.6 || o > CityLayout.ROAD - 1.6)) {
                boolean alsoCross = false;
                for (int z : CityLayout.ZS) if (lz >= z && lz < z + CityLayout.ROAD) alsoCross = true;
                if (!alsoCross) return true;
            }
        }
        for (int z : CityLayout.ZS) {
            double o = lz - z;
            if (o >= 0 && o < CityLayout.ROAD && (o < 1.6 || o > CityLayout.ROAD - 1.6)) {
                boolean alsoCross = false;
                for (int x : CityLayout.XS) if (lx >= x && lx < x + CityLayout.ROAD) alsoCross = true;
                if (!alsoCross) return true;
            }
        }
        return false;
    }

    /** A stopped car sitting just past where we'd come out of the intersection. */
    private boolean exitBlocked(CarEntity car, BlockPos city) {
        Vec3 out = CityLayout.exitPoint(nextI, nextJ, turnTo, 3.5).add(city.getX(), 0, city.getZ());
        AABB box = new AABB(out.x - 2.5, car.getY() - 1, out.z - 2.5, out.x + 2.5, car.getY() + 2, out.z + 2.5);
        return !car.level().getEntitiesOfClass(CarEntity.class, box, o -> o != car && !o.ghosting() && Math.abs(o.speed()) < 0.05).isEmpty();
    }

    private void honk(CarEntity car) {
        honkCooldown = 80;
        car.level().playSound(null, car.getX(), car.getY() + 1, car.getZ(), dev.freerun.economy.ModSounds.CAR_HORN,
                SoundSource.NEUTRAL, 1.4f, 0.9f + car.getRandom().nextFloat() * 0.2f);
    }

    public void reset() {
        heading = -1;
        stage = Stage.FOLLOW;
        turnTo = -1;
        stopped = false;
        stuck = 0;
        boxWait = 0;
        claimed = null;
    }
}
