package io.github.kawaiicakes.nobullship.multiblock.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static io.github.kawaiicakes.nobullship.NoBullship.MOD_ID;

@OnlyIn(Dist.CLIENT)
public class ProxyScreen extends AbstractContainerScreen<ProxyMenu> {
    public static final ResourceLocation TEXTURE = new ResourceLocation(MOD_ID, "textures/gui/proxy_screen.png");

    public ProxyScreen(ProxyMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);

        this.imageWidth = 202;
        this.imageHeight = 192;

        this.inventoryLabelY += 28;
    }

    @Override
    protected void renderBg(PoseStack pPoseStack, float pPartialTick, int pMouseX, int pMouseY) {
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
        this.renderBackground(pPoseStack);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();

        blit(pPoseStack, x, y, 0, 0, 0, this.imageWidth, this.imageHeight, 305, 245);
        blit(pPoseStack, x + 138, y + 51, 1, 202, 0, 16, 16, 305, 245);

        pPoseStack.pushPose();
        pPoseStack.translate(x - 48.6, (this.height - 191.7) / 2, -1);
        pPoseStack.scale(0.9F, 0.9F, 0);
        blit(pPoseStack, 0, 0, 0, 202, 32, 54, 213, 305, 245);
        pPoseStack.popPose();

        blit(pPoseStack, x + 202, (this.height - 106) / 2, -1, 256, 32, 49, 106, 305, 245);

        drawCenteredString(pPoseStack, Minecraft.getInstance().font, Component.translatable("gui.nobullship.proxy_screen"), x, y, 0x555555);

        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
    }
}
