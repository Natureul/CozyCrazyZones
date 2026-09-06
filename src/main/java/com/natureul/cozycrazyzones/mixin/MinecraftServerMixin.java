package com.natureul.cozycrazyzones.mixin;

import com.natureul.cozycrazyzones.CozyCrazyZones;
import com.natureul.cozycrazyzones.StarterLandSelector;
import com.natureul.cozycrazyzones.VillageRingPlanner;
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
        BlockPos vanillaSpawn = level.getSharedSpawnPos();
        BlockPos selectedSpawn = debugWorld ? vanillaSpawn : StarterLandSelector.choose(level, vanillaSpawn);

        if (!selectedSpawn.equals(vanillaSpawn)) {
            levelData.setSpawn(selectedSpawn, 0.0F);
            CozyCrazyZones.LOGGER.info(
                    "Moved initial shared spawn from {} to naturally land-rich Hearthlands center {}",
                    vanillaSpawn,
                    selectedSpawn
            );
        }

        BlockPos spawn = level.getSharedSpawnPos();
        WorldGeographyContext.setSharedSpawn(spawn);
        CozyCrazyZones.LOGGER.info("Initial shared spawn selected {}; regional worldgen anchor snapped before start-region generation", spawn);

        // Reserve all four authored Hearthlands settlements now, while the world itself is being
        // initialized. Player login/Atlas code merely consumes this already-complete world plan.
        var generator = level.getChunkSource().getGenerator();
        var targets = VillageRingPlanner.targetsFor(
                level,
                generator,
                level.getChunkSource().getGeneratorState(),
                level.registryAccess()
        );
        if (targets.size() == 4) {
            CozyCrazyZones.LOGGER.info("World creation reserved all four Hearthlands starter village anchors");
        } else {
            CozyCrazyZones.LOGGER.error(
                    "World creation expected four Hearthlands village anchors but reserved {}",
                    targets.size()
            );
        }
    }
}
