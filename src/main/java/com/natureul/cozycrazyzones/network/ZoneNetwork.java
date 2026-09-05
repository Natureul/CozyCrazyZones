package com.natureul.cozycrazyzones.network;

import com.natureul.cozycrazyzones.CozyCrazyZones;
import com.natureul.cozycrazyzones.RegionalCell;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ZoneNetwork {
    private static final String PROTOCOL = "2";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CozyCrazyZones.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private ZoneNetwork() {}

    public static void register() {
        CHANNEL.registerMessage(0, RegionSyncPacket.class, RegionSyncPacket::encode, RegionSyncPacket::decode, RegionSyncPacket::handle);
    }

    public static void sync(ServerPlayer player, RegionalCell cell) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new RegionSyncPacket(
                cell.radialZone().ordinal(), cell.macroRegion().ordinal(), cell.influenceBand().ordinal()));
    }

    public static void clear(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new RegionSyncPacket(-1, -1, -1));
    }
}
