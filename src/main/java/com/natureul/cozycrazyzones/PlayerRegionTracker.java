package com.natureul.cozycrazyzones;

import com.natureul.cozycrazyzones.network.ZoneNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
            if (state.syncedRegion != null) {
                ZoneNetwork.sync(player, -1);
                state.syncedRegion = null;
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

        if (state.syncedRegion != resolved) {
            ZoneNetwork.sync(player, resolved.ordinal());
            state.syncedRegion = resolved;
        }

        CompoundTag persistent = player.getPersistentData();
        String lastAnnounced = persistent.getString(LAST_ANNOUNCED_KEY);
        boolean neverAnnouncedHere = !resolved.id().equals(lastAnnounced);
        int cooldown = CozyZonesConfig.ANNOUNCEMENT_COOLDOWN.get();
        boolean cooldownReady = state.lastAnnouncementTick == Integer.MIN_VALUE || player.tickCount - state.lastAnnouncementTick >= cooldown;

        if ((changed || neverAnnouncedHere) && cooldownReady) announce(player, resolved, state);
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

    private static void announce(ServerPlayer player, Region region, State state) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 55, 18));
        player.connection.send(new ClientboundSetSubtitleTextPacket(region.subtitleComponent()));
        player.connection.send(new ClientboundSetTitleTextPacket(region.titleComponent()));
        float pitch = switch (region) {
            case HEARTHLANDS -> 1.18F;
            case FRONTIER -> 1.04F;
            case WILDLANDS -> 0.90F;
            case DREAD_REACHES -> 0.76F;
        };
        player.playNotifySound(SoundEvents.UI_TOAST_IN, SoundSource.MASTER, 0.42F, pitch);
        player.getPersistentData().putString(LAST_ANNOUNCED_KEY, region.id());
        state.lastAnnouncementTick = player.tickCount;
    }

    private static final class State {
        Region committedRegion;
        Region syncedRegion;
        int lastAnnouncementTick = Integer.MIN_VALUE;
    }
}
