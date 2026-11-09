package dev.freerun.skate;

/**
 * The shapes a skate ramp block can take. Each is a height curve h(t) in [0, 1] across one block,
 * where t runs from 0 at the low side to 1 at the high side (the ramp's facing).
 *
 * <ul>
 *   <li>0: kicker, a straight 45° ramp.</li>
 *   <li>1, 2: a two-block bank, the low half and the high half.</li>
 *   <li>3 to 11: one block of a quarter pipe with a 3-block radius. Index = 3 + col * 3 + row,
 *   where col is how many blocks from the bottom of the transition (0 to 2) and row is the
 *   block layer (0 to 2). Stacked together they make a curve that goes vertical at the top.</li>
 * </ul>
 */
public final class RampProfiles {
    public static final int KICKER = 0, BANK_LOW = 1, BANK_HIGH = 2, QP = 3;
    public static final int COUNT = 12;
    public static final double R = 3.0;

    private RampProfiles() {}

    public static int qp(int col, int row) {
        return QP + col * 3 + row;
    }

    /** The quarter pipe's height (0 to R) at distance x (0 to R) from the start of the curve. */
    public static double qpHeight(double x) {
        x = Math.max(0, Math.min(R, x));
        return R - Math.sqrt(Math.max(0, R * R - x * x));
    }

    /** Height in [0, 1] of profile {@code p} at {@code t}. */
    public static double height(int p, double t) {
        t = Math.max(0, Math.min(1, t));
        if (p == KICKER) return t;
        if (p == BANK_LOW) return 0.5 * t;
        if (p == BANK_HIGH) return 0.5 + 0.5 * t;
        if (p >= QP && p < QP + 9) {
            int col = (p - QP) / 3, row = (p - QP) % 3;
            return Math.max(0, Math.min(1, qpHeight(col + t) - row));
        }
        return 0;
    }

    /** Is this quarter pipe block anything at all (and not simply full)? 0 empty, 1 ramp, 2 full. */
    public static int qpKind(int col, int row) {
        double lo = qpHeight(col) - row, hi = qpHeight(col + 1) - row;
        if (hi <= 0.001) return 0;
        if (lo >= 0.999) return 2;
        return 1;
    }
}
