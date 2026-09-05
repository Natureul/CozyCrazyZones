package com.natureul.cozycrazyzones;

/**
 * One continuous low-frequency policy shared by biome conversion and physical terrain shaping.
 *
 * The old implementation made a hard decision about ocean biome identity and then separately
 * raised arbitrary water columns. That could produce artificial chunk/biome-shaped cliffs. This
 * policy instead exposes both the keep-ocean decision and a continuous land strength so a native
 * ocean basin can become shallow shelf -> coast -> inland terrain over hundreds of blocks.
 */
public final class HearthlandsOceanPolicy {
    private HearthlandsOceanPolicy() {}

    public static boolean keepOcean(RegionalCell cell, long seed, int blockX, int blockZ) {
        if (cell.radialZone() != Region.HEARTHLANDS) return true;
        return field(seed, blockX, blockZ) > threshold(cell.distanceFromSpawn());
    }

    /**
     * 0 = retained ocean / very edge of converted land, 1 = strongly inland.
     * Callers should still require proof that the native biome was actually ocean.
     */
    public static double convertedLandStrength(RegionalCell cell, long seed, int blockX, int blockZ) {
        if (cell.radialZone() != Region.HEARTHLANDS) return 0.0D;
        double delta = threshold(cell.distanceFromSpawn()) - field(seed, blockX, blockZ);
        if (delta <= 0.0D) return 0.0D;
        return smoothstep(0.0D, 0.58D, delta);
    }

    private static double field(long seed, int x, int z) {
        // Deliberately continental scales: no checkerboard coastlines.
        return 0.68D * RegionalNoise.sample(seed ^ 0x2CE16A3B5DL, x, z, 1050.0D)
                + 0.32D * RegionalNoise.sample(seed ^ 0x6A09E667F3L, x, z, 2100.0D);
    }

    private static double threshold(double distance) {
        // The inner core is effectively guaranteed land. From there, bays and coasts return
        // organically, and by ~2450 the original ocean geography is fully restored before the
        // 2500-block Frontier boundary. This avoids drawing a circular coastline on the tier edge.
        if (distance <= 650.0D) return 1.35D;
        if (distance <= 1000.0D) {
            return lerp(1.35D, 0.70D, smoothstep(650.0D, 1000.0D, distance));
        }
        if (distance <= 1200.0D) {
            return lerp(0.70D, 0.58D, smoothstep(1000.0D, 1200.0D, distance));
        }
        if (distance <= 1750.0D) {
            return lerp(0.58D, 0.34D, smoothstep(1200.0D, 1750.0D, distance));
        }
        if (distance <= 2450.0D) {
            return lerp(0.34D, -1.05D, smoothstep(1750.0D, 2450.0D, distance));
        }
        return -1.05D;
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double smoothstep(double edge0, double edge1, double value) {
        if (edge1 <= edge0) return value >= edge1 ? 1.0D : 0.0D;
        double t = Math.max(0.0D, Math.min(1.0D, (value - edge0) / (edge1 - edge0)));
        return t * t * (3.0D - 2.0D * t);
    }
}
