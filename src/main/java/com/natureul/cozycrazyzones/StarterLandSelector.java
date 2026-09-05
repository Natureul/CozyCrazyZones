package com.natureul.cozycrazyzones;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.PlayerRespawnLogic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * Picks a better shared-spawn center for a brand-new world without modifying terrain.
 *
 * Tectonic already makes excellent large-scale landforms. The right way to keep the Hearthlands
 * from being swallowed by a huge ocean is therefore to start on the interior/shoulder of a natural
 * continent, not to fill an ocean basin after the fact. This selector samples the generator's own
 * height function before the 441-chunk start region is prepared and moves the shared spawn only when
 * another nearby candidate gives the starter region substantially more natural land.
 *
 * Water is intentionally NOT eliminated. The scoring target simply rejects candidates where water
 * dominates; rivers, lakes, bays and a reasonable coastline remain welcome.
 */
public final class StarterLandSelector {
    private static final int[] SEARCH_RADII = {2048, 4096, 6144};
    private static final int SAMPLE_RADIUS = 2250;
    private static final int SAMPLE_STEP = 500;

    private StarterLandSelector() {}

    public static BlockPos choose(ServerLevel level, BlockPos vanillaSpawn) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState randomState = level.getChunkSource().randomState();
        int seaLevel = generator.getSeaLevel();

        Candidate vanilla = score(level, generator, randomState, seaLevel, vanillaSpawn.getX(), vanillaSpawn.getZ());
        Candidate best = vanilla;

        for (int radius : SEARCH_RADII) {
            int diagonal = (int) Math.round(radius / Math.sqrt(2.0D));
            int[][] offsets = {
                    { radius, 0 }, { -radius, 0 }, { 0, radius }, { 0, -radius },
                    { diagonal, diagonal }, { diagonal, -diagonal },
                    { -diagonal, diagonal }, { -diagonal, -diagonal }
            };

            for (int[] offset : offsets) {
                Candidate candidate = score(
                        level,
                        generator,
                        randomState,
                        seaLevel,
                        vanillaSpawn.getX() + offset[0],
                        vanillaSpawn.getZ() + offset[1]
                );
                if (candidate.score() > best.score()) best = candidate;
            }
        }

        // Don't relocate for a microscopic improvement. Vanilla's spawn choice is perfectly fine
        // when it already sits on good country.
        if (best != vanilla && best.score() < vanilla.score() + 4.0D) best = vanilla;

        BlockPos safe = findSafeSpawn(level, best.blockX(), best.blockZ());
        if (safe == null) {
            CozyCrazyZones.LOGGER.warn(
                    "Land-rich starter selector preferred {},{} but could not resolve a safe spawn there; keeping vanilla spawn {}",
                    best.blockX(), best.blockZ(), vanillaSpawn
            );
            return vanillaSpawn;
        }

        CozyCrazyZones.LOGGER.info(
                "Starter land selector chose {} (sampled land {}%, inner land {}%, relief {} blocks; vanilla was {}% / {}%)",
                safe,
                Math.round(best.landRatio() * 100.0D),
                Math.round(best.innerLandRatio() * 100.0D),
                Math.round(best.relief()),
                Math.round(vanilla.landRatio() * 100.0D),
                Math.round(vanilla.innerLandRatio() * 100.0D)
        );
        return safe;
    }

    private static Candidate score(ServerLevel level,
                                   ChunkGenerator generator,
                                   RandomState randomState,
                                   int seaLevel,
                                   int centerX,
                                   int centerZ) {
        int centerFloor = generator.getBaseHeight(
                centerX,
                centerZ,
                Heightmap.Types.OCEAN_FLOOR_WG,
                level,
                randomState
        );

        // A candidate whose actual center is underwater is never suitable for the starter house.
        if (centerFloor <= seaLevel + 1) {
            return new Candidate(centerX, centerZ, -10000.0D, 0.0D, 0.0D, 0.0D);
        }

        double totalWeight = 0.0D;
        double landWeight = 0.0D;
        double innerWeight = 0.0D;
        double innerLandWeight = 0.0D;
        double heightSum = 0.0D;
        double heightSqSum = 0.0D;
        int landSamples = 0;

        for (int dz = -SAMPLE_RADIUS; dz <= SAMPLE_RADIUS; dz += SAMPLE_STEP) {
            for (int dx = -SAMPLE_RADIUS; dx <= SAMPLE_RADIUS; dx += SAMPLE_STEP) {
                double distance = Math.sqrt((double) dx * dx + (double) dz * dz);
                if (distance > SAMPLE_RADIUS) continue;

                double weight = distance <= 850.0D ? 3.0D : distance <= 1550.0D ? 1.75D : 1.0D;
                int floor = generator.getBaseHeight(
                        centerX + dx,
                        centerZ + dz,
                        Heightmap.Types.OCEAN_FLOOR_WG,
                        level,
                        randomState
                );
                boolean land = floor > seaLevel + 1;

                totalWeight += weight;
                if (land) {
                    landWeight += weight;
                    heightSum += floor;
                    heightSqSum += (double) floor * floor;
                    landSamples++;
                }

                if (distance <= 900.0D) {
                    innerWeight += weight;
                    if (land) innerLandWeight += weight;
                }
            }
        }

        double landRatio = totalWeight == 0.0D ? 0.0D : landWeight / totalWeight;
        double innerLandRatio = innerWeight == 0.0D ? 0.0D : innerLandWeight / innerWeight;
        double relief = 0.0D;
        if (landSamples > 1) {
            double mean = heightSum / landSamples;
            relief = Math.sqrt(Math.max(0.0D, heightSqSum / landSamples - mean * mean));
        }

        // The desired feel is land-dominant, not waterless. Ratios below ~70% are heavily punished;
        // very high land ratios are fine, but receive no special bonus beyond the useful range.
        double usefulLand = Math.min(0.90D, landRatio);
        double usefulInner = Math.min(0.94D, innerLandRatio);
        double score = usefulLand * 100.0D + usefulInner * 65.0D;

        if (landRatio < 0.70D) score -= (0.70D - landRatio) * 420.0D;
        if (innerLandRatio < 0.80D) score -= (0.80D - innerLandRatio) * 520.0D;

        // A little natural relief is desirable, but don't steer the house onto an extreme peak.
        score += Math.min(9.0D, relief * 0.45D);
        if (centerFloor > 112) score -= (centerFloor - 112) * 0.8D;

        return new Candidate(centerX, centerZ, score, landRatio, innerLandRatio, relief);
    }

    private static BlockPos findSafeSpawn(ServerLevel level, int blockX, int blockZ) {
        ChunkPos center = new ChunkPos(blockX >> 4, blockZ >> 4);

        BlockPos direct = PlayerRespawnLogic.getSpawnPosInChunk(level, center);
        if (direct != null) return direct;

        for (int radius = 1; radius <= 2; radius++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) continue;
                    BlockPos candidate = PlayerRespawnLogic.getSpawnPosInChunk(
                            level,
                            new ChunkPos(center.x + dx, center.z + dz)
                    );
                    if (candidate != null) return candidate;
                }
            }
        }
        return null;
    }

    private record Candidate(int blockX,
                             int blockZ,
                             double score,
                             double landRatio,
                             double innerLandRatio,
                             double relief) {}
}
