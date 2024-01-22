package io.github.kawaiicakes.nobullship.multiblock;

import com.google.common.math.IntMath;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.nobullship.api.BlockInWorldPredicateBuilder;
import io.github.kawaiicakes.nobullship.api.MultiblockRecipeManager;
import io.github.kawaiicakes.nobullship.api.multiblock.MultiblockRecipe;
import io.github.kawaiicakes.nobullship.multiblock.block.MultiblockWorkshopBlockEntity;
import io.github.kawaiicakes.nobullship.schematic.SchematicRecipe;
import it.unimi.dsi.fastutil.objects.Object2ByteLinkedOpenHashMap;
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
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.*;

import static io.github.kawaiicakes.nobullship.Registry.ITEM_MARKER_PARTICLE;
import static io.github.kawaiicakes.nobullship.Registry.WILDCARD_BLOCK;
import static net.minecraft.client.renderer.LevelRenderer.DIRECTIONS;

@OnlyIn(Dist.CLIENT)
public class SchematicRenderer implements BlockEntityRenderer<MultiblockWorkshopBlockEntity> {
    protected static final Map<CompoundTag, Item> NBT_ITEM = new HashMap<>();
    protected static final Logger LOGGER = LogUtils.getLogger();
    protected static final Map<BlockPos, RenderInstructions> RENDER_QUEUE = new HashMap<>();
    protected static final Map<BakedModel, ChunkRenderTypeSet> RENDER_TYPE_CACHE = new HashMap<>();

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

        RenderSystem.disableCull();
        RenderSystem.enableBlend();
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

                    boolean isVertical = pBlockEntity.verticalRenderSlicing;

                    BlockIngredient inFront = !isVertical && (i - 1 >= 0) ? palette.get((pattern.get(i - 1))[j].charAt(k)) : BlockIngredient.AIR;
                    BlockIngredient behind = !isVertical && (i + 1 < zSize) ? palette.get((pattern.get(i + 1))[j].charAt(k)) : BlockIngredient.AIR;
                    BlockIngredient toLeft = (k - 1 >= 0) ? palette.get((pattern.get(i))[j].charAt(k - 1)) : BlockIngredient.AIR;
                    BlockIngredient toRight = (k + 1 < xSize) ? palette.get((pattern.get(i))[j].charAt(k + 1)) : BlockIngredient.AIR;
                    BlockIngredient above = isVertical && (j - 1 >= 0) ? palette.get((pattern.get(i))[j - 1].charAt(k)) : BlockIngredient.AIR;
                    BlockIngredient below = isVertical && (j + 1 < ySize) ? palette.get((pattern.get(i))[j + 1].charAt(k)) : BlockIngredient.AIR;

                    Map<Direction, BlockIngredient> blockIngredientByDirection = new HashMap<>(6);
                    blockIngredientByDirection.put(facing, behind);
                    blockIngredientByDirection.put(facing.getOpposite(), inFront);
                    blockIngredientByDirection.put(Direction.UP, above);
                    blockIngredientByDirection.put(Direction.DOWN, below);
                    blockIngredientByDirection.put(facing.getCounterClockWise(), toLeft);
                    blockIngredientByDirection.put(facing.getClockWise(), toRight);

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

                    if (forRender.hasNbt() && forRender.biwPredicateBuilder != null) {
                        pPoseStack.scale(0.5F, 0.5F, 0.5F);
                        pPoseStack.translate(0.5F, 0, 0.5F);

                        clientLevel.addParticle(
                                new ItemParticleOption(ITEM_MARKER_PARTICLE.get(), getNbtDisplayEntry(forRender.biwPredicateBuilder.getNaiveNbt())),
                                newPos.getX() + 0.5,
                                newPos.getY() + 0.8,
                                newPos.getZ() + 0.5,
                                0, 0, 0);
                    }

                    this.renderGhostBlock(
                            blockIngredientByDirection,
                            forRender,
                            newPos,
                            clientLevel, pPoseStack,
                            proxyBuffer,
                            clientLevel.getRandom(),
                            ModelData.EMPTY
                    );

                    pPoseStack.popPose();
                }
            }
        }

        ModelBlockRenderer.clearCache();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    @Override
    public boolean shouldRenderOffScreen(MultiblockWorkshopBlockEntity pBlockEntity) {
        return true;
    }

    public void tessellateWithAO(Map<Direction, BlockIngredient> neighbours, ModelBlockRenderer pRenderer, BlockAndTintGetter pLevel, BakedModel pModel, boolean stateHasNbt, BlockState pState, BlockPos pPos, PoseStack pStack, VertexConsumer pConsumer, RandomSource pRandom, long pSeed, int pPackedOverlay, ModelData modelData, RenderType renderType) {
        float[] afloat = new float[DIRECTIONS.length * 2];
        BitSet bitset = new BitSet(3);
        ModelBlockRenderer.AmbientOcclusionFace modelblockrenderer$ambientocclusionface = new ModelBlockRenderer.AmbientOcclusionFace();
        BlockPos.MutableBlockPos neighbourPos = pPos.mutable();

        ClientLevel clientLevel = Minecraft.getInstance().level;
        if (clientLevel == null) return;

        for(Direction direction : DIRECTIONS) {
            neighbourPos.setWithOffset(pPos, direction);
            BlockState neighbourState = neighbours.get(direction).getCurrentlySelected(clientLevel, neighbourPos);
            boolean neighbourHasNbt = neighbours.get(direction).hasNbt;

            pRandom.setSeed(pSeed);
            List<BakedQuad> list = pModel.getQuads(pState, direction, pRandom, modelData, renderType);
            if (!list.isEmpty() && this.shouldRenderFace(pLevel, stateHasNbt || neighbourHasNbt, pPos, neighbourPos, pState, neighbourState, direction)) {
                pRenderer.renderModelFaceAO(pLevel, pState, pPos, pStack, pConsumer, list, afloat, bitset, modelblockrenderer$ambientocclusionface, pPackedOverlay);
            }
        }

        pRandom.setSeed(pSeed);
        List<BakedQuad> quadList = pModel.getQuads(pState, null, pRandom, modelData, renderType);
        if (!quadList.isEmpty()) {
            pRenderer.renderModelFaceAO(pLevel, pState, pPos, pStack, pConsumer, quadList, afloat, bitset, modelblockrenderer$ambientocclusionface, pPackedOverlay);
        }
    }

    public void renderGhostBlock(Map<Direction, BlockIngredient> neighbours, BlockIngredient forRender, BlockPos pPos, BlockAndTintGetter pLevel, PoseStack pPoseStack, VertexConsumer pConsumer, RandomSource pRandom, ModelData modelData) {
        Level clientLevel = Minecraft.getInstance().level;
        if (clientLevel == null) return;
        BlockState stateForRender = forRender.getCurrentlySelected(clientLevel, pPos);

        try {
            RenderShape rendershape = stateForRender.getRenderShape();
            if (rendershape == RenderShape.MODEL) {
                BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
                ModelBlockRenderer modelRenderer = blockRenderer.getModelRenderer();

                BakedModel ogModel = blockRenderer.getBlockModel(stateForRender);
                RENDER_TYPE_CACHE.put(ogModel, ogModel.getRenderTypes(stateForRender, pRandom, ModelData.EMPTY));

                RenderType renderType = RenderType.solid();
                if (!RENDER_TYPE_CACHE.get(ogModel).asList().contains(RenderType.solid())) {
                    try {
                        renderType = RENDER_TYPE_CACHE.get(ogModel).asList().stream().findAny().orElseThrow();
                    } catch (RuntimeException e) {
                        LOGGER.error("Unable to render BakedModel for preview!", e);
                    }
                }

                this.tessellateWithAO(neighbours, modelRenderer, pLevel, ogModel, forRender.hasNbt, stateForRender, pPos, pPoseStack, pConsumer, pRandom, stateForRender.getSeed(pPos), OverlayTexture.NO_OVERLAY, modelData, renderType);
            }

        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Tessellating block in world");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Block being tessellated");
            CrashReportCategory.populateBlockDetails(crashreportcategory, pLevel, pPos, stateForRender);
            throw new ReportedException(crashreport);
        }
    }

    /**
     * Basically copied from {@link Block#shouldRenderFace(BlockState, BlockGetter, BlockPos, Direction, BlockPos)}.
     * The difference is simply the adaptation to work with the <code>SchematicRenderer</code>, who does not put
     * physical blocks in the world. This quirk means the original method would fail to work.
     * @param containsNbt whether either <code>BlockState</code> has NBT inside its <code>BlockEntity</code>.
     * @param pPos the <code>BlockPos</code> representing the coordinates of the block whose faces are being scrutinized.
     * @param pNeighbourPos the <code>BlockPos</code> representing the coordinates of the block adjacent to the <code>pPos</code>
     *             block in the <code>Direction</code> given by <code>pFace</code>.
     * @param pState the <code>BlockState</code> of the block whose faces are being scrutinized.
     * @param pFace the <code>Direction</code> representing the face on the block <code>pState</code> being scrutinized.
     * @return <code>true</code> if the passed arguments represent a circumstance where the face should render.
     * <code>false</code> otherwise.
     */
    public boolean shouldRenderFace(BlockAndTintGetter pLevel, boolean containsNbt, BlockPos pPos, BlockPos pNeighbourPos, BlockState pState, BlockState pNeighbourState, Direction pFace) {
        if (containsNbt) return true;
        if (pState.skipRendering(pNeighbourState, pFace)) {
            return false;
        } else if (pState.supportsExternalFaceHiding() && pNeighbourState.hidesNeighborFace(pLevel, pNeighbourPos, pState, pFace.getOpposite())) {
            return false;
        } else if (pNeighbourState.canOcclude()) {
            Block.BlockStatePairKey blockStatePairKey = new Block.BlockStatePairKey(pState, pNeighbourState, pFace);
            Object2ByteLinkedOpenHashMap<Block.BlockStatePairKey> occlusionCache = Block.OCCLUSION_CACHE.get();
            byte b0 = occlusionCache.getAndMoveToFirst(blockStatePairKey);
            if (b0 != 127) {
                return b0 != 0;
            } else {
                VoxelShape voxelshape = pState.getFaceOcclusionShape(pLevel, pPos, pFace);
                if (voxelshape.isEmpty()) {
                    return true;
                } else {
                    VoxelShape faceOcclusionShape = pNeighbourState.getFaceOcclusionShape(pLevel, pNeighbourPos, pFace.getOpposite());
                    boolean flag = Shapes.joinIsNotEmpty(voxelshape, faceOcclusionShape, BooleanOp.ONLY_FIRST);
                    if (occlusionCache.size() == 2048) {
                        occlusionCache.removeLastByte();
                    }

                    occlusionCache.putAndMoveToFirst(blockStatePairKey, (byte)(flag ? 1 : 0));
                    return flag;
                }
            }
        } else {
            return true;
        }
    }

    public static ItemStack getNbtDisplayEntry(CompoundTag strictNbt) {
        //noinspection OptionalGetWithoutIsPresent
        return NBT_ITEM.computeIfAbsent(strictNbt, (key) -> ForgeRegistries.ITEMS.getValues().stream().findAny().get()).getDefaultInstance();
    }

    public static class BlockIngredient {
        public static final BlockIngredient AIR = new BlockIngredient(null, Collections.singleton(Blocks.AIR.defaultBlockState()), null, false);
        public static final BlockIngredient WILDCARD = new BlockIngredient(null, Collections.singleton(WILDCARD_BLOCK.get().defaultBlockState()), null, false);
        protected static Random RANDOM_SRC = new Random();
        protected static int INCREMENT;

        @Nullable
        protected final BlockInWorldPredicateBuilder biwPredicateBuilder;
        protected final List<BlockState> validBlockStates;
        protected final Direction facing;
        protected Random seed;
        protected int randomIndexOld = 0;
        protected int randomIndex = 0;
        protected final boolean hasNbt;

        public BlockIngredient(BlockInWorldPredicateBuilder builder, @Nullable Direction facing) {
            this(builder, builder.getValidBlockstates(), facing, builder.requiresNbt());
        }

        public BlockIngredient(@Nullable BlockInWorldPredicateBuilder biwPredicateBuilder, Set<BlockState> validBlockStates, @Nullable Direction facing, boolean hasNbt) {
            this.biwPredicateBuilder = biwPredicateBuilder;
            this.validBlockStates = validBlockStates.stream().toList();
            this.facing = facing == null ? Direction.NORTH : facing;
            this.hasNbt = hasNbt;
        }

        public boolean hasNbt() {
            return this.hasNbt;
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
