package com.natureul.cozycrazyzones.mixin;

import com.natureul.cozycrazyzones.CozyZonesApi;
import com.natureul.cozycrazyzones.MacroRegion;
import com.natureul.cozycrazyzones.Region;
import com.natureul.cozycrazyzones.RegionalCell;
import com.natureul.cozycrazyzones.ZoneRuleRegistry;
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
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin {
    private static final Set<ResourceLocation> COZYZONES$AQUAMIRAE_DREAD_STRUCTURES = Set.of(
            new ResourceLocation("aquamirae", "outpost"),
            new ResourceLocation("aquamirae", "shelter"),
            new ResourceLocation("aquamirae", "ship"),
            new ResourceLocation("aquamirae", "surface/arch"),
            new ResourceLocation("aquamirae", "surface/spiral")
    );

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

        // Aquamirae's registered Ice-Maze surface structures are endgame Frostmarch content.
        // Keep this independent of biome remapping so another biome modifier cannot leak one into
        // the Hearthlands or another macro-region.
        if (COZYZONES$AQUAMIRAE_DREAD_STRUCTURES.contains(id)) {
            RegionalCell cell = CozyZonesApi.regionalCellAt(level, x, z);
            if (cell.radialZone() != Region.DREAD_REACHES || cell.macroRegion() != MacroRegion.NORTH) {
                cir.setReturnValue(false);
                return;
            }
        }

        if (!CozyZonesApi.structureAllowed(level, id, x, z)) cir.setReturnValue(false);
    }
}
