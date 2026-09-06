package com.natureul.cozycrazyzones;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Ensures the four names printed on the starter desk map are reserved in the persistent ledger. */
public final class VillageNameBootstrapService {
    private static final int RETRY_INTERVAL_TICKS = 20;
    private static final int MAX_ATTEMPTS = 45;
    private static final ConcurrentMap<UUID, Integer> PENDING = new ConcurrentHashMap<>();

    private VillageNameBootstrapService() {}

    public static void begin(ServerPlayer player) {
        if (player.serverLevel().dimension() != Level.OVERWORLD) return;
        PENDING.put(player.getUUID(), 0);
        if (tryReserve(player)) PENDING.remove(player.getUUID());
    }

    public static void tick(ServerPlayer player) {
        Integer attempts = PENDING.get(player.getUUID());
        if (attempts == null || player.tickCount % RETRY_INTERVAL_TICKS != 0) return;
        if (tryReserve(player)) {
            PENDING.remove(player.getUUID());
            return;
        }
        int next = attempts + 1;
        if (next >= MAX_ATTEMPTS) PENDING.remove(player.getUUID());
        else PENDING.put(player.getUUID(), next);
    }

    public static void remove(ServerPlayer player) {
        PENDING.remove(player.getUUID());
    }

    public static void clear() {
        PENDING.clear();
    }

    private static boolean tryReserve(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Map<MacroRegion, ChunkPos> targets = VillageRingPlanner.targetsFor(
                level,
                level.getChunkSource().getGenerator(),
                level.getChunkSource().getGeneratorState(),
                level.registryAccess()
        );
        if (targets.size() != MacroRegion.values().length) return false;

        VillageNameSavedData names = VillageNameSavedData.get(level);
        for (MacroRegion region : MacroRegion.values()) {
            ChunkPos target = targets.get(region);
            if (target != null) names.getOrAssign(region, level.getSeed(), target);
        }
        return true;
    }
}
