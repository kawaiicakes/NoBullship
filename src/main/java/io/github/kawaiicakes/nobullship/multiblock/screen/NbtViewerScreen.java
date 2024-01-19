package io.github.kawaiicakes.nobullship.multiblock.screen;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.nobullship.api.MultiblockRecipeManager;
import io.github.kawaiicakes.nobullship.api.multiblock.MultiblockRecipe;
import io.github.kawaiicakes.nobullship.multiblock.SchematicRenderer;
import io.github.kawaiicakes.nobullship.multiblock.block.MultiblockWorkshopBlockEntity;
import io.github.kawaiicakes.nobullship.multiblock.block.ProxyContainer;
import io.github.kawaiicakes.nobullship.network.NoBullshipPackets;
import io.github.kawaiicakes.nobullship.network.ServerboundRequestContainerIdPacket;
import io.github.kawaiicakes.nobullship.network.ServerboundWorkshopOpenPacket;
import io.github.kawaiicakes.nobullship.schematic.SchematicRecipe;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.github.kawaiicakes.nobullship.NoBullship.MOD_ID;
import static io.github.kawaiicakes.nobullship.multiblock.screen.RequisiteScreen.*;

@OnlyIn(Dist.CLIENT)
public class NbtViewerScreen extends Screen {
    protected static final Logger LOGGER = LogUtils.getLogger();
    public static final ResourceLocation NBT_VIEWER_TEX = new ResourceLocation(MOD_ID, "textures/gui/nbt_viewer.png");
    public static final Component NBT_VIEWER_MSG = Component.translatable("gui.nobullship.nbt_viewer_screen");
    protected static int CONTAINER_COUNT;

    protected final BlockPos blockEntityPos;
    protected final ImmutableList<ItemStack> nbtBlockList;
    protected final DisplayButton close;
    protected final DisplayButton leftPg;
    protected final DisplayButton rightPg;
    protected final DisplayButton topLeft;
    protected final DisplayButton topRight;
    protected final DisplayButton bottomLeft;
    protected final DisplayButton bottomRight;
    protected Pair<Block, CompoundTag> topLeftBlock;
    protected Pair<Block, CompoundTag> topRightBlock;
    protected Pair<Block, CompoundTag> bottomLeftBlock;
    protected Pair<Block, CompoundTag> bottomRightBlock;
    protected final int maxPages;
    protected int page = 0;

    protected NbtViewerScreen(BlockPos blockEntityPos) {
        super(Component.empty());
        this.blockEntityPos = blockEntityPos;

        NoBullshipPackets.sendToServer(new ServerboundRequestContainerIdPacket());

        this.close = new DisplayButton(
                16, 16,
                176, 0, 16,
                NBT_VIEWER_TEX, 256, 256,
                (button) -> Minecraft.getInstance().setScreen(null),
                (button, stack, mX, mY) -> this.renderTooltip(stack, CLOSE_MSG, mX, mY));
        this.leftPg = new DisplayButton(
                16, 16,
                192, 0, 16,
                NBT_VIEWER_TEX, 256, 256,
                (button) -> this.page--,
                (button, stack, mX, mY) -> {});
        this.rightPg = new DisplayButton(
                16, 16,
                208, 0, 16,
                NBT_VIEWER_TEX, 256, 256,
                (button) -> this.page++,
                (button, stack, mX, mY) -> {});
        this.topLeft = new DisplayButton(
                20, 16,
                224, 0, 16,
                NBT_VIEWER_TEX, 256, 256,
                (button) ->
                        this.createProxyContainer(this.blockEntityPos.above(), this.topLeftBlock.getFirst(), this.topLeftBlock.getSecond()),
                (button, stack, mX, mY) -> this.renderTooltip(stack, VIEW_DATA, mX, mY));
        this.topRight = new DisplayButton(
                20, 16,
                224, 0, 16,
                NBT_VIEWER_TEX, 256, 256,
                (button) ->
                        this.createProxyContainer(this.blockEntityPos.atY(30), this.topRightBlock.getFirst(), this.topRightBlock.getSecond()),
                (button, stack, mX, mY) -> this.renderTooltip(stack, VIEW_DATA, mX, mY));
        this.bottomLeft = new DisplayButton(
                20, 16,
                224, 0, 16,
                NBT_VIEWER_TEX, 256, 256,
                (button) ->
                        this.createProxyContainer(this.blockEntityPos.atY(30), this.bottomLeftBlock.getFirst(), this.bottomLeftBlock.getSecond()),
                (button, stack, mX, mY) -> this.renderTooltip(stack, VIEW_DATA, mX, mY));
        this.bottomRight = new DisplayButton(
                20, 16,
                224, 0, 16,
                NBT_VIEWER_TEX, 256, 256,
                (button) ->
                        this.createProxyContainer(this.blockEntityPos.atY(30), this.bottomRightBlock.getFirst(), this.bottomRightBlock.getSecond()),
                (button, stack, mX, mY) -> this.renderTooltip(stack, VIEW_DATA, mX, mY));

        if (Minecraft.getInstance().level == null) throw new IllegalArgumentException("No client level yet a screen attempted to be instantiated!");

        BlockEntity be = Minecraft.getInstance().level.getExistingBlockEntity(this.blockEntityPos);
        if (!(be instanceof MultiblockWorkshopBlockEntity workshop)) throw new IllegalArgumentException("No workshop at passed coords!");

        List<SchematicRecipe> recipeList
                = Minecraft.getInstance().level.getRecipeManager().getAllRecipesFor(SchematicRecipe.Type.INSTANCE);
        if (recipeList.isEmpty()) {
            this.nbtBlockList = ImmutableList.of();
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
            this.nbtBlockList = ImmutableList.of();
            this.maxPages = 0;
            return;
        }

        MultiblockRecipe resultRecipe = MultiblockRecipeManager.getInstance().getRecipe(matchingRecipe.get().getResultId()).orElse(null);
        if (resultRecipe == null) {
            this.nbtBlockList = ImmutableList.of();
            this.maxPages = 0;
            return;
        }

        this.nbtBlockList = ImmutableList.copyOf(
                resultRecipe.recipe().getTotalBlocks()
                        .stream()
                        .filter((stack) -> {
                            if (!stack.hasTag()) return false;
                            assert stack.getTag() != null;
                            return stack.getTag().contains("BlockEntityTag");
                        })
                        .collect(Collectors.toList())
        );

        if (((((float) this.nbtBlockList.size()) / 36F) == (float) (this.nbtBlockList.size() / 36))) {
            this.maxPages = this.nbtBlockList.size() / 36;
        } else {
            this.maxPages = (this.nbtBlockList.size() / 36) + 1;
        }

        this.topLeft.setVisible(true);
        this.topRight.setVisible(false);
        this.bottomLeft.setVisible(false);
        this.bottomRight.setVisible(false);
    }

    public static void setContainerCount(int count) {
        CONTAINER_COUNT = count;
    }

    @Override
    protected void init() {
        super.init();

        int guiX = (this.width - 176) / 2;
        int guiY = (this.height - 186) / 2;

        this.close.setActive(true);
        this.leftPg.setActive(this.maxPages > 1);
        this.rightPg.setActive(this.maxPages > 1);
        this.topLeft.setActive(true);
        this.topRight.setActive(true);
        this.bottomLeft.setActive(true);
        this.bottomRight.setActive(true);

        this.close.setPosition(8 + guiX, 7 + guiY);
        this.leftPg.setPosition(134 + guiX, 7 + guiY);
        this.rightPg.setPosition(152 + guiX, 7 + guiY);
        this.topLeft.setPosition(57 + guiX, guiY + 44);
        this.topRight.setPosition(139 + guiX, guiY + 44);
        this.bottomLeft.setPosition(57 + guiX, guiY + 78);
        this.bottomRight.setPosition(139 + guiX, guiY + 78);

        this.addRenderableWidget(this.close);
        this.addRenderableWidget(this.leftPg);
        this.addRenderableWidget(this.rightPg);
        this.addRenderableWidget(this.topLeft);
        this.addRenderableWidget(this.topRight);
        this.addRenderableWidget(this.bottomLeft);
        this.addRenderableWidget(this.bottomRight);
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        InputConstants.Key key = InputConstants.getKey(pKeyCode, pScanCode);
        if (Minecraft.getInstance().options.keyInventory.isActiveAndMatches(key)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(pPoseStack);

        if (this.page < 0) this.page = this.maxPages - 1;
        if (this.page > this.maxPages - 1) this.page = 0;

        int guiX = (this.width - 176) / 2;
        int guiY = (this.height - 186) / 2;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, NBT_VIEWER_TEX);

        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();

        blit(pPoseStack, guiX, guiY, -5, 0, 0, 176, 194, 256, 256);

        Font font = Minecraft.getInstance().font;
        drawCenteredString(pPoseStack, font, NBT_VIEWER_MSG, this.width / 2, guiY + 6, 16777215);
        drawCenteredString(pPoseStack, font, Component.translatable("gui.nobullship.pg_number", this.page + 1, this.maxPages), this.width / 2, guiY + 16, 0xAAAAAA);

        final int currentPage = this.page;
        int nbtId = 0;
        for (int i = 0; i < 4; i++) {
            Block entityBlock = null;
            CompoundTag tag = null;

            if (i > (this.nbtBlockList.size() - 1)) {
                switch (i) {
                    case 0 -> this.topLeftBlock = Pair.of(entityBlock, tag);
                    case 1 -> this.topRightBlock = Pair.of(entityBlock, tag);
                    case 2 -> this.bottomLeftBlock = Pair.of(entityBlock, tag);
                    case 3 -> this.bottomRightBlock = Pair.of(entityBlock, tag);
                    default -> throw new IllegalArgumentException();
                }
                break;
            }

            int slotNumber = i + (4 * currentPage);
            if (slotNumber > (this.nbtBlockList.size() - 1)) {
                switch (i) {
                    case 0 -> this.topLeftBlock = Pair.of(entityBlock, tag);
                    case 1 -> this.topRightBlock = Pair.of(entityBlock, tag);
                    case 2 -> this.bottomLeftBlock = Pair.of(entityBlock, tag);
                    case 3 -> this.bottomRightBlock = Pair.of(entityBlock, tag);
                    default -> throw new IllegalArgumentException();
                }
                break;
            }

            ItemStack stack = this.nbtBlockList.get(slotNumber);
            if (stack == null || stack.isEmpty()) {
                switch (i) {
                    case 0 -> this.topLeftBlock = Pair.of(entityBlock, tag);
                    case 1 -> this.topRightBlock = Pair.of(entityBlock, tag);
                    case 2 -> this.bottomLeftBlock = Pair.of(entityBlock, tag);
                    case 3 -> this.bottomRightBlock = Pair.of(entityBlock, tag);
                    default -> throw new IllegalArgumentException();
                }
                continue;
            }
            if (stack.getTag() == null) {
                switch (i) {
                    case 0 -> this.topLeftBlock = Pair.of(entityBlock, tag);
                    case 1 -> this.topRightBlock = Pair.of(entityBlock, tag);
                    case 2 -> this.bottomLeftBlock = Pair.of(entityBlock, tag);
                    case 3 -> this.bottomRightBlock = Pair.of(entityBlock, tag);
                    default -> throw new IllegalArgumentException();
                }
                continue;
            }

            tag = stack.getTag().getCompound("BlockEntityTag");
            if (tag.isEmpty()) {
                switch (i) {
                    case 0 -> this.topLeftBlock = Pair.of(entityBlock, tag);
                    case 1 -> this.topRightBlock = Pair.of(entityBlock, tag);
                    case 2 -> this.bottomLeftBlock = Pair.of(entityBlock, tag);
                    case 3 -> this.bottomRightBlock = Pair.of(entityBlock, tag);
                    default -> throw new IllegalArgumentException();
                }
                continue;
            }

            Block block = stack.getItem() instanceof BlockItem blockItem ? blockItem.getBlock() : null;
            if (block instanceof EntityBlock entityBlock1) entityBlock = (Block) entityBlock1;
            switch (i) {
                case 0 -> this.topLeftBlock = Pair.of(entityBlock, tag);
                case 1 -> this.topRightBlock = Pair.of(entityBlock, tag);
                case 2 -> this.bottomLeftBlock = Pair.of(entityBlock, tag);
                case 3 -> this.bottomRightBlock = Pair.of(entityBlock, tag);
                default -> throw new IllegalArgumentException();
            }

            int slotX = 14 + ((i % 2) * 82);
            int slotY = 39 + ((i / 2) * 34);

            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShaderTexture(0, NBT_VIEWER_TEX);
            blit(pPoseStack, slotX + guiX - 8, slotY + guiY - 8, -4, 176, 96, 80, 32, 256, 256);

            Minecraft.getInstance().getItemRenderer().renderAndDecorateFakeItem(stack, slotX + guiX, slotY + guiY);
            Minecraft.getInstance().getItemRenderer().renderGuiItemDecorations(font, stack, slotX + guiX, slotY + guiY);
            if (isHovering(guiX, guiY, slotX, slotY, pMouseX, pMouseY))
                this.renderTooltip(pPoseStack, stack, pMouseX, pMouseY);

            ItemStack nbtKey = SchematicRenderer.getNbtDisplayEntry(tag);
            drawCenteredString(pPoseStack, font, Component.literal("NBT #" + nbtId++), slotX + guiX + 43, slotY + guiY - 3, 0xAAAAAA);
            Minecraft.getInstance().getItemRenderer().renderAndDecorateFakeItem(nbtKey, slotX + guiX + 26, slotY + guiY + 5);
            if (isHovering(guiX, guiY, slotX + 26, slotY + 5, pMouseX, pMouseY))
                this.renderTooltip(pPoseStack, Component.translatable("gui.nobullship.nbt_key", nbtKey.getDisplayName()), pMouseX, pMouseY);
        }

        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);

        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();

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
            int slotY = 162;
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
                int slotY = (i * 18) + 104;
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
        // a packet is used here because I can't be bothered to figure out how to force it open from the client
        // from the bits I did see though, I'm not sure if doing it from the client would be safe anyway...
        // oh btw future ashley, setting screen to null does indeed exit the current screen :)
        NoBullshipPackets.sendToServer(new ServerboundWorkshopOpenPacket(blockEntityPos));
    }

    public void createProxyContainer(BlockPos blockPos, Block block, CompoundTag tag) {
        try {
            if (!(block instanceof EntityBlock entityBlock)) throw new IllegalArgumentException("Passed block is not an EntityBlock!");
            BlockEntity originalContainer = entityBlock.newBlockEntity(blockPos, block.defaultBlockState());
            if (originalContainer == null) {
                LOGGER.error("Unable to create proxy container; spoofed block entity could not be made!");
                return;
            }
            originalContainer.load(tag);
            assert Minecraft.getInstance().level != null;
            originalContainer.setLevel(Minecraft.getInstance().level);

            ProxyContainer<?> proxyContainer = new ProxyContainer<>(originalContainer, blockPos, block.defaultBlockState(), CONTAINER_COUNT);

            MenuScreens.getScreenFactory(proxyContainer.getMenuType(), Minecraft.getInstance(), CONTAINER_COUNT, proxyContainer.getDisplayName()).ifPresent(f -> {
                assert Minecraft.getInstance().player != null;
                AbstractContainerMenu c = proxyContainer.getMenuType().create(CONTAINER_COUNT, Minecraft.getInstance().player.getInventory(), new FriendlyByteBuf(Unpooled.buffer()));

                @SuppressWarnings("unchecked") Screen s = ((MenuScreens.ScreenConstructor<AbstractContainerMenu, ?>) f).create(c, Minecraft.getInstance().player.getInventory(), proxyContainer.getDisplayName());
                Minecraft.getInstance().player.containerMenu = ((MenuAccess<?>) s).getMenu();
                Minecraft.getInstance().setScreen(s);
            });
        } catch (RuntimeException e) {
            LOGGER.error("Unable to create proxy container!", e);
        }
    }
}
