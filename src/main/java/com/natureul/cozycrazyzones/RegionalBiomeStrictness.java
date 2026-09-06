package com.natureul.cozycrazyzones;

import net.minecraft.resources.ResourceLocation;

/**
 * Second-pass regional identity filter.
 *
 * BiomeRegionality is deliberately conservative so it can preserve native TerraBlender shapes.
 * The downside was that COMMON plains/forest identities could survive surprisingly deep into an
 * otherwise established cardinal region. This pass only targets those common identities: it keeps
 * a narrow neutral seam at organic macro borders and a short shared-country transition near home,
 * then converts generic terrain into a shape-compatible regional palette.
 */
public final class RegionalBiomeStrictness {
    private RegionalBiomeStrictness() {}

    public static ResourceLocation refine(ResourceLocation target,
                                          ResourceLocation original,
                                          RegionalCell cell,
                                          long seed,
                                          int blockX,
                                          int blockZ) {
        if (cell.influenceBand() == RegionalInfluenceBand.SHARED_CORE) return target;

        BiomeRegionality.Profile targetProfile = BiomeRegionality.profile(target).orElse(null);
        BiomeRegionality.Profile originalProfile = BiomeRegionality.profile(original).orElse(null);
        if (targetProfile == null) return target;

        BiomeRegionality.Shape shape = targetProfile.shape();
        if (shape == BiomeRegionality.Shape.OCEAN
                || shape == BiomeRegionality.Shape.RIVER
                || shape == BiomeRegionality.Shape.COAST
                || shape == BiomeRegionality.Shape.SPECIAL) {
            return target;
        }

        if (targetProfile.affinity() != BiomeRegionality.Affinity.COMMON) return target;
        if (cell.macroBoundaryStrength() < 0.30D) return target;

        if (cell.influenceBand() == RegionalInfluenceBand.CARDINAL_TRANSITION
                && cell.regionalStrength() < 0.34D) {
            return target;
        }

        if (originalProfile != null
                && originalProfile.shape() != BiomeRegionality.Shape.OCEAN
                && originalProfile.shape() != BiomeRegionality.Shape.RIVER
                && originalProfile.shape() != BiomeRegionality.Shape.COAST
                && originalProfile.shape() != BiomeRegionality.Shape.SPECIAL
                && targetProfile.shape() == BiomeRegionality.Shape.OPEN) {
            shape = originalProfile.shape();
        }

        int stage = switch (cell.radialZone()) {
            case HEARTHLANDS -> 1;
            case FRONTIER -> 2;
            case WILDLANDS -> 3;
            case DREAD_REACHES -> 4;
        };

        return regionalCommonTarget(cell.macroRegion(), stage, shape, seed, blockX, blockZ);
    }

    private static ResourceLocation regionalCommonTarget(MacroRegion region,
                                                          int stage,
                                                          BiomeRegionality.Shape shape,
                                                          long seed,
                                                          int x,
                                                          int z) {
        return switch (region) {
            case NORTH -> frost(stage, shape, seed, x, z);
            case EAST -> greenveil(stage, shape, seed, x, z);
            case SOUTH -> sunscar(stage, shape, seed, x, z);
            case WEST -> harvestwood(stage, shape, seed, x, z);
        };
    }

    private static ResourceLocation frost(int stage, BiomeRegionality.Shape shape, long seed, int x, int z) {
        if (stage == 1) {
            return switch (shape) {
                case FOREST -> choose(seed, x, z, 0x11A2L, "minecraft:taiga", "biomesoplenty:coniferous_forest", "biomesoplenty:fir_clearing");
                case WETLAND -> choose(seed, x, z, 0x11A3L, "biomesoplenty:bog", "minecraft:taiga");
                case MOUNTAIN -> choose(seed, x, z, 0x11A4L, "biomesoplenty:highland", "minecraft:meadow", "minecraft:taiga");
                default -> choose(seed, x, z, 0x11A5L, "biomesoplenty:field", "minecraft:taiga", "biomesoplenty:fir_clearing");
            };
        }
        if (stage == 2) {
            return switch (shape) {
                case FOREST -> choose(seed, x, z, 0x12A2L, "minecraft:taiga", "biomesoplenty:coniferous_forest", "minecraft:snowy_taiga");
                case WETLAND -> choose(seed, x, z, 0x12A3L, "biomesoplenty:bog", "biomesoplenty:muskeg");
                case MOUNTAIN -> choose(seed, x, z, 0x12A4L, "minecraft:snowy_slopes", "minecraft:jagged_peaks", "biomesoplenty:highland");
                default -> choose(seed, x, z, 0x12A5L, "biomesoplenty:tundra", "minecraft:snowy_plains", "minecraft:taiga");
            };
        }
        if (stage == 3) {
            return switch (shape) {
                case FOREST -> choose(seed, x, z, 0x13A2L, "minecraft:snowy_taiga", "biomesoplenty:snowy_coniferous_forest", "biomesoplenty:snowy_fir_clearing");
                case WETLAND -> choose(seed, x, z, 0x13A3L, "biomesoplenty:muskeg", "minecraft:snowy_taiga");
                case MOUNTAIN -> choose(seed, x, z, 0x13A4L, "minecraft:frozen_peaks", "minecraft:jagged_peaks", "minecraft:snowy_slopes");
                default -> choose(seed, x, z, 0x13A5L, "minecraft:snowy_plains", "biomesoplenty:tundra", "minecraft:ice_spikes");
            };
        }
        return switch (shape) {
            case FOREST -> choose(seed, x, z, 0x14A2L, "biomesoplenty:snowy_coniferous_forest", "minecraft:snowy_taiga", "biomesoplenty:snowy_maple_woods");
            case WETLAND -> choose(seed, x, z, 0x14A3L, "biomesoplenty:muskeg", "minecraft:snowy_taiga");
            case MOUNTAIN -> choose(seed, x, z, 0x14A4L, "minecraft:frozen_peaks", "minecraft:ice_spikes", "minecraft:snowy_slopes");
            default -> choose(seed, x, z, 0x14A5L, "minecraft:ice_spikes", "minecraft:snowy_plains", "biomesoplenty:cold_desert", "biomesoplenty:tundra");
        };
    }

    private static ResourceLocation greenveil(int stage, BiomeRegionality.Shape shape, long seed, int x, int z) {
        if (stage == 1) {
            return switch (shape) {
                case FOREST -> choose(seed, x, z, 0x21A2L, "biomesoplenty:jacaranda_glade", "biomesoplenty:orchard");
                case WETLAND -> choose(seed, x, z, 0x21A3L, "biomesoplenty:marsh", "minecraft:swamp", "biomesoplenty:floodplain");
                case MOUNTAIN -> choose(seed, x, z, 0x21A4L, "biomesoplenty:jade_cliffs", "biomesoplenty:rocky_rainforest", "minecraft:windswept_forest");
                default -> choose(seed, x, z, 0x21A5L, "biomesoplenty:overgrown_greens", "biomesoplenty:forested_field", "biomesoplenty:jacaranda_glade");
            };
        }
        if (stage == 2) {
            return switch (shape) {
                case FOREST -> choose(seed, x, z, 0x22A2L, "minecraft:sparse_jungle", "biomesoplenty:rainforest", "biomesoplenty:jacaranda_glade");
                case WETLAND -> choose(seed, x, z, 0x22A3L, "biomesoplenty:marsh", "biomesoplenty:floodplain", "minecraft:swamp");
                case MOUNTAIN -> choose(seed, x, z, 0x22A4L, "biomesoplenty:jade_cliffs", "biomesoplenty:rocky_rainforest");
                default -> choose(seed, x, z, 0x22A5L, "biomesoplenty:overgrown_greens", "minecraft:sparse_jungle", "biomesoplenty:rainforest");
            };
        }
        if (stage == 3) {
            return switch (shape) {
                case FOREST -> choose(seed, x, z, 0x23A2L, "minecraft:jungle", "biomesoplenty:rainforest", "biomesoplenty:rocky_rainforest");
                case WETLAND -> choose(seed, x, z, 0x23A3L, "minecraft:mangrove_swamp", "biomesoplenty:bayou", "biomesoplenty:wetland");
                case MOUNTAIN -> choose(seed, x, z, 0x23A4L, "biomesoplenty:rocky_rainforest", "biomesoplenty:jade_cliffs");
                default -> choose(seed, x, z, 0x23A5L, "minecraft:sparse_jungle", "minecraft:jungle", "biomesoplenty:overgrown_greens");
            };
        }
        return switch (shape) {
            case WETLAND -> choose(seed, x, z, 0x24A3L, "minecraft:mangrove_swamp", "biomesoplenty:bayou", "biomesoplenty:wetland");
            case MOUNTAIN -> choose(seed, x, z, 0x24A4L, "biomesoplenty:rocky_rainforest", "biomesoplenty:jade_cliffs");
            case OPEN, ARID -> choose(seed, x, z, 0x24A5L, "biomesoplenty:overgrown_greens", "biomesoplenty:tropics", "minecraft:bamboo_jungle");
            default -> choose(seed, x, z, 0x24A2L, "minecraft:bamboo_jungle", "biomesoplenty:fungal_jungle", "biomesoplenty:tropics", "minecraft:jungle");
        };
    }

    private static ResourceLocation sunscar(int stage, BiomeRegionality.Shape shape, long seed, int x, int z) {
        if (stage == 1) {
            return switch (shape) {
                case FOREST -> choose(seed, x, z, 0x31A2L, "biomesoplenty:mediterranean_forest", "minecraft:savanna", "biomesoplenty:lush_savanna");
                case WETLAND -> choose(seed, x, z, 0x31A3L, "biomesoplenty:lush_savanna", "biomesoplenty:floodplain", "minecraft:river");
                case MOUNTAIN -> choose(seed, x, z, 0x31A4L, "minecraft:savanna_plateau", "minecraft:windswept_savanna", "biomesoplenty:highland");
                default -> choose(seed, x, z, 0x31A5L, "minecraft:savanna", "biomesoplenty:lush_savanna", "biomesoplenty:scrubland");
            };
        }
        if (stage == 2) {
            return switch (shape) {
                case FOREST -> choose(seed, x, z, 0x32A2L, "biomesoplenty:mediterranean_forest", "minecraft:savanna", "minecraft:wooded_badlands");
                case WETLAND -> choose(seed, x, z, 0x32A3L, "biomesoplenty:lush_savanna", "biomesoplenty:dryland", "biomesoplenty:floodplain");
                case MOUNTAIN -> choose(seed, x, z, 0x32A4L, "minecraft:savanna_plateau", "minecraft:badlands", "minecraft:windswept_savanna");
                default -> choose(seed, x, z, 0x32A5L, "minecraft:savanna", "biomesoplenty:dryland", "minecraft:desert", "biomesoplenty:scrubland");
            };
        }
        if (stage == 3) {
            return switch (shape) {
                case FOREST -> choose(seed, x, z, 0x33A2L, "minecraft:wooded_badlands", "biomesoplenty:mediterranean_forest");
                case WETLAND -> choose(seed, x, z, 0x33A3L, "biomesoplenty:lush_desert", "biomesoplenty:dryland");
                case MOUNTAIN -> choose(seed, x, z, 0x33A4L, "minecraft:badlands", "minecraft:eroded_badlands", "biomesoplenty:volcano");
                default -> choose(seed, x, z, 0x33A5L, "minecraft:desert", "biomesoplenty:dryland", "minecraft:badlands");
            };
        }
        return switch (shape) {
            case FOREST -> choose(seed, x, z, 0x34A2L, "minecraft:wooded_badlands", "biomesoplenty:wasteland_steppe", "biomesoplenty:mediterranean_forest");
            case WETLAND -> choose(seed, x, z, 0x34A3L, "biomesoplenty:dryland", "minecraft:desert");
            case MOUNTAIN -> choose(seed, x, z, 0x34A4L, "minecraft:eroded_badlands", "biomesoplenty:volcano", "minecraft:badlands");
            default -> choose(seed, x, z, 0x34A5L, "biomesoplenty:volcanic_plains", "biomesoplenty:wasteland_steppe", "biomesoplenty:wasteland", "minecraft:desert");
        };
    }

    private static ResourceLocation harvestwood(int stage, BiomeRegionality.Shape shape, long seed, int x, int z) {
        if (stage == 1) {
            return switch (shape) {
                case FOREST -> choose(seed, x, z, 0x41A2L, "biomesoplenty:seasonal_forest", "biomesoplenty:maple_woods", "biomesoplenty:aspen_glade");
                case WETLAND -> choose(seed, x, z, 0x41A3L, "biomesoplenty:forested_field", "biomesoplenty:seasonal_forest");
                case MOUNTAIN -> choose(seed, x, z, 0x41A4L, "biomesoplenty:highland", "biomesoplenty:maple_woods", "minecraft:windswept_forest");
                default -> choose(seed, x, z, 0x41A5L, "biomesoplenty:orchard", "biomesoplenty:pumpkin_patch", "biomesoplenty:seasonal_forest");
            };
        }
        if (stage == 2) {
            return switch (shape) {
                case FOREST -> choose(seed, x, z, 0x42A2L, "biomesoplenty:seasonal_forest", "biomesoplenty:maple_woods", "biomesoplenty:redwood_forest");
                case WETLAND -> choose(seed, x, z, 0x42A3L, "biomesoplenty:forested_field", "biomesoplenty:old_growth_woodland");
                case MOUNTAIN -> choose(seed, x, z, 0x42A4L, "biomesoplenty:highland", "biomesoplenty:redwood_forest", "minecraft:windswept_forest");
                default -> choose(seed, x, z, 0x42A5L, "biomesoplenty:pumpkin_patch", "biomesoplenty:seasonal_forest", "biomesoplenty:orchard");
            };
        }
        if (stage == 3) {
            return switch (shape) {
                case FOREST -> choose(seed, x, z, 0x43A2L, "biomesoplenty:redwood_forest", "biomesoplenty:old_growth_woodland", "minecraft:dark_forest");
                case WETLAND -> choose(seed, x, z, 0x43A3L, "biomesoplenty:old_growth_woodland", "biomesoplenty:ominous_woods");
                case MOUNTAIN -> choose(seed, x, z, 0x43A4L, "biomesoplenty:redwood_forest", "biomesoplenty:highland");
                default -> choose(seed, x, z, 0x43A5L, "biomesoplenty:pumpkin_patch", "biomesoplenty:seasonal_forest", "biomesoplenty:old_growth_woodland");
            };
        }
        return switch (shape) {
            case WETLAND -> choose(seed, x, z, 0x44A3L, "biomesoplenty:ominous_woods", "biomesoplenty:old_growth_dead_forest");
            case MOUNTAIN -> choose(seed, x, z, 0x44A4L, "biomesoplenty:redwood_forest", "biomesoplenty:old_growth_woodland");
            case OPEN, ARID -> choose(seed, x, z, 0x44A5L, "biomesoplenty:pumpkin_patch", "biomesoplenty:dead_forest", "biomesoplenty:seasonal_forest");
            default -> choose(seed, x, z, 0x44A2L, "biomesoplenty:ominous_woods", "biomesoplenty:old_growth_dead_forest", "biomesoplenty:redwood_forest");
        };
    }

    private static ResourceLocation choose(long seed, int x, int z, long salt, String... candidates) {
        double n = RegionalNoise.fractal(seed ^ salt, x, z, 1050.0D);
        double unit = Math.max(0.0D, Math.min(0.999999D, (n + 1.0D) * 0.5D));
        int index = (int) Math.floor(unit * candidates.length);
        return new ResourceLocation(candidates[index]);
    }
}
