package com.natureul.cozycrazyzones.mixin;

import com.natureul.cozycrazyzones.CozyCrazyZones;
import com.natureul.cozycrazyzones.WorldGeographyContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Establishes the regional worldgen anchor inside vanilla level creation, before initial spawn
 * chunks can be generated. Forge's ServerStartedEvent is too late for a brand-new world: by then
 * Minecraft has already selected the shared spawn and generated the 441-chunk start region.
 */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Inject(method = "createLevels", at = @At("HEAD"))
    private void cozyzones$prepareGeographyBeforeLevels(ChunkProgressListener progressListener, CallbackInfo ci) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        ServerLevelData data = server.getWorldData().overworldData();
        long seed = server.getWorldData().worldGenOptions().seed();

        WorldGeographyContext.prepare(seed);

        if (data.isInitialized()) {
            BlockPos savedSpawn = new BlockPos(data.getXSpawn(), data.getYSpawn(), data.getZSpawn());
            WorldGeographyContext.setSharedSpawn(savedSpawn);
            CozyCrazyZones.LOGGER.info("Prepared regional worldgen before level creation using saved shared spawn {}", savedSpawn);
        } else {
            CozyCrazyZones.LOGGER.info("Prepared regional worldgen before initial spawn search for seed {}; temporary anchor is world origin", seed);
        }
    }

    @Inject(method = "setInitialSpawn", at = @At("RETURN"))
    private static void cozyzones$anchorAtInitialSpawn(ServerLevel level,
                                                        ServerLevelData levelData,
                                                        boolean generateBonusChest,
                                                        boolean debugWorld,
                                                        CallbackInfo ci) {
        BlockPos spawn = level.getSharedSpawnPos();
        WorldGeographyContext.setSharedSpawn(spawn);
        CozyCrazyZones.LOGGER.info("Initial shared spawn selected {}; regional worldgen anchor snapped before start-region generation", spawn);
    }
}
