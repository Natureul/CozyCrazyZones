package com.natureul.cozycrazyzones;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Replaces the fragile desk-map navigation concept with a simple survey contract:
 * the personal Atlas begins knowing Home and one real settlement in each cardinal Hearthlands.
 * Terrain is not /located or scanned here; VillageRingPlanner already owns those four real targets.
 */
public final class StarterSurveyService {
    private static final String SURVEY_VERSION_TAG = "CozyCrazyZonesHearthlandsSurveyVersion";
    // v3 canonicalizes nearby physical village starts back to the four reserved settlement identities.
    private static final int SURVEY_VERSION = 3;
    private static final int RETRY_INTERVAL_TICKS = 20;
    private static final int MAX_ATTEMPTS = 45;
    private static final ConcurrentMap<UUID, Integer> PENDING = new ConcurrentHashMap<>();

    private StarterSurveyService() {}

    public static void begin(ServerPlayer player) {
        if (player.serverLevel().dimension() != Level.OVERWORLD) return;
        if (player.getPersistentData().getInt(SURVEY_VERSION_TAG) >= SURVEY_VERSION) return;
        PENDING.put(player.getUUID(), 0);
        if (tryInstall(player)) PENDING.remove(player.getUUID());
    }

    public static void tick(ServerPlayer player) {
        Integer attempts = PENDING.get(player.getUUID());
        if (attempts == null || player.tickCount % RETRY_INTERVAL_TICKS != 0) return;
        if (tryInstall(player)) {
            PENDING.remove(player.getUUID());
            return;
        }
        int next = attempts + 1;
        if (next >= MAX_ATTEMPTS) {
            PENDING.remove(player.getUUID());
            CozyCrazyZones.LOGGER.warn("Could not prepare the Hearthlands starter survey after {} attempts", MAX_ATTEMPTS);
        } else {
            PENDING.put(player.getUUID(), next);
        }
    }

    public static void remove(ServerPlayer player) {
        PENDING.remove(player.getUUID());
    }

    public static void clear() {
        PENDING.clear();
    }

    /** Preserve the survey migration version across Forge's player clone on death/respawn. */
    public static void copyPersistentState(ServerPlayer original, ServerPlayer replacement) {
        int version = original.getPersistentData().getInt(SURVEY_VERSION_TAG);
        if (version > 0) replacement.getPersistentData().putInt(SURVEY_VERSION_TAG, version);
    }

    private static boolean tryInstall(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Map<MacroRegion, ChunkPos> targets = VillageRingPlanner.targetsFor(
                level,
                level.getChunkSource().getGenerator(),
                level.getChunkSource().getGeneratorState(),
                level.registryAccess()
        );
        if (targets.size() != MacroRegion.values().length) return false;

        int previousVersion = player.getPersistentData().getInt(SURVEY_VERSION_TAG);
        String homeName = StarterHomeNameSavedData.get(level).getOrAssign(level.getSeed());
        BlockPos spawn = level.getSharedSpawnPos();
        AtlasDiscoveryMarkerService.enqueue(
                player,
                "starter_home",
                DiscoveryCategory.HOUSE,
                homeName,
                spawn,
                RegionalMapSymbolPolicy.neutralHome()
        );

        VillageNameSavedData villageNames = VillageNameSavedData.get(level);
        for (MacroRegion region : MacroRegion.values()) {
            ChunkPos target = targets.get(region);
            if (target == null) return false;
            String name = villageNames.getOrAssign(region, level.getSeed(), target);
            BlockPos marker = new BlockPos(target.getMiddleBlockX(), spawn.getY(), target.getMiddleBlockZ());
            AtlasDiscoveryMarkerService.enqueue(
                    player,
                    VillageNameSavedData.keyFor(target),
                    DiscoveryCategory.VILLAGE,
                    name,
                    marker,
                    RegionalMapSymbolPolicy.regionalBanner(region)
            );
        }

        // Repair old physical-start aliases after the canonical four markers are guaranteed to exist.
        StarterVillageIdentityService.repairNow(player);
        player.getPersistentData().putInt(SURVEY_VERSION_TAG, SURVEY_VERSION);

        // Existing v2 players already know about the survey; the v3 repair should be silent rather
        // than pretending they just received the Atlas a second time.
        if (previousVersion < 2) {
            player.displayClientMessage(
                    Component.literal("✦ Hearthlands Survey loaded · ")
                            .append(Component.literal(homeName))
                            .append(Component.literal(" + four nearby settlements are marked in your Atlas")),
                    true
            );
            CozyCrazyZones.LOGGER.info("Prepared starter Atlas survey: home '{}' plus four Hearthlands settlements", homeName);
        } else {
            CozyCrazyZones.LOGGER.info("Upgraded starter Atlas survey to canonical settlement identities");
        }
        return true;
    }
}
