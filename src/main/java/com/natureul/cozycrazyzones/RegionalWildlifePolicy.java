package com.natureul.cozycrazyzones;

import net.minecraft.resources.ResourceLocation;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Cardinal wildlife firewall independent of the visible biome's inherited Forge tags.
 *
 * CozyCrazyZones can remap a TerraBlender/native savanna or jungle into a neutral-looking final
 * Hearthlands biome. Biome modifiers may already have populated the old biome's creature list,
 * including CHUNK_GENERATION animals. This table is therefore geography-authoritative for strongly
 * regional wildlife and is evaluated for both runtime NATURAL and initial CHUNK_GENERATION spawns.
 */
public final class RegionalWildlifePolicy {
    private static final Map<ResourceLocation, WildlifeRule> RULES = new HashMap<>();

    static {
        // Sunscar warm/dry fauna. Keep these completely out of the neutral home core.
        regional("alexsmobs:gazelle", RegionalInfluenceBand.ESTABLISHED, Region.HEARTHLANDS, MacroRegion.SOUTH);
        regional("alexsmobs:elephant", RegionalInfluenceBand.ESTABLISHED, Region.HEARTHLANDS, MacroRegion.SOUTH);
        regional("alexsmobs:emu", RegionalInfluenceBand.ESTABLISHED, Region.HEARTHLANDS, MacroRegion.SOUTH);
        regional("alexsmobs:kangaroo", RegionalInfluenceBand.ESTABLISHED, Region.HEARTHLANDS, MacroRegion.SOUTH);
        regional("alexsmobs:maned_wolf", RegionalInfluenceBand.ESTABLISHED, Region.HEARTHLANDS, MacroRegion.SOUTH);
        regional("alexsmobs:jerboa", RegionalInfluenceBand.ESTABLISHED, Region.HEARTHLANDS, MacroRegion.SOUTH);
        regional("alexsmobs:roadrunner", RegionalInfluenceBand.ESTABLISHED, Region.HEARTHLANDS, MacroRegion.SOUTH);
        regional("alexsmobs:rain_frog", RegionalInfluenceBand.ESTABLISHED, Region.HEARTHLANDS, MacroRegion.SOUTH);
        regional("alexsmobs:triops", RegionalInfluenceBand.ESTABLISHED, Region.HEARTHLANDS, MacroRegion.SOUTH);
        regional("alexsmobs:rhinoceros", RegionalInfluenceBand.ESTABLISHED, Region.FRONTIER, MacroRegion.SOUTH);
        regional("alexsmobs:rattlesnake", RegionalInfluenceBand.ESTABLISHED, Region.FRONTIER, MacroRegion.SOUTH);
        regional("alexsmobs:rocky_roller", RegionalInfluenceBand.ESTABLISHED, Region.FRONTIER, MacroRegion.SOUTH);
        regional("alexsmobs:guster", RegionalInfluenceBand.ESTABLISHED, Region.FRONTIER, MacroRegion.SOUTH);
        regional("alexsmobs:tarantula_hawk", RegionalInfluenceBand.ESTABLISHED, Region.FRONTIER, MacroRegion.SOUTH);
        regional("alexsmobs:sunbird", RegionalInfluenceBand.ESTABLISHED, Region.WILDLANDS, MacroRegion.SOUTH);

        // Greenveil tropical/wet fauna. The first four are valid Hearthlands identity once the east
        // is established; serious predators begin at Frontier.
        regional("alexsmobs:toucan", RegionalInfluenceBand.ESTABLISHED, Region.HEARTHLANDS, MacroRegion.EAST);
        regional("alexsmobs:capuchin_monkey", RegionalInfluenceBand.ESTABLISHED, Region.HEARTHLANDS, MacroRegion.EAST);
        regional("alexsmobs:leafcutter_ant", RegionalInfluenceBand.ESTABLISHED, Region.HEARTHLANDS, MacroRegion.EAST);
        regional("alexsmobs:anteater", RegionalInfluenceBand.ESTABLISHED, Region.HEARTHLANDS, MacroRegion.EAST);
        regional("alexsmobs:gorilla", RegionalInfluenceBand.ESTABLISHED, Region.FRONTIER, MacroRegion.EAST);
        regional("alexsmobs:tiger", RegionalInfluenceBand.ESTABLISHED, Region.FRONTIER, MacroRegion.EAST);
        regional("alexsmobs:anaconda", RegionalInfluenceBand.ESTABLISHED, Region.FRONTIER, MacroRegion.EAST);
        regional("alexsmobs:caiman", RegionalInfluenceBand.ESTABLISHED, Region.FRONTIER, MacroRegion.EAST);
        regional("alexsmobs:crocodile", RegionalInfluenceBand.ESTABLISHED, Region.FRONTIER, MacroRegion.EAST);
        regional("alexsmobs:shoebill", RegionalInfluenceBand.ESTABLISHED, Region.FRONTIER, MacroRegion.EAST);
        regional("alexsmobs:mudskipper", RegionalInfluenceBand.ESTABLISHED, Region.FRONTIER, MacroRegion.EAST);

        // Frostmarch / cool old-growth fauna.
        regional("alexsmobs:froststalker", RegionalInfluenceBand.CARDINAL_TRANSITION, Region.HEARTHLANDS, MacroRegion.NORTH);
        regional("alexsmobs:snow_leopard", RegionalInfluenceBand.ESTABLISHED, Region.FRONTIER, MacroRegion.NORTH);
        regional("alexsmobs:tusklin", RegionalInfluenceBand.ESTABLISHED, Region.WILDLANDS, MacroRegion.NORTH);
        regional("alexsmobs:moose", RegionalInfluenceBand.ESTABLISHED, Region.HEARTHLANDS, MacroRegion.NORTH, MacroRegion.WEST);
        regional("alexsmobs:grizzly_bear", RegionalInfluenceBand.ESTABLISHED, Region.FRONTIER, MacroRegion.NORTH, MacroRegion.WEST);

        // Skunk is particularly useful as Harvestwood rural ecology, with a little Greenveil-edge
        // compatibility. Crows/raccoons/blue jays/hummingbirds remain genuinely common-core fauna
        // and are intentionally not geography-blocked here.
        regional("alexsmobs:skunk", RegionalInfluenceBand.CARDINAL_TRANSITION, Region.HEARTHLANDS, MacroRegion.WEST, MacroRegion.EAST);
    }

    private RegionalWildlifePolicy() {}

    public static boolean allows(ResourceLocation id, RegionalCell cell) {
        WildlifeRule rule = RULES.get(id);
        return rule == null || rule.allows(cell);
    }

    public static boolean isManaged(ResourceLocation id) {
        return RULES.containsKey(id);
    }

    private static void regional(String id,
                                 RegionalInfluenceBand minimumInfluence,
                                 Region minimumRegion,
                                 MacroRegion... regions) {
        RULES.put(new ResourceLocation(id), new WildlifeRule(enumSet(regions), minimumInfluence, minimumRegion));
    }

    private static Set<MacroRegion> enumSet(MacroRegion... regions) {
        EnumSet<MacroRegion> set = EnumSet.noneOf(MacroRegion.class);
        for (MacroRegion region : regions) set.add(region);
        return Set.copyOf(set);
    }

    private record WildlifeRule(Set<MacroRegion> regions,
                                RegionalInfluenceBand minimumInfluence,
                                Region minimumRegion) {
        boolean allows(RegionalCell cell) {
            if (cell.radialZone().tier() < minimumRegion.tier()) return false;
            if (!cell.influenceBand().atLeast(minimumInfluence)) return false;
            return regions.contains(cell.macroRegion());
        }
    }
}
