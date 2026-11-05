package dev.freerun.laser;

import java.util.ArrayList;
import java.util.List;

/**
 * A part drawn on the CNC terminal: a {@value #N} x {@value #N} grid of cells, packed into longs
 * (row-major, bit i of long i/64). Shared by the server (cutting) and the client (drawing and
 * rendering the finished part).
 */
public final class LaserShape {
    public static final int N = 32;
    public static final int LONGS = N * N / 64;

    private LaserShape() {}

    public static boolean get(long[] s, int x, int y) {
        if (s == null || x < 0 || y < 0 || x >= N || y >= N) return false;
        int i = y * N + x;
        return i / 64 < s.length && (s[i / 64] >>> (i % 64) & 1L) != 0;
    }

    public static void set(long[] s, int x, int y, boolean on) {
        if (x < 0 || y < 0 || x >= N || y >= N) return;
        int i = y * N + x;
        if (on) s[i / 64] |= 1L << (i % 64);
        else s[i / 64] &= ~(1L << (i % 64));
    }

    public static int count(long[] s) {
        int n = 0;
        for (long l : s) n += Long.bitCount(l);
        return n;
    }

    /** Cell edges the beam has to cut along (filled next to empty or the sheet edge). */
    public static int perimeter(long[] s) {
        int p = 0;
        for (int y = 0; y < N; y++) for (int x = 0; x < N; x++) {
            if (!get(s, x, y)) continue;
            if (!get(s, x - 1, y)) p++;
            if (!get(s, x + 1, y)) p++;
            if (!get(s, x, y - 1)) p++;
            if (!get(s, x, y + 1)) p++;
        }
        return p;
    }

    /** Separate pieces (4-connected islands): each one needs its own pierce. */
    public static int pierces(long[] s) {
        boolean[] seen = new boolean[N * N];
        int n = 0;
        int[] stack = new int[N * N];
        for (int i = 0; i < N * N; i++) {
            if (seen[i] || !get(s, i % N, i / N)) continue;
            n++;
            int sp = 0;
            stack[sp++] = i;
            seen[i] = true;
            while (sp > 0) {
                int c = stack[--sp], cx = c % N, cy = c / N;
                int[][] nb = {{cx - 1, cy}, {cx + 1, cy}, {cx, cy - 1}, {cx, cy + 1}};
                for (int[] q : nb) {
                    if (q[0] < 0 || q[1] < 0 || q[0] >= N || q[1] >= N) continue;
                    int j = q[1] * N + q[0];
                    if (!seen[j] && get(s, q[0], q[1])) {
                        seen[j] = true;
                        stack[sp++] = j;
                    }
                }
            }
        }
        return n;
    }

    /**
     * The order the cutting head visits the outline: boundary cells, each hop to the nearest one
     * not yet cut (so it follows contours instead of raster-scanning). Deterministic, so the
     * server and every client trace the same path.
     */
    public static List<int[]> path(long[] s) {
        List<int[]> edge = new ArrayList<>();
        for (int y = 0; y < N; y++) for (int x = 0; x < N; x++) {
            if (get(s, x, y) && (!get(s, x - 1, y) || !get(s, x + 1, y) || !get(s, x, y - 1) || !get(s, x, y + 1))) {
                edge.add(new int[]{x, y});
            }
        }
        List<int[]> out = new ArrayList<>(edge.size());
        boolean[] used = new boolean[edge.size()];
        int cur = edge.isEmpty() ? -1 : 0;
        while (cur >= 0) {
            used[cur] = true;
            out.add(edge.get(cur));
            int[] c = edge.get(cur);
            int best = -1, bestD = Integer.MAX_VALUE;
            for (int i = 0; i < edge.size(); i++) {
                if (used[i]) continue;
                int[] e = edge.get(i);
                int d = (e[0] - c[0]) * (e[0] - c[0]) + (e[1] - c[1]) * (e[1] - c[1]);
                if (d < bestD) {
                    bestD = d;
                    best = i;
                    if (d <= 2) break;
                }
            }
            cur = best;
        }
        return out;
    }

    public static boolean empty(long[] s) {
        if (s == null) return true;
        for (long l : s) if (l != 0) return false;
        return true;
    }

    public static long[] copy(long[] s) {
        long[] out = new long[LONGS];
        if (s != null) System.arraycopy(s, 0, out, 0, Math.min(s.length, LONGS));
        return out;
    }
}
