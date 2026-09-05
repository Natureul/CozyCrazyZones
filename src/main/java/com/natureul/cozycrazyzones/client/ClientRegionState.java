package com.natureul.cozycrazyzones.client;

import com.natureul.cozycrazyzones.Region;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientRegionState {
    private static Region region;

    private ClientRegionState() {}

    public static void setOrdinal(int ordinal) {
        Region[] values = Region.values();
        region = ordinal >= 0 && ordinal < values.length ? values[ordinal] : null;
    }

    public static Region region() { return region; }
}
