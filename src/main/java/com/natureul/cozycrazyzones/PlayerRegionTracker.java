package com.natureul.cozycrazyzones;

import com.natureul.cozycrazyzones.network.ZoneNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerRegionTracker {
    private static final String LAST_ANNOUNCED_KEY = "cozycrazyzones:last_announced_region";
    private static final Map<UUID, State> STATES = new HashMap<>();

    private PlayerRegionTracker() {}

    public static void tick(ServerPlayer player) {
        State state = STATES.computeIfAbsent(player.getUUID(), id -> new State());

        if (player.level().dimension() != Level.OVERWORLD) {
            if (state.syncedCell != null) {
                ZoneNetwork.clear(player);
                state.syncedCell = null;
            }
            state.committedRegion = null;
            return;
        }

        ServerLevel level = player.serverLevel();
        double distance = CozyZonesApi.distanceFromSpawn(level, player.getX(), player.getZ());
        if (state.committedRegion == null) state.committedRegion = CozyZonesApi.regionForDistance(distance);

        Region resolved = applyHysteresis(state.committedRegion, distance, CozyZonesConfig.HYSTERESIS.get());
        boolean changed = resolved != state.committedRegion;
        state.committedRegion = resolved;

        RegionalCell raw = CozyZonesApi.regionalCellAt(level, player.getX(), player.getZ());
        RegionalCell synced = new RegionalCell(
                resolved,
                raw.macroRegion(),
                raw.influenceBand(),
                raw.distanceFromSpawn(),
                raw.regionalStrength(),
                raw.macroBoundaryStrength()
        );

        if (!sameDisplayCell(state.syncedCell, synced)) {
            ZoneNetwork.sync(player, synced);
            state.syncedCell = synced;
        }

        CompoundTag persistent = player.getPersistentData();
        String lastAnnounced = persistent.getString(LAST_ANNOUNCED_KEY);
        boolean firstEverAnnouncement = lastAnnounced.isBlank();
        boolean neverAnnouncedHere = !resolved.id().equals(lastAnnounced);
        int cooldown = CozyZonesConfig.ANNOUNCEMENT_COOLDOWN.get();
        boolean cooldownReady = state.lastAnnouncementTick == Integer.MIN_VALUE || player.tickCount - state.lastAnnouncementTick >= cooldown;

        // Full-screen announcements remain radial-only. Cardinal identity lives in the persistent
        // badge/debug API so walking a warped ecological border never causes title spam.
        if ((changed || neverAnnouncedHere) && cooldownReady) {
            announce(player, resolved, state, firstEverAnnouncement);
        }
    }

    private static boolean sameDisplayCell(RegionalCell a, RegionalCell b) {
        return a != null && b != null
                && a.radialZone() == b.radialZone()
                && a.macroRegion() == b.macroRegion()
                && a.influenceBand() == b.influenceBand();
    }

    public static void remove(ServerPlayer player) {
        STATES.remove(player.getUUID());
    }

    public static void copyPersistentMarker(ServerPlayer original, ServerPlayer replacement) {
        String value = original.getPersistentData().getString(LAST_ANNOUNCED_KEY);
        if (!value.isBlank()) replacement.getPersistentData().putString(LAST_ANNOUNCED_KEY, value);
    }

    static Region applyHysteresis(Region current, double distance, int hysteresis) {
        int frontier = CozyZonesConfig.FRONTIER_RADIUS.get();
        int wildlands = Math.max(frontier + 1, CozyZonesConfig.WILDLANDS_RADIUS.get());
        int dread = Math.max(wildlands + 1, CozyZonesConfig.DREAD_RADIUS.get());

        Region region = current;
        boolean moved;
        do {
            moved = false;
            switch (region) {
                case HEARTHLANDS -> { if (distance >= frontier + hysteresis) { region = Region.FRONTIER; moved = true; } }
                case FRONTIER -> {
                    if (distance <= frontier - hysteresis) { region = Region.HEARTHLANDS; moved = true; }
                    else if (distance >= wildlands + hysteresis) { region = Region.WILDLANDS; moved = true; }
                }
                case WILDLANDS -> {
                    if (distance <= wildlands - hysteresis) { region = Region.FRONTIER; moved = true; }
                    else if (distance >= dread + hysteresis) { region = Region.DREAD_REACHES; moved = true; }
                }
                case DREAD_REACHES -> { if (distance <= dread - hysteresis) { region = Region.WILDLANDS; moved = true; } }
            }
        } while (moved && CozyZonesApi.regionForDistance(distance).tier() != region.tier());
        return region;
    }

    private static void announce(ServerPlayer player, Region region, State state, boolean firstEverAnnouncement) {
        // Give the opening Hearthlands reveal time to actually register while the player is taking
        // in the starter house. Ordinary boundary crossings stay brisk and non-intrusive.
        if (firstEverAnnouncement && region == Region.HEARTHLANDS) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(15, 110, 25));
        } else {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 55, 18));
        }
        player.connection.send(new ClientboundSetSubtitleTextPacket(region.subtitleComponent()));
        player.connection.send(new ClientboundSetTitleTextPacket(region.titleComponent()));

        // This is now an actual short motif rather than the old single 0.42-volume UI toast.
        StingerService.queueZone(player, region);
        player.getPersistentData().putString(LAST_ANNOUNCED_KEY, region.id());
        state.lastAnnouncementTick = player.tickCount;
    }

    private static final class State {
        Region committedRegion;
        RegionalCell syncedCell;
        int lastAnnouncementTick = Integer.MIN_VALUE;
    }
}
