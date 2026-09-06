package com.natureul.cozycrazyzones;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.Map;

/**
 * Cheap, loaded-position-only structure discovery.
 *
 * There is deliberately no radius scan and no locate call here. Once per second we ask Minecraft
 * which already-generated structures contain the player's current block. v0.3.17 enables villages;
 * the category substrate is intentionally ready for the later dungeon/temple/ruin pass.
 */
public final class StructureDiscoveryService {
    private static final String DISCOVERED_TAG = "cozycrazyzones:discovered_structures";

    private StructureDiscoveryService() {}

    public static void tick(ServerPlayer player) {
        if (player.serverLevel().dimension() != Level.OVERWORLD) return;
        ServerLevel level = player.serverLevel();

        Map<Structure, it.unimi.dsi.fastutil.longs.LongSet> structures =
                level.structureManager().getAllStructuresAt(player.blockPosition());
        if (structures.isEmpty()) return;

        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (Structure structure : structures.keySet()) {
            ResourceLocation structureId = registry.getKey(structure);
            if (structureId == null || !isVillage(registry, structure, structureId)) continue;

            StructureStart start = level.structureManager().getStructureAt(player.blockPosition(), structure);
            if (start == null || !start.isValid() || !start.getBoundingBox().isInside(player.blockPosition())) continue;

            discoverVillage(player, level, start);
        }
    }

    public static void copyPersistentState(ServerPlayer original, ServerPlayer replacement) {
        CompoundTag old = original.getPersistentData().getCompound(DISCOVERED_TAG);
        if (!old.isEmpty()) replacement.getPersistentData().put(DISCOVERED_TAG, old.copy());
    }

    private static boolean isVillage(Registry<Structure> registry, Structure structure, ResourceLocation id) {
        boolean tagged = registry.getTag(StructureTags.VILLAGE)
                .map(set -> set.stream().anyMatch(holder -> holder.value() == structure))
                .orElse(false);
        // A few structure mods neglect the vanilla tag. The fallback catches sensible village ids
        // while staying narrow enough not to turn every settlement-like dungeon into a village.
        return tagged || id.getPath().contains("village");
    }

    private static void discoverVillage(ServerPlayer player, ServerLevel level, StructureStart start) {
        ChunkPos startChunk = start.getChunkPos();
        String discoveryKey = VillageNameSavedData.keyFor(startChunk);
        CompoundTag discovered = player.getPersistentData().getCompound(DISCOVERED_TAG);
        if (discovered.getBoolean(discoveryKey)) return;

        int x = startChunk.getMiddleBlockX();
        int z = startChunk.getMiddleBlockZ();
        MacroRegion region = CozyZonesApi.regionalCellAt(level, x, z).macroRegion();
        String name = VillageNameSavedData.get(level).getOrAssign(region, level.getSeed(), startChunk);
        BlockPos marker = new BlockPos(x, player.blockPosition().getY(), z);

        // Mark discovered before presentation: even if a third-party Atlas renderer throws later,
        // walking around the same village will never spam the cue every second.
        discovered.putBoolean(discoveryKey, true);
        player.getPersistentData().put(DISCOVERED_TAG, discovered);

        player.displayClientMessage(
                Component.literal("✦ Village discovered: ")
                        .append(Component.literal(name).withStyle(region.formatting()))
                        .append(Component.literal(" · " + region.displayName())),
                true
        );
        StingerService.queueVillage(player, region);
        AtlasDiscoveryMarkerService.enqueue(
                player,
                discoveryKey,
                DiscoveryCategory.VILLAGE,
                name,
                marker
        );

        CozyCrazyZones.LOGGER.info(
                "{} discovered village '{}' at start chunk {},{} ({})",
                player.getGameProfile().getName(), name, startChunk.x, startChunk.z, region.displayName()
        );
    }
}
