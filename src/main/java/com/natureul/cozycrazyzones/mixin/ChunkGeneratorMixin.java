package com.natureul.cozycrazyzones.mixin;

import com.natureul.cozycrazyzones.CozyCrazyZones;
import com.natureul.cozycrazyzones.CozyZonesApi;
import com.natureul.cozycrazyzones.FinalDestinationPolicy;
import com.natureul.cozycrazyzones.MacroRegion;
import com.natureul.cozycrazyzones.RegionalCell;
import com.natureul.cozycrazyzones.VillageRingPlanner;
import com.natureul.cozycrazyzones.WorldGeographyContext;
import com.natureul.cozycrazyzones.ZoneRuleRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin {
    private static final ResourceLocation COZYZONES$VILLAGE_PLAINS = new ResourceLocation("minecraft", "village_plains");
    private static final ResourceLocation COZYZONES$VILLAGE_SAVANNA = new ResourceLocation("minecraft", "village_savanna");
    private static final ResourceLocation COZYZONES$VILLAGE_SNOWY = new ResourceLocation("minecraft", "village_snowy");
    private static final ResourceLocation COZYZONES$VILLAGE_TAIGA = new ResourceLocation("minecraft", "village_taiga");
    private static final ResourceLocation COZYZONES$VILLAGE_DESERT = new ResourceLocation("minecraft", "village_desert");

    @Inject(method = "tryGenerateStructure", at = @At("HEAD"), cancellable = true)
    private void cozyzones$gateStructure(StructureSet.StructureSelectionEntry entry,
                                        StructureManager structureManager,
                                        RegistryAccess registryAccess,
                                        RandomState randomState,
                                        StructureTemplateManager templateManager,
                                        long seed,
                                        ChunkAccess chunk,
                                        ChunkPos chunkPos,
                                        SectionPos sectionPos,
                                        CallbackInfoReturnable<Boolean> cir) {
        if (!(((StructureManagerAccessor) structureManager).cozyzones$getLevelAccessor() instanceof net.minecraft.world.level.ServerLevelAccessor accessor)) return;
        ServerLevel level = accessor.getLevel();

        Structure structure = entry.structure().value();
        ResourceLocation id = registryAccess.registryOrThrow(Registries.STRUCTURE).getKey(structure);
        if (id == null) return;

        // Global suppression rules (currently Cataclysm's non-pyramid structures) must work in
        // Nether/End too. Radial/cardinal geography itself remains Overworld-only.
        if (level.dimension() != Level.OVERWORLD) {
            if (ZoneRuleRegistry.structureExplicitlySuppressed(id)) cir.setReturnValue(false);
            return;
        }

        double x = chunkPos.getMinBlockX() + 8.0D;
        double z = chunkPos.getMinBlockZ() + 8.0D;

        // The starter house is settlement zero. During vanilla's provisional spawn search there is
        // no authoritative center yet, so suppress village starts entirely. Once spawn is committed,
        // no vanilla village start may begin inside the 1000-block sanctuary.
        if (cozyzones$isVanillaVillage(id)) {
            if (WorldGeographyContext.provisionalAnchor()
                    || CozyZonesApi.distanceFromSpawn(level, x, z) < VillageRingPlanner.MIN_VILLAGE_START_DISTANCE) {
                cir.setReturnValue(false);
                return;
            }
        }

        // Regional finals require both halves of the geography contract: the correct cardinal Dread
        // region AND a finite outer expedition limit. This prevents the first valid Cursed Pyramid
        // or Aquamirae structure from drifting to 20k-30k+ blocks simply because Dread is unbounded.
        if (FinalDestinationPolicy.isFinalStructure(id)) {
            RegionalCell cell = CozyZonesApi.regionalCellAt(level, x, z);
            if (!FinalDestinationPolicy.allowsStructure(id, cell)) {
                cir.setReturnValue(false);
                return;
            }
        }

        if (!CozyZonesApi.structureAllowed(level, id, x, z)) cir.setReturnValue(false);
    }

    /**
     * A normal village placement candidate is reserved around ~1.15k blocks. If vanilla already
     * generated a village there, leave it alone. If its native biome rejected all village starts,
     * force a macro-appropriate vanilla village at that same locator-compatible candidate.
     */
    @Inject(method = "createStructures", at = @At("TAIL"))
    private void cozyzones$ensureFirstVillage(RegistryAccess registryAccess,
                                               ChunkGeneratorStructureState structureState,
                                               StructureManager structureManager,
                                               ChunkAccess chunk,
                                               StructureTemplateManager templateManager,
                                               CallbackInfo ci) {
        if (!WorldGeographyContext.prepared() || WorldGeographyContext.provisionalAnchor()) return;
        if (!structureManager.shouldGenerateStructures()) return;
        if (!(((StructureManagerAccessor) structureManager).cozyzones$getLevelAccessor() instanceof net.minecraft.world.level.ServerLevelAccessor accessor)) return;

        ServerLevel level = accessor.getLevel();
        if (level.dimension() != Level.OVERWORLD) return;

        ChunkGenerator generator = (ChunkGenerator) (Object) this;
        ChunkPos reserved = VillageRingPlanner.targetFor(level, generator, structureState, registryAccess);
        if (reserved == null || !reserved.equals(chunk.getPos())) return;

        Registry<Structure> structures = registryAccess.registryOrThrow(Registries.STRUCTURE);
        for (var existing : chunk.getAllStarts().entrySet()) {
            ResourceLocation existingId = structures.getKey(existing.getKey());
            if (existingId != null && cozyzones$isVanillaVillage(existingId)
                    && existing.getValue() != null && existing.getValue().isValid()) {
                CozyCrazyZones.LOGGER.info(
                        "Reserved first-village candidate {},{} already generated {} normally",
                        reserved.x, reserved.z, existingId
                );
                return;
            }
        }

        RegionalCell cell = WorldGeographyContext.cellAt(reserved.getMiddleBlockX(), reserved.getMiddleBlockZ());
        for (ResourceLocation preferredId : cozyzones$preferredVillageOrder(cell.macroRegion())) {
            Structure structure = structures.get(preferredId);
            if (structure == null) continue;

            StructureStart start = structure.generate(
                    registryAccess,
                    generator,
                    generator.getBiomeSource(),
                    structureState.randomState(),
                    templateManager,
                    structureState.getLevelSeed(),
                    reserved,
                    0,
                    chunk,
                    biome -> true
            );
            if (!start.isValid()) continue;

            structureManager.setStartForStructure(SectionPos.bottomOf(chunk), structure, start, chunk);
            CozyCrazyZones.LOGGER.info(
                    "Guaranteed first village {} at chunk {},{} ({} / {} blocks from spawn)",
                    preferredId,
                    reserved.x,
                    reserved.z,
                    cell.macroRegion().displayName(),
                    Math.round(cell.distanceFromSpawn())
            );
            return;
        }

        CozyCrazyZones.LOGGER.warn(
                "Reserved first-village candidate {},{} was reached, but every forced vanilla village variant returned an invalid start",
                reserved.x, reserved.z
        );
    }

    private static boolean cozyzones$isVanillaVillage(ResourceLocation id) {
        if (!"minecraft".equals(id.getNamespace())) return false;
        return id.equals(COZYZONES$VILLAGE_PLAINS)
                || id.equals(COZYZONES$VILLAGE_SAVANNA)
                || id.equals(COZYZONES$VILLAGE_SNOWY)
                || id.equals(COZYZONES$VILLAGE_TAIGA)
                || id.equals(COZYZONES$VILLAGE_DESERT);
    }

    private static List<ResourceLocation> cozyzones$preferredVillageOrder(MacroRegion region) {
        return switch (region) {
            case NORTH -> List.of(COZYZONES$VILLAGE_TAIGA, COZYZONES$VILLAGE_SNOWY, COZYZONES$VILLAGE_PLAINS);
            case EAST -> List.of(COZYZONES$VILLAGE_PLAINS, COZYZONES$VILLAGE_SAVANNA, COZYZONES$VILLAGE_TAIGA);
            case SOUTH -> List.of(COZYZONES$VILLAGE_SAVANNA, COZYZONES$VILLAGE_DESERT, COZYZONES$VILLAGE_PLAINS);
            case WEST -> List.of(COZYZONES$VILLAGE_TAIGA, COZYZONES$VILLAGE_PLAINS, COZYZONES$VILLAGE_SAVANNA);
        };
    }
}
