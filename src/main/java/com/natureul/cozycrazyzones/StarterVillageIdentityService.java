package com.natureul.cozycrazyzones;

import com.natureul.cozycrazyzones.mixin.MapItemSavedDataAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Gives each authored starter settlement one logical identity even if vanilla also creates a nearby
 * village structure start. The starter survey works from reserved anchor chunks, while physical
 * discovery works from StructureStart chunks; without this bridge those two coordinate systems can
 * assign two different names to what looks like one continuous village on the Atlas.
 */
public final class StarterVillageIdentityService {
    /**
     * A second start this close belongs to the same visible starter settlement. 192 blocks is wide
     * enough for jigsaw village spillover/forced-anchor mismatch without swallowing a genuinely
     * separate vanilla village several hundred blocks down the road.
     */
    public static final int STARTER_SETTLEMENT_RADIUS = 192;

    private static final ResourceLocation ATLAS_ID = new ResourceLocation("map_atlases", "atlas");
    private static final String KNOWN_TAG = "cozycrazyzones:known_atlas_markers";
    private static final String PENDING_TAG = "cozycrazyzones:pending_atlas_markers";
    private static final String DISCOVERED_TAG = "cozycrazyzones:discovered_structures";
    private static final String RETIRED_TAG = "cozycrazyzones:retired_atlas_marker_keys";
    private static final String REPAIR_VERSION_TAG = "CozyCrazyZonesStarterVillageIdentityVersion";
    private static final int REPAIR_VERSION = 1;

    private StarterVillageIdentityService() {}

    public record CanonicalVillage(ChunkPos start, MacroRegion region, boolean starter) {}

    /**
     * Resolve a physical village StructureStart to the reserved starter identity when it belongs to
     * the same authored settlement cluster. Existing alias state is retired immediately.
     */
    public static CanonicalVillage canonicalizePhysical(ServerPlayer player, ChunkPos actualStart) {
        ServerLevel level = player.serverLevel();
        CanonicalVillage canonical = canonicalFor(level, actualStart);
        if (!canonical.starter() || canonical.start().equals(actualStart)) return canonical;

        VillageNameSavedData.get(level).bindAlias(actualStart, canonical.start());
        retireAliasState(player, VillageNameSavedData.keyFor(actualStart), VillageNameSavedData.keyFor(canonical.start()));
        CozyCrazyZones.LOGGER.info(
                "Canonicalized nearby village start {},{} to reserved {} starter settlement {},{}",
                actualStart.x, actualStart.z,
                canonical.region().displayName(),
                canonical.start().x, canonical.start().z
        );
        return canonical;
    }

    /** Returns the reserved starter identity for a nearby village start, otherwise the start itself. */
    public static CanonicalVillage canonicalFor(ServerLevel level, ChunkPos actualStart) {
        Map<MacroRegion, ChunkPos> targets = targets(level);
        if (targets.isEmpty()) {
            RegionalCell cell = CozyZonesApi.regionalCellAt(level, actualStart.getMiddleBlockX(), actualStart.getMiddleBlockZ());
            return new CanonicalVillage(actualStart, cell.macroRegion(), false);
        }

        RegionalCell actualCell = CozyZonesApi.regionalCellAt(level, actualStart.getMiddleBlockX(), actualStart.getMiddleBlockZ());
        long maxSq = (long) STARTER_SETTLEMENT_RADIUS * STARTER_SETTLEMENT_RADIUS;
        MacroRegion bestRegion = null;
        ChunkPos best = null;
        long bestSq = Long.MAX_VALUE;

        for (var entry : targets.entrySet()) {
            // Starter anchors are deliberately placed in established cardinal country. Requiring
            // the same macro-region prevents a separate village across a regional seam from being
            // silently swallowed by the starter settlement.
            if (entry.getKey() != actualCell.macroRegion()) continue;
            ChunkPos target = entry.getValue();
            long dx = actualStart.getMiddleBlockX() - (long) target.getMiddleBlockX();
            long dz = actualStart.getMiddleBlockZ() - (long) target.getMiddleBlockZ();
            long distSq = dx * dx + dz * dz;
            if (distSq <= maxSq && distSq < bestSq) {
                bestSq = distSq;
                bestRegion = entry.getKey();
                best = target;
            }
        }

        return best == null
                ? new CanonicalVillage(actualStart, actualCell.macroRegion(), false)
                : new CanonicalVillage(best, bestRegion, true);
    }

    /** True when a non-reserved vanilla village candidate would visibly crowd a starter settlement. */
    public static boolean crowdsReservedStarter(ServerLevel level, ChunkPos candidate) {
        Map<MacroRegion, ChunkPos> targets = targets(level);
        if (targets.isEmpty()) return false;
        long maxSq = (long) STARTER_SETTLEMENT_RADIUS * STARTER_SETTLEMENT_RADIUS;
        for (ChunkPos target : targets.values()) {
            if (target.equals(candidate)) return false;
            long dx = candidate.getMiddleBlockX() - (long) target.getMiddleBlockX();
            long dz = candidate.getMiddleBlockZ() - (long) target.getMiddleBlockZ();
            if (dx * dx + dz * dz <= maxSq) return true;
        }
        return false;
    }

    /**
     * Cheap once-per-world/player migration plus deferred map-tile cleanup. Called on the existing
     * one-second discovery cadence; after migration it is effectively a pair of integer/tag checks.
     */
    public static void tick(ServerPlayer player) {
        if (player.serverLevel().dimension() != Level.OVERWORLD) return;
        if (player.getPersistentData().getInt(REPAIR_VERSION_TAG) < REPAIR_VERSION) {
            repairPersistentAliases(player);
            player.getPersistentData().putInt(REPAIR_VERSION_TAG, REPAIR_VERSION);
        }
        cleanupRetiredDecorations(player);
    }

    public static void repairNow(ServerPlayer player) {
        if (player.serverLevel().dimension() != Level.OVERWORLD) return;
        repairPersistentAliases(player);
        player.getPersistentData().putInt(REPAIR_VERSION_TAG, REPAIR_VERSION);
        cleanupRetiredDecorations(player);
    }

    /** Preserve one-time repair/cleanup state across the Forge player clone used by death/respawn. */
    public static void copyPersistentState(ServerPlayer original, ServerPlayer replacement) {
        int version = original.getPersistentData().getInt(REPAIR_VERSION_TAG);
        if (version > 0) replacement.getPersistentData().putInt(REPAIR_VERSION_TAG, version);
        CompoundTag retired = original.getPersistentData().getCompound(RETIRED_TAG);
        if (!retired.isEmpty()) replacement.getPersistentData().put(RETIRED_TAG, retired.copy());
    }

    private static void repairPersistentAliases(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Map<MacroRegion, ChunkPos> targets = targets(level);
        if (targets.size() != MacroRegion.values().length) return;

        CompoundTag known = player.getPersistentData().getCompound(KNOWN_TAG);
        CompoundTag pending = player.getPersistentData().getCompound(PENDING_TAG);
        CompoundTag discovered = player.getPersistentData().getCompound(DISCOVERED_TAG);
        Set<String> keys = new HashSet<>();
        keys.addAll(known.getAllKeys());
        keys.addAll(pending.getAllKeys());
        keys.addAll(discovered.getAllKeys());

        VillageNameSavedData names = VillageNameSavedData.get(level);
        List<Install> installs = new ArrayList<>();
        int repaired = 0;

        for (String aliasKey : keys) {
            ChunkPos aliasStart = parseVillageKey(aliasKey);
            if (aliasStart == null) continue;
            CanonicalVillage canonical = canonicalFor(level, aliasStart);
            if (!canonical.starter() || canonical.start().equals(aliasStart)) continue;

            String canonicalKey = VillageNameSavedData.keyFor(canonical.start());
            names.bindAlias(aliasStart, canonical.start());

            CompoundTag oldMarker = known.contains(aliasKey) ? known.getCompound(aliasKey).copy()
                    : pending.contains(aliasKey) ? pending.getCompound(aliasKey).copy()
                    : null;
            boolean wasDiscovered = discovered.getBoolean(aliasKey);

            known.remove(aliasKey);
            pending.remove(aliasKey);
            discovered.remove(aliasKey);
            if (wasDiscovered) discovered.putBoolean(canonicalKey, true);
            rememberRetired(player, aliasKey);

            if (!known.contains(canonicalKey) && !pending.contains(canonicalKey)) {
                String canonicalName = names.getOrAssign(canonical.region(), level.getSeed(), canonical.start());
                BlockPos markerPos = oldMarker != null
                        ? new BlockPos(oldMarker.getInt("X"), level.getSharedSpawnPos().getY(), oldMarker.getInt("Z"))
                        : new BlockPos(canonical.start().getMiddleBlockX(), level.getSharedSpawnPos().getY(), canonical.start().getMiddleBlockZ());
                installs.add(new Install(
                        canonicalKey,
                        canonicalName,
                        markerPos,
                        RegionalMapSymbolPolicy.regionalBanner(canonical.region())
                ));
            }
            repaired++;
        }

        player.getPersistentData().put(KNOWN_TAG, known);
        player.getPersistentData().put(PENDING_TAG, pending);
        player.getPersistentData().put(DISCOVERED_TAG, discovered);
        for (Install install : installs) {
            AtlasDiscoveryMarkerService.enqueue(
                    player,
                    install.key(),
                    DiscoveryCategory.VILLAGE,
                    install.name(),
                    install.pos(),
                    install.icon()
            );
        }

        if (repaired > 0) {
            CozyCrazyZones.LOGGER.info(
                    "Repaired {} starter-village alias marker(s) for {}",
                    repaired, player.getGameProfile().getName()
            );
        }
    }

    private static void retireAliasState(ServerPlayer player, String aliasKey, String canonicalKey) {
        CompoundTag known = player.getPersistentData().getCompound(KNOWN_TAG);
        CompoundTag pending = player.getPersistentData().getCompound(PENDING_TAG);
        CompoundTag discovered = player.getPersistentData().getCompound(DISCOVERED_TAG);

        known.remove(aliasKey);
        pending.remove(aliasKey);
        if (discovered.getBoolean(aliasKey)) discovered.putBoolean(canonicalKey, true);
        discovered.remove(aliasKey);
        rememberRetired(player, aliasKey);

        player.getPersistentData().put(KNOWN_TAG, known);
        player.getPersistentData().put(PENDING_TAG, pending);
        player.getPersistentData().put(DISCOVERED_TAG, discovered);
    }

    private static void rememberRetired(ServerPlayer player, String key) {
        CompoundTag retired = player.getPersistentData().getCompound(RETIRED_TAG);
        retired.putBoolean(key, true);
        player.getPersistentData().put(RETIRED_TAG, retired);
    }

    /** Remove old alias decorations from every vanilla map tile held by the player's Atlas. */
    private static void cleanupRetiredDecorations(ServerPlayer player) {
        CompoundTag retired = player.getPersistentData().getCompound(RETIRED_TAG);
        if (retired.isEmpty() || !ModList.get().isLoaded("map_atlases")) return;
        ItemStack atlas = findAtlas(player);
        if (atlas == null) return;

        try {
            Class<?> atlasItemClass = Class.forName("pepjebs.mapatlases.item.MapAtlasItem");
            Method getMaps = atlasItemClass.getMethod("getMaps", ItemStack.class, Level.class);
            Object collection = getMaps.invoke(null, atlas, player.serverLevel());
            if (collection == null) return;
            Method getAllIds = collection.getClass().getMethod("getAllIds");
            int[] ids = (int[]) getAllIds.invoke(collection);

            for (String key : Set.copyOf(retired.getAllKeys())) {
                String decorationId = decorationId(key);
                for (int id : ids) {
                    MapItemSavedData data = MapItem.getSavedData(id, player.serverLevel());
                    if (data == null) continue;
                    MapItemSavedDataAccessor accessor = (MapItemSavedDataAccessor) (Object) data;
                    if (accessor.cozyzones$getDecorations().remove(decorationId) != null) data.setDirty();
                }
                retired.remove(key);
            }
            player.getPersistentData().put(RETIRED_TAG, retired);
        } catch (Throwable ex) {
            CozyCrazyZones.LOGGER.debug("Deferred starter-village Atlas alias cleanup will retry", ex);
        }
    }

    private static Map<MacroRegion, ChunkPos> targets(ServerLevel level) {
        if (!WorldGeographyContext.prepared()) return Map.of();
        return VillageRingPlanner.targetsFor(
                level,
                level.getChunkSource().getGenerator(),
                level.getChunkSource().getGeneratorState(),
                level.registryAccess()
        );
    }

    @Nullable
    private static ChunkPos parseVillageKey(String key) {
        if (key == null || !key.startsWith("village@")) return null;
        String coords = key.substring("village@".length());
        int comma = coords.indexOf(',');
        if (comma <= 0 || comma + 1 >= coords.length()) return null;
        try {
            return new ChunkPos(
                    Integer.parseInt(coords.substring(0, comma)),
                    Integer.parseInt(coords.substring(comma + 1))
            );
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Nullable
    private static ItemStack findAtlas(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (isAtlas(stack)) return stack;
        }
        ItemStack offhand = player.getOffhandItem();
        return isAtlas(offhand) ? offhand : null;
    }

    private static boolean isAtlas(ItemStack stack) {
        return stack != null && !stack.isEmpty() && ATLAS_ID.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }

    private static String decorationId(String discoveryKey) {
        return "cozyzones_" + Integer.toUnsignedString(discoveryKey.hashCode(), 36);
    }

    private record Install(
            String key,
            String name,
            BlockPos pos,
            net.minecraft.world.level.saveddata.maps.MapDecoration.Type icon
    ) {}
}
