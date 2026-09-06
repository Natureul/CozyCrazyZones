package com.natureul.cozycrazyzones;

import com.natureul.cozycrazyzones.mixin.MapItemSavedDataAccessor;
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
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Owns the real filled map displayed in the starter house.
 *
 * This is an authored Hearthlands overview, not the player's Atlas and not a fake template map id.
 * The four red targets correspond to four real reserved villages. Because a scale-4 vanilla map is
 * only ~2,048 blocks wide while the guaranteed villages must begin beyond the 1,000-block starter
 * sanctuary, the wall-map icons are intentionally *directional overview markers*: each is projected
 * inward along the exact bearing of its real village. The personal Atlas keeps exact navigation to
 * the nearest village; the wall map teaches the player that all four regional roads have somewhere
 * real to go.
 *
 * trackingPosition is deliberately disabled on this map. That prevents the item frame itself from
 * being serialized as Minecraft's automatic "Frame" map decoration—the stray marker seen in the
 * earlier playtest. A client-only render hook makes this same filled-map stack look like an ordinary
 * unused map while it remains framed on the desk; removing it reveals the real map and its four
 * named village markers.
 */
public final class StarterDeskVillageMapService {
    private static final ResourceLocation ATLAS_ID = new ResourceLocation("map_atlases", "atlas");

    private static final String GUIDE_TAG = "CozyCrazyZonesDeskVillageGuide";
    private static final String GUIDE_VERSION_TAG = "CozyCrazyZonesDeskVillageGuideVersion";
    private static final int GUIDE_VERSION = 4;

    private static final int RETRY_INTERVAL_TICKS = 20;
    private static final int MAX_ATTEMPTS = 45;
    private static final int PAINT_TICKS = 48;
    private static final byte DESK_MAP_SCALE = 4;
    private static final double OVERVIEW_MARKER_RADIUS = 860.0D;

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

        // Vanilla persists target-decoration coordinates on the ItemStack, but not the optional text
        // component stored in MapItemSavedData's live decoration map. Re-assert the names once per
        // second while the player carries this special guide, so names survive save/reload and a
        // quick grab from the desk without turning this into an always-on per-tick inventory cost.
        if (player.tickCount % RETRY_INTERVAL_TICKS == 0) {
            refreshCarriedGuideNames(player);
        }

        Integer attempts = PENDING.get(player.getUUID());
        if (attempts != null && player.tickCount % RETRY_INTERVAL_TICKS == 0) {
            if (tryPrepare(player)) {
                PENDING.remove(player.getUUID());
            } else {
                int next = attempts + 1;
                if (next >= MAX_ATTEMPTS) {
                    PENDING.remove(player.getUUID());
                    CozyCrazyZones.LOGGER.warn(
                            "Could not find/prepare the starter-house Hearthlands map after {} attempts",
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
        ItemFrame frame = findDeskGuideFrame(level);
        if (frame == null) return false;

        Map<MacroRegion, ChunkPos> targets = VillageRingPlanner.targetsFor(
                level,
                level.getChunkSource().getGenerator(),
                level.getChunkSource().getGeneratorState(),
                level.registryAccess()
        );
        if (targets.size() != MacroRegion.values().length) return false;

        ItemStack existing = frame.getItem();
        CompoundTag existingTag = existing.getTag();
        if (existing.getItem() instanceof MapItem
                && existingTag != null
                && existingTag.getBoolean(GUIDE_TAG)
                && existingTag.getInt(GUIDE_VERSION_TAG) == GUIDE_VERSION
                && targetTagsMatch(existingTag, targets)) {
            ensureNamedDecorations(level, existing);
            PAINTING.putIfAbsent(player.getUUID(), PAINT_TICKS);
            return true;
        }

        BlockPos spawn = level.getSharedSpawnPos();

        // This is intentionally a world-specific, tracking-disabled filled map. The exact center is
        // moved back to spawn after vanilla allocates the saved-map record so the overview is stable
        // regardless of scale-4 grid snapping.
        ItemStack guide = MapItem.create(level, spawn.getX(), spawn.getZ(), DESK_MAP_SCALE, false, false);
        MapItemSavedData data = MapItem.getSavedData(guide, level);
        if (data == null) return false;

        MapItemSavedDataAccessor accessor = (MapItemSavedDataAccessor) (Object) data;
        accessor.cozyzones$setCenterX(spawn.getX());
        accessor.cozyzones$setCenterZ(spawn.getZ());

        MapItemSavedData.addTargetDecoration(guide, spawn, "Home", MapDecoration.Type.BLUE_MARKER);
        accessor.cozyzones$addNamedDecoration(
                MapDecoration.Type.BLUE_MARKER,
                level,
                "Home",
                spawn.getX(),
                spawn.getZ(),
                180.0D,
                Component.literal("Home")
        );

        for (MacroRegion region : MacroRegion.values()) {
            ChunkPos target = targets.get(region);
            if (target == null) continue;

            BlockPos realVillage = new BlockPos(target.getMiddleBlockX(), spawn.getY(), target.getMiddleBlockZ());
            BlockPos overview = overviewMarker(spawn, realVillage);
            String villageName = HearthVillageNames.nameFor(region, level.getSeed(), target);
            MapItemSavedData.addTargetDecoration(
                    guide,
                    overview,
                    villageName,
                    MapDecoration.Type.TARGET_X
            );
            accessor.cozyzones$addNamedDecoration(
                    MapDecoration.Type.TARGET_X,
                    level,
                    villageName,
                    overview.getX(),
                    overview.getZ(),
                    180.0D,
                    Component.literal(villageName)
            );
        }
        guide.setHoverName(Component.literal("Hearthlands Village Map"));

        CompoundTag tag = guide.getOrCreateTag();
        tag.putBoolean(GUIDE_TAG, true);
        tag.putInt(GUIDE_VERSION_TAG, GUIDE_VERSION);
        writeTargetTags(tag, targets);

        data.setDirty();
        frame.setItem(guide, false);
        PAINTING.put(player.getUUID(), PAINT_TICKS);

        StringBuilder summary = new StringBuilder();
        for (MacroRegion region : MacroRegion.values()) {
            ChunkPos target = targets.get(region);
            if (target == null) continue;
            if (!summary.isEmpty()) summary.append("; ");
            summary.append(HearthVillageNames.nameFor(region, level.getSeed(), target))
                    .append(" [")
                    .append(region.displayName())
                    .append("] ")
                    .append(target.getMiddleBlockX())
                    .append(',')
                    .append(target.getMiddleBlockZ());
        }
        CozyCrazyZones.LOGGER.info("Starter-house Hearthlands map prepared with four named real village anchors: {}", summary);
        return true;
    }

    private static void refreshCarriedGuideNames(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (isPreparedGuide(stack)) ensureNamedDecorations(level, stack);
        }
        ItemStack offhand = player.getOffhandItem();
        if (isPreparedGuide(offhand)) ensureNamedDecorations(level, offhand);
    }

    private static boolean isPreparedGuide(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof MapItem)) return false;
        CompoundTag tag = stack.getTag();
        return tag != null
                && tag.getBoolean(GUIDE_TAG)
                && tag.getInt(GUIDE_VERSION_TAG) == GUIDE_VERSION;
    }

    /** Restore the optional visible labels that vanilla does not serialize in the ItemStack target list. */
    private static void ensureNamedDecorations(ServerLevel level, ItemStack guide) {
        if (!isPreparedGuide(guide)) return;
        MapItemSavedData data = MapItem.getSavedData(guide, level);
        CompoundTag tag = guide.getTag();
        if (data == null || tag == null) return;

        MapItemSavedDataAccessor accessor = (MapItemSavedDataAccessor) (Object) data;
        BlockPos spawn = level.getSharedSpawnPos();
        accessor.cozyzones$addNamedDecoration(
                MapDecoration.Type.BLUE_MARKER,
                level,
                "Home",
                spawn.getX(),
                spawn.getZ(),
                180.0D,
                Component.literal("Home")
        );

        for (MacroRegion region : MacroRegion.values()) {
            String xTag = targetTag(region, "X");
            String zTag = targetTag(region, "Z");
            if (!tag.contains(xTag) || !tag.contains(zTag)) continue;

            int targetX = tag.getInt(xTag);
            int targetZ = tag.getInt(zTag);
            ChunkPos targetChunk = new ChunkPos(targetX >> 4, targetZ >> 4);
            BlockPos realVillage = new BlockPos(targetX, spawn.getY(), targetZ);
            BlockPos overview = overviewMarker(spawn, realVillage);
            String villageName = HearthVillageNames.nameFor(region, level.getSeed(), targetChunk);

            accessor.cozyzones$addNamedDecoration(
                    MapDecoration.Type.TARGET_X,
                    level,
                    villageName,
                    overview.getX(),
                    overview.getZ(),
                    180.0D,
                    Component.literal(villageName)
            );
        }
    }

    private static BlockPos overviewMarker(BlockPos spawn, BlockPos realVillage) {
        double dx = realVillage.getX() - spawn.getX();
        double dz = realVillage.getZ() - spawn.getZ();
        double length = Math.hypot(dx, dz);
        if (length < 1.0D) return spawn;
        double scale = Math.min(1.0D, OVERVIEW_MARKER_RADIUS / length);
        return new BlockPos(
                (int) Math.round(spawn.getX() + dx * scale),
                spawn.getY(),
                (int) Math.round(spawn.getZ() + dz * scale)
        );
    }

    private static void writeTargetTags(CompoundTag tag, Map<MacroRegion, ChunkPos> targets) {
        for (MacroRegion region : MacroRegion.values()) {
            ChunkPos target = targets.get(region);
            if (target == null) continue;
            tag.putInt(targetTag(region, "X"), target.getMiddleBlockX());
            tag.putInt(targetTag(region, "Z"), target.getMiddleBlockZ());
        }
    }

    private static boolean targetTagsMatch(CompoundTag tag, Map<MacroRegion, ChunkPos> targets) {
        for (MacroRegion region : MacroRegion.values()) {
            ChunkPos target = targets.get(region);
            if (target == null) continue;
            if (tag.getInt(targetTag(region, "X")) != target.getMiddleBlockX()
                    || tag.getInt(targetTag(region, "Z")) != target.getMiddleBlockZ()) {
                return false;
            }
        }
        return true;
    }

    private static String targetTag(MacroRegion region, String axis) {
        return "CozyCrazyZonesDeskVillage" + region.name() + axis;
    }

    private static boolean paintOnePass(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        ItemFrame frame = findPreparedDeskMapFrame(level);
        if (frame == null) return false;
        ItemStack stack = frame.getItem();
        if (!(stack.getItem() instanceof MapItem mapItem)) return false;
        MapItemSavedData data = MapItem.getSavedData(stack, level);
        if (data == null) return false;

        // A tracking-disabled wall map still accepts normal terrain-color updates; it simply does
        // not auto-add player/item-frame position decorations.
        mapItem.update(level, player, data);
        return true;
    }

    private static ItemFrame findDeskGuideFrame(ServerLevel level) {
        BlockPos spawn = level.getSharedSpawnPos();
        AABB box = new AABB(
                spawn.getX() - 56.0D, spawn.getY() - 56.0D, spawn.getZ() - 56.0D,
                spawn.getX() + 57.0D, spawn.getY() + 57.0D, spawn.getZ() + 57.0D
        );
        List<ItemFrame> frames = level.getEntitiesOfClass(
                ItemFrame.class,
                box,
                frame -> frame.getItem().getItem() instanceof MapItem || isAtlas(frame.getItem())
        );
        if (frames.isEmpty()) return null;
        frames.sort(Comparator.comparingDouble(frame -> frame.distanceToSqr(
                spawn.getX() + 0.5D,
                spawn.getY() + 0.5D,
                spawn.getZ() + 0.5D
        )));
        return frames.get(0);
    }

    private static boolean isAtlas(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return ATLAS_ID.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }

    private static ItemFrame findPreparedDeskMapFrame(ServerLevel level) {
        ItemFrame frame = findDeskGuideFrame(level);
        if (frame == null || !(frame.getItem().getItem() instanceof MapItem)) return null;
        CompoundTag tag = frame.getItem().getTag();
        return tag != null && tag.getBoolean(GUIDE_TAG) ? frame : null;
    }
}
