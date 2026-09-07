package com.natureul.cozycrazyzones;

import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

/**
 * Fallback structure classifier for modded regional variants that were not known when the exact
 * structure registry table was authored.
 *
 * TerraBlender/native biome identity can be queried by a structure before CozyCrazyZones applies its
 * final visible biome. A savanna/acacia or jungle structure can therefore be selected for terrain
 * that later appears as neutral Plains. Exact ZoneRuleRegistry entries remain authoritative; this
 * fallback classifies unmistakable regional vocabulary plus a very small set of audited hidden names.
 */
public final class RegionalStructureSemanticPolicy {
    private static final ResourceLocation DUNGEONS_ENHANCED_TREE_HOUSE =
            new ResourceLocation("dungeons_enhanced", "tree_house");

    private RegionalStructureSemanticPolicy() {}

    public static boolean allowsUnknown(ResourceLocation id, RegionalCell cell) {
        Affinity affinity = affinity(id);
        if (affinity == Affinity.NONE) return true;

        // The shared home core is deliberately neutral/common country. Strongly regional structure
        // variants never belong there even if an earlier native biome source happened to qualify.
        if (cell.influenceBand() == RegionalInfluenceBand.SHARED_CORE) return false;

        return switch (affinity) {
            case SOUTH -> cell.macroRegion() == MacroRegion.SOUTH;
            case EAST -> cell.macroRegion() == MacroRegion.EAST;
            case NORTH -> cell.macroRegion() == MacroRegion.NORTH;
            case NORTH_OR_WEST -> cell.macroRegion() == MacroRegion.NORTH || cell.macroRegion() == MacroRegion.WEST;
            case WEST -> cell.macroRegion() == MacroRegion.WEST;
            case NONE -> true;
        };
    }

    private static Affinity affinity(ResourceLocation id) {
        // Dungeons Enhanced calls its jungle tree-house simply "tree_house". The ID contains no
        // jungle token even though the authored structure is explicitly jungle content, so it needs
        // one audited semantic exception to avoid generating from a pre-remap jungle near home.
        if (DUNGEONS_ENHANCED_TREE_HOUSE.equals(id)) return Affinity.EAST;

        String path = id.getPath().toLowerCase(Locale.ROOT);
        if (containsAny(path,
                "desert", "savanna", "savannah", "acacia", "badlands", "mesa", "red_desert", "dune")) {
            return Affinity.SOUTH;
        }
        if (containsAny(path,
                "jungle", "bamboo", "mangrove", "swamp", "rainforest", "tropical", "tropics", "bayou")) {
            return Affinity.EAST;
        }
        if (containsAny(path,
                "snowy", "snow_", "_snow", "frozen", "frost", "ice_", "_ice", "glacier")) {
            return Affinity.NORTH;
        }
        if (containsAny(path, "taiga", "spruce", "conifer")) return Affinity.NORTH_OR_WEST;
        if (containsAny(path, "pumpkin", "autumn", "maple", "seasonal", "dark_oak")) return Affinity.WEST;
        return Affinity.NONE;
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) if (value.contains(needle)) return true;
        return false;
    }

    private enum Affinity {
        NONE,
        SOUTH,
        EAST,
        NORTH,
        NORTH_OR_WEST,
        WEST
    }
}
