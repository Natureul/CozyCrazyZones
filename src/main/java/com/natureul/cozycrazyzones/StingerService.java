package com.natureul.cozycrazyzones;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Tiny server-side sequencer for zone and discovery stingers. */
public final class StingerService {
    private static final ResourceLocation AMETHYST = id("minecraft:block.amethyst_block.chime");
    private static final ResourceLocation NOTE_CHIME = id("minecraft:block.note_block.chime");
    private static final ResourceLocation NOTE_FLUTE = id("minecraft:block.note_block.flute");
    private static final ResourceLocation NOTE_XYLOPHONE = id("minecraft:block.note_block.xylophone");
    private static final ResourceLocation NOTE_HARP = id("minecraft:block.note_block.harp");
    private static final ResourceLocation NOTE_BASS = id("minecraft:block.note_block.bass");
    private static final ResourceLocation NOTE_BELL = id("minecraft:block.note_block.bell");
    private static final ResourceLocation NOTE_COW_BELL = id("minecraft:block.note_block.cow_bell");
    private static final ResourceLocation WARDEN_HEARTBEAT = id("minecraft:entity.warden.heartbeat");

    private static final Map<UUID, ArrayDeque<ScheduledCue>> QUEUES = new HashMap<>();

    private StingerService() {}

    public static void tick(ServerPlayer player) {
        ArrayDeque<ScheduledCue> queue = QUEUES.get(player.getUUID());
        if (queue == null) return;

        while (!queue.isEmpty() && queue.peekFirst().tick() <= player.tickCount) {
            play(player, queue.removeFirst().cue());
        }
        if (queue.isEmpty()) QUEUES.remove(player.getUUID());
    }

    public static void queueZone(ServerPlayer player, Region region) {
        // A radial-zone crossing is the large navigational event, so it takes precedence over stale cues.
        ArrayDeque<ScheduledCue> queue = new ArrayDeque<>();
        QUEUES.put(player.getUUID(), queue);
        append(queue, zoneMotif(region), player.tickCount);
    }

    public static void queueVillage(ServerPlayer player, MacroRegion region) {
        ArrayDeque<ScheduledCue> queue = QUEUES.computeIfAbsent(player.getUUID(), ignored -> new ArrayDeque<>());
        int start = queue.isEmpty() ? player.tickCount : Math.max(player.tickCount, queue.peekLast().tick() + 4);
        append(queue, villageMotif(region), start);
    }

    public static void queueDiscovery(ServerPlayer player,
                                      DiscoveryCategory category,
                                      MacroRegion region,
                                      boolean major) {
        ArrayDeque<ScheduledCue> queue = QUEUES.computeIfAbsent(player.getUUID(), ignored -> new ArrayDeque<>());
        int start = queue.isEmpty() ? player.tickCount : Math.max(player.tickCount, queue.peekLast().tick() + 4);
        append(queue, discoveryMotif(category, region, major), start);
    }

    public static void clear(ServerPlayer player) {
        QUEUES.remove(player.getUUID());
    }

    private static void append(ArrayDeque<ScheduledCue> queue, Cue[] motif, int startTick) {
        for (Cue cue : motif) queue.addLast(new ScheduledCue(startTick + cue.delayTicks(), cue));
    }

    private static Cue[] zoneMotif(Region region) {
        return switch (region) {
            case HEARTHLANDS -> new Cue[]{
                    cue(AMETHYST, 0, 0.78F, 0.92F),
                    cue(AMETHYST, 4, 0.82F, 1.18F),
                    cue(AMETHYST, 8, 0.88F, 1.48F)
            };
            case FRONTIER -> new Cue[]{
                    cue(AMETHYST, 0, 0.82F, 0.80F),
                    cue(AMETHYST, 4, 0.84F, 1.00F),
                    cue(AMETHYST, 8, 0.88F, 1.24F)
            };
            case WILDLANDS -> new Cue[]{
                    cue(NOTE_HARP, 0, 0.82F, 0.82F),
                    cue(AMETHYST, 4, 0.78F, 0.72F),
                    cue(AMETHYST, 8, 0.84F, 0.94F),
                    cue(NOTE_HARP, 12, 0.72F, 0.78F)
            };
            case DREAD_REACHES -> new Cue[]{
                    cue(WARDEN_HEARTBEAT, 0, 0.90F, 0.78F),
                    cue(AMETHYST, 3, 0.82F, 0.64F),
                    cue(AMETHYST, 7, 0.88F, 0.54F),
                    cue(AMETHYST, 11, 0.92F, 0.46F)
            };
        };
    }

    private static Cue[] villageMotif(MacroRegion region) {
        ResourceLocation instrument = switch (region) {
            case NORTH -> NOTE_CHIME;
            case EAST -> NOTE_FLUTE;
            case SOUTH -> NOTE_XYLOPHONE;
            case WEST -> NOTE_HARP;
        };
        return switch (region) {
            case NORTH -> new Cue[]{
                    cue(instrument, 0, 0.82F, 1.46F),
                    cue(instrument, 4, 0.86F, 1.72F),
                    cue(AMETHYST, 8, 0.78F, 1.92F)
            };
            case EAST -> new Cue[]{
                    cue(instrument, 0, 0.82F, 1.04F),
                    cue(instrument, 4, 0.84F, 1.30F),
                    cue(instrument, 8, 0.86F, 1.56F),
                    cue(AMETHYST, 12, 0.72F, 1.30F)
            };
            case SOUTH -> new Cue[]{
                    cue(instrument, 0, 0.86F, 0.86F),
                    cue(instrument, 4, 0.88F, 1.08F),
                    cue(AMETHYST, 8, 0.82F, 1.38F)
            };
            case WEST -> new Cue[]{
                    cue(instrument, 0, 0.84F, 1.00F),
                    cue(instrument, 4, 0.82F, 1.24F),
                    cue(instrument, 8, 0.78F, 1.00F),
                    cue(AMETHYST, 12, 0.84F, 1.48F)
            };
        };
    }

    private static Cue[] discoveryMotif(DiscoveryCategory category, MacroRegion region, boolean major) {
        float shift = switch (region) {
            case NORTH -> 1.08F;
            case EAST -> 1.02F;
            case SOUTH -> 0.96F;
            case WEST -> 0.92F;
        };

        Cue[] base = switch (category) {
            case DUNGEON -> new Cue[]{
                    cue(NOTE_BASS, 0, 0.72F, 0.82F * shift),
                    cue(NOTE_HARP, 5, 0.78F, 0.72F * shift),
                    cue(AMETHYST, 10, 0.72F, 0.88F * shift)
            };
            case TEMPLE, SHRINE -> new Cue[]{
                    cue(NOTE_BELL, 0, 0.76F, 0.94F * shift),
                    cue(AMETHYST, 5, 0.80F, 1.16F * shift),
                    cue(NOTE_CHIME, 10, 0.76F, 1.30F * shift)
            };
            case RUIN -> new Cue[]{
                    cue(NOTE_HARP, 0, 0.70F, 0.76F * shift),
                    cue(AMETHYST, 6, 0.68F, 0.88F * shift)
            };
            case TOWER, FORTRESS -> new Cue[]{
                    cue(NOTE_COW_BELL, 0, 0.74F, 0.84F * shift),
                    cue(NOTE_CHIME, 5, 0.78F, 1.02F * shift),
                    cue(NOTE_CHIME, 10, 0.80F, 1.22F * shift)
            };
            case CAMP, HOUSE -> new Cue[]{
                    cue(NOTE_HARP, 0, 0.62F, 1.02F * shift),
                    cue(NOTE_HARP, 5, 0.64F, 1.24F * shift)
            };
            case MINE -> new Cue[]{
                    cue(NOTE_BASS, 0, 0.68F, 0.86F * shift),
                    cue(NOTE_COW_BELL, 6, 0.66F, 0.92F * shift)
            };
            case SHIP -> new Cue[]{
                    cue(NOTE_XYLOPHONE, 0, 0.68F, 0.92F * shift),
                    cue(NOTE_HARP, 5, 0.70F, 1.08F * shift),
                    cue(AMETHYST, 10, 0.68F, 1.24F * shift)
            };
            case PORTAL -> new Cue[]{
                    cue(AMETHYST, 0, 0.74F, 0.70F * shift),
                    cue(AMETHYST, 4, 0.78F, 0.94F * shift),
                    cue(AMETHYST, 8, 0.80F, 1.18F * shift)
            };
            case BOSS -> new Cue[]{
                    cue(WARDEN_HEARTBEAT, 0, 0.84F, 0.82F),
                    cue(NOTE_BASS, 3, 0.82F, 0.66F * shift),
                    cue(AMETHYST, 8, 0.86F, 0.58F * shift),
                    cue(NOTE_BELL, 13, 0.82F, 0.72F * shift)
            };
            case LANDMARK -> new Cue[]{
                    cue(NOTE_CHIME, 0, 0.76F, 0.92F * shift),
                    cue(AMETHYST, 5, 0.82F, 1.14F * shift),
                    cue(AMETHYST, 10, 0.84F, 1.42F * shift)
            };
            case VILLAGE -> villageMotif(region);
        };

        if (!major || category == DiscoveryCategory.BOSS || category == DiscoveryCategory.VILLAGE) return base;
        Cue[] extended = new Cue[base.length + 1];
        System.arraycopy(base, 0, extended, 0, base.length);
        int last = base[base.length - 1].delayTicks();
        extended[base.length] = cue(AMETHYST, last + 5, 0.72F, 1.48F * shift);
        return extended;
    }

    private static Cue cue(ResourceLocation sound, int delay, float volume, float pitch) {
        return new Cue(sound, delay, volume, pitch);
    }

    private static void play(ServerPlayer player, Cue cue) {
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(cue.sound());
        if (sound == null) sound = ForgeRegistries.SOUND_EVENTS.getValue(AMETHYST);
        if (sound != null) player.playNotifySound(sound, SoundSource.MASTER, cue.volume(), cue.pitch());
    }

    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }

    private record Cue(ResourceLocation sound, int delayTicks, float volume, float pitch) {}
    private record ScheduledCue(int tick, Cue cue) {}
}
