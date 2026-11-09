package dev.freerun.city;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The city's bus network. Each route is a clockwise loop around a rectangle of streets (so every
 * turn is a right turn, keeping to the right-hand lane). Stops are on every other block of street,
 * on the curb side, a shelter with a bench and a sign. Everything here is in city-local
 * coordinates and is the same on the server and the client, so the client can draw the bus where
 * the route says it is and show the next stop.
 */
public final class BusRoutes {
    /** Bus centre stops this far into the block of street, in the direction of travel. */
    static final double STOP_AT = 11.0;
    /** Shelters occupy these blocks along the street, in the direction of travel. */
    static final int SHELTER_FROM = 9, SHELTER_TO = 12;
    public static final double HALF_LENGTH = 4.5;

    public static final class Route {
        public final int id;
        public final String number, name;
        public final int color;
        final int i0, i1, j0, j1;
        float[] xs, zs, ss;
        public double length;
        public final List<Stop> stops = new ArrayList<>();
        /** Where the bus must stop for each intersection's lights or sign (bus centre), and which one. */
        final List<double[]> lines = new ArrayList<>();   // {s, i, j, alongX}
        /** Stretches through corners: {s0, s1}. */
        final List<double[]> corners = new ArrayList<>();

        Route(int id, String number, String name, int color, int i0, int i1, int j0, int j1) {
            this.id = id;
            this.number = number;
            this.name = name;
            this.color = color;
            this.i0 = i0;
            this.i1 = i1;
            this.j0 = j0;
            this.j1 = j1;
        }

        public String title() {
            return number + " " + name;
        }

        public Vec3 at(double s) {
            s = wrap(s);
            int k = index(s);
            float t = (float) ((s - ss[k]) / Math.max(1e-4, ss[k + 1] - ss[k]));
            return new Vec3(Mth.lerp(t, xs[k], xs[k + 1]), 0, Mth.lerp(t, zs[k], zs[k + 1]));
        }

        /** Yaw (Minecraft degrees) of the direction of travel at {@code s}. */
        public float yawAt(double s) {
            Vec3 a = at(s - 0.8), b = at(s + 0.8);
            return (float) (Mth.atan2(b.z - a.z, b.x - a.x) * Mth.RAD_TO_DEG) - 90.0f;
        }

        public double wrap(double s) {
            s %= length;
            return s < 0 ? s + length : s;
        }

        /** Distance from {@code from} forward to {@code to} along the loop. */
        public double ahead(double from, double to) {
            return wrap(to - from);
        }

        private int index(double s) {
            int lo = 0, hi = ss.length - 2;
            while (lo < hi) {
                int mid = (lo + hi + 1) >>> 1;
                if (ss[mid] <= s) lo = mid;
                else hi = mid - 1;
            }
            return lo;
        }

        /** The nearest point of the loop to a city-local position: {s, distance}. */
        public double[] nearest(double lx, double lz) {
            double best = Double.MAX_VALUE, bs = 0;
            for (int k = 0; k < xs.length; k += 2) {
                double dx = xs[k] - lx, dz = zs[k] - lz, d = dx * dx + dz * dz;
                if (d < best) {
                    best = d;
                    bs = ss[k];
                }
            }
            return new double[]{bs, Math.sqrt(best)};
        }

        public Stop nextStop(double s) {
            Stop best = null;
            double bd = Double.MAX_VALUE;
            for (Stop st : stops) {
                double d = ahead(s - 0.5, st.s);
                if (d < bd) {
                    bd = d;
                    best = st;
                }
            }
            return best;
        }
    }

    /** One stop: where the bus halts, where people wait, the shelter's road piece and its name. */
    public static final class Stop {
        public final Route route;
        public final int index;
        public final double s;
        /** City-local point on the sidewalk where people wait. */
        public final Vec3 wait;
        /** City-local block of the sign post. */
        public final int signX, signZ;
        public final boolean alongX;
        public final int line, seg;
        /** Travelling toward +x / +z on this street. */
        public final boolean positive;
        public String name;

        Stop(Route route, int index, double s, Vec3 wait, int signX, int signZ, boolean alongX, int line, int seg, boolean positive) {
            this.route = route;
            this.index = index;
            this.s = s;
            this.wait = wait;
            this.signX = signX;
            this.signZ = signZ;
            this.alongX = alongX;
            this.line = line;
            this.seg = seg;
            this.positive = positive;
        }
    }

    public static final Route[] ROUTES = {
            new Route(0, "1", "Downtown Loop", 0xE8B820, 7, 11, 7, 11),
            new Route(1, "2", "Crosstown", 0x2E7FD0, 3, 15, 3, 15),
            new Route(2, "3", "Westside", 0x3EA055, 1, 5, 9, 17),
            new Route(3, "4", "Factory Express", 0xD04030, 10, 17, 10, 17),
    };

    private static final String[] EW = {"Levee Rd", "Bayou St", "Cypress St", "Magnolia St", "Pelican St", "Pecan St", "Sugarcane St",
            "Oak St", "Crawfish Ln", "Main St", "Canal St", "Delta St", "Moss St", "Heron St", "Indigo St", "Willow St", "Marsh St",
            "Gator St", "Parish Line Rd"};

    static {
        for (Route r : ROUTES) build(r);
    }

    private BusRoutes() {}

    public static String ewName(int j) {
        return EW[Math.floorMod(j, EW.length)];
    }

    public static String nsName(int i) {
        int n = i + 1;
        String suf = (n % 100 >= 11 && n % 100 <= 13) ? "th" : n % 10 == 1 ? "st" : n % 10 == 2 ? "nd" : n % 10 == 3 ? "rd" : "th";
        return n + suf + " Ave";
    }

    public static Route route(int id) {
        return ROUTES[Math.floorMod(id, ROUTES.length)];
    }

    // ------------------------------------------------------------------ building the loops

    private static void build(Route r) {
        int R = CityLayout.ROAD;
        double[][] corner = {   // corner point, incoming direction, outgoing direction
                {CityLayout.XS[r.i1] + 3.5, CityLayout.ZS[r.j0] + 6.5, 1, 0, 0, 1},    // top right: east -> south
                {CityLayout.XS[r.i1] + 3.5, CityLayout.ZS[r.j1] + 3.5, 0, 1, -1, 0},   // bottom right: south -> west
                {CityLayout.XS[r.i0] + 6.5, CityLayout.ZS[r.j1] + 3.5, -1, 0, 0, -1},  // bottom left: west -> north
                {CityLayout.XS[r.i0] + 6.5, CityLayout.ZS[r.j0] + 6.5, 0, -1, 1, 0},   // top left: north -> east
        };
        double rad = 3.0;
        List<double[]> pts = new ArrayList<>();
        for (int k = 0; k < 4; k++) {
            double[] c = corner[k], prev = corner[(k + 3) % 4];
            // the straight from the previous corner's exit to this corner's entry
            double sx = prev[0] + prev[4] * rad, sz = prev[1] + prev[5] * rad;
            double ex = c[0] - c[2] * rad, ez = c[1] - c[3] * rad;
            double len = Math.hypot(ex - sx, ez - sz);
            int n = Math.max(1, (int) Math.ceil(len / 0.5));
            for (int i = 0; i < n; i++) pts.add(new double[]{sx + (ex - sx) * i / n, sz + (ez - sz) * i / n});
            // the quarter-circle turn
            double cx = c[0] - c[2] * rad + c[4] * rad, cz = c[1] - c[3] * rad + c[5] * rad;
            for (int i = 0; i < 10; i++) {
                double th = i / 10.0 * Math.PI / 2;
                pts.add(new double[]{cx - c[4] * rad * Math.cos(th) + c[2] * rad * Math.sin(th),
                        cz - c[5] * rad * Math.cos(th) + c[3] * rad * Math.sin(th)});
            }
        }
        pts.add(pts.get(0));
        int n = pts.size();
        r.xs = new float[n];
        r.zs = new float[n];
        r.ss = new float[n];
        double s = 0;
        for (int i = 0; i < n; i++) {
            if (i > 0) s += Math.hypot(pts.get(i)[0] - pts.get(i - 1)[0], pts.get(i)[1] - pts.get(i - 1)[1]);
            r.xs[i] = (float) pts.get(i)[0];
            r.zs[i] = (float) pts.get(i)[1];
            r.ss[i] = (float) s;
        }
        r.length = s;

        // the four streets in order: east along j0, south along i1, west along j1, north along i0
        int[][] edges = {{CityLayout.EAST, r.j0, r.i0, r.i1}, {CityLayout.SOUTH, r.i1, r.j0, r.j1},
                {CityLayout.WEST, r.j1, r.i1, r.i0}, {CityLayout.NORTH, r.i0, r.j1, r.j0}};
        for (int[] e : edges) {
            int h = e[0], line = e[1], from = e[2], to = e[3];
            boolean alongX = h == CityLayout.EAST || h == CityLayout.WEST;
            boolean positive = h == CityLayout.EAST || h == CityLayout.SOUTH;
            int step = to > from ? 1 : -1;
            for (int k = from; k != to; k += step) {
                int seg = Math.min(k, k + step);
                // the stretch of street between lines k and k + step
                double entry = positive ? CityLayout.XS[seg] + R : CityLayout.XS[seg + 1];   // same table for z
                int next = k + step;
                // stop line for the intersection at the end of this stretch
                double lineCoord = positive ? CityLayout.XS[next] - 1.0 : CityLayout.XS[next] + R + 1.0;
                double lane = CityLayout.laneCoord(h, line);
                Vec3 lp = alongX ? new Vec3(lineCoord, 0, lane) : new Vec3(lane, 0, lineCoord);
                double sl = r.nearest(lp.x, lp.z)[0] - HALF_LENGTH;
                r.lines.add(new double[]{sl, alongX ? next : line, alongX ? line : next, alongX ? 1 : 0});
                // a stop on every other stretch
                if (Math.floorMod(seg, 2) == 0) {
                    double along = positive ? entry + STOP_AT : entry - STOP_AT;
                    Vec3 bp = alongX ? new Vec3(along, 0, lane) : new Vec3(lane, 0, along);
                    double ss = r.nearest(bp.x, bp.z)[0];
                    int curbSide = curbColumn(h);   // 0 or 9 across the road
                    double wAlong = positive ? entry + SHELTER_TO + 1.5 : entry - SHELTER_TO - 1.5;
                    double wAcross = (alongX ? CityLayout.ZS[line] : CityLayout.XS[line]) + curbSide + 0.5;
                    Vec3 wait = alongX ? new Vec3(wAlong, 0, wAcross) : new Vec3(wAcross, 0, wAlong);
                    int signAlong = (int) Math.floor(positive ? entry + SHELTER_TO + 2 : entry - SHELTER_TO - 3);
                    int signAcross = (alongX ? CityLayout.ZS[line] : CityLayout.XS[line]) + (curbSide == 9 ? 8 : 1);
                    Stop st = new Stop(r, r.stops.size(), ss, wait, alongX ? signAlong : signAcross, alongX ? signAcross : signAlong,
                            alongX, line, seg, positive);
                    String street = alongX ? ewName(line) : nsName(line);
                    String cross = alongX ? nsName(next) : ewName(next);
                    st.name = street + " & " + cross;
                    r.stops.add(st);
                }
            }
        }
        // slow stretches: each corner's arc
        for (double[] c : corner) {
            double[] at = r.nearest(c[0] - c[2] * rad, c[1] - c[3] * rad);
            r.corners.add(new double[]{at[0], at[0] + Math.PI * rad / 2});
        }
        r.lines.sort((a, b) -> Double.compare(a[0], b[0]));
        r.stops.sort((a, b) -> Double.compare(a.s, b.s));
    }

    /** The sidewalk column (0 or 9 across the road) on the right of travel in heading h. */
    static int curbColumn(int h) {
        return switch (h) {
            case CityLayout.EAST, CityLayout.NORTH -> 9;
            default -> 0;
        };
    }

    /** The stop whose shelter is on this block of road, if any. */
    public static Stop stopOn(boolean alongX, int line, int seg) {
        for (Route r : ROUTES) for (Stop s : r.stops) if (s.alongX == alongX && s.line == line && s.seg == seg) return s;
        return null;
    }

    /** The stop whose sign post is at this city-local block. */
    public static Stop stopAtSign(int lx, int lz) {
        for (Route r : ROUTES) for (Stop s : r.stops) if (s.signX == lx && s.signZ == lz) return s;
        return null;
    }

    /** Stops of any route within {@code radius} of a city-local point. */
    public static List<Stop> stopsNear(double lx, double lz, double radius) {
        List<Stop> out = new ArrayList<>();
        for (Route r : ROUTES) for (Stop s : r.stops) if (Math.hypot(s.wait.x - lx, s.wait.z - lz) < radius) out.add(s);
        return out;
    }

    /** Name landmark stops after what's next to them. */
    static void nameLandmarks() {
        for (Route r : ROUTES) {
            for (Stop s : r.stops) {
                double off = s.wait.x, offz = s.wait.z;
                // the lot just beyond the sidewalk
                double lx = s.alongX ? off : off + (s.wait.x - (CityLayout.XS[s.line] + 5) > 0 ? 2 : -2);
                double lz = s.alongX ? offz + (s.wait.z - (CityLayout.ZS[s.line] + 5) > 0 ? 2 : -2) : offz;
                CityLayout.District d = CityLayout.districtAt(lx, lz);
                if (d != null && d.landmark()) s.name = d.title;
            }
        }
    }

    static {
        nameLandmarks();
    }
}
