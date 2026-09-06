package com.natureul.cozycrazyzones;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;

/** Deterministic naming language for discovered destinations. */
public final class StructurePlaceNameGenerator {
    private static final ResourceLocation TUNNEL_GORE_LAIR = new ResourceLocation("skarrier_mobs", "tunnel_gore_lair_x");

    private static final String[] NORTH_ROOTS = {"Pine","Fir","Frost","Winter","Snow","Rime","Silver","White","Cold","Ice","Wolf","Spruce","Glacier","North","Moonfrost","Bluepine","Snowcap","Everfrost"};
    private static final String[] EAST_ROOTS = {"Fern","Moss","Vine","Willow","Rain","Jade","Orchid","Bramble","Lotus","Cypress","Verdant","Canopy","Reed","Palm","Mangrove","Ivy","Riverfern","Rainleaf"};
    private static final String[] SOUTH_ROOTS = {"Sun","Saffron","Gold","Red","Dust","Cinder","Ember","Copper","Mesa","Dawn","Ochre","Sand","Sol","Bright","Dunefire","Sunstone","Gilded","Warm"};
    private static final String[] WEST_ROOTS = {"Pumpkin","Maple","Cider","Amber","Russet","Harvest","Autumn","Orchard","Apple","Acorn","Chestnut","Copperleaf","Oak","Cranberry","Bonfire","Goldenleaf","Redmaple","Hearthwood"};

    private static final String[] NORTH_ADJECTIVES = {"Frostbound","Rimebound","Snowbound","Silver","Silent","Winter","Icebound","White","Cold","Pale","Moonlit","Northwind"};
    private static final String[] EAST_ADJECTIVES = {"Mossbound","Verdant","Rainwashed","Jade","Overgrown","Fernbound","Emerald","Vinebound","Green","Canopied","Blooming","Riverworn"};
    private static final String[] SOUTH_ADJECTIVES = {"Sunscorched","Gilded","Cinder","Saffron","Dustbound","Ember","Ochre","Redstone","Golden","Warm","Dawnlit","Sandworn"};
    private static final String[] WEST_ADJECTIVES = {"Amber","Russet","Harvest","Autumnal","Cider","Maple","Lanternlit","Copperleaf","Goldenleaf","Orchard","Bonfire","Redleaf"};

    // Tunnel Gore deliberately masquerades as an unusually profitable underground find. These names
    // describe what the player can actually observe -- rich, strangely regular tunnels -- without
    // spoiling the creature, its lair, or the encounter's greed-versus-danger mechanic.
    private static final String[] TUNNEL_NOUNS = {"Vein","Seam","Galleries","Cut","Drift","Oreway","Passage","Diggings","Deepworks","Run"};
    private static final String[] NEUTRAL_TUNNEL_ROOTS = {"Stone","Grey","Deep","Slate","Iron","Silver","Old","Low","Under","Hollowstone","Deepstone","Blackstone"};
    private static final String[] NEUTRAL_TUNNEL_ADJECTIVES = {"Deep","Slate","Stonebound","Grey","Old","Lower","Quiet","Long","Understone","Darkstone"};

    private StructurePlaceNameGenerator() {}

    public static String candidate(StructureDiscoveryProfile profile,
                                   RegionalCell cell,
                                   long worldSeed,
                                   ResourceLocation structureId,
                                   ChunkPos start,
                                   int attempt) {
        if (TUNNEL_GORE_LAIR.equals(structureId)) {
            return tunnelGoreCandidate(cell, worldSeed, structureId, start, attempt);
        }

        if (HearthlandsNeutralNames.shouldUseNeutralName(cell)) {
            return HearthlandsNeutralNames.candidateFor(profile, worldSeed, structureId, start, attempt);
        }

        MacroRegion region = cell.macroRegion();
        long salt = 0x9E3779B97F4A7C15L * (profile.category().ordinal() + 1L);
        long mixed = mix64(worldSeed ^ start.toLong() ^ structureId.hashCode() ^ salt ^ (attempt * 0xD1B54A32D192ED03L));

        String[] roots = roots(region);
        String[] adjectives = adjectives(region);
        String[] nouns = nouns(profile.category());
        String root = roots[Math.floorMod((int) mixed, roots.length)];
        String adjective = adjectives[Math.floorMod((int) (mixed >>> 19), adjectives.length)];
        String noun = nouns[Math.floorMod((int) (mixed >>> 37), nouns.length)];

        return switch (Math.floorMod(attempt + (int) mixed, 4)) {
            case 0 -> "The " + adjective + " " + noun;
            case 1 -> merge(root, noun);
            case 2 -> root + " " + noun;
            default -> "The " + root + " " + noun;
        };
    }

    private static String tunnelGoreCandidate(RegionalCell cell,
                                              long worldSeed,
                                              ResourceLocation structureId,
                                              ChunkPos start,
                                              int attempt) {
        long mixed = mix64(worldSeed ^ start.toLong() ^ structureId.hashCode()
                ^ 0x6A09E667F3BCC909L ^ (attempt * 0xD1B54A32D192ED03L));

        String[] roots;
        String[] adjectives;
        if (HearthlandsNeutralNames.shouldUseNeutralName(cell)) {
            roots = NEUTRAL_TUNNEL_ROOTS;
            adjectives = NEUTRAL_TUNNEL_ADJECTIVES;
        } else {
            roots = roots(cell.macroRegion());
            adjectives = adjectives(cell.macroRegion());
        }

        String root = roots[Math.floorMod((int) mixed, roots.length)];
        String adjective = adjectives[Math.floorMod((int) (mixed >>> 19), adjectives.length)];
        String noun = TUNNEL_NOUNS[Math.floorMod((int) (mixed >>> 37), TUNNEL_NOUNS.length)];

        return switch (Math.floorMod(attempt + (int) mixed, 5)) {
            case 0 -> root + " " + noun;
            case 1 -> "The " + adjective + " " + noun;
            case 2 -> merge(root, noun);
            case 3 -> "The " + root + " " + noun;
            default -> adjective + " " + noun;
        };
    }

    public static boolean isTunnelGoreStructure(ResourceLocation structureId) {
        return TUNNEL_GORE_LAIR.equals(structureId);
    }

    public static boolean looksLikeLegacyTunnelGoreName(String name) {
        if (name == null || name.isBlank()) return false;
        String lower = name.toLowerCase(java.util.Locale.ROOT);
        return lower.contains("tunnel gore")
                || lower.contains("lair")
                || lower.contains("throne")
                || lower.contains("sepulcher")
                || lower.contains("maw")
                || lower.contains("sanctum")
                || lower.endsWith(" court")
                || lower.endsWith(" hollow");
    }

    private static String[] nouns(DiscoveryCategory category) {
        return switch (category) {
            case VILLAGE -> new String[]{"Village"};
            case DUNGEON -> new String[]{"Crypt","Vault","Delve","Catacombs","Depths","Warrens","Hollow","Dungeon"};
            case TEMPLE -> new String[]{"Temple","Sanctum","Reliquary","Hall","Shrine","Court"};
            case SHRINE -> new String[]{"Shrine","Altar","Reliquary","Sanctum","Chapel","Circle"};
            case RUIN -> new String[]{"Ruins","Remnant","Broken Hall","Fallen Court","Old Stones","Wreck"};
            case TOWER -> new String[]{"Watch","Spire","Tower","Beacon","Highwatch","Lookout"};
            case FORTRESS -> new String[]{"Keep","Hold","Citadel","Bastion","Castle","Fort"};
            case CAMP -> new String[]{"Camp","Rest","Encampment","Lodge","Crossing","Waypost"};
            case MINE -> new String[]{"Mine","Delve","Shaft","Works","Pit","Deepworks"};
            case SHIP -> new String[]{"Wake","Wreck","Corsair","Galleon","Mariner","Voyager"};
            case HOUSE -> new String[]{"Lodge","Hall","House","Homestead","Cottage","Hearth"};
            case PORTAL -> new String[]{"Gate","Rift","Arch","Crossing","Threshold","Passage"};
            case BOSS -> new String[]{"Lair","Throne","Court","Sepulcher","Maw","Sanctum","Hollow"};
            case LANDMARK -> new String[]{"Reach","Crown","Rise","Gate","Beacon","Hollow","Rest"};
        };
    }

    private static String[] roots(MacroRegion region) {
        return switch (region) { case NORTH -> NORTH_ROOTS; case EAST -> EAST_ROOTS; case SOUTH -> SOUTH_ROOTS; case WEST -> WEST_ROOTS; };
    }

    private static String[] adjectives(MacroRegion region) {
        return switch (region) { case NORTH -> NORTH_ADJECTIVES; case EAST -> EAST_ADJECTIVES; case SOUTH -> SOUTH_ADJECTIVES; case WEST -> WEST_ADJECTIVES; };
    }

    private static String merge(String root, String noun) {
        if (noun.contains(" ")) return root + " " + noun;
        return root.length() <= 7 ? root + noun.toLowerCase() : root + " " + noun;
    }

    private static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }
}
