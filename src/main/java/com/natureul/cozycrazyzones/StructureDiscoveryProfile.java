package com.natureul.cozycrazyzones;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecoration;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;

/**
 * Curates which generated structures deserve a named place on the Atlas.
 * Tiny treasure structures and technical worldgen helpers are intentionally excluded.
 */
public record StructureDiscoveryProfile(
        DiscoveryCategory category,
        String kind,
        MapDecoration.Type icon,
        boolean major
) {
    private static final Map<String, StructureDiscoveryProfile> EXACT = Map.ofEntries(
            Map.entry("mowziesmobs:wrought_chamber", boss("Wrought Chamber")),
            Map.entry("mowziesmobs:frostmaw_spawn", boss("Frostmaw Lair")),
            Map.entry("mowziesmobs:umvuthana_grove", boss("Umvuthana Grove")),
            Map.entry("cataclysm:cursed_pyramid", boss("Cursed Pyramid")),
            Map.entry("born_in_chaos_v1:infernal_pumpkin", boss("Infernal Pumpkin")),

            Map.entry("minecraft:woodland_mansion", new StructureDiscoveryProfile(DiscoveryCategory.FORTRESS, "Woodland Mansion", MapDecoration.Type.MANSION, true)),
            Map.entry("minecraft:ocean_monument", new StructureDiscoveryProfile(DiscoveryCategory.TEMPLE, "Ocean Monument", MapDecoration.Type.MONUMENT, true)),
            Map.entry("minecraft:jungle_pyramid", new StructureDiscoveryProfile(DiscoveryCategory.TEMPLE, "Jungle Temple", MapDecoration.Type.MONUMENT, true)),
            Map.entry("minecraft:swamp_hut", new StructureDiscoveryProfile(DiscoveryCategory.HOUSE, "Witch Hut", MapDecoration.Type.BANNER_PURPLE, false)),
            Map.entry("minecraft:ancient_city", new StructureDiscoveryProfile(DiscoveryCategory.LANDMARK, "Ancient City", MapDecoration.Type.BLUE_MARKER, true)),
            Map.entry("minecraft:stronghold", new StructureDiscoveryProfile(DiscoveryCategory.FORTRESS, "Stronghold", MapDecoration.Type.BANNER_BLACK, true)),
            Map.entry("minecraft:pillager_outpost", new StructureDiscoveryProfile(DiscoveryCategory.TOWER, "Pillager Outpost", MapDecoration.Type.MANSION, true)),
            Map.entry("minecraft:desert_pyramid", new StructureDiscoveryProfile(DiscoveryCategory.TEMPLE, "Desert Temple", MapDecoration.Type.BANNER_ORANGE, true)),
            Map.entry("minecraft:igloo", new StructureDiscoveryProfile(DiscoveryCategory.HOUSE, "Igloo", MapDecoration.Type.BANNER_LIGHT_BLUE, false)),

            Map.entry("dungeons_enhanced:castle", fortress("Castle")),
            Map.entry("dungeons_enhanced:watch_tower", tower("Watchtower")),
            Map.entry("dungeons_enhanced:witch_tower", tower("Witch Tower")),
            Map.entry("dungeons_enhanced:tower_of_the_undead", tower("Undead Tower")),
            Map.entry("dungeons_enhanced:pillager_camp", camp("Pillager Camp")),
            Map.entry("dungeons_enhanced:pirate_ship", ship("Pirate Ship")),
            Map.entry("dungeons_enhanced:flying_dutchman", ship("Ghost Ship")),
            Map.entry("dungeons_enhanced:sunken_shrine", shrine("Sunken Shrine")),
            Map.entry("dungeons_enhanced:elders_temple", temple("Elder Temple")),
            Map.entry("dungeons_enhanced:jungle_monument", new StructureDiscoveryProfile(DiscoveryCategory.TEMPLE, "Jungle Monument", MapDecoration.Type.MONUMENT, true)),
            Map.entry("dungeons_enhanced:desert_temple", new StructureDiscoveryProfile(DiscoveryCategory.TEMPLE, "Desert Temple", MapDecoration.Type.BANNER_ORANGE, true)),
            Map.entry("dungeons_enhanced:desert_tomb", dungeon("Desert Tomb", false)),
            Map.entry("dungeons_enhanced:deep_crypt", dungeon("Deep Crypt", true)),
            Map.entry("dungeons_enhanced:monster_maze", dungeon("Monster Maze", true)),
            Map.entry("dungeons_enhanced:large_dungeon", dungeon("Grand Dungeon", true)),
            Map.entry("dungeons_enhanced:ice_pit", dungeon("Ice Pit", true)),
            Map.entry("dungeons_enhanced:stables", new StructureDiscoveryProfile(DiscoveryCategory.CAMP, "Stables", MapDecoration.Type.BANNER_BROWN, false)),

            Map.entry("valhelsia_structures:castle", fortress("Castle")),
            Map.entry("valhelsia_structures:castle_ruin", ruin("Castle Ruin", true)),
            Map.entry("valhelsia_structures:tower_ruin", ruin("Tower Ruin", false)),
            Map.entry("valhelsia_structures:forge", new StructureDiscoveryProfile(DiscoveryCategory.HOUSE, "Forge", MapDecoration.Type.BANNER_YELLOW, false)),
            Map.entry("valhelsia_structures:desert_house", new StructureDiscoveryProfile(DiscoveryCategory.HOUSE, "Desert House", MapDecoration.Type.BANNER_YELLOW, false)),

            Map.entry("betterwitchhuts:witch_hut", new StructureDiscoveryProfile(DiscoveryCategory.HOUSE, "Witch Hut", MapDecoration.Type.BANNER_PURPLE, false)),
            Map.entry("betterwitchhuts:witch_circle", shrine("Witch Circle")),
            Map.entry("betterdeserttemples:desert_temple", new StructureDiscoveryProfile(DiscoveryCategory.TEMPLE, "Desert Temple", MapDecoration.Type.BANNER_ORANGE, true)),
            Map.entry("betterjungletemples:jungle_temple", temple("Jungle Temple")),

            Map.entry("mowziesmobs:monastery", fortress("Monastery")),
            Map.entry("born_in_chaos_v1:firewell", shrine("Firewell")),
            Map.entry("born_in_chaos_v1:mound_of_hounds", dungeon("Mound of Hounds", true))
    );

    private static final String[] DISCOVERABLE_NAMESPACES = {
            "minecraft", "dungeons_enhanced", "betterdungeons", "valhelsia_structures",
            "born_in_chaos_v1", "mowziesmobs", "betterwitchhuts", "betterdeserttemples",
            "betterjungletemples", "bettermineshafts", "beautify", "cataclysm"
    };

    @Nullable
    public static StructureDiscoveryProfile classify(Registry<Structure> registry,
                                                      Structure structure,
                                                      ResourceLocation id) {
        boolean village = registry.getTag(StructureTags.VILLAGE)
                .map(set -> set.stream().anyMatch(holder -> holder.value() == structure))
                .orElse(false);
        if (village || id.getPath().contains("village")) {
            return new StructureDiscoveryProfile(DiscoveryCategory.VILLAGE, "Village", villageIcon(id), true);
        }

        StructureDiscoveryProfile exact = EXACT.get(id.toString());
        if (exact != null) return exact;

        if (!namespaceDiscoverable(id.getNamespace())) return null;
        String path = id.getPath().toLowerCase(Locale.ROOT);

        if (path.contains("buried_treasure") || path.contains("fossil")) return null;

        if (path.contains("mineshaft") || path.endsWith("_mine") || path.contains("mine_")) {
            return new StructureDiscoveryProfile(DiscoveryCategory.MINE, prettifyKind(path, "Mine"), MapDecoration.Type.BANNER_GRAY, false);
        }
        if (path.contains("shipwreck") || path.contains("pirate_ship") || path.contains("dutchman") || path.endsWith("_ship")) {
            return ship(prettifyKind(path, "Ship"));
        }
        if (path.contains("ruined_portal") || path.endsWith("_portal") || path.contains("portal_")) {
            return new StructureDiscoveryProfile(DiscoveryCategory.PORTAL, prettifyKind(path, "Ruined Portal"), MapDecoration.Type.BANNER_PURPLE, false);
        }
        if (path.contains("castle_ruin") || path.contains("tower_ruin") || path.contains("trail_ruin") || path.contains("ocean_ruin") || path.contains("ruin")) {
            return ruin(prettifyKind(path, "Ruins"), path.contains("castle") || path.contains("ancient"));
        }
        if (path.contains("jungle_temple") || path.contains("jungle_pyramid")) {
            return new StructureDiscoveryProfile(DiscoveryCategory.TEMPLE, prettifyKind(path, "Jungle Temple"), MapDecoration.Type.MONUMENT, true);
        }
        if (path.contains("monument")) {
            return new StructureDiscoveryProfile(DiscoveryCategory.TEMPLE, prettifyKind(path, "Monument"), MapDecoration.Type.MONUMENT, true);
        }
        if (path.contains("temple") || path.contains("pyramid")) {
            return temple(prettifyKind(path, "Temple"));
        }
        if (path.contains("shrine") || path.contains("altar") || path.contains("circle") || path.contains("firewell")) {
            return shrine(prettifyKind(path, "Shrine"));
        }
        if (path.contains("witch_hut")) {
            return new StructureDiscoveryProfile(DiscoveryCategory.HOUSE, prettifyKind(path, "Witch Hut"), MapDecoration.Type.BANNER_PURPLE, false);
        }
        if (path.contains("dark_tower") || path.contains("watch_tower") || path.contains("observation_tower") || path.contains("tower")) {
            return tower(prettifyKind(path, "Tower"));
        }
        if (path.contains("castle") || path.contains("fortress") || path.contains("stronghold") || path.contains("monastery") || path.contains("outpost")) {
            return fortress(prettifyKind(path, "Fortress"));
        }
        if (path.contains("camp") || path.contains("caravan") || path.contains("stables")) {
            return camp(prettifyKind(path, "Camp"));
        }
        if (path.contains("house") || path.contains("forge") || path.contains("hut") || path.contains("lodge")) {
            return new StructureDiscoveryProfile(DiscoveryCategory.HOUSE, prettifyKind(path, "Homestead"), MapDecoration.Type.BANNER_YELLOW, false);
        }
        if (path.contains("dungeon") || path.contains("crypt") || path.contains("chamber") || path.contains("tomb")
                || path.contains("maze") || path.contains("pit") || path.contains("mound")) {
            return dungeon(prettifyKind(path, "Dungeon"), path.contains("large") || path.contains("deep") || path.contains("maze"));
        }
        if (path.contains("grove") || path.contains("city")) {
            return new StructureDiscoveryProfile(DiscoveryCategory.LANDMARK, prettifyKind(path, "Landmark"), MapDecoration.Type.BLUE_MARKER, true);
        }

        if (!"minecraft".equals(id.getNamespace())) {
            return new StructureDiscoveryProfile(DiscoveryCategory.LANDMARK, prettifyKind(path, "Landmark"), MapDecoration.Type.BLUE_MARKER, false);
        }
        return null;
    }

    private static boolean namespaceDiscoverable(String namespace) {
        for (String allowed : DISCOVERABLE_NAMESPACES) if (allowed.equals(namespace)) return true;
        return false;
    }

    /** 1.20.1 lacks the newer dedicated village map icons, so biome families get banner colors. */
    private static MapDecoration.Type villageIcon(ResourceLocation id) {
        String path = id.getPath();
        if (path.contains("desert")) return MapDecoration.Type.BANNER_ORANGE;
        if (path.contains("savanna")) return MapDecoration.Type.BANNER_YELLOW;
        if (path.contains("snow")) return MapDecoration.Type.BANNER_LIGHT_BLUE;
        if (path.contains("taiga")) return MapDecoration.Type.BANNER_GREEN;
        return MapDecoration.Type.BANNER_WHITE;
    }

    private static StructureDiscoveryProfile boss(String kind) {
        return new StructureDiscoveryProfile(DiscoveryCategory.BOSS, kind, MapDecoration.Type.BANNER_RED, true);
    }

    private static StructureDiscoveryProfile dungeon(String kind, boolean major) {
        return new StructureDiscoveryProfile(DiscoveryCategory.DUNGEON, kind, MapDecoration.Type.RED_X, major);
    }

    private static StructureDiscoveryProfile temple(String kind) {
        return new StructureDiscoveryProfile(DiscoveryCategory.TEMPLE, kind, MapDecoration.Type.MONUMENT, true);
    }

    private static StructureDiscoveryProfile shrine(String kind) {
        return new StructureDiscoveryProfile(DiscoveryCategory.SHRINE, kind, MapDecoration.Type.BANNER_LIGHT_BLUE, false);
    }

    private static StructureDiscoveryProfile ruin(String kind, boolean major) {
        return new StructureDiscoveryProfile(DiscoveryCategory.RUIN, kind, MapDecoration.Type.TARGET_X, major);
    }

    private static StructureDiscoveryProfile tower(String kind) {
        return new StructureDiscoveryProfile(DiscoveryCategory.TOWER, kind, MapDecoration.Type.MANSION, true);
    }

    private static StructureDiscoveryProfile fortress(String kind) {
        return new StructureDiscoveryProfile(DiscoveryCategory.FORTRESS, kind, MapDecoration.Type.BANNER_BLACK, true);
    }

    private static StructureDiscoveryProfile camp(String kind) {
        return new StructureDiscoveryProfile(DiscoveryCategory.CAMP, kind, MapDecoration.Type.BANNER_ORANGE, false);
    }

    private static StructureDiscoveryProfile ship(String kind) {
        return new StructureDiscoveryProfile(DiscoveryCategory.SHIP, kind, MapDecoration.Type.BANNER_CYAN, true);
    }

    private static String prettifyKind(String path, String fallback) {
        if (path == null || path.isBlank()) return fallback;
        String cleaned = path.replaceAll("_[0-9]+$", "").replace('_', ' ').trim();
        if (cleaned.isBlank()) return fallback;
        StringBuilder result = new StringBuilder();
        for (String part : cleaned.split("\\s+")) {
            if (part.isBlank()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return result.isEmpty() ? fallback : result.toString();
    }
}
