package io.github.kawaiicakes.nobullship.multiblock.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;
import io.github.kawaiicakes.nobullship.api.MultiblockRecipeManager;
import io.github.kawaiicakes.nobullship.api.multiblock.MultiblockRecipe;
import io.github.kawaiicakes.nobullship.multiblock.MultiblockPattern;
import io.github.kawaiicakes.nobullship.multiblock.screen.MultiblockWorkshopMenu;
import io.github.kawaiicakes.nobullship.schematic.SchematicRecipe;
import it.unimi.dsi.fastutil.ints.IntImmutableList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.github.kawaiicakes.nobullship.NoBullship.*;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

/**
 * I am merely implementing <code>Container</code> via <code>BaseContainerBlockEntity</code> so that this is a valid
 * type parameter for <code>Recipe</code>. That being said accessing the 'inventory' of this block entity should
 * preferentially be done through the capability as that is where I am more certain things will behave nicely.
 * <br><br>
 * Addendum 1: In retrospect, maybe this will function better than anticipated. It's probably fine to use the
 * <code>Container</code> implementations given that I've taken care to avoid using anything which would
 * mutate the <code>ItemStack</code>s in the <code>ItemStackHandler</code>.
 * Addendum 2: Okay, some weird shit is happening, and I'm attributing it to desync. Looking through a tutorial
 * I can see that the "forbidden" methods mentioned above are actually being used. Welp, no harm in trying.
 */
public class MultiblockWorkshopBlockEntity extends BaseContainerBlockEntity {
    public static final IntImmutableList SHAPELESS_SLOTS = IntImmutableList.of(9, 10, 11, 12, 13, 14, 15, 16, 17);
    public static final IntImmutableList CRAFTING_SLOTS = IntImmutableList.of(0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17);
    public static final byte EMPTY_SCHEM_SLOT = 18;
    public static final byte FILLED_SCHEM_SLOT = 19;
    public static final Component DEFAULT_NAME = Component.translatable("block.nobullship.workshop");

    @Nullable
    protected SchematicRecipe hasRecipe;
    protected LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    public final ItemStackHandler itemHandler = new ItemStackHandler(20) {
        @Override
        protected void onContentsChanged(int slot) {
            MultiblockWorkshopBlockEntity.this.setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == FILLED_SCHEM_SLOT) return false;
            return MultiblockWorkshopBlockEntity.this.canPlaceItem(slot, stack);
        }
    };

    public MultiblockWorkshopBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(WORKSHOP_BLOCK_ENTITY.get(), pPos, pBlockState);
    }

    public void setActiveRecipe(@Nullable SchematicRecipe recipe) {
        this.hasRecipe = recipe;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        this.lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        this.itemHandler.deserializeNBT(pTag.getCompound("inventory"));
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        pTag.put("inventory", this.itemHandler.serializeNBT());
        super.saveAdditional(pTag);
    }

    @Override
    public boolean canPlaceItem(int pIndex, ItemStack pStack) {
        if (pIndex == FILLED_SCHEM_SLOT) return false;
        if (pStack.is(SCHEMATIC.get()) && pIndex == EMPTY_SCHEM_SLOT) return true;
        return CRAFTING_SLOTS.intStream().anyMatch(integer -> integer == pIndex);
    }

    @Override
    protected AbstractContainerMenu createMenu(int pContainerId, Inventory pInventory) {
        return new MultiblockWorkshopMenu(pContainerId, pInventory, this);
    }

    @Override
    public int getContainerSize() {
        return SHAPELESS_SLOTS.size() + 1;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < this.itemHandler.getSlots(); i++) {
            // DO NOT MODIFY THE STACK FROM THE ITEM HANDLER
            if (!this.itemHandler.getStackInSlot(i).isEmpty()) return false;
        }

        return true;
    }

    /** This returns a copy of a value that should be treated as immutable to avoid problems. If there are issues with
     *  this inventory, this is a good place to start looking for clues
     */
    @Override
    public ItemStack getItem(int pSlot) {
        return this.itemHandler.getStackInSlot(pSlot).copy();
    }

    @Override
    public ItemStack removeItem(int pSlot, int pAmount) {
        boolean testValidity =
                pSlot >= 0 && pSlot < this.itemHandler.getSlots() && !this.getItem(pSlot).isEmpty() && pAmount > 0;

        ItemStack toReturn = ItemStack.EMPTY;
        // #extractItem returns a stack which may be safely modified
        if (testValidity) toReturn = this.itemHandler.extractItem(pSlot, pAmount, false);

        return toReturn;
    }

    @Override
    public ItemStack removeItemNoUpdate(int pSlot) {
        final int stackSize = this.getItem(pSlot).getCount();

        return pSlot >= 0 && pSlot < this.itemHandler.getSlots()
                ? this.itemHandler.extractItem(pSlot, stackSize, false)
                : ItemStack.EMPTY;
    }

    @Override
    public void setItem(int pSlot, ItemStack pStack) {
        if (pSlot <  0 || pSlot > this.itemHandler.getSlots()) return;

        if (pStack.getCount() > this.getMaxStackSize()) {
            pStack.setCount(this.getMaxStackSize());
        }

        this.itemHandler.setStackInSlot(pSlot, pStack);
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        if (this.level == null) return false;
        if (this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        } else {
            return pPlayer.distanceToSqr((double)this.worldPosition.getX() + 0.5D, (double)this.worldPosition.getY() + 0.5D, (double)this.worldPosition.getZ() + 0.5D) <= 64.0D;
        }
    }

    @Override
    public void clearContent() {
        // Again, calling ItemStackHandler#setStackInSlot is dubious.
        // This is just taken from the implementation of #clear() in NonNullList lol
        for (int i = 0; i < this.itemHandler.getSlots(); i++) {
            final int count = this.getItem(i).getCount();
            this.itemHandler.extractItem(i, count, false);
        }
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction facing) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            return this.lazyItemHandler.cast();
        }

        return super.getCapability(capability, facing);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        this.lazyItemHandler.invalidate();
    }

    @Override
    protected Component getDefaultName() {
        return DEFAULT_NAME;
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T t) {
        if (!(t instanceof MultiblockWorkshopBlockEntity entity)) return;

        if (level instanceof ClientLevel clientLevel) {
            SchematicRenderer.renderRecipe(clientLevel.getRecipeManager().getRecipeFor(SchematicRecipe.Type.INSTANCE, entity, clientLevel).orElse(null), state.getValue(HORIZONTAL_FACING), pos);
            return;
        }

        if (level instanceof ServerLevel serverLevel) {
            if (entity.hasRecipe != null && entity.hasRecipe.shapedMatches(entity)) {
                serverLevel.sendParticles(new BlockParticleOption(MINI_GHOST_PARTICLE.get(), Blocks.DIRT.defaultBlockState()), (double) pos.getX() + 0.5, (double) pos.getY() + 2.5, (double) pos.getZ() + 0.5, 1, 0, 0, 0, 0);
            }
        }
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class SchematicRenderer {
        protected static final Map<Pair<BlockPos, Direction>, Pair<List<String[]>, Map<Character, BlockState>>> RENDER_QUEUE = new HashMap<>();

        public static void renderRecipe(@Nullable SchematicRecipe recipe, Direction facing, BlockPos origin) {
            Pair<BlockPos, Direction> keyPair = Pair.of(origin, facing);

            if (recipe == null) {
                forceStopRender(keyPair);
                return;
            }

            MultiblockRecipe forRender = MultiblockRecipeManager.getInstance().getRecipe(recipe.getResultId()).orElse(null);
            if (forRender == null) return;
            if (RENDER_QUEUE.containsKey(keyPair)) return;

            CompoundTag patternTag = forRender.recipe().getSerializedPattern();
            if (patternTag == null) return;

            RENDER_QUEUE.put(keyPair, MultiblockPattern.rawPatternFromNbt(patternTag));
        }

        public static void forceStopRender(Pair<BlockPos, Direction> entityPos) {
            RENDER_QUEUE.remove(entityPos);
        }

        @SubscribeEvent
        public static void render(RenderLevelStageEvent event) {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) return;
            if (RENDER_QUEUE.isEmpty()) return;

            ClientLevel clientLevel = Minecraft.getInstance().level;
            if (clientLevel == null) return;

            BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
            Vec3 cameraPosition = event.getCamera().getPosition();

            PoseStack stack = event.getPoseStack();

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

            for (Map.Entry<Pair<BlockPos, Direction>, Pair<List<String[]>, Map<Character, BlockState>>> entry : RENDER_QUEUE.entrySet()) {
                int zSize = entry.getValue().getFirst().size();
                int ySize = entry.getValue().getFirst().get(0).length;
                int xSize = entry.getValue().getFirst().get(0)[0].length();

                BlockPos previewPosition = entry.getKey().getFirst().mutable().move(entry.getKey().getSecond(), -(zSize + 1)).move(0, ySize - 1, 0).immutable();

                for(int i = 0; i < zSize; ++i) {
                    for(int j = 0; j < ySize; ++j) {
                        for(int k = 0; k < xSize; ++k) {
                            BlockState forRender = entry.getValue().getSecond().get((entry.getValue().getFirst().get(i))[j].charAt(k));
                            if (forRender == null) continue;

                            BlockPos newPos = MultiblockPattern.translateAndRotate(previewPosition, entry.getKey().getSecond(), Direction.UP, k, j, i);
                            if (!clientLevel.getBlockState(newPos).isAir()) continue;

                            stack.pushPose();
                            stack.translate(newPos.getX() - cameraPosition.x(), newPos.getY() - cameraPosition.y(), newPos.getZ() - cameraPosition.z());

                            //noinspection deprecation
                            blockRenderer.renderBatched(
                                    forRender, newPos,
                                    clientLevel, stack,
                                    proxyBuffer, true,
                                    clientLevel.getRandom()
                            );

                            stack.popPose();
                        }
                    }
                }
            }
        }
    }
}
