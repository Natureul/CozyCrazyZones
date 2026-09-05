package com.natureul.cozycrazyzones;

import net.minecraftforge.common.ForgeConfigSpec;

public final class CozyZonesConfig {
    public enum HudMode { ATLAS_OWNED, ALWAYS, OFF }

    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec CLIENT_SPEC;

    public static final ForgeConfigSpec.IntValue INNER_CORE_RADIUS;
    public static final ForgeConfigSpec.IntValue CARDINAL_ESTABLISHED_RADIUS;
    public static final ForgeConfigSpec.DoubleValue MACRO_BORDER_BLEND_DEGREES;
    public static final ForgeConfigSpec.IntValue FRONTIER_RADIUS;
    public static final ForgeConfigSpec.IntValue WILDLANDS_RADIUS;
    public static final ForgeConfigSpec.IntValue DREAD_RADIUS;
    public static final ForgeConfigSpec.IntValue HYSTERESIS;
    public static final ForgeConfigSpec.IntValue ANNOUNCEMENT_COOLDOWN;
    public static final ForgeConfigSpec.EnumValue<HudMode> HUD_MODE;

    static {
        ForgeConfigSpec.Builder common = new ForgeConfigSpec.Builder();
        common.push("regions");
        INNER_CORE_RADIUS = common.comment(
                "Inside this radius cardinal ecology is intentionally neutral/shared countryside.",
                "Default 700: starter house -> ordinary temperate countryside."
        ).defineInRange("innerCoreRadius", 700, 0, 10000);
        CARDINAL_ESTABLISHED_RADIUS = common.comment(
                "By this radius the cardinal macro-region should be clearly established.",
                "Between innerCoreRadius and this value is the organic cardinal transition band."
        ).defineInRange("cardinalEstablishedRadius", 1200, 1, 20000);
        MACRO_BORDER_BLEND_DEGREES = common.comment(
                "Angular half-width used by future biome/ecology rules to soften warped borders between cardinal macro-regions."
        ).defineInRange("macroBorderBlendDegrees", 11.0D, 1.0D, 30.0D);
        FRONTIER_RADIUS = common.comment("Hearthlands ends at this horizontal distance from actual Overworld spawn.").defineInRange("frontierRadius", 2500, 256, 100000);
        WILDLANDS_RADIUS = common.defineInRange("wildlandsRadius", 5500, 512, 200000);
        DREAD_RADIUS = common.defineInRange("dreadRadius", 9000, 1024, 500000);
        HYSTERESIS = common.comment("Boundary padding used to prevent rapid title/HUD ping-pong.").defineInRange("boundaryHysteresis", 48, 0, 512);
        ANNOUNCEMENT_COOLDOWN = common.comment("Minimum ticks between full region-entry announcements.").defineInRange("announcementCooldownTicks", 200, 0, 2400);
        common.pop();
        COMMON_SPEC = common.build();

        ForgeConfigSpec.Builder client = new ForgeConfigSpec.Builder();
        client.push("hud");
        // persistentMode is intentionally a new key (v0.3.2). Existing v0.3.1 installs therefore
        // receive the new ALWAYS default instead of silently retaining the old ATLAS_OWNED value.
        HUD_MODE = client.comment(
                "Persistent zone badge mode. ALWAYS is the intended CozyCrazyCraft default; ATLAS_OWNED restricts it to players carrying a Map Atlases atlas; OFF hides it."
        ).defineEnum("persistentMode", HudMode.ALWAYS);
        client.pop();
        CLIENT_SPEC = client.build();
    }

    public static int effectiveInnerCoreRadius() {
        int frontier = FRONTIER_RADIUS.get();
        return Math.max(0, Math.min(INNER_CORE_RADIUS.get(), Math.max(0, frontier - 2)));
    }

    public static int effectiveCardinalEstablishedRadius() {
        int core = effectiveInnerCoreRadius();
        int frontier = FRONTIER_RADIUS.get();
        return Math.max(core + 1, Math.min(CARDINAL_ESTABLISHED_RADIUS.get(), Math.max(core + 1, frontier - 1)));
    }

    private CozyZonesConfig() {}
}
