package com.natureul.cozycrazyzones;

import net.minecraft.world.level.saveddata.maps.MapDecoration;

/**
 * Visual grammar for Atlas discoveries.
 *
 * Geography color is the primary language for ordinary discoveries: Inner Hearthlands white,
 * Frostmarch light blue, Greenveil green, Sunscar orange, Harvestwood brown. Truly distinctive
 * vanilla destination icons are still used where they communicate more than a generic colored pin,
 * while explicit boss sites remain red on purpose.
 */
public final class RegionalMapSymbolPolicy {
    private RegionalMapSymbolPolicy() {}

    public static MapDecoration.Type iconFor(StructureDiscoveryProfile profile, RegionalCell cell) {
        return iconForCategory(profile.category(), cell);
    }

    public static MapDecoration.Type iconForCategory(DiscoveryCategory category, RegionalCell cell) {
        return switch (category) {
            case TEMPLE -> MapDecoration.Type.MONUMENT;
            case TOWER, FORTRESS -> MapDecoration.Type.MANSION;
            case BOSS -> MapDecoration.Type.BANNER_RED;
            case VILLAGE, DUNGEON, SHRINE, RUIN, CAMP, MINE, SHIP, HOUSE, PORTAL, LANDMARK -> bannerFor(cell);
        };
    }

    public static MapDecoration.Type bannerFor(RegionalCell cell) {
        if (HearthlandsNeutralNames.shouldUseNeutralName(cell)) return MapDecoration.Type.BANNER_WHITE;
        return regionalBanner(cell.macroRegion());
    }

    public static MapDecoration.Type regionalBanner(MacroRegion region) {
        return switch (region) {
            case NORTH -> MapDecoration.Type.BANNER_LIGHT_BLUE;
            case EAST -> MapDecoration.Type.BANNER_GREEN;
            case SOUTH -> MapDecoration.Type.BANNER_ORANGE;
            case WEST -> MapDecoration.Type.BANNER_BROWN;
        };
    }

    public static MapDecoration.Type neutralHome() {
        return MapDecoration.Type.BANNER_WHITE;
    }
}
