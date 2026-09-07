package com.natureul.cozycrazyzones;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Natural-spawn resolver implementing the CozyCrazyCraft Danger & Enemy Bible.
 *
 * This layer is intentionally evaluated only for MobSpawnType.NATURAL. Structure inhabitants,
 * spawners, summons, raids, Undead Nights and authored boss encounters bypass it. That distinction
 * lets the world have a tightly authored ambient ecology without breaking bespoke encounter logic.
 *
 * The policy is deliberately cheap: exact-id lookup, the already-computed regional cell, one random
 * roll for curated high-weight defaults, and an in-memory 512-block apex cooldown. There are no
 * radius entity scans or chunk searches on the spawn path.
 */
public final class DangerBibleSpawnPolicy {
    private static final Map<ResourceLocation, SpawnProfile> PROFILES = new HashMap<>();
    private static final Set<ResourceLocation> HARD_DENY = new HashSet<>();

    /** Born in Chaos ships many forge:any biome modifiers; uncurated entries are denied NATURAL spawning. */
    private static final Set<String> WHITELIST_NATURAL_NAMESPACES = Set.of("born_in_chaos_v1");

    /** These systems are event/special-encounter systems rather than ambient biome ecology. */
    private static final Set<String> DENY_NATURAL_NAMESPACES = Set.of(
            "anomaly_rephased",
            "undeadnights",
            "the_knocker"
    );

    private static final Map<ApexKey, Long> LAST_APEX = new HashMap<>();
    private static final int APEX_CELL_SIZE = 512;

    static {
        // -----------------------------------------------------------------
        // P0: authored bosses / EX encounters can never be NATURAL ecology.
        // -----------------------------------------------------------------
        deny(
                "born_in_chaos_v1:lord_pumpkinhead",
                "born_in_chaos_v1:sir_pumpkinhead",
                "born_in_chaos_v1:sir_pumpkinhead_without_horse",
                "born_in_chaos_v1:sir_the_headless",
                "mowziesmobs:frostmaw",
                "mowziesmobs:umvuthi",
                "mowziesmobs:ferrous_wroughtnaut",
                "aquamirae:captain_cornelia",
                "aquamirae:maze_mother",
                "cataclysm:ancient_remnant",
                "skarrier_mobs:tunnel_gore",
                "skarrier_mobs:wrought"
        );

        // Mutant Monsters are E8 encounters, not ordinary background population. Keep the random
        // biome route closed; explicit hunts/events/structures can still spawn them by non-NATURAL means.
        deny(
                "mutantmonsters:mutant_creeper",
                "mutantmonsters:mutant_zombie",
                "mutantmonsters:mutant_skeleton",
                "mutantmonsters:mutant_enderman"
        );

        // Dungeons Mobs: global Wraith/necromancer convenience defaults conflict with authored ecology.
        deny("dungeonsmobs:wraith", "dungeonsmobs:necromancer");

        // Skarrier broad/default entries that still need a mechanics audit remain structure/special only
        // rather than leaking into every regional population in the meantime.
        deny(
                "skarrier_mobs:bowlder",
                "skarrier_mobs:breacher",
                "skarrier_mobs:dragger",
                "skarrier_mobs:eater",
                "skarrier_mobs:gorger",
                "skarrier_mobs:quake"
        );

        // -----------------------------------------------------------------
        // HARVESTWOOD / WEST
        // Farms and wildlife remain visible; pumpkin corruption grows with distance.
        // -----------------------------------------------------------------
        regional("born_in_chaos_v1:mr_pumpkin", MacroRegion.WEST,
                Region.HEARTHLANDS, Region.DREAD_REACHES, RegionalInfluenceBand.CARDINAL_TRANSITION,
                0.08f, 0.24f, 0.16f, 0.08f, 0.40f, 1.00f);
        regional("born_in_chaos_v1:pumpkin_dunce", MacroRegion.WEST,
                Region.HEARTHLANDS, Region.DREAD_REACHES, RegionalInfluenceBand.CARDINAL_TRANSITION,
                0.05f, 0.20f, 0.13f, 0.07f, 0.40f, 1.00f);
        regional("born_in_chaos_v1:mrs_pumpkin", MacroRegion.WEST,
                Region.FRONTIER, Region.DREAD_REACHES, RegionalInfluenceBand.ESTABLISHED,
                0.00f, 0.14f, 0.13f, 0.08f, 0.45f, 1.00f);
        regional("born_in_chaos_v1:senor_pumpkin", MacroRegion.WEST,
                Region.FRONTIER, Region.DREAD_REACHES, RegionalInfluenceBand.ESTABLISHED,
                0.00f, 0.05f, 0.13f, 0.09f, 0.35f, 1.00f);
        regional("born_in_chaos_v1:restless_spirit", MacroRegion.WEST,
                Region.HEARTHLANDS, Region.DREAD_REACHES, RegionalInfluenceBand.CARDINAL_TRANSITION,
                0.03f, 0.10f, 0.16f, 0.13f, 0.10f, 1.00f);
        regional("born_in_chaos_v1:dread_hound", MacroRegion.WEST,
                Region.FRONTIER, Region.DREAD_REACHES, RegionalInfluenceBand.ESTABLISHED,
                0.00f, 0.11f, 0.20f, 0.17f, 0.20f, 1.00f);
        regional("born_in_chaos_v1:zombie_lumberjack", MacroRegion.WEST,
                Region.FRONTIER, Region.DREAD_REACHES, RegionalInfluenceBand.ESTABLISHED,
                0.00f, 0.10f, 0.18f, 0.13f, 0.00f, 1.00f);
        regional("born_in_chaos_v1:door_knight", MacroRegion.WEST,
                Region.WILDLANDS, Region.DREAD_REACHES, RegionalInfluenceBand.ESTABLISHED,
                0.00f, 0.00f, 0.045f, 0.050f, 0.00f, 1.00f);
        regional("born_in_chaos_v1:missioner", MacroRegion.WEST,
                Region.WILDLANDS, Region.DREAD_REACHES, RegionalInfluenceBand.ESTABLISHED,
                0.00f, 0.00f, 0.035f, 0.045f, 0.00f, 1.00f);
        regional("born_in_chaos_v1:supreme_bonescaller", MacroRegion.WEST,
                Region.DREAD_REACHES, Region.DREAD_REACHES, RegionalInfluenceBand.ESTABLISHED,
                0.00f, 0.00f, 0.00f, 0.025f, 0.00f, 1.00f);
        regional("born_in_chaos_v1:seared_spirit", MacroRegion.WEST,
                Region.WILDLANDS, Region.DREAD_REACHES, RegionalInfluenceBand.ESTABLISHED,
                0.00f, 0.00f, 0.075f, 0.12f, 0.45f, 1.00f);

        apex("born_in_chaos_v1:pumpkinhead", MacroRegion.WEST,
                Region.WILDLANDS, Region.DREAD_REACHES, 0.018f, 0.022f,
                0.25f, 1.00f, "harvestwood_apex", 12_000);
        apex("born_in_chaos_v1:pumpkin_bruiser", MacroRegion.WEST,
                Region.WILDLANDS, Region.DREAD_REACHES, 0.045f, 0.050f,
                0.40f, 1.00f, "harvestwood_apex", 6_000);
        apex("born_in_chaos_v1:nightmare_stalker", MacroRegion.WEST,
                Region.WILDLANDS, Region.DREAD_REACHES, 0.018f, 0.025f,
                0.00f, 1.00f, "harvestwood_apex", 12_000);
        apex("born_in_chaos_v1:dire_hound_leader", MacroRegion.WEST,
                Region.WILDLANDS, Region.DREAD_REACHES, 0.018f, 0.022f,
                0.10f, 1.00f, "harvestwood_apex", 8_000);
        apex("born_in_chaos_v1:lifestealer", MacroRegion.WEST,
                Region.WILDLANDS, Region.DREAD_REACHES, 0.025f, 0.035f,
                0.00f, 1.00f, "harvestwood_apex", 8_000);

        regional("whisperwoods:hidebehind", MacroRegion.WEST,
                Region.FRONTIER, Region.DREAD_REACHES, RegionalInfluenceBand.ESTABLISHED,
                0.00f, 0.12f, 0.20f, 0.18f, 0.05f, 1.00f);
        regional("whisperwoods:zotzpyre", MacroRegion.WEST,
                Region.FRONTIER, Region.DREAD_REACHES, RegionalInfluenceBand.ESTABLISHED,
                0.00f, 0.12f, 0.18f, 0.16f, 0.05f, 1.00f);
        apex("whisperwoods:hirschgeist", MacroRegion.WEST,
                Region.WILDLANDS, Region.DREAD_REACHES, 0.025f, 0.035f,
                0.55f, 0.80f, "harvestwood_territorial", 12_000);

        // -----------------------------------------------------------------
        // GREENVEIL / EAST
        // Daylight danger is biological: predators and territorial plants; night adds jungle undead.
        // -----------------------------------------------------------------
        regional("skarrier_mobs:zombiflore", MacroRegion.EAST,
                Region.FRONTIER, Region.DREAD_REACHES, RegionalInfluenceBand.ESTABLISHED,
                0.00f, 0.50f, 0.65f, 0.58f, 0.55f, 1.00f);
        regional("skarrier_mobs:carniflore", MacroRegion.EAST,
                Region.FRONTIER, Region.DREAD_REACHES, RegionalInfluenceBand.ESTABLISHED,
                0.00f, 0.18f, 0.34f, 0.28f, 1.00f, 1.00f);
        regional("mowziesmobs:foliaath", MacroRegion.EAST,
                Region.FRONTIER, Region.DREAD_REACHES, RegionalInfluenceBand.ESTABLISHED,
                0.00f, 0.42f, 0.55f, 0.48f, 1.00f, 1.00f);
        regional("mowziesmobs:naga", MacroRegion.EAST,
                Region.FRONTIER, Region.DREAD_REACHES, RegionalInfluenceBand.ESTABLISHED,
                0.00f, 0.12f, 0.18f, 0.15f, 0.80f, 1.00f);
        regional("skarrier_mobs:dangle", MacroRegion.EAST,
                Region.FRONTIER, Region.DREAD_REACHES, RegionalInfluenceBand.ESTABLISHED,
                0.00f, 0.28f, 0.38f, 0.32f, 0.85f, 1.00f);
        regional("skarrier_mobs:slither_spawner_dummy", MacroRegion.EAST,
                Region.FRONTIER, Region.DREAD_REACHES, RegionalInfluenceBand.ESTABLISHED,
                0.00f, 0.08f, 0.12f, 0.10f, 0.70f, 1.00f);
        apex("skarrier_mobs:slither_matriarch", MacroRegion.EAST,
                Region.WILDLANDS, Region.DREAD_REACHES, 0.022f, 0.028f,
                0.50f, 1.00f, "greenveil_apex", 10_000);
        apex("born_in_chaos_v1:mother_spider", MacroRegion.EAST,
                Region.WILDLANDS, Region.DREAD_REACHES, 0.015f, 0.022f,
                0.00f, 1.00f, "greenveil_apex", 12_000);

        regional("dungeonsmobs:jungle_zombie", MacroRegion.EAST,
                Region.HEARTHLANDS, Region.DREAD_REACHES, RegionalInfluenceBand.CARDINAL_TRANSITION,
                0.10f, 0.42f, 0.55f, 0.45f, 0.00f, 1.00f);
        regional("dungeonsmobs:mossy_skeleton", MacroRegion.EAST,
                Region.HEARTHLANDS, Region.DREAD_REACHES, RegionalInfluenceBand.CARDINAL_TRANSITION,
                0.08f, 0.36f, 0.48f, 0.42f, 0.00f, 1.00f);
        regional("dungeonsmobs:whisperer", MacroRegion.EAST,
                Region.FRONTIER, Region.DREAD_REACHES, RegionalInfluenceBand.ESTABLISHED,
                0.00f, 0.075f, 0.12f, 0.10f, 0.00f, 1.00f);
        apex("dungeonsmobs:leapleaf", MacroRegion.EAST,
                Region.WILDLANDS, Region.DREAD_REACHES, 0.018f, 0.028f,
                0.65f, 1.00f, "greenveil_apex", 12_000);

        // Alex's Mobs biome defaults remain the first filter; these rolls stop serious predators from
        // becoming carpets once the visible biome has been regionalized by CozyCrazyZones.
        regional("alexsmobs:tiger", MacroRegion.EAST,
                Region.FRONTIER, Region.DREAD_REACHES, RegionalInfluenceBand.ESTABLISHED,
                0.00f, 0.18f, 0.24f, 0.20f, 1.00f, 1.00f);
        regional("alexsmobs:crocodile", MacroRegion.EAST,
                Region.FRONTIER, Region.DREAD_REACHES, RegionalInfluenceBand.ESTABLISHED,
                0.00f, 0.45f, 0.58f, 0.50f, 1.00f, 1.00f);
        regional("alexsmobs:caiman", MacroRegion.EAST,
                Region.FRONTIER, Region.DREAD_REACHES, RegionalInfluenceBand.ESTABLISHED,
                0.00f, 0.58f, 0.68f, 0.60f, 1.00f, 1.00f);
        regional("alexsmobs:anaconda", MacroRegion.EAST,
                Region.FRONTIER, Region.DREAD_REACHES, RegionalInfluenceBand.ESTABLISHED,
                0.00f, 0.20f, 0.28f, 0.25f, 1.00f, 1.00f);

        // -----------------------------------------------------------------
        // SUNSCAR / SOUTH
        // Sparse mob pressure: heat/open ground do part of the difficulty work.
        // -----------------------------------------------------------------
        regional("skarrier_mobs:snap", MacroRegion.SOUTH,
                Region.FRONTIER, Region.DREAD_REACHES, RegionalInfluenceBand.ESTABLISHED,
                0.00f, 0.30f, 0.38f, 0.30f, 0.85f, 1.00f);
        regional("skarrier_mobs:trawler", MacroRegion.SOUTH,
                Region.WILDLANDS, Region.DREAD_REACHES, RegionalInfluenceBand.ESTABLISHED,
                0.00f, 0.00f, 0.055f, 0.060f, 0.70f, 1.00f);
        regional("born_in_chaos_v1:spirit_guide", MacroRegion.SOUTH,
                Region.FRONTIER, Region.DREAD_REACHES, RegionalInfluenceBand.ESTABLISHED,
                0.00f, 0.055f, 0.075f, 0.060f, 0.25f, 1.00f);
        regional("dungeonsmobs:geomancer", MacroRegion.SOUTH,
                Region.FRONTIER, Region.DREAD_REACHES, RegionalInfluenceBand.ESTABLISHED,
                0.00f, 0.040f, 0.075f, 0.060f, 0.35f, 1.00f);
        regional("alexsmobs:rattlesnake", MacroRegion.SOUTH,
                Region.FRONTIER, Region.DREAD_REACHES, RegionalInfluenceBand.ESTABLISHED,
                0.00f, 0.48f, 0.52f, 0.42f, 1.00f, 1.00f);
        regional("alexsmobs:rocky_roller", MacroRegion.SOUTH,
                Region.FRONTIER, Region.DREAD_REACHES, RegionalInfluenceBand.ESTABLISHED,
                0.00f, 0.22f, 0.30f, 0.24f, 1.00f, 1.00f);
        regional("alexsmobs:guster", MacroRegion.SOUTH,
                Region.FRONTIER, Region.DREAD_REACHES, RegionalInfluenceBand.ESTABLISHED,
                0.00f, 0.16f, 0.22f, 0.18f, 0.90f, 1.00f);
        regional("alexsmobs:rhinoceros", MacroRegion.SOUTH,
                Region.FRONTIER, Region.DREAD_REACHES, RegionalInfluenceBand.ESTABLISHED,
                0.00f, 0.18f, 0.24f, 0.18f, 1.00f, 1.00f);

        // -----------------------------------------------------------------
        // FROSTMARCH / NORTH
        // Cold/exposure is part of difficulty, so hostile density stays below a generic monster soup.
        // -----------------------------------------------------------------
        regional("alexsmobs:froststalker", MacroRegion.NORTH,
                Region.HEARTHLANDS, Region.DREAD_REACHES, RegionalInfluenceBand.CARDINAL_TRANSITION,
                0.12f, 0.32f, 0.42f, 0.38f, 1.00f, 1.00f);
        regional("alexsmobs:snow_leopard", MacroRegion.NORTH,
                Region.FRONTIER, Region.DREAD_REACHES, RegionalInfluenceBand.ESTABLISHED,
                0.00f, 0.12f, 0.18f, 0.16f, 1.00f, 1.00f);
        regional("alexsmobs:tusklin", MacroRegion.NORTH,
                Region.WILDLANDS, Region.DREAD_REACHES, RegionalInfluenceBand.ESTABLISHED,
                0.00f, 0.00f, 0.18f, 0.16f, 1.00f, 1.00f);
        regional("dungeonsmobs:frozen_zombie", MacroRegion.NORTH,
                Region.FRONTIER, Region.DREAD_REACHES, RegionalInfluenceBand.ESTABLISHED,
                0.00f, 0.30f, 0.42f, 0.36f, 0.00f, 1.00f);
        regional("dungeonsmobs:iceologer", MacroRegion.NORTH,
                Region.FRONTIER, Region.DREAD_REACHES, RegionalInfluenceBand.ESTABLISHED,
                0.00f, 0.075f, 0.13f, 0.11f, 0.00f, 1.00f);
        regional("dungeonsmobs:icy_creeper", MacroRegion.NORTH,
                Region.FRONTIER, Region.DREAD_REACHES, RegionalInfluenceBand.ESTABLISHED,
                0.00f, 0.10f, 0.17f, 0.14f, 0.00f, 1.00f);
        regional("dungeonsmobs:mountaineer", MacroRegion.NORTH,
                Region.FRONTIER, Region.DREAD_REACHES, RegionalInfluenceBand.ESTABLISHED,
                0.00f, 0.075f, 0.12f, 0.10f, 0.25f, 1.00f);
        regional("dungeonsmobs:windcaller", MacroRegion.NORTH,
                Region.WILDLANDS, Region.DREAD_REACHES, RegionalInfluenceBand.ESTABLISHED,
                0.00f, 0.00f, 0.065f, 0.085f, 0.25f, 1.00f);

        // Aquamirae is a distinct deep northern frozen-sea ecology, not every snowy biome in the world.
        regional("aquamirae:anglerfish", MacroRegion.NORTH,
                Region.DREAD_REACHES, Region.DREAD_REACHES, RegionalInfluenceBand.ESTABLISHED,
                0.00f, 0.00f, 0.00f, 0.48f, 1.00f, 1.00f);
        regional("aquamirae:maw", MacroRegion.NORTH,
                Region.DREAD_REACHES, Region.DREAD_REACHES, RegionalInfluenceBand.ESTABLISHED,
                0.00f, 0.00f, 0.00f, 0.14f, 1.00f, 1.00f);
        regional("aquamirae:spinefish", MacroRegion.NORTH,
                Region.DREAD_REACHES, Region.DREAD_REACHES, RegionalInfluenceBand.ESTABLISHED,
                0.00f, 0.00f, 0.00f, 0.45f, 1.00f, 1.00f);
        regional("aquamirae:tortured_soul", MacroRegion.NORTH,
                Region.DREAD_REACHES, Region.DREAD_REACHES, RegionalInfluenceBand.ESTABLISHED,
                0.00f, 0.00f, 0.00f, 0.18f, 0.00f, 1.00f);

        // -----------------------------------------------------------------
        // Creeper Overhaul regional vocabulary. Biome tags still decide exact terrain; cardinal
        // filtering prevents post-processed biomes from showing the wrong regional creeper family.
        // -----------------------------------------------------------------
        simpleRegional("creeperoverhaul:jungle_creeper", MacroRegion.EAST);
        simpleRegional("creeperoverhaul:bamboo_creeper", MacroRegion.EAST);
        simpleRegional("creeperoverhaul:swamp_creeper", MacroRegion.EAST);
        simpleRegional("creeperoverhaul:desert_creeper", MacroRegion.SOUTH);
        simpleRegional("creeperoverhaul:savannah_creeper", MacroRegion.SOUTH);
        simpleRegional("creeperoverhaul:badlands_creeper", MacroRegion.SOUTH);
        simpleRegional("creeperoverhaul:snowy_creeper", MacroRegion.NORTH);
        simpleRegional("creeperoverhaul:spruce_creeper", MacroRegion.NORTH, MacroRegion.WEST);
        simpleRegional("creeperoverhaul:dark_oak_creeper", MacroRegion.WEST);

        // Mythic ocean shapes remain rare. Their own water-spawn predicates provide the habitat;
        // the Dread gate and cooldown keep them legendary instead of normal sea population.
        globalApex("myths_of_the_sea:kraken", Region.DREAD_REACHES, 0.018f, "ocean_mythic", 18_000);
        globalApex("myths_of_the_sea:leviathan", Region.DREAD_REACHES, 0.018f, "ocean_mythic", 18_000);

        // A small amount of non-themed Born in Chaos pressure is deliberately shared only outside
        // the familiar core. Everything else in the namespace is denied by the whitelist rule below.
        broad("born_in_chaos_v1:decaying_zombie",
                Region.HEARTHLANDS, Region.DREAD_REACHES, RegionalInfluenceBand.CARDINAL_TRANSITION,
                0.08f, 0.18f, 0.17f, 0.12f, 0.00f, 1.00f);
        broad("born_in_chaos_v1:baby_skeleton",
                Region.FRONTIER, Region.DREAD_REACHES, RegionalInfluenceBand.ESTABLISHED,
                0.00f, 0.06f, 0.07f, 0.05f, 0.00f, 1.00f);
        broad("born_in_chaos_v1:zombie_bruiser",
                Region.FRONTIER, Region.DREAD_REACHES, RegionalInfluenceBand.ESTABLISHED,
                0.00f, 0.055f, 0.075f, 0.065f, 0.00f, 1.00f);
        multiRegional("born_in_chaos_v1:baby_spider", Set.of(MacroRegion.EAST, MacroRegion.WEST),
                Region.FRONTIER, Region.DREAD_REACHES, RegionalInfluenceBand.ESTABLISHED,
                0.00f, 0.04f, 0.06f, 0.05f, 0.00f, 1.00f);
        multiRegional("born_in_chaos_v1:bonescaller", Set.of(MacroRegion.NORTH, MacroRegion.WEST),
                Region.FRONTIER, Region.DREAD_REACHES, RegionalInfluenceBand.ESTABLISHED,
                0.00f, 0.06f, 0.09f, 0.075f, 0.00f, 1.00f);
    }

    private DangerBibleSpawnPolicy() {}

    public static boolean allowsNatural(ServerLevel level, ResourceLocation id, Mob mob, double x, double z) {
        if (HARD_DENY.contains(id)) return false;
        if (DENY_NATURAL_NAMESPACES.contains(id.getNamespace())) return false;

        SpawnProfile profile = PROFILES.get(id);
        if (profile == null) {
            // Born in Chaos broad forge:any injection is opt-in from here onward. Non-NATURAL
            // structure/event/summon spawns never reach this method and remain untouched.
            return !WHITELIST_NATURAL_NAMESPACES.contains(id.getNamespace());
        }

        RegionalCell cell = CozyZonesApi.regionalCellAt(level, x, z);
        if (!profile.allowsCell(cell)) return false;

        float tierChance = profile.chance(cell.radialZone());
        float timeMultiplier = level.isDay() ? profile.dayMultiplier() : profile.nightMultiplier();
        float effectiveChance = Math.min(1.0f, tierChance * timeMultiplier);
        if (effectiveChance <= 0.0f) return false;
        if (effectiveChance < 1.0f && mob.getRandom().nextFloat() >= effectiveChance) return false;

        if (profile.apexGroup() != null && profile.cooldownTicks() > 0) {
            return claimApex(level, profile.apexGroup(), x, z, profile.cooldownTicks());
        }
        return true;
    }

    /** Clear ephemeral apex cooldowns between server sessions/world changes. */
    public static synchronized void clearRuntimeState() {
        LAST_APEX.clear();
    }

    private static synchronized boolean claimApex(ServerLevel level, String group, double x, double z, int cooldownTicks) {
        int cellX = Math.floorDiv((int) Math.floor(x), APEX_CELL_SIZE);
        int cellZ = Math.floorDiv((int) Math.floor(z), APEX_CELL_SIZE);
        ApexKey key = new ApexKey(level.getSeed(), group, cellX, cellZ);
        long now = level.getGameTime();
        Long previous = LAST_APEX.get(key);
        if (previous != null && now >= previous && now - previous < cooldownTicks) return false;
        LAST_APEX.put(key, now);
        return true;
    }

    private static void deny(String... ids) {
        for (String id : ids) HARD_DENY.add(new ResourceLocation(id));
    }

    private static void simpleRegional(String id, MacroRegion... regions) {
        multiRegional(id, Set.of(regions), Region.HEARTHLANDS, Region.DREAD_REACHES,
                RegionalInfluenceBand.CARDINAL_TRANSITION,
                1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static void regional(String id,
                                 MacroRegion region,
                                 Region minimum,
                                 Region maximum,
                                 RegionalInfluenceBand minimumInfluence,
                                 float hearthlands,
                                 float frontier,
                                 float wildlands,
                                 float dread,
                                 float dayMultiplier,
                                 float nightMultiplier) {
        multiRegional(id, Set.of(region), minimum, maximum, minimumInfluence,
                hearthlands, frontier, wildlands, dread, dayMultiplier, nightMultiplier);
    }

    private static void multiRegional(String id,
                                      Set<MacroRegion> regions,
                                      Region minimum,
                                      Region maximum,
                                      RegionalInfluenceBand minimumInfluence,
                                      float hearthlands,
                                      float frontier,
                                      float wildlands,
                                      float dread,
                                      float dayMultiplier,
                                      float nightMultiplier) {
        PROFILES.put(new ResourceLocation(id), new SpawnProfile(
                regions, minimum, maximum, minimumInfluence,
                hearthlands, frontier, wildlands, dread,
                dayMultiplier, nightMultiplier, null, 0
        ));
    }

    private static void broad(String id,
                              Region minimum,
                              Region maximum,
                              RegionalInfluenceBand minimumInfluence,
                              float hearthlands,
                              float frontier,
                              float wildlands,
                              float dread,
                              float dayMultiplier,
                              float nightMultiplier) {
        PROFILES.put(new ResourceLocation(id), new SpawnProfile(
                Set.of(), minimum, maximum, minimumInfluence,
                hearthlands, frontier, wildlands, dread,
                dayMultiplier, nightMultiplier, null, 0
        ));
    }

    private static void apex(String id,
                             MacroRegion region,
                             Region minimum,
                             Region maximum,
                             float wildlands,
                             float dread,
                             float dayMultiplier,
                             float nightMultiplier,
                             String group,
                             int cooldownTicks) {
        PROFILES.put(new ResourceLocation(id), new SpawnProfile(
                Set.of(region), minimum, maximum, RegionalInfluenceBand.ESTABLISHED,
                0.0f, 0.0f, wildlands, dread,
                dayMultiplier, nightMultiplier, group, cooldownTicks
        ));
    }

    private static void globalApex(String id, Region minimum, float dreadChance, String group, int cooldownTicks) {
        PROFILES.put(new ResourceLocation(id), new SpawnProfile(
                Set.of(), minimum, Region.DREAD_REACHES, RegionalInfluenceBand.SHARED_CORE,
                0.0f, 0.0f, 0.0f, dreadChance,
                1.0f, 1.0f, group, cooldownTicks
        ));
    }

    private record SpawnProfile(
            Set<MacroRegion> macroRegions,
            Region minimum,
            Region maximum,
            RegionalInfluenceBand minimumInfluence,
            float hearthlandsChance,
            float frontierChance,
            float wildlandsChance,
            float dreadChance,
            float dayMultiplier,
            float nightMultiplier,
            String apexGroup,
            int cooldownTicks
    ) {
        private SpawnProfile {
            macroRegions = Set.copyOf(macroRegions);
        }

        boolean allowsCell(RegionalCell cell) {
            if (cell.radialZone().tier() < minimum.tier() || cell.radialZone().tier() > maximum.tier()) return false;
            if (!cell.influenceBand().atLeast(minimumInfluence)) return false;
            return macroRegions.isEmpty() || macroRegions.contains(cell.macroRegion());
        }

        float chance(Region region) {
            return switch (region) {
                case HEARTHLANDS -> hearthlandsChance;
                case FRONTIER -> frontierChance;
                case WILDLANDS -> wildlandsChance;
                case DREAD_REACHES -> dreadChance;
            };
        }
    }

    private record ApexKey(long worldSeed, String group, int cellX, int cellZ) {}
}
