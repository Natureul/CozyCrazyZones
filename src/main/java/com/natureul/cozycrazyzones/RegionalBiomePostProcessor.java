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
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Regionalizes the already-completed BIOMES-stage chunk palette.
 *
 * This intentionally does NOT compete with TerraBlender/Citadel inside
 * MultiNoiseBiomeSource#getNoiseBiome. TerraBlender gets to produce the native large-scale biome
 * shapes first; once that asynchronous pass is complete, we rewrite the finished chunk palette.
 * That makes CozyCrazyZones the final geographic policy without depending on mixin ordering.
 *
 * Important 1.20.1 detail: ChunkStatus marks a ProtoChunk as BIOMES only after createBiomes' future
 * returns to the status pipeline. Therefore ProtoChunk#getNoiseBiome is still guarded while our
 * thenApply callback runs, even though its LevelChunkSection palettes have already been filled.
 * Read those section palettes directly instead of calling the guarded ProtoChunk method.
 */
public final class RegionalBiomePostProcessor {
    private static final ResourceLocation ASPEN_GLADE = id("biomesoplenty:aspen_glade");
    private static final ResourceLocation SEASONAL_FOREST = id("biomesoplenty:seasonal_forest");
    private static final ResourceLocation MOOR = id("biomesoplenty:moor");

    private static final Map<BiomeSource, Map<ResourceLocation, Holder<Biome>>> HOLDER_LOOKUPS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final ConcurrentMap<ChunkAccess, boolean[]> CONVERTED_OCEAN_MASKS = new ConcurrentHashMap<>();

    private static final AtomicBoolean FIRST_REMAP_LOGGED = new AtomicBoolean();
    private static final AtomicBoolean FIRST_OCEAN_CONVERSION_LOGGED = new AtomicBoolean();
    private static final AtomicBoolean FIRST_MISSING_TARGET_LOGGED = new AtomicBoolean();

    private RegionalBiomePostProcessor() {}

    public static void regionalize(ChunkAccess chunk,
                                   int seaLevel,
                                   BiomeSource biomeSource,
                                   Climate.Sampler sampler) {
        if (!WorldGeographyContext.prepared()) return;

        Map<ResourceLocation, Holder<Biome>> lookup = lookupFor(biomeSource);
        int minQuartX = QuartPos.fromBlock(chunk.getPos().getMinBlockX());
        int minQuartZ = QuartPos.fromBlock(chunk.getPos().getMinBlockZ());
        int surfaceQuartY = QuartPos.fromBlock(seaLevel);

        // Snapshot only the 4x4 surface palette needed by the physical ocean-conversion pass.
        // Do NOT call ProtoChunk#getNoiseBiome here: createBiomes has filled the section palettes,
        // but ChunkStatus has not yet advanced the ProtoChunk to BIOMES.
        @SuppressWarnings("unchecked")
        Holder<Biome>[] nativeSurface = (Holder<Biome>[]) new Holder<?>[16];
        for (int qz = 0; qz < 4; qz++) {
            for (int qx = 0; qx < 4; qx++) {
                nativeSurface[qz * 4 + qx] = rawNoiseBiome(
                        chunk,
                        minQuartX + qx,
                        surfaceQuartY,
                        minQuartZ + qz
                );
            }
        }

        // ChunkAccess#fillBiomesFromNoise is safe before the status flag flips. Its section-level
        // implementation recreates a palette, resolves all 64 cells, and only then swaps it in.
        // rawNoiseBiome therefore still sees the native palette for the section currently being
        // resolved, without touching ProtoChunk's status guard.
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
            if (targetId.equals(originalId)) return original;

            Holder<Biome> replacement = lookup.get(targetId);
            if (replacement == null) {
                if (FIRST_MISSING_TARGET_LOGGED.compareAndSet(false, true)) {
                    CozyCrazyZones.LOGGER.warn(
                            "Regional palette wanted {} -> {} at {},{} but the target is absent from this biome source",
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

        // Derive the terrain mask from the native surface snapshot and the exact remap decision.
        // Reading the ProtoChunk again here would hit the same status guard that caused v0.3.3 to
        // crash. Also require the target holder to exist, because a missing target means the actual
        // palette stayed native and the physical terrain must not be raised.
        boolean[] converted = new boolean[16];
        boolean anyConverted = false;
        for (int qz = 0; qz < 4; qz++) {
            for (int qx = 0; qx < 4; qx++) {
                int index = qz * 4 + qx;
                ResourceLocation before = key(nativeSurface[index]);
                if (before == null || !BiomeRegionality.isOcean(before)) continue;

                int blockX = QuartPos.toBlock(minQuartX + qx);
                int blockZ = QuartPos.toBlock(minQuartZ + qz);
                RegionalCell cell = WorldGeographyContext.cellAt(blockX, blockZ);
                ResourceLocation target = remap(before, cell, blockX, blockZ);

                if (!target.equals(before)
                        && lookup.containsKey(target)
                        && !BiomeRegionality.isOcean(target)) {
                    converted[index] = true;
                    anyConverted = true;
                }
            }
        }

        if (anyConverted) {
            CONVERTED_OCEAN_MASKS.put(chunk, converted);
            if (FIRST_OCEAN_CONVERSION_LOGGED.compareAndSet(false, true)) {
                CozyCrazyZones.LOGGER.info(
                        "Hearthlands native-ocean conversion ACTIVE in chunk {},{}; tapered land shaping armed",
                        chunk.getPos().x, chunk.getPos().z
                );
            }
        }
    }

    /**
     * Directly reads the section biome palette and intentionally bypasses ProtoChunk#getNoiseBiome.
     * The latter rejects reads until ChunkStatus.BIOMES is committed, which happens just after the
     * createBiomes future (and our postprocessor) returns.
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
     * Consumed exactly once by the subsequent NOISE-stage terrain pass.
     */
    public static boolean[] takeConvertedOceanMask(ChunkAccess chunk) {
        return CONVERTED_OCEAN_MASKS.remove(chunk);
    }

    public static void clearTransientState() {
        CONVERTED_OCEAN_MASKS.clear();
        synchronized (HOLDER_LOOKUPS) {
            HOLDER_LOOKUPS.clear();
        }
        FIRST_REMAP_LOGGED.set(false);
        FIRST_OCEAN_CONVERSION_LOGGED.set(false);
        FIRST_MISSING_TARGET_LOGGED.set(false);
    }

    private static ResourceLocation remap(ResourceLocation original,
                                          RegionalCell cell,
                                          int blockX,
                                          int blockZ) {
        if (ASPEN_GLADE.equals(original)) return remapAspen(cell, blockX, blockZ);
        if (MOOR.equals(original)) return remapMoor(cell);

        if (BiomeRegionality.isOcean(original) && cell.radialZone() == Region.HEARTHLANDS) {
            if (HearthlandsOceanPolicy.keepOcean(cell, WorldGeographyContext.worldSeed(), blockX, blockZ)) {
                return hearthlandsOcean(cell.macroRegion(), original);
            }
            return hearthlandsFormerOceanLand(cell, original);
        }

        ResourceLocation target = BiomeRegionality.remap(
                original,
                cell,
                WorldGeographyContext.worldSeed(),
                blockX,
                blockZ
        );

        // Aquamirae's Ice Maze tags frozen/deep-frozen ocean. Keep the northern sea merely cold
        // through Frontier/Wildlands; frozen ocean is reserved for Frostmarch Dread Reaches.
        if (BiomeRegionality.isOcean(original) && cell.macroRegion() == MacroRegion.NORTH) {
            boolean deep = original.getPath().startsWith("deep_");
            if (cell.radialZone() == Region.DREAD_REACHES) {
                return id(deep ? "minecraft:deep_frozen_ocean" : "minecraft:frozen_ocean");
            }
            if (target.getPath().equals("frozen_ocean") || target.getPath().equals("deep_frozen_ocean")) {
                return id(deep ? "minecraft:deep_cold_ocean" : "minecraft:cold_ocean");
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
        // Re-use the established autumn-forest profile so Aspen cannot leak into another macro-region.
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

    private static ResourceLocation hearthlandsFormerOceanLand(RegionalCell cell, ResourceLocation original) {
        if (cell.influenceBand() == RegionalInfluenceBand.SHARED_CORE) {
            return Math.floorMod(original.toString().hashCode(), 2) == 0
                    ? id("minecraft:plains")
                    : id("biomesoplenty:grassland");
        }

        return switch (cell.macroRegion()) {
            case NORTH -> pick(original, "biomesoplenty:field", "minecraft:meadow", "minecraft:taiga");
            case EAST -> pick(original, "biomesoplenty:overgrown_greens", "biomesoplenty:forested_field", "minecraft:plains");
            case SOUTH -> pick(original, "minecraft:savanna", "biomesoplenty:lush_savanna", "biomesoplenty:scrubland");
            case WEST -> pick(original, "biomesoplenty:aspen_glade", "biomesoplenty:seasonal_forest", "biomesoplenty:prairie");
        };
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

    private static ResourceLocation pick(ResourceLocation original, String... candidates) {
        return id(candidates[Math.floorMod(original.toString().hashCode(), candidates.length)]);
    }

    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }
}
