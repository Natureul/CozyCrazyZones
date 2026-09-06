package com.natureul.cozycrazyzones.mixin;

import com.natureul.cozycrazyzones.MacroRegion;
import com.natureul.cozycrazyzones.RegionalCell;
import com.natureul.cozycrazyzones.RegionalInfluenceBand;
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
 * Aspen Glade is a Harvestwood/autumn biome, never neutral starter countryside.
 * It is legal only once the player is in established WEST country. Provisional spawn search,
 * Shared Core, the entire cardinal transition band and every other macro-region replace it.
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
    private void cozyzones$keepAspenAutumnal(int quartX,
                                             int quartY,
                                             int quartZ,
                                             Climate.Sampler sampler,
                                             CallbackInfoReturnable<Holder<Biome>> cir) {
        if (!WorldGeographyContext.prepared() || QuartPos.toBlock(quartY) < 48) return;

        Holder<Biome> returned = cir.getReturnValue();
        if (returned == null) return;
        ResourceLocation returnedId = returned.unwrapKey().map(key -> key.location()).orElse(null);
        if (!COZYZONES$ASPEN.equals(returnedId)) return;

        int blockX = QuartPos.toBlock(quartX);
        int blockZ = QuartPos.toBlock(quartZ);
        RegionalCell cell = WorldGeographyContext.cellAt(blockX, blockZ);

        boolean legalHarvestwoodAspen = !WorldGeographyContext.provisionalAnchor()
                && cell.macroRegion() == MacroRegion.WEST
                && cell.influenceBand() == RegionalInfluenceBand.ESTABLISHED
                && cell.macroBoundaryStrength() >= 0.42D;
        if (legalHarvestwoodAspen) return;

        Holder<Biome> replacement;
        if (WorldGeographyContext.provisionalAnchor()
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
