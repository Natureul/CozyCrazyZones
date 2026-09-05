package com.natureul.cozycrazyzones.client;

import com.natureul.cozycrazyzones.MacroRegion;
import com.natureul.cozycrazyzones.Region;
import com.natureul.cozycrazyzones.RegionalInfluenceBand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientRegionState {
    private static Region region;
    private static MacroRegion macroRegion;
    private static RegionalInfluenceBand influenceBand;

    private ClientRegionState() {}

    public static void setOrdinals(int radialOrdinal, int macroOrdinal, int influenceOrdinal) {
        Region[] radialValues = Region.values();
        MacroRegion[] macroValues = MacroRegion.values();
        RegionalInfluenceBand[] influenceValues = RegionalInfluenceBand.values();
        region = radialOrdinal >= 0 && radialOrdinal < radialValues.length ? radialValues[radialOrdinal] : null;
        macroRegion = macroOrdinal >= 0 && macroOrdinal < macroValues.length ? macroValues[macroOrdinal] : null;
        influenceBand = influenceOrdinal >= 0 && influenceOrdinal < influenceValues.length ? influenceValues[influenceOrdinal] : null;
    }

    public static Region region() { return region; }
    public static MacroRegion macroRegion() { return macroRegion; }
    public static RegionalInfluenceBand influenceBand() { return influenceBand; }
}
