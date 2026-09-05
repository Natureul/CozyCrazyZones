package com.natureul.cozycrazyzones.network;

import com.natureul.cozycrazyzones.client.ClientRegionState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record RegionSyncPacket(int regionOrdinal) {
    public static void encode(RegionSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.regionOrdinal + 1);
    }

    public static RegionSyncPacket decode(FriendlyByteBuf buffer) {
        return new RegionSyncPacket(buffer.readVarInt() - 1);
    }

    public static void handle(RegionSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientRegionState.setOrdinal(packet.regionOrdinal)));
        context.setPacketHandled(true);
    }
}
