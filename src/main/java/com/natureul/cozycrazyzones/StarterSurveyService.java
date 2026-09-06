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
    private static final int SURVEY_VERSION = 2;
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

    private static boolean tryInstall(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Map<MacroRegion, ChunkPos> targets = VillageRingPlanner.targetsFor(
                level,
                level.getChunkSource().getGenerator(),
                level.getChunkSource().getGeneratorState(),
                level.registryAccess()
        );
        if (targets.size() != MacroRegion.values().length) return false;

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

        player.getPersistentData().putInt(SURVEY_VERSION_TAG, SURVEY_VERSION);
        player.displayClientMessage(
                Component.literal("✦ Hearthlands Survey loaded · ")
                        .append(Component.literal(homeName))
                        .append(Component.literal(" + four nearby settlements are marked in your Atlas")),
                true
        );
        CozyCrazyZones.LOGGER.info("Prepared starter Atlas survey: home '{}' plus four Hearthlands settlements", homeName);
        return true;
    }
}
