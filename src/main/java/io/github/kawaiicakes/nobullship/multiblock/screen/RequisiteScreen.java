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
public class RequisiteScreen extends Screen {
    protected final BlockPos blockEntityPos;

    protected RequisiteScreen(BlockPos blockEntityPos) {
        super(Component.empty());
        this.blockEntityPos = blockEntityPos;
    }

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
        drawCenteredString(pPoseStack, Minecraft.getInstance().font, "Test 2 lol", pMouseX, pMouseY, 16777215);
    }

    @Override
    public void removed() {
        super.removed();
        NoBullshipPackets.sendToServer(new ServerboundWorkshopOpenPacket(blockEntityPos));
    }
}
