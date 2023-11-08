package io.github.kawaiicakes.nobullship.block;

import io.github.kawaiicakes.nobullship.data.SchematicRecipe;
import io.github.kawaiicakes.nobullship.screen.MultiblockWorkshopMenu;
import it.unimi.dsi.fastutil.ints.IntImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static io.github.kawaiicakes.nobullship.NoBullship.SCHEMATIC;
import static io.github.kawaiicakes.nobullship.NoBullship.WORKSHOP_BLOCK_ENTITY;

/**
 * I am merely implementing <code>Container</code> via <code>BaseContainerBlockEntity</code> so that this is a valid
 * type parameter for <code>Recipe</code>. That being said accessing the 'inventory' of this block entity should
 * preferentially be done through the capability as that is where I am more certain things will behave nicely.
 * <br><br>
 * Edit: In retrospect, maybe this will function better than anticipated. It's probably fine to use the
 * <code>Container</code> implementations given that I've taken care to avoid using anything which would
 * mutate the <code>ItemStack</code>s in the <code>ItemStackHandler</code>.
 */
public class MultiblockWorkshopBlockEntity extends BlockEntity implements Container, MenuProvider, Nameable, StackedContentsCompatible {
    public static final IntImmutableList SHAPED_SLOTS = IntImmutableList.of(0,1,2,3,4,5,6,7,8);
    public static final IntImmutableList SHAPELESS_SLOTS = IntImmutableList.of(9, 10, 11, 12, 13, 14, 15, 16, 17);
    public static final IntImmutableList CRAFTING_SLOTS = IntImmutableList.of(0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17);
    public static final byte EMPTY_SCHEM_SLOT = 18;
    public static final byte FILLED_SCHEM_SLOT = 19;

    // I don't like the idea of this block having to tick
    protected boolean hasRecipe = false;
    protected SchematicRecipe currentRecipe;
    protected LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    protected final ItemStackHandler itemHandler = new ItemStackHandler(20) {
        @Override
        protected void onContentsChanged(int slot) {
            MultiblockWorkshopBlockEntity.this.contentsUpdated();
            MultiblockWorkshopBlockEntity.this.setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return MultiblockWorkshopBlockEntity.this.canPlaceItem(slot, stack);
        }
    };

    public MultiblockWorkshopBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(WORKSHOP_BLOCK_ENTITY.get(), pPos, pBlockState);
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
        this.contentsUpdated();
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
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new MultiblockWorkshopMenu(pContainerId, pPlayerInventory, this);
    }

    @Override
    public int getContainerSize() {
        return this.itemHandler.getSlots();
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

    /**
     * The returned contents are safe to modify and are not tied to the values in the <code>ItemHandler</code>,
     * which should be treated as immutable.
     * @return a <code>List</code> of the <code>ItemStack</code>s present in the shapeless slots of the workbench.
     */
    @NotNull
    public List<ItemStack> getShapelessContents() {
        List<ItemStack> toReturn = new ArrayList<>(SHAPELESS_SLOTS.size());
        for (int i : SHAPELESS_SLOTS) {
            toReturn.add(this.getItem(i));
        }
        return toReturn;
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

        // Calling ItemStackHandler#setStackInSlot is dubious
        final int stackSize = this.getItem(pSlot).getCount();
        this.itemHandler.extractItem(pSlot, stackSize, false);
        this.itemHandler.insertItem(pSlot, pStack, false);
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
    public void fillStackedContents(StackedContents pHelper) {
        for (int i = 0; i < this.itemHandler.getSlots(); i++) {
            pHelper.accountStack(this.getItem(i));
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
    public void reviveCaps() {
        super.reviveCaps();
        this.lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public Component getName() {
        return Component.translatable("block.nobullship.workshop");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.nobullship.workshop");
    }

    public void dropContents() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }

        assert this.level != null;
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    public void contentsUpdated() {
        if (this.level == null || !(this.level instanceof ServerLevel serverLevel)) return;

        Optional<SchematicRecipe> recipe = serverLevel.getRecipeManager()
                .getRecipeFor(SchematicRecipe.Type.INSTANCE, this, serverLevel);

        this.hasRecipe = recipe.isPresent() && canInsertAmountIntoOutputSlot(this) && canInsertItemIntoOutputSlot(this);
    }

    /**
     * When called, the contents of this entity are consumed and the result is added to the filled schematic slot.
     */
    protected void doCraft() {
        final SchematicRecipe immutableRecipe = this.currentRecipe;
        if (!this.hasRecipe || immutableRecipe == null) return;

        for (int i : SHAPELESS_SLOTS) {
            if (immutableRecipe.getShapelessIngredients().isEmpty()) break;

            final ItemStack newAmount = this.getItem(i);
            // The following nonsense is to account for if a recipe has the same ItemStack but with different counts
            // spread out in different slots...
            final int recipeAmount = immutableRecipe.getShapelessIngredients()
                    .stream()
                    .filter(standard -> standard.is(this.getItem(i).getItem())
                            && this.getItem(i).getCount() >= standard.getCount())
                    .map(ItemStack::getCount)
                    .max(Comparator.naturalOrder())
                    .orElseThrow();

            newAmount.shrink(recipeAmount);
            this.setItem(i, newAmount);
        }

        final ItemStack newAmount = this.getItem(EMPTY_SCHEM_SLOT);
        newAmount.shrink(1);
        this.setItem(EMPTY_SCHEM_SLOT, newAmount);

        if (this.getItem(FILLED_SCHEM_SLOT).isEmpty()) {
            this.setItem(FILLED_SCHEM_SLOT, immutableRecipe.assemble(this));
        } else {
            final ItemStack newCount = this.getItem(FILLED_SCHEM_SLOT);
            newCount.grow(1);
            this.setItem(FILLED_SCHEM_SLOT, newCount);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MultiblockWorkshopBlockEntity pEntity) {
        if (level.isClientSide()) return;

        if (pEntity.hasRecipe) {
            pEntity.doCraft();
            pEntity.contentsUpdated();
            setChanged(level, pos, state);
        }
    }

    protected static boolean canInsertItemIntoOutputSlot(MultiblockWorkshopBlockEntity container) {
        return container.getItem(FILLED_SCHEM_SLOT).getItem() == SCHEMATIC.get() || container.getItem(FILLED_SCHEM_SLOT).isEmpty();
    }

    protected static boolean canInsertAmountIntoOutputSlot(MultiblockWorkshopBlockEntity container) {
        return container.getItem(FILLED_SCHEM_SLOT).getMaxStackSize() > container.getItem(FILLED_SCHEM_SLOT).getCount();
    }
}
