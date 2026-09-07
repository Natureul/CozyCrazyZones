package com.natureul.cozycrazyzones;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.MobSpawnSettings;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Positive ecology layer for CozyCrazyCraft.
 *
 * Instead of periodically creating entities or inflating every biome's static spawn list, this layer
 * replaces a controlled fraction of vanilla's already-scheduled natural spawn selections. That keeps
 * vanilla mob caps and spawn cadence authoritative while allowing cardinal/radial geography to decide
 * what the attempted entity is. Vanilla still runs each chosen entity's SpawnPlacements, light,
 * collision and obstruction checks.
 */
public final class RegionalSpawnInjector {
    private static final EnumMap<MacroRegion, List<Candidate>> CANDIDATES = new EnumMap<>(MacroRegion.class);
    private static final ThreadLocal<InjectionToken> ACTIVE_SELECTION = new ThreadLocal<>();
    private static final int TOKEN_RADIUS = 48;

    static {
        for (MacroRegion region : MacroRegion.values()) CANDIDATES.put(region, new ArrayList<>());

        // -----------------------------------------------------------------
        // HARVESTWOOD — rural life first; folklore/pumpkin pressure outward.
        // -----------------------------------------------------------------
        west("alexsmobs:crow", Habitat.ANY_LAND, TimeRule.DAY, RegionalInfluenceBand.CARDINAL_TRANSITION, 24, 20, 12, 7, 1, 3);
        west("alexsmobs:raccoon", Habitat.ANY_LAND, TimeRule.ANY, RegionalInfluenceBand.CARDINAL_TRANSITION, 18, 18, 12, 7, 1, 2);
        west("alexsmobs:skunk", Habitat.WOODED, TimeRule.ANY, RegionalInfluenceBand.CARDINAL_TRANSITION, 7, 10, 9, 5, 1, 2);
        west("alexsmobs:bison", Habitat.OPEN, TimeRule.DAY, RegionalInfluenceBand.CARDINAL_TRANSITION, 10, 7, 3, 0, 2, 4);
        west("golemoverhaul:hay_golem", Habitat.CULTIVATED, TimeRule.DAY, RegionalInfluenceBand.CARDINAL_TRANSITION, 3, 5, 2, 1, 1, 1);
        west("born_in_chaos_v1:pumpkin_spirit", Habitat.CULTIVATED_OR_WOODED, TimeRule.ANY, RegionalInfluenceBand.CARDINAL_TRANSITION, 2, 4, 3, 2, 1, 1);

        west("born_in_chaos_v1:mr_pumpkin", Habitat.CULTIVATED_OR_WOODED, TimeRule.ANY, RegionalInfluenceBand.CARDINAL_TRANSITION, 2, 6, 5, 3, 1, 2);
        west("born_in_chaos_v1:pumpkin_dunce", Habitat.CULTIVATED_OR_WOODED, TimeRule.ANY, RegionalInfluenceBand.CARDINAL_TRANSITION, 2, 5, 4, 2, 1, 2);
        west("born_in_chaos_v1:restless_spirit", Habitat.ANY_LAND, TimeRule.NIGHT, RegionalInfluenceBand.CARDINAL_TRANSITION, 1, 4, 6, 5, 1, 2);
        west("born_in_chaos_v1:mrs_pumpkin", Habitat.CULTIVATED_OR_WOODED, TimeRule.ANY, RegionalInfluenceBand.ESTABLISHED, 0, 3, 4, 3, 1, 1);
        west("born_in_chaos_v1:senor_pumpkin", Habitat.WOODED, TimeRule.ANY, RegionalInfluenceBand.ESTABLISHED, 0, 1, 3, 3, 1, 1);
        west("born_in_chaos_v1:zombie_lumberjack", Habitat.WOODED, TimeRule.NIGHT, RegionalInfluenceBand.ESTABLISHED, 0, 4, 7, 6, 1, 2);
        west("born_in_chaos_v1:dread_hound", Habitat.WOODED, TimeRule.NIGHT, RegionalInfluenceBand.ESTABLISHED, 0, 3, 6, 6, 1, 2);
        west("whisperwoods:hidebehind", Habitat.WOODED, TimeRule.NIGHT, RegionalInfluenceBand.ESTABLISHED, 0, 1, 4, 4, 1, 1);
        west("whisperwoods:zotzpyre", Habitat.WOODED, TimeRule.NIGHT, RegionalInfluenceBand.ESTABLISHED, 0, 1, 3, 3, 1, 2);
        west("born_in_chaos_v1:seared_spirit", Habitat.WOODED, TimeRule.ANY, RegionalInfluenceBand.ESTABLISHED, 0, 0, 2, 4, 1, 1);
        west("born_in_chaos_v1:pumpkin_bruiser", Habitat.WOODED, TimeRule.ANY, RegionalInfluenceBand.ESTABLISHED, 0, 0, 1, 1, 1, 1);
        west("born_in_chaos_v1:nightmare_stalker", Habitat.WOODED, TimeRule.NIGHT, RegionalInfluenceBand.ESTABLISHED, 0, 0, 1, 1, 1, 1);
        west("born_in_chaos_v1:dire_hound_leader", Habitat.WOODED, TimeRule.NIGHT, RegionalInfluenceBand.ESTABLISHED, 0, 0, 1, 1, 1, 1);
        west("born_in_chaos_v1:lifestealer", Habitat.WOODED, TimeRule.NIGHT, RegionalInfluenceBand.ESTABLISHED, 0, 0, 1, 1, 1, 1);

        // -----------------------------------------------------------------
        // GREENVEIL — abundant fauna, then plants/predators, then sparse apex.
        // -----------------------------------------------------------------
        east("alexsmobs:toucan", Habitat.TROPICAL, TimeRule.DAY, RegionalInfluenceBand.ESTABLISHED, 18, 15, 9, 6, 1, 3);
        east("alexsmobs:capuchin_monkey", Habitat.TROPICAL, TimeRule.DAY, RegionalInfluenceBand.ESTABLISHED, 14, 14, 9, 6, 2, 4);
        east("alexsmobs:leafcutter_ant", Habitat.TROPICAL, TimeRule.DAY, RegionalInfluenceBand.ESTABLISHED, 13, 13, 10, 7, 2, 4);
        east("alexsmobs:anteater", Habitat.TROPICAL, TimeRule.DAY, RegionalInfluenceBand.ESTABLISHED, 7, 8, 6, 4, 1, 2);
        east("alexsmobs:gorilla", Habitat.TROPICAL, TimeRule.DAY, RegionalInfluenceBand.ESTABLISHED, 0, 4, 5, 4, 1, 2);
        east("alexsmobs:caiman", Habitat.WET_TROPICAL, TimeRule.ANY, RegionalInfluenceBand.ESTABLISHED, 0, 6, 8, 7, 1, 2);
        east("alexsmobs:crocodile", Habitat.WET_TROPICAL, TimeRule.ANY, RegionalInfluenceBand.ESTABLISHED, 0, 4, 6, 5, 1, 2);
        east("alexsmobs:anaconda", Habitat.WET_TROPICAL, TimeRule.ANY, RegionalInfluenceBand.ESTABLISHED, 0, 2, 4, 4, 1, 1);
        east("alexsmobs:tiger", Habitat.TROPICAL, TimeRule.ANY, RegionalInfluenceBand.ESTABLISHED, 0, 2, 3, 3, 1, 1);

        east("dungeonsmobs:jungle_zombie", Habitat.TROPICAL, TimeRule.NIGHT, RegionalInfluenceBand.ESTABLISHED, 4, 8, 10, 9, 1, 2);
        east("dungeonsmobs:mossy_skeleton", Habitat.TROPICAL, TimeRule.NIGHT, RegionalInfluenceBand.ESTABLISHED, 3, 7, 9, 8, 1, 2);
        east("skarrier_mobs:zombiflore", Habitat.TROPICAL, TimeRule.ANY, RegionalInfluenceBand.ESTABLISHED, 0, 10, 13, 11, 1, 2);
        east("mowziesmobs:foliaath", Habitat.TROPICAL, TimeRule.ANY, RegionalInfluenceBand.ESTABLISHED, 0, 7, 10, 9, 1, 2);
        east("skarrier_mobs:dangle", Habitat.TROPICAL, TimeRule.DAY, RegionalInfluenceBand.ESTABLISHED, 0, 5, 7, 6, 1, 2);
        east("mowziesmobs:naga", Habitat.TROPICAL, TimeRule.ANY, RegionalInfluenceBand.ESTABLISHED, 0, 2, 3, 3, 1, 1);
        east("skarrier_mobs:carniflore", Habitat.TROPICAL, TimeRule.ANY, RegionalInfluenceBand.ESTABLISHED, 0, 2, 4, 4, 1, 1);
        east("dungeonsmobs:whisperer", Habitat.TROPICAL, TimeRule.NIGHT, RegionalInfluenceBand.ESTABLISHED, 0, 1, 2, 2, 1, 1);
        east("dungeonsmobs:leapleaf", Habitat.TROPICAL, TimeRule.ANY, RegionalInfluenceBand.ESTABLISHED, 0, 0, 1, 1, 1, 1);
        east("born_in_chaos_v1:mother_spider", Habitat.TROPICAL, TimeRule.NIGHT, RegionalInfluenceBand.ESTABLISHED, 0, 0, 1, 1, 1, 1);
        east("skarrier_mobs:slither_matriarch", Habitat.WET_TROPICAL, TimeRule.ANY, RegionalInfluenceBand.ESTABLISHED, 0, 0, 1, 1, 1, 1);

        // -----------------------------------------------------------------
        // SUNSCAR — wildlife-rich but combat-sparse; open terrain/environment do work.
        // -----------------------------------------------------------------
        south("alexsmobs:gazelle", Habitat.DRY_OPEN, TimeRule.DAY, RegionalInfluenceBand.ESTABLISHED, 20, 17, 11, 8, 2, 4);
        south("alexsmobs:roadrunner", Habitat.DRY, TimeRule.DAY, RegionalInfluenceBand.ESTABLISHED, 14, 13, 9, 6, 1, 2);
        south("alexsmobs:jerboa", Habitat.DRY, TimeRule.ANY, RegionalInfluenceBand.ESTABLISHED, 13, 12, 8, 5, 1, 3);
        south("alexsmobs:emu", Habitat.DRY_OPEN, TimeRule.DAY, RegionalInfluenceBand.ESTABLISHED, 11, 10, 7, 5, 1, 3);
        south("alexsmobs:kangaroo", Habitat.DRY_OPEN, TimeRule.ANY, RegionalInfluenceBand.ESTABLISHED, 10, 9, 7, 5, 1, 2);
        south("alexsmobs:maned_wolf", Habitat.DRY_OPEN, TimeRule.ANY, RegionalInfluenceBand.ESTABLISHED, 6, 7, 6, 4, 1, 2);
        south("alexsmobs:elephant", Habitat.DRY_OPEN, TimeRule.DAY, RegionalInfluenceBand.ESTABLISHED, 5, 5, 4, 3, 1, 3);
        south("alexsmobs:rain_frog", Habitat.DRY, TimeRule.ANY, RegionalInfluenceBand.ESTABLISHED, 4, 4, 3, 2, 1, 2);
        south("alexsmobs:rhinoceros", Habitat.DRY_OPEN, TimeRule.ANY, RegionalInfluenceBand.ESTABLISHED, 0, 2, 3, 2, 1, 1);
        south("alexsmobs:rattlesnake", Habitat.DRY, TimeRule.ANY, RegionalInfluenceBand.ESTABLISHED, 0, 6, 7, 6, 1, 2);
        south("alexsmobs:rocky_roller", Habitat.DRY, TimeRule.ANY, RegionalInfluenceBand.ESTABLISHED, 0, 3, 5, 4, 1, 2);
        south("alexsmobs:guster", Habitat.DRY, TimeRule.ANY, RegionalInfluenceBand.ESTABLISHED, 0, 2, 3, 3, 1, 1);
        south("skarrier_mobs:snap", Habitat.DRY, TimeRule.DAY, RegionalInfluenceBand.ESTABLISHED, 0, 5, 6, 5, 1, 2);
        south("born_in_chaos_v1:spirit_guide", Habitat.DRY, TimeRule.ANY, RegionalInfluenceBand.ESTABLISHED, 0, 1, 2, 2, 1, 1);
        south("dungeonsmobs:geomancer", Habitat.DRY, TimeRule.ANY, RegionalInfluenceBand.ESTABLISHED, 0, 1, 2, 2, 1, 1);
        south("skarrier_mobs:trawler", Habitat.DRY, TimeRule.ANY, RegionalInfluenceBand.ESTABLISHED, 0, 0, 1, 1, 1, 1);

        // -----------------------------------------------------------------
        // FROSTMARCH — cold fauna + restrained specialists; no density compensation for weather.
        // -----------------------------------------------------------------
        north("alexsmobs:moose", Habitat.COLD_OR_WOODED, TimeRule.ANY, RegionalInfluenceBand.ESTABLISHED, 14, 13, 9, 5, 1, 3);
        north("alexsmobs:froststalker", Habitat.COLD, TimeRule.ANY, RegionalInfluenceBand.CARDINAL_TRANSITION, 4, 8, 10, 9, 1, 2);
        north("alexsmobs:snow_leopard", Habitat.COLD, TimeRule.ANY, RegionalInfluenceBand.ESTABLISHED, 0, 3, 4, 4, 1, 1);
        north("alexsmobs:tusklin", Habitat.COLD, TimeRule.ANY, RegionalInfluenceBand.ESTABLISHED, 0, 0, 4, 4, 1, 2);
        north("dungeonsmobs:frozen_zombie", Habitat.COLD, TimeRule.NIGHT, RegionalInfluenceBand.ESTABLISHED, 0, 7, 10, 9, 1, 2);
        north("dungeonsmobs:icy_creeper", Habitat.COLD, TimeRule.NIGHT, RegionalInfluenceBand.ESTABLISHED, 0, 3, 5, 5, 1, 1);
        north("dungeonsmobs:iceologer", Habitat.COLD, TimeRule.NIGHT, RegionalInfluenceBand.ESTABLISHED, 0, 2, 3, 3, 1, 1);
        north("dungeonsmobs:mountaineer", Habitat.COLD, TimeRule.ANY, RegionalInfluenceBand.ESTABLISHED, 0, 2, 3, 3, 1, 1);
        north("dungeonsmobs:windcaller", Habitat.COLD, TimeRule.ANY, RegionalInfluenceBand.ESTABLISHED, 0, 0, 2, 3, 1, 1);
    }

    private RegionalSpawnInjector() {}

    /**
     * Called from NaturalSpawner#getRandomSpawnMobAt before vanilla chooses from the static biome list.
     * Empty means "use vanilla's normal candidate" rather than "do not spawn".
     */
    public static Optional<MobSpawnSettings.SpawnerData> tryInject(ServerLevel level,
                                                                   MobCategory category,
                                                                   RandomSource random,
                                                                   BlockPos pos) {
        ACTIVE_SELECTION.remove();
        if (level.dimension() != Level.OVERWORLD) return Optional.empty();
        if (category != MobCategory.CREATURE && category != MobCategory.MONSTER) return Optional.empty();

        RegionalCell cell = CozyZonesApi.regionalCellAt(level, pos.getX() + 0.5D, pos.getZ() + 0.5D);
        if (cell.influenceBand() == RegionalInfluenceBand.SHARED_CORE) return Optional.empty();

        // Do not spend creature replacement attempts deep underground. Monster geography can still
        // color caves naturally; the friendly/wildlife layer is meant to read on the surface.
        if (category == MobCategory.CREATURE && pos.getY() < level.getSeaLevel() - 12) return Optional.empty();

        float opportunity = opportunityChance(cell, category);
        if (opportunity <= 0.0f || random.nextFloat() >= opportunity) return Optional.empty();

        ResourceLocation biomeId = level.getBiome(pos).unwrapKey().map(key -> key.location()).orElse(null);
        String biomePath = biomeId == null ? "" : biomeId.getPath().toLowerCase(Locale.ROOT);
        boolean day = level.isDay();

        List<ResolvedCandidate> eligible = new ArrayList<>();
        int totalWeight = 0;
        for (Candidate candidate : CANDIDATES.get(cell.macroRegion())) {
            int weight = candidate.weight(cell.radialZone());
            if (weight <= 0) continue;
            if (!candidate.minimumInfluence().allows(cell.influenceBand())) continue;
            if (!candidate.timeRule().allows(day)) continue;
            if (!candidate.habitat().matches(biomePath)) continue;

            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(candidate.id()).orElse(null);
            if (type == null || type.getCategory() != category || !type.canSummon()) continue;
            if (!CozyZonesApi.naturalEntityAllowed(level, candidate.id(), pos.getX(), pos.getZ())) continue;
            if (!DangerBibleSpawnPolicy.permitsInjectedSelection(level, candidate.id(), pos.getX(), pos.getZ())) continue;

            eligible.add(new ResolvedCandidate(candidate, type, weight));
            totalWeight += weight;
        }

        if (eligible.isEmpty() || totalWeight <= 0) return Optional.empty();
        int roll = random.nextInt(totalWeight);
        ResolvedCandidate selected = eligible.get(eligible.size() - 1);
        for (ResolvedCandidate candidate : eligible) {
            roll -= candidate.weight();
            if (roll < 0) {
                selected = candidate;
                break;
            }
        }

        Candidate definition = selected.definition();
        MobSpawnSettings.SpawnerData data = new MobSpawnSettings.SpawnerData(
                selected.type(), 1, definition.minCount(), definition.maxCount()
        );
        ACTIVE_SELECTION.set(new InjectionToken(level, selected.type(), level.getGameTime(), pos.immutable()));
        return Optional.of(data);
    }

    /** Vanilla validates that a selected SpawnerData is literally present in the static biome list.
     * Our dynamically selected entries are intentionally not, so the NaturalSpawner mixin bypasses
     * only that membership check for the active selection. All subsequent spawn checks still run. */
    public static boolean matchesInjected(ServerLevel level, EntityType<?> type, BlockPos pos) {
        InjectionToken token = ACTIVE_SELECTION.get();
        if (token == null || token.level() != level || token.type() != type || token.gameTime() != level.getGameTime()) return false;
        return Math.abs(pos.getX() - token.origin().getX()) <= TOKEN_RADIUS
                && Math.abs(pos.getY() - token.origin().getY()) <= TOKEN_RADIUS
                && Math.abs(pos.getZ() - token.origin().getZ()) <= TOKEN_RADIUS;
    }

    public static void clearThreadSelection() {
        ACTIVE_SELECTION.remove();
    }

    private static float opportunityChance(RegionalCell cell, MobCategory category) {
        float base = switch (category) {
            case CREATURE -> switch (cell.radialZone()) {
                case HEARTHLANDS -> 0.34f;
                case FRONTIER -> 0.36f;
                case WILDLANDS -> 0.30f;
                case DREAD_REACHES -> 0.22f;
            };
            case MONSTER -> switch (cell.radialZone()) {
                case HEARTHLANDS -> 0.11f;
                case FRONTIER -> 0.23f;
                case WILDLANDS -> 0.30f;
                case DREAD_REACHES -> 0.32f;
            };
            default -> 0.0f;
        };

        // Organic macro borders should blend rather than instantly replacing one themed population
        // with another. Regional strength also eases the authored tables into the 700-1200 transition.
        double strength = Math.max(0.20D, cell.regionalStrength());
        double border = 0.45D + 0.55D * cell.macroBoundaryStrength();
        return (float) Math.min(0.45D, base * strength * border);
    }

    private static void west(String id, Habitat habitat, TimeRule time, RegionalInfluenceBand influence,
                             int h, int f, int w, int d, int min, int max) {
        add(MacroRegion.WEST, id, habitat, time, influence, h, f, w, d, min, max);
    }

    private static void east(String id, Habitat habitat, TimeRule time, RegionalInfluenceBand influence,
                             int h, int f, int w, int d, int min, int max) {
        add(MacroRegion.EAST, id, habitat, time, influence, h, f, w, d, min, max);
    }

    private static void south(String id, Habitat habitat, TimeRule time, RegionalInfluenceBand influence,
                              int h, int f, int w, int d, int min, int max) {
        add(MacroRegion.SOUTH, id, habitat, time, influence, h, f, w, d, min, max);
    }

    private static void north(String id, Habitat habitat, TimeRule time, RegionalInfluenceBand influence,
                              int h, int f, int w, int d, int min, int max) {
        add(MacroRegion.NORTH, id, habitat, time, influence, h, f, w, d, min, max);
    }

    private static void add(MacroRegion region,
                            String id,
                            Habitat habitat,
                            TimeRule time,
                            RegionalInfluenceBand influence,
                            int hearthlands,
                            int frontier,
                            int wildlands,
                            int dread,
                            int minCount,
                            int maxCount) {
        CANDIDATES.get(region).add(new Candidate(
                new ResourceLocation(id), habitat, time, influence,
                hearthlands, frontier, wildlands, dread, minCount, maxCount
        ));
    }

    private record Candidate(ResourceLocation id,
                             Habitat habitat,
                             TimeRule timeRule,
                             RegionalInfluenceBand minimumInfluence,
                             int hearthlandsWeight,
                             int frontierWeight,
                             int wildlandsWeight,
                             int dreadWeight,
                             int minCount,
                             int maxCount) {
        int weight(Region region) {
            return switch (region) {
                case HEARTHLANDS -> hearthlandsWeight;
                case FRONTIER -> frontierWeight;
                case WILDLANDS -> wildlandsWeight;
                case DREAD_REACHES -> dreadWeight;
            };
        }
    }

    private record ResolvedCandidate(Candidate definition, EntityType<?> type, int weight) {}
    private record InjectionToken(ServerLevel level, EntityType<?> type, long gameTime, BlockPos origin) {}

    private enum TimeRule {
        ANY,
        DAY,
        NIGHT;

        boolean allows(boolean day) {
            return this == ANY || (this == DAY && day) || (this == NIGHT && !day);
        }
    }

    private enum Habitat {
        ANY_LAND,
        OPEN,
        WOODED,
        CULTIVATED,
        CULTIVATED_OR_WOODED,
        TROPICAL,
        WET_TROPICAL,
        DRY,
        DRY_OPEN,
        COLD,
        COLD_OR_WOODED;

        boolean matches(String biome) {
            boolean open = containsAny(biome, "plains", "meadow", "field", "grassland", "pasture", "prairie", "shrub", "scrub", "steppe");
            boolean wooded = containsAny(biome, "forest", "woods", "woodland", "grove", "orchard", "maple", "seasonal", "redwood", "taiga", "conifer", "ominous");
            boolean cultivated = containsAny(biome, "field", "pasture", "plains", "meadow", "orchard", "pumpkin", "grassland");
            boolean tropical = containsAny(biome, "jungle", "rainforest", "tropic", "mangrove", "bamboo", "bayou", "overgrown", "lush");
            boolean wet = containsAny(biome, "river", "swamp", "marsh", "mangrove", "bayou", "wetland");
            boolean dry = containsAny(biome, "desert", "savanna", "badlands", "mesa", "scrub", "dune", "dry", "steppe");
            boolean cold = containsAny(biome, "snow", "frozen", "ice", "taiga", "conifer", "alpine", "grove");

            return switch (this) {
                case ANY_LAND -> true;
                case OPEN -> open;
                case WOODED -> wooded;
                case CULTIVATED -> cultivated;
                case CULTIVATED_OR_WOODED -> cultivated || wooded;
                case TROPICAL -> tropical;
                case WET_TROPICAL -> tropical && wet;
                case DRY -> dry;
                case DRY_OPEN -> dry || open;
                case COLD -> cold;
                case COLD_OR_WOODED -> cold || wooded;
            };
        }

        private static boolean containsAny(String value, String... needles) {
            for (String needle : needles) if (value.contains(needle)) return true;
            return false;
        }
    }
}
