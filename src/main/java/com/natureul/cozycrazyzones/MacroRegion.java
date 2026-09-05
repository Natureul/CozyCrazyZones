package com.natureul.cozycrazyzones;

import net.minecraft.ChatFormatting;

/**
 * Large-scale ecological identity. Enum constants intentionally keep cardinal names because
 * coordinate math and integrations depend on stable semantic directions. Player-facing names
 * are proper region names.
 *
 * Minecraft cardinal convention:
 * NORTH = -Z, EAST = +X, SOUTH = +Z, WEST = -X.
 */
public enum MacroRegion {
    NORTH("north", "Frostmarch", "Frostmarch", ChatFormatting.AQUA),
    EAST("east", "Greenveil", "Greenveil", ChatFormatting.DARK_GREEN),
    SOUTH("south", "Sunscar", "Sunscar", ChatFormatting.GOLD),
    WEST("west", "Amberwood", "Amberwood", ChatFormatting.DARK_RED);

    private final String id;
    private final String displayName;
    private final String adjective;
    private final ChatFormatting formatting;

    MacroRegion(String id, String displayName, String adjective, ChatFormatting formatting) {
        this.id = id;
        this.displayName = displayName;
        this.adjective = adjective;
        this.formatting = formatting;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public String adjective() { return adjective; }
    public ChatFormatting formatting() { return formatting; }

    public String directionalDebugName() {
        return name().charAt(0) + name().substring(1).toLowerCase();
    }
}
