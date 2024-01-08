package io.github.kawaiicakes.nobullship.network;

import io.github.kawaiicakes.nobullship.multiblock.block.MultiblockWorkshopBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Supplier;

public class ServerboundWorkshopOpenPacket {
    protected final BlockPos blockEntityPos;

    public ServerboundWorkshopOpenPacket(BlockPos blockEntityPos) {
        this.blockEntityPos = blockEntityPos;
    }

    public ServerboundWorkshopOpenPacket(FriendlyByteBuf buf) {
        this.blockEntityPos = buf.readBlockPos();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.blockEntityPos);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            ServerLevel level = player.getLevel();

            BlockEntity blockEntity = level.getBlockEntity(this.blockEntityPos);
            if (!(blockEntity instanceof MultiblockWorkshopBlockEntity workshop)) return;

            NetworkHooks.openScreen(player, workshop, this.blockEntityPos);
        });
    }
}
