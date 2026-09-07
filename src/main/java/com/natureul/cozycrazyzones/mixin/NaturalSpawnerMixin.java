package com.natureul.cozycrazyzones.mixin;

import com.natureul.cozycrazyzones.RegionalSpawnInjector;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Lets CozyCrazyZones replace part of vanilla's candidate selection without creating a second spawn
 * scheduler. The canSpawnMobAt hook bypasses only vanilla's static-biome-list membership check for
 * the active injected candidate; SpawnPlacements/light/collision/entity checks still execute after it.
 */
@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerMixin {

    @Inject(method = "getRandomSpawnMobAt", at = @At("HEAD"), cancellable = true)
    private static void cozyzones$regionalCandidate(ServerLevel level,
                                                     StructureManager structureManager,
                                                     ChunkGenerator generator,
                                                     MobCategory category,
                                                     RandomSource random,
                                                     BlockPos pos,
                                                     CallbackInfoReturnable<Optional<MobSpawnSettings.SpawnerData>> cir) {
        Optional<MobSpawnSettings.SpawnerData> injected = RegionalSpawnInjector.tryInject(level, category, random, pos);
        if (injected.isPresent()) cir.setReturnValue(injected);
    }

    @Inject(method = "canSpawnMobAt", at = @At("HEAD"), cancellable = true)
    private static void cozyzones$allowDynamicCandidate(ServerLevel level,
                                                        StructureManager structureManager,
                                                        ChunkGenerator generator,
                                                        MobCategory category,
                                                        MobSpawnSettings.SpawnerData data,
                                                        BlockPos pos,
                                                        CallbackInfoReturnable<Boolean> cir) {
        if (RegionalSpawnInjector.matchesInjected(level, data.type, pos)) {
            cir.setReturnValue(true);
        }
    }
}
