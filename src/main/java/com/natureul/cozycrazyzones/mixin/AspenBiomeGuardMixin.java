package com.natureul.cozycrazyzones.mixin;

import com.natureul.cozycrazyzones.MacroRegion;
import com.natureul.cozycrazyzones.Region;
import com.natureul.cozycrazyzones.RegionalCell;
import com.natureul.cozycrazyzones.RegionalInfluenceBand;
import com.natureul.cozycrazyzones.SharedCoreBiomePolicy;
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

/**
 * Final source-level guard for two worldgen contracts:
 *  - Aspen Glade is outer Harvestwood/autumn country, never Hearthlands countryside.
 *  - The immediate shared Hearthlands may not become enormous bare BOP grassland/prairie belts.
 *
 * Priority 50 intentionally runs after the broader regional remapper so this sees its final answer.
 */
@Mixin(value = MultiNoiseBiomeSource.class, priority = 50)
public abstract class AspenBiomeGuardMixin {
    @Unique
    private static final ResourceLocation COZYZONES$ASPEN = id("biomesoplenty:aspen_glade");

    @Inject(
            method = "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void cozyzones$guardSurfaceBiomes(int quartX,
                                              int quartY,
                                              int quartZ,
                                              Climate.Sampler sampler,
                                              CallbackInfoReturnable<Holder<Biome>> cir) {
        if (!WorldGeographyContext.prepared() || QuartPos.toBlock(quartY) < 48) return;

        Holder<Biome> returned = cir.getReturnValue();
        if (returned == null) return;
        ResourceLocation returnedId = returned.unwrapKey().map(key -> key.location()).orElse(null);
        if (returnedId == null) return;

        int blockX = QuartPos.toBlock(quartX);
        int blockZ = QuartPos.toBlock(quartZ);
        RegionalCell cell = WorldGeographyContext.cellAt(blockX, blockZ);

        ResourceLocation coreTarget = SharedCoreBiomePolicy.temper(
                returnedId,
                cell,
                WorldGeographyContext.worldSeed(),
                blockX,
                blockZ
        );
        if (!coreTarget.equals(returnedId)) {
            Holder<Biome> replacement = cozyzones$firstAvailable(
                    coreTarget,
                    id("minecraft:plains"),
                    id("minecraft:forest"),
                    id("minecraft:birch_forest"),
                    id("minecraft:meadow")
            );
            if (replacement != null) {
                cir.setReturnValue(replacement);
                return;
            }
        }

        if (!COZYZONES$ASPEN.equals(returnedId)) return;

        boolean legalHarvestwoodAspen = !WorldGeographyContext.provisionalAnchor()
                && cell.radialZone() != Region.HEARTHLANDS
                && cell.macroRegion() == MacroRegion.WEST
                && cell.influenceBand() == RegionalInfluenceBand.ESTABLISHED
                && cell.macroBoundaryStrength() >= 0.42D;
        if (legalHarvestwoodAspen) return;

        Holder<Biome> replacement;
        if (WorldGeographyContext.provisionalAnchor()
                || cell.radialZone() == Region.HEARTHLANDS
                || cell.influenceBand() != RegionalInfluenceBand.ESTABLISHED
                || cell.macroBoundaryStrength() < 0.42D) {
            replacement = cozyzones$firstAvailable(
                    id("minecraft:birch_forest"),
                    id("minecraft:forest"),
                    id("biomesoplenty:woodland"),
                    id("minecraft:plains")
            );
        } else {
            replacement = switch (cell.macroRegion()) {
                case NORTH -> cozyzones$firstAvailable(
                        id("minecraft:taiga"),
                        id("biomesoplenty:coniferous_forest"),
                        id("minecraft:birch_forest")
                );
                case EAST -> cozyzones$firstAvailable(
                        id("minecraft:sparse_jungle"),
                        id("biomesoplenty:jacaranda_glade"),
                        id("biomesoplenty:overgrown_greens"),
                        id("minecraft:forest")
                );
                case SOUTH -> cozyzones$firstAvailable(
                        id("minecraft:savanna"),
                        id("biomesoplenty:lush_savanna"),
                        id("minecraft:plains")
                );
                case WEST -> cozyzones$firstAvailable(id("minecraft:birch_forest"), id("minecraft:forest"));
            };
        }

        if (replacement != null) cir.setReturnValue(replacement);
    }

    @Unique
    private Holder<Biome> cozyzones$firstAvailable(ResourceLocation... ids) {
        MultiNoiseBiomeSource source = (MultiNoiseBiomeSource) (Object) this;
        for (ResourceLocation wanted : ids) {
            for (Holder<Biome> holder : source.possibleBiomes()) {
                ResourceLocation id = holder.unwrapKey().map(key -> key.location()).orElse(null);
                if (wanted.equals(id)) return holder;
            }
        }
        return null;
    }

    @Unique
    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }
}
