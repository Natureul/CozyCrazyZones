package com.natureul.cozycrazyzones;

import com.mojang.brigadier.Command;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
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
        if (level.dimension() != Level.OVERWORLD) return;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType());
        if (id == null) return;
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
            if (!structures.containsKey(id)) CozyCrazyZones.LOGGER.warn("Configured structure ID is absent from this runtime: {}", id);
        }
        for (ResourceLocation id : ZoneRuleRegistry.naturalEntities().keySet()) {
            if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id)) CozyCrazyZones.LOGGER.warn("Configured entity ID is absent from this runtime: {}", id);
        }
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
                        ctx.getSource().sendSuccess(() -> Component.literal("CozyCrazyZones is radial only in the Overworld."), false);
                        return Command.SINGLE_SUCCESS;
                    }
                    ServerLevel level = player.serverLevel();
                    double distance = CozyZonesApi.distanceFromSpawn(level, player.getX(), player.getZ());
                    Region region = CozyZonesApi.regionAt(level, player.getX(), player.getZ());
                    ctx.getSource().sendSuccess(() -> Component.literal(String.format("%s — %.1f blocks from world spawn", region.displayName(), distance)).withStyle(region.formatting()), false);
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
