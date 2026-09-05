package com.natureul.cozycrazyzones;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum Region {
    HEARTHLANDS(0, "hearthlands", "Hearthlands", "Home country — lived-in, but never harmless.", ChatFormatting.GOLD),
    FRONTIER(1, "frontier", "The Frontier", "Beyond the familiar roads.", ChatFormatting.YELLOW),
    WILDLANDS(2, "wildlands", "Wildlands", "Prepare for an expedition, not a stroll.", ChatFormatting.DARK_GREEN),
    DREAD_REACHES(3, "dread_reaches", "Dread Reaches", "The known world has grown very far away.", ChatFormatting.DARK_RED);

    private final int tier;
    private final String id;
    private final String displayName;
    private final String subtitle;
    private final ChatFormatting formatting;

    Region(int tier, String id, String displayName, String subtitle, ChatFormatting formatting) {
        this.tier = tier;
        this.id = id;
        this.displayName = displayName;
        this.subtitle = subtitle;
        this.formatting = formatting;
    }

    public int tier() { return tier; }
    public String id() { return id; }
    public String displayName() { return displayName; }
    public String subtitle() { return subtitle; }
    public ChatFormatting formatting() { return formatting; }
    public Component titleComponent() { return Component.literal(displayName).withStyle(formatting); }
    public Component subtitleComponent() { return Component.literal(subtitle).withStyle(ChatFormatting.GRAY); }

    public boolean atLeast(Region minimum) {
        return tier >= minimum.tier;
    }

    public static Region byId(String id) {
        for (Region region : values()) if (region.id.equalsIgnoreCase(id)) return region;
        throw new IllegalArgumentException("Unknown CozyCrazyZones region: " + id);
    }
}
