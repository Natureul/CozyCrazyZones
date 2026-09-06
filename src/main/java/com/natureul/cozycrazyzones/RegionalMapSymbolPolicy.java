package com.natureul.cozycrazyzones;

import net.minecraft.world.level.saveddata.maps.MapDecoration;

/**
 * Visual grammar for Atlas discoveries.
 *
 * Shape communicates category when vanilla 1.20.1 provides a useful fixed icon; otherwise banner
 * color communicates cardinal region consistently: Frostmarch light blue, Greenveil green,
 * Sunscar orange, Harvestwood brown. This replaces the previous assortment of category colors.
 */
public final class RegionalMapSymbolPolicy {
    private RegionalMapSymbolPolicy() {}

    public static MapDecoration.Type iconFor(StructureDiscoveryProfile profile, MacroRegion region) {
        return switch (profile.category()) {
            case DUNGEON -> MapDecoration.Type.RED_X;
            case RUIN -> MapDecoration.Type.TARGET_X;
            case TEMPLE -> MapDecoration.Type.MONUMENT;
            case TOWER, FORTRESS -> MapDecoration.Type.MANSION;
            case BOSS -> MapDecoration.Type.BANNER_RED;
            case VILLAGE, SHRINE, CAMP, MINE, SHIP, HOUSE, PORTAL, LANDMARK -> regionalBanner(region);
        };
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
