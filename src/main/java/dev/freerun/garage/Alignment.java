package dev.freerun.garage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;

/**
 * A car's wheel alignment, in degrees: camber and toe on all four wheels, caster on the fronts.
 *
 *   camber: the wheel's tilt in or out at the top, seen from the front (negative = top in)
 *   caster: the steering axis's tilt back, seen from the side; more caster = more self-centring
 *   toe:    the wheel's angle in or out, seen from above (positive = toe-in)
 *
 * What the driver feels: a side-to-side difference in camber or caster makes the car pull to one
 * side; toe that's off scrubs the tyres, which costs a little speed.
 */
public final class Alignment {
    // specs: target and tolerance
    public static final double CAMBER = -0.5, CAMBER_TOL = 0.5;
    public static final double CASTER = 4.0, CASTER_TOL = 0.5;
    public static final double TOE = 0.10, TOE_TOL = 0.06;

    /** fl, fr, rl, rr camber; fl, fr caster; fl, fr, rl, rr toe. */
    public final double[] camber = new double[4], caster = new double[2], toe = new double[4];

    public static Alignment perfect() {
        Alignment a = new Alignment();
        for (int i = 0; i < 4; i++) {
            a.camber[i] = CAMBER;
            a.toe[i] = TOE;
        }
        a.caster[0] = a.caster[1] = CASTER;
        return a;
    }

    /** A car that's been around: hit a few potholes. */
    public static Alignment worn(RandomSource r) {
        Alignment a = perfect();
        for (int i = 0; i < 4; i++) {
            a.camber[i] += r.nextGaussian() * 0.7;
            a.toe[i] += r.nextGaussian() * 0.15;
        }
        for (int i = 0; i < 2; i++) a.caster[i] += r.nextGaussian() * 0.8;
        a.round();
        return a;
    }

    public void round() {
        for (int i = 0; i < 4; i++) {
            camber[i] = Math.round(camber[i] * 100) / 100.0;
            toe[i] = Math.round(toe[i] * 100) / 100.0;
        }
        for (int i = 0; i < 2; i++) caster[i] = Math.round(caster[i] * 100) / 100.0;
    }

    public static boolean ok(double v, double spec, double tol) {
        return Math.abs(v - spec) <= tol + 1.0E-6;
    }

    public boolean inSpec() {
        for (int i = 0; i < 4; i++) if (!ok(camber[i], CAMBER, CAMBER_TOL) || !ok(toe[i], TOE, TOE_TOL)) return false;
        for (int i = 0; i < 2; i++) if (!ok(caster[i], CASTER, CASTER_TOL)) return false;
        // and no side-to-side difference big enough to pull
        return Math.abs(camber[0] - camber[1]) <= 0.5 && Math.abs(caster[0] - caster[1]) <= 0.5;
    }

    /** Steering drift while driving (positive pulls right): from cross-camber and cross-caster. */
    public double pull() {
        double crossCamber = camber[0] - camber[1];   // a car pulls toward the side with more positive camber
        double crossCaster = caster[1] - caster[0];   // ...and toward the side with less caster
        return Math.max(-0.25, Math.min(0.25, -crossCamber * 0.08 + crossCaster * 0.05));
    }

    /** Fraction of top speed lost to tyre scrub from toe that's out of spec. */
    public double scrub() {
        double s = 0;
        for (double t : toe) s += Math.max(0, Math.abs(t - TOE) - TOE_TOL);
        return Math.min(0.15, s * 0.12);
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        for (int i = 0; i < 4; i++) {
            t.putDouble("C" + i, camber[i]);
            t.putDouble("T" + i, toe[i]);
        }
        for (int i = 0; i < 2; i++) t.putDouble("K" + i, caster[i]);
        return t;
    }

    public static Alignment load(CompoundTag t) {
        if (t == null || !t.contains("C0")) return perfect();
        Alignment a = new Alignment();
        for (int i = 0; i < 4; i++) {
            a.camber[i] = t.getDouble("C" + i);
            a.toe[i] = t.getDouble("T" + i);
        }
        for (int i = 0; i < 2; i++) a.caster[i] = t.getDouble("K" + i);
        return a;
    }
}
