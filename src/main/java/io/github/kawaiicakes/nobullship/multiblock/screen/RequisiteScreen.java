package io.github.kawaiicakes.nobullship.multiblock.screen;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kawaiicakes.nobullship.api.MultiblockRecipeManager;
import io.github.kawaiicakes.nobullship.api.multiblock.MultiblockRecipe;
import io.github.kawaiicakes.nobullship.multiblock.block.MultiblockWorkshopBlockEntity;
import io.github.kawaiicakes.nobullship.network.NoBullshipPackets;
import io.github.kawaiicakes.nobullship.network.ServerboundWorkshopOpenPacket;
import io.github.kawaiicakes.nobullship.schematic.SchematicRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.Optional;

import static io.github.kawaiicakes.nobullship.NoBullship.MOD_ID;

@OnlyIn(Dist.CLIENT)
public class RequisiteScreen extends Screen {
    public static final ResourceLocation REQUISITES = new ResourceLocation(MOD_ID, "textures/gui/requisites.png");
    public static final Component REQ_MESSAGE = Component.translatable("gui.nobullship.requisite_screen");
    public static final Component CLOSE_MSG = Component.translatable("gui.nobullship.close");

    protected final BlockPos blockEntityPos;
    protected final ImmutableList<ItemStack> requisites;
    protected final DisplayButton close;
    protected final DisplayButton leftPg;
    protected final DisplayButton rightPg;
    protected final int maxPages;
    protected int page = 0;

    protected RequisiteScreen(BlockPos blockEntityPos) {
        super(Component.empty());

        this.close = new DisplayButton(
                16, 16,
                176, 0, 16,
                REQUISITES, 256, 256,
                (button) -> Minecraft.getInstance().setScreen(null),
                (button, stack, mX, mY) -> this.renderTooltip(stack, CLOSE_MSG, mX, mY));
        this.leftPg = new DisplayButton(
                16, 16,
                192, 0, 16,
                REQUISITES, 256, 256,
                (button) -> this.page--,
                (button, stack, mX, mY) -> {});
        this.rightPg = new DisplayButton(
                16, 16,
                208, 0, 16,
                REQUISITES, 256, 256,
                (button) -> this.page++,
                (button, stack, mX, mY) -> {});

        this.blockEntityPos = blockEntityPos;
        if (Minecraft.getInstance().level == null) throw new IllegalArgumentException("No client level yet a screen attempted to be instantiated!");

        BlockEntity be = Minecraft.getInstance().level.getExistingBlockEntity(this.blockEntityPos);
        if (!(be instanceof MultiblockWorkshopBlockEntity workshop)) throw new IllegalArgumentException("No workshop at passed coords!");

        List<SchematicRecipe> recipeList
                = Minecraft.getInstance().level.getRecipeManager().getAllRecipesFor(SchematicRecipe.Type.INSTANCE);
        if (recipeList.isEmpty()) {
            this.requisites = ImmutableList.of();
            this.maxPages = 0;
            return;
        }

        List<SchematicRecipe> shapedMatches = recipeList.stream()
                .filter(recipe -> recipe.shapedMatches(workshop))
                .toList();

        Optional<SchematicRecipe> matchingRecipe = shapedMatches.stream().findFirst();
        if (shapedMatches.size() > 1)
            matchingRecipe = shapedMatches.stream().filter(recipe -> recipe.shapelessMatches(workshop)).findFirst();
        if (matchingRecipe.isEmpty()) {
            this.requisites = ImmutableList.of();
            this.maxPages = 0;
            return;
        }

        MultiblockRecipe resultRecipe = MultiblockRecipeManager.getInstance().getRecipe(matchingRecipe.get().getResultId()).orElse(null);
        if (resultRecipe == null) {
            this.requisites = ImmutableList.of();
            this.maxPages = 0;
            return;
        }

        ImmutableList.Builder<ItemStack> requisiteBuilder = ImmutableList.builder();
        if (matchingRecipe.get().getRequisites() != null) requisiteBuilder.addAll(matchingRecipe.get().getRequisites());
        // written like this because for some reason IntelliJ states that resultRecipe.requisites() could be null
        // even after the null check... I'm paranoid, so I'll just acquiesce to its desires despite me being fairly certain that it's fine
        ImmutableList<ItemStack> multiblockReqs = resultRecipe.requisites();
        if (multiblockReqs != null) requisiteBuilder.addAll(multiblockReqs);
        
        this.requisites = requisiteBuilder.build();

        if (((((float) this.requisites.size()) / 36F) == (float) (this.requisites.size() / 36))) {
            this.maxPages = this.requisites.size() / 36;
        } else {
            this.maxPages = (this.requisites.size() / 36) + 1;
        }
    }

    @Override
    protected void init() {
        super.init();

        int guiX = (this.width - 176) / 2;
        int guiY = (this.height - 194) / 2;

        this.close.setActive(true);
        this.leftPg.setActive(this.maxPages > 1);
        this.rightPg.setActive(this.maxPages > 1);

        this.close.setPosition(8 + guiX, 7 + guiY);
        this.leftPg.setPosition(134 + guiX, 7 + guiY);
        this.rightPg.setPosition(152 + guiX, 7 + guiY);

        this.addRenderableWidget(this.close);
        this.addRenderableWidget(this.leftPg);
        this.addRenderableWidget(this.rightPg);
    }

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(pPoseStack);
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);

        if (this.page < 0) this.page = this.maxPages - 1;
        if (this.page > this.maxPages - 1) this.page = 0;

        int guiX = (this.width - 176) / 2;
        int guiY = (this.height - 194) / 2;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, REQUISITES);

        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();

        blit(pPoseStack, guiX, guiY, -5, 0, 0, 176, 194, 256, 256);

        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();

        Font font = Minecraft.getInstance().font;
        drawCenteredString(pPoseStack, font, REQ_MESSAGE, this.width / 2, guiY + 6, 16777215);
        drawCenteredString(pPoseStack, font, Component.translatable("gui.nobullship.pg_number", this.page + 1, this.maxPages), this.width / 2, guiY + 16, 0xAAAAAA);

        final int currentPage = this.page;
        for (int i = 0; i < 36; i++) {
            if (i > (this.requisites.size() - 1)) break;

            int slotNumber = i + (36 * currentPage);
            if (slotNumber > (this.requisites.size() - 1)) break;

            int slotX = 8 + ((i % 9) * 18);
            int slotY = 28 + ((i / 9) * 18);

            ItemStack stack = this.requisites.get(slotNumber);

            if (stack == null || stack.isEmpty()) continue;

            Minecraft.getInstance().getItemRenderer().renderAndDecorateFakeItem(stack, slotX + guiX, slotY + guiY);
            Minecraft.getInstance().getItemRenderer().renderGuiItemDecorations(font, stack, slotX + guiX, slotY + guiY);
            if (isHovering(guiX, guiY, slotX, slotY, pMouseX, pMouseY))
                this.renderTooltip(pPoseStack, stack, pMouseX, pMouseY);
        }

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        List<ItemStack> invItems = player.getInventory().items;
        for (int i = 0; i < 9; ++i) {
            ItemStack stack;
            try {
                stack = invItems.get(i);
            } catch (RuntimeException ignored) {
                continue;
            }
            if (stack.isEmpty()) continue;

            int slotX = 8 + i * 18;
            int slotY = 170;
            Minecraft.getInstance().getItemRenderer().renderAndDecorateFakeItem(stack, slotX + guiX, slotY + guiY);
            Minecraft.getInstance().getItemRenderer().renderGuiItemDecorations(font, stack, slotX + guiX, slotY + guiY);
            if (isHovering(guiX, guiY, slotX, slotY, pMouseX, pMouseY))
                this.renderTooltip(pPoseStack, stack, pMouseX, pMouseY);
        }

        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                ItemStack stack;
                try {
                    stack = invItems.get(l + i * 9 + 9);
                } catch (RuntimeException ignored) {
                    continue;
                }
                if (stack.isEmpty()) continue;

                int slotX = 8 + l * 18;
                int slotY = (i * 18) + 112;
                Minecraft.getInstance().getItemRenderer().renderAndDecorateFakeItem(stack, slotX + guiX, slotY + guiY);
                Minecraft.getInstance().getItemRenderer().renderGuiItemDecorations(font, stack, slotX + guiX, slotY + guiY);
                if (isHovering(guiX, guiY, slotX, slotY, pMouseX, pMouseY))
                    this.renderTooltip(pPoseStack, stack, pMouseX, pMouseY);
            }
        }
    }

    @Override
    public void removed() {
        super.removed();
        NoBullshipPackets.sendToServer(new ServerboundWorkshopOpenPacket(blockEntityPos));
    }

    protected static boolean isHovering(int guiX, int guiY, int pX, int pY, double pMouseX, double pMouseY) {
        pMouseX -= guiX;
        pMouseY -= guiY;

        return pMouseX >= (double)(pX - 1) && pMouseX < (double)(pX + 16 + 1) && pMouseY >= (double)(pY - 1) && pMouseY < (double)(pY + 16 + 1);
    }
}
