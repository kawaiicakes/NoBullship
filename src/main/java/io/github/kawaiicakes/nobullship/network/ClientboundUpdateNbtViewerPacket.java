package io.github.kawaiicakes.nobullship.network;

import io.github.kawaiicakes.nobullship.multiblock.screen.NbtViewerScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientboundUpdateNbtViewerPacket {
    protected final int containerId;

    public ClientboundUpdateNbtViewerPacket(int containerId) {
        this.containerId = containerId;
    }

    public ClientboundUpdateNbtViewerPacket(FriendlyByteBuf buf) {
        this.containerId = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.containerId);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> NbtViewerScreen.setContainerCount(this.containerId));
    }
}
