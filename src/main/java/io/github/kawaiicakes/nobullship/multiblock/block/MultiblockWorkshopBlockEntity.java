package io.github.kawaiicakes.nobullship.multiblock.block;

import io.github.kawaiicakes.nobullship.multiblock.SchematicRenderer;
import io.github.kawaiicakes.nobullship.multiblock.screen.MultiblockWorkshopMenu;
import io.github.kawaiicakes.nobullship.schematic.SchematicRecipe;
import it.unimi.dsi.fastutil.ints.IntImmutableList;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

import static io.github.kawaiicakes.nobullship.Registry.SCHEMATIC;
import static io.github.kawaiicakes.nobullship.Registry.WORKSHOP_BLOCK_ENTITY;
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
    public boolean shouldRenderSchematicInWorld;
    public boolean verticalRenderSlicing;
    public int renderedLayer = 0;
    public int actualRenderedLayer;
    public boolean queueLayerReset = false;

    public MultiblockWorkshopBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(WORKSHOP_BLOCK_ENTITY.get(), pPos, pBlockState);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(this.getBlockPos()).inflate(140);
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
        if (!level.isClientSide) return;
        if (!(t instanceof MultiblockWorkshopBlockEntity entity)) return;
        if (!(level instanceof ClientLevel clientLevel)) return;

        if (!entity.shouldRenderSchematicInWorld) {
            SchematicRenderer.setRecipe(entity, null, state.getValue(HORIZONTAL_FACING), pos);
            entity.actualRenderedLayer = 0;
            return;
        }

        if (entity.isEmpty()) {
            SchematicRenderer.setRecipe(entity, null, state.getValue(HORIZONTAL_FACING), pos);
            entity.actualRenderedLayer = 0;
            return;
        }

        SchematicRecipe recipe = clientLevel.getRecipeManager().getAllRecipesFor(SchematicRecipe.Type.INSTANCE).stream().filter(managerRecipe ->
                managerRecipe.shapedMatches(entity)).findAny().orElse(null);
        SchematicRenderer.setRecipe(entity, recipe, state.getValue(HORIZONTAL_FACING), pos);
        if (recipe == null) entity.actualRenderedLayer = 0;
    }
}
