package com.natureul.cozycrazyzones;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Turns the Atlas on the starter-house desk into the player's first navigation quest.
 *
 * The village itself is selected by VillageRingPlanner from Minecraft's real village-placement
 * lattice. This service adds a short chain of same-scale map tiles from spawn toward that reserved
 * candidate and puts a focused Map Atlases pin named "Nearest Village" at the destination.
 *
 * Integration with Map Atlases/Moonlight is deliberately reflective so CozyCrazyZones remains
 * buildable without redistributing either dependency. If their immediate capability API is ever
 * unavailable, the old Map Atlases "maps" NBT migration is used as a compatibility fallback.
 */
public final class StarterVillageMapService {
    private static final ResourceLocation ATLAS_ID = new ResourceLocation("map_atlases", "atlas");
    private static final ResourceLocation PIN_ID = new ResourceLocation("map_atlases", "pin");

    private static final String LINKED_TAG = "CozyCrazyZonesVillageGuide";
    private static final String TARGET_X_TAG = "CozyCrazyZonesVillageX";
    private static final String TARGET_Z_TAG = "CozyCrazyZonesVillageZ";
    private static final String MAP_IDS_TAG = "CozyCrazyZonesVillageMapIds";

    private static final int RETRY_INTERVAL_TICKS = 20;
    private static final int MAX_ATTEMPTS = 30;
    private static final byte DEFAULT_GUIDE_SCALE = 2;

    private static final ConcurrentMap<UUID, Integer> PENDING = new ConcurrentHashMap<>();

    private StarterVillageMapService() {}

    public static void begin(ServerPlayer player) {
        if (!eligible(player)) return;
        PENDING.put(player.getUUID(), 0);
        if (tryLink(player)) PENDING.remove(player.getUUID());
    }

    public static void tick(ServerPlayer player) {
        Integer attempts = PENDING.get(player.getUUID());
        if (attempts == null || player.tickCount % RETRY_INTERVAL_TICKS != 0) return;

        if (tryLink(player)) {
            PENDING.remove(player.getUUID());
            return;
        }

        int next = attempts + 1;
        if (next >= MAX_ATTEMPTS) {
            PENDING.remove(player.getUUID());
            CozyCrazyZones.LOGGER.warn(
                    "Could not find/link the starter-house Map Atlas after {} attempts; village generation remains enabled",
                    MAX_ATTEMPTS
            );
        } else {
            PENDING.put(player.getUUID(), next);
        }
    }

    public static void remove(ServerPlayer player) {
        PENDING.remove(player.getUUID());
    }

    public static void clear() {
        PENDING.clear();
    }

    private static boolean eligible(ServerPlayer player) {
        return player.serverLevel().dimension() == Level.OVERWORLD && ModList.get().isLoaded("map_atlases");
    }

    private static boolean tryLink(ServerPlayer player) {
        if (!eligible(player)) return true;

        ServerLevel level = player.serverLevel();
        AtlasHandle handle = findStarterAtlas(level, player);
        if (handle == null) return false;

        ItemStack original = handle.stack();
        CompoundTag existingTag = original.getTag();
        if (existingTag != null && existingTag.getBoolean(LINKED_TAG)) return true;

        ChunkPos targetChunk = VillageRingPlanner.targetFor(
                level,
                level.getChunkSource().getGenerator(),
                level.getChunkSource().getGeneratorState(),
                level.registryAccess()
        );
        if (targetChunk == null) return false;

        BlockPos spawn = level.getSharedSpawnPos();
        BlockPos village = new BlockPos(targetChunk.getMiddleBlockX(), 64, targetChunk.getMiddleBlockZ());

        // ItemFrame stores a one-count copy. Work on our own copy there so the capability + tag
        // mutation is committed atomically with setItem. Inventory Atlases can be mutated in place.
        ItemStack atlas = handle.frame() == null ? original : original.copy();

        GuideResult result;
        try {
            result = addGuideThroughMapAtlasesApi(level, atlas, spawn, village);
        } catch (Throwable apiFailure) {
            CozyCrazyZones.LOGGER.warn(
                    "Immediate Map Atlases API integration failed; falling back to its legacy map-list migration",
                    apiFailure
            );
            try {
                result = addGuideThroughLegacyNbt(level, atlas, spawn, village);
            } catch (Throwable fallbackFailure) {
                CozyCrazyZones.LOGGER.error("Could not prepare the starter Atlas village guide", fallbackFailure);
                return false;
            }
        }

        CompoundTag tag = atlas.getOrCreateTag();
        tag.putBoolean(LINKED_TAG, true);
        tag.putInt(TARGET_X_TAG, village.getX());
        tag.putInt(TARGET_Z_TAG, village.getZ());
        tag.putIntArray(MAP_IDS_TAG, result.mapIds());

        if (handle.frame() != null) {
            handle.frame().setItem(atlas, false);
        }

        CozyCrazyZones.LOGGER.info(
                "Starter Atlas linked to reserved first village at {},{} (chunk {},{}): {} guide map(s), scale {}, focused pin {}",
                village.getX(),
                village.getZ(),
                targetChunk.x,
                targetChunk.z,
                result.mapIds().length,
                result.scale(),
                result.focusedPin()
        );
        return true;
    }

    @Nullable
    private static AtlasHandle findStarterAtlas(ServerLevel level, ServerPlayer player) {
        BlockPos spawn = level.getSharedSpawnPos();
        AABB search = new AABB(
                spawn.getX() - 64.0D, spawn.getY() - 40.0D, spawn.getZ() - 64.0D,
                spawn.getX() + 65.0D, spawn.getY() + 41.0D, spawn.getZ() + 65.0D
        );

        List<ItemFrame> frames = level.getEntitiesOfClass(
                ItemFrame.class,
                search,
                frame -> isAtlas(frame.getItem())
        );
        if (!frames.isEmpty()) {
            frames.sort(Comparator.comparingDouble(frame -> frame.distanceToSqr(
                    spawn.getX() + 0.5D,
                    spawn.getY() + 0.5D,
                    spawn.getZ() + 0.5D
            )));
            ItemFrame frame = frames.get(0);
            return new AtlasHandle(frame.getItem(), frame);
        }

        // The player can grab the Atlas very quickly. Keep the handoff race-free by also checking
        // their inventory while the retry window is active.
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (isAtlas(stack)) return new AtlasHandle(stack, null);
        }
        return null;
    }

    private static boolean isAtlas(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return ATLAS_ID.equals(id);
    }

    private static GuideResult addGuideThroughMapAtlasesApi(ServerLevel level,
                                                             ItemStack atlas,
                                                             BlockPos spawn,
                                                             BlockPos village) throws Exception {
        Class<?> atlasItemClass = Class.forName("pepjebs.mapatlases.item.MapAtlasItem");
        Method getMaps = atlasItemClass.getMethod("getMaps", ItemStack.class, Level.class);
        Object collection = getMaps.invoke(null, atlas, level);
        if (collection == null) throw new IllegalStateException("MapAtlasItem.getMaps returned null");

        Method getCount = collection.getClass().getMethod("getCount");
        Method getScale = collection.getClass().getMethod("getScale");
        Method getAllIds = collection.getClass().getMethod("getAllIds");
        Method add = collection.getClass().getMethod("add", int.class, Level.class);

        int existingCount = ((Number) getCount.invoke(collection)).intValue();
        byte scale = existingCount > 0
                ? clampScale(((Number) getScale.invoke(collection)).byteValue())
                : DEFAULT_GUIDE_SCALE;

        int[] existingIds = (int[]) getAllIds.invoke(collection);
        Map<Long, Integer> centers = existingCenters(level, existingIds, scale);
        List<GuidePoint> route = route(spawn, village, scale, existingCount == 0);
        List<Integer> linkedIds = new ArrayList<>();

        MapItemSavedData destinationData = null;
        Integer destinationId = null;

        for (int i = 0; i < route.size(); i++) {
            GuidePoint point = route.get(i);
            boolean destination = i == route.size() - 1;
            long centerKey = centerKey(point.centerX(), point.centerZ());
            Integer mapId = centers.get(centerKey);
            MapItemSavedData data;

            if (mapId != null) {
                data = MapItem.getSavedData(mapId, level);
            } else {
                ItemStack map = MapItem.create(level, point.requestX(), point.requestZ(), scale, true, true);
                mapId = MapItem.getMapId(map);
                data = MapItem.getSavedData(map, level);
                if (mapId == null || data == null) {
                    throw new IllegalStateException("Minecraft failed to allocate a guide map");
                }

                Object added = add.invoke(collection, mapId.intValue(), level);
                boolean accepted = !(added instanceof Boolean acceptedBoolean) || acceptedBoolean;
                if (!accepted) {
                    throw new IllegalStateException("Map Atlases rejected guide map " + mapId + " at scale " + scale);
                }
                centers.put(centerKey, mapId);
            }

            linkedIds.add(mapId);
            if (destination) {
                destinationId = mapId;
                destinationData = data;
            }
        }

        if (destinationId == null || destinationData == null) {
            throw new IllegalStateException("Guide route produced no destination map");
        }

        boolean focused = addFocusedVillagePin(destinationData, village);
        return new GuideResult(uniqueInts(linkedIds), scale, focused);
    }

    private static GuideResult addGuideThroughLegacyNbt(ServerLevel level,
                                                         ItemStack atlas,
                                                         BlockPos spawn,
                                                         BlockPos village) throws Exception {
        byte scale = DEFAULT_GUIDE_SCALE;
        List<GuidePoint> route = route(spawn, village, scale, true);
        List<Integer> created = new ArrayList<>();
        MapItemSavedData destinationData = null;

        for (int i = 0; i < route.size(); i++) {
            GuidePoint point = route.get(i);
            ItemStack map = MapItem.create(level, point.requestX(), point.requestZ(), scale, true, true);
            Integer mapId = MapItem.getMapId(map);
            MapItemSavedData data = MapItem.getSavedData(map, level);
            if (mapId == null || data == null) throw new IllegalStateException("Minecraft failed to allocate a fallback guide map");
            created.add(mapId);
            if (i == route.size() - 1) destinationData = data;
        }

        if (destinationData == null) throw new IllegalStateException("Fallback route produced no destination map");
        boolean focused = addFocusedVillagePin(destinationData, village);

        CompoundTag tag = atlas.getOrCreateTag();
        int[] old = tag.getIntArray("maps");
        Set<Integer> merged = new LinkedHashSet<>();
        Arrays.stream(old).forEach(merged::add);
        merged.addAll(created);
        tag.putIntArray("maps", merged.stream().mapToInt(Integer::intValue).toArray());

        return new GuideResult(uniqueInts(created), scale, focused);
    }

    private static Map<Long, Integer> existingCenters(ServerLevel level, int[] ids, byte scale) {
        Map<Long, Integer> result = new HashMap<>();
        for (int id : ids) {
            MapItemSavedData data = MapItem.getSavedData(id, level);
            if (data == null || data.scale != scale || data.dimension != level.dimension()) continue;
            result.put(centerKey(data.centerX, data.centerZ), id);
        }
        return result;
    }

    /**
     * Create a small chain of neighboring map cells. An empty starter Atlas defaults to scale 2,
     * yielding only ~3-4 maps for the ~1.1 km village trip. If the Atlas already has a scale, keep
     * that scale so Map Atlases accepts every new tile.
     */
    private static List<GuidePoint> route(BlockPos spawn, BlockPos village, byte scale, boolean includeSpawn) {
        double dx = village.getX() - spawn.getX();
        double dz = village.getZ() - spawn.getZ();
        double distance = Math.hypot(dx, dz);
        int mapWidth = 128 << scale;
        double desiredStep = mapWidth * 0.72D;
        int segments = Math.max(1, (int) Math.ceil(distance / desiredStep));

        List<GuidePoint> points = new ArrayList<>();
        Set<Long> seen = new LinkedHashSet<>();
        int first = includeSpawn ? 0 : 1;
        for (int i = first; i <= segments; i++) {
            double t = i / (double) segments;
            int requestX = (int) Math.round(spawn.getX() + dx * t);
            int requestZ = (int) Math.round(spawn.getZ() + dz * t);
            int centerX = snappedMapCenter(requestX, scale);
            int centerZ = snappedMapCenter(requestZ, scale);
            long key = centerKey(centerX, centerZ);
            if (seen.add(key)) points.add(new GuidePoint(requestX, requestZ, centerX, centerZ));
        }

        // Rounding/grid de-duplication can theoretically omit the exact destination cell. Guarantee
        // that the last tile is the one containing the village.
        int destinationCenterX = snappedMapCenter(village.getX(), scale);
        int destinationCenterZ = snappedMapCenter(village.getZ(), scale);
        long destinationKey = centerKey(destinationCenterX, destinationCenterZ);
        if (points.isEmpty() || centerKey(points.get(points.size() - 1).centerX(), points.get(points.size() - 1).centerZ()) != destinationKey) {
            if (seen.add(destinationKey)) {
                points.add(new GuidePoint(village.getX(), village.getZ(), destinationCenterX, destinationCenterZ));
            }
        }
        return points;
    }

    private static int snappedMapCenter(int coordinate, byte scale) {
        int size = 128 * (1 << scale);
        int grid = (int) Math.floor((coordinate + 64.0D) / size);
        return grid * size + size / 2 - 64;
    }

    private static boolean addFocusedVillagePin(MapItemSavedData data, BlockPos village) {
        try {
            Class<?> compat = Class.forName("pepjebs.mapatlases.integration.moonlight.MoonlightCompat");
            Method addDecoration = compat.getMethod(
                    "addDecoration",
                    MapItemSavedData.class,
                    BlockPos.class,
                    ResourceLocation.class,
                    Component.class
            );
            addDecoration.invoke(null, data, village, PIN_ID, Component.literal("Nearest Village"));

            // A focused Map Atlases pin also produces its small directional indicator while the
            // destination is outside the current minimap view, which makes this an actual guide
            // rather than merely a label once the player has already arrived.
            try {
                Method getMarkers = data.getClass().getMethod("ml$getCustomMarkers");
                Object markersObject = getMarkers.invoke(data);
                if (markersObject instanceof Map<?, ?> markers) {
                    for (Object marker : markers.values()) {
                        if (marker == null || !marker.getClass().getName().endsWith("PinMarker")) continue;
                        try {
                            Method getPos = marker.getClass().getMethod("getPos");
                            Object pos = getPos.invoke(marker);
                            if (!(pos instanceof BlockPos markerPos) || !markerPos.equals(village)) continue;
                            marker.getClass().getMethod("setFocused", boolean.class).invoke(marker, true);
                            data.setDirty();
                            return true;
                        } catch (ReflectiveOperationException ignored) {
                            // Keep looking; the pin itself was still successfully added.
                        }
                    }
                }
            } catch (ReflectiveOperationException ignored) {
                // Pin persists even if this Map Atlases/Moonlight version does not expose focus.
            }
            data.setDirty();
            return false;
        } catch (Throwable ex) {
            CozyCrazyZones.LOGGER.warn("Could not add Map Atlases destination pin to starter guide map", ex);
            return false;
        }
    }

    private static byte clampScale(byte scale) {
        return (byte) Math.max(0, Math.min(4, scale));
    }

    private static int[] uniqueInts(List<Integer> values) {
        return values.stream().distinct().mapToInt(Integer::intValue).toArray();
    }

    private static long centerKey(int x, int z) {
        return (((long) x) << 32) ^ (z & 0xffffffffL);
    }

    private record AtlasHandle(ItemStack stack, @Nullable ItemFrame frame) {}
    private record GuidePoint(int requestX, int requestZ, int centerX, int centerZ) {}
    private record GuideResult(int[] mapIds, byte scale, boolean focusedPin) {}
}
