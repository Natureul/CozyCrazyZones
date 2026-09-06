package com.natureul.cozycrazyzones;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * World-global village-name ledger.
 *
 * Names are assigned once per village start and persisted in the Overworld data storage, so every
 * player sees the same name and a village never silently changes after a reload. Collision probing
 * makes the assigned names genuinely unique within the world rather than merely "unlikely" to repeat.
 */
public final class VillageNameSavedData extends SavedData {
    private static final String DATA_NAME = "cozycrazyzones_village_names";
    private static final String NAMES_TAG = "Names";
    private static final int MAX_NAMED_CANDIDATES = 4096;

    private final Map<String, String> names = new HashMap<>();
    private final Set<String> used = new HashSet<>();

    public VillageNameSavedData() {}

    public static VillageNameSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                VillageNameSavedData::load,
                VillageNameSavedData::new,
                DATA_NAME
        );
    }

    public static VillageNameSavedData load(CompoundTag root) {
        VillageNameSavedData data = new VillageNameSavedData();
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

    public String getOrAssign(MacroRegion region, long worldSeed, ChunkPos villageStart) {
        String key = keyFor(villageStart);
        String existing = names.get(key);
        if (existing != null) return existing;

        for (int attempt = 0; attempt < MAX_NAMED_CANDIDATES; attempt++) {
            String candidate = HearthVillageNames.candidateFor(region, worldSeed, villageStart, attempt);
            if (used.add(candidate)) {
                names.put(key, candidate);
                setDirty();
                return candidate;
            }
        }

        // The generated pools are large enough that this should never be reached in normal play,
        // but coordinates make the uniqueness contract absolute even in an absurdly village-dense world.
        String fallbackBase = HearthVillageNames.nameFor(region, worldSeed, villageStart);
        String fallback = fallbackBase + " " + Math.abs(villageStart.x) + "·" + Math.abs(villageStart.z);
        int suffix = 2;
        while (!used.add(fallback)) fallback = fallbackBase + " " + Math.abs(villageStart.x) + "·" + Math.abs(villageStart.z) + "-" + suffix++;
        names.put(key, fallback);
        setDirty();
        return fallback;
    }

    public String getIfAssigned(ChunkPos villageStart) {
        return names.get(keyFor(villageStart));
    }

    public static String keyFor(ChunkPos villageStart) {
        return "village@" + villageStart.x + "," + villageStart.z;
    }
}
