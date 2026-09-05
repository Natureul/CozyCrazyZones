package com.natureul.cozycrazyzones;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Runtime classification derived from the 2026-09-05 full-pack biome registry dump.
 *
 * The biome source remapper deliberately preserves Minecraft/TerraBlender's large native biome
 * shapes. We replace incompatible biome identities instead of sprinkling per-block random biomes,
 * so the result stays continental and coherent.
 */
public final class BiomeRegionality {
    public enum Affinity {
        COMMON,
        FROSTMARCH,
        GREENVEIL,
        SUNSCAR,
        AMBERWOOD,
        OCEAN,
        RIVER,
        SPECIAL
    }

    public enum Shape {
        OPEN,
        FOREST,
        WETLAND,
        MOUNTAIN,
        ARID,
        COAST,
        OCEAN,
        RIVER,
        SPECIAL
    }

    public record Profile(Affinity affinity, Shape shape, int intensity) {}

    private static final Map<ResourceLocation, Profile> PROFILES = new LinkedHashMap<>();

    static {
        profile(Affinity.COMMON, Shape.OPEN, 0,
                "minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow",
                "biomesoplenty:grassland", "biomesoplenty:highland", "biomesoplenty:lavender_field",
                "biomesoplenty:pasture", "biomesoplenty:prairie", "biomesoplenty:rocky_shrubland",
                "biomesoplenty:shrubland");
        profile(Affinity.COMMON, Shape.FOREST, 0,
                "minecraft:forest", "minecraft:birch_forest", "minecraft:old_growth_birch_forest",
                "minecraft:flower_forest", "minecraft:windswept_forest",
                "biomesoplenty:aspen_glade", "biomesoplenty:forested_field",
                "biomesoplenty:woodland");
        profile(Affinity.COMMON, Shape.MOUNTAIN, 0,
                "minecraft:windswept_hills", "minecraft:windswept_gravelly_hills",
                "minecraft:stony_peaks", "biomesoplenty:crag");
        profile(Affinity.COMMON, Shape.COAST, 0,
                "minecraft:beach", "minecraft:stony_shore", "biomesoplenty:gravel_beach");
        profile(Affinity.COMMON, Shape.SPECIAL, 0,
                "minecraft:mushroom_fields", "biomesoplenty:origin_valley");

        profile(Affinity.FROSTMARCH, Shape.FOREST, 1,
                "minecraft:taiga", "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga",
                "biomesoplenty:coniferous_forest", "biomesoplenty:fir_clearing");
        profile(Affinity.FROSTMARCH, Shape.OPEN, 1,
                "biomesoplenty:field");
        profile(Affinity.FROSTMARCH, Shape.WETLAND, 1,
                "biomesoplenty:bog");
        profile(Affinity.FROSTMARCH, Shape.FOREST, 2,
                "minecraft:snowy_taiga", "minecraft:grove",
                "biomesoplenty:snowblossom_grove", "biomesoplenty:snowy_coniferous_forest",
                "biomesoplenty:snowy_fir_clearing", "biomesoplenty:snowy_maple_woods");
        profile(Affinity.FROSTMARCH, Shape.OPEN, 2,
                "minecraft:snowy_plains", "biomesoplenty:tundra");
        profile(Affinity.FROSTMARCH, Shape.WETLAND, 2,
                "biomesoplenty:muskeg", "biomesoplenty:hot_springs");
        profile(Affinity.FROSTMARCH, Shape.MOUNTAIN, 2,
                "minecraft:snowy_slopes", "minecraft:jagged_peaks");
        profile(Affinity.FROSTMARCH, Shape.ARID, 3,
                "biomesoplenty:cold_desert");
        profile(Affinity.FROSTMARCH, Shape.MOUNTAIN, 3,
                "minecraft:frozen_peaks");
        profile(Affinity.FROSTMARCH, Shape.OPEN, 3,
                "minecraft:ice_spikes");
        profile(Affinity.FROSTMARCH, Shape.SPECIAL, 3,
                "biomesoplenty:auroral_garden", "biomesoplenty:wintry_origin_valley");
        profile(Affinity.FROSTMARCH, Shape.COAST, 2,
                "minecraft:snowy_beach");

        profile(Affinity.GREENVEIL, Shape.FOREST, 1,
                "biomesoplenty:jacaranda_glade", "biomesoplenty:orchard");
        profile(Affinity.GREENVEIL, Shape.OPEN, 1,
                "biomesoplenty:overgrown_greens");
        profile(Affinity.GREENVEIL, Shape.WETLAND, 1,
                "biomesoplenty:marsh");
        profile(Affinity.GREENVEIL, Shape.FOREST, 2,
                "minecraft:sparse_jungle", "biomesoplenty:rainforest");
        profile(Affinity.GREENVEIL, Shape.WETLAND, 2,
                "minecraft:swamp", "minecraft:mangrove_swamp",
                "biomesoplenty:bayou", "biomesoplenty:floodplain", "biomesoplenty:wetland");
        profile(Affinity.GREENVEIL, Shape.MOUNTAIN, 2,
                "biomesoplenty:jade_cliffs", "biomesoplenty:rocky_rainforest");
        profile(Affinity.GREENVEIL, Shape.FOREST, 3,
                "minecraft:jungle", "minecraft:bamboo_jungle",
                "biomesoplenty:fungal_jungle", "biomesoplenty:tropics");

        profile(Affinity.SUNSCAR, Shape.OPEN, 1,
                "minecraft:savanna", "biomesoplenty:lush_savanna", "biomesoplenty:scrubland");
        profile(Affinity.SUNSCAR, Shape.FOREST, 1,
                "biomesoplenty:mediterranean_forest");
        profile(Affinity.SUNSCAR, Shape.MOUNTAIN, 1,
                "minecraft:savanna_plateau", "minecraft:windswept_savanna");
        profile(Affinity.SUNSCAR, Shape.ARID, 2,
                "minecraft:desert", "biomesoplenty:dryland", "biomesoplenty:lush_desert");
        profile(Affinity.SUNSCAR, Shape.MOUNTAIN, 2,
                "minecraft:badlands", "minecraft:wooded_badlands");
        profile(Affinity.SUNSCAR, Shape.COAST, 1,
                "biomesoplenty:dune_beach");
        profile(Affinity.SUNSCAR, Shape.ARID, 3,
                "minecraft:eroded_badlands", "biomesoplenty:wasteland",
                "biomesoplenty:wasteland_steppe");
        profile(Affinity.SUNSCAR, Shape.MOUNTAIN, 3,
                "biomesoplenty:volcano");
        profile(Affinity.SUNSCAR, Shape.OPEN, 3,
                "biomesoplenty:volcanic_plains");

        profile(Affinity.AMBERWOOD, Shape.FOREST, 1,
                "minecraft:cherry_grove", "biomesoplenty:maple_woods",
                "biomesoplenty:seasonal_forest", "biomesoplenty:pumpkin_patch");
        profile(Affinity.AMBERWOOD, Shape.FOREST, 2,
                "minecraft:dark_forest", "biomesoplenty:redwood_forest",
                "biomesoplenty:old_growth_woodland", "biomesoplenty:dead_forest");
        profile(Affinity.AMBERWOOD, Shape.FOREST, 3,
                "biomesoplenty:old_growth_dead_forest", "biomesoplenty:ominous_woods");
        profile(Affinity.AMBERWOOD, Shape.SPECIAL, 2,
                "biomesoplenty:mystic_grove");

        profile(Affinity.RIVER, Shape.RIVER, 0,
                "minecraft:river");
        profile(Affinity.FROSTMARCH, Shape.RIVER, 2,
                "minecraft:frozen_river");

        profile(Affinity.OCEAN, Shape.OCEAN, 0,
                "minecraft:ocean", "minecraft:deep_ocean");
        profile(Affinity.OCEAN, Shape.OCEAN, 1,
                "minecraft:lukewarm_ocean", "minecraft:deep_lukewarm_ocean",
                "minecraft:cold_ocean", "minecraft:deep_cold_ocean");
        profile(Affinity.OCEAN, Shape.OCEAN, 2,
                "minecraft:warm_ocean", "minecraft:frozen_ocean", "minecraft:deep_frozen_ocean");
    }

    private BiomeRegionality() {}

    private static void profile(Affinity affinity, Shape shape, int intensity, String... ids) {
        for (String value : ids) {
            PROFILES.put(new ResourceLocation(value), new Profile(affinity, shape, intensity));
        }
    }

    public static Optional<Profile> profile(ResourceLocation id) {
        return Optional.ofNullable(PROFILES.get(id));
    }

    public static boolean isManagedSurfaceBiome(ResourceLocation id) {
        return PROFILES.containsKey(id);
    }

    public static boolean isRiver(ResourceLocation id) {
        Profile profile = PROFILES.get(id);
        return profile != null && profile.shape() == Shape.RIVER;
    }

    public static boolean isOcean(ResourceLocation id) {
        Profile profile = PROFILES.get(id);
        return profile != null && profile.shape() == Shape.OCEAN;
    }

    public static boolean isWetland(ResourceLocation id) {
        Profile profile = PROFILES.get(id);
        return profile != null && profile.shape() == Shape.WETLAND;
    }

    public static ResourceLocation remap(ResourceLocation original,
                                         RegionalCell cell,
                                         long worldSeed,
                                         int blockX,
                                         int blockZ) {
        Profile source = PROFILES.get(original);
        if (source == null) return original;

        if (source.shape() == Shape.RIVER) {
            if (cell.influenceBand() == RegionalInfluenceBand.ESTABLISHED
                    && cell.macroRegion() == MacroRegion.NORTH
                    && cell.radialZone().atLeast(Region.FRONTIER)) {
                return id("minecraft:frozen_river");
            }
            if (cell.macroRegion() != MacroRegion.NORTH && original.toString().equals("minecraft:frozen_river")) {
                return id("minecraft:river");
            }
            return original;
        }

        if (cell.influenceBand() == RegionalInfluenceBand.SHARED_CORE) {
            if (source.shape() == Shape.OCEAN && keepHearthlandsOcean(cell, worldSeed, blockX, blockZ)) {
                return id("minecraft:ocean");
            }
            if (source.affinity() == Affinity.COMMON && source.intensity() == 0) return original;
            return commonTarget(original, source.shape());
        }

        if (source.shape() == Shape.OCEAN && cell.radialZone() == Region.HEARTHLANDS) {
            if (!keepHearthlandsOcean(cell, worldSeed, blockX, blockZ)) {
                return cell.influenceBand() == RegionalInfluenceBand.CARDINAL_TRANSITION
                        ? transitionTarget(cell.macroRegion(), original, Shape.OPEN)
                        : regionalTarget(cell.macroRegion(), Region.HEARTHLANDS, original, Shape.OPEN);
            }
            return hearthlandsOceanTarget(cell.macroRegion());
        }

        if (cell.macroBoundaryStrength() < 0.42D && cell.radialZone().tier() <= Region.WILDLANDS.tier()) {
            if (source.shape() == Shape.OCEAN) return original;
            if (source.affinity() == Affinity.COMMON && source.intensity() == 0) return original;
            return commonTarget(original, source.shape());
        }

        Affinity wanted = affinity(cell.macroRegion());

        if (cell.influenceBand() == RegionalInfluenceBand.CARDINAL_TRANSITION) {
            if (source.affinity() == Affinity.COMMON) return original;
            if (source.affinity() == wanted && source.intensity() <= 1) return original;
            return transitionTarget(cell.macroRegion(), original, source.shape());
        }

        if (cell.radialZone() == Region.HEARTHLANDS) {
            if (source.affinity() == Affinity.COMMON) return original;
            if (source.affinity() == wanted && source.intensity() <= 1) return original;
            if (source.shape() == Shape.OCEAN) return hearthlandsOceanTarget(cell.macroRegion());
            return regionalTarget(cell.macroRegion(), Region.HEARTHLANDS, original, source.shape());
        }

        if (source.affinity() == wanted && source.intensity() <= maximumIntensity(cell.radialZone())) {
            return original;
        }
        if (source.shape() == Shape.OCEAN) {
            return outerOceanTarget(cell.macroRegion(), cell.radialZone());
        }
        return regionalTarget(cell.macroRegion(), cell.radialZone(), original, source.shape());
    }

    public static boolean shouldUpliftHearthlandsWater(RegionalCell cell, ResourceLocation remappedSurfaceBiome) {
        if (cell.radialZone() != Region.HEARTHLANDS) return false;
        Profile profile = PROFILES.get(remappedSurfaceBiome);
        if (profile == null) return false;
        return profile.shape() != Shape.OCEAN
                && profile.shape() != Shape.RIVER
                && profile.shape() != Shape.WETLAND
                && profile.shape() != Shape.COAST
                && profile.shape() != Shape.SPECIAL;
    }

    private static int maximumIntensity(Region region) {
        return switch (region) {
            case HEARTHLANDS -> 1;
            case FRONTIER -> 2;
            case WILDLANDS, DREAD_REACHES -> 3;
        };
    }

    private static Affinity affinity(MacroRegion region) {
        return switch (region) {
            case NORTH -> Affinity.FROSTMARCH;
            case EAST -> Affinity.GREENVEIL;
            case SOUTH -> Affinity.SUNSCAR;
            case WEST -> Affinity.AMBERWOOD;
        };
    }

    private static ResourceLocation commonTarget(ResourceLocation original, Shape shape) {
        return switch (shape) {
            case OPEN, ARID -> pick(original,
                    "minecraft:plains", "minecraft:meadow", "biomesoplenty:grassland", "biomesoplenty:prairie");
            case FOREST -> pick(original,
                    "minecraft:forest", "minecraft:birch_forest", "biomesoplenty:woodland", "biomesoplenty:aspen_glade");
            case WETLAND -> pick(original,
                    "minecraft:forest", "biomesoplenty:forested_field", "minecraft:river");
            case MOUNTAIN -> pick(original,
                    "minecraft:meadow", "minecraft:windswept_hills", "biomesoplenty:highland");
            case COAST -> id("minecraft:beach");
            case OCEAN -> pick(original,
                    "minecraft:plains", "biomesoplenty:grassland");
            case RIVER -> id("minecraft:river");
            case SPECIAL -> pick(original,
                    "minecraft:flower_forest", "biomesoplenty:woodland");
        };
    }

    private static ResourceLocation transitionTarget(MacroRegion region, ResourceLocation original, Shape shape) {
        return switch (region) {
            case NORTH -> switch (shape) {
                case FOREST, SPECIAL -> pick(original, "minecraft:taiga", "biomesoplenty:coniferous_forest", "biomesoplenty:fir_clearing");
                case WETLAND -> pick(original, "biomesoplenty:bog", "minecraft:taiga");
                case MOUNTAIN -> pick(original, "minecraft:meadow", "biomesoplenty:highland");
                case COAST -> id("minecraft:beach");
                default -> pick(original, "biomesoplenty:field", "minecraft:meadow", "minecraft:taiga");
            };
            case EAST -> switch (shape) {
                case FOREST, SPECIAL -> pick(original, "biomesoplenty:jacaranda_glade", "biomesoplenty:woodland", "minecraft:forest");
                case WETLAND -> pick(original, "biomesoplenty:marsh", "minecraft:swamp");
                case MOUNTAIN -> pick(original, "biomesoplenty:jade_cliffs", "minecraft:windswept_forest");
                case COAST -> id("minecraft:beach");
                default -> pick(original, "biomesoplenty:overgrown_greens", "biomesoplenty:forested_field", "minecraft:plains");
            };
            case SOUTH -> switch (shape) {
                case FOREST, SPECIAL -> pick(original, "biomesoplenty:mediterranean_forest", "minecraft:forest");
                case WETLAND -> pick(original, "biomesoplenty:lush_savanna", "minecraft:river");
                case MOUNTAIN -> pick(original, "minecraft:savanna_plateau", "biomesoplenty:highland");
                case COAST -> pick(original, "minecraft:beach", "biomesoplenty:dune_beach");
                default -> pick(original, "minecraft:savanna", "biomesoplenty:lush_savanna", "biomesoplenty:prairie");
            };
            case WEST -> switch (shape) {
                case FOREST, SPECIAL -> pick(original, "biomesoplenty:seasonal_forest", "biomesoplenty:maple_woods", "biomesoplenty:woodland");
                case WETLAND -> pick(original, "biomesoplenty:forested_field", "minecraft:forest");
                case MOUNTAIN -> pick(original, "biomesoplenty:highland", "minecraft:windswept_forest");
                case COAST -> id("minecraft:beach");
                default -> pick(original, "biomesoplenty:orchard", "biomesoplenty:prairie", "minecraft:plains");
            };
        };
    }

    private static ResourceLocation regionalTarget(MacroRegion region, Region radial, ResourceLocation original, Shape shape) {
        int stage = radial == Region.HEARTHLANDS ? 1 : radial == Region.FRONTIER ? 2 : radial == Region.WILDLANDS ? 3 : 4;
        return switch (region) {
            case NORTH -> frostTarget(stage, original, shape);
            case EAST -> greenveilTarget(stage, original, shape);
            case SOUTH -> sunscarTarget(stage, original, shape);
            case WEST -> amberwoodTarget(stage, original, shape);
        };
    }

    private static ResourceLocation frostTarget(int stage, ResourceLocation original, Shape shape) {
        if (stage <= 1) return transitionTarget(MacroRegion.NORTH, original, shape);
        if (stage == 2) {
            return switch (shape) {
                case FOREST, SPECIAL -> pick(original, "minecraft:taiga", "biomesoplenty:coniferous_forest", "minecraft:snowy_taiga");
                case WETLAND -> pick(original, "biomesoplenty:bog", "biomesoplenty:muskeg");
                case MOUNTAIN -> pick(original, "minecraft:snowy_slopes", "minecraft:jagged_peaks", "biomesoplenty:highland");
                case ARID, OPEN -> pick(original, "biomesoplenty:tundra", "minecraft:snowy_plains", "biomesoplenty:field");
                case COAST -> id("minecraft:snowy_beach");
                default -> id("minecraft:taiga");
            };
        }
        if (stage == 3) {
            return switch (shape) {
                case FOREST, SPECIAL -> pick(original, "minecraft:snowy_taiga", "biomesoplenty:snowy_coniferous_forest", "biomesoplenty:snowy_fir_clearing");
                case WETLAND -> pick(original, "biomesoplenty:muskeg", "minecraft:frozen_river");
                case MOUNTAIN -> pick(original, "minecraft:frozen_peaks", "minecraft:jagged_peaks", "minecraft:snowy_slopes");
                case ARID -> pick(original, "biomesoplenty:cold_desert", "biomesoplenty:tundra");
                case OPEN -> pick(original, "minecraft:snowy_plains", "biomesoplenty:tundra", "minecraft:ice_spikes");
                case COAST -> id("minecraft:snowy_beach");
                default -> id("minecraft:snowy_taiga");
            };
        }
        return switch (shape) {
            case FOREST -> pick(original, "biomesoplenty:snowy_coniferous_forest", "minecraft:snowy_taiga");
            case WETLAND -> pick(original, "biomesoplenty:muskeg", "minecraft:frozen_river");
            case MOUNTAIN, SPECIAL -> pick(original, "minecraft:frozen_peaks", "biomesoplenty:auroral_garden", "biomesoplenty:wintry_origin_valley");
            case ARID -> id("biomesoplenty:cold_desert");
            case OPEN -> pick(original, "minecraft:ice_spikes", "minecraft:snowy_plains", "biomesoplenty:tundra");
            case COAST -> id("minecraft:snowy_beach");
            default -> id("minecraft:snowy_taiga");
        };
    }

    private static ResourceLocation greenveilTarget(int stage, ResourceLocation original, Shape shape) {
        if (stage <= 1) return transitionTarget(MacroRegion.EAST, original, shape);
        if (stage == 2) {
            return switch (shape) {
                case FOREST, SPECIAL -> pick(original, "minecraft:sparse_jungle", "biomesoplenty:jacaranda_glade", "biomesoplenty:rainforest");
                case WETLAND -> pick(original, "biomesoplenty:marsh", "biomesoplenty:floodplain", "minecraft:swamp");
                case MOUNTAIN -> pick(original, "biomesoplenty:jade_cliffs", "biomesoplenty:rocky_rainforest");
                case ARID, OPEN -> pick(original, "biomesoplenty:overgrown_greens", "biomesoplenty:grassland", "minecraft:sparse_jungle");
                case COAST -> id("minecraft:beach");
                default -> id("minecraft:sparse_jungle");
            };
        }
        if (stage == 3) {
            return switch (shape) {
                case FOREST, SPECIAL -> pick(original, "minecraft:jungle", "biomesoplenty:rainforest", "biomesoplenty:rocky_rainforest");
                case WETLAND -> pick(original, "minecraft:mangrove_swamp", "biomesoplenty:bayou", "biomesoplenty:wetland");
                case MOUNTAIN -> pick(original, "biomesoplenty:rocky_rainforest", "biomesoplenty:jade_cliffs");
                case ARID, OPEN -> pick(original, "biomesoplenty:overgrown_greens", "biomesoplenty:rainforest", "minecraft:sparse_jungle");
                case COAST -> id("minecraft:beach");
                default -> id("minecraft:jungle");
            };
        }
        return switch (shape) {
            case WETLAND -> pick(original, "minecraft:mangrove_swamp", "biomesoplenty:bayou", "biomesoplenty:wetland");
            case MOUNTAIN -> pick(original, "biomesoplenty:rocky_rainforest", "biomesoplenty:jade_cliffs");
            case OPEN -> pick(original, "biomesoplenty:overgrown_greens", "biomesoplenty:tropics");
            case COAST -> id("minecraft:beach");
            default -> pick(original, "minecraft:bamboo_jungle", "biomesoplenty:fungal_jungle", "biomesoplenty:tropics", "minecraft:jungle");
        };
    }

    private static ResourceLocation sunscarTarget(int stage, ResourceLocation original, Shape shape) {
        if (stage <= 1) return transitionTarget(MacroRegion.SOUTH, original, shape);
        if (stage == 2) {
            return switch (shape) {
                case FOREST, SPECIAL -> pick(original, "biomesoplenty:mediterranean_forest", "minecraft:savanna");
                case WETLAND -> pick(original, "biomesoplenty:lush_savanna", "biomesoplenty:floodplain");
                case MOUNTAIN -> pick(original, "minecraft:savanna_plateau", "minecraft:windswept_savanna", "minecraft:badlands");
                case ARID -> pick(original, "biomesoplenty:dryland", "biomesoplenty:lush_desert", "minecraft:desert");
                case COAST -> pick(original, "biomesoplenty:dune_beach", "minecraft:beach");
                default -> pick(original, "minecraft:savanna", "biomesoplenty:lush_savanna", "biomesoplenty:scrubland");
            };
        }
        if (stage == 3) {
            return switch (shape) {
                case FOREST, SPECIAL -> pick(original, "minecraft:wooded_badlands", "biomesoplenty:mediterranean_forest");
                case WETLAND -> pick(original, "biomesoplenty:lush_desert", "biomesoplenty:dryland");
                case MOUNTAIN -> pick(original, "minecraft:badlands", "minecraft:eroded_badlands", "biomesoplenty:volcano");
                case ARID, OPEN -> pick(original, "minecraft:desert", "biomesoplenty:dryland", "minecraft:badlands");
                case COAST -> id("biomesoplenty:dune_beach");
                default -> id("minecraft:desert");
            };
        }
        return switch (shape) {
            case FOREST -> pick(original, "minecraft:wooded_badlands", "biomesoplenty:wasteland_steppe");
            case WETLAND -> pick(original, "biomesoplenty:dryland", "minecraft:desert");
            case MOUNTAIN, SPECIAL -> pick(original, "minecraft:eroded_badlands", "biomesoplenty:volcano", "minecraft:badlands");
            case ARID -> pick(original, "biomesoplenty:wasteland", "biomesoplenty:dryland", "minecraft:desert");
            case OPEN -> pick(original, "biomesoplenty:volcanic_plains", "biomesoplenty:wasteland_steppe", "minecraft:desert");
            case COAST -> id("biomesoplenty:dune_beach");
            default -> id("minecraft:desert");
        };
    }

    private static ResourceLocation amberwoodTarget(int stage, ResourceLocation original, Shape shape) {
        if (stage <= 1) return transitionTarget(MacroRegion.WEST, original, shape);
        if (stage == 2) {
            return switch (shape) {
                case FOREST, SPECIAL -> pick(original, "biomesoplenty:seasonal_forest", "biomesoplenty:maple_woods", "biomesoplenty:redwood_forest");
                case WETLAND -> pick(original, "biomesoplenty:forested_field", "biomesoplenty:woodland");
                case MOUNTAIN -> pick(original, "biomesoplenty:highland", "minecraft:windswept_forest");
                case ARID, OPEN -> pick(original, "biomesoplenty:pumpkin_patch", "biomesoplenty:orchard", "biomesoplenty:prairie");
                case COAST -> id("minecraft:beach");
                default -> id("biomesoplenty:seasonal_forest");
            };
        }
        if (stage == 3) {
            return switch (shape) {
                case FOREST, SPECIAL -> pick(original, "biomesoplenty:redwood_forest", "biomesoplenty:old_growth_woodland", "minecraft:dark_forest");
                case WETLAND -> pick(original, "biomesoplenty:ominous_woods", "biomesoplenty:old_growth_woodland");
                case MOUNTAIN -> pick(original, "biomesoplenty:redwood_forest", "biomesoplenty:highland");
                case ARID, OPEN -> pick(original, "biomesoplenty:pumpkin_patch", "biomesoplenty:seasonal_forest");
                case COAST -> id("minecraft:beach");
                default -> id("biomesoplenty:redwood_forest");
            };
        }
        return switch (shape) {
            case WETLAND -> pick(original, "biomesoplenty:ominous_woods", "biomesoplenty:old_growth_dead_forest");
            case MOUNTAIN -> pick(original, "biomesoplenty:redwood_forest", "biomesoplenty:old_growth_woodland");
            case ARID, OPEN -> pick(original, "biomesoplenty:pumpkin_patch", "biomesoplenty:dead_forest", "biomesoplenty:seasonal_forest");
            case COAST -> id("minecraft:beach");
            default -> pick(original, "biomesoplenty:ominous_woods", "biomesoplenty:old_growth_dead_forest", "biomesoplenty:redwood_forest");
        };
    }

    private static ResourceLocation hearthlandsOceanTarget(MacroRegion region) {
        return switch (region) {
            case NORTH -> id("minecraft:cold_ocean");
            case EAST -> id("minecraft:lukewarm_ocean");
            case SOUTH -> id("minecraft:lukewarm_ocean");
            case WEST -> id("minecraft:ocean");
        };
    }

    private static ResourceLocation outerOceanTarget(MacroRegion region, Region radial) {
        return switch (region) {
            case NORTH -> radial.atLeast(Region.WILDLANDS) ? id("minecraft:frozen_ocean") : id("minecraft:cold_ocean");
            case EAST -> radial.atLeast(Region.WILDLANDS) ? id("minecraft:warm_ocean") : id("minecraft:lukewarm_ocean");
            case SOUTH -> id("minecraft:warm_ocean");
            case WEST -> id("minecraft:ocean");
        };
    }

    private static boolean keepHearthlandsOcean(RegionalCell cell, long seed, int x, int z) {
        double d = cell.distanceFromSpawn();
        if (d < 800.0D) return false;

        double field = 0.68D * RegionalNoise.sample(seed ^ 0x2CE16A3B5DL, x, z, 900.0D)
                + 0.32D * RegionalNoise.sample(seed ^ 0x6A09E667F3L, x, z, 1800.0D);

        if (d < 1200.0D) return field > 0.62D;

        double t = Math.max(0.0D, Math.min(1.0D, (d - 1200.0D) / 1300.0D));
        double threshold = 0.55D + (0.22D - 0.55D) * t;
        return field > threshold;
    }

    private static ResourceLocation pick(ResourceLocation original, String... candidates) {
        int hash = original.toString().hashCode();
        int index = Math.floorMod(hash, candidates.length);
        return id(candidates[index]);
    }

    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }

    public static Map<ResourceLocation, Profile> profiles() {
        return Map.copyOf(PROFILES);
    }
}
