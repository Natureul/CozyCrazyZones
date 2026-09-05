package com.natureul.cozycrazyzones;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

public final class CozyZonesApi {
    private static final double TWO_PI = Math.PI * 2.0D;
    private static final double QUARTER_PI = Math.PI / 4.0D;
    private static final double THREE_QUARTER_PI = Math.PI * 3.0D / 4.0D;

    private CozyZonesApi() {}

    public static double distanceFromSpawn(ServerLevel level, double x, double z) {
        BlockPos spawn = level.getSharedSpawnPos();
        return Math.hypot(x - (spawn.getX() + 0.5D), z - (spawn.getZ() + 0.5D));
    }

    public static Region regionAt(ServerLevel level, double x, double z) {
        return regionForDistance(distanceFromSpawn(level, x, z));
    }

    public static Region regionForDistance(double distance) {
        int frontier = CozyZonesConfig.FRONTIER_RADIUS.get();
        int wildlands = Math.max(frontier + 1, CozyZonesConfig.WILDLANDS_RADIUS.get());
        int dread = Math.max(wildlands + 1, CozyZonesConfig.DREAD_RADIUS.get());
        if (distance < frontier) return Region.HEARTHLANDS;
        if (distance < wildlands) return Region.FRONTIER;
        if (distance < dread) return Region.WILDLANDS;
        return Region.DREAD_REACHES;
    }

    public static RegionalInfluenceBand influenceBandAt(ServerLevel level, double x, double z) {
        return influenceBandForDistance(distanceFromSpawn(level, x, z));
    }

    public static RegionalInfluenceBand influenceBandForDistance(double distance) {
        int core = CozyZonesConfig.effectiveInnerCoreRadius();
        int established = CozyZonesConfig.effectiveCardinalEstablishedRadius();
        if (distance < core) return RegionalInfluenceBand.SHARED_CORE;
        if (distance < established) return RegionalInfluenceBand.CARDINAL_TRANSITION;
        return RegionalInfluenceBand.ESTABLISHED;
    }

    /**
     * 0 in the shared core, smoothly rises through the 700-1200-ish transition,
     * and reaches 1 once the cardinal ecology should read clearly.
     */
    public static double regionalStrengthAt(ServerLevel level, double x, double z) {
        return regionalStrengthForDistance(distanceFromSpawn(level, x, z));
    }

    public static double regionalStrengthForDistance(double distance) {
        int core = CozyZonesConfig.effectiveInnerCoreRadius();
        int established = CozyZonesConfig.effectiveCardinalEstablishedRadius();
        return smoothstep(core, established, distance);
    }

    /**
     * Authoritative cardinal macro-region. Boundaries are 90-degree cardinal sectors whose
     * diagonal borders are gently warped by deterministic, seed-dependent low-frequency waves.
     * This keeps strong continental-scale directionality without X=0/Z=0 seams or checkerboard noise.
     */
    public static MacroRegion macroRegionAt(ServerLevel level, double x, double z) {
        BlockPos spawn = level.getSharedSpawnPos();
        double dx = x - (spawn.getX() + 0.5D);
        double dz = z - (spawn.getZ() + 0.5D);
        return macroRegionForOffset(level.getSeed(), dx, dz);
    }

    public static MacroRegion macroRegionForOffset(long worldSeed, double dx, double dz) {
        double angle = warpedAngle(worldSeed, dx, dz);
        if (angle >= -QUARTER_PI && angle < QUARTER_PI) return MacroRegion.EAST;
        if (angle >= QUARTER_PI && angle < THREE_QUARTER_PI) return MacroRegion.SOUTH;
        if (angle >= -THREE_QUARTER_PI && angle < -QUARTER_PI) return MacroRegion.NORTH;
        return MacroRegion.WEST;
    }

    /**
     * 0 exactly on a warped macro-region border and 1 once comfortably inside a macro-region core.
     * Future biome remapping can use this to blend compatible/common biomes along organic borders.
     */
    public static double macroBoundaryStrengthAt(ServerLevel level, double x, double z) {
        BlockPos spawn = level.getSharedSpawnPos();
        double dx = x - (spawn.getX() + 0.5D);
        double dz = z - (spawn.getZ() + 0.5D);
        double angle = warpedAngle(level.getSeed(), dx, dz);
        double local = distanceFromSectorCenter(angle);
        double edgeDistance = Math.max(0.0D, QUARTER_PI - local);
        double blendRadians = Math.toRadians(CozyZonesConfig.MACRO_BORDER_BLEND_DEGREES.get());
        return smoothstep(0.0D, blendRadians, edgeDistance);
    }

    public static RegionalCell regionalCellAt(ServerLevel level, double x, double z) {
        double distance = distanceFromSpawn(level, x, z);
        return new RegionalCell(
                regionForDistance(distance),
                macroRegionAt(level, x, z),
                influenceBandForDistance(distance),
                distance,
                regionalStrengthForDistance(distance),
                macroBoundaryStrengthAt(level, x, z)
        );
    }

    public static boolean structureAllowed(ServerLevel level, ResourceLocation structureId, double x, double z) {
        if (ZoneRuleRegistry.structureExplicitlySuppressed(structureId)) return false;
        RegionalCell cell = regionalCellAt(level, x, z);
        return ZoneRuleRegistry.structureRule(structureId)
                .map(rule -> rule.allows(cell))
                .orElse(true);
    }

    public static boolean naturalEntityAllowed(ServerLevel level, ResourceLocation entityId, double x, double z) {
        if (ZoneRuleRegistry.naturalEntityNamespaceSuppressed(entityId)) return false;
        RegionalCell cell = regionalCellAt(level, x, z);
        return ZoneRuleRegistry.naturalEntityRule(entityId)
                .map(rule -> !rule.enabled() || rule.allows(cell))
                .orElse(true);
    }

    public static boolean isDaytimeCandidate(ResourceLocation entityId) {
        return ZoneRuleRegistry.naturalEntityRule(entityId).map(ZoneRuleRegistry.NaturalEntityRule::daytimeCandidate).orElse(false);
    }

    private static double warpedAngle(long worldSeed, double dx, double dz) {
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

    private static double distanceFromSectorCenter(double angle) {
        MacroRegion region;
        double center;
        if (angle >= -QUARTER_PI && angle < QUARTER_PI) {
            region = MacroRegion.EAST;
            center = 0.0D;
        } else if (angle >= QUARTER_PI && angle < THREE_QUARTER_PI) {
            region = MacroRegion.SOUTH;
            center = Math.PI / 2.0D;
        } else if (angle >= -THREE_QUARTER_PI && angle < -QUARTER_PI) {
            region = MacroRegion.NORTH;
            center = -Math.PI / 2.0D;
        } else {
            region = MacroRegion.WEST;
            center = Math.PI;
        }
        // Keep the variable so this helper remains easy to instrument in debug builds.
        if (region == null) return 0.0D;
        return Math.abs(normalizeAngle(angle - center));
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
