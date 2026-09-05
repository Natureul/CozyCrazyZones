package com.natureul.cozycrazyzones;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(CozyCrazyZones.MOD_ID)
public final class CozyCrazyZones {
    public static final String MOD_ID = "cozycrazyzones";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CozyCrazyZones() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CozyZonesConfig.COMMON_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CozyZonesConfig.CLIENT_SPEC);
        MinecraftForge.EVENT_BUS.register(ZoneServerEvents.class);
    }
}
