package io.github.kawaiicakes.nobullship.multiblock.screen;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import io.github.kawaiicakes.nobullship.api.MultiblockRecipeManager;
import io.github.kawaiicakes.nobullship.api.multiblock.MultiblockRecipe;
import io.github.kawaiicakes.nobullship.schematic.SchematicRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Optional;

import static io.github.kawaiicakes.nobullship.NoBullship.MOD_ID;

public class MultiblockWorkshopScreen extends AbstractContainerScreen<MultiblockWorkshopMenu> {
    public static final ResourceLocation TEXTURE = new ResourceLocation(MOD_ID, "textures/gui/workbench_gui.png");
    public static final Component NO_RESULT = Component.translatable("gui.nobullship.no_recipe");
    public static final Component VISIBILITY_BUTTON = Component.translatable("gui.nobullship.toggle_render");
    public static final Component SLICE_DIRECTION = Component.translatable("gui.nobullship.slice_direction");
    public static final Component INCREMENT = Component.translatable("gui.nobullship.increment");
    public static final Component DECREMENT = Component.translatable("gui.nobullship.decrement");
    public static final Quaternion ROTATE_180 = Vector3f.ZP.rotationDegrees(-180F);

    protected boolean renderSchematic;
    public boolean verticalRenderSlicing;
    public int renderedLayer;
    protected NbtViewerButton nbtViewerButton;
    protected WorkshopButton visibilityButton;
    protected WorkshopButton verticalButton;
    protected WorkshopButton incrementButton;
    protected WorkshopButton decrementButton;
    protected boolean hasShapedMatch;

    public MultiblockWorkshopScreen(MultiblockWorkshopMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.imageWidth = 202;
        this.imageHeight = 206;

        this.titleLabelX += 12;
        this.inventoryLabelX += 12;
        this.inventoryLabelY += 40;
        this.renderSchematic = pMenu.entity.shouldRenderSchematicInWorld;
        this.verticalRenderSlicing = pMenu.entity.verticalRenderSlicing;
        this.renderedLayer = pMenu.entity.renderedLayer;
        this.nbtViewerButton = new NbtViewerButton(pMenu.entity.getBlockPos());
        this.visibilityButton = new WorkshopButton(
                16, 16,
                115, 206, 16,
                (button) -> this.toggleSchematicDisplay(),
                (button, stack, mX, mY) -> this.renderTooltip(stack, VISIBILITY_BUTTON, mX, mY));
        this.verticalButton = new WorkshopButton(
                16, 16,
                131, 206, 16,
                (button) -> this.toggleSlice(),
                (button, stack, mX, mY) -> this.renderTooltip(stack, SLICE_DIRECTION, mX, mY));
        this.incrementButton = new WorkshopButton(
                8, 8,
                147, 206, 0,
                (button) -> this.incrementLayer(),
                (button, stack, mX, mY) -> this.renderTooltip(stack, INCREMENT, mX, mY));
        this.decrementButton = new WorkshopButton(
                8, 8,
                147, 214, 0,
                (button) -> this.decrementLayer(),
                (button, stack, mX, mY) -> this.renderTooltip(stack, DECREMENT, mX, mY));
        this.visibilityButton.alternateTexture = !this.renderSchematic;
        this.verticalButton.alternateTexture = this.verticalRenderSlicing;
        this.hasShapedMatch = false;
    }

    @Override
    protected void init() {
        super.init();

        this.visibilityButton.setPosition(this.leftPos + 129, this.topPos + 71);
        this.verticalButton.setPosition(this.leftPos + 147, this.topPos + 71);
        this.incrementButton.setPosition(this.leftPos + 165, this.topPos + 72);
        this.decrementButton.setPosition(this.leftPos + 165, this.topPos + 80);

        this.addRenderableWidget(this.nbtViewerButton);
        this.addRenderableWidget(this.visibilityButton);
        this.addRenderableWidget(this.verticalButton);
        this.addRenderableWidget(this.incrementButton);
        this.addRenderableWidget(this.decrementButton);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        // TODO: change condition to check for if the multiblock recipe uses NBT
        this.nbtViewerButton.setVisibility(this.hasShapedMatch);
    }

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(pPoseStack);
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
        this.renderTooltip(pPoseStack, pMouseX, pMouseY);
        drawString(pPoseStack, Minecraft.getInstance().font, String.valueOf(this.menu.entity.actualRenderedLayer), this.leftPos + 174, this.topPos + 75, 0x555555);

        if (this.menu.entity.queueLayerReset) this.resetLayer();
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
        blit(pPoseStack, x + 234, y + 54, 98, 206, 17, 6);

        pPoseStack.pushPose();
        pPoseStack.translate(x, y + 209, -3);
        pPoseStack.mulPose(ROTATE_180);
        blit(pPoseStack, 0, 0, 202, 122, 54, 106);
        pPoseStack.popPose();

        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();

        if (!(this.menu.player.level instanceof ClientLevel clientLevel)) return;
        if (this.menu.entity.isEmpty()) {
            drawNoResultString(pPoseStack, x + 155, y + 5);
            return;
        }
        List<SchematicRecipe> recipeList
                = clientLevel.getRecipeManager().getAllRecipesFor(SchematicRecipe.Type.INSTANCE);
        if (recipeList.isEmpty()) {
            drawNoResultString(pPoseStack, x + 155, y + 5);
            return;
        }

        List<SchematicRecipe> shapedMatches = recipeList.stream()
                .filter(recipe -> recipe.shapedMatches(this.menu.entity))
                .toList();

        Optional<SchematicRecipe> matchingRecipe = shapedMatches.stream().findFirst();
        if (shapedMatches.size() > 1)
            matchingRecipe = shapedMatches.stream().filter(recipe -> recipe.shapelessMatches(this.menu.entity)).findFirst();

        if (matchingRecipe.isEmpty()) {
            drawNoResultString(pPoseStack, x + 155, y + 5);
            this.hasShapedMatch = false;
            return;
        } else {
            this.hasShapedMatch = true;
        }

        AABB entityHitbox = this.menu.player.getBoundingBox();
        double longestSide = Math.max(entityHitbox.getXsize(), Math.max(entityHitbox.getYsize(), entityHitbox.getZsize()));

        int entityHeightOffset = (int) (54 / (longestSide / 1.8));

        float f = (float) Math.atan(((x + 155) - pMouseX) / 40.0F);
        float f1 = (float) Math.atan(((y + 64 - entityHeightOffset) - pMouseY) / 40.0F);

        int scale = (int) (22 / (longestSide / 1.8));

        Entity resultEntity =
            MultiblockRecipeManager.getInstance().getEntityForRecipe(matchingRecipe.get().getResultId(), clientLevel);
        if (resultEntity == null) {
            drawNoResultString(pPoseStack, x + 155, y + 5);
            return;
        }

        if (resultEntity instanceof LivingEntity livingResult) {
            renderLivingEntity(x + 155, y + 64, scale, f, f1, livingResult);
            drawCenteredString(pPoseStack, this.font, resultEntity.getDisplayName(), x + 155, y + 5, 8453920);
        } else {
            renderEntity(x + 155, y + 64, scale, f, f1, resultEntity);
            drawCenteredString(pPoseStack, this.font, resultEntity.getDisplayName(), x + 155, y + 5, 8453920);
        }

        MultiblockRecipe resultRecipe = MultiblockRecipeManager.getInstance().getRecipe(matchingRecipe.get().getResultId()).orElse(null);
        if (resultRecipe == null) return;
        if (resultRecipe.nbt() != null) {
            pPoseStack.pushPose();
            pPoseStack.translate(x + 132, y + 20, 0);
            pPoseStack.scale(0.8F, 0.8F, 1);
            GuiComponent.drawString(pPoseStack, Minecraft.getInstance().font, "+NBT", 0, 0, 11141290);
            pPoseStack.popPose();
        }
    }

    protected void drawNoResultString(PoseStack pPoseStack, int x, int y) {
        drawCenteredString(pPoseStack, this.font, NO_RESULT, x, y, 16736352);
    }

    protected void toggleSchematicDisplay() {
        this.renderSchematic = !this.renderSchematic;
        this.menu.entity.shouldRenderSchematicInWorld = this.renderSchematic;
        this.visibilityButton.alternateTexture = !this.renderSchematic;
    }

    protected void toggleSlice() {
        this.verticalRenderSlicing = !this.verticalRenderSlicing;
        this.menu.entity.verticalRenderSlicing = this.verticalRenderSlicing;
        this.verticalButton.alternateTexture = this.verticalRenderSlicing;
    }

    public void resetLayer() {
        this.renderedLayer = 0;
        this.menu.entity.renderedLayer = this.renderedLayer;
        this.menu.entity.queueLayerReset = false;
        this.menu.entity.actualRenderedLayer = 0;
    }

    protected void incrementLayer() {
        this.renderedLayer++;
        this.menu.entity.renderedLayer = this.renderedLayer;
    }

    protected void decrementLayer() {
        this.renderedLayer--;
        this.menu.entity.renderedLayer = this.renderedLayer;
    }

    protected static void renderLivingEntity(int pPosX, int pPosY, int pScale, float angleXComponent, float angleYComponent, LivingEntity pLivingEntity) {
        PoseStack posestack = RenderSystem.getModelViewStack();
        posestack.pushPose();
        posestack.translate(pPosX, pPosY, 1050.0D);
        posestack.scale(1.0F, 1.0F, -1.0F);

        RenderSystem.applyModelViewMatrix();
        PoseStack posestack1 = new PoseStack();
        posestack1.translate(0.0D, 0.0D, 1000.0D);
        posestack1.scale((float)pScale, (float)pScale, (float)pScale);

        Quaternion quaternion = Vector3f.ZP.rotationDegrees(180.0F);
        Quaternion quaternion1 = Vector3f.XP.rotationDegrees(angleYComponent * 20.0F);
        quaternion.mul(quaternion1);
        posestack1.mulPose(quaternion);

        float f2 = pLivingEntity.yBodyRot;
        float f3 = pLivingEntity.getYRot();
        float f4 = pLivingEntity.getXRot();
        float f5 = pLivingEntity.yHeadRotO;
        float f6 = pLivingEntity.yHeadRot;

        pLivingEntity.yBodyRot = 180.0F + angleXComponent * 20.0F;
        pLivingEntity.setYRot(180.0F + angleXComponent * 40.0F);
        pLivingEntity.setXRot(-angleYComponent * 20.0F);
        pLivingEntity.yHeadRot = pLivingEntity.getYRot();
        pLivingEntity.yHeadRotO = pLivingEntity.getYRot();
        Lighting.setupForEntityInInventory();

        EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        quaternion1.conj();
        entityrenderdispatcher.overrideCameraOrientation(quaternion1);
        entityrenderdispatcher.setRenderShadow(false);
        MultiBufferSource.BufferSource multibuffersource$buffersource = Minecraft.getInstance().renderBuffers().bufferSource();
        //noinspection deprecation
        RenderSystem.runAsFancy(() ->
                entityrenderdispatcher.render(pLivingEntity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, posestack1, multibuffersource$buffersource, 15728880));
        multibuffersource$buffersource.endBatch();
        entityrenderdispatcher.setRenderShadow(true);

        pLivingEntity.yBodyRot = f2;
        pLivingEntity.setYRot(f3);
        pLivingEntity.setXRot(f4);
        pLivingEntity.yHeadRotO = f5;
        pLivingEntity.yHeadRot = f6;
        posestack.popPose();

        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
    }

    protected static void renderEntity(int pPosX, int pPosY, int pScale, float angleXComponent, float angleYComponent, Entity pEntity) {
        PoseStack posestack = RenderSystem.getModelViewStack();
        posestack.pushPose();
        posestack.translate(pPosX, pPosY, 1050.0D);
        posestack.scale(1.0F, 1.0F, -1.0F);

        RenderSystem.applyModelViewMatrix();
        PoseStack posestack1 = new PoseStack();
        posestack1.translate(0.0D, 0.0D, 1000.0D);
        posestack1.scale((float)pScale, (float)pScale, (float)pScale);

        Quaternion quaternion = Vector3f.ZP.rotationDegrees(180.0F);
        Quaternion quaternion1 = Vector3f.XP.rotationDegrees(angleYComponent * 20.0F);
        quaternion.mul(quaternion1);
        posestack1.mulPose(quaternion);

        float yRot = pEntity.getYRot();
        float xRot = pEntity.getXRot();

        pEntity.setYRot(180.0F + angleXComponent * 40.0F);
        pEntity.setXRot(-angleYComponent * 20.0F);

        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        quaternion1.conj();
        entityrenderdispatcher.overrideCameraOrientation(quaternion1);

        entityrenderdispatcher.setRenderShadow(false);
        MultiBufferSource.BufferSource multibuffersource$buffersource = Minecraft.getInstance().renderBuffers().bufferSource();
        //noinspection deprecation
        RenderSystem.runAsFancy(() ->
                entityrenderdispatcher.render(pEntity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, posestack1, multibuffersource$buffersource, 15728880));
        multibuffersource$buffersource.endBatch();
        entityrenderdispatcher.setRenderShadow(true);

        pEntity.setYRot(yRot);
        pEntity.setXRot(xRot);

        posestack.popPose();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
    }
}
