package com.natureul.cozycrazyzones;

import net.minecraft.world.level.saveddata.maps.MapDecoration;

/**
 * Atlas visual language: semantic shapes where vanilla 1.20.1 has a good one, regional color for
 * everything that would otherwise become a grab-bag of unrelated banner colors.
 */
public final class DiscoveryMarkerStyle {
    private DiscoveryMarkerStyle() {}

    public static MapDecoration.Type iconFor(DiscoveryCategory category,
                                             MacroRegion region,
                                             RegionalInfluenceBand influenceBand) {
        return switch (category) {
            case DUNGEON -> MapDecoration.Type.RED_X;
            case RUIN -> MapDecoration.Type.TARGET_X;
            case TOWER -> MapDecoration.Type.MANSION;
            case TEMPLE -> MapDecoration.Type.MONUMENT;
            case FORTRESS -> MapDecoration.Type.BANNER_BLACK;
            case PORTAL -> MapDecoration.Type.BANNER_PURPLE;
            case BOSS -> MapDecoration.Type.BANNER_RED;
            case LANDMARK -> MapDecoration.Type.BLUE_MARKER;
            case VILLAGE, SHRINE, CAMP, MINE, SHIP, HOUSE -> regionalBanner(region, influenceBand);
        };
    }

    /**
     * Neutral starter-country markers are white. Once cardinal ecology is established the banners
     * resolve into one consistent regional accent: Frostmarch blue, Greenveil green, Sunscar orange,
     * Harvestwood brown.
     */
    public static MapDecoration.Type regionalBanner(MacroRegion region,
                                                     RegionalInfluenceBand influenceBand) {
        if (influenceBand != RegionalInfluenceBand.ESTABLISHED) {
            return MapDecoration.Type.BANNER_WHITE;
        }
        return switch (region) {
            case NORTH -> MapDecoration.Type.BANNER_LIGHT_BLUE;
            case EAST -> MapDecoration.Type.BANNER_GREEN;
            case SOUTH -> MapDecoration.Type.BANNER_ORANGE;
            case WEST -> MapDecoration.Type.BANNER_BROWN;
        };
    }
}
