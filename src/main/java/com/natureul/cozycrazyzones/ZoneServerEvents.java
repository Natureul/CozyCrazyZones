package com.natureul.cozycrazyzones;

import com.mojang.brigadier.Command;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ZoneServerEvents {
    private ZoneServerEvents() {}

    @SubscribeEvent
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (event.getSpawnType() != MobSpawnType.NATURAL) return;
        ServerLevel level = event.getLevel().getLevel();
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType());
        if (id == null) return;

        // Namespace-wide suppressions are global. This is how unrelated Cataclysm natural mobs
        // stay out of the Overworld, Nether and End without touching authored structure/summon spawns.
        if (ZoneRuleRegistry.naturalEntityNamespaceSuppressed(id)) {
            event.setSpawnCancelled(true);
            return;
        }

        if (level.dimension() != Level.OVERWORLD) return;
        if (!CozyZonesApi.naturalEntityAllowed(level, id, event.getX(), event.getZ())) event.setSpawnCancelled(true);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % 10 == 0) PlayerRegionTracker.tick(player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) PlayerRegionTracker.remove(player);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer original && event.getEntity() instanceof ServerPlayer replacement) {
            PlayerRegionTracker.copyPersistentMarker(original, replacement);
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        var access = event.getServer().registryAccess();
        var structures = access.registryOrThrow(Registries.STRUCTURE);
        for (ResourceLocation id : ZoneRuleRegistry.structures().keySet()) {
            if (!namespaceLoaded(id)) continue;
            if (!structures.containsKey(id)) CozyCrazyZones.LOGGER.warn("Configured structure ID is absent from this runtime: {}", id);
        }
        for (ResourceLocation id : ZoneRuleRegistry.naturalEntities().keySet()) {
            if (!namespaceLoaded(id)) continue;
            if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id)) CozyCrazyZones.LOGGER.warn("Configured entity ID is absent from this runtime: {}", id);
        }
    }

    private static boolean namespaceLoaded(ResourceLocation id) {
        return "minecraft".equals(id.getNamespace()) || ModList.get().isLoaded(id.getNamespace());
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("cozyzones")
                .then(Commands.literal("where").executes(ctx -> {
                    if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                        ctx.getSource().sendFailure(Component.literal("This command requires a player."));
                        return 0;
                    }
                    if (player.level().dimension() != Level.OVERWORLD) {
                        ctx.getSource().sendSuccess(() -> Component.literal("CozyCrazyZones geography is Overworld-only."), false);
                        return Command.SINGLE_SUCCESS;
                    }
                    ServerLevel level = player.serverLevel();
                    RegionalCell cell = CozyZonesApi.regionalCellAt(level, player.getX(), player.getZ());
                    String ecology = switch (cell.influenceBand()) {
                        case SHARED_CORE -> "Shared Core";
                        case CARDINAL_TRANSITION -> cell.macroRegion().displayName() + " transition";
                        case ESTABLISHED -> cell.macroRegion().displayName();
                    };
                    int strength = (int) Math.round(cell.regionalStrength() * 100.0D);
                    int border = (int) Math.round(cell.macroBoundaryStrength() * 100.0D);
                    String message = String.format(
                            "Radial: %s | Ecology: %s | %.1f blocks from world spawn | cardinal strength %d%% | regional-core strength %d%%",
                            cell.radialZone().displayName(), ecology, cell.distanceFromSpawn(), strength, border
                    );
                    ctx.getSource().sendSuccess(() -> Component.literal(message).withStyle(cell.radialZone().formatting()), false);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("dump_registry").requires(src -> src.hasPermission(2)).executes(ctx -> dumpRegistry(ctx.getSource()))));
    }

    private static int dumpRegistry(net.minecraft.commands.CommandSourceStack source) {
        try {
            List<String> lines = new ArrayList<>();
            lines.add("# CozyCrazyZones registry dump");
            lines.add("# Structures");
            source.registryAccess().registryOrThrow(Registries.STRUCTURE).keySet().stream().map(ResourceLocation::toString).sorted().forEach(lines::add);
            lines.add("");
            lines.add("# Structure sets");
            source.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).keySet().stream().map(ResourceLocation::toString).sorted().forEach(lines::add);
            lines.add("");
            lines.add("# Entity types");
            BuiltInRegistries.ENTITY_TYPE.keySet().stream().map(ResourceLocation::toString).sorted(Comparator.naturalOrder()).forEach(lines::add);

            var biomeRegistry = source.registryAccess().registryOrThrow(Registries.BIOME);
            List<ResourceLocation> biomeIds = biomeRegistry.keySet().stream().sorted(Comparator.comparing(ResourceLocation::toString)).toList();
            lines.add("");
            lines.add("# Biomes");
            biomeIds.stream().map(ResourceLocation::toString).forEach(lines::add);

            lines.add("");
            lines.add("# Biome details: tags and final natural spawn pools after biome modifiers");
            for (ResourceLocation biomeId : biomeIds) {
                ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, biomeId);
                var holderOptional = biomeRegistry.getHolder(key);
                if (holderOptional.isEmpty()) continue;
                var holder = holderOptional.get();
                List<String> tags = holder.tags().map(tag -> "#" + tag.location()).sorted().toList();
                lines.add("BIOME " + biomeId + (tags.isEmpty() ? "" : " tags=" + String.join(",", tags)));

                for (MobCategory category : MobCategory.values()) {
                    for (var spawn : holder.value().getMobSettings().getMobs(category).unwrap()) {
                        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(spawn.type);
                        if (entityId == null) continue;
                        lines.add("  SPAWN " + category.getName() + " " + entityId + " min=" + spawn.minCount + " max=" + spawn.maxCount);
                    }
                }
            }

            Path path = FMLPaths.GAMEDIR.get().resolve("logs").resolve("cozycrazyzones-registry-dump.txt");
            Files.createDirectories(path.getParent());
            Files.write(path, lines, StandardCharsets.UTF_8);
            source.sendSuccess(() -> Component.literal("Wrote " + path.toAbsolutePath()), false);
            return Command.SINGLE_SUCCESS;
        } catch (IOException ex) {
            CozyCrazyZones.LOGGER.error("Could not write CozyCrazyZones registry dump", ex);
            source.sendFailure(Component.literal("Could not write registry dump; see latest.log."));
            return 0;
        }
    }
}
