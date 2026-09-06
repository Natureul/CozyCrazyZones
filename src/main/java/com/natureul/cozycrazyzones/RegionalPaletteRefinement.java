package com.natureul.cozycrazyzones;

import net.minecraft.resources.ResourceLocation;

/**
 * Final visual-selection pass after the broad compatibility remapper.
 *
 * The generic remapper is deliberately conservative because it has to accept every native
 * TerraBlender/BOP shape. This pass is where regions that need a particularly strong visual
 * identity can be made more selective without touching Tectonic terrain density. Variants use
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

        // Greenveil must already read as humid/jungle-edge country once the 1,200-block transition
        // has finished. The Hearthlands remain gentler than the outer jungle, but generic Plains or
        // an isolated ornamental grove is not enough to communicate the eastern ecology.
        if (cell.macroRegion() == MacroRegion.EAST && cell.macroBoundaryStrength() >= 0.38D) {
            return greenveil(target, shape, cell, seed, blockX, blockZ);
        }

        // Harvestwood is intentionally much more curated than the old "temperate west" pass.
        // The user should read autumn/pumpkin/old-growth from the landscape itself, not only from
        // occasional structures or mobs.
        if (cell.macroRegion() == MacroRegion.WEST && cell.macroBoundaryStrength() >= 0.48D) {
            return harvestwood(target, original, shape, cell, seed, blockX, blockZ);
        }

        return target;
    }

    private static ResourceLocation greenveil(ResourceLocation target,
                                               BiomeRegionality.Shape shape,
                                               RegionalCell cell,
                                               long seed,
                                               int blockX,
                                               int blockZ) {
        double broad = RegionalNoise.fractal(
                seed ^ 0x47E3B11A9D6C5F21L,
                blockX,
                blockZ,
                980.0D
        );
        double detail = RegionalNoise.sample(
                seed ^ 0x1B6D88A4C2F9703EL,
                blockX,
                blockZ,
                520.0D
        );

        return switch (cell.radialZone()) {
            case HEARTHLANDS -> {
                if (shape == BiomeRegionality.Shape.WETLAND) {
                    if (broad > 0.28D) yield id("biomesoplenty:floodplain");
                    if (broad < -0.34D) yield id("minecraft:swamp");
                    yield id("biomesoplenty:marsh");
                }
                if (shape == BiomeRegionality.Shape.MOUNTAIN) {
                    yield broad > 0.12D
                            ? id("biomesoplenty:rocky_rainforest")
                            : id("biomesoplenty:jade_cliffs");
                }
                if (shape == BiomeRegionality.Shape.OPEN || shape == BiomeRegionality.Shape.ARID) {
                    if (broad > 0.38D) yield id("minecraft:sparse_jungle");
                    if (broad < -0.38D) yield id("biomesoplenty:jacaranda_glade");
                    yield id("biomesoplenty:overgrown_greens");
                }
                // Forested Hearthlands should feel like the margin of a jungle: flowering and
                // breathable near home, but with broad sparse-jungle/rainforest pockets by ~1.5 km.
                if (broad > 0.42D) yield id("biomesoplenty:rainforest");
                if (broad < -0.30D) yield id("biomesoplenty:jacaranda_glade");
                yield detail > 0.16D
                        ? id("minecraft:sparse_jungle")
                        : id("biomesoplenty:jacaranda_glade");
            }
            case FRONTIER -> {
                if (shape == BiomeRegionality.Shape.WETLAND) {
                    yield broad > 0.10D
                            ? id("biomesoplenty:floodplain")
                            : id("minecraft:mangrove_swamp");
                }
                if (shape == BiomeRegionality.Shape.MOUNTAIN) {
                    yield broad > -0.08D
                            ? id("biomesoplenty:rocky_rainforest")
                            : id("biomesoplenty:jade_cliffs");
                }
                if (shape == BiomeRegionality.Shape.OPEN || shape == BiomeRegionality.Shape.ARID) {
                    yield broad > -0.05D
                            ? id("minecraft:sparse_jungle")
                            : id("biomesoplenty:overgrown_greens");
                }
                if (broad > 0.30D) yield id("biomesoplenty:rainforest");
                if (broad < -0.34D) yield id("biomesoplenty:jacaranda_glade");
                yield detail > 0.0D
                        ? id("minecraft:sparse_jungle")
                        : id("biomesoplenty:rainforest");
            }
            case WILDLANDS -> {
                if (shape == BiomeRegionality.Shape.WETLAND) {
                    yield broad > 0.10D
                            ? id("minecraft:mangrove_swamp")
                            : id("biomesoplenty:bayou");
                }
                if (shape == BiomeRegionality.Shape.MOUNTAIN) {
                    yield id("biomesoplenty:rocky_rainforest");
                }
                if (broad > 0.34D) yield id("minecraft:bamboo_jungle");
                if (broad < -0.34D) yield id("biomesoplenty:rainforest");
                yield detail > 0.06D
                        ? id("minecraft:jungle")
                        : id("minecraft:sparse_jungle");
            }
            case DREAD_REACHES -> {
                if (shape == BiomeRegionality.Shape.WETLAND) {
                    yield broad > 0.0D
                            ? id("minecraft:mangrove_swamp")
                            : id("biomesoplenty:bayou");
                }
                if (shape == BiomeRegionality.Shape.MOUNTAIN) {
                    yield id("biomesoplenty:rocky_rainforest");
                }
                if (broad > 0.34D) yield id("biomesoplenty:tropics");
                if (broad < -0.30D) yield id("biomesoplenty:fungal_jungle");
                yield detail > 0.0D
                        ? id("minecraft:bamboo_jungle")
                        : id("minecraft:jungle");
            }
        };
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
