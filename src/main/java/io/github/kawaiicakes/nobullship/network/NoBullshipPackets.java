package io.github.kawaiicakes.nobullship.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.jetbrains.annotations.Nullable;

import static io.github.kawaiicakes.nobullship.NoBullship.MOD_ID;

public class NoBullshipPackets {
    private static SimpleChannel INSTANCE;
    private static int packetId = 0;
    private static int id() {
        return packetId++;
    }

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(MOD_ID, "messages"))
                .networkProtocolVersion(() -> "1.0") // checks for packet compatibility
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

        net.messageBuilder(ClientboundUpdateNoBullshipPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(ClientboundUpdateNoBullshipPacket::new)
                .encoder(ClientboundUpdateNoBullshipPacket::toBytes)
                .consumerMainThread(ClientboundUpdateNoBullshipPacket::handle)
                .add();
    }

    public static <MSG> void sendToPlayer(MSG msg, @Nullable ServerPlayer player) {
        if (player == null) {
            INSTANCE.send(PacketDistributor.ALL.noArg(), msg);
            return;
        }
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }
}
