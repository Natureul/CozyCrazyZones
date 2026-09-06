package com.natureul.cozycrazyzones;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;

/**
 * Hard geography contract for the four regional-final tier destinations.
 *
 * A minimum Dread-Reaches rule by itself is not enough: without an outer bound the first valid
 * candidate can be tens of thousands of blocks from home. Final destinations instead live in a
 * finite Dread expedition belt. This class also owns the Aquamirae Ice-Maze biome territory rule,
 * because the maze itself is feature/biome driven rather than a single Structure registry entry.
 */
public final class FinalDestinationPolicy {
    private static final ResourceLocation CURSED_PYRAMID = id("cataclysm:cursed_pyramid");

    private static final Map<ResourceLocation, MacroRegion> FINAL_STRUCTURES = Map.of(
            CURSED_PYRAMID, MacroRegion.SOUTH,
            id("aquamirae:outpost"), MacroRegion.NORTH,
            id("aquamirae:shelter"), MacroRegion.NORTH,
            id("aquamirae:ship"), MacroRegion.NORTH,
            id("aquamirae:surface/arch"), MacroRegion.NORTH,
            id("aquamirae:surface/spiral"), MacroRegion.NORTH
    );

    private FinalDestinationPolicy() {}

    public static boolean isFinalStructure(ResourceLocation structureId) {
        return FINAL_STRUCTURES.containsKey(structureId);
    }

    public static Optional<MacroRegion> expectedMacroRegion(ResourceLocation structureId) {
        return Optional.ofNullable(FINAL_STRUCTURES.get(structureId));
    }

    public static boolean allowsStructure(ResourceLocation structureId, RegionalCell cell) {
        MacroRegion expected = FINAL_STRUCTURES.get(structureId);
        return expected == null || allowsFinalLocation(expected, cell);
    }

    /** Shared geography predicate used by generation, biome territory and player-facing locators. */
    public static boolean allowsFinalLocation(MacroRegion expected, RegionalCell cell) {
        return cell.radialZone() == Region.DREAD_REACHES
                && cell.influenceBand() == RegionalInfluenceBand.ESTABLISHED
                && cell.macroRegion() == expected
                && cell.distanceFromSpawn() <= CozyZonesConfig.effectiveFinalDestinationMaxRadius();
    }

    /**
     * Aquamirae 6.4.0 uses its #aquamirae:ice_maze biome tag to drive the maze features. In the
     * installed pack that tag resolves to frozen_ocean/deep_frozen_ocean, so those biomes are only
     * permitted to become Maze territory inside the finite northern final-destination belt.
     */
    public static boolean iceMazeTerritory(RegionalCell cell) {
        return allowsFinalLocation(MacroRegion.NORTH, cell);
    }

    public static boolean isIceMazeOcean(ResourceLocation biomeId) {
        if (!"minecraft".equals(biomeId.getNamespace())) return false;
        String path = biomeId.getPath();
        return "frozen_ocean".equals(path) || "deep_frozen_ocean".equals(path);
    }

    /** Non-Ice-Maze ocean analogue used when a frozen-ocean answer leaks outside its legal belt. */
    public static ResourceLocation nonMazeOcean(RegionalCell cell, boolean deep) {
        return switch (cell.macroRegion()) {
            case NORTH -> id(deep ? "minecraft:deep_cold_ocean" : "minecraft:cold_ocean");
            case EAST -> id(deep ? "minecraft:deep_lukewarm_ocean" : "minecraft:lukewarm_ocean");
            case SOUTH -> id(deep ? "minecraft:deep_lukewarm_ocean" : "minecraft:warm_ocean");
            case WEST -> id(deep ? "minecraft:deep_ocean" : "minecraft:ocean");
        };
    }

    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }
}
