package io.github.kawaiicakes.nobullship.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerboundRequestContainerIdPacket {

    public ServerboundRequestContainerIdPacket() {}

    public ServerboundRequestContainerIdPacket(FriendlyByteBuf buf) {}

    public void toBytes(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            player.doCloseContainer();
            player.nextContainerCounter();
            int openContainerId = player.containerCounter;

            NoBullshipPackets.sendToPlayer(new ClientboundUpdateNbtViewerPacket(openContainerId), player);
        });
    }
}
