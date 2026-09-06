package com.natureul.cozycrazyzones;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Picks a better shared-spawn center for a brand-new world without modifying terrain.
 *
 * Tectonic already makes excellent large-scale landforms. The right way to keep the Hearthlands
 * from being swallowed by a huge ocean is therefore to start on the interior/shoulder of a natural
 * continent, not to fill an ocean basin after the fact. The selector also checks each cardinal
 * sector separately: a candidate cannot score well merely because three directions are solid land
 * while the fourth is a giant sea.
 *
 * Finally, the starter structure needs a build site, not merely a legal player-spawn column. The
 * old PlayerRespawnLogic fallback could choose a dramatic Tectonic peak (one test world landed at
 * Y=240), which left the starter house buried in a slope. We now choose a naturally flatter,
 * moderate-elevation pad from the generator's own pre-feature height function.
 */
public final class StarterLandSelector {
    private static final int[] SEARCH_RADII = {2048, 4096, 6144};

    private static final int SAMPLE_RADIUS = 2900;
    private static final int SAMPLE_STEP = 580;
    private static final int INNER_RADIUS = 950;

    private static final int PAD_SEARCH_RADIUS = 320;
    private static final int PAD_SEARCH_STEP = 32;
    private static final int PAD_FOOTPRINT_RADIUS = 24;
    private static final int PAD_FOOTPRINT_STEP = 12;
    private static final int MAX_PAD_RELIEF = 8;
    private static final int HARD_MAX_PAD_Y = 145;
    private static final int PREFERRED_MAX_PAD_Y = 112;
    private static final int PAD_CANDIDATES_TO_TRY = 8;

    private StarterLandSelector() {}

    public static BlockPos choose(ServerLevel level, BlockPos vanillaSpawn) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState randomState = level.getChunkSource().randomState();
        int seaLevel = generator.getSeaLevel();

        Candidate vanilla = score(level, generator, randomState, seaLevel, vanillaSpawn.getX(), vanillaSpawn.getZ());
        List<Candidate> candidates = new ArrayList<>();
        candidates.add(vanilla);

        for (int radius : SEARCH_RADII) {
            int diagonal = (int) Math.round(radius / Math.sqrt(2.0D));
            int[][] offsets = {
                    { radius, 0 }, { -radius, 0 }, { 0, radius }, { 0, -radius },
                    { diagonal, diagonal }, { diagonal, -diagonal },
                    { -diagonal, diagonal }, { -diagonal, -diagonal }
            };

            for (int[] offset : offsets) {
                candidates.add(score(
                        level,
                        generator,
                        randomState,
                        seaLevel,
                        vanillaSpawn.getX() + offset[0],
                        vanillaSpawn.getZ() + offset[1]
                ));
            }
        }

        candidates.sort(Comparator.comparingDouble(Candidate::score).reversed());

        Candidate chosen = null;
        BuildSite site = null;
        int tried = 0;
        for (Candidate candidate : candidates) {
            if (candidate.score() <= -9000.0D) continue;
            if (tried++ >= PAD_CANDIDATES_TO_TRY) break;

            BuildSite candidateSite = findBuildSite(
                    level,
                    generator,
                    randomState,
                    seaLevel,
                    candidate.blockX(),
                    candidate.blockZ()
            );
            if (candidateSite == null) continue;

            double combined = candidate.score() + candidateSite.quality();
            if (chosen == null || combined > chosen.score() + site.quality()) {
                chosen = candidate;
                site = candidateSite;
            }
        }

        if (chosen == null || site == null) {
            CozyCrazyZones.LOGGER.warn(
                    "Land-rich starter selector could not find a naturally buildable pad; keeping vanilla spawn {}",
                    vanillaSpawn
            );
            return vanillaSpawn;
        }

        // Do not relocate for a microscopic broad-geography improvement unless the vanilla spot is
        // itself a poor house pad. This keeps worlds feeling naturally seeded rather than overfit.
        BuildSite vanillaSite = findBuildSite(
                level,
                generator,
                randomState,
                seaLevel,
                vanilla.blockX(),
                vanilla.blockZ()
        );
        if (chosen != vanilla && vanillaSite != null) {
            double chosenCombined = chosen.score() + site.quality();
            double vanillaCombined = vanilla.score() + vanillaSite.quality();
            if (chosenCombined < vanillaCombined + 4.0D) {
                chosen = vanilla;
                site = vanillaSite;
            }
        }

        BlockPos safe = new BlockPos(site.blockX(), site.surfaceY(), site.blockZ());
        CozyCrazyZones.LOGGER.info(
                "Starter land selector chose {} (land {}%, inner {}%, weakest cardinal {}%, relief {} blocks; house-pad relief {} blocks at Y={}; vanilla land {}% / inner {}% / weakest cardinal {}%)",
                safe,
                Math.round(chosen.landRatio() * 100.0D),
                Math.round(chosen.innerLandRatio() * 100.0D),
                Math.round(chosen.weakestCardinalLandRatio() * 100.0D),
                Math.round(chosen.relief()),
                site.relief(),
                site.surfaceY(),
                Math.round(vanilla.landRatio() * 100.0D),
                Math.round(vanilla.innerLandRatio() * 100.0D),
                Math.round(vanilla.weakestCardinalLandRatio() * 100.0D)
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

        if (centerFloor <= seaLevel + 1) {
            return new Candidate(centerX, centerZ, -10000.0D, 0.0D, 0.0D, 0.0D, 0.0D);
        }

        double totalWeight = 0.0D;
        double landWeight = 0.0D;
        double innerWeight = 0.0D;
        double innerLandWeight = 0.0D;
        double heightSum = 0.0D;
        double heightSqSum = 0.0D;
        int landSamples = 0;

        double[] sectorTotal = new double[4];
        double[] sectorLand = new double[4];

        for (int dz = -SAMPLE_RADIUS; dz <= SAMPLE_RADIUS; dz += SAMPLE_STEP) {
            for (int dx = -SAMPLE_RADIUS; dx <= SAMPLE_RADIUS; dx += SAMPLE_STEP) {
                double distance = Math.sqrt((double) dx * dx + (double) dz * dz);
                if (distance > SAMPLE_RADIUS) continue;

                double weight = distance <= 900.0D ? 3.0D : distance <= 1700.0D ? 1.75D : 1.0D;
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

                if (distance <= INNER_RADIUS) {
                    innerWeight += weight;
                    if (land) innerLandWeight += weight;
                }

                // Ignore the ambiguous center and diagonal seam; every real direction still gets
                // many samples across the transition/Hearthlands/near-Frontier shoulder.
                if (distance >= 700.0D) {
                    int sector = sector(dx, dz);
                    sectorTotal[sector] += weight;
                    if (land) sectorLand[sector] += weight;
                }
            }
        }

        double landRatio = totalWeight == 0.0D ? 0.0D : landWeight / totalWeight;
        double innerLandRatio = innerWeight == 0.0D ? 0.0D : innerLandWeight / innerWeight;
        double weakestCardinal = 1.0D;
        for (int i = 0; i < sectorTotal.length; i++) {
            if (sectorTotal[i] <= 0.0D) continue;
            weakestCardinal = Math.min(weakestCardinal, sectorLand[i] / sectorTotal[i]);
        }

        double relief = 0.0D;
        if (landSamples > 1) {
            double mean = heightSum / landSamples;
            relief = Math.sqrt(Math.max(0.0D, heightSqSum / landSamples - mean * mean));
        }

        // Target a land-dominant starter country, not a waterless one. A coast, lakes and rivers
        // are welcome. The cardinal term specifically prevents one entire direction from being the
        // sacrificial ocean just because the other three directions scored well.
        double usefulLand = Math.min(0.90D, landRatio);
        double usefulInner = Math.min(0.94D, innerLandRatio);
        double usefulCardinal = Math.min(0.72D, weakestCardinal);
        double score = usefulLand * 95.0D + usefulInner * 60.0D + usefulCardinal * 58.0D;

        if (landRatio < 0.68D) score -= (0.68D - landRatio) * 420.0D;
        if (innerLandRatio < 0.80D) score -= (0.80D - innerLandRatio) * 520.0D;
        if (weakestCardinal < 0.46D) score -= (0.46D - weakestCardinal) * 480.0D;

        score += Math.min(8.0D, relief * 0.35D);
        if (centerFloor > 125) score -= (centerFloor - 125) * 0.7D;

        return new Candidate(centerX, centerZ, score, landRatio, innerLandRatio, weakestCardinal, relief);
    }

    private static int sector(int dx, int dz) {
        if (Math.abs(dx) > Math.abs(dz)) return dx >= 0 ? 1 : 3; // east / west
        return dz >= 0 ? 2 : 0; // south / north
    }

    @Nullable
    private static BuildSite findBuildSite(ServerLevel level,
                                           ChunkGenerator generator,
                                           RandomState randomState,
                                           int seaLevel,
                                           int centerX,
                                           int centerZ) {
        BuildSite best = null;

        for (int dz = -PAD_SEARCH_RADIUS; dz <= PAD_SEARCH_RADIUS; dz += PAD_SEARCH_STEP) {
            for (int dx = -PAD_SEARCH_RADIUS; dx <= PAD_SEARCH_RADIUS; dx += PAD_SEARCH_STEP) {
                if ((long) dx * dx + (long) dz * dz > (long) PAD_SEARCH_RADIUS * PAD_SEARCH_RADIUS) continue;
                BuildSite site = scoreBuildSite(
                        level,
                        generator,
                        randomState,
                        seaLevel,
                        centerX + dx,
                        centerZ + dz,
                        Math.hypot(dx, dz)
                );
                if (site == null) continue;
                if (best == null || site.quality() > best.quality()) best = site;
            }
        }
        return best;
    }

    @Nullable
    private static BuildSite scoreBuildSite(ServerLevel level,
                                            ChunkGenerator generator,
                                            RandomState randomState,
                                            int seaLevel,
                                            int blockX,
                                            int blockZ,
                                            double offsetDistance) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        double sum = 0.0D;
        int samples = 0;

        for (int dz = -PAD_FOOTPRINT_RADIUS; dz <= PAD_FOOTPRINT_RADIUS; dz += PAD_FOOTPRINT_STEP) {
            for (int dx = -PAD_FOOTPRINT_RADIUS; dx <= PAD_FOOTPRINT_RADIUS; dx += PAD_FOOTPRINT_STEP) {
                int x = blockX + dx;
                int z = blockZ + dz;
                int floor = generator.getBaseHeight(
                        x,
                        z,
                        Heightmap.Types.OCEAN_FLOOR_WG,
                        level,
                        randomState
                );
                if (floor <= seaLevel + 1) return null;

                int surface = generator.getBaseHeight(
                        x,
                        z,
                        Heightmap.Types.WORLD_SURFACE_WG,
                        level,
                        randomState
                );
                min = Math.min(min, surface);
                max = Math.max(max, surface);
                sum += surface;
                samples++;
            }
        }

        if (samples == 0) return null;
        int relief = max - min;
        if (relief > MAX_PAD_RELIEF) return null;

        int centerY = generator.getBaseHeight(
                blockX,
                blockZ,
                Heightmap.Types.WORLD_SURFACE_WG,
                level,
                randomState
        );
        if (centerY > HARD_MAX_PAD_Y) return null;

        double mean = sum / samples;
        double quality = 45.0D - relief * 5.5D;
        quality -= offsetDistance / 95.0D;
        quality -= Math.abs(centerY - mean) * 1.2D;
        if (centerY > PREFERRED_MAX_PAD_Y) quality -= (centerY - PREFERRED_MAX_PAD_Y) * 1.8D;
        if (centerY < seaLevel + 4) quality -= (seaLevel + 4 - centerY) * 2.0D;

        return new BuildSite(blockX, blockZ, centerY, relief, quality);
    }

    private record Candidate(int blockX,
                             int blockZ,
                             double score,
                             double landRatio,
                             double innerLandRatio,
                             double weakestCardinalLandRatio,
                             double relief) {}

    private record BuildSite(int blockX, int blockZ, int surfaceY, int relief, double quality) {}
}
