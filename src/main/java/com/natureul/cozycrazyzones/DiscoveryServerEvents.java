package com.natureul.cozycrazyzones;

import com.mojang.brigadier.Command;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Lightweight runtime hooks for discovered places and audible stingers. */
@Mod.EventBusSubscriber(modid = CozyCrazyZones.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DiscoveryServerEvents {
    private DiscoveryServerEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;

        // The sequencer is O(1) and normally has no queue at all.
        StingerService.tick(player);

        // Structure membership and pending Atlas work are sampled only once per second. Both operate
        // exclusively on loaded/current-player data; neither performs a radius locate or chunk scan.
        if (player.tickCount % 20 == 0) {
            StructureDiscoveryService.tick(player);
            AtlasDiscoveryMarkerService.tick(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) StingerService.clear(player);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer original && event.getEntity() instanceof ServerPlayer replacement) {
            StructureDiscoveryService.copyPersistentState(original, replacement);
            AtlasDiscoveryMarkerService.copyPersistentState(original, replacement);
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        // Dedicated test command avoids forcing the player to travel thousands of blocks simply to
        // tune/check audio. It is deliberately available without operator permissions.
        event.getDispatcher().register(Commands.literal("cozystinger")
                .executes(ctx -> replayZone(ctx.getSource()))
                .then(Commands.literal("zone").executes(ctx -> replayZone(ctx.getSource())))
                .then(Commands.literal("village").executes(ctx -> replayVillage(ctx.getSource()))));
    }

    private static int replayZone(net.minecraft.commands.CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player) || player.level().dimension() != Level.OVERWORLD) {
            source.sendFailure(Component.literal("This command requires an Overworld player."));
            return 0;
        }
        Region region = CozyZonesApi.regionAt(player.serverLevel(), player.getX(), player.getZ());
        StingerService.queueZone(player, region);
        source.sendSuccess(() -> Component.literal("Replaying " + region.displayName() + " stinger."), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int replayVillage(net.minecraft.commands.CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player) || player.level().dimension() != Level.OVERWORLD) {
            source.sendFailure(Component.literal("This command requires an Overworld player."));
            return 0;
        }
        MacroRegion region = CozyZonesApi.regionalCellAt(player.serverLevel(), player.getX(), player.getZ()).macroRegion();
        StingerService.queueVillage(player, region);
        source.sendSuccess(() -> Component.literal("Replaying " + region.displayName() + " village stinger."), false);
        return Command.SINGLE_SUCCESS;
    }
}
