package com.natureul.cozycrazyzones;

/**
 * Neutral starter-country names used before the cardinal regions are visually established.
 *
 * These deliberately avoid Frostmarch ice language, Greenveil jungle language, Sunscar desert
 * language and Harvestwood autumn language. The starter core should sound like ordinary inhabited
 * countryside: old roads, oak woods, meadows, stonework, rivers and modest local landmarks.
 */
public final class NeutralHearthlandsNames {
    private static final String[] STARTER_HOMES = {
            "Oakrest House",
            "Fairhaven Cottage",
            "Willowmere House",
            "Meadowrest Cottage",
            "Stonebrook Lodge",
            "Riverbend House",
            "Oakstead House",
            "Bluebell Cottage"
    };

    private static final String[] DUNGEONS = {
            "Old Crypt", "The Old Cellar", "Stonebrook Crypt", "Oak Hollow Vault",
            "The Buried Hall", "Mossy Crypt", "The Old Warrens", "Greenhill Crypt",
            "The Lower Vault", "The Old Delve", "Stone Cellar", "The Forgotten Cellar"
    };
    private static final String[] TEMPLES = {
            "Village Chapel", "The Old Chapel", "Wayside Chapel", "Stonebrook Chapel",
            "Meadow Chapel", "The Quiet Hall", "Oak Shrine", "The Old Sanctuary"
    };
    private static final String[] SHRINES = {
            "Village Chapel", "Wayside Shrine", "The Old Shrine", "Meadow Shrine",
            "Stonebrook Shrine", "Oak Circle", "The Wayside Altar", "The Quiet Chapel"
    };
    private static final String[] RUINS = {
            "Old Ruin", "The Old Stones", "Mossy Ruins", "The Broken Hall",
            "Willow Ruins", "Stonebrook Ruins", "The Fallen House", "The Old Walls",
            "Oakfield Ruins", "The Forgotten Walls"
    };
    private static final String[] TOWERS = {
            "Oak Watch", "The Old Lookout", "Stone Watch", "Hilltop Tower",
            "River Watch", "Greenhill Watch", "The Old Tower", "Oakridge Lookout",
            "Meadow Watch", "Stonebrook Tower"
    };
    private static final String[] FORTRESSES = {
            "Stone Keep", "The Old Keep", "Greenhill Fort", "Oakgate Keep",
            "Stonebrook Hold", "The Old Fort", "Oakridge Keep", "Riverbend Fort"
    };
    private static final String[] CAMPS = {
            "Wayfarer Camp", "Oakcross Camp", "Riverside Camp", "Hill Camp",
            "The Old Waypost", "Meadow Camp", "Stonebrook Camp", "Oakgate Rest"
    };
    private static final String[] MINES = {
            "Oakbank Mine", "Stonebrook Mine", "The Old Quarry", "Greenhill Mine",
            "Meadow Quarry", "Riverbend Mine", "The Old Works", "Oakridge Quarry"
    };
    private static final String[] SHIPS = {
            "The Wayfarer", "Willow Ferry", "The River Trader", "Oakbrook Ferry",
            "The Old Mariner", "Fairhaven Trader", "The Meadow Star", "Stonebrook Ferry"
    };
    private static final String[] HOUSES = {
            "The Old Cottage", "Oak Hollow House", "Willowbrook Cottage", "Meadowvale House",
            "Riverbend Lodge", "Stonebrook House", "Greenhill Cottage", "Oakfield House",
            "The Old Homestead", "The Wayside House"
    };
    private static final String[] PORTALS = {
            "The Old Arch", "Stone Gate", "Oakgate Arch", "The Old Crossing",
            "The Broken Arch", "River Gate", "The Old Threshold", "Stonebrook Gate"
    };
    private static final String[] BOSS_SITES = {
            "The Old Hollow", "Stone Maw", "Barrow Hollow", "The Deep Court",
            "The Black Cellar", "The Old Barrow", "The Buried Court", "The Deep Hollow"
    };
    private static final String[] LANDMARKS = {
            "Oak Hill", "The Old Meadow", "Bluebell Meadow", "Foxglove Hill",
            "Greenhill", "Riverbend", "Oak Hollow", "Oak Vale", "Oakmere",
            "Oakbrook", "Oakfield", "Oakridge", "Willowbrook", "Meadowvale",
            "Stonebrook", "Fairhaven", "The Old Road", "Oakcross", "Oakgate"
    };
    private static final String[] VILLAGES = {
            "Oakridge", "Willowbrook", "Meadowvale", "Riverbend", "Stonebrook",
            "Greenhill", "Fairhaven", "Oakrest", "Oak Vale", "Oakstead",
            "Oakmere", "Oakford", "Oakbrook", "Oakfield", "Oakcross", "Oakgate", "Oakbank"
    };

    private NeutralHearthlandsNames() {}

    public static String candidate(DiscoveryCategory category, long mixed, int attempt) {
        String[] bank = switch (category) {
            case VILLAGE -> VILLAGES;
            case DUNGEON -> DUNGEONS;
            case TEMPLE -> TEMPLES;
            case SHRINE -> SHRINES;
            case RUIN -> RUINS;
            case TOWER -> TOWERS;
            case FORTRESS -> FORTRESSES;
            case CAMP -> CAMPS;
            case MINE -> MINES;
            case SHIP -> SHIPS;
            case HOUSE -> HOUSES;
            case PORTAL -> PORTALS;
            case BOSS -> BOSS_SITES;
            case LANDMARK -> LANDMARKS;
        };
        int index = Math.floorMod((int) (mixed + attempt * 31L), bank.length);
        return bank[index];
    }

    /** A stable, world-specific canonical name for the authored starter house. */
    public static String starterHouseName(long worldSeed) {
        long mixed = mix64(worldSeed ^ 0x48534C414E44534CL);
        return STARTER_HOMES[Math.floorMod((int) mixed, STARTER_HOMES.length)];
    }

    private static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }
}
