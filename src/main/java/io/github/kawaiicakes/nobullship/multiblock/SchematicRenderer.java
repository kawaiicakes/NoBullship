package io.github.kawaiicakes.nobullship.multiblock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.kawaiicakes.nobullship.api.MultiblockRecipeManager;
import io.github.kawaiicakes.nobullship.api.multiblock.MultiblockRecipe;
import io.github.kawaiicakes.nobullship.multiblock.block.MultiblockWorkshopBlockEntity;
import io.github.kawaiicakes.nobullship.schematic.SchematicRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SchematicRenderer implements BlockEntityRenderer<MultiblockWorkshopBlockEntity> {
    protected static final Map<BlockPos, RenderInstructions> RENDER_QUEUE = new HashMap<>();

    public SchematicRenderer(BlockEntityRendererProvider.Context context) {};

    public static void setRecipe(@Nullable SchematicRecipe recipe, Direction facing, BlockPos origin) {
        if (recipe == null) {
            stopRenderAt(origin);
            return;
        }

        MultiblockRecipe forRender = MultiblockRecipeManager.getInstance().getRecipe(recipe.getResultId()).orElse(null);
        if (forRender == null) return;
        if (RENDER_QUEUE.containsKey(origin)) return;

        CompoundTag patternTag = forRender.recipe().getSerializedPattern();
        if (patternTag == null) return;

        RENDER_QUEUE.put(origin, new RenderInstructions(MultiblockPattern.rawPaletteFromNbt(patternTag),
                MultiblockPattern.rawPatternFromNbt(patternTag), facing));
    }

    public static void stopRenderAt(BlockPos entityPos) {
        RENDER_QUEUE.remove(entityPos);
    }

    @Override
    public void render(MultiblockWorkshopBlockEntity pBlockEntity, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pPackedOverlay) {
        if (RENDER_QUEUE.isEmpty()) return;

        ClientLevel clientLevel = Minecraft.getInstance().level;
        if (clientLevel == null) return;

        BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();

        VertexConsumer buffer = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(RenderType.solid());
        buffer.color(0.145F, 0.588F, 0.745F, 1.0f);
        VertexConsumer proxyBuffer = new VertexConsumer() {
            @Override
            public void putBulkData(PoseStack.Pose pPoseEntry, BakedQuad pQuad, float[] pColorMuls, float pRed, float pGreen, float pBlue, int[] pCombinedLights, int pCombinedOverlay, boolean pMulColor) {
                putBulkData(pPoseEntry, pQuad, pColorMuls, pRed, pGreen, pBlue, 0.5f, pCombinedLights, pCombinedOverlay, pMulColor);
            }

            @Override
            public VertexConsumer vertex(double pX, double pY, double pZ) {
                return buffer.vertex(pX, pY, pZ);
            }

            @Override
            public VertexConsumer color(int pRed, int pGreen, int pBlue, int pAlpha) {
                return buffer.color(pRed, pGreen, pBlue, pAlpha);
            }

            @Override
            public VertexConsumer uv(float pU, float pV) {
                return buffer.uv(pU, pV);
            }

            @Override
            public VertexConsumer overlayCoords(int pU, int pV) {
                return buffer.overlayCoords(pU, pV);
            }

            @Override
            public VertexConsumer uv2(int pU, int pV) {
                return buffer.uv2(pU, pV);
            }

            @Override
            public VertexConsumer normal(float pX, float pY, float pZ) {
                return buffer.normal(pX, pY, pZ);
            }

            @Override
            public void endVertex() {
                buffer.endVertex();
            }

            @Override
            public void defaultColor(int pDefaultR, int pDefaultG, int pDefaultB, int pDefaultA) {
                buffer.defaultColor(pDefaultR, pDefaultG, pDefaultB, pDefaultA);
            }

            @Override
            public void unsetDefaultColor() {
                buffer.unsetDefaultColor();
            }
        };

        for (Map.Entry<BlockPos, RenderInstructions> entry : RENDER_QUEUE.entrySet()) {
            List<String[]> pattern = entry.getValue().pattern;
            Map<Character, BlockState> palette = entry.getValue().palette;
            Direction facing = entry.getValue().direction;

            int zSize = pattern.size();
            int ySize = pattern.get(0).length;
            int xSize = pattern.get(0)[0].length();

            BlockPos previewPosition = entry.getKey().mutable().move(facing, -(zSize + 1)).move(0, ySize - 1, 0).immutable();

            for(int i = 0; i < zSize; ++i) {
                for(int j = 0; j < ySize; ++j) {
                    for(int k = 0; k < xSize; ++k) {
                        BlockState forRender = palette.get((pattern.get(i))[j].charAt(k));
                        if (forRender == null) continue;

                        BlockPos newPos = MultiblockPattern.translateAndRotate(previewPosition, facing, Direction.UP, k, j, i);
                        if (!clientLevel.isEmptyBlock(newPos)) continue;

                        //noinspection deprecation
                        blockRenderer.renderBatched(
                                forRender, newPos,
                                clientLevel, pPoseStack,
                                proxyBuffer, true,
                                clientLevel.getRandom()
                        );
                    }
                }
            }
        }
    }

    public record RenderInstructions(Map<Character, BlockState> palette, List<String[]> pattern, Direction direction) {}
}
