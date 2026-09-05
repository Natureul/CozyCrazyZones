package com.natureul.cozycrazyzones;

/**
 * Tiny deterministic 2D value-noise helper used for broad geography masks.
 * No allocations, no external noise library, and intentionally low-frequency.
 */
public final class RegionalNoise {
    private RegionalNoise() {}

    public static double sample(long seed, double x, double z, double scale) {
        if (scale <= 0.0D) return 0.0D;

        double gx = x / scale;
        double gz = z / scale;
        long x0 = fastFloor(gx);
        long z0 = fastFloor(gz);
        long x1 = x0 + 1L;
        long z1 = z0 + 1L;

        double tx = gx - x0;
        double tz = gz - z0;
        tx = fade(tx);
        tz = fade(tz);

        double n00 = corner(seed, x0, z0);
        double n10 = corner(seed, x1, z0);
        double n01 = corner(seed, x0, z1);
        double n11 = corner(seed, x1, z1);

        double nx0 = lerp(tx, n00, n10);
        double nx1 = lerp(tx, n01, n11);
        return lerp(tz, nx0, nx1);
    }

    public static double fractal(long seed, double x, double z, double baseScale) {
        return 0.60D * sample(seed, x, z, baseScale)
                + 0.28D * sample(seed ^ 0x9E3779B97F4A7C15L, x, z, baseScale * 0.5D)
                + 0.12D * sample(seed ^ 0xD1B54A32D192ED03L, x, z, baseScale * 0.25D);
    }

    private static long fastFloor(double value) {
        long whole = (long) value;
        return value < whole ? whole - 1L : whole;
    }

    private static double corner(long seed, long x, long z) {
        long value = seed
                ^ (x * 0x9E3779B97F4A7C15L)
                ^ (z * 0xC2B2AE3D27D4EB4FL);
        value = mix64(value);
        double unit = (double) (value >>> 11) * 0x1.0p-53;
        return unit * 2.0D - 1.0D;
    }

    private static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    private static double fade(double t) {
        return t * t * (3.0D - 2.0D * t);
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }
}
