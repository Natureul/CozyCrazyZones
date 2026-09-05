package com.natureul.cozycrazyzones;

import net.minecraftforge.common.ForgeConfigSpec;

public final class CozyZonesConfig {
    public enum HudMode { ATLAS_OWNED, ALWAYS, OFF }

    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec CLIENT_SPEC;

    public static final ForgeConfigSpec.IntValue FRONTIER_RADIUS;
    public static final ForgeConfigSpec.IntValue WILDLANDS_RADIUS;
    public static final ForgeConfigSpec.IntValue DREAD_RADIUS;
    public static final ForgeConfigSpec.IntValue HYSTERESIS;
    public static final ForgeConfigSpec.IntValue ANNOUNCEMENT_COOLDOWN;
    public static final ForgeConfigSpec.EnumValue<HudMode> HUD_MODE;

    static {
        ForgeConfigSpec.Builder common = new ForgeConfigSpec.Builder();
        common.push("regions");
        FRONTIER_RADIUS = common.comment("Hearthlands ends at this horizontal distance from actual Overworld spawn.").defineInRange("frontierRadius", 2500, 256, 100000);
        WILDLANDS_RADIUS = common.defineInRange("wildlandsRadius", 5500, 512, 200000);
        DREAD_RADIUS = common.defineInRange("dreadRadius", 9000, 1024, 500000);
        HYSTERESIS = common.comment("Boundary padding used to prevent rapid title/HUD ping-pong.").defineInRange("boundaryHysteresis", 48, 0, 512);
        ANNOUNCEMENT_COOLDOWN = common.comment("Minimum ticks between full region-entry announcements.").defineInRange("announcementCooldownTicks", 200, 0, 2400);
        common.pop();
        COMMON_SPEC = common.build();

        ForgeConfigSpec.Builder client = new ForgeConfigSpec.Builder();
        client.push("hud");
        HUD_MODE = client.comment("ATLAS_OWNED shows the persistent badge only while carrying a Map Atlases atlas. Entry titles always remain enabled.").defineEnum("mode", HudMode.ATLAS_OWNED);
        client.pop();
        CLIENT_SPEC = client.build();
    }

    private CozyZonesConfig() {}
}
