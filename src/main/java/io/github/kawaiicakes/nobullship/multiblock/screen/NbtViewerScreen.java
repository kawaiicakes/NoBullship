package io.github.kawaiicakes.nobullship.multiblock.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kawaiicakes.nobullship.network.NoBullshipPackets;
import io.github.kawaiicakes.nobullship.network.ServerboundWorkshopOpenPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class NbtViewerScreen extends Screen {
    protected final BlockPos blockEntityPos;

    protected NbtViewerScreen(BlockPos blockEntityPos) {
        super(Component.empty());
        this.blockEntityPos = blockEntityPos;
    }

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
        drawCenteredString(pPoseStack, Minecraft.getInstance().font, "Test lol", pMouseX, pMouseY, 16777215);
    }

    @Override
    public void removed() {
        super.removed();
        // a packet is used here because I can't be bothered to figure out how to force it open from the client
        // from the bits I did see though, I'm not sure if doing it from the client would be safe anyway...
        // oh btw future ashley, setting screen to null does indeed exit the current screen :)
        NoBullshipPackets.sendToServer(new ServerboundWorkshopOpenPacket(blockEntityPos));
    }
}
