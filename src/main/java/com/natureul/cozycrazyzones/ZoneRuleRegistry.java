package com.natureul.cozycrazyzones;

import net.minecraft.resources.ResourceLocation;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ZoneRuleRegistry {
    public record StructureRule(
            Region minimum,
            Set<MacroRegion> macroRegions,
            RegionalInfluenceBand minimumInfluence,
            String note
    ) {
        public StructureRule {
            macroRegions = Set.copyOf(macroRegions);
        }

        public boolean allows(RegionalCell cell) {
            if (!cell.radialZone().atLeast(minimum)) return false;
            if (!cell.influenceBand().atLeast(minimumInfluence)) return false;
            return macroRegions.isEmpty() || macroRegions.contains(cell.macroRegion());
        }
    }

    public record NaturalEntityRule(
            Region minimum,
            Set<MacroRegion> macroRegions,
            RegionalInfluenceBand minimumInfluence,
            boolean daytimeCandidate,
            boolean enabled,
            String note
    ) {
        public NaturalEntityRule {
            macroRegions = Set.copyOf(macroRegions);
        }

        public boolean allows(RegionalCell cell) {
            if (!cell.radialZone().atLeast(minimum)) return false;
            if (!cell.influenceBand().atLeast(minimumInfluence)) return false;
            return macroRegions.isEmpty() || macroRegions.contains(cell.macroRegion());
        }
    }

    public record PrefixStructureRule(String prefix, StructureRule rule) {}

    private static final Map<ResourceLocation, StructureRule> STRUCTURES = new LinkedHashMap<>();
    private static final Map<ResourceLocation, NaturalEntityRule> NATURAL_ENTITIES = new LinkedHashMap<>();
    private static final List<PrefixStructureRule> STRUCTURE_PREFIXES;

    // Cataclysm is intentionally treated as a Cursed-Pyramid-only worldgen dependency.
    // Registered content is left intact; unrelated structures/natural spawns are suppressed externally.
    private static final Set<String> SUPPRESSED_STRUCTURE_NAMESPACES = Set.of("cataclysm");
    private static final Set<ResourceLocation> STRUCTURE_NAMESPACE_EXCEPTIONS = Set.of(id("cataclysm:cursed_pyramid"));
    private static final Set<String> SUPPRESSED_NATURAL_ENTITY_NAMESPACES = Set.of("cataclysm");

    static {
        // ---------------------------------------------------------------------
        // BIOME-SEMANTIC STRUCTURE FIREWALL
        // ---------------------------------------------------------------------
        // Biome identity is post-processed after TerraBlender has chosen the native palette. Some
        // structure mods ask the original BiomeSource earlier than that, so their "desert" or
        // "jungle" structure can otherwise survive even after the visible biome becomes Field or
        // Grassland. These rules make CozyCrazyZones the final authority for obvious biome-specific
        // structure families.

        regionalStructure("beautify:botanist_house_desert", Region.HEARTHLANDS, RegionalInfluenceBand.CARDINAL_TRANSITION, "Sunscar desert botanist house", MacroRegion.SOUTH);
        regionalStructure("beautify:botanist_house_savanna", Region.HEARTHLANDS, RegionalInfluenceBand.CARDINAL_TRANSITION, "Sunscar savanna botanist house", MacroRegion.SOUTH);
        regionalStructure("beautify:botanist_house_snowy", Region.HEARTHLANDS, RegionalInfluenceBand.CARDINAL_TRANSITION, "Frostmarch snowy botanist house", MacroRegion.NORTH);
        regionalStructure("beautify:botanist_house_taiga", Region.HEARTHLANDS, RegionalInfluenceBand.CARDINAL_TRANSITION, "Frostmarch taiga botanist house", MacroRegion.NORTH);

        regionalStructure("valhelsia_structures:desert_house", Region.HEARTHLANDS, RegionalInfluenceBand.CARDINAL_TRANSITION, "Sunscar desert house", MacroRegion.SOUTH);

        regionalStructure("minecraft:village_desert", Region.HEARTHLANDS, RegionalInfluenceBand.CARDINAL_TRANSITION, "Sunscar village variant", MacroRegion.SOUTH);
        regionalStructure("minecraft:village_savanna", Region.HEARTHLANDS, RegionalInfluenceBand.CARDINAL_TRANSITION, "Sunscar village variant", MacroRegion.SOUTH);
        regionalStructure("minecraft:village_snowy", Region.HEARTHLANDS, RegionalInfluenceBand.CARDINAL_TRANSITION, "Frostmarch village variant", MacroRegion.NORTH);
        regionalStructure("minecraft:village_taiga", Region.HEARTHLANDS, RegionalInfluenceBand.CARDINAL_TRANSITION, "Cool/wooded village variant", MacroRegion.NORTH, MacroRegion.WEST);

        regionalStructure("minecraft:desert_pyramid", Region.FRONTIER, RegionalInfluenceBand.ESTABLISHED, "Sunscar desert temple", MacroRegion.SOUTH);
        regionalStructure("minecraft:jungle_pyramid", Region.FRONTIER, RegionalInfluenceBand.ESTABLISHED, "Greenveil jungle temple", MacroRegion.EAST);
        regionalStructure("minecraft:igloo", Region.FRONTIER, RegionalInfluenceBand.ESTABLISHED, "Frostmarch igloo", MacroRegion.NORTH);
        regionalStructure("minecraft:swamp_hut", Region.FRONTIER, RegionalInfluenceBand.ESTABLISHED, "Greenveil swamp hut", MacroRegion.EAST);
        regionalStructure("betterwitchhuts:witch_hut", Region.FRONTIER, RegionalInfluenceBand.ESTABLISHED, "Greenveil witch hut", MacroRegion.EAST);
        regionalStructure("betterwitchhuts:witch_circle", Region.FRONTIER, RegionalInfluenceBand.ESTABLISHED, "Greenveil witch circle", MacroRegion.EAST);

        regionalStructure("minecraft:ruined_portal_desert", Region.HEARTHLANDS, RegionalInfluenceBand.CARDINAL_TRANSITION, "Sunscar desert portal", MacroRegion.SOUTH);
        regionalStructure("minecraft:ruined_portal_jungle", Region.HEARTHLANDS, RegionalInfluenceBand.CARDINAL_TRANSITION, "Greenveil jungle portal", MacroRegion.EAST);
        regionalStructure("minecraft:ruined_portal_swamp", Region.HEARTHLANDS, RegionalInfluenceBand.CARDINAL_TRANSITION, "Greenveil swamp portal", MacroRegion.EAST);

        regionalStructure("bettermineshafts:mineshaft_acacia", Region.HEARTHLANDS, RegionalInfluenceBand.CARDINAL_TRANSITION, "Sunscar acacia mineshaft", MacroRegion.SOUTH);
        regionalStructure("bettermineshafts:mineshaft_desert", Region.HEARTHLANDS, RegionalInfluenceBand.CARDINAL_TRANSITION, "Sunscar desert mineshaft", MacroRegion.SOUTH);
        regionalStructure("bettermineshafts:mineshaft_mesa", Region.FRONTIER, RegionalInfluenceBand.ESTABLISHED, "Sunscar mesa mineshaft", MacroRegion.SOUTH);
        regionalStructure("bettermineshafts:mineshaft_red_desert", Region.FRONTIER, RegionalInfluenceBand.ESTABLISHED, "Sunscar red-desert mineshaft", MacroRegion.SOUTH);
        regionalStructure("bettermineshafts:mineshaft_jungle", Region.HEARTHLANDS, RegionalInfluenceBand.CARDINAL_TRANSITION, "Greenveil jungle mineshaft", MacroRegion.EAST);
        regionalStructure("bettermineshafts:mineshaft_lush", Region.HEARTHLANDS, RegionalInfluenceBand.CARDINAL_TRANSITION, "Greenveil lush mineshaft", MacroRegion.EAST);
        regionalStructure("bettermineshafts:mineshaft_overgrown", Region.FRONTIER, RegionalInfluenceBand.ESTABLISHED, "Greenveil overgrown mineshaft", MacroRegion.EAST);
        regionalStructure("bettermineshafts:mineshaft_ice", Region.FRONTIER, RegionalInfluenceBand.ESTABLISHED, "Frostmarch ice mineshaft", MacroRegion.NORTH);
        regionalStructure("bettermineshafts:mineshaft_spruce_snowy", Region.FRONTIER, RegionalInfluenceBand.ESTABLISHED, "Frostmarch snowy-spruce mineshaft", MacroRegion.NORTH);
        regionalStructure("bettermineshafts:mineshaft_spruce", Region.HEARTHLANDS, RegionalInfluenceBand.CARDINAL_TRANSITION, "Cool/wooded spruce mineshaft", MacroRegion.NORTH, MacroRegion.WEST);

        regionalStructure("born_in_chaos_v1:clown_caravan_savanna", Region.FRONTIER, RegionalInfluenceBand.ESTABLISHED, "Sunscar caravan variant", MacroRegion.SOUTH);
        regionalStructure("born_in_chaos_v1:clown_caravan_taiga", Region.FRONTIER, RegionalInfluenceBand.ESTABLISHED, "Cool/wooded caravan variant", MacroRegion.NORTH, MacroRegion.WEST);
        regionalStructure("born_in_chaos_v1:infernal_pumpkin", Region.WILDLANDS, RegionalInfluenceBand.ESTABLISHED, "Harvestwood infernal pumpkin", MacroRegion.WEST);

        // ---------------------------------------------------------------------
        // PROGRESSION STRUCTURES
        // ---------------------------------------------------------------------
        // Hearthlands: local adventure content. Anything absent is unrestricted.
        structure("dungeons_enhanced:stables", Region.HEARTHLANDS);
        structure("dungeons_enhanced:dungeon_variant", Region.HEARTHLANDS);
        structure("dungeons_enhanced:watch_tower", Region.HEARTHLANDS);
        structure("dungeons_enhanced:witch_tower", Region.HEARTHLANDS);
        regionalStructure("dungeons_enhanced:desert_tomb", Region.HEARTHLANDS, RegionalInfluenceBand.CARDINAL_TRANSITION, "Small Sunscar desert-site content", MacroRegion.SOUTH);
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
        regionalStructure("dungeons_enhanced:desert_temple", Region.FRONTIER, RegionalInfluenceBand.ESTABLISHED, "Strongly arid Sunscar structure", MacroRegion.SOUTH);
        regionalStructure("dungeons_enhanced:jungle_monument", Region.FRONTIER, RegionalInfluenceBand.ESTABLISHED, "Strongly tropical Greenveil structure", MacroRegion.EAST);
        structure("dungeons_enhanced:large_dungeon", Region.FRONTIER);
        structure("betterdungeons:skeleton_dungeon", Region.FRONTIER);
        structure("betterdungeons:zombie_dungeon", Region.FRONTIER);
        regionalStructure("betterdeserttemples:desert_temple", Region.FRONTIER, RegionalInfluenceBand.ESTABLISHED, "YUNG Sunscar desert temple", MacroRegion.SOUTH);
        regionalStructure("betterjungletemples:jungle_temple", Region.FRONTIER, RegionalInfluenceBand.ESTABLISHED, "YUNG Greenveil jungle temple", MacroRegion.EAST);
        structure("valhelsia_structures:forge", Region.FRONTIER);
        structure("valhelsia_structures:castle", Region.FRONTIER);
        structure("valhelsia_structures:castle_ruin", Region.FRONTIER);
        structure("born_in_chaos_v1:firewell", Region.FRONTIER);
        structure("born_in_chaos_v1:mound_of_hounds", Region.FRONTIER);

        // Wildlands: deliberate diamond-tier expeditions.
        structure("dungeons_enhanced:deep_crypt", Region.WILDLANDS);
        regionalStructure("dungeons_enhanced:ice_pit", Region.WILDLANDS, RegionalInfluenceBand.ESTABLISHED, "Frostmarch cold-region dungeon", MacroRegion.NORTH);
        structure("dungeons_enhanced:monster_maze", Region.WILDLANDS);
        structure("dungeons_enhanced:elders_temple", Region.WILDLANDS);
        structure("dungeons_enhanced:flying_dutchman", Region.WILDLANDS);
        structure("mowziesmobs:wrought_chamber", Region.WILDLANDS);
        regionalStructure("mowziesmobs:frostmaw_spawn", Region.WILDLANDS, RegionalInfluenceBand.ESTABLISHED, "Required Frostmarch Wildlands boss", MacroRegion.NORTH);
        regionalStructure("mowziesmobs:umvuthana_grove", Region.WILDLANDS, RegionalInfluenceBand.ESTABLISHED, "Required Sunscar Wildlands boss", MacroRegion.SOUTH);
        structure("mowziesmobs:monastery", Region.WILDLANDS);

        // Known regional final destination. Cataclysm's other structures are namespace-suppressed above.
        regionalStructure("cataclysm:cursed_pyramid", Region.DREAD_REACHES, RegionalInfluenceBand.ESTABLISHED, "Sunscar final: Ancient Remnant", MacroRegion.SOUTH);

        // Born in Chaos tower families. Observation towers remain broad local content for now;
        // Dark Towers are pushed to the Harvestwood Dread ecology.
        STRUCTURE_PREFIXES = List.of(
                new PrefixStructureRule("born_in_chaos_v1:observation_tower_", new StructureRule(Region.HEARTHLANDS, Set.of(), RegionalInfluenceBand.SHARED_CORE, "Observation tower family")),
                new PrefixStructureRule("born_in_chaos_v1:dark_tower_", new StructureRule(Region.DREAD_REACHES, EnumSet.of(MacroRegion.WEST), RegionalInfluenceBand.ESTABLISHED, "Harvestwood Born in Chaos deep-forest tower family"))
        );

        // ---------------------------------------------------------------------
        // NATURAL-SPAWN ECOLOGY FIREWALL
        // ---------------------------------------------------------------------
        // The registry audit showed that some biome modifiers inject warm-climate animals into BOP
        // biomes such as Seasonal Forest and Maple Woods. Therefore visible biome identity alone is
        // not enough. These rules apply only to NATURAL spawns; authored raids/hordes/structures,
        // summons and commands intentionally bypass them.

        // Warm/dry wildlife belongs to established Sunscar country, not Frostmarch/Harvestwood.
        regionalNatural("alexsmobs:gazelle", Region.HEARTHLANDS, true, true, "Sunscar warm-grassland wildlife", MacroRegion.SOUTH);
        regionalNatural("alexsmobs:elephant", Region.HEARTHLANDS, true, true, "Sunscar warm-grassland wildlife", MacroRegion.SOUTH);
        regionalNatural("alexsmobs:emu", Region.HEARTHLANDS, true, true, "Sunscar dry-country wildlife", MacroRegion.SOUTH);
        regionalNatural("alexsmobs:kangaroo", Region.HEARTHLANDS, true, true, "Sunscar dry-country wildlife", MacroRegion.SOUTH);
        regionalNatural("alexsmobs:maned_wolf", Region.HEARTHLANDS, true, true, "Sunscar open-country wildlife", MacroRegion.SOUTH);
        regionalNatural("alexsmobs:rhinoceros", Region.HEARTHLANDS, true, true, "Sunscar warm-grassland wildlife", MacroRegion.SOUTH);
        regionalNatural("alexsmobs:jerboa", Region.HEARTHLANDS, true, true, "Sunscar desert wildlife", MacroRegion.SOUTH);
        regionalNatural("alexsmobs:roadrunner", Region.HEARTHLANDS, true, true, "Sunscar desert wildlife", MacroRegion.SOUTH);
        regionalNatural("alexsmobs:rattlesnake", Region.FRONTIER, true, true, "Sunscar dry-country danger", MacroRegion.SOUTH);
        regionalNatural("alexsmobs:rocky_roller", Region.FRONTIER, true, true, "Sunscar badlands wildlife", MacroRegion.SOUTH);
        regionalNatural("alexsmobs:guster", Region.FRONTIER, true, true, "Sunscar badlands danger", MacroRegion.SOUTH);
        regionalNatural("alexsmobs:tarantula_hawk", Region.FRONTIER, true, true, "Sunscar desert danger", MacroRegion.SOUTH);
        regionalNatural("alexsmobs:sunbird", Region.WILDLANDS, true, true, "Sunscar high-tier roaming creature", MacroRegion.SOUTH);

        // Tropical wildlife is kept in Greenveil once its ecology has actually emerged.
        regionalNatural("alexsmobs:gorilla", Region.HEARTHLANDS, true, true, "Greenveil tropical wildlife", MacroRegion.EAST);
        regionalNatural("alexsmobs:tiger", Region.FRONTIER, true, true, "Greenveil jungle predator", MacroRegion.EAST);
        regionalNatural("alexsmobs:toucan", Region.HEARTHLANDS, true, true, "Greenveil tropical bird", MacroRegion.EAST);
        regionalNatural("alexsmobs:capuchin_monkey", Region.HEARTHLANDS, true, true, "Greenveil tropical wildlife", MacroRegion.EAST);
        regionalNatural("alexsmobs:anaconda", Region.FRONTIER, true, true, "Greenveil wet-jungle predator", MacroRegion.EAST);
        regionalNatural("alexsmobs:caiman", Region.FRONTIER, true, true, "Greenveil wetland predator", MacroRegion.EAST);
        regionalNatural("alexsmobs:crocodile", Region.FRONTIER, true, true, "Greenveil wetland predator", MacroRegion.EAST);

        // Cold/temperate large wildlife. Moose and grizzlies make sense in both the northern woods
        // and the old-growth Harvestwood rather than across every biome modifier that happens to add them.
        regionalNatural("alexsmobs:snow_leopard", Region.FRONTIER, true, true, "Frostmarch alpine predator", MacroRegion.NORTH);
        regionalNatural("alexsmobs:moose", Region.HEARTHLANDS, true, true, "Cool forest wildlife", MacroRegion.NORTH, MacroRegion.WEST);
        regionalNatural("alexsmobs:grizzly_bear", Region.FRONTIER, true, true, "Cool/old-growth predator", MacroRegion.NORTH, MacroRegion.WEST);

        // Frontier ecology: recognizable step above the Hearthlands baseline.
        regionalNatural("mowziesmobs:foliaath", Region.FRONTIER, false, true, "Greenveil biome predator", MacroRegion.EAST);
        natural("mowziesmobs:naga", Region.FRONTIER, false, true, "Coastal/stony-shore predator; remains multi-region");
        regionalNatural("skarrier_mobs:snap", Region.FRONTIER, true, true, "Sunscar desert daytime predator", MacroRegion.SOUTH);
        regionalNatural("skarrier_mobs:dangle", Region.FRONTIER, true, true, "Greenveil bamboo-jungle daytime danger", MacroRegion.EAST);
        regionalNatural("skarrier_mobs:slither_spawner_dummy", Region.FRONTIER, true, true, "Gate NATURAL dummy; it creates Slithers as MOB_SUMMONED", MacroRegion.EAST);
        regionalNatural("skarrier_mobs:quake", Region.FRONTIER, true, true, "80 HP Sunscar desert surface daytime threat", MacroRegion.SOUTH);

        natural("born_in_chaos_v1:skeleton_thrasher", Region.FRONTIER, false, true, "Heavy armored night escalation");
        natural("born_in_chaos_v1:zombie_bruiser", Region.FRONTIER, true, true, "Heavy roaming threat");
        natural("born_in_chaos_v1:bonescaller", Region.FRONTIER, false, true, "Caster escalation");
        natural("born_in_chaos_v1:dire_hound_leader", Region.FRONTIER, true, true, "Roaming pack leader");
        natural("born_in_chaos_v1:dread_hound", Region.FRONTIER, false, true, "Pack threat paired with Dire Hound Leader progression");
        natural("born_in_chaos_v1:door_knight", Region.FRONTIER, false, true, "Armored blocking melee escalation");
        natural("born_in_chaos_v1:skeleton_demoman", Region.FRONTIER, false, true, "Explosive skeleton escalation");
        regionalNatural("born_in_chaos_v1:spirit_guide", Region.FRONTIER, true, true, "Dry/hot roaming caster for Sunscar", MacroRegion.SOUTH);

        natural("myths_of_the_sea:bunyip", Region.FRONTIER, true, true, "Swamp predator; remains multi-region until water ecology pass");
        natural("myths_of_the_sea:bake_kujira", Region.FRONTIER, true, true, "Coastal danger");

        // Wildlands ecology: miniboss-class and major roaming threats.
        regionalNatural("skarrier_mobs:carniflore", Region.WILDLANDS, true, true, "Major Greenveil jungle predator", MacroRegion.EAST);
        regionalNatural("skarrier_mobs:slither_matriarch", Region.WILDLANDS, true, true, "Greenveil mangrove escalation", MacroRegion.EAST);
        natural("skarrier_mobs:wrought", Region.WILDLANDS, true, true, "Skarrier miniboss-class natural spawn");

        natural("born_in_chaos_v1:lifestealer", Region.WILDLANDS, false, true, "Miniboss-class natural spawn");
        natural("born_in_chaos_v1:missioner", Region.WILDLANDS, false, true, "NATURAL spawn only; authored appearances remain legal");
        natural("born_in_chaos_v1:nightmare_stalker", Region.WILDLANDS, false, true, "Fast high-danger roaming predator");
        natural("born_in_chaos_v1:mother_spider", Region.WILDLANDS, false, true, "Large spider/miniboss-class natural spawn");
        regionalNatural("born_in_chaos_v1:sir_pumpkinhead", Region.WILDLANDS, false, true, "Harvestwood seasonal Wildlands boss; non-NATURAL authored appearances bypass", MacroRegion.WEST);
        natural("born_in_chaos_v1:fallen_chaos_knight", Region.WILDLANDS, true, false, "Disabled until Scarlet Persecutor suppression interaction is tested");

        // Dread Reaches apex roamers.
        natural("myths_of_the_sea:leviathan", Region.DREAD_REACHES, true, true, "Dread Reaches sea monster");
        natural("myths_of_the_sea:kraken", Region.DREAD_REACHES, true, true, "Dread Reaches sea monster");
        natural("born_in_chaos_v1:supreme_bonescaller", Region.DREAD_REACHES, false, true, "Natural roaming boss only; structure/event spawns bypass this rule");
    }

    private ZoneRuleRegistry() {}

    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }

    private static void structure(String id, Region minimum) {
        STRUCTURES.put(new ResourceLocation(id), new StructureRule(minimum, Set.of(), RegionalInfluenceBand.SHARED_CORE, "Radial rule"));
    }

    private static void regionalStructure(String id, Region minimum, RegionalInfluenceBand minimumInfluence, String note, MacroRegion... regions) {
        STRUCTURES.put(new ResourceLocation(id), new StructureRule(minimum, enumSet(regions), minimumInfluence, note));
    }

    private static void natural(String id, Region minimum, boolean daytime, boolean enabled, String note) {
        NATURAL_ENTITIES.put(new ResourceLocation(id), new NaturalEntityRule(minimum, Set.of(), RegionalInfluenceBand.SHARED_CORE, daytime, enabled, note));
    }

    private static void regionalNatural(String id, Region minimum, boolean daytime, boolean enabled, String note, MacroRegion... regions) {
        NATURAL_ENTITIES.put(new ResourceLocation(id), new NaturalEntityRule(minimum, enumSet(regions), RegionalInfluenceBand.ESTABLISHED, daytime, enabled, note));
    }

    private static Set<MacroRegion> enumSet(MacroRegion... regions) {
        if (regions.length == 0) return Set.of();
        EnumSet<MacroRegion> set = EnumSet.noneOf(MacroRegion.class);
        for (MacroRegion region : regions) set.add(region);
        return set;
    }

    public static Optional<StructureRule> structureRule(ResourceLocation id) {
        StructureRule exact = STRUCTURES.get(id);
        if (exact != null) return Optional.of(exact);
        String serialized = id.toString();
        for (PrefixStructureRule rule : STRUCTURE_PREFIXES) {
            if (serialized.startsWith(rule.prefix())) return Optional.of(rule.rule());
        }
        return Optional.empty();
    }

    public static Optional<Region> minimumStructureRegion(ResourceLocation id) {
        return structureRule(id).map(StructureRule::minimum);
    }

    public static boolean structureExplicitlySuppressed(ResourceLocation id) {
        return SUPPRESSED_STRUCTURE_NAMESPACES.contains(id.getNamespace()) && !STRUCTURE_NAMESPACE_EXCEPTIONS.contains(id);
    }

    public static boolean naturalEntityNamespaceSuppressed(ResourceLocation id) {
        return SUPPRESSED_NATURAL_ENTITY_NAMESPACES.contains(id.getNamespace());
    }

    public static Optional<NaturalEntityRule> naturalEntityRule(ResourceLocation id) {
        return Optional.ofNullable(NATURAL_ENTITIES.get(id));
    }

    public static Map<ResourceLocation, StructureRule> structures() { return Map.copyOf(STRUCTURES); }
    public static Map<ResourceLocation, NaturalEntityRule> naturalEntities() { return Map.copyOf(NATURAL_ENTITIES); }
    public static List<PrefixStructureRule> structurePrefixes() { return STRUCTURE_PREFIXES; }
}
