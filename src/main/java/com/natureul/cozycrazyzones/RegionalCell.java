package com.natureul.cozycrazyzones;

/**
 * Authoritative two-axis geography answer for future quests/maps/bounties.
 */
public record RegionalCell(
        Region radialZone,
        MacroRegion macroRegion,
        RegionalInfluenceBand influenceBand,
        double distanceFromSpawn,
        double regionalStrength,
        double macroBoundaryStrength
) {
    public boolean sharedCore() {
        return influenceBand == RegionalInfluenceBand.SHARED_CORE;
    }

    public String ecologyDisplayName() {
        if (sharedCore()) return "Shared Core";
        if (influenceBand == RegionalInfluenceBand.CARDINAL_TRANSITION) {
            return macroRegion.displayName() + " Transition";
        }
        return macroRegion.displayName();
    }

    public String cellDisplayName() {
        if (sharedCore() && radialZone == Region.HEARTHLANDS) return radialZone.displayName();
        return macroRegion.adjective() + " " + radialZone.displayName();
    }
}
