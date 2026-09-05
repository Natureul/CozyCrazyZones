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

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Picks one deterministic, locator-compatible vanilla village candidate around the starter area.
 *
 * The result - including a failed search - is cached. Caching the empty result matters because this
 * method is queried from structure generation for many chunks; ConcurrentHashMap#computeIfAbsent
 * does not retain a null mapping, which previously caused the same village search and warning to run
 * over and over during start-region generation.
 */
public final class VillageRingPlanner {
    public static final double MIN_VILLAGE_START_DISTANCE = 1000.0D;
    private static final double IDEAL_DISTANCE = 1150.0D;
    private static final ResourceLocation VILLAGE_SET = new ResourceLocation("minecraft", "villages");
    private static final ConcurrentMap<Key, Optional<ChunkPos>> TARGETS = new ConcurrentHashMap<>();

    private VillageRingPlanner() {}

    public static ChunkPos targetFor(ServerLevel level,
                                     ChunkGenerator generator,
                                     ChunkGeneratorStructureState structureState,
                                     RegistryAccess registryAccess) {
        BlockPos spawn = level.getSharedSpawnPos();
        Key key = new Key(structureState.getLevelSeed(), spawn.getX(), spawn.getZ());
        return TARGETS.computeIfAbsent(key, ignored -> Optional.ofNullable(computeTarget(
                spawn,
                generator,
                structureState,
                registryAccess
        ))).orElse(null);
    }

    public static void clear() {
        TARGETS.clear();
    }

    private static ChunkPos computeTarget(BlockPos spawn,
                                          ChunkGenerator generator,
                                          ChunkGeneratorStructureState structureState,
                                          RegistryAccess registryAccess) {
        StructureSet villageSet = registryAccess.registryOrThrow(Registries.STRUCTURE_SET).get(VILLAGE_SET);
        if (villageSet == null || !(villageSet.placement() instanceof RandomSpreadStructurePlacement placement)) {
            CozyCrazyZones.LOGGER.warn("Could not resolve minecraft:villages RandomSpread placement; first-village guarantee disabled");
            return null;
        }

        ChunkPos best = chooseCandidate(spawn, generator, structureState, placement, 1050.0D, 1250.0D, 0.50D);
        if (best == null) {
            best = chooseCandidate(spawn, generator, structureState, placement, 1000.0D, 1450.0D, 0.38D);
        }
        if (best == null) {
            // Last-resort ring is still firmly outside the starter sanctuary, but is intentionally
            // more permissive. A slightly farther first village is better than silently having none.
            best = chooseCandidate(spawn, generator, structureState, placement, 1000.0D, 1650.0D, 0.20D);
        }

        if (best == null) {
            CozyCrazyZones.LOGGER.warn("No suitable locator-compatible village candidate found between 1000 and 1650 blocks; result cached for this world");
            return null;
        }

        double distance = distanceFromSpawn(spawn, best);
        RegionalCell cell = WorldGeographyContext.cellAt(best.getMiddleBlockX(), best.getMiddleBlockZ());
        CozyCrazyZones.LOGGER.info(
                "Reserved first-village candidate at chunk {},{} ({} blocks from spawn, {} {})",
                best.x,
                best.z,
                Math.round(distance),
                cell.macroRegion().displayName(),
                cell.influenceBand()
        );
        return best;
    }

    private static ChunkPos chooseCandidate(BlockPos spawn,
                                            ChunkGenerator generator,
                                            ChunkGeneratorStructureState structureState,
                                            RandomSpreadStructurePlacement placement,
                                            double minDistance,
                                            double maxDistance,
                                            double minimumBoundaryStrength) {
        int spawnChunkX = SectionPos.blockToSectionCoord(spawn.getX());
        int spawnChunkZ = SectionPos.blockToSectionCoord(spawn.getZ());
        int spacing = placement.spacing();
        Set<Long> seen = new HashSet<>();

        ChunkPos best = null;
        double bestScore = Double.MAX_VALUE;

        // +/-6 placement cells remains a tiny one-time computation and comfortably covers the
        // wider fallback annulus even with modded village spacing.
        for (int gx = -6; gx <= 6; gx++) {
            for (int gz = -6; gz <= 6; gz++) {
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
                if (cell.macroBoundaryStrength() < minimumBoundaryStrength) continue;
                if (!looksLikeVillageLand(generator, structureState, blockX, blockZ)) continue;

                double jitter = (mix64(structureState.getLevelSeed() ^ candidate.toLong()) >>> 11) * 0x1.0p-53;
                double score = Math.abs(distance - IDEAL_DISTANCE) + jitter * 8.0D;
                if (score < bestScore) {
                    bestScore = score;
                    best = candidate;
                }
            }
        }
        return best;
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

    private record Key(long seed, int spawnX, int spawnZ) {}
}
