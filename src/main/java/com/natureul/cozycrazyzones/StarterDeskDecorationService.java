package com.natureul.cozycrazyzones;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
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

/** Replaces the obsolete horizontal desk map with a useful survey compass. */
public final class StarterDeskDecorationService {
    private static final ResourceLocation ATLAS_ID = new ResourceLocation("map_atlases", "atlas");
    private static final String OLD_GUIDE_TAG = "CozyCrazyZonesDeskVillageGuide";
    private static final String OLD_GUIDE_NAME = "Hearthlands Village Map";
    private static final String DONE_TAG = "CozyCrazyZonesDeskCompassInstalledV2";
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
                spawn.getX() - 18.0D, spawn.getY() - 12.0D, spawn.getZ() - 18.0D,
                spawn.getX() + 19.0D, spawn.getY() + 13.0D, spawn.getZ() + 19.0D
        );

        List<ItemFrame> candidates = level.getEntitiesOfClass(
                ItemFrame.class,
                box,
                frame -> frame.getDirection() == Direction.UP && isDeskNavigationItem(frame.getItem())
        );

        // Migration fallback for old starter structures where the authored frame was not horizontal.
        if (candidates.isEmpty()) {
            candidates = level.getEntitiesOfClass(
                    ItemFrame.class,
                    box,
                    frame -> isTaggedOldGuide(frame.getItem()) || isAtlas(frame.getItem())
            );
        }
        if (candidates.isEmpty()) return false;

        candidates.sort(Comparator.comparingDouble(frame -> frame.distanceToSqr(
                spawn.getX() + 0.5D,
                spawn.getY() + 0.5D,
                spawn.getZ() + 0.5D
        )));

        ItemFrame frame = candidates.get(0);
        ItemStack compass = new ItemStack(Items.COMPASS);
        compass.setHoverName(Component.literal("Hearthlands Survey Compass").withStyle(ChatFormatting.GOLD));
        ListTag lore = new ListTag();
        lore.add(StringTag.valueOf(Component.Serializer.toJson(
                Component.literal("Home and four nearby settlements are marked in your Atlas.")
                        .withStyle(ChatFormatting.GRAY)
        )));
        compass.getOrCreateTagElement("display").put("Lore", lore);

        frame.setItem(compass, false);
        player.getPersistentData().putBoolean(DONE_TAG, true);
        CozyCrazyZones.LOGGER.info("Removed the obsolete starter desk map and installed the Hearthlands Survey Compass");
        return true;
    }

    private static boolean isDeskNavigationItem(ItemStack stack) {
        return stack != null && !stack.isEmpty() && (stack.getItem() instanceof MapItem || isAtlas(stack));
    }

    private static boolean isTaggedOldGuide(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof MapItem)) return false;
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.getBoolean(OLD_GUIDE_TAG)) return true;
        return stack.hasCustomHoverName() && OLD_GUIDE_NAME.equals(stack.getHoverName().getString());
    }

    private static boolean isAtlas(ItemStack stack) {
        return stack != null && !stack.isEmpty() && ATLAS_ID.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }
}
