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
 * and stop signs (and actually wait at the sign), brake for anything in front of the car, then
 * pick a random way to go at each intersection and steer through the turn into the new lane.
 * It works out throttle and steering, and the car's own physics does the rest.
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
    private boolean stopped;   // already came to a full stop at this stop sign
    private int waited;
    private int stuck;
    private int honkCooldown;
    public int stallTicks;
    public int joyrideTicks;

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
        if (heading < 0 && !locate(car, p)) return;
        if (stallTicks > 0) {
            stallTicks--;
            if (stallTicks % 30 == 0) honk(car);
            brakeTo(car, 0.0);
            return;
        }
        boolean wild = joyrideTicks > 0;
        if (wild) joyrideTicks--;

        Vec3 aim;
        double target = wild ? 0.95 : CRUISE;
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
                    if (edge <= STOP_AT + 0.6 && Math.abs(car.speed()) < 0.03) {
                        if (++waited > 25 && !intersectionBusy(car, city)) {
                            stopped = true;
                            waited = 0;
                        }
                    }
                }
            }
            if (mustStop) target = Math.min(target, Math.max(0.0, (edge - STOP_AT) * 0.09));
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
                heading = turnTo;
                road = (heading == CityLayout.EAST || heading == CityLayout.WEST) ? nextJ : nextI;
                turnTo = -1;
                stopped = false;
                stage = Stage.FOLLOW;
                if (!findNext(p)) heading = -1;
            }
        }

        // anything in front of us?
        if (!wild) {
            double gap = clearAhead(car);
            if (gap < 9.0) {
                target = Math.min(target, Math.max(0.0, (gap - 4.0) * 0.07));
                if (gap < 5.0 && honkCooldown <= 0 && car.getRandom().nextInt(60) == 0) honk(car);
            }
        }

        steerToward(car, p, aim, wild ? 1.0f : 1.35f);
        brakeTo(car, target);

        // wedged on something for a long while: put it back in its lane
        if (target > 0.1 && Math.abs(car.speed()) < 0.02) {
            if (++stuck > 300 && car.level().getNearestPlayer(car, 24.0) == null) {
                Vec3 out = CityLayout.exitPoint(nextI, nextJ, turnTo >= 0 ? turnTo : heading, 4.0);
                int h2 = turnTo >= 0 ? turnTo : heading;
                if (CityLayout.canLeave(nextI, nextJ, h2)) {
                    car.moveTo(city.getX() + out.x, car.getY(), city.getZ() + out.z, CityLayout.yaw(h2), 0.0f);
                    heading = -1;
                    stage = Stage.FOLLOW;
                    turnTo = -1;
                }
                stuck = 0;
            }
        } else {
            stuck = 0;
        }
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

    /** Distance to the nearest car or creature in our path, up to 12 blocks. */
    private double clearAhead(CarEntity car) {
        Vec3 f = car.forwardVec();
        Vec3 c = car.position();
        AABB box = car.getBoundingBox().inflate(0.2).expandTowards(f.scale(10.0));
        double best = 99.0;
        List<Entity> found = car.level().getEntities(car, box, e -> e.isAlive() && !e.isPassengerOfSameVehicle(car)
                && (e instanceof CarEntity || e instanceof LivingEntity) && e.getVehicle() == null);
        for (Entity e : found) {
            Vec3 d = e.position().subtract(c);
            double along = d.dot(f);
            double side = Math.abs(d.x * f.z - d.z * f.x);
            if (along > 0.5 && side < (e instanceof CarEntity ? 2.0 : 1.6)) best = Math.min(best, along);
        }
        return best;
    }

    private boolean intersectionBusy(CarEntity car, BlockPos city) {
        double x0 = city.getX() + CityLayout.XS[nextI], z0 = city.getZ() + CityLayout.ZS[nextJ];
        AABB box = new AABB(x0, car.getY() - 1, z0, x0 + CityLayout.ROAD, car.getY() + 3, z0 + CityLayout.ROAD);
        return !car.level().getEntitiesOfClass(CarEntity.class, box, c -> c != car && Math.abs(c.speed()) > 0.02).isEmpty();
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
    }
}
