package com.natureul.cozycrazyzones.mixin;

import com.natureul.cozycrazyzones.BiomeRegionality;
import com.natureul.cozycrazyzones.CozyCrazyZones;
import com.natureul.cozycrazyzones.MacroRegion;
import com.natureul.cozycrazyzones.Region;
import com.natureul.cozycrazyzones.RegionalCell;
import com.natureul.cozycrazyzones.RegionalInfluenceBand;
import com.natureul.cozycrazyzones.RegionalNoise;
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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Final surface-biome regionalization pass.
 *
 * TerraBlender injects a cancellable HEAD return into MultiNoiseBiomeSource#getNoiseBiome. This
 * mixin deliberately uses a much lower priority so it is applied after TerraBlender/Citadel have
 * transformed the method; our RETURN hook then sees their synthetic early-return path too. In
 * other words: TerraBlender decides the native biome shape first, CozyCrazyZones regionalizes that
 * finished answer second.
 */
@Mixin(value = MultiNoiseBiomeSource.class, priority = 100)
public abstract class MultiNoiseBiomeSourceMixin {
    @Unique
    private static final ResourceLocation COZYZONES$MOOR = new ResourceLocation("biomesoplenty", "moor");
    @Unique
    private static final AtomicBoolean COZYZONES$FIRST_REMAP_LOGGED = new AtomicBoolean();
    @Unique
    private static final AtomicBoolean COZYZONES$FIRST_OCEAN_SUPPRESSION_LOGGED = new AtomicBoolean();
    @Unique
    private static final AtomicBoolean COZYZONES$MISSING_TARGET_LOGGED = new AtomicBoolean();

    @Unique
    private Map<ResourceLocation, Holder<Biome>> cozyzones$possibleBiomes;

    @Inject(
            method = "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;",
            at = @At("RETURN"),
            cancellable = true
    )
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
        if (originalId == null) return;
        boolean isMoor = COZYZONES$MOOR.equals(originalId);
        if (!isMoor && !BiomeRegionality.isManagedSurfaceBiome(originalId)) return;

        int blockX = QuartPos.toBlock(quartX);
        int blockZ = QuartPos.toBlock(quartZ);
        RegionalCell cell = WorldGeographyContext.cellAt(blockX, blockZ);

        ResourceLocation targetId = isMoor
                ? cozyzones$remapMoor(cell)
                : BiomeRegionality.remap(
                        originalId,
                        cell,
                        WorldGeographyContext.worldSeed(),
                        blockX,
                        blockZ
                );
        targetId = cozyzones$softenHearthlandsOceanEdge(originalId, targetId, cell, blockX, blockZ);
        targetId = cozyzones$reserveIceMazeForDread(originalId, targetId, cell);
        if (targetId.equals(originalId)) return;

        Holder<Biome> replacement = cozyzones$lookup(targetId);
        if (replacement == null) {
            if (COZYZONES$MISSING_TARGET_LOGGED.compareAndSet(false, true)) {
                CozyCrazyZones.LOGGER.warn(
                        "Regional biome remapper wanted {} -> {} at {},{} but the target is absent from this MultiNoiseBiomeSource possible-biome set",
                        originalId, targetId, blockX, blockZ
                );
            }
            return;
        }

        cir.setReturnValue(replacement);

        if (COZYZONES$FIRST_REMAP_LOGGED.compareAndSet(false, true)) {
            CozyCrazyZones.LOGGER.info(
                    "Regional biome remapper ACTIVE after native biome selection: {} -> {} at {},{} ({} / {} / {:.0f} blocks)",
                    originalId,
                    targetId,
                    blockX,
                    blockZ,
                    cell.radialZone().displayName(),
                    cell.influenceBand(),
                    cell.distanceFromSpawn()
            );
        }
        if (BiomeRegionality.isOcean(originalId)
                && !BiomeRegionality.isOcean(targetId)
                && COZYZONES$FIRST_OCEAN_SUPPRESSION_LOGGED.compareAndSet(false, true)) {
            CozyCrazyZones.LOGGER.info(
                    "Hearthlands ocean suppression ACTIVE: {} -> {} at {},{} ({:.0f} blocks from geography anchor)",
                    originalId, targetId, blockX, blockZ, cell.distanceFromSpawn()
            );
        }
    }

    /**
     * The runtime audit identifies BOP Moor as a wet, swamp-tagged mountain biome. Treat it as a
     * mild Greenveil wetland; outside Greenveil it becomes a terrain-compatible regional analogue.
     */
    @Unique
    private ResourceLocation cozyzones$remapMoor(RegionalCell cell) {
        if (cell.influenceBand() == RegionalInfluenceBand.SHARED_CORE || cell.macroBoundaryStrength() < 0.42D) {
            return new ResourceLocation("minecraft", "meadow");
        }

        return switch (cell.macroRegion()) {
            case EAST -> COZYZONES$MOOR;
            case NORTH -> new ResourceLocation("biomesoplenty",
                    cell.radialZone().atLeast(Region.FRONTIER) ? "muskeg" : "bog");
            case SOUTH -> new ResourceLocation("minecraft",
                    cell.radialZone().atLeast(Region.FRONTIER) ? "savanna_plateau" : "savanna");
            case WEST -> new ResourceLocation("biomesoplenty",
                    cell.radialZone().atLeast(Region.WILDLANDS) ? "redwood_forest" : "seasonal_forest");
        };
    }

    /**
     * Ocean suppression is strongest near home, but must not terminate on the exact 2,500-block
     * Frontier circle. Between roughly 1,850 and 2,450 blocks, some original ocean sectors are
     * restored with a broad noise mask. By the boundary, normal ocean geography has returned, so
     * no artificial circular coastline can reveal the gameplay-zone radius.
     */
    @Unique
    private ResourceLocation cozyzones$softenHearthlandsOceanEdge(ResourceLocation originalId,
                                                                   ResourceLocation targetId,
                                                                   RegionalCell cell,
                                                                   int blockX,
                                                                   int blockZ) {
        if (cell.radialZone() != Region.HEARTHLANDS
                || cell.distanceFromSpawn() < 1850.0D
                || !BiomeRegionality.isOcean(originalId)
                || BiomeRegionality.isOcean(targetId)) {
            return targetId;
        }

        double t = cozyzones$smoothstep(1850.0D, 2450.0D, cell.distanceFromSpawn());
        double field = (RegionalNoise.sample(
                WorldGeographyContext.worldSeed() ^ 0x54D3A7B1C62F09E5L,
                blockX,
                blockZ,
                720.0D
        ) + 1.0D) * 0.5D;
        if (field >= t) return targetId;

        boolean deep = originalId.getPath().startsWith("deep_");
        return switch (cell.macroRegion()) {
            case NORTH -> new ResourceLocation("minecraft", deep ? "deep_cold_ocean" : "cold_ocean");
            case EAST, SOUTH -> new ResourceLocation("minecraft", deep ? "deep_lukewarm_ocean" : "lukewarm_ocean");
            case WEST -> new ResourceLocation("minecraft", deep ? "deep_ocean" : "ocean");
        };
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
    private double cozyzones$smoothstep(double edge0, double edge1, double value) {
        if (edge1 <= edge0) return value >= edge1 ? 1.0D : 0.0D;
        double t = Math.max(0.0D, Math.min(1.0D, (value - edge0) / (edge1 - edge0)));
        return t * t * (3.0D - 2.0D * t);
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
