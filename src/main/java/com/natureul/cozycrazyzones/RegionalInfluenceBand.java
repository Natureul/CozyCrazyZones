package com.natureul.cozycrazyzones;

/**
 * How strongly cardinal ecology should be expressed at a position.
 * This is deliberately independent from radial danger tier.
 */
public enum RegionalInfluenceBand {
    SHARED_CORE(0, "shared_core", "Shared Core"),
    CARDINAL_TRANSITION(1, "cardinal_transition", "Cardinal Transition"),
    ESTABLISHED(2, "established", "Established Region");

    private final int tier;
    private final String id;
    private final String displayName;

    RegionalInfluenceBand(int tier, String id, String displayName) {
        this.tier = tier;
        this.id = id;
        this.displayName = displayName;
    }

    public int tier() { return tier; }
    public String id() { return id; }
    public String displayName() { return displayName; }
    public boolean atLeast(RegionalInfluenceBand minimum) { return tier >= minimum.tier; }
}
