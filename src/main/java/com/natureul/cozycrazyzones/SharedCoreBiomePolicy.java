package com.natureul.cozycrazyzones;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/**
 * Keeps the immediate shared Hearthlands around home visually temperate and ecologically neutral.
 *
 * Some otherwise-common BOP open biomes are extremely broad, treeless and carry biome tags that
 * can make the inner countryside read like savanna/outback territory. Tectonic remains responsible
 * for the physical hills and valleys; this policy only swaps the biome identity before surface and
 * feature generation, using very broad deterministic patches so the result does not checkerboard.
 */
public final class SharedCoreBiomePolicy {
    private static final Set<ResourceLocation> TOO_BARE_FOR_CORE = Set.of(
            id("biomesoplenty:grassland"),
            id("biomesoplenty:prairie"),
            id("biomesoplenty:shrubland"),
            id("biomesoplenty:rocky_shrubland")
    );

    private SharedCoreBiomePolicy() {}

    public static boolean shouldTemper(ResourceLocation biomeId, RegionalCell cell) {
        return biomeId != null
                && cell.radialZone() == Region.HEARTHLANDS
                && cell.influenceBand() == RegionalInfluenceBand.SHARED_CORE
                && TOO_BARE_FOR_CORE.contains(biomeId);
    }

    public static ResourceLocation temper(ResourceLocation biomeId,
                                           RegionalCell cell,
                                           long seed,
                                           int blockX,
                                           int blockZ) {
        if (!shouldTemper(biomeId, cell)) return biomeId;

        // One broad field creates woodland interruptions hundreds of blocks wide rather than
        // noisy 4x4-biome speckling. A second broad sample provides occasional meadow clearings.
        double woodland = RegionalNoise.fractal(
                seed ^ 0xD6E8FEB86659FD93L,
                blockX,
                blockZ,
                760.0D
        );
        if (woodland > 0.27D) return id("minecraft:forest");
        if (woodland < -0.31D) return id("minecraft:birch_forest");

        double clearing = RegionalNoise.sample(
                seed ^ 0xA0761D6478BD642FL,
                blockX,
                blockZ,
                520.0D
        );
        if (clearing > 0.48D) return id("minecraft:meadow");
        return id("minecraft:plains");
    }

    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }
}
