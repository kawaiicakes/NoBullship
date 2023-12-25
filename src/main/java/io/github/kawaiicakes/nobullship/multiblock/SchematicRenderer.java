package io.github.kawaiicakes.nobullship.multiblock;

import com.google.common.math.IntMath;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.nobullship.api.BlockInWorldPredicateBuilder;
import io.github.kawaiicakes.nobullship.api.MultiblockRecipeManager;
import io.github.kawaiicakes.nobullship.api.multiblock.MultiblockRecipe;
import io.github.kawaiicakes.nobullship.multiblock.block.MultiblockWorkshopBlockEntity;
import io.github.kawaiicakes.nobullship.schematic.SchematicRecipe;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.*;

import static io.github.kawaiicakes.nobullship.Registry.WILDCARD_BLOCK;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

public class SchematicRenderer implements BlockEntityRenderer<MultiblockWorkshopBlockEntity> {
    protected static final Logger LOGGER = LogUtils.getLogger();
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

        List<String[]> listForRender = MultiblockPattern.rawPatternFromNbt(patternTag);
        if (listForRender == null || listForRender.isEmpty()) {
            LOGGER.error("Unable to set recipe {} as pattern for render is null or empty!", recipe.getId());
            return;
        }
        Map<Character, BlockInWorldPredicateBuilder> predicateMap = MultiblockPattern.rawPaletteFromNbt(patternTag);
        if (predicateMap == null || predicateMap.isEmpty()) {
            LOGGER.error("Unable to set recipe {} as palette is null or empty!", recipe.getId());
            return;
        }

        Map<Character, BlockIngredient> paletteMap = new HashMap<>();
        for (Map.Entry<Character, BlockInWorldPredicateBuilder> stateEntry : predicateMap.entrySet()) {
            paletteMap.put(stateEntry.getKey(), new BlockIngredient(stateEntry.getValue(), facing));
        }

        RENDER_QUEUE.put(origin, new RenderInstructions(forRender.result(), paletteMap, listForRender, facing));
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

        int layerForRender = pBlockEntity.verticalRenderSlicing
                ? IntMath.mod(pBlockEntity.renderedLayer, zSize)
                : IntMath.mod(pBlockEntity.renderedLayer, ySize);

        pBlockEntity.actualRenderedLayer = layerForRender;

        if (!pBlockEntity.shouldRenderSchematicInWorld) return;

        ClientLevel clientLevel = Minecraft.getInstance().level;
        if (clientLevel == null) return;

        ModelBlockRenderer.enableCaching();

        Map<Character, BlockIngredient> palette = entry.palette;
        Direction facing = entry.facing;
        palette.put(' ', BlockIngredient.AIR);
        palette.put('$', BlockIngredient.WILDCARD);

        BlockPos previewPosition = posOfEntity.mutable().move(facing, -(zSize + 1)).offset(0, ySize - 1, 0).immutable();

        for(int i = 0; i < zSize; ++i) {
            if (pBlockEntity.verticalRenderSlicing && layerForRender != i) continue;

            for(int j = 0; j < ySize; ++j) {
                if (!pBlockEntity.verticalRenderSlicing && layerForRender != j) continue;

                for(int k = 0; k < xSize; ++k) {
                    BlockIngredient forRender = palette.get((pattern.get(i))[j].charAt(k));
                    if (forRender == null) continue;

                    BlockPos newPos = MultiblockPattern.translateAndRotate(previewPosition, facing, Direction.UP, k, j, i);
                    if (!clientLevel.isEmptyBlock(newPos)) continue;
                    BlockPos offset = newPos.subtract(previewPosition).mutable().move(facing, -(zSize + 1)).offset(0, ySize - 1, 0).immutable();

                    VertexConsumer buffer = pBufferSource.getBuffer(RenderType.translucent());
                    buffer.color(1.0F, 1.0F, 1.0F, 0.1F);

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

                    pPoseStack.pushPose();
                    pPoseStack.translate(offset.getX(), offset.getY(), offset.getZ());

                    List<Direction> hiddenFaces = new ArrayList<>();
                    Direction workshopFacing = pBlockEntity.getBlockState().getValue(HORIZONTAL_FACING);
                    for (Direction direction : Direction.values()) {
                        if (pBlockEntity.verticalRenderSlicing) {
                            if (direction.equals(workshopFacing) || direction.equals(workshopFacing.getOpposite())) continue;

                            BlockIngredient toLeft = BlockIngredient.AIR;
                            if (k - 1 >= 0) toLeft = palette.get((pattern.get(i))[j].charAt(k - 1));

                            BlockIngredient toRight = BlockIngredient.AIR;
                            if (k + 1 < xSize) toRight = palette.get((pattern.get(i))[j].charAt(k + 1));

                            BlockIngredient above = BlockIngredient.AIR;
                            if (j - 1 >= 0) above = palette.get((pattern.get(i))[j - 1].charAt(k));

                            BlockIngredient below = BlockIngredient.AIR;
                            if (j + 1 < ySize) below = palette.get((pattern.get(i))[j + 1].charAt(k));

                            if (direction.equals(workshopFacing.getCounterClockWise()) && blockDoesNotOccludeFace(forRender.getCurrentlySelected(clientLevel, newPos), toLeft.getCurrentlySelected(clientLevel, newPos))) continue;
                            if (direction.equals(workshopFacing.getClockWise()) && blockDoesNotOccludeFace(forRender.getCurrentlySelected(clientLevel, newPos), toRight.getCurrentlySelected(clientLevel, newPos))) continue;
                            if (direction.equals(Direction.UP) && blockDoesNotOccludeFace(forRender.getCurrentlySelected(clientLevel, newPos), above.getCurrentlySelected(clientLevel, newPos))) continue;
                            if (direction.equals(Direction.DOWN) && blockDoesNotOccludeFace(forRender.getCurrentlySelected(clientLevel, newPos), below.getCurrentlySelected(clientLevel, newPos))) continue;

                            hiddenFaces.add(direction);
                            continue;
                        }
                        if (direction.equals(Direction.UP) || direction.equals(Direction.DOWN)) continue;

                        BlockIngredient toLeft = BlockIngredient.AIR;
                        if (k - 1 >= 0) toLeft = palette.get((pattern.get(i))[j].charAt(k - 1));

                        BlockIngredient toRight = BlockIngredient.AIR;
                        if (k + 1 < xSize) toRight = palette.get((pattern.get(i))[j].charAt(k + 1));

                        BlockIngredient toFront = BlockIngredient.AIR;
                        if (i - 1 >= 0) toFront = palette.get((pattern.get(i - 1))[j].charAt(k));

                        BlockIngredient behind = BlockIngredient.AIR;
                        if (i + 1 < zSize) behind = palette.get((pattern.get(i + 1))[j].charAt(k));

                        if (direction.equals(workshopFacing.getCounterClockWise()) && blockDoesNotOccludeFace(forRender.getCurrentlySelected(clientLevel, newPos), toLeft.getCurrentlySelected(clientLevel, newPos))) continue;
                        if (direction.equals(workshopFacing.getClockWise()) && blockDoesNotOccludeFace(forRender.getCurrentlySelected(clientLevel, newPos), toRight.getCurrentlySelected(clientLevel, newPos))) continue;
                        if (direction.equals(workshopFacing.getOpposite()) && blockDoesNotOccludeFace(forRender.getCurrentlySelected(clientLevel, newPos), toFront.getCurrentlySelected(clientLevel, newPos))) continue;
                        if (direction.equals(workshopFacing) && blockDoesNotOccludeFace(forRender.getCurrentlySelected(clientLevel, newPos), behind.getCurrentlySelected(clientLevel, newPos))) continue;

                        hiddenFaces.add(direction);
                    }

                    this.renderGhostBlock(
                            forRender.getCurrentlySelected(clientLevel, newPos), newPos,
                            clientLevel, pPoseStack,
                            proxyBuffer, true,
                            clientLevel.getRandom(),
                            ModelData.EMPTY, RenderType.translucent(),
                            true,
                            hiddenFaces
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

    public void renderGhostBlock(BlockState pState, BlockPos pPos, BlockAndTintGetter pLevel, PoseStack pPoseStack, VertexConsumer pConsumer, boolean pCheckSides, RandomSource pRandom, ModelData modelData, RenderType renderType, boolean queryModelSpecificData, List<Direction> hiddenFaces) {
        try {
            RenderShape rendershape = pState.getRenderShape();
            if (rendershape == RenderShape.MODEL) {
                BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
                ModelBlockRenderer modelRenderer = blockRenderer.getModelRenderer();

                BakedModel ogModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(pState);
                BakedModelWrapper<?> model = new BakedModelWrapper<>(ogModel) {
                    @Override
                    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
                        if (side == null) {
                            return super.getQuads(state, null, rand);
                        }

                        for (Direction face : hiddenFaces) {
                            if (face.equals(side)) return new ArrayList<>();
                        }
                        return super.getQuads(state, side, rand);
                    }

                    @Override
                    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData extraData, @Nullable RenderType renderType) {
                        if (side == null) {
                            return super.getQuads(state, null, rand, extraData, renderType);
                        }

                        for (Direction face : hiddenFaces) {
                            if (face.equals(side)) return new ArrayList<>();
                        }
                        return super.getQuads(state, side, rand, extraData, renderType);
                    }
                };

                modelRenderer.tesselateBlock(pLevel, model, pState, pPos, pPoseStack, pConsumer, pCheckSides, pRandom, pState.getSeed(pPos), OverlayTexture.NO_OVERLAY, modelData, renderType, queryModelSpecificData);
            }

        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Tessellating block in world");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Block being tessellated");
            CrashReportCategory.populateBlockDetails(crashreportcategory, pLevel, pPos, pState);
            throw new ReportedException(crashreport);
        }
    }

    public static boolean blockDoesNotOccludeFace(BlockState occludedBlock, BlockState block) {
        if (occludedBlock.is(WILDCARD_BLOCK.get()) && block.is(WILDCARD_BLOCK.get())) return false;
        return block.getRenderShape() == RenderShape.INVISIBLE || block.is(WILDCARD_BLOCK.get());
    }

    public static class BlockIngredient {
        public static final BlockIngredient AIR = new BlockIngredient(Collections.singleton(Blocks.AIR.defaultBlockState()), null);
        public static final BlockIngredient WILDCARD = new BlockIngredient(Collections.singleton(WILDCARD_BLOCK.get().defaultBlockState()), null);
        protected static Random RANDOM_SRC = new Random();
        protected static int INCREMENT;

        protected final List<BlockState> validBlockStates;
        protected final Direction facing;
        protected Random seed;
        protected int randomIndexOld = 0;
        protected int randomIndex = 0;

        public BlockIngredient(BlockInWorldPredicateBuilder builder, @Nullable Direction facing) {
            this(builder.getValidBlockstates(), facing);
        }

        public BlockIngredient(Set<BlockState> validBlockStates, @Nullable Direction facing) {
            this.validBlockStates = validBlockStates.stream().toList();
            this.facing = facing == null ? Direction.NORTH : facing;
        }

        public BlockState getCurrentlySelected(LevelAccessor levelAccessor, BlockPos pos) {
            // Prevents ArrayIndexOutOfBoundsException when rendering preview for first time
            if (this.validBlockStates.isEmpty()) return Blocks.AIR.defaultBlockState();
            int arraySize = this.validBlockStates.size();

            if (!RANDOM_SRC.equals(this.seed)) {
                this.randomIndex = RANDOM_SRC.nextInt(arraySize);
                this.seed = RANDOM_SRC;

                if (this.randomIndexOld == this.randomIndex) this.randomIndex
                        += this.randomIndex == (arraySize - 1) ? -arraySize + 1 : 1;
            }

            this.randomIndexOld = this.randomIndex;
            return this.validBlockStates.get(this.randomIndex).rotate(
                    levelAccessor,
                    pos,
                    rotationFromNorth(this.facing)
            );
        }

        public static void tickRandomSeedChange() {
            if (INCREMENT++ < 60) return;
            INCREMENT = 0;
            RANDOM_SRC = new Random();
        }

        /**
         * Returns the <code>Rotation</code> necessary to rotate the passed argument from north to the direction of this ingredient.
         * This is written solely for use in the horizontal plane.
         * @param facing the <code>Direction</code> something is facing. Meaningless for values of UP or DOWN.
         * @return the <code>Rotation</code> necessary to point towards the passed <code>Direction</code>.
         */
        public static Rotation rotationFromNorth(Direction facing) {
            return switch (facing) {
                case SOUTH -> Rotation.CLOCKWISE_180;
                case WEST -> Rotation.COUNTERCLOCKWISE_90;
                case EAST -> Rotation.CLOCKWISE_90;
                default -> Rotation.NONE;
            };
        }
    }

    public record RenderInstructions(ResourceLocation id, Map<Character, BlockIngredient> palette, List<String[]> pattern, Direction facing) {}
}
