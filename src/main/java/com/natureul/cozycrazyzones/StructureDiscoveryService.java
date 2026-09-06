package com.natureul.cozycrazyzones;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.Map;

/**
 * Cheap loaded-position-only named-place discovery.
 *
 * Once per second we ask Minecraft which already-generated structures contain the player's current
 * block. There are no radius scans and no locate calls. The resulting place name is world-persistent;
 * player discovery state and Atlas markers remain per-player.
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
            if (structureId == null) continue;

            StructureDiscoveryProfile profile = StructureDiscoveryProfile.classify(registry, structure, structureId);
            if (profile == null) continue;

            StructureStart start = level.structureManager().getStructureAt(player.blockPosition(), structure);
            if (start == null || !start.isValid() || !start.getBoundingBox().isInside(player.blockPosition())) continue;

            discover(player, level, structureId, start, profile);
        }
    }

    public static void copyPersistentState(ServerPlayer original, ServerPlayer replacement) {
        CompoundTag old = original.getPersistentData().getCompound(DISCOVERED_TAG);
        if (!old.isEmpty()) replacement.getPersistentData().put(DISCOVERED_TAG, old.copy());
    }

    private static void discover(ServerPlayer player,
                                 ServerLevel level,
                                 ResourceLocation structureId,
                                 StructureStart start,
                                 StructureDiscoveryProfile profile) {
        ChunkPos startChunk = start.getChunkPos();
        String discoveryKey = profile.category() == DiscoveryCategory.VILLAGE
                ? VillageNameSavedData.keyFor(startChunk)
                : StructureNameSavedData.keyFor(structureId, startChunk);

        BoundingBox box = start.getBoundingBox();
        int x = (box.minX() + box.maxX()) / 2;
        int z = (box.minZ() + box.maxZ()) / 2;
        BlockPos marker = new BlockPos(x, player.blockPosition().getY(), z);
        MacroRegion region = CozyZonesApi.regionalCellAt(level, x, z).macroRegion();

        String name = profile.category() == DiscoveryCategory.VILLAGE
                ? VillageNameSavedData.get(level).getOrAssign(region, level.getSeed(), startChunk)
                : StructureNameSavedData.get(level).getOrAssign(profile, region, level.getSeed(), structureId, startChunk);

        CompoundTag discovered = player.getPersistentData().getCompound(DISCOVERED_TAG);
        if (discovered.getBoolean(discoveryKey)) {
            // v0.3.17 knew only village booleans + a Moonlight pin. Re-entering an old discovery
            // silently upgrades it into the permanent named/category marker ledger without replaying
            // the announcement or stinger.
            if (!AtlasDiscoveryMarkerService.hasKnownMarker(player, discoveryKey)) {
                AtlasDiscoveryMarkerService.enqueue(
                        player,
                        discoveryKey,
                        profile.category(),
                        name,
                        marker,
                        profile.icon()
                );
            }
            return;
        }

        // Commit discovery before presentation. A third-party Atlas error can never cause repeated
        // title/audio spam while the player walks around inside a large structure.
        discovered.putBoolean(discoveryKey, true);
        player.getPersistentData().put(DISCOVERED_TAG, discovered);

        player.displayClientMessage(
                Component.literal("✦ " + profile.kind() + " discovered: ")
                        .append(Component.literal(name).withStyle(region.formatting()))
                        .append(Component.literal(" · " + region.displayName())),
                true
        );

        if (profile.category() == DiscoveryCategory.VILLAGE) {
            StingerService.queueVillage(player, region);
        } else {
            StingerService.queueDiscovery(player, profile.category(), region, profile.major());
        }

        AtlasDiscoveryMarkerService.enqueue(
                player,
                discoveryKey,
                profile.category(),
                name,
                marker,
                profile.icon()
        );

        CozyCrazyZones.LOGGER.info(
                "{} discovered {} '{}' [{}] at start chunk {},{} ({})",
                player.getGameProfile().getName(),
                profile.kind(),
                name,
                structureId,
                startChunk.x,
                startChunk.z,
                region.displayName()
        );
    }
}
