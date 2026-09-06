package com.natureul.cozycrazyzones;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Plans the starter Hearthlands settlement ring on Minecraft's real village-placement lattice.
 *
 * The first implementation reserved one nearby village. The authored start now needs a readable
 * four-direction world: one real village anchor in Frostmarch, Greenveil, Sunscar and Harvestwood.
 * Every target remains outside the 1,000-block starter sanctuary and inside the 2,500-block
 * Hearthlands boundary. The compact passes are preferred; a wider final pass exists mainly for
 * awkward coastal seeds (especially northern ones) where suitable village land is scarce.
 */
public final class VillageRingPlanner {
    public static final double MIN_VILLAGE_START_DISTANCE = 1000.0D;
    public static final double MAX_HEARTH_VILLAGE_DISTANCE = 2250.0D;

    private static final double IDEAL_DISTANCE = 1250.0D;
    private static final ResourceLocation VILLAGE_SET = new ResourceLocation("minecraft", "villages");
    private static final ConcurrentMap<Key, VillagePlan> PLANS = new ConcurrentHashMap<>();

    private VillageRingPlanner() {}

    /**
     * Exact reserved village targets keyed by their intended macro-region. The map may be partial
     * only if a very unusual world exposes no valid village-lattice land in one direction.
     */
    public static Map<MacroRegion, ChunkPos> targetsFor(ServerLevel level,
                                                         ChunkGenerator generator,
                                                         ChunkGeneratorStructureState structureState,
                                                         RegistryAccess registryAccess) {
        return planFor(level, generator, structureState, registryAccess).targets();
    }

    /**
     * Backwards-compatible "first village" answer used by the personal Atlas route. It is the
     * nearest of the four regional anchors, not a fifth independent settlement.
     */
    @Nullable
    public static ChunkPos targetFor(ServerLevel level,
                                     ChunkGenerator generator,
                                     ChunkGeneratorStructureState structureState,
                                     RegistryAccess registryAccess) {
        return planFor(level, generator, structureState, registryAccess).nearest();
    }

    /** Returns the authored macro-region for a reserved target chunk, or null for ordinary chunks. */
    @Nullable
    public static MacroRegion targetRegionFor(ServerLevel level,
                                               ChunkGenerator generator,
                                               ChunkGeneratorStructureState structureState,
                                               RegistryAccess registryAccess,
                                               ChunkPos chunkPos) {
        for (var entry : targetsFor(level, generator, structureState, registryAccess).entrySet()) {
            if (entry.getValue().equals(chunkPos)) return entry.getKey();
        }
        return null;
    }

    public static void clear() {
        PLANS.clear();
    }

    private static VillagePlan planFor(ServerLevel level,
                                       ChunkGenerator generator,
                                       ChunkGeneratorStructureState structureState,
                                       RegistryAccess registryAccess) {
        BlockPos spawn = level.getSharedSpawnPos();
        Key key = new Key(structureState.getLevelSeed(), spawn.getX(), spawn.getZ());
        return PLANS.computeIfAbsent(key, ignored -> computePlan(
                spawn,
                generator,
                structureState,
                registryAccess
        ));
    }

    private static VillagePlan computePlan(BlockPos spawn,
                                           ChunkGenerator generator,
                                           ChunkGeneratorStructureState structureState,
                                           RegistryAccess registryAccess) {
        StructureSet villageSet = registryAccess.registryOrThrow(Registries.STRUCTURE_SET).get(VILLAGE_SET);
        if (villageSet == null || !(villageSet.placement() instanceof RandomSpreadStructurePlacement placement)) {
            CozyCrazyZones.LOGGER.warn("Could not resolve minecraft:villages RandomSpread placement; Hearth village guarantees disabled");
            return VillagePlan.EMPTY;
        }

        EnumMap<MacroRegion, ChunkPos> targets = new EnumMap<>(MacroRegion.class);

        // Prefer a tight, clearly established ring around the start. Later passes only fill regions
        // still missing a village; they never replace a good compact target with a farther one.
        fillMissing(spawn, generator, structureState, placement, targets, 1050.0D, 1400.0D, 0.55D);
        fillMissing(spawn, generator, structureState, placement, targets, 1000.0D, 1750.0D, 0.38D);
        fillMissing(spawn, generator, structureState, placement, targets, 1000.0D, MAX_HEARTH_VILLAGE_DISTANCE, 0.20D);

        if (targets.isEmpty()) {
            CozyCrazyZones.LOGGER.warn("No suitable Hearthlands village candidates found between 1000 and {} blocks", Math.round(MAX_HEARTH_VILLAGE_DISTANCE));
            return VillagePlan.EMPTY;
        }

        for (MacroRegion region : MacroRegion.values()) {
            ChunkPos target = targets.get(region);
            if (target == null) {
                CozyCrazyZones.LOGGER.warn("No suitable {} Hearthlands village candidate was found", region.displayName());
                continue;
            }
            CozyCrazyZones.LOGGER.info(
                    "Reserved {} Hearthlands village at chunk {},{} ({} blocks from spawn)",
                    region.displayName(),
                    target.x,
                    target.z,
                    Math.round(distanceFromSpawn(spawn, target))
            );
        }

        ChunkPos nearest = targets.values().stream()
                .min((a, b) -> Double.compare(distanceFromSpawn(spawn, a), distanceFromSpawn(spawn, b)))
                .orElse(null);
        return new VillagePlan(Map.copyOf(targets), nearest);
    }

    private static void fillMissing(BlockPos spawn,
                                    ChunkGenerator generator,
                                    ChunkGeneratorStructureState structureState,
                                    RandomSpreadStructurePlacement placement,
                                    EnumMap<MacroRegion, ChunkPos> targets,
                                    double minDistance,
                                    double maxDistance,
                                    double minimumBoundaryStrength) {
        if (targets.size() == MacroRegion.values().length) return;

        int spawnChunkX = SectionPos.blockToSectionCoord(spawn.getX());
        int spawnChunkZ = SectionPos.blockToSectionCoord(spawn.getZ());
        int spacing = placement.spacing();
        Set<Long> seen = new HashSet<>();
        EnumMap<MacroRegion, Candidate> best = new EnumMap<>(MacroRegion.class);

        // +/-8 placement cells comfortably covers the entire 2,250-block Hearthlands search even
        // with the vanilla village spacing, while remaining a tiny one-time world-start operation.
        for (int gx = -8; gx <= 8; gx++) {
            for (int gz = -8; gz <= 8; gz++) {
                int probeChunkX = spawnChunkX + spacing * gx;
                int probeChunkZ = spawnChunkZ + spacing * gz;
                ChunkPos candidate = placement.getPotentialStructureChunk(
                        structureState.getLevelSeed(),
                        probeChunkX,
                        probeChunkZ
                );
                if (!seen.add(candidate.toLong())) continue;

                double distance = distanceFromSpawn(spawn, candidate);
                if (distance < minDistance || distance > maxDistance) continue;

                int blockX = candidate.getMiddleBlockX();
                int blockZ = candidate.getMiddleBlockZ();
                RegionalCell cell = WorldGeographyContext.cellAt(blockX, blockZ);
                MacroRegion macro = cell.macroRegion();
                if (targets.containsKey(macro)) continue;
                if (cell.radialZone() != Region.HEARTHLANDS) continue;
                if (cell.influenceBand() != RegionalInfluenceBand.ESTABLISHED) continue;
                if (cell.macroBoundaryStrength() < minimumBoundaryStrength) continue;
                if (!looksLikeVillageLand(generator, structureState, blockX, blockZ)) continue;

                double jitter = (mix64(structureState.getLevelSeed() ^ candidate.toLong()) >>> 11) * 0x1.0p-53;
                double boundaryPenalty = (1.0D - cell.macroBoundaryStrength()) * 80.0D;
                double score = Math.abs(distance - IDEAL_DISTANCE) + boundaryPenalty + jitter * 8.0D;
                Candidate existing = best.get(macro);
                if (existing == null || score < existing.score()) {
                    best.put(macro, new Candidate(candidate, score));
                }
            }
        }

        best.forEach((region, candidate) -> targets.putIfAbsent(region, candidate.chunk()));
    }

    private static boolean looksLikeVillageLand(ChunkGenerator generator,
                                                 ChunkGeneratorStructureState structureState,
                                                 int blockX,
                                                 int blockZ) {
        Holder<Biome> holder = generator.getBiomeSource().getNoiseBiome(
                QuartPos.fromBlock(blockX),
                QuartPos.fromBlock(80),
                QuartPos.fromBlock(blockZ),
                structureState.randomState().sampler()
        );
        ResourceLocation id = holder.unwrapKey().map(key -> key.location()).orElse(null);
        if (id == null) return false;
        if (BiomeRegionality.isOcean(id) || BiomeRegionality.isRiver(id) || BiomeRegionality.isWetland(id)) return false;
        return BiomeRegionality.profile(id)
                .map(profile -> profile.shape() != BiomeRegionality.Shape.SPECIAL)
                .orElse(true);
    }

    private static double distanceFromSpawn(BlockPos spawn, ChunkPos chunk) {
        double dx = chunk.getMiddleBlockX() - (spawn.getX() + 0.5D);
        double dz = chunk.getMiddleBlockZ() - (spawn.getZ() + 0.5D);
        return Math.hypot(dx, dz);
    }

    private static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    private record Candidate(ChunkPos chunk, double score) {}
    private record VillagePlan(Map<MacroRegion, ChunkPos> targets, @Nullable ChunkPos nearest) {
        private static final VillagePlan EMPTY = new VillagePlan(Map.of(), null);
    }
    private record Key(long seed, int spawnX, int spawnZ) {}
}
