package com.natureul.cozycrazyzones;

import net.minecraft.world.level.ChunkPos;

/** Deterministic seed-specific names for the four authored Hearthlands village anchors. */
public final class HearthVillageNames {
    private static final String[] FROSTMARCH = {
            "Pinewatch", "Firhaven", "Northmere", "Snowmelt", "Whitepine", "Coldwater", "Frostford", "Winterbrook"
    };
    private static final String[] GREENVEIL = {
            "Fernhollow", "Mossvale", "Vinecross", "Willowmere", "Rainford", "Greenwater", "Jacaranda Rest", "Bramblewick"
    };
    private static final String[] SUNSCAR = {
            "Sunwell", "Saffron Rest", "Goldmesa", "Redstone", "Dustmere", "Warmwater", "Sundown", "Cinderford"
    };
    private static final String[] HARVESTWOOD = {
            "Pumpkin Hollow", "Mapleford", "Ciderbrook", "Ambervale", "Russet Hollow", "Harveston", "Autumnmere", "Orchard Rest"
    };

    private HearthVillageNames() {}

    public static String nameFor(MacroRegion region, long worldSeed, ChunkPos target) {
        String[] pool = switch (region) {
            case NORTH -> FROSTMARCH;
            case EAST -> GREENVEIL;
            case SOUTH -> SUNSCAR;
            case WEST -> HARVESTWOOD;
        };
        long regionSalt = 0x9E3779B97F4A7C15L * (region.ordinal() + 1L);
        long mixed = mix64(worldSeed ^ target.toLong() ^ regionSalt);
        return pool[Math.floorMod((int) mixed, pool.length)];
    }

    private static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }
}
