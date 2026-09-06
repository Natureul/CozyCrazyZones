package com.natureul.cozycrazyzones;

import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Regionalizes the already-completed BIOMES-stage chunk palette.
 *
 * This is the authoritative biome-writing stage. TerraBlender/Tectonic decide the native terrain
 * and native biome palette first; CozyCrazyZones then applies the final regional identity contract
 * to that completed palette. Terrain density is intentionally left untouched.
 */
public final class RegionalBiomePostProcessor {
    private static final ResourceLocation ASPEN_GLADE = id("biomesoplenty:aspen_glade");
    private static final ResourceLocation SEASONAL_FOREST = id("biomesoplenty:seasonal_forest");
    private static final ResourceLocation MOOR = id("biomesoplenty:moor");

    private static final Map<BiomeSource, Map<ResourceLocation, Holder<Biome>>> HOLDER_LOOKUPS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private static final AtomicBoolean FIRST_REMAP_LOGGED = new AtomicBoolean();
    private static final AtomicBoolean FIRST_MISSING_TARGET_LOGGED = new AtomicBoolean();
    private static final AtomicBoolean FIRST_FALLBACK_TARGET_LOGGED = new AtomicBoolean();

    private RegionalBiomePostProcessor() {}

    public static void regionalize(ChunkAccess chunk,
                                   int seaLevel,
                                   BiomeSource biomeSource,
                                   Climate.Sampler sampler) {
        if (!WorldGeographyContext.prepared()) return;

        Map<ResourceLocation, Holder<Biome>> lookup = lookupFor(biomeSource);

        // ChunkAccess#fillBiomesFromNoise is safe before the status flag flips. Its section-level
        // implementation recreates a palette, resolves all 64 cells, and only then swaps it in.
        // rawNoiseBiome therefore still sees the native palette for the section currently being
        // resolved, without touching ProtoChunk#getNoiseBiome's BIOMES-status guard.
        chunk.fillBiomesFromNoise((quartX, quartY, quartZ, ignoredSampler) -> {
            Holder<Biome> original = rawNoiseBiome(chunk, quartX, quartY, quartZ);
            if (QuartPos.toBlock(quartY) < 48) return original;

            ResourceLocation originalId = key(original);
            if (originalId == null) return original;
            if (!BiomeRegionality.isManagedSurfaceBiome(originalId)
                    && !ASPEN_GLADE.equals(originalId)
                    && !MOOR.equals(originalId)) {
                return original;
            }

            int blockX = QuartPos.toBlock(quartX);
            int blockZ = QuartPos.toBlock(quartZ);
            RegionalCell cell = WorldGeographyContext.cellAt(blockX, blockZ);

            ResourceLocation targetId = remap(originalId, cell, blockX, blockZ);
            targetId = RegionalPaletteRefinement.refine(
                    targetId,
                    originalId,
                    cell,
                    WorldGeographyContext.worldSeed(),
                    blockX,
                    blockZ
            );

            // IMPORTANT: this stricter COMMON-biome pass must happen here, after the native chunk
            // palette exists. The earlier MultiNoise hook is useful for structure-time biome reads,
            // but this completed-palette stage is what the player actually sees in generated chunks.
            targetId = RegionalBiomeStrictness.refine(
                    targetId,
                    originalId,
                    cell,
                    WorldGeographyContext.worldSeed(),
                    blockX,
                    blockZ
            );

            // Aquamirae's Maze is biome/feature-driven. The final palette, not merely an earlier
            // biome-source answer, must therefore enforce the finite northern Dread expedition belt.
            targetId = reserveIceMazeForFinalBelt(originalId, targetId, cell);

            if (targetId.equals(originalId)) return original;

            Holder<Biome> replacement = lookup.get(targetId);
            if (replacement == null && !BiomeRegionality.isOcean(targetId) && !BiomeRegionality.isRiver(targetId)) {
                ResourceLocation requested = targetId;
                ResourceLocation fallbackId = firstAvailableRegionalFallback(cell, lookup);
                if (fallbackId != null) {
                    replacement = lookup.get(fallbackId);
                    targetId = fallbackId;
                    if (FIRST_FALLBACK_TARGET_LOGGED.compareAndSet(false, true)) {
                        CozyCrazyZones.LOGGER.warn(
                                "Final regional palette target {} was unavailable at {},{}; using {} instead of leaking original biome {}",
                                requested, blockX, blockZ, fallbackId, originalId
                        );
                    }
                }
            }

            if (replacement == null) {
                if (FIRST_MISSING_TARGET_LOGGED.compareAndSet(false, true)) {
                    CozyCrazyZones.LOGGER.warn(
                            "Final regional palette wanted {} -> {} at {},{} but neither the target nor a safe regional fallback is available in this biome source",
                            originalId, targetId, blockX, blockZ
                    );
                }
                return original;
            }

            if (FIRST_REMAP_LOGGED.compareAndSet(false, true)) {
                CozyCrazyZones.LOGGER.info(
                        "Regional chunk-biome postprocessor ACTIVE: {} -> {} at {},{} ({} / {} / {} blocks)",
                        originalId,
                        targetId,
                        blockX,
                        blockZ,
                        cell.radialZone().displayName(),
                        cell.influenceBand(),
                        Math.round(cell.distanceFromSpawn())
                );
            }
            return replacement;
        }, sampler);
    }

    /**
     * Directly reads the section biome palette and intentionally bypasses ProtoChunk#getNoiseBiome.
     */
    private static Holder<Biome> rawNoiseBiome(ChunkAccess chunk, int quartX, int quartY, int quartZ) {
        int minQuartY = QuartPos.fromBlock(chunk.getMinBuildHeight());
        int maxQuartY = minQuartY + QuartPos.fromBlock(chunk.getHeight()) - 1;
        int clampedQuartY = Math.max(minQuartY, Math.min(maxQuartY, quartY));
        int blockY = QuartPos.toBlock(clampedQuartY);
        int sectionIndex = chunk.getSectionIndex(blockY);
        return chunk.getSection(sectionIndex).getNoiseBiome(quartX & 3, clampedQuartY & 3, quartZ & 3);
    }

    /**
     * Retained for binary/source compatibility with older in-project callers. There is no longer a
     * terrain-conversion mask because CozyCrazyZones no longer raises native ocean basins.
     */
    public static boolean[] takeConvertedOceanMask(ChunkAccess chunk) {
        return null;
    }

    public static void clearTransientState() {
        synchronized (HOLDER_LOOKUPS) {
            HOLDER_LOOKUPS.clear();
        }
        FIRST_REMAP_LOGGED.set(false);
        FIRST_MISSING_TARGET_LOGGED.set(false);
        FIRST_FALLBACK_TARGET_LOGGED.set(false);
    }

    private static ResourceLocation remap(ResourceLocation original,
                                          RegionalCell cell,
                                          int blockX,
                                          int blockZ) {
        if (ASPEN_GLADE.equals(original)) return remapAspen(cell, blockX, blockZ);
        if (MOOR.equals(original)) return remapMoor(cell);

        // Water stays water. Native Tectonic ocean density is never silently converted into land.
        if (BiomeRegionality.isOcean(original) && cell.radialZone() == Region.HEARTHLANDS) {
            return hearthlandsOcean(cell.macroRegion(), original);
        }

        ResourceLocation target = BiomeRegionality.remap(
                original,
                cell,
                WorldGeographyContext.worldSeed(),
                blockX,
                blockZ
        );

        return enrichRegionalTexture(target, original, cell, blockX, blockZ);
    }

    /**
     * Aquamirae's Ice Maze uses frozen/deep-frozen ocean as its biome territory. That territory is
     * legal only in established northern Dread Reaches and only until finalDestinationMaxRadius.
     * Outside the legal belt, frozen-ocean leakage becomes the region's non-Maze ocean analogue.
     */
    private static ResourceLocation reserveIceMazeForFinalBelt(ResourceLocation original,
                                                                ResourceLocation target,
                                                                RegionalCell cell) {
        if (!BiomeRegionality.isOcean(target)) return target;

        boolean deep = target.getPath().startsWith("deep_") || original.getPath().startsWith("deep_");
        if (FinalDestinationPolicy.iceMazeTerritory(cell)) {
            return id(deep ? "minecraft:deep_frozen_ocean" : "minecraft:frozen_ocean");
        }

        if (FinalDestinationPolicy.isIceMazeOcean(target)
                || FinalDestinationPolicy.isIceMazeOcean(original)) {
            return FinalDestinationPolicy.nonMazeOcean(cell, deep);
        }
        return target;
    }

    /**
     * A single giant native savanna should not remain a single giant visual note for thousands of
     * blocks. Broad low-frequency patches add mild southern variety without checkerboarding or
     * inventing terrain. This only changes biome dressing; Tectonic's hills, valleys and coastlines
     * remain untouched.
     */
    private static ResourceLocation enrichRegionalTexture(ResourceLocation target,
                                                           ResourceLocation original,
                                                           RegionalCell cell,
                                                           int blockX,
                                                           int blockZ) {
        if (cell.macroRegion() != MacroRegion.SOUTH
                || cell.influenceBand() != RegionalInfluenceBand.ESTABLISHED
                || !cell.radialZone().atLeast(Region.HEARTHLANDS)) {
            return target;
        }

        BiomeRegionality.Profile profile = BiomeRegionality.profile(target).orElse(null);
        if (profile == null) return target;

        double broad = RegionalNoise.fractal(
                WorldGeographyContext.worldSeed() ^ 0x8CB92BA72F3D8DD7L,
                blockX,
                blockZ,
                900.0D
        );

        if (cell.radialZone() == Region.HEARTHLANDS) {
            if (profile.shape() == BiomeRegionality.Shape.OPEN
                    || profile.shape() == BiomeRegionality.Shape.ARID) {
                if (broad > 0.42D) return id("biomesoplenty:lush_savanna");
                if (broad < -0.52D && cell.distanceFromSpawn() > 1550.0D) return id("biomesoplenty:scrubland");
                if (target.equals(id("minecraft:savanna")) && Math.abs(broad) < 0.12D) {
                    return id("biomesoplenty:scrubland");
                }
            }
            return target;
        }

        if (cell.radialZone() == Region.FRONTIER) {
            if (profile.shape() == BiomeRegionality.Shape.OPEN
                    || profile.shape() == BiomeRegionality.Shape.ARID) {
                if (broad > 0.38D) return id("biomesoplenty:lush_savanna");
                if (broad < -0.32D) return id("biomesoplenty:dryland");
                if (Math.abs(broad) < 0.14D) return id("biomesoplenty:scrubland");
            }
            if (profile.shape() == BiomeRegionality.Shape.MOUNTAIN && broad < -0.36D) {
                return id("minecraft:badlands");
            }
        }

        return target;
    }

    /** Aspen is deliberately western/autumnal now, not Shared-Core/common. */
    private static ResourceLocation remapAspen(RegionalCell cell, int blockX, int blockZ) {
        if (cell.influenceBand() == RegionalInfluenceBand.SHARED_CORE || cell.macroBoundaryStrength() < 0.42D) {
            return id("minecraft:birch_forest");
        }
        if (cell.macroRegion() == MacroRegion.WEST) {
            return switch (cell.radialZone()) {
                case HEARTHLANDS, FRONTIER -> ASPEN_GLADE;
                case WILDLANDS -> id("biomesoplenty:redwood_forest");
                case DREAD_REACHES -> id("biomesoplenty:ominous_woods");
            };
        }
        return BiomeRegionality.remap(
                SEASONAL_FOREST,
                cell,
                WorldGeographyContext.worldSeed(),
                blockX,
                blockZ
        );
    }

    private static ResourceLocation remapMoor(RegionalCell cell) {
        if (cell.influenceBand() == RegionalInfluenceBand.SHARED_CORE || cell.macroBoundaryStrength() < 0.42D) {
            return id("minecraft:meadow");
        }
        return switch (cell.macroRegion()) {
            case EAST -> MOOR;
            case NORTH -> id(cell.radialZone().atLeast(Region.FRONTIER)
                    ? "biomesoplenty:muskeg" : "biomesoplenty:bog");
            case SOUTH -> id(cell.radialZone().atLeast(Region.FRONTIER)
                    ? "minecraft:savanna_plateau" : "minecraft:savanna");
            case WEST -> id(cell.radialZone().atLeast(Region.WILDLANDS)
                    ? "biomesoplenty:redwood_forest" : "biomesoplenty:seasonal_forest");
        };
    }

    private static ResourceLocation hearthlandsOcean(MacroRegion region, ResourceLocation original) {
        boolean deep = original.getPath().startsWith("deep_");
        return switch (region) {
            case NORTH -> id(deep ? "minecraft:deep_cold_ocean" : "minecraft:cold_ocean");
            case EAST, SOUTH -> id(deep ? "minecraft:deep_lukewarm_ocean" : "minecraft:lukewarm_ocean");
            case WEST -> id(deep ? "minecraft:deep_ocean" : "minecraft:ocean");
        };
    }

    private static ResourceLocation firstAvailableRegionalFallback(RegionalCell cell,
                                                                    Map<ResourceLocation, Holder<Biome>> lookup) {
        boolean deep = cell.radialZone().atLeast(Region.WILDLANDS);
        List<ResourceLocation> candidates = switch (cell.macroRegion()) {
            case NORTH -> deep
                    ? List.of(id("minecraft:snowy_taiga"), id("minecraft:snowy_plains"), id("minecraft:taiga"))
                    : List.of(id("minecraft:taiga"), id("minecraft:snowy_taiga"), id("minecraft:meadow"));
            case EAST -> deep
                    ? List.of(id("minecraft:jungle"), id("minecraft:bamboo_jungle"), id("minecraft:sparse_jungle"), id("minecraft:mangrove_swamp"))
                    : List.of(id("minecraft:sparse_jungle"), id("biomesoplenty:overgrown_greens"), id("minecraft:jungle"), id("minecraft:swamp"));
            case SOUTH -> deep
                    ? List.of(id("minecraft:desert"), id("minecraft:badlands"), id("minecraft:savanna"))
                    : List.of(id("minecraft:savanna"), id("biomesoplenty:dryland"), id("minecraft:desert"));
            case WEST -> deep
                    ? List.of(id("biomesoplenty:old_growth_woodland"), id("biomesoplenty:redwood_forest"), id("minecraft:dark_forest"), id("minecraft:taiga"))
                    : List.of(id("biomesoplenty:seasonal_forest"), id("biomesoplenty:maple_woods"), id("minecraft:dark_forest"), id("minecraft:taiga"));
        };

        for (ResourceLocation candidate : candidates) {
            if (lookup.containsKey(candidate)) return candidate;
        }
        return null;
    }

    private static Map<ResourceLocation, Holder<Biome>> lookupFor(BiomeSource source) {
        synchronized (HOLDER_LOOKUPS) {
            return HOLDER_LOOKUPS.computeIfAbsent(source, ignored -> {
                Map<ResourceLocation, Holder<Biome>> result = new HashMap<>();
                for (Holder<Biome> holder : source.possibleBiomes()) {
                    ResourceLocation id = key(holder);
                    if (id != null) result.put(id, holder);
                }
                return Map.copyOf(result);
            });
        }
    }

    private static ResourceLocation key(Holder<Biome> holder) {
        return holder.unwrapKey().map(resourceKey -> resourceKey.location()).orElse(null);
    }

    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }
}
