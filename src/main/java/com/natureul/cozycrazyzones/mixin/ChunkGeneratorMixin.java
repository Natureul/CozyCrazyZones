package com.natureul.cozycrazyzones.mixin;

import com.natureul.cozycrazyzones.CozyZonesApi;
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

@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin {
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
        if (!CozyZonesApi.structureAllowed(level, id, x, z)) cir.setReturnValue(false);
    }
}
