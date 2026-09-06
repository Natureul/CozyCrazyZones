package com.natureul.cozycrazyzones;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.maps.MapDecoration;

/** Semantic categories for named places recorded on the player's Atlas. */
public enum DiscoveryCategory {
    VILLAGE("Settlement", MapDecoration.Type.PLAINS_VILLAGE),
    DUNGEON("Dungeon", MapDecoration.Type.RED_X),
    TEMPLE("Temple", MapDecoration.Type.JUNGLE_TEMPLE),
    SHRINE("Shrine", MapDecoration.Type.BANNER_LIGHT_BLUE),
    RUIN("Ruin", MapDecoration.Type.TARGET_X),
    TOWER("Tower", MapDecoration.Type.MANSION),
    FORTRESS("Fortress", MapDecoration.Type.BANNER_BLACK),
    CAMP("Camp", MapDecoration.Type.BANNER_ORANGE),
    MINE("Mine", MapDecoration.Type.BANNER_GRAY),
    SHIP("Ship", MapDecoration.Type.BANNER_CYAN),
    HOUSE("Homestead", MapDecoration.Type.BANNER_YELLOW),
    PORTAL("Portal", MapDecoration.Type.BANNER_PURPLE),
    BOSS("Boss Site", MapDecoration.Type.BANNER_RED),
    LANDMARK("Landmark", MapDecoration.Type.BLUE_MARKER);

    private static final ResourceLocation FALLBACK_PIN = new ResourceLocation("map_atlases", "pin");

    private final String displayName;
    private final MapDecoration.Type defaultDecorationType;

    DiscoveryCategory(String displayName, MapDecoration.Type defaultDecorationType) {
        this.displayName = displayName;
        this.defaultDecorationType = defaultDecorationType;
    }

    public String displayName() {
        return displayName;
    }

    public MapDecoration.Type defaultDecorationType() {
        return defaultDecorationType;
    }

    /** Kept for Map Atlases/Moonlight compatibility fallbacks. */
    public ResourceLocation atlasMarkerId() {
        return FALLBACK_PIN;
    }
}
