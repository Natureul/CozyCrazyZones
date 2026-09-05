package com.natureul.cozycrazyzones;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

public final class CozyZonesApi {
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

    public static boolean structureAllowed(ServerLevel level, ResourceLocation structureId, double x, double z) {
        return ZoneRuleRegistry.minimumStructureRegion(structureId)
                .map(minimum -> regionAt(level, x, z).atLeast(minimum))
                .orElse(true);
    }

    public static boolean naturalEntityAllowed(ServerLevel level, ResourceLocation entityId, double x, double z) {
        return ZoneRuleRegistry.naturalEntityRule(entityId)
                .map(rule -> !rule.enabled() || regionAt(level, x, z).atLeast(rule.minimum()))
                .orElse(true);
    }

    public static boolean isDaytimeCandidate(ResourceLocation entityId) {
        return ZoneRuleRegistry.naturalEntityRule(entityId).map(ZoneRuleRegistry.NaturalEntityRule::daytimeCandidate).orElse(false);
    }
}
