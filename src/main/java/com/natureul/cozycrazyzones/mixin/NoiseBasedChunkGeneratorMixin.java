package com.natureul.cozycrazyzones.mixin;

import com.natureul.cozycrazyzones.HearthlandsLandShaper;
import com.natureul.cozycrazyzones.WorldGeographyContext;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseBasedChunkGeneratorMixin {
    @Inject(method = "fillFromNoise", at = @At("RETURN"), cancellable = true)
    private void cozyzones$reduceHearthlandsOcean(Executor executor,
                                                   Blender blender,
                                                   RandomState randomState,
                                                   StructureManager structureManager,
                                                   ChunkAccess chunk,
                                                   CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir) {
        if (!WorldGeographyContext.prepared()) return;

        NoiseBasedChunkGenerator generator = (NoiseBasedChunkGenerator) (Object) this;
        int seaLevel = generator.getSeaLevel();
        CompletableFuture<ChunkAccess> original = cir.getReturnValue();

        cir.setReturnValue(original.thenApply(generated -> {
            HearthlandsLandShaper.shape(generated, seaLevel);
            return generated;
        }));
    }
}
