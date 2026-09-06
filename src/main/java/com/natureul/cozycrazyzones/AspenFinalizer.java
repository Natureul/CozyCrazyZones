package com.natureul.cozycrazyzones;

import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.HashMap;
import java.util.Map;

/**
 * Last-line surface-biome guard at the actual chunk palette.
 *
 * Aspen is never legal anywhere in the Hearthlands. The same final pass also prevents enormous
 * bare BOP grassland/prairie/shrubland belts from surviving in the immediate shared Hearthlands.
 * Source-level guards should already enforce both contracts; this remains defense in depth against
 * another worldgen path writing an unwanted biome into the completed chunk palette.
 */
public final class AspenFinalizer {
    private static final ResourceLocation ASPEN = id("biomesoplenty:aspen_glade");

    private AspenFinalizer() {}

    public static void sanitize(ChunkAccess chunk, BiomeSource source, Climate.Sampler sampler) {
        if (!WorldGeographyContext.prepared() || !surfaceNeedsSanitizing(chunk)) return;

        Map<ResourceLocation, Holder<Biome>> lookup = new HashMap<>();
        for (Holder<Biome> holder : source.possibleBiomes()) {
            holder.unwrapKey().ifPresent(key -> lookup.put(key.location(), holder));
        }

        chunk.fillBiomesFromNoise((quartX, quartY, quartZ, ignored) -> {
            Holder<Biome> original = rawNoiseBiome(chunk, quartX, quartY, quartZ);
            ResourceLocation originalId = original.unwrapKey().map(key -> key.location()).orElse(null);
            if (originalId == null || QuartPos.toBlock(quartY) < 48) return original;

            int blockX = QuartPos.toBlock(quartX);
            int blockZ = QuartPos.toBlock(quartZ);
            RegionalCell cell = WorldGeographyContext.cellAt(blockX, blockZ);

            ResourceLocation coreTarget = SharedCoreBiomePolicy.temper(
                    originalId,
                    cell,
                    WorldGeographyContext.worldSeed(),
                    blockX,
                    blockZ
            );
            if (!coreTarget.equals(originalId)) {
                ResourceLocation available = firstAvailable(
                        lookup,
                        coreTarget,
                        id("minecraft:plains"),
                        id("minecraft:forest"),
                        id("minecraft:birch_forest"),
                        id("minecraft:meadow")
                );
                if (available != null) return lookup.getOrDefault(available, original);
            }

            if (!ASPEN.equals(originalId)) return original;

            boolean legal = !WorldGeographyContext.provisionalAnchor()
                    && cell.radialZone() != Region.HEARTHLANDS
                    && cell.macroRegion() == MacroRegion.WEST
                    && cell.influenceBand() == RegionalInfluenceBand.ESTABLISHED
                    && cell.macroBoundaryStrength() >= 0.42D;
            if (legal) return original;

            ResourceLocation target;
            if (WorldGeographyContext.provisionalAnchor()
                    || cell.radialZone() == Region.HEARTHLANDS
                    || cell.influenceBand() != RegionalInfluenceBand.ESTABLISHED
                    || cell.macroBoundaryStrength() < 0.42D) {
                target = firstAvailable(lookup,
                        id("minecraft:birch_forest"),
                        id("minecraft:forest"),
                        id("biomesoplenty:woodland"),
                        id("minecraft:plains"));
            } else {
                target = switch (cell.macroRegion()) {
                    case NORTH -> firstAvailable(lookup,
                            id("minecraft:taiga"), id("biomesoplenty:coniferous_forest"), id("minecraft:birch_forest"));
                    case EAST -> firstAvailable(lookup,
                            id("minecraft:sparse_jungle"), id("biomesoplenty:jacaranda_glade"),
                            id("biomesoplenty:overgrown_greens"), id("minecraft:forest"));
                    case SOUTH -> firstAvailable(lookup,
                            id("minecraft:savanna"), id("biomesoplenty:lush_savanna"), id("minecraft:plains"));
                    case WEST -> firstAvailable(lookup, id("minecraft:birch_forest"), id("minecraft:forest"));
                };
            }

            return target == null ? original : lookup.getOrDefault(target, original);
        }, sampler);
    }

    private static boolean surfaceNeedsSanitizing(ChunkAccess chunk) {
        int quartY = QuartPos.fromBlock(80);
        int minQuartY = QuartPos.fromBlock(chunk.getMinBuildHeight());
        int maxQuartY = minQuartY + QuartPos.fromBlock(chunk.getHeight()) - 1;
        int clampedQuartY = Math.max(minQuartY, Math.min(maxQuartY, quartY));
        int blockY = QuartPos.toBlock(clampedQuartY);
        int sectionIndex = chunk.getSectionIndex(blockY);
        int minBlockX = chunk.getPos().getMinBlockX();
        int minBlockZ = chunk.getPos().getMinBlockZ();

        for (int localX = 0; localX < 4; localX++) {
            for (int localZ = 0; localZ < 4; localZ++) {
                Holder<Biome> holder = chunk.getSection(sectionIndex).getNoiseBiome(localX, clampedQuartY & 3, localZ);
                ResourceLocation biomeId = holder.unwrapKey().map(key -> key.location()).orElse(null);
                if (ASPEN.equals(biomeId)) return true;

                int blockX = minBlockX + localX * 4;
                int blockZ = minBlockZ + localZ * 4;
                RegionalCell cell = WorldGeographyContext.cellAt(blockX, blockZ);
                if (SharedCoreBiomePolicy.shouldTemper(biomeId, cell)) return true;
            }
        }
        return false;
    }

    private static Holder<Biome> rawNoiseBiome(ChunkAccess chunk, int quartX, int quartY, int quartZ) {
        int minQuartY = QuartPos.fromBlock(chunk.getMinBuildHeight());
        int maxQuartY = minQuartY + QuartPos.fromBlock(chunk.getHeight()) - 1;
        int clampedQuartY = Math.max(minQuartY, Math.min(maxQuartY, quartY));
        int blockY = QuartPos.toBlock(clampedQuartY);
        int sectionIndex = chunk.getSectionIndex(blockY);
        return chunk.getSection(sectionIndex).getNoiseBiome(quartX & 3, clampedQuartY & 3, quartZ & 3);
    }

    private static ResourceLocation firstAvailable(Map<ResourceLocation, Holder<Biome>> lookup,
                                                   ResourceLocation... candidates) {
        for (ResourceLocation candidate : candidates) if (lookup.containsKey(candidate)) return candidate;
        return null;
    }

    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }
}
