package com.natureul.cozycrazyzones.network;

import com.natureul.cozycrazyzones.client.ClientRegionState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record RegionSyncPacket(int radialOrdinal, int macroOrdinal, int influenceOrdinal) {
    public static void encode(RegionSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.radialOrdinal + 1);
        buffer.writeVarInt(packet.macroOrdinal + 1);
        buffer.writeVarInt(packet.influenceOrdinal + 1);
    }

    public static RegionSyncPacket decode(FriendlyByteBuf buffer) {
        return new RegionSyncPacket(buffer.readVarInt() - 1, buffer.readVarInt() - 1, buffer.readVarInt() - 1);
    }

    public static void handle(RegionSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientRegionState.setOrdinals(packet.radialOrdinal, packet.macroOrdinal, packet.influenceOrdinal)));
        context.setPacketHandled(true);
    }
}
