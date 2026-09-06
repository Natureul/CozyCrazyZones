package com.natureul.cozycrazyzones;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Server-side retry/lifecycle hooks for the personal starter Atlas and shared Hearthlands desk map. */
@Mod.EventBusSubscriber(modid = CozyCrazyZones.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StarterVillageMapEvents {
    private StarterVillageMapEvents() {}

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Grant the personal Atlas first, then claim/convert the decorative desk frame into the
            // real filled Hearthlands map before Atlas routing looks for legacy frame-held Atlases.
            // Finally reserve those same four printed names in the world-global discovery ledger.
            StarterAtlasService.ensureStarterAtlas(player);
            StarterDeskVillageMapService.begin(player);
            StarterVillageMapService.begin(player);
            VillageNameBootstrapService.begin(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            // Same ordering during retries: if the starter entities arrive a tick late, the desk
            // frame is still converted before the exact-route service can mistake it for the player Atlas.
            StarterDeskVillageMapService.tick(player);
            StarterVillageMapService.tick(player);
            VillageNameBootstrapService.tick(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StarterVillageMapService.remove(player);
            StarterDeskVillageMapService.remove(player);
            VillageNameBootstrapService.remove(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        StarterVillageMapService.clear();
        StarterDeskVillageMapService.clear();
        VillageNameBootstrapService.clear();
    }
}
