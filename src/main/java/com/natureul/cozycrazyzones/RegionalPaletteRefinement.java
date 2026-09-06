package com.natureul.cozycrazyzones;

import net.minecraft.resources.ResourceLocation;

/**
 * Final visual-selection pass after the broad compatibility remapper.
 *
 * The generic remapper is deliberately conservative because it has to accept every native
 * TerraBlender/BOP shape.  This pass is where a region that needs a particularly strong visual
 * identity can be made more selective without touching Tectonic terrain density.  Variants use
 * low-frequency noise, so one huge native biome becomes a few broad landscape belts rather than
 * quart-sized confetti.
 */
public final class RegionalPaletteRefinement {
    private RegionalPaletteRefinement() {}

    public static ResourceLocation refine(ResourceLocation target,
                                          ResourceLocation original,
                                          RegionalCell cell,
                                          long seed,
                                          int blockX,
                                          int blockZ) {
        if (cell.influenceBand() != RegionalInfluenceBand.ESTABLISHED) return target;
        if (cell.macroBoundaryStrength() < 0.48D) return target;

        BiomeRegionality.Profile targetProfile = BiomeRegionality.profile(target).orElse(null);
        BiomeRegionality.Profile originalProfile = BiomeRegionality.profile(original).orElse(null);
        BiomeRegionality.Shape shape = targetProfile != null
                ? targetProfile.shape()
                : originalProfile != null ? originalProfile.shape() : null;
        if (shape == null
                || shape == BiomeRegionality.Shape.OCEAN
                || shape == BiomeRegionality.Shape.RIVER
                || shape == BiomeRegionality.Shape.COAST) {
            return target;
        }

        // Harvestwood is intentionally much more curated than the old "temperate west" pass.
        // The user should read autumn/pumpkin/old-growth from the landscape itself, not only from
        // occasional structures or mobs.
        if (cell.macroRegion() == MacroRegion.WEST) {
            return harvestwood(target, original, shape, cell, seed, blockX, blockZ);
        }

        return target;
    }

    private static ResourceLocation harvestwood(ResourceLocation target,
                                                 ResourceLocation original,
                                                 BiomeRegionality.Shape shape,
                                                 RegionalCell cell,
                                                 long seed,
                                                 int blockX,
                                                 int blockZ) {
        double broad = RegionalNoise.fractal(
                seed ^ 0xA11CE5EED5EEDL,
                blockX,
                blockZ,
                1050.0D
        );
        double detail = RegionalNoise.sample(
                seed ^ 0x6C8E9CF570932BD5L,
                blockX,
                blockZ,
                560.0D
        );

        return switch (cell.radialZone()) {
            case HEARTHLANDS -> {
                if (shape == BiomeRegionality.Shape.OPEN || shape == BiomeRegionality.Shape.ARID) {
                    if (broad > 0.18D) yield id("biomesoplenty:pumpkin_patch");
                    if (broad < -0.34D) yield id("biomesoplenty:maple_woods");
                    yield id("biomesoplenty:seasonal_forest");
                }
                if (shape == BiomeRegionality.Shape.MOUNTAIN) {
                    yield broad > 0.24D
                            ? id("biomesoplenty:maple_woods")
                            : id("biomesoplenty:seasonal_forest");
                }
                if (shape == BiomeRegionality.Shape.WETLAND) {
                    yield detail > 0.18D
                            ? id("biomesoplenty:seasonal_forest")
                            : id("biomesoplenty:forested_field");
                }
                if (broad > 0.38D) yield id("biomesoplenty:pumpkin_patch");
                if (broad < -0.38D) yield id("biomesoplenty:aspen_glade");
                yield detail > 0.10D
                        ? id("biomesoplenty:maple_woods")
                        : id("biomesoplenty:seasonal_forest");
            }
            case FRONTIER -> {
                if (shape == BiomeRegionality.Shape.OPEN || shape == BiomeRegionality.Shape.ARID) {
                    yield broad > -0.08D
                            ? id("biomesoplenty:pumpkin_patch")
                            : id("biomesoplenty:seasonal_forest");
                }
                if (shape == BiomeRegionality.Shape.MOUNTAIN) {
                    yield broad > 0.05D
                            ? id("biomesoplenty:redwood_forest")
                            : id("biomesoplenty:maple_woods");
                }
                if (shape == BiomeRegionality.Shape.WETLAND) {
                    yield broad < -0.20D
                            ? id("biomesoplenty:old_growth_woodland")
                            : id("biomesoplenty:seasonal_forest");
                }
                if (broad > 0.42D) yield id("biomesoplenty:redwood_forest");
                if (broad < -0.42D) yield id("biomesoplenty:pumpkin_patch");
                yield detail > 0.08D
                        ? id("biomesoplenty:maple_woods")
                        : id("biomesoplenty:seasonal_forest");
            }
            case WILDLANDS -> {
                if (shape == BiomeRegionality.Shape.OPEN || shape == BiomeRegionality.Shape.ARID) {
                    yield broad > 0.18D
                            ? id("biomesoplenty:pumpkin_patch")
                            : id("biomesoplenty:old_growth_woodland");
                }
                if (broad > 0.34D) yield id("biomesoplenty:redwood_forest");
                if (broad < -0.38D) yield id("minecraft:dark_forest");
                yield detail > 0.12D
                        ? id("biomesoplenty:old_growth_woodland")
                        : id("biomesoplenty:dead_forest");
            }
            case DREAD_REACHES -> {
                if ((shape == BiomeRegionality.Shape.OPEN || shape == BiomeRegionality.Shape.ARID)
                        && broad > 0.30D) {
                    yield id("biomesoplenty:pumpkin_patch");
                }
                if (broad > 0.48D) yield id("biomesoplenty:redwood_forest");
                if (broad < -0.22D) yield id("biomesoplenty:old_growth_dead_forest");
                yield detail > 0.0D
                        ? id("biomesoplenty:ominous_woods")
                        : id("biomesoplenty:dead_forest");
            }
        };
    }

    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }
}
