package com.natureul.cozycrazyzones.mixin;

import com.natureul.cozycrazyzones.BiomeRegionality;
import com.natureul.cozycrazyzones.MacroRegion;
import com.natureul.cozycrazyzones.Region;
import com.natureul.cozycrazyzones.RegionalCell;
import com.natureul.cozycrazyzones.WorldGeographyContext;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

@Mixin(MultiNoiseBiomeSource.class)
public abstract class MultiNoiseBiomeSourceMixin {
    @Unique
    private Map<ResourceLocation, Holder<Biome>> cozyzones$possibleBiomes;

    @Inject(method = "getNoiseBiome", at = @At("RETURN"), cancellable = true)
    private void cozyzones$regionalizeSurfaceBiomes(int quartX,
                                                    int quartY,
                                                    int quartZ,
                                                    Climate.Sampler sampler,
                                                    CallbackInfoReturnable<Holder<Biome>> cir) {
        if (!WorldGeographyContext.prepared()) return;

        int blockY = QuartPos.toBlock(quartY);
        if (blockY < 48) return;

        Holder<Biome> original = cir.getReturnValue();
        if (original == null) return;

        ResourceLocation originalId = original.unwrapKey().map(key -> key.location()).orElse(null);
        if (originalId == null || !BiomeRegionality.isManagedSurfaceBiome(originalId)) return;

        int blockX = QuartPos.toBlock(quartX);
        int blockZ = QuartPos.toBlock(quartZ);
        RegionalCell cell = WorldGeographyContext.cellAt(blockX, blockZ);

        ResourceLocation targetId = BiomeRegionality.remap(
                originalId,
                cell,
                WorldGeographyContext.worldSeed(),
                blockX,
                blockZ
        );
        targetId = cozyzones$reserveIceMazeForDread(originalId, targetId, cell);
        if (targetId.equals(originalId)) return;

        Holder<Biome> replacement = cozyzones$lookup(targetId);
        if (replacement != null) cir.setReturnValue(replacement);
    }

    /**
     * In this exact pack Aquamirae marks only frozen_ocean and deep_frozen_ocean with its
     * #aquamirae:ice_maze biome tag. Keeping Frostmarch merely cold until Dread Reaches therefore
     * turns the Ice Maze/Cornelia destination into real outer-region geography rather than letting
     * it consume the northern Wildlands coastline too early.
     */
    @Unique
    private ResourceLocation cozyzones$reserveIceMazeForDread(ResourceLocation originalId,
                                                               ResourceLocation targetId,
                                                               RegionalCell cell) {
        if (cell.macroRegion() != MacroRegion.NORTH || !BiomeRegionality.isOcean(originalId)) return targetId;

        boolean deep = originalId.getPath().startsWith("deep_");
        if (cell.radialZone() == Region.DREAD_REACHES) {
            return new ResourceLocation("minecraft", deep ? "deep_frozen_ocean" : "frozen_ocean");
        }

        String targetPath = targetId.getPath();
        if ("frozen_ocean".equals(targetPath) || "deep_frozen_ocean".equals(targetPath)) {
            return new ResourceLocation("minecraft", deep ? "deep_cold_ocean" : "cold_ocean");
        }
        return targetId;
    }

    @Unique
    private Holder<Biome> cozyzones$lookup(ResourceLocation id) {
        if (cozyzones$possibleBiomes == null) {
            cozyzones$possibleBiomes = new HashMap<>();
            MultiNoiseBiomeSource source = (MultiNoiseBiomeSource) (Object) this;
            for (Holder<Biome> holder : source.possibleBiomes()) {
                holder.unwrapKey().ifPresent(key -> cozyzones$possibleBiomes.put(key.location(), holder));
            }
        }
        return cozyzones$possibleBiomes.get(id);
    }
}
