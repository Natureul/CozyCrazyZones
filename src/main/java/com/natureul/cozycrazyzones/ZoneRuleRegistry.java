package com.natureul.cozycrazyzones;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ZoneRuleRegistry {
    public record NaturalEntityRule(Region minimum, boolean daytimeCandidate, boolean enabled, String note) {}
    public record PrefixStructureRule(String prefix, Region minimum) {}

    private static final Map<ResourceLocation, Region> STRUCTURES = new LinkedHashMap<>();
    private static final Map<ResourceLocation, NaturalEntityRule> NATURAL_ENTITIES = new LinkedHashMap<>();
    private static final List<PrefixStructureRule> STRUCTURE_PREFIXES;

    static {
        // Hearthlands: local adventure content. Anything absent is unrestricted.
        structure("dungeons_enhanced:stables", Region.HEARTHLANDS);
        structure("dungeons_enhanced:dungeon_variant", Region.HEARTHLANDS);
        structure("dungeons_enhanced:watch_tower", Region.HEARTHLANDS);
        structure("dungeons_enhanced:witch_tower", Region.HEARTHLANDS);
        structure("dungeons_enhanced:desert_tomb", Region.HEARTHLANDS);
        structure("dungeons_enhanced:sunken_shrine", Region.HEARTHLANDS);
        structure("betterdungeons:spider_dungeon", Region.HEARTHLANDS);
        structure("valhelsia_structures:spawner_dungeon", Region.HEARTHLANDS);
        structure("valhelsia_structures:tower_ruin", Region.HEARTHLANDS);

        // Frontier: full-iron expedition territory and meaningful progression accelerators.
        structure("dungeons_enhanced:pillager_camp", Region.FRONTIER);
        structure("dungeons_enhanced:tall_witch_hut", Region.FRONTIER);
        structure("dungeons_enhanced:tower_of_the_undead", Region.FRONTIER);
        structure("dungeons_enhanced:castle", Region.FRONTIER);
        structure("dungeons_enhanced:pirate_ship", Region.FRONTIER);
        structure("dungeons_enhanced:desert_temple", Region.FRONTIER);
        structure("dungeons_enhanced:jungle_monument", Region.FRONTIER);
        structure("dungeons_enhanced:large_dungeon", Region.FRONTIER);
        structure("betterdungeons:skeleton_dungeon", Region.FRONTIER);
        structure("betterdungeons:zombie_dungeon", Region.FRONTIER);
        structure("betterdeserttemples:desert_temple", Region.FRONTIER);
        structure("betterjungletemples:jungle_temple", Region.FRONTIER);
        structure("valhelsia_structures:forge", Region.FRONTIER);
        structure("valhelsia_structures:castle", Region.FRONTIER);
        structure("valhelsia_structures:castle_ruin", Region.FRONTIER);
        structure("born_in_chaos_v1:firewell", Region.FRONTIER);
        structure("born_in_chaos_v1:mound_of_hounds", Region.FRONTIER);

        // Wildlands: deliberate diamond-tier expeditions.
        structure("dungeons_enhanced:deep_crypt", Region.WILDLANDS);
        structure("dungeons_enhanced:ice_pit", Region.WILDLANDS);
        structure("dungeons_enhanced:monster_maze", Region.WILDLANDS);
        structure("dungeons_enhanced:elders_temple", Region.WILDLANDS);
        structure("dungeons_enhanced:flying_dutchman", Region.WILDLANDS);
        structure("mowziesmobs:wrought_chamber", Region.WILDLANDS);
        structure("mowziesmobs:frostmaw_spawn", Region.WILDLANDS);
        structure("mowziesmobs:umvuthana_grove", Region.WILDLANDS);
        structure("mowziesmobs:monastery", Region.WILDLANDS);

        // Born in Chaos tower families were verified in the supplied 1.7.5 jar. Exact runtime
        // IDs are still dumped on first full-pack test so we can replace these family rules later.
        STRUCTURE_PREFIXES = List.of(
                new PrefixStructureRule("born_in_chaos_v1:observation_tower_", Region.HEARTHLANDS),
                new PrefixStructureRule("born_in_chaos_v1:dark_tower_", Region.DREAD_REACHES)
        );

        natural("mowziesmobs:foliaath", Region.FRONTIER, false, true, "Biome predator");
        natural("mowziesmobs:naga", Region.FRONTIER, false, true, "Coastal/mesa predator");
        natural("skarrier_mobs:snap", Region.FRONTIER, true, true, "Daytime-capable biome predator");
        natural("skarrier_mobs:dangle", Region.FRONTIER, true, true, "Bamboo-jungle daytime danger");
        natural("skarrier_mobs:slither_spawner_dummy", Region.FRONTIER, true, true, "Gate the NATURAL dummy; it creates Slithers as MOB_SUMMONED");
        natural("skarrier_mobs:quake", Region.FRONTIER, true, true, "80 HP desert surface daytime threat");
        natural("born_in_chaos_v1:skeleton_thrasher", Region.FRONTIER, false, true, "Night escalation");
        natural("born_in_chaos_v1:zombie_bruiser", Region.FRONTIER, true, true, "Heavy roaming threat");
        natural("born_in_chaos_v1:bonescaller", Region.FRONTIER, false, true, "Caster escalation");
        natural("born_in_chaos_v1:dire_hound_leader", Region.FRONTIER, true, true, "Roaming pack threat");
        natural("myths_of_the_sea:bunyip", Region.FRONTIER, true, true, "Swamp predator");
        natural("myths_of_the_sea:bake_kujira", Region.FRONTIER, true, true, "Coastal danger");

        natural("skarrier_mobs:carniflore", Region.WILDLANDS, true, true, "Major jungle predator");
        natural("skarrier_mobs:slither_matriarch", Region.WILDLANDS, true, true, "Mangrove escalation");
        natural("born_in_chaos_v1:lifestealer", Region.WILDLANDS, false, true, "Miniboss-class natural spawn");
        natural("born_in_chaos_v1:missioner", Region.WILDLANDS, false, true, "NATURAL spawn only; authored appearances remain legal");
        natural("born_in_chaos_v1:fallen_chaos_knight", Region.WILDLANDS, true, false, "Disabled until Scarlet Persecutor suppression interaction is tested");

        natural("myths_of_the_sea:leviathan", Region.DREAD_REACHES, true, true, "Dread Reaches sea monster");
        natural("myths_of_the_sea:kraken", Region.DREAD_REACHES, true, true, "Dread Reaches sea monster");
        natural("born_in_chaos_v1:supreme_bonescaller", Region.DREAD_REACHES, false, true, "Natural roaming boss only; structure/event spawns bypass this rule");
    }

    private ZoneRuleRegistry() {}

    private static void structure(String id, Region minimum) {
        STRUCTURES.put(new ResourceLocation(id), minimum);
    }

    private static void natural(String id, Region minimum, boolean daytime, boolean enabled, String note) {
        NATURAL_ENTITIES.put(new ResourceLocation(id), new NaturalEntityRule(minimum, daytime, enabled, note));
    }

    public static Optional<Region> minimumStructureRegion(ResourceLocation id) {
        Region exact = STRUCTURES.get(id);
        if (exact != null) return Optional.of(exact);
        String serialized = id.toString();
        for (PrefixStructureRule rule : STRUCTURE_PREFIXES) {
            if (serialized.startsWith(rule.prefix())) return Optional.of(rule.minimum());
        }
        return Optional.empty();
    }

    public static Optional<NaturalEntityRule> naturalEntityRule(ResourceLocation id) {
        return Optional.ofNullable(NATURAL_ENTITIES.get(id));
    }

    public static Map<ResourceLocation, Region> structures() { return Map.copyOf(STRUCTURES); }
    public static Map<ResourceLocation, NaturalEntityRule> naturalEntities() { return Map.copyOf(NATURAL_ENTITIES); }
    public static List<PrefixStructureRule> structurePrefixes() { return STRUCTURE_PREFIXES; }
}
