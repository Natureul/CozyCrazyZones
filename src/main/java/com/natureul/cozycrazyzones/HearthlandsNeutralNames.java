package com.natureul.cozycrazyzones;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;

/**
 * Shared-country naming language for the Inner Hearthlands.
 *
 * These names are intentionally non-cardinal: no frost, jungle, desert, harvest/autumn language.
 * The core should feel like ordinary settled countryside before Frostmarch/Greenveil/Sunscar/
 * Harvestwood begin asserting themselves. The curated entries preserve the neutral bank developed
 * for the pack; combinatorial fallbacks keep names unique in very structure-dense worlds.
 */
public final class HearthlandsNeutralNames {
    public static final double NEUTRAL_NAMING_RADIUS = 1000.0D;

    private static final String[] STARTER_HOMES = {
            "Oakrest House", "Willowbrook House", "Meadowvale Cottage", "Riverbend House",
            "Stonebrook Cottage", "Oak Hollow House", "Oakmere Cottage", "Oakstead House",
            "Willowrest Cottage", "Meadowbrook House", "Stonefield Cottage", "Oakcross House",
            "Riverstone Cottage", "Willowmere House", "Meadowrest Cottage", "Stonevale House"
    };

    private static final String[] GENERIC_ROOTS = {
            "Oak", "Willow", "Meadow", "River", "Stone", "Brook", "Hill", "Green", "Field", "Wood",
            "Mill", "Vale", "Hollow", "Mere", "Ford", "Cross", "Rest", "Ridge", "Glen", "Bridge"
    };

    private static final String[] GENERIC_ADJECTIVES = {
            "Old", "Little", "Lower", "Upper", "Quiet", "Stone", "Oak", "Willow", "Meadow", "Riverside",
            "Village", "Wayside", "Common", "Green", "Hilltop", "Brookside"
    };

    private static final String[] TOWERS = {
            "Oak Watch", "Stone Watchtower", "The Old Lookout", "Willow Watch", "Meadow Watch",
            "River Watch", "Oakridge Tower", "Stonebrook Watch", "The Village Lookout", "Hill Watch"
    };
    private static final String[] FORTRESSES = {
            "Oak Keep", "Stone Keep", "The Village Gatehouse", "Riverbend Keep", "Willow Keep",
            "Meadow Keep", "Stonebrook Fort", "Oakridge Hold", "The Old Gatehouse", "Hill Keep"
    };
    private static final String[] HOUSES = {
            "The Old Cabin", "Willowbrook House", "Meadowvale Cottage", "Riverbend House", "Stonebrook Cottage",
            "Oak Hollow House", "Oakrest Lodge", "The Wayside Cottage", "The Old House", "Meadowbrook Lodge"
    };
    private static final String[] MINES = {
            "The Old Mine", "Oakfield Mine", "Stonebrook Mine", "Willow Mine", "Meadow Shaft",
            "Riverbend Mine", "Stone Hollow Mine", "Oakridge Works", "The Old Quarry", "Hill Mine"
    };
    private static final String[] DUNGEONS = {
            "Old Crypt", "The Old Cellar", "Stonebrook Vault", "Oak Hollow Crypt", "Meadowvale Crypt",
            "The Old Barrow", "River Crypt", "Stone Vault", "Willow Hollow", "The Old Dungeon"
    };
    private static final String[] RUINS = {
            "Old Ruin", "The Old Stones", "Oakridge Ruins", "Stonebrook Ruins", "Willow Ruin",
            "Meadow Ruins", "Riverbend Ruins", "The Fallen House", "The Broken Hall", "Old Walls"
    };
    private static final String[] SHRINES = {
            "Village Chapel", "The Old Shrine", "Meadow Chapel", "Willow Chapel", "Stonebrook Chapel",
            "Oak Shrine", "Wayside Shrine", "River Chapel", "The Old Chapel", "Hill Shrine"
    };
    private static final String[] CAMPS = {
            "Wayfarer Camp", "Oakrest Camp", "Meadow Camp", "Stonebrook Camp", "Willow Rest",
            "Riverbend Camp", "Wayside Camp", "Oakfield Rest", "The Old Camp", "Hill Rest"
    };
    private static final String[] LANDMARKS = {
            "Oak Hill", "Willow Hollow", "Meadowvale", "Riverbend", "Stonebrook",
            "Oakridge", "Willowbrook", "Meadow Ridge", "Stone Hill", "River Hollow"
    };
    private static final String[] TEMPLES = {
            "The Old Hall", "Village Chapel", "Stone Hall", "Oak Hall", "Meadow Hall",
            "Willow Hall", "River Hall", "The Old Sanctuary", "Wayside Chapel", "Stonebrook Hall"
    };
    private static final String[] SHIPS = {
            "The Old Ferry", "Riverbend Ferry", "Willowbrook Ferry", "Stonebrook Ferry", "Meadow Ferry",
            "The River Boat", "Oakcross Ferry", "Wayside Ferry", "Old Riverboat", "Brook Ferry"
    };
    private static final String[] PORTALS = {
            "The Old Gate", "Stone Gate", "Oak Gate", "Willow Gate", "Meadow Gate",
            "River Gate", "Old Crossing", "Stone Crossing", "Oakcross Gate", "Wayside Gate"
    };
    private static final String[] BOSS_SITES = {
            "The Old Hollow", "Stone Hollow", "Oak Hollow", "The Old Court", "Meadow Hollow",
            "Willow Hollow", "River Hollow", "The Old Pit", "Hill Hollow", "Old Stones"
    };

    private HearthlandsNeutralNames() {}

    public static boolean shouldUseNeutralName(RegionalCell cell) {
        return cell.distanceFromSpawn() < NEUTRAL_NAMING_RADIUS;
    }

    public static String starterHomeName(long worldSeed) {
        long mixed = mix64(worldSeed ^ 0x4845415254484C4DL);
        return STARTER_HOMES[Math.floorMod((int) mixed, STARTER_HOMES.length)];
    }

    public static String candidateFor(StructureDiscoveryProfile profile,
                                      long worldSeed,
                                      ResourceLocation structureId,
                                      ChunkPos start,
                                      int attempt) {
        String[] curated = curated(profile.category());
        long salt = 0x6A09E667F3BCC909L * (profile.category().ordinal() + 1L);
        long mixed = mix64(worldSeed ^ start.toLong() ^ structureId.hashCode() ^ salt ^ (attempt * 0x9E3779B97F4A7C15L));

        if (attempt < curated.length) {
            int index = Math.floorMod((int) (mixed + attempt * 31L), curated.length);
            return curated[index];
        }

        String root = GENERIC_ROOTS[Math.floorMod((int) mixed, GENERIC_ROOTS.length)];
        String adjective = GENERIC_ADJECTIVES[Math.floorMod((int) (mixed >>> 23), GENERIC_ADJECTIVES.length)];
        String noun = neutralNoun(profile.category(), mixed >>> 41);

        return switch (Math.floorMod(attempt + (int) mixed, 4)) {
            case 0 -> root + " " + noun;
            case 1 -> "The " + adjective + " " + noun;
            case 2 -> root + neutralSuffix(profile.category());
            default -> "The " + root + " " + noun;
        };
    }

    private static String[] curated(DiscoveryCategory category) {
        return switch (category) {
            case TOWER -> TOWERS;
            case FORTRESS -> FORTRESSES;
            case HOUSE -> HOUSES;
            case MINE -> MINES;
            case DUNGEON -> DUNGEONS;
            case RUIN -> RUINS;
            case SHRINE -> SHRINES;
            case CAMP -> CAMPS;
            case LANDMARK -> LANDMARKS;
            case TEMPLE -> TEMPLES;
            case SHIP -> SHIPS;
            case PORTAL -> PORTALS;
            case BOSS -> BOSS_SITES;
            case VILLAGE -> LANDMARKS; // villages keep HearthVillageNames in normal use
        };
    }

    private static String neutralNoun(DiscoveryCategory category, long value) {
        String[] nouns = switch (category) {
            case VILLAGE -> new String[]{"Village", "Crossing", "Green"};
            case DUNGEON -> new String[]{"Crypt", "Vault", "Cellar", "Barrow", "Dungeon"};
            case TEMPLE -> new String[]{"Hall", "Chapel", "Sanctuary"};
            case SHRINE -> new String[]{"Shrine", "Chapel", "Wayside"};
            case RUIN -> new String[]{"Ruins", "Old Stones", "Broken Hall"};
            case TOWER -> new String[]{"Watch", "Tower", "Lookout"};
            case FORTRESS -> new String[]{"Keep", "Fort", "Gatehouse"};
            case CAMP -> new String[]{"Camp", "Rest", "Waypost"};
            case MINE -> new String[]{"Mine", "Works", "Quarry"};
            case SHIP -> new String[]{"Ferry", "Boat", "Landing"};
            case HOUSE -> new String[]{"House", "Cottage", "Lodge", "Cabin"};
            case PORTAL -> new String[]{"Gate", "Crossing", "Arch"};
            case BOSS -> new String[]{"Hollow", "Court", "Pit"};
            case LANDMARK -> new String[]{"Hill", "Hollow", "Ridge", "Crossing"};
        };
        return nouns[Math.floorMod((int) value, nouns.length)];
    }

    private static String neutralSuffix(DiscoveryCategory category) {
        return switch (category) {
            case VILLAGE -> "stead";
            case DUNGEON -> " Crypt";
            case TEMPLE -> " Hall";
            case SHRINE -> " Chapel";
            case RUIN -> " Ruins";
            case TOWER -> " Watch";
            case FORTRESS -> " Keep";
            case CAMP -> " Rest";
            case MINE -> " Mine";
            case SHIP -> " Ferry";
            case HOUSE -> " Cottage";
            case PORTAL -> " Gate";
            case BOSS -> " Hollow";
            case LANDMARK -> " Hill";
        };
    }

    private static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }
}
