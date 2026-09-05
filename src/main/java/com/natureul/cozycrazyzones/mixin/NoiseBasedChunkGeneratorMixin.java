package com.natureul.cozycrazyzones.mixin;

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

    /**
     * TerraBlender/Citadel finish their native biome selection first. We then rewrite the completed
     * chunk palette, avoiding all getNoiseBiome mixin-order races.
     *
     * Important: terrain density itself is intentionally left to Tectonic/Minecraft. Earlier builds
     * tried to turn native ocean basins into land after density generation; that produced flat plates,
     * blocky shelf walls and stranded ocean structures. CozyCrazyZones now keeps native terrain intact
     * and solves the "starter region swallowed by ocean" problem by choosing a land-rich shared spawn.
     */
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
            return generated;
        }));
    }
}
