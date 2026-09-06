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
 * Aspen Glade is a Harvestwood/autumn biome, not neutral starter countryside.
 *
 * The completed-chunk postprocessor already enforces this visually. This narrow source-layer guard
 * closes the earlier-biome-query path as well, so spawn selection, structures and feature-time biome
 * reads cannot still treat BOP Aspen Glade as a generic forest before the final palette is written.
 * It deliberately touches no biome except Aspen Glade.
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

        // Aspen belongs to established Harvestwood. Near home and on broad macro seams it must read
        // as ordinary shared countryside instead of leaking the western autumn identity into spawn.
        if (cell.influenceBand() == RegionalInfluenceBand.SHARED_CORE || cell.macroBoundaryStrength() < 0.42D) {
            Holder<Biome> replacement = cozyzones$firstAvailable(
                    id("minecraft:birch_forest"),
                    id("minecraft:forest"),
                    id("biomesoplenty:woodland")
            );
            if (replacement != null) cir.setReturnValue(replacement);
            return;
        }

        if (cell.macroRegion() == MacroRegion.WEST) return;

        Holder<Biome> replacement = switch (cell.macroRegion()) {
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
            case WEST -> null;
        };

        if (replacement == null) {
            replacement = cozyzones$firstAvailable(id("minecraft:birch_forest"), id("minecraft:forest"));
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
