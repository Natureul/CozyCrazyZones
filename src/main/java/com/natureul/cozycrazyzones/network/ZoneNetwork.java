package com.natureul.cozycrazyzones.network;

import com.natureul.cozycrazyzones.CozyCrazyZones;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ZoneNetwork {
    private static final String PROTOCOL = "1";
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

    public static void sync(ServerPlayer player, int regionOrdinal) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new RegionSyncPacket(regionOrdinal));
    }
}
