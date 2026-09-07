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
 * Names are assigned once per logical village and persisted in the Overworld data storage. Starter
 * villages may have an authored reserved anchor plus a nearby vanilla structure start; aliases let
 * both coordinates resolve to one canonical settlement identity instead of inventing two names for
 * what the player experiences as the same village.
 */
public final class VillageNameSavedData extends SavedData {
    private static final String DATA_NAME = "cozycrazyzones_village_names";
    private static final String NAMES_TAG = "Names";
    private static final String ALIASES_TAG = "Aliases";
    private static final int MAX_NAMED_CANDIDATES = 4096;

    private final Map<String, String> names = new HashMap<>();
    private final Map<String, String> aliases = new HashMap<>();
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

        CompoundTag aliasesTag = root.getCompound(ALIASES_TAG);
        for (String key : aliasesTag.getAllKeys()) {
            String value = aliasesTag.getString(key);
            if (!value.isBlank() && !key.equals(value)) data.aliases.put(key, value);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        CompoundTag namesTag = new CompoundTag();
        names.forEach(namesTag::putString);
        root.put(NAMES_TAG, namesTag);

        CompoundTag aliasesTag = new CompoundTag();
        aliases.forEach(aliasesTag::putString);
        root.put(ALIASES_TAG, aliasesTag);
        return root;
    }

    public String getOrAssign(MacroRegion region, long worldSeed, ChunkPos villageStart) {
        String key = resolveKey(keyFor(villageStart));
        String existing = names.get(key);
        if (existing != null) return existing;

        ChunkPos namingStart = parseKey(key);
        if (namingStart == null) namingStart = villageStart;
        for (int attempt = 0; attempt < MAX_NAMED_CANDIDATES; attempt++) {
            String candidate = HearthVillageNames.candidateFor(region, worldSeed, namingStart, attempt);
            if (used.add(candidate)) {
                names.put(key, candidate);
                setDirty();
                return candidate;
            }
        }

        String fallbackBase = HearthVillageNames.nameFor(region, worldSeed, namingStart);
        String fallback = fallbackBase + " " + Math.abs(namingStart.x) + "·" + Math.abs(namingStart.z);
        int suffix = 2;
        while (!used.add(fallback)) fallback = fallbackBase + " " + Math.abs(namingStart.x) + "·" + Math.abs(namingStart.z) + "-" + suffix++;
        names.put(key, fallback);
        setDirty();
        return fallback;
    }

    public String getIfAssigned(ChunkPos villageStart) {
        return names.get(resolveKey(keyFor(villageStart)));
    }

    /** Bind a secondary/physical village start to the reserved canonical starter-village anchor. */
    public void bindAlias(ChunkPos aliasStart, ChunkPos canonicalStart) {
        String aliasKey = keyFor(aliasStart);
        String canonicalKey = resolveKey(keyFor(canonicalStart));
        if (aliasKey.equals(canonicalKey)) return;

        String retiredName = names.remove(aliasKey);
        if (retiredName != null && !names.containsValue(retiredName)) used.remove(retiredName);

        String previous = aliases.put(aliasKey, canonicalKey);
        if (!canonicalKey.equals(previous) || retiredName != null) setDirty();
    }

    public String canonicalKeyFor(ChunkPos villageStart) {
        return resolveKey(keyFor(villageStart));
    }

    public static String keyFor(ChunkPos villageStart) {
        return "village@" + villageStart.x + "," + villageStart.z;
    }

    private String resolveKey(String key) {
        String current = key;
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 16 && seen.add(current); i++) {
            String next = aliases.get(current);
            if (next == null || next.equals(current)) break;
            current = next;
        }
        return current;
    }

    private static ChunkPos parseKey(String key) {
        if (key == null || !key.startsWith("village@")) return null;
        String coords = key.substring("village@".length());
        int comma = coords.indexOf(',');
        if (comma <= 0 || comma + 1 >= coords.length()) return null;
        try {
            return new ChunkPos(
                    Integer.parseInt(coords.substring(0, comma)),
                    Integer.parseInt(coords.substring(comma + 1))
            );
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
