package com.natureul.cozycrazyzones;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Server-side retry/lifecycle hooks for starter Atlas survey and desk decoration. */
@Mod.EventBusSubscriber(modid = CozyCrazyZones.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StarterVillageMapEvents {
    private StarterVillageMapEvents() {}

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StarterAtlasService.ensureStarterAtlas(player);
            StarterDeskDecorationService.begin(player);
            StarterSurveyService.begin(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            StarterDeskDecorationService.tick(player);
            StarterSurveyService.tick(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StarterDeskDecorationService.remove(player);
            StarterSurveyService.remove(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        StarterDeskDecorationService.clear();
        StarterSurveyService.clear();
    }
}
