package io.github.kawaiicakes.nobullship.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import static io.github.kawaiicakes.nobullship.NoBullship.MOD_ID;

public class MultiblockWorkshopScreen extends AbstractContainerScreen<MultiblockWorkshopMenu> {
    public static final ResourceLocation TEXTURE = new ResourceLocation(MOD_ID, "textures/gui/workbench_gui.png");
    public static final Quaternion ROTATE_180 = Vector3f.ZP.rotationDegrees(-180F);

    public MultiblockWorkshopScreen(MultiblockWorkshopMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.imageWidth = 202;
        this.imageHeight = 206;

        this.titleLabelX += 12;
        this.inventoryLabelX += 12;
        // this.titleLabelY -= 4;
        this.inventoryLabelY += 40;
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
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        blit(pPoseStack, x, y, 0, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
        blit(pPoseStack, (this.width - 16) / 2, y + 35, 1, 202, 0, 16, 16, 256, 256);

        blit(pPoseStack, x - 52, y - 4, -2, 202, 16, 54, 106, 256, 256);

        blit(pPoseStack, x + 202, y + 110, 0, 206, 49, 50);
        blit(pPoseStack, x + 202, y + 60, 49, 206, 49, 50);
        blit(pPoseStack, x + 234, y + 54, 98, 206, 49, 6);

        pPoseStack.pushPose();
        pPoseStack.translate(x, y + 209, -3);
        pPoseStack.mulPose(ROTATE_180);
        blit(pPoseStack, 0, 0, 202, 122, 54, 106);
        pPoseStack.popPose();

        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
    }
}
