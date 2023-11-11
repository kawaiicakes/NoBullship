package io.github.kawaiicakes.nobullship.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import static io.github.kawaiicakes.nobullship.NoBullship.MOD_ID;

public class MultiblockWorkshopScreen extends AbstractContainerScreen<MultiblockWorkshopMenu> {
    // TODO: contingencies for screen widths under 379 px
    public static final ResourceLocation TEXTURE = new ResourceLocation(MOD_ID, "textures/gui/workbench_gui.png");

    public MultiblockWorkshopScreen(MultiblockWorkshopMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.imageWidth = 352;
        this.imageHeight = 219;
    }

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(pPoseStack);
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
        this.renderTooltip(pPoseStack, pMouseX, pMouseY);
    }

    @Override
    protected void renderBg(PoseStack pPoseStack, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        blit(pPoseStack, x, y, 0, 0, 0, imageWidth, imageHeight, 512, 512);
        blit(pPoseStack, ((width - 16) / 2) + 1, y+48, 1, 352, 0, 16, 16, 512, 512);
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
    }
}
