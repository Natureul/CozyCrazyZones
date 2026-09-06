package com.natureul.cozycrazyzones;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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

/**
 * The desk map went through several increasingly clever render workarounds and remained fragile.
 * The personal Atlas is now the authoritative exploration record, so the desk gets something that
 * is useful, thematic and impossible to desync: a normal framed survey compass.
 */
public final class StarterDeskDecorationService {
    private static final ResourceLocation ATLAS_ID = new ResourceLocation("map_atlases", "atlas");
    private static final String DONE_TAG = "CozyCrazyZonesDeskCompassInstalled";
    private static final int RETRY_INTERVAL_TICKS = 20;
    private static final int MAX_ATTEMPTS = 45;
    private static final ConcurrentMap<UUID, Integer> PENDING = new ConcurrentHashMap<>();

    private StarterDeskDecorationService() {}

    public static void begin(ServerPlayer player) {
        if (player.serverLevel().dimension() != Level.OVERWORLD) return;
        if (player.getPersistentData().getBoolean(DONE_TAG)) return;
        PENDING.put(player.getUUID(), 0);
        if (tryInstall(player)) PENDING.remove(player.getUUID());
    }

    public static void tick(ServerPlayer player) {
        Integer attempts = PENDING.get(player.getUUID());
        if (attempts == null || player.tickCount % RETRY_INTERVAL_TICKS != 0) return;
        if (tryInstall(player)) {
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

    private static boolean tryInstall(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos spawn = level.getSharedSpawnPos();
        AABB box = new AABB(
                spawn.getX() - 56.0D, spawn.getY() - 56.0D, spawn.getZ() - 56.0D,
                spawn.getX() + 57.0D, spawn.getY() + 57.0D, spawn.getZ() + 57.0D
        );

        List<ItemFrame> frames = level.getEntitiesOfClass(
                ItemFrame.class,
                box,
                frame -> isOldDeskNavigationItem(frame.getItem())
        );
        if (frames.isEmpty()) return false;

        frames.sort(Comparator.comparingDouble(frame -> frame.distanceToSqr(
                spawn.getX() + 0.5D,
                spawn.getY() + 0.5D,
                spawn.getZ() + 0.5D
        )));

        ItemFrame frame = frames.get(0);
        ItemStack compass = new ItemStack(Items.COMPASS);
        compass.setHoverName(Component.literal("Hearthlands Survey Compass"));
        frame.setItem(compass, false);
        player.getPersistentData().putBoolean(DONE_TAG, true);

        CozyCrazyZones.LOGGER.info("Retired the fragile starter desk map and installed the Hearthlands Survey Compass");
        return true;
    }

    private static boolean isOldDeskNavigationItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.getItem() instanceof MapItem) return true;
        return ATLAS_ID.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }
}
