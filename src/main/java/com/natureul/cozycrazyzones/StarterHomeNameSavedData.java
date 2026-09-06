package com.natureul.cozycrazyzones;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Persistent identity for the canonical starter house at shared spawn. */
public final class StarterHomeNameSavedData extends SavedData {
    private static final String DATA_NAME = "cozycrazyzones_starter_home";
    private static final String NAME_TAG = "Name";

    private String name = "";

    public StarterHomeNameSavedData() {}

    public static StarterHomeNameSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                StarterHomeNameSavedData::load,
                StarterHomeNameSavedData::new,
                DATA_NAME
        );
    }

    public static StarterHomeNameSavedData load(CompoundTag tag) {
        StarterHomeNameSavedData data = new StarterHomeNameSavedData();
        data.name = tag.getString(NAME_TAG);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putString(NAME_TAG, name);
        return tag;
    }

    public String getOrAssign(long worldSeed) {
        if (name == null || name.isBlank()) {
            name = HearthlandsNeutralNames.starterHomeName(worldSeed);
            setDirty();
        }
        return name;
    }
}
