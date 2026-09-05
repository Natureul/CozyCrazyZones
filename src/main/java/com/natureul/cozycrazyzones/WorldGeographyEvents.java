package com.natureul.cozycrazyzones;

import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CozyCrazyZones.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WorldGeographyEvents {
    private WorldGeographyEvents() {}

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        long seed = event.getServer().getWorldData().worldGenOptions().seed();
        WorldGeographyContext.prepare(seed);
        CozyCrazyZones.LOGGER.info("Prepared regional worldgen context for seed {}", seed);
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        var overworld = event.getServer().overworld();
        if (overworld == null) return;
        WorldGeographyContext.setSharedSpawn(overworld.getSharedSpawnPos());
        CozyCrazyZones.LOGGER.info("Regional worldgen anchor snapped to shared spawn {}", overworld.getSharedSpawnPos());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        WorldGeographyContext.clear();
    }
}
