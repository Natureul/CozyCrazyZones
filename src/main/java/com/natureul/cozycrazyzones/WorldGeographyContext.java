package com.natureul.cozycrazyzones;

import net.minecraft.core.BlockPos;

/**
 * Worldgen-safe geography context for code paths that do not have a ServerLevel reference
 * (notably BiomeSource during chunk generation).
 *
 * Before the Overworld's shared spawn is available we use the canonical world origin. Once the
 * server is started, the anchor is snapped to the actual shared spawn so gameplay and future
 * chunks use the same center. Initial spawn chunks are inside the neutral shared core either way.
 */
public final class WorldGeographyContext {
    private static final double TWO_PI = Math.PI * 2.0D;
    private static final double QUARTER_PI = Math.PI / 4.0D;

    private static volatile boolean prepared;
    private static volatile long worldSeed;
    private static volatile double anchorX = 0.5D;
    private static volatile double anchorZ = 0.5D;

    private WorldGeographyContext() {}

    public static void prepare(long seed) {
        worldSeed = seed;
        anchorX = 0.5D;
        anchorZ = 0.5D;
        prepared = true;
    }

    public static void setSharedSpawn(BlockPos spawn) {
        anchorX = spawn.getX() + 0.5D;
        anchorZ = spawn.getZ() + 0.5D;
    }

    public static void clear() {
        prepared = false;
    }

    public static boolean prepared() {
        return prepared;
    }

    public static long worldSeed() {
        return worldSeed;
    }

    public static RegionalCell cellAt(double x, double z) {
        double dx = x - anchorX;
        double dz = z - anchorZ;
        double distance = Math.hypot(dx, dz);
        return new RegionalCell(
                CozyZonesApi.regionForDistance(distance),
                CozyZonesApi.macroRegionForOffset(worldSeed, dx, dz),
                CozyZonesApi.influenceBandForDistance(distance),
                distance,
                CozyZonesApi.regionalStrengthForDistance(distance),
                boundaryStrength(dx, dz)
        );
    }

    private static double boundaryStrength(double dx, double dz) {
        double angle = warpedAngle(dx, dz);
        double quarter = Math.PI / 4.0D;
        double threeQuarter = Math.PI * 3.0D / 4.0D;
        double center;
        if (angle >= -quarter && angle < quarter) center = 0.0D;
        else if (angle >= quarter && angle < threeQuarter) center = Math.PI / 2.0D;
        else if (angle >= -threeQuarter && angle < -quarter) center = -Math.PI / 2.0D;
        else center = Math.PI;

        double local = Math.abs(normalizeAngle(angle - center));
        double edgeDistance = Math.max(0.0D, QUARTER_PI - local);
        double blendRadians = Math.toRadians(CozyZonesConfig.MACRO_BORDER_BLEND_DEGREES.get());
        return smoothstep(0.0D, blendRadians, edgeDistance);
    }

    private static double warpedAngle(double dx, double dz) {
        double distance = Math.hypot(dx, dz);
        double base = Math.atan2(dz, dx);
        double ramp = smoothstep(350.0D, 1250.0D, distance);

        double phase1 = phase(worldSeed ^ 0x4F9939F508L);
        double phase2 = phase(worldSeed ^ 0x1EF1565BD5L);
        double phase3 = phase(worldSeed ^ 0x6C8E9CF570L);

        double warp = Math.toRadians(11.0D) * Math.sin((dx + dz) / 2300.0D + phase1)
                + Math.toRadians(6.5D) * Math.sin((dx - dz) / 4300.0D + phase2)
                + Math.toRadians(3.5D) * Math.sin(distance / 3100.0D + phase3);
        return normalizeAngle(base + warp * ramp);
    }

    private static double normalizeAngle(double angle) {
        double normalized = angle % TWO_PI;
        if (normalized >= Math.PI) normalized -= TWO_PI;
        if (normalized < -Math.PI) normalized += TWO_PI;
        return normalized;
    }

    private static double phase(long value) {
        long mixed = mix64(value);
        double unit = (double) (mixed >>> 11) * 0x1.0p-53;
        return unit * TWO_PI;
    }

    private static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    private static double smoothstep(double edge0, double edge1, double value) {
        if (edge1 <= edge0) return value >= edge1 ? 1.0D : 0.0D;
        double t = Math.max(0.0D, Math.min(1.0D, (value - edge0) / (edge1 - edge0)));
        return t * t * (3.0D - 2.0D * t);
    }
}
