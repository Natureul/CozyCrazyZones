package com.natureul.cozycrazyzones;

import net.minecraft.world.level.ChunkPos;

/** Regional village-name generator used by the persistent world name ledger. */
public final class HearthVillageNames {
    private static final String[] FROSTMARCH = {
            "Pinewatch", "Firhaven", "Northmere", "Snowmelt", "Whitepine", "Coldwater", "Frostford", "Winterbrook",
            "Icemere", "Pinecross", "Snowberry", "Frosthollow", "Northwatch", "Firgrove", "Wintermere", "Coldharbor",
            "Snowford", "Whitebrook", "Pinehaven", "Frostvale", "Silverpine", "Northstead", "Icebrook", "Firwatch"
    };
    private static final String[] GREENVEIL = {
            "Fernhollow", "Mossvale", "Vinecross", "Willowmere", "Rainford", "Greenwater", "Jacaranda Rest", "Bramblewick",
            "Jadegrove", "Orchid Hollow", "Fernmere", "Mossbrook", "Rainvale", "Willowcross", "Vinehaven", "Reedwater",
            "Greenhollow", "Jacaranda Vale", "Brambleford", "Jadecross", "Orchid Rest", "Fernwatch", "Mossmere", "Rainwick"
    };
    private static final String[] SUNSCAR = {
            "Sunwell", "Saffron Rest", "Goldmesa", "Redstone", "Dustmere", "Warmwater", "Sundown", "Cinderford",
            "Emberwell", "Copper Rest", "Suncrest", "Dustford", "Goldwater", "Saffron Vale", "Redmesa", "Cinderwell",
            "Warmstone", "Sunford", "Embercrest", "Coppermere", "Dustwatch", "Goldrest", "Sundrop", "Redvale"
    };
    private static final String[] HARVESTWOOD = {
            "Pumpkin Hollow", "Mapleford", "Ciderbrook", "Ambervale", "Russet Hollow", "Harveston", "Autumnmere", "Orchard Rest",
            "Applewick", "Maple Hollow", "Acornford", "Cider Vale", "Amberbrook", "Orchardmere", "Harvest Hollow", "Russetford",
            "Autumn Rest", "Pumpkinford", "Applebrook", "Maplewick", "Acorn Hollow", "Ciderrest", "Ambermere", "Orchard Vale"
    };

    private static final String[] FROSTMARCH_ROOTS = {
            "Pine", "Fir", "North", "Snow", "White", "Cold", "Frost", "Winter", "Ice", "Silver", "Wolf", "Spruce",
            "Glacier", "Rime", "Tundra", "Bluepine", "Everfrost", "Snowcap", "Hearthpine", "Moonfrost"
    };
    private static final String[] GREENVEIL_ROOTS = {
            "Fern", "Moss", "Vine", "Willow", "Rain", "Green", "Jacaranda", "Bramble", "Jade", "Orchid", "Reed", "Ivy",
            "Lotus", "Canopy", "Cypress", "Mangrove", "Verdant", "Riverfern", "Palm", "Rainleaf"
    };
    private static final String[] SUNSCAR_ROOTS = {
            "Sun", "Saffron", "Gold", "Red", "Dust", "Warm", "Cinder", "Ember", "Copper", "Mesa", "Dawn", "Ochre",
            "Sand", "Sol", "Amber", "Heat", "Bright", "Dunefire", "Golden", "Sunstone"
    };
    private static final String[] HARVESTWOOD_ROOTS = {
            "Pumpkin", "Maple", "Cider", "Amber", "Russet", "Harvest", "Autumn", "Orchard", "Apple", "Acorn", "Chestnut", "Copperleaf",
            "Oak", "Cranberry", "Hay", "Bonfire", "Goldenleaf", "Redmaple", "Corn", "Hearthwood"
    };
    private static final String[] SUFFIXES = {
            "watch", "haven", "mere", "ford", "brook", "vale", "wick", "stead", "grove", "water", "crest", "field",
            "wood", "reach", "fall", "gate", "hill", " Hollow", " Rest", " Crossing", " Run", " Hearth", " Mill", " Bridge"
    };

    private HearthVillageNames() {}

    /** Stable first-choice name; retained for callers that do not need collision probing. */
    public static String nameFor(MacroRegion region, long worldSeed, ChunkPos target) {
        return candidateFor(region, worldSeed, target, 0);
    }

    /**
     * Produces deterministic alternatives for the same village. VillageNameSavedData walks these
     * candidates until it finds one unused in the world, so names stay pleasant and truly unique.
     */
    public static String candidateFor(MacroRegion region, long worldSeed, ChunkPos target, int attempt) {
        String[] curated = curated(region);
        long regionSalt = 0x9E3779B97F4A7C15L * (region.ordinal() + 1L);
        long base = mix64(worldSeed ^ target.toLong() ^ regionSalt);

        if (attempt < curated.length) {
            int index = Math.floorMod((int) (base + attempt * 0x632BE59BD9B4E019L), curated.length);
            return curated[index];
        }

        String[] roots = roots(region);
        long mixed = mix64(base ^ (0xD1B54A32D192ED03L * (attempt + 1L)));
        int rootIndex = Math.floorMod((int) mixed, roots.length);
        int suffixIndex = Math.floorMod((int) (mixed >>> 32), SUFFIXES.length);
        return roots[rootIndex] + SUFFIXES[suffixIndex];
    }

    private static String[] curated(MacroRegion region) {
        return switch (region) {
            case NORTH -> FROSTMARCH;
            case EAST -> GREENVEIL;
            case SOUTH -> SUNSCAR;
            case WEST -> HARVESTWOOD;
        };
    }

    private static String[] roots(MacroRegion region) {
        return switch (region) {
            case NORTH -> FROSTMARCH_ROOTS;
            case EAST -> GREENVEIL_ROOTS;
            case SOUTH -> SUNSCAR_ROOTS;
            case WEST -> HARVESTWOOD_ROOTS;
        };
    }

    private static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }
}
