package com.natureul.cozycrazyzones;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** World-global persistent name ledger for non-village structures. */
public final class StructureNameSavedData extends SavedData {
    private static final String DATA_NAME = "cozycrazyzones_structure_names";
    private static final String NAMES_TAG = "Names";
    private static final int MAX_CANDIDATES = 4096;

    private final Map<String, String> names = new HashMap<>();
    private final Set<String> used = new HashSet<>();

    public StructureNameSavedData() {}

    public static StructureNameSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                StructureNameSavedData::load,
                StructureNameSavedData::new,
                DATA_NAME
        );
    }

    public static StructureNameSavedData load(CompoundTag root) {
        StructureNameSavedData data = new StructureNameSavedData();
        CompoundTag namesTag = root.getCompound(NAMES_TAG);
        for (String key : namesTag.getAllKeys()) {
            String value = namesTag.getString(key);
            if (value.isBlank()) continue;
            data.names.put(key, value);
            data.used.add(value);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        CompoundTag namesTag = new CompoundTag();
        names.forEach(namesTag::putString);
        root.put(NAMES_TAG, namesTag);
        return root;
    }

    public String getOrAssign(StructureDiscoveryProfile profile,
                              RegionalCell cell,
                              long worldSeed,
                              ResourceLocation structureId,
                              ChunkPos start) {
        String key = keyFor(structureId, start);
        String existing = names.get(key);
        if (existing != null) {
            if (StructurePlaceNameGenerator.isTunnelGoreStructure(structureId)
                    && StructurePlaceNameGenerator.looksLikeLegacyTunnelGoreName(existing)) {
                used.remove(existing);
                names.remove(key);
            } else {
                return existing;
            }
        }

        for (int attempt = 0; attempt < MAX_CANDIDATES; attempt++) {
            String candidate = StructurePlaceNameGenerator.candidate(profile, cell, worldSeed, structureId, start, attempt);
            if (used.add(candidate)) {
                names.put(key, candidate);
                setDirty();
                return candidate;
            }
        }

        String fallback = profile.kind() + " " + Math.abs(start.x) + "·" + Math.abs(start.z);
        int suffix = 2;
        while (!used.add(fallback)) fallback = profile.kind() + " " + Math.abs(start.x) + "·" + Math.abs(start.z) + "-" + suffix++;
        names.put(key, fallback);
        setDirty();
        return fallback;
    }

    public String getIfAssigned(ResourceLocation structureId, ChunkPos start) {
        return names.get(keyFor(structureId, start));
    }

    public static String keyFor(ResourceLocation structureId, ChunkPos start) {
        return "structure@" + structureId + "@" + start.x + "," + start.z;
    }
}
