package com.natureul.cozycrazyzones;

import net.minecraft.resources.ResourceLocation;

/**
 * Semantic categories for things the player can discover and record on the Atlas.
 *
 * v0.3.17 intentionally activates villages first. Keeping the map marker id here means later
 * dungeon/temple/ruin/boss icon work can be added without rewriting discovery persistence.
 */
public enum DiscoveryCategory {
    VILLAGE("Village", "map_atlases:pin"),
    DUNGEON("Dungeon", "map_atlases:pin"),
    TEMPLE("Temple", "map_atlases:pin"),
    RUIN("Ruin", "map_atlases:pin"),
    TOWER("Tower", "map_atlases:pin"),
    CAMP("Camp", "map_atlases:pin"),
    BOSS("Boss", "map_atlases:pin"),
    LANDMARK("Landmark", "map_atlases:pin");

    private final String displayName;
    private final ResourceLocation atlasMarkerId;

    DiscoveryCategory(String displayName, String atlasMarkerId) {
        this.displayName = displayName;
        this.atlasMarkerId = new ResourceLocation(atlasMarkerId);
    }

    public String displayName() {
        return displayName;
    }

    public ResourceLocation atlasMarkerId() {
        return atlasMarkerId;
    }
}
