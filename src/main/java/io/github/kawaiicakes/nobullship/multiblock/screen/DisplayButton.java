package io.github.kawaiicakes.nobullship.multiblock.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import static io.github.kawaiicakes.nobullship.multiblock.screen.MultiblockWorkshopScreen.TEXTURE;

public class DisplayButton extends ImageButton {
    protected final boolean renderDefault;
    protected boolean altTexture;

    public DisplayButton(int pWidth, int pHeight, int pXTexStart, int pYTexStart, int pYDiffTex, ResourceLocation texture, int texWidth, int texHeight, OnPress pOnPress, OnTooltip onTooltip) {
        super(
                0, 0,
                pWidth, pHeight,
                pXTexStart, pYTexStart, pYDiffTex,
                texture, texWidth, texHeight,
                pOnPress,
                onTooltip,
                Component.empty()
        );

        this.active = false;
        this.renderDefault = true;
        this.altTexture = false;
    }

    public DisplayButton(int pWidth, int pHeight, int pXTexStart, int pYTexStart, int pYDiffTex, OnPress pOnPress, OnTooltip onTooltip, boolean renderDefault) {
        super(
                0, 0,
                pWidth, pHeight,
                pXTexStart, pYTexStart, pYDiffTex,
                TEXTURE, 305, 245,
                pOnPress,
                onTooltip,
                Component.empty()
        );

        this.visible = true;
        this.active = false;
        this.renderDefault = renderDefault;
        this.altTexture = false;
    }

    @Override
    public void renderButton(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        if (this.renderDefault) {
            this.renderDefault(pPoseStack, pMouseX, pMouseY, pPartialTick);
        } else {
            this.renderModified(pPoseStack, pMouseX, pMouseY, pPartialTick);
        }

        if (this.isHovered) {
            this.renderToolTip(pPoseStack, pMouseX, pMouseY);
        }
    }

    public void setActive(boolean isActive) {
        this.active = isActive;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    protected void renderDefault(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, this.resourceLocation);
        int i = this.yTexStart;
        if (!this.isActive()) {
            i += this.yDiffTex * 2;
        } else if (this.isHoveredOrFocused()) {
            i += this.yDiffTex;
        }

        RenderSystem.enableDepthTest();
        //noinspection SuspiciousNameCombination
        blit(pPoseStack, this.x, this.y, 3, (float)this.xTexStart, (float)i, this.width, this.height, this.textureWidth, this.textureHeight);
    }

    protected void renderModified(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, this.resourceLocation);
        int i = this.yTexStart;
        if (!this.isActive()) {
            i += this.yDiffTex * 2;
        } else if (this.altTexture) {
            i += this.yDiffTex;
        }

        RenderSystem.enableDepthTest();
        //noinspection SuspiciousNameCombination
        blit(pPoseStack, this.x, this.y, 3, (float)this.xTexStart, (float)i, this.width, this.height, this.textureWidth, this.textureHeight);
    }
}
