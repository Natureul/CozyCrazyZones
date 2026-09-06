package com.natureul.cozycrazyzones;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Server-side retry/lifecycle hooks for the personal starter Atlas and starter desk decoration. */
@Mod.EventBusSubscriber(modid = CozyCrazyZones.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StarterVillageMapEvents {
    private StarterVillageMapEvents() {}

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // The personal Atlas is the navigation system now. Before exact-route linking inspects
            // nearby item frames, retire the old desk Atlas/map into a simple framed compass so the
            // route service cannot mistake decorative furniture for the player's Atlas.
            StarterAtlasService.ensureStarterAtlas(player);
            StarterDeskDecorationService.begin(player);
            StarterVillageMapService.begin(player);
            VillageNameBootstrapService.begin(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            // Decoration retry stays first for the same race-avoidance reason as login.
            StarterDeskDecorationService.tick(player);
            StarterVillageMapService.tick(player);
            VillageNameBootstrapService.tick(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StarterVillageMapService.remove(player);
            StarterDeskDecorationService.remove(player);
            VillageNameBootstrapService.remove(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        StarterVillageMapService.clear();
        StarterDeskDecorationService.clear();
        VillageNameBootstrapService.clear();
    }
}
