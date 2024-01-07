package io.github.kawaiicakes.nobullship.multiblock.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;

import static io.github.kawaiicakes.nobullship.multiblock.screen.MultiblockWorkshopScreen.TEXTURE;

public class WorkshopButton extends ImageButton {
    protected boolean alternateTexture;

    public WorkshopButton(int pWidth, int pHeight, int pXTexStart, int pYTexStart, int pYDiffTex, OnPress pOnPress, OnTooltip onTooltip) {
        super(
                0, 0,
                pWidth, pHeight,
                pXTexStart, pYTexStart, pYDiffTex,
                TEXTURE, 256, 256,
                pOnPress,
                onTooltip,
                Component.empty()
        );
    }

    @Override
    public void renderButton(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, this.resourceLocation);

        int i = this.yTexStart;
        if (!this.alternateTexture) i += this.yDiffTex;

        RenderSystem.enableDepthTest();
        blit(pPoseStack, this.x, this.y, 5, this.xTexStart, i, this.width, this.height, this.textureHeight, this.textureWidth);
        if (this.isHovered) {
            this.renderToolTip(pPoseStack, pMouseX, pMouseY);
        }
    }

    public void setVisibility(boolean isActive) {
        this.active = isActive;
        this.visible = isActive;
    }
}
