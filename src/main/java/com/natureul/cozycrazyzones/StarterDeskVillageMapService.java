package com.natureul.cozycrazyzones;

import com.natureul.cozycrazyzones.mixin.MapItemSavedDataAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Owns the physical map displayed in the starter house.
 *
 * The structure template contains a filled-map item as scenery. A baked filled map necessarily
 * carries an arbitrary map id, so the first real map allocated in a new world can accidentally make
 * that frame start displaying somebody else's local map. That is exactly the confusing "I opened my
 * Atlas and the desk map became the house" behaviour seen in testing.
 *
 * We replace that template item with a freshly allocated, world-specific map. Its center is moved to
 * the exact midpoint between the starter house and the reserved first village, so a scale-4 wall map
 * can always contain both endpoints of the <=1650-block starter trip. Home gets a blue marker and the
 * village gets a red target. Each player still receives their own scale-2 Atlas; the wall map is a
 * shared visual clue rather than a multiplayer-shared Atlas.
 */
public final class StarterDeskVillageMapService {
    private static final String GUIDE_TAG = "CozyCrazyZonesDeskVillageGuide";
    private static final String TARGET_X_TAG = "CozyCrazyZonesDeskVillageX";
    private static final String TARGET_Z_TAG = "CozyCrazyZonesDeskVillageZ";

    private static final int RETRY_INTERVAL_TICKS = 20;
    private static final int MAX_ATTEMPTS = 45;
    private static final int PAINT_TICKS = 48;
    private static final byte DESK_MAP_SCALE = 4;

    private static final ConcurrentMap<UUID, Integer> PENDING = new ConcurrentHashMap<>();
    private static final ConcurrentMap<UUID, Integer> PAINTING = new ConcurrentHashMap<>();

    private StarterDeskVillageMapService() {}

    public static void begin(ServerPlayer player) {
        if (player.serverLevel().dimension() != Level.OVERWORLD) return;
        PENDING.put(player.getUUID(), 0);
        if (tryPrepare(player)) PENDING.remove(player.getUUID());
    }

    public static void tick(ServerPlayer player) {
        if (player.serverLevel().dimension() != Level.OVERWORLD) return;

        Integer attempts = PENDING.get(player.getUUID());
        if (attempts != null && player.tickCount % RETRY_INTERVAL_TICKS == 0) {
            if (tryPrepare(player)) {
                PENDING.remove(player.getUUID());
            } else {
                int next = attempts + 1;
                if (next >= MAX_ATTEMPTS) {
                    PENDING.remove(player.getUUID());
                    CozyCrazyZones.LOGGER.warn(
                            "Could not find/prepare the starter-house desk map after {} attempts",
                            MAX_ATTEMPTS
                    );
                } else {
                    PENDING.put(player.getUUID(), next);
                }
            }
        }

        Integer remaining = PAINTING.get(player.getUUID());
        if (remaining == null) return;
        if (!paintOnePass(player)) {
            PAINTING.remove(player.getUUID());
            return;
        }
        if (remaining <= 1) PAINTING.remove(player.getUUID());
        else PAINTING.put(player.getUUID(), remaining - 1);
    }

    public static void remove(ServerPlayer player) {
        PENDING.remove(player.getUUID());
        PAINTING.remove(player.getUUID());
    }

    public static void clear() {
        PENDING.clear();
        PAINTING.clear();
    }

    private static boolean tryPrepare(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        ItemFrame frame = findDeskMapFrame(level);
        if (frame == null) return false;

        ChunkPos targetChunk = VillageRingPlanner.targetFor(
                level,
                level.getChunkSource().getGenerator(),
                level.getChunkSource().getGeneratorState(),
                level.registryAccess()
        );
        if (targetChunk == null) return false;

        BlockPos village = new BlockPos(targetChunk.getMiddleBlockX(), 64, targetChunk.getMiddleBlockZ());
        ItemStack existing = frame.getItem();
        CompoundTag existingTag = existing.getTag();
        if (existingTag != null
                && existingTag.getBoolean(GUIDE_TAG)
                && existingTag.getInt(TARGET_X_TAG) == village.getX()
                && existingTag.getInt(TARGET_Z_TAG) == village.getZ()) {
            PAINTING.putIfAbsent(player.getUUID(), PAINT_TICKS);
            return true;
        }

        BlockPos spawn = level.getSharedSpawnPos();
        int centerX = (int) Math.round((spawn.getX() + village.getX()) * 0.5D);
        int centerZ = (int) Math.round((spawn.getZ() + village.getZ()) * 0.5D);

        ItemStack guide = MapItem.create(level, centerX, centerZ, DESK_MAP_SCALE, true, true);
        MapItemSavedData data = MapItem.getSavedData(guide, level);
        if (data == null) return false;

        // Vanilla maps snap their center to a 2048-block grid at scale 4. A village and house can
        // sit on opposite sides of that invisible grid boundary even though they are only ~1.1 km
        // apart. For this one authored guide, use the actual midpoint so both markers always fit.
        MapItemSavedDataAccessor accessor = (MapItemSavedDataAccessor) (Object) data;
        accessor.cozyzones$setCenterX(centerX);
        accessor.cozyzones$setCenterZ(centerZ);

        MapItemSavedData.addTargetDecoration(guide, spawn, "Home", MapDecoration.Type.BLUE_MARKER);
        MapItemSavedData.addTargetDecoration(guide, village, "Nearest Village", MapDecoration.Type.TARGET_X);
        guide.setHoverName(Component.literal("Map to the Nearest Village"));

        CompoundTag tag = guide.getOrCreateTag();
        tag.putBoolean(GUIDE_TAG, true);
        tag.putInt(TARGET_X_TAG, village.getX());
        tag.putInt(TARGET_Z_TAG, village.getZ());

        data.setDirty();
        frame.setItem(guide, false);
        PAINTING.put(player.getUUID(), PAINT_TICKS);

        CozyCrazyZones.LOGGER.info(
                "Starter-house desk map now points from home {},{} to nearest village {},{} (scale {}, exact midpoint {},{})",
                spawn.getX(), spawn.getZ(), village.getX(), village.getZ(), DESK_MAP_SCALE, centerX, centerZ
        );
        return true;
    }

    private static boolean paintOnePass(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        ItemFrame frame = findPreparedDeskMapFrame(level);
        if (frame == null) return false;
        ItemStack stack = frame.getItem();
        if (!(stack.getItem() instanceof MapItem mapItem)) return false;
        MapItemSavedData data = MapItem.getSavedData(stack, level);
        if (data == null) return false;

        // A map sitting in an item frame does not naturally receive the same terrain-color updates
        // as a held map. Feed it a few ordinary vanilla map updates while the player is still around
        // the starter house, so the home end of the guide is visibly geographic instead of pure tan.
        mapItem.update(level, player, data);
        return true;
    }

    private static ItemFrame findDeskMapFrame(ServerLevel level) {
        BlockPos spawn = level.getSharedSpawnPos();
        AABB box = new AABB(
                spawn.getX() - 56.0D, spawn.getY() - 56.0D, spawn.getZ() - 56.0D,
                spawn.getX() + 57.0D, spawn.getY() + 57.0D, spawn.getZ() + 57.0D
        );
        List<ItemFrame> frames = level.getEntitiesOfClass(
                ItemFrame.class,
                box,
                frame -> frame.getItem().getItem() instanceof MapItem
        );
        if (frames.isEmpty()) return null;
        frames.sort(Comparator.comparingDouble(frame -> frame.distanceToSqr(
                spawn.getX() + 0.5D,
                spawn.getY() + 0.5D,
                spawn.getZ() + 0.5D
        )));
        return frames.get(0);
    }

    private static ItemFrame findPreparedDeskMapFrame(ServerLevel level) {
        ItemFrame frame = findDeskMapFrame(level);
        if (frame == null) return null;
        CompoundTag tag = frame.getItem().getTag();
        return tag != null && tag.getBoolean(GUIDE_TAG) ? frame : null;
    }
}
