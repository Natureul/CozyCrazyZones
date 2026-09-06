package com.natureul.cozycrazyzones;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Removes the now-obsolete starter navigation frame from fresh and existing worlds. */
public final class StarterDeskDecorationService {
    private static final ResourceLocation ATLAS_ID = new ResourceLocation("map_atlases", "atlas");
    private static final String OLD_GUIDE_TAG = "CozyCrazyZonesDeskVillageGuide";
    private static final String OLD_GUIDE_NAME = "Hearthlands Village Map";
    private static final String SURVEY_COMPASS_NAME = "Hearthlands Survey Compass";
    private static final String DONE_TAG = "CozyCrazyZonesDeskNavigationRemovedV3";
    private static final int RETRY_INTERVAL_TICKS = 20;
    private static final int MAX_ATTEMPTS = 45;
    private static final ConcurrentMap<UUID, Integer> PENDING = new ConcurrentHashMap<>();

    private StarterDeskDecorationService() {}

    public static void begin(ServerPlayer player) {
        if (player.serverLevel().dimension() != Level.OVERWORLD) return;
        if (player.getPersistentData().getBoolean(DONE_TAG)) return;
        PENDING.put(player.getUUID(), 0);
        if (tryRemove(player)) PENDING.remove(player.getUUID());
    }

    public static void tick(ServerPlayer player) {
        Integer attempts = PENDING.get(player.getUUID());
        if (attempts == null || player.tickCount % RETRY_INTERVAL_TICKS != 0) return;
        if (tryRemove(player)) {
            PENDING.remove(player.getUUID());
            return;
        }
        int next = attempts + 1;
        if (next >= MAX_ATTEMPTS) PENDING.remove(player.getUUID());
        else PENDING.put(player.getUUID(), next);
    }

    public static void remove(ServerPlayer player) {
        PENDING.remove(player.getUUID());
    }

    public static void clear() {
        PENDING.clear();
    }

    private static boolean tryRemove(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos spawn = level.getSharedSpawnPos();
        AABB box = new AABB(
                spawn.getX() - 18.0D, spawn.getY() - 12.0D, spawn.getZ() - 18.0D,
                spawn.getX() + 19.0D, spawn.getY() + 13.0D, spawn.getZ() + 19.0D
        );

        // Fresh starter structures contain the authored navigation map/atlas in a horizontal frame.
        List<ItemFrame> candidates = level.getEntitiesOfClass(
                ItemFrame.class,
                box,
                frame -> frame.getDirection() == Direction.UP && isDeskNavigationItem(frame.getItem())
        );

        // Existing 0.3.20/0.3.21 worlds may already contain our named survey compass instead.
        if (candidates.isEmpty()) {
            candidates = level.getEntitiesOfClass(
                    ItemFrame.class,
                    box,
                    frame -> isKnownStarterNavigationItem(frame.getItem())
            );
        }
        if (candidates.isEmpty()) return false;

        candidates.sort(Comparator.comparingDouble(frame -> frame.distanceToSqr(
                spawn.getX() + 0.5D,
                spawn.getY() + 0.5D,
                spawn.getZ() + 0.5D
        )));

        ItemFrame frame = candidates.get(0);
        frame.discard();
        player.getPersistentData().putBoolean(DONE_TAG, true);
        CozyCrazyZones.LOGGER.info("Removed obsolete starter navigation item frame");
        return true;
    }

    private static boolean isDeskNavigationItem(ItemStack stack) {
        return stack != null && !stack.isEmpty() && (stack.getItem() instanceof MapItem || isAtlas(stack));
    }

    private static boolean isKnownStarterNavigationItem(ItemStack stack) {
        return isTaggedOldGuide(stack) || isAtlas(stack) || isSurveyCompass(stack);
    }

    private static boolean isTaggedOldGuide(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof MapItem)) return false;
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.getBoolean(OLD_GUIDE_TAG)) return true;
        return stack.hasCustomHoverName() && OLD_GUIDE_NAME.equals(stack.getHoverName().getString());
    }

    private static boolean isSurveyCompass(ItemStack stack) {
        return stack != null
                && !stack.isEmpty()
                && stack.is(Items.COMPASS)
                && stack.hasCustomHoverName()
                && SURVEY_COMPASS_NAME.equals(stack.getHoverName().getString());
    }

    private static boolean isAtlas(ItemStack stack) {
        return stack != null && !stack.isEmpty() && ATLAS_ID.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }
}
