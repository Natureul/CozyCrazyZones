package com.natureul.cozycrazyzones;

import com.natureul.cozycrazyzones.mixin.MapItemSavedDataAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Persistent discovered-place layer for Map Atlases.
 *
 * Map Atlases uses vanilla MapItemSavedData tiles, so CozyCrazyZones can give categories genuinely
 * different symbols without bundling a second map renderer. The compact known-place ledger also
 * lets us migrate old marker styles and keep one logical place from appearing on multiple Atlas
 * tiles when a starter marker is later refined by physical discovery.
 */
public final class AtlasDiscoveryMarkerService {
    private static final ResourceLocation ATLAS_ID = new ResourceLocation("map_atlases", "atlas");
    private static final ResourceLocation TUNNEL_GORE_LAIR = new ResourceLocation("skarrier_mobs", "tunnel_gore_lair_x");
    private static final String TUNNEL_GORE_KEY_PREFIX = "structure@skarrier_mobs:tunnel_gore_lair_x@";
    private static final String PENDING_TAG = "cozycrazyzones:pending_atlas_markers";
    private static final String KNOWN_TAG = "cozycrazyzones:known_atlas_markers";
    private static final byte DEFAULT_SCALE = 2;
    private static final int REFRESH_BATCH = 8;

    private static final Map<UUID, Integer> REFRESH_CURSOR = new HashMap<>();
    private static final Map<UUID, Set<String>> DEDUPE_SEEN = new HashMap<>();

    private AtlasDiscoveryMarkerService() {}

    public static void enqueue(ServerPlayer player,
                               String discoveryKey,
                               DiscoveryCategory category,
                               String name,
                               BlockPos pos) {
        enqueue(player, discoveryKey, category, name, pos, category.defaultDecorationType());
    }

    public static void enqueue(ServerPlayer player,
                               String discoveryKey,
                               DiscoveryCategory category,
                               String name,
                               BlockPos pos,
                               MapDecoration.Type icon) {
        CompoundTag marker = markerTag(category, name, pos, icon);

        CompoundTag known = player.getPersistentData().getCompound(KNOWN_TAG);
        known.put(discoveryKey, marker.copy());
        player.getPersistentData().put(KNOWN_TAG, known);

        CompoundTag pending = player.getPersistentData().getCompound(PENDING_TAG);
        pending.put(discoveryKey, marker);
        player.getPersistentData().put(PENDING_TAG, pending);
    }

    public static boolean hasKnownMarker(ServerPlayer player, String discoveryKey) {
        return player.getPersistentData().getCompound(KNOWN_TAG).contains(discoveryKey);
    }

    @Nullable
    public static String knownMarkerName(ServerPlayer player, String discoveryKey) {
        CompoundTag known = player.getPersistentData().getCompound(KNOWN_TAG);
        if (!known.contains(discoveryKey)) return null;
        String name = known.getCompound(discoveryKey).getString("Name");
        return name.isBlank() ? null : name;
    }

    public static void tick(ServerPlayer player) {
        if (player.serverLevel().dimension() != Level.OVERWORLD || !ModList.get().isLoaded("map_atlases")) return;
        ItemStack atlas = findAtlas(player);
        if (atlas == null) return;

        ServerLevel level = player.serverLevel();
        CompoundTag pending = player.getPersistentData().getCompound(PENDING_TAG);
        String pendingKey = pending.getAllKeys().stream().findFirst().orElse(null);
        if (pendingKey != null) {
            CompoundTag marker = pending.getCompound(pendingKey);
            normalizeMarkerStyle(level, pendingKey, marker);
            syncKnownCopy(player, pendingKey, marker);
            if (installMarker(level, atlas, pendingKey, marker, true, true, true)) {
                pending.remove(pendingKey);
                player.getPersistentData().put(PENDING_TAG, pending);
                DEDUPE_SEEN.computeIfAbsent(player.getUUID(), ignored -> new HashSet<>()).add(pendingKey);
            }
        }

        refreshKnownBatch(player, atlas);
    }

    public static void copyPersistentState(ServerPlayer original, ServerPlayer replacement) {
        CompoundTag pending = original.getPersistentData().getCompound(PENDING_TAG);
        if (!pending.isEmpty()) replacement.getPersistentData().put(PENDING_TAG, pending.copy());
        CompoundTag known = original.getPersistentData().getCompound(KNOWN_TAG);
        if (!known.isEmpty()) replacement.getPersistentData().put(KNOWN_TAG, known.copy());
    }

    public static void removeRuntimeState(ServerPlayer player) {
        REFRESH_CURSOR.remove(player.getUUID());
        DEDUPE_SEEN.remove(player.getUUID());
    }

    private static CompoundTag markerTag(DiscoveryCategory category,
                                         String name,
                                         BlockPos pos,
                                         MapDecoration.Type icon) {
        CompoundTag marker = new CompoundTag();
        marker.putString("Category", category.name());
        marker.putString("Name", name);
        marker.putString("Icon", icon.name());
        marker.putInt("X", pos.getX());
        marker.putInt("Z", pos.getZ());
        return marker;
    }

    private static void refreshKnownBatch(ServerPlayer player, ItemStack atlas) {
        CompoundTag known = player.getPersistentData().getCompound(KNOWN_TAG);
        if (known.isEmpty()) return;

        List<String> keys = new ArrayList<>(known.getAllKeys());
        keys.sort(String::compareTo);
        int cursor = Math.floorMod(REFRESH_CURSOR.getOrDefault(player.getUUID(), 0), keys.size());
        int count = Math.min(REFRESH_BATCH, keys.size());
        boolean changed = false;
        Set<String> cleaned = DEDUPE_SEEN.computeIfAbsent(player.getUUID(), ignored -> new HashSet<>());

        for (int i = 0; i < count; i++) {
            String key = keys.get((cursor + i) % keys.size());
            CompoundTag marker = known.getCompound(key);
            boolean styleChanged = normalizeMarkerStyle(player.serverLevel(), key, marker);
            if (styleChanged) {
                known.put(key, marker.copy());
                changed = true;
            }
            boolean firstCleanup = cleaned.add(key);
            installMarker(player.serverLevel(), atlas, key, marker, false, false, firstCleanup || styleChanged);
        }
        if (changed) player.getPersistentData().put(KNOWN_TAG, known);
        REFRESH_CURSOR.put(player.getUUID(), (cursor + count) % keys.size());
    }

    private static boolean normalizeMarkerStyle(ServerLevel level, String discoveryKey, CompoundTag marker) {
        if (marker == null || marker.isEmpty()) return false;
        boolean changed = migrateSpecialMarker(level, discoveryKey, marker);

        DiscoveryCategory category;
        try {
            category = DiscoveryCategory.valueOf(marker.getString("Category"));
        } catch (IllegalArgumentException ex) {
            return changed;
        }

        int x = marker.getInt("X");
        int z = marker.getInt("Z");
        RegionalCell cell = CozyZonesApi.regionalCellAt(level, x, z);
        MapDecoration.Type wanted = RegionalMapSymbolPolicy.iconForCategory(category, cell);
        if (!wanted.name().equals(marker.getString("Icon"))) {
            marker.putString("Icon", wanted.name());
            changed = true;
        }
        return changed;
    }

    private static boolean migrateSpecialMarker(ServerLevel level, String discoveryKey, CompoundTag marker) {
        if (discoveryKey == null || !discoveryKey.startsWith(TUNNEL_GORE_KEY_PREFIX)) return false;

        ChunkPos start = parseStructureStart(discoveryKey);
        if (start == null) return false;

        int x = marker.getInt("X");
        int z = marker.getInt("Z");
        RegionalCell cell = CozyZonesApi.regionalCellAt(level, x, z);
        StructureDiscoveryProfile profile = new StructureDiscoveryProfile(
                DiscoveryCategory.MINE,
                "Unusual Tunnels",
                MapDecoration.Type.BANNER_GRAY,
                false
        );
        String wantedName = StructureNameSavedData.get(level).getOrAssign(
                profile, cell, level.getSeed(), TUNNEL_GORE_LAIR, start
        );

        boolean changed = false;
        if (!DiscoveryCategory.MINE.name().equals(marker.getString("Category"))) {
            marker.putString("Category", DiscoveryCategory.MINE.name());
            changed = true;
        }
        if (!wantedName.equals(marker.getString("Name"))) {
            marker.putString("Name", wantedName);
            changed = true;
        }
        return changed;
    }

    @Nullable
    private static ChunkPos parseStructureStart(String discoveryKey) {
        int at = discoveryKey.lastIndexOf('@');
        if (at < 0 || at + 1 >= discoveryKey.length()) return null;
        String coords = discoveryKey.substring(at + 1);
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

    private static void syncKnownCopy(ServerPlayer player, String key, CompoundTag marker) {
        CompoundTag known = player.getPersistentData().getCompound(KNOWN_TAG);
        known.put(key, marker.copy());
        player.getPersistentData().put(KNOWN_TAG, known);
    }

    private static boolean installMarker(ServerLevel level,
                                         ItemStack atlas,
                                         String discoveryKey,
                                         CompoundTag marker,
                                         boolean allowCreateMap,
                                         boolean clearOldMoonlightPin,
                                         boolean dedupeVanillaDecoration) {
        MarkerRecord record = parse(marker);
        if (record == null) return true;

        try {
            Class<?> atlasItemClass = Class.forName("pepjebs.mapatlases.item.MapAtlasItem");
            Method getMaps = atlasItemClass.getMethod("getMaps", ItemStack.class, Level.class);
            Object collection = getMaps.invoke(null, atlas, level);
            if (collection == null) return false;

            Method getCount = collection.getClass().getMethod("getCount");
            Method getScale = collection.getClass().getMethod("getScale");
            Method getAllIds = collection.getClass().getMethod("getAllIds");
            Method add = collection.getClass().getMethod("add", int.class, Level.class);

            int count = ((Number) getCount.invoke(collection)).intValue();
            byte scale = count > 0
                    ? clampScale(((Number) getScale.invoke(collection)).byteValue())
                    : DEFAULT_SCALE;

            int[] ids = (int[]) getAllIds.invoke(collection);
            MapItemSavedData data = findCoveringMap(level, ids, record.pos(), scale);
            if (data == null) {
                if (!allowCreateMap) return false;

                ItemStack map = MapItem.create(level, record.pos().getX(), record.pos().getZ(), scale, true, true);
                Integer mapId = MapItem.getMapId(map);
                data = MapItem.getSavedData(map, level);
                if (mapId == null || data == null) return false;

                Object added = add.invoke(collection, mapId.intValue(), level);
                if (added instanceof Boolean accepted && !accepted) return false;
            }

            if (dedupeVanillaDecoration) {
                removeVanillaDecorationEverywhere(level, ids, data, decorationId(discoveryKey));
            }

            if (clearOldMoonlightPin) {
                int radius = record.category() == DiscoveryCategory.VILLAGE ? 96 : 24;
                removeCustomMarkersNear(data, record.pos(), radius);
            }

            addVanillaDecoration(level, data, discoveryKey, record);
            data.setDirty();
            return true;
        } catch (Throwable ex) {
            if (allowCreateMap) {
                CozyCrazyZones.LOGGER.warn(
                        "Could not add discovered {} '{}' to Map Atlases; will retry",
                        record.category(), record.name(), ex
                );
            }
            return false;
        }
    }

    private static void removeVanillaDecorationEverywhere(ServerLevel level,
                                                           int[] atlasMapIds,
                                                           MapItemSavedData target,
                                                           String decorationId) {
        for (int id : atlasMapIds) {
            MapItemSavedData data = MapItem.getSavedData(id, level);
            if (data != null) removeVanillaDecoration(data, decorationId);
        }
        // A just-created target map is not present in the ids snapshot above yet.
        removeVanillaDecoration(target, decorationId);
    }

    private static void removeVanillaDecoration(MapItemSavedData data, String decorationId) {
        MapItemSavedDataAccessor accessor = (MapItemSavedDataAccessor) (Object) data;
        if (accessor.cozyzones$getDecorations().remove(decorationId) != null) {
            data.setDirty();
        }
    }

    private static void addVanillaDecoration(ServerLevel level,
                                             MapItemSavedData data,
                                             String discoveryKey,
                                             MarkerRecord record) {
        MapItemSavedDataAccessor accessor = (MapItemSavedDataAccessor) (Object) data;
        accessor.cozyzones$addNamedDecoration(
                record.icon(),
                level,
                decorationId(discoveryKey),
                record.pos().getX(),
                record.pos().getZ(),
                180.0D,
                Component.literal(record.name())
        );
    }

    @Nullable
    private static MarkerRecord parse(CompoundTag marker) {
        if (marker == null || marker.isEmpty()) return null;
        DiscoveryCategory category;
        try {
            category = DiscoveryCategory.valueOf(marker.getString("Category"));
        } catch (IllegalArgumentException ex) {
            return null;
        }

        MapDecoration.Type icon = category.defaultDecorationType();
        String iconName = marker.getString("Icon");
        if (!iconName.isBlank()) {
            try {
                icon = MapDecoration.Type.valueOf(iconName);
            } catch (IllegalArgumentException ignored) {
                // Keep category default when an old/removed enum value is encountered.
            }
        }

        String name = marker.getString("Name");
        if (name.isBlank()) name = category.displayName();
        return new MarkerRecord(
                category,
                name,
                new BlockPos(marker.getInt("X"), 64, marker.getInt("Z")),
                icon
        );
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

    @Nullable
    private static MapItemSavedData findCoveringMap(ServerLevel level, int[] ids, BlockPos pos, byte atlasScale) {
        for (int id : ids) {
            MapItemSavedData data = MapItem.getSavedData(id, level);
            if (data == null || data.dimension != level.dimension() || data.scale != atlasScale) continue;
            int blocksPerPixel = 1 << data.scale;
            int halfWidth = 64 * blocksPerPixel;
            if (pos.getX() >= data.centerX - halfWidth
                    && pos.getX() < data.centerX + halfWidth
                    && pos.getZ() >= data.centerZ - halfWidth
                    && pos.getZ() < data.centerZ + halfWidth) {
                return data;
            }
        }
        return null;
    }

    private static void removeCustomMarkersNear(MapItemSavedData data, BlockPos pos, int radius) {
        try {
            Method getMarkers = data.getClass().getMethod("ml$getCustomMarkers");
            Object raw = getMarkers.invoke(data);
            if (!(raw instanceof Map<?, ?> markers)) return;

            Method remove = data.getClass().getMethod("ml$removeCustomMarker", String.class);
            Map<?, ?> copy = new HashMap<>(markers);
            long radiusSq = (long) radius * radius;
            for (Map.Entry<?, ?> entry : copy.entrySet()) {
                Object custom = entry.getValue();
                if (!(entry.getKey() instanceof String markerId) || custom == null) continue;
                try {
                    Object markerPos = custom.getClass().getMethod("getPos").invoke(custom);
                    if (markerPos instanceof BlockPos blockPos) {
                        long dx = blockPos.getX() - (long) pos.getX();
                        long dz = blockPos.getZ() - (long) pos.getZ();
                        if (dx * dx + dz * dz <= radiusSq) remove.invoke(data, markerId);
                    }
                } catch (ReflectiveOperationException ignored) {
                    // Foreign marker type: leave it alone.
                }
            }
        } catch (ReflectiveOperationException ignored) {
            // Atlas still receives the vanilla category marker even without Moonlight access.
        }
    }

    private static String decorationId(String discoveryKey) {
        return "cozyzones_" + Integer.toUnsignedString(discoveryKey.hashCode(), 36);
    }

    private static byte clampScale(byte scale) {
        return (byte) Math.max(0, Math.min(4, scale));
    }

    private record MarkerRecord(
            DiscoveryCategory category,
            String name,
            BlockPos pos,
            MapDecoration.Type icon
    ) {}
}
