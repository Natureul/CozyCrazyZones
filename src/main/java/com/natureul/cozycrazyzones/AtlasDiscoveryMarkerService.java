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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persistent discovered-place layer for Map Atlases.
 *
 * Map Atlases uses vanilla MapItemSavedData tiles, so CozyCrazyZones can give categories genuinely
 * different symbols without bundling a second map renderer: village icons, mansion silhouettes,
 * monument icons, colored banners, target-X ruins, red-X dungeons, etc. Vanilla's arbitrary named
 * decorations are not guaranteed to survive every map reload path, so the player's compact known-
 * place ledger reasserts a handful each second while an Atlas is carried.
 */
public final class AtlasDiscoveryMarkerService {
    private static final ResourceLocation ATLAS_ID = new ResourceLocation("map_atlases", "atlas");
    private static final String PENDING_TAG = "cozycrazyzones:pending_atlas_markers";
    private static final String KNOWN_TAG = "cozycrazyzones:known_atlas_markers";
    private static final byte DEFAULT_SCALE = 2;
    private static final int REFRESH_BATCH = 8;

    private static final Map<UUID, Integer> REFRESH_CURSOR = new HashMap<>();

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

    public static void tick(ServerPlayer player) {
        if (player.serverLevel().dimension() != Level.OVERWORLD || !ModList.get().isLoaded("map_atlases")) return;
        ItemStack atlas = findAtlas(player);
        if (atlas == null) return;

        CompoundTag pending = player.getPersistentData().getCompound(PENDING_TAG);
        String pendingKey = pending.getAllKeys().stream().findFirst().orElse(null);
        if (pendingKey != null) {
            CompoundTag marker = pending.getCompound(pendingKey);
            if (installMarker(player.serverLevel(), atlas, pendingKey, marker, true, true)) {
                pending.remove(pendingKey);
                player.getPersistentData().put(PENDING_TAG, pending);
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

        for (int i = 0; i < count; i++) {
            String key = keys.get((cursor + i) % keys.size());
            installMarker(player.serverLevel(), atlas, key, known.getCompound(key), false, false);
        }
        REFRESH_CURSOR.put(player.getUUID(), (cursor + count) % keys.size());
    }

    private static boolean installMarker(ServerLevel level,
                                         ItemStack atlas,
                                         String discoveryKey,
                                         CompoundTag marker,
                                         boolean allowCreateMap,
                                         boolean clearOldMoonlightPin) {
        MarkerRecord record = parse(marker);
        if (record == null) return true; // malformed legacy entry: drop it rather than retry forever

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

                // This location was physically discovered already. Adding its local tile records
                // explored ground; it is not a /locate-style remote reveal.
                ItemStack map = MapItem.create(level, record.pos().getX(), record.pos().getZ(), scale, true, true);
                Integer mapId = MapItem.getMapId(map);
                data = MapItem.getSavedData(map, level);
                if (mapId == null || data == null) return false;

                Object added = add.invoke(collection, mapId.intValue(), level);
                if (added instanceof Boolean accepted && !accepted) return false;
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
