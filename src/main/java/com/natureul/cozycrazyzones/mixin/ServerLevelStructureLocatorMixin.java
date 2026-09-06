package com.natureul.cozycrazyzones.mixin;

import com.natureul.cozycrazyzones.CozyCrazyZones;
import com.natureul.cozycrazyzones.FinalDestinationPolicy;
import com.natureul.cozycrazyzones.MacroRegion;
import com.natureul.cozycrazyzones.RegionalCell;
import com.natureul.cozycrazyzones.WorldGeographyContext;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Filters third-party structure locators through the same geography contract used by generation.
 *
 * Cataclysm's Desert Eye and Aquamirae's structure maps both call
 * ServerLevel#findNearestMapStructure on raw structure-placement candidates. CozyCrazyZones gates
 * the actual starts later, so without this filter those items can point at a candidate that our
 * generator will correctly refuse to build. A locator that points to a nonexistent 20k-block
 * destination is worse than a locator that says "nothing nearby yet".
 *
 * We intentionally do not enlarge either mod's search radius or manufacture a destination here.
 * If the nearest raw candidate is outside the authored regional final belt, the lookup simply
 * returns null. Once the player is in the correct Dread country, normal upstream locating resumes.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelStructureLocatorMixin {
    @Unique
    private static final ResourceLocation COZYZONES$CATACLYSM_DESERT_EYE = id("cataclysm:eye_of_desert_located");
    @Unique
    private static final ResourceLocation COZYZONES$AQUAMIRAE_SHIP = id("aquamirae:ship");
    @Unique
    private static final ResourceLocation COZYZONES$AQUAMIRAE_OUTPOST = id("aquamirae:outpost");
    @Unique
    private static final ResourceLocation COZYZONES$AQUAMIRAE_SHELTER = id("aquamirae:shelter");
    @Unique
    private static final AtomicBoolean COZYZONES$FIRST_REJECTION_LOGGED = new AtomicBoolean();

    @Inject(method = "findNearestMapStructure", at = @At("RETURN"), cancellable = true)
    private void cozyzones$filterFinalDestinationLocator(TagKey<Structure> structureTag,
                                                          BlockPos origin,
                                                          int radius,
                                                          boolean skipKnownStructures,
                                                          CallbackInfoReturnable<BlockPos> cir) {
        BlockPos candidate = cir.getReturnValue();
        if (candidate == null || !WorldGeographyContext.prepared()) return;

        ServerLevel level = (ServerLevel) (Object) this;
        if (level.dimension() != Level.OVERWORLD) return;

        MacroRegion expected = cozyzones$expectedRegion(structureTag.location());
        if (expected == null) return;

        RegionalCell cell = WorldGeographyContext.cellAt(candidate.getX(), candidate.getZ());
        if (FinalDestinationPolicy.allowsFinalLocation(expected, cell)) return;

        cir.setReturnValue(null);
        if (COZYZONES$FIRST_REJECTION_LOGGED.compareAndSet(false, true)) {
            CozyCrazyZones.LOGGER.info(
                    "Rejected misleading final-destination locator candidate for {} at {},{}: candidate lies in {} / {} blocks from shared spawn rather than the legal {} Dread expedition belt",
                    structureTag.location(),
                    candidate.getX(),
                    candidate.getZ(),
                    cell.macroRegion().displayName(),
                    Math.round(cell.distanceFromSpawn()),
                    expected.displayName()
            );
        }
    }

    @Unique
    private static MacroRegion cozyzones$expectedRegion(ResourceLocation tagId) {
        if (COZYZONES$CATACLYSM_DESERT_EYE.equals(tagId)) return MacroRegion.SOUTH;
        if (COZYZONES$AQUAMIRAE_SHIP.equals(tagId)
                || COZYZONES$AQUAMIRAE_OUTPOST.equals(tagId)
                || COZYZONES$AQUAMIRAE_SHELTER.equals(tagId)) {
            return MacroRegion.NORTH;
        }
        return null;
    }

    @Unique
    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }
}
