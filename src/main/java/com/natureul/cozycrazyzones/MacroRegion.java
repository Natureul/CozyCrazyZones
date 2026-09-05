package com.natureul.cozycrazyzones;

import net.minecraft.ChatFormatting;

/**
 * Large-scale ecological identity. Minecraft's cardinal convention is used:
 * North = -Z, East = +X, South = +Z, West = -X.
 */
public enum MacroRegion {
    NORTH("north", "North", "Northern", ChatFormatting.AQUA),
    EAST("east", "East", "Eastern", ChatFormatting.DARK_GREEN),
    SOUTH("south", "South", "Southern", ChatFormatting.GOLD),
    WEST("west", "West", "Western", ChatFormatting.DARK_RED);

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
}
