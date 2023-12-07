package io.github.kawaiicakes.nobullship.multiblock;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kawaiicakes.nobullship.api.MultiblockRecipeManager;
import io.github.kawaiicakes.nobullship.api.multiblock.MultiblockRecipe;
import io.github.kawaiicakes.nobullship.multiblock.block.MultiblockWorkshopBlockEntity;
import io.github.kawaiicakes.nobullship.schematic.SchematicRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SchematicRenderer implements BlockEntityRenderer<MultiblockWorkshopBlockEntity> {
    protected static final Map<BlockPos, RenderInstructions> RENDER_QUEUE = new HashMap<>();

    public SchematicRenderer(BlockEntityRendererProvider.Context context) {}

    public static void setRecipe(MultiblockWorkshopBlockEntity entity, @Nullable SchematicRecipe recipe, Direction facing, BlockPos origin) {
        if (recipe == null) {
            stopRenderAt(origin);
            return;
        }

        MultiblockRecipe forRender = MultiblockRecipeManager.getInstance().getRecipe(recipe.getResultId()).orElse(null);
        if (forRender == null) return;
        if (RENDER_QUEUE.containsKey(origin) && forRender.result().equals(RENDER_QUEUE.get(origin).id)) return;

        CompoundTag patternTag = forRender.recipe().getSerializedPattern();
        if (patternTag == null) return;

        entity.queueLayerReset = true;

        RENDER_QUEUE.put(origin, new RenderInstructions(forRender.result(), MultiblockPattern.rawPaletteFromNbt(patternTag),
                MultiblockPattern.rawPatternFromNbt(patternTag), facing));
    }

    public static void stopRenderAt(BlockPos entityPos) {
        RENDER_QUEUE.remove(entityPos);
    }

    @Override
    public void render(MultiblockWorkshopBlockEntity pBlockEntity, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pPackedOverlay) {
        if (RENDER_QUEUE.isEmpty()) return;
        BlockPos posOfEntity = pBlockEntity.getBlockPos();
        // I want to avoid caching block entities. I'm concerned about memory leaks
        if (!RENDER_QUEUE.containsKey(posOfEntity)) return;

        RenderInstructions entry = RENDER_QUEUE.get(posOfEntity);
        List<String[]> pattern = entry.pattern;
        int zSize = pattern.size();
        int ySize = pattern.get(0).length;
        int xSize = pattern.get(0)[0].length();

        // FIXME: zero will appear twice when renderedLayer is near 0
        int layerForRender = pBlockEntity.verticalRenderSlicing
                ? (pBlockEntity.renderedLayer < 0 ? zSize - Math.abs(pBlockEntity.renderedLayer % zSize) - 1 : pBlockEntity.renderedLayer % zSize)
                : (pBlockEntity.renderedLayer < 0 ? ySize - Math.abs(pBlockEntity.renderedLayer % ySize) - 1 : pBlockEntity.renderedLayer % ySize);

        pBlockEntity.actualRenderedLayer = layerForRender;

        if (!pBlockEntity.shouldRenderSchematicInWorld) return;

        ClientLevel clientLevel = Minecraft.getInstance().level;
        if (clientLevel == null) return;

        BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
        ModelBlockRenderer.enableCaching();

        Map<Character, BlockState> palette = entry.palette;
        Direction facing = entry.direction;

        BlockPos previewPosition = posOfEntity.mutable().move(facing, -(zSize + 1)).offset(0, ySize - 1, 0).immutable();

        for(int i = 0; i < zSize; ++i) {
            if (pBlockEntity.verticalRenderSlicing && layerForRender != i) continue;

            for(int j = 0; j < ySize; ++j) {
                if (!pBlockEntity.verticalRenderSlicing && layerForRender != j) continue;

                for(int k = 0; k < xSize; ++k) {
                    BlockState forRender = palette.get((pattern.get(i))[j].charAt(k));
                    if (forRender == null) continue;

                    BlockPos newPos = MultiblockPattern.translateAndRotate(previewPosition, facing, Direction.UP, k, j, i);
                    if (!clientLevel.isEmptyBlock(newPos)) continue;
                    BlockPos offset = newPos.subtract(previewPosition).mutable().move(facing, -(zSize + 1)).offset(0, ySize - 1, 0).immutable();

                    pPoseStack.pushPose();
                    pPoseStack.translate(offset.getX(), offset.getY(), offset.getZ());

                    //noinspection deprecation
                    blockRenderer.renderBatched(
                            forRender, newPos,
                            clientLevel, pPoseStack,
                            pBufferSource.getBuffer(RenderType.solid()), true,
                            clientLevel.getRandom()
                    );

                    pPoseStack.popPose();
                }
            }
        }

        ModelBlockRenderer.clearCache();
    }

    @Override
    public boolean shouldRenderOffScreen(MultiblockWorkshopBlockEntity pBlockEntity) {
        return true;
    }

    public record RenderInstructions(ResourceLocation id, Map<Character, BlockState> palette, List<String[]> pattern, Direction direction) {}
}
