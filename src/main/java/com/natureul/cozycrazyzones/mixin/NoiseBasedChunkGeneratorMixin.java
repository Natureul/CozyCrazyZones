package com.natureul.cozycrazyzones.mixin;

import com.natureul.cozycrazyzones.AspenFinalizer;
import com.natureul.cozycrazyzones.RegionalBiomePostProcessor;
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

    @Inject(method = "createBiomes", at = @At("RETURN"), cancellable = true)
    private void cozyzones$regionalizeCompletedBiomePalette(Executor executor,
                                                             RandomState randomState,
                                                             Blender blender,
                                                             StructureManager structureManager,
                                                             ChunkAccess chunk,
                                                             CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir) {
        if (!WorldGeographyContext.prepared()) return;

        NoiseBasedChunkGenerator generator = (NoiseBasedChunkGenerator) (Object) this;
        int seaLevel = generator.getSeaLevel();
        CompletableFuture<ChunkAccess> original = cir.getReturnValue();

        cir.setReturnValue(original.thenApply(generated -> {
            RegionalBiomePostProcessor.regionalize(
                    generated,
                    seaLevel,
                    generator.getBiomeSource(),
                    randomState.sampler()
            );
            // Defense-in-depth for the one biome that screenshots proved could still leak through
            // another mod's finished-palette path. Fast path is only sixteen biome-holder reads.
            AspenFinalizer.sanitize(
                    generated,
                    generator.getBiomeSource(),
                    randomState.sampler()
            );
            return generated;
        }));
    }
}
