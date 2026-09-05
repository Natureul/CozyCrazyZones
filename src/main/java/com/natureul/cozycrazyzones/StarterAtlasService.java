package com.natureul.cozycrazyzones;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

/** Gives the player their own Atlas once, instead of relying on the decorative starter-house desk. */
public final class StarterAtlasService {
    private static final ResourceLocation ATLAS_ID = new ResourceLocation("map_atlases", "atlas");
    private static final String GRANTED_TAG = "CozyCrazyZonesStarterAtlasGranted";

    private StarterAtlasService() {}

    public static void ensureStarterAtlas(ServerPlayer player) {
        if (!ModList.get().isLoaded("map_atlases")) return;

        CompoundTag persistent = player.getPersistentData();
        if (persistent.getBoolean(GRANTED_TAG)) return;

        // Respect an Atlas the player already has (commands, imported player data, etc.).
        if (ownsAtlas(player)) {
            persistent.putBoolean(GRANTED_TAG, true);
            return;
        }

        Item atlasItem = ForgeRegistries.ITEMS.getValue(ATLAS_ID);
        if (atlasItem == null) {
            CozyCrazyZones.LOGGER.warn("Map Atlases is loaded but map_atlases:atlas was not registered");
            return;
        }

        ItemStack atlas = new ItemStack(atlasItem);
        if (!player.getInventory().add(atlas)) {
            player.drop(atlas, false);
        }
        persistent.putBoolean(GRANTED_TAG, true);
        CozyCrazyZones.LOGGER.info("Granted {} a personal starter Atlas", player.getGameProfile().getName());
    }

    private static boolean ownsAtlas(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty()) continue;
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (ATLAS_ID.equals(id)) return true;
        }
        return false;
    }
}
