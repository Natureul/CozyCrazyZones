package com.natureul.cozycrazyzones;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Adds discoveries to the player's Map Atlases atlas without forcing remote chunk/map generation.
 *
 * Discoveries are queued in persistent player NBT. If the Atlas is in a chest when a village is
 * found, the marker waits and is installed the next time that player carries an Atlas. Only one
 * pending marker is processed per second, keeping this effectively free during ordinary play.
 */
public final class AtlasDiscoveryMarkerService {
    private static final ResourceLocation ATLAS_ID = new ResourceLocation("map_atlases", "atlas");
    private static final String PENDING_TAG = "cozycrazyzones:pending_atlas_markers";
    private static final byte DEFAULT_SCALE = 2;

    private AtlasDiscoveryMarkerService() {}

    public static void enqueue(ServerPlayer player,
                               String discoveryKey,
                               DiscoveryCategory category,
                               String name,
                               BlockPos pos) {
        CompoundTag pending = player.getPersistentData().getCompound(PENDING_TAG);
        CompoundTag marker = new CompoundTag();
        marker.putString("Category", category.name());
        marker.putString("Name", name);
        marker.putInt("X", pos.getX());
        marker.putInt("Z", pos.getZ());
        pending.put(discoveryKey, marker);
        player.getPersistentData().put(PENDING_TAG, pending);
    }

    public static void tick(ServerPlayer player) {
        if (player.serverLevel().dimension() != Level.OVERWORLD || !ModList.get().isLoaded("map_atlases")) return;
        CompoundTag pending = player.getPersistentData().getCompound(PENDING_TAG);
        if (pending.isEmpty()) return;

        ItemStack atlas = findAtlas(player);
        if (atlas == null) return;

        String key = pending.getAllKeys().stream().findFirst().orElse(null);
        if (key == null) return;
        CompoundTag marker = pending.getCompound(key);

        DiscoveryCategory category;
        try {
            category = DiscoveryCategory.valueOf(marker.getString("Category"));
        } catch (IllegalArgumentException ex) {
            pending.remove(key);
            player.getPersistentData().put(PENDING_TAG, pending);
            return;
        }

        BlockPos pos = new BlockPos(marker.getInt("X"), 64, marker.getInt("Z"));
        if (installMarker(player.serverLevel(), atlas, pos, category, marker.getString("Name"))) {
            pending.remove(key);
            player.getPersistentData().put(PENDING_TAG, pending);
        }
    }

    public static void copyPersistentState(ServerPlayer original, ServerPlayer replacement) {
        CompoundTag old = original.getPersistentData().getCompound(PENDING_TAG);
        if (!old.isEmpty()) replacement.getPersistentData().put(PENDING_TAG, old.copy());
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

    private static boolean installMarker(ServerLevel level,
                                         ItemStack atlas,
                                         BlockPos pos,
                                         DiscoveryCategory category,
                                         String name) {
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
            MapItemSavedData data = findCoveringMap(level, ids, pos, scale);
            if (data == null) {
                // The player is physically at the discovery, so adding its local tile reveals no
                // remote terrain. It merely lets the Atlas remember the place the player just found.
                ItemStack map = MapItem.create(level, pos.getX(), pos.getZ(), scale, true, true);
                Integer mapId = MapItem.getMapId(map);
                data = MapItem.getSavedData(map, level);
                if (mapId == null || data == null) return false;

                Object added = add.invoke(collection, mapId.intValue(), level);
                if (added instanceof Boolean accepted && !accepted) return false;
            }

            // Replace the starter guide's generic marker (or an earlier retry) at this exact block
            // with the authoritative discovered-place marker and its permanent name.
            removeCustomMarkersAt(data, pos);
            addMoonlightDecoration(level, data, pos, category.atlasMarkerId(), Component.literal(name));
            data.setDirty();
            return true;
        } catch (Throwable ex) {
            CozyCrazyZones.LOGGER.warn("Could not add discovered {} '{}' to Map Atlases; will retry", category, name, ex);
            return false;
        }
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

    private static void removeCustomMarkersAt(MapItemSavedData data, BlockPos pos) {
        try {
            Method getMarkers = data.getClass().getMethod("ml$getCustomMarkers");
            Object raw = getMarkers.invoke(data);
            if (!(raw instanceof Map<?, ?> markers)) return;

            Method remove = data.getClass().getMethod("ml$removeCustomMarker", String.class);
            Map<?, ?> copy = new HashMap<>(markers);
            for (Map.Entry<?, ?> entry : copy.entrySet()) {
                Object marker = entry.getValue();
                if (!(entry.getKey() instanceof String markerId) || marker == null) continue;
                try {
                    Object markerPos = marker.getClass().getMethod("getPos").invoke(marker);
                    if (markerPos instanceof BlockPos blockPos && blockPos.getX() == pos.getX() && blockPos.getZ() == pos.getZ()) {
                        remove.invoke(data, markerId);
                    }
                } catch (ReflectiveOperationException ignored) {
                    // A foreign custom marker type can simply coexist; do not break the discovery.
                }
            }
        } catch (ReflectiveOperationException ignored) {
            // Moonlight custom markers are optional. addMoonlightDecoration below remains authoritative.
        }
    }

    private static void addMoonlightDecoration(ServerLevel level,
                                               MapItemSavedData data,
                                               BlockPos pos,
                                               ResourceLocation markerId,
                                               Component name) throws Exception {
        Class<?> compat = Class.forName("pepjebs.mapatlases.integration.moonlight.MoonlightCompat");

        // Map Atlases 1.20-6.0.20 (the pack version) exposes the four-argument method.
        try {
            Method method = compat.getMethod(
                    "addDecoration",
                    MapItemSavedData.class,
                    BlockPos.class,
                    ResourceLocation.class,
                    Component.class
            );
            method.invoke(null, data, pos, markerId, name);
            return;
        } catch (NoSuchMethodException ignored) {
            // Newer Map Atlases adds Level as the first parameter; keeping this makes upgrades cheap.
        }

        Method method = compat.getMethod(
                "addDecoration",
                Level.class,
                MapItemSavedData.class,
                BlockPos.class,
                ResourceLocation.class,
                Component.class
        );
        method.invoke(null, level, data, pos, markerId, name);
    }

    private static byte clampScale(byte scale) {
        return (byte) Math.max(0, Math.min(4, scale));
    }
}
