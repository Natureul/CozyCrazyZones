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
 * continent, not to fill an ocean basin after the fact. The selector checks each cardinal sector
 * separately and now gives the northern sector an extra land-availability preference: Frostmarch
 * should contain cold coasts and water, but its early-country experience should not repeatedly be
 * a giant sea simply because the other three directions happened to be excellent land.
 *
 * IMPORTANT: this runs synchronously inside Minecraft's initial-spawn path. getBaseHeight is much
 * more expensive with a large modded noise router than in vanilla, so the selector intentionally
 * reuses its existing sparse cardinal samples rather than adding more probes, and retains a hard
 * wall-clock budget.
 */
public final class StarterLandSelector {
    private static final int[] SEARCH_RADII = {2048, 4096, 6144};
    private static final int[] SAMPLE_RING_RADII = {650, 1300, 2000, 2700};

    private static final int PAD_SEARCH_RADIUS = 288;
    private static final int PAD_SEARCH_STEP = 72;
    private static final int PAD_FOOTPRINT_RADIUS = 22;
    private static final int MAX_PAD_RELIEF = 8;
    private static final int HARD_MAX_PAD_Y = 145;
    private static final int PREFERRED_MAX_PAD_Y = 112;
    private static final int PAD_CANDIDATES_TO_TRY = 4;

    // World creation must remain responsive even when another terrain mod makes getBaseHeight slow.
    private static final long TIME_BUDGET_NANOS = 8_000_000_000L;

    private StarterLandSelector() {}

    public static BlockPos choose(ServerLevel level, BlockPos vanillaSpawn) {
        long started = System.nanoTime();
        long deadline = started + TIME_BUDGET_NANOS;

        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState randomState = level.getChunkSource().randomState();
        int seaLevel = generator.getSeaLevel();

        Candidate vanilla = score(level, generator, randomState, seaLevel, vanillaSpawn.getX(), vanillaSpawn.getZ(), deadline);
        List<Candidate> candidates = new ArrayList<>();
        candidates.add(vanilla);

        outer:
        for (int radius : SEARCH_RADII) {
            int diagonal = (int) Math.round(radius / Math.sqrt(2.0D));
            int[][] offsets = {
                    { radius, 0 }, { -radius, 0 }, { 0, radius }, { 0, -radius },
                    { diagonal, diagonal }, { diagonal, -diagonal },
                    { -diagonal, diagonal }, { -diagonal, -diagonal }
            };

            for (int[] offset : offsets) {
                if (expired(deadline)) break outer;
                Candidate candidate = score(
                        level,
                        generator,
                        randomState,
                        seaLevel,
                        vanillaSpawn.getX() + offset[0],
                        vanillaSpawn.getZ() + offset[1],
                        deadline
                );
                if (candidate != null) candidates.add(candidate);
            }
        }

        candidates.removeIf(candidate -> candidate == null || candidate.score() <= -9000.0D);
        candidates.sort(Comparator.comparingDouble(Candidate::score).reversed());

        Candidate chosen = null;
        BuildSite site = null;
        BuildSite vanillaSite = null;
        int tried = 0;

        for (Candidate candidate : candidates) {
            if (expired(deadline) || tried++ >= PAD_CANDIDATES_TO_TRY) break;

            BuildSite candidateSite = findBuildSite(
                    level,
                    generator,
                    randomState,
                    seaLevel,
                    candidate.blockX(),
                    candidate.blockZ(),
                    deadline
            );
            if (candidateSite == null) continue;
            if (candidate == vanilla) vanillaSite = candidateSite;

            double combined = candidate.score() + candidateSite.quality();
            if (chosen == null || combined > chosen.score() + site.quality()) {
                chosen = candidate;
                site = candidateSite;
            }
        }

        // If the budget was consumed before vanilla happened to be among the top candidates, do one
        // cheap local pass only when time remains. Otherwise simply keep vanilla rather than stalling.
        if (vanillaSite == null && !expired(deadline)) {
            vanillaSite = findBuildSite(
                    level,
                    generator,
                    randomState,
                    seaLevel,
                    vanilla.blockX(),
                    vanilla.blockZ(),
                    deadline
            );
        }

        if (chosen == null || site == null) {
            long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
            CozyCrazyZones.LOGGER.warn(
                    "Starter land selector kept vanilla spawn {} after {} ms (time budget reached: {})",
                    vanillaSpawn,
                    elapsedMs,
                    expired(deadline)
            );
            return vanillaSpawn;
        }

        // Do not relocate for a microscopic broad-geography improvement unless the vanilla spot is
        // itself a poor house pad. This keeps worlds feeling naturally seeded rather than overfit.
        if (chosen != vanilla && vanillaSite != null) {
            double chosenCombined = chosen.score() + site.quality();
            double vanillaCombined = vanilla.score() + vanillaSite.quality();
            if (chosenCombined < vanillaCombined + 4.0D) {
                chosen = vanilla;
                site = vanillaSite;
            }
        }

        BlockPos safe = new BlockPos(site.blockX(), site.surfaceY(), site.blockZ());
        long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
        CozyCrazyZones.LOGGER.info(
                "Starter land selector chose {} in {} ms (land {}%, inner {}%, weakest cardinal {}%, relief {} blocks; house-pad relief {} blocks at Y={}; vanilla land {}% / inner {}% / weakest cardinal {}%)",
                safe,
                elapsedMs,
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

    @Nullable
    private static Candidate score(ServerLevel level,
                                   ChunkGenerator generator,
                                   RandomState randomState,
                                   int seaLevel,
                                   int centerX,
                                   int centerZ,
                                   long deadline) {
        if (expired(deadline)) return null;

        int centerSurface = surface(generator, randomState, level, centerX, centerZ);
        if (centerSurface <= seaLevel + 1) {
            return new Candidate(centerX, centerZ, -10000.0D, 0.0D, 0.0D, 0.0D, 0.0D);
        }

        double totalWeight = 4.0D;
        double landWeight = 4.0D;
        double innerWeight = 4.0D;
        double innerLandWeight = 4.0D;
        double heightSum = centerSurface;
        double heightSqSum = (double) centerSurface * centerSurface;
        int landSamples = 1;

        double[] sectorTotal = new double[4];
        double[] sectorLand = new double[4];

        for (int radius : SAMPLE_RING_RADII) {
            int diagonal = (int) Math.round(radius / Math.sqrt(2.0D));
            int[][] offsets = {
                    { radius, 0 }, { -radius, 0 }, { 0, radius }, { 0, -radius },
                    { diagonal, diagonal }, { diagonal, -diagonal },
                    { -diagonal, diagonal }, { -diagonal, -diagonal }
            };

            double weight = radius <= 700 ? 3.0D : radius <= 1500 ? 1.75D : 1.0D;
            for (int[] offset : offsets) {
                if (expired(deadline)) return null;
                int dx = offset[0];
                int dz = offset[1];
                int y = surface(generator, randomState, level, centerX + dx, centerZ + dz);
                boolean land = y > seaLevel + 1;

                totalWeight += weight;
                if (land) {
                    landWeight += weight;
                    heightSum += y;
                    heightSqSum += (double) y * y;
                    landSamples++;
                }

                if (radius <= 950) {
                    innerWeight += weight;
                    if (land) innerLandWeight += weight;
                }

                if (radius >= 1000) {
                    int sector = sector(dx, dz);
                    sectorTotal[sector] += weight;
                    if (land) sectorLand[sector] += weight;
                }
            }
        }

        double landRatio = landWeight / totalWeight;
        double innerLandRatio = innerLandWeight / innerWeight;
        double weakestCardinal = 1.0D;
        double northCardinal = 1.0D;
        for (int i = 0; i < sectorTotal.length; i++) {
            if (sectorTotal[i] <= 0.0D) continue;
            double ratio = sectorLand[i] / sectorTotal[i];
            weakestCardinal = Math.min(weakestCardinal, ratio);
            if (i == 0) northCardinal = ratio;
        }

        double relief = 0.0D;
        if (landSamples > 1) {
            double mean = heightSum / landSamples;
            relief = Math.sqrt(Math.max(0.0D, heightSqSum / landSamples - mean * mean));
        }

        double usefulLand = Math.min(0.90D, landRatio);
        double usefulInner = Math.min(0.94D, innerLandRatio);
        double usefulCardinal = Math.min(0.72D, weakestCardinal);
        double usefulNorth = Math.min(0.82D, northCardinal);
        double score = usefulLand * 95.0D
                + usefulInner * 60.0D
                + usefulCardinal * 58.0D
                + usefulNorth * 44.0D;

        if (landRatio < 0.68D) score -= (0.68D - landRatio) * 420.0D;
        if (innerLandRatio < 0.80D) score -= (0.80D - innerLandRatio) * 520.0D;
        if (weakestCardinal < 0.46D) score -= (0.46D - weakestCardinal) * 480.0D;

        // Frostmarch gets a stronger floor than the generic "weakest cardinal" safeguard. This is
        // a preference rather than a hard rejection: water-rich seeds still work, but a continent
        // with a substantially landier north will decisively beat an otherwise similar candidate.
        if (northCardinal < 0.60D) score -= (0.60D - northCardinal) * 620.0D;

        score += Math.min(8.0D, relief * 0.35D);
        if (centerSurface > 125) score -= (centerSurface - 125) * 0.7D;

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
                                           int centerZ,
                                           long deadline) {
        BuildSite best = null;

        for (int dz = -PAD_SEARCH_RADIUS; dz <= PAD_SEARCH_RADIUS; dz += PAD_SEARCH_STEP) {
            for (int dx = -PAD_SEARCH_RADIUS; dx <= PAD_SEARCH_RADIUS; dx += PAD_SEARCH_STEP) {
                if (expired(deadline)) return best;
                if ((long) dx * dx + (long) dz * dz > (long) PAD_SEARCH_RADIUS * PAD_SEARCH_RADIUS) continue;

                BuildSite candidate = scoreBuildSite(
                        level,
                        generator,
                        randomState,
                        seaLevel,
                        centerX + dx,
                        centerZ + dz,
                        Math.hypot(dx, dz),
                        deadline
                );
                if (candidate != null && (best == null || candidate.quality() > best.quality())) best = candidate;
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
                                            double offsetDistance,
                                            long deadline) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        double sum = 0.0D;
        int samples = 0;
        int centerY = Integer.MIN_VALUE;

        // Nine samples cover a roughly 44x44 pad: center, sides and corners. This is enough to reject
        // a steep Tectonic slope without turning spawn selection into thousands of full noise scans.
        int[] footprint = {-PAD_FOOTPRINT_RADIUS, 0, PAD_FOOTPRINT_RADIUS};
        for (int dz : footprint) {
            for (int dx : footprint) {
                if (expired(deadline)) return null;
                int y = surface(generator, randomState, level, blockX + dx, blockZ + dz);
                if (y <= seaLevel + 1) return null;

                if (dx == 0 && dz == 0) centerY = y;
                min = Math.min(min, y);
                max = Math.max(max, y);
                sum += y;
                samples++;
            }
        }

        if (samples == 0 || centerY == Integer.MIN_VALUE) return null;
        int relief = max - min;
        if (relief > MAX_PAD_RELIEF || centerY > HARD_MAX_PAD_Y) return null;

        double mean = sum / samples;
        double quality = 45.0D - relief * 5.5D;
        quality -= offsetDistance / 95.0D;
        quality -= Math.abs(centerY - mean) * 1.2D;
        if (centerY > PREFERRED_MAX_PAD_Y) quality -= (centerY - PREFERRED_MAX_PAD_Y) * 1.8D;
        if (centerY < seaLevel + 4) quality -= (seaLevel + 4 - centerY) * 2.0D;

        return new BuildSite(blockX, blockZ, centerY, relief, quality);
    }

    private static int surface(ChunkGenerator generator,
                               RandomState randomState,
                               ServerLevel level,
                               int x,
                               int z) {
        return generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, randomState);
    }

    private static boolean expired(long deadline) {
        return System.nanoTime() >= deadline;
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
