package io.github.kawaiicakes.nobullship.block;

import io.github.kawaiicakes.nobullship.data.SchematicRecipe;
import io.github.kawaiicakes.nobullship.screen.MultiblockWorkshopMenu;
import it.unimi.dsi.fastutil.ints.IntImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static io.github.kawaiicakes.nobullship.NoBullship.SCHEMATIC;
import static io.github.kawaiicakes.nobullship.NoBullship.WORKSHOP_BLOCK_ENTITY;

public class MultiblockWorkshopBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, StackedContentsCompatible {
    public static final IntImmutableList SHAPED_SLOTS = IntImmutableList.of(0,1,2,3,4,5,6,7,8);
    public static final IntImmutableList SHAPELESS_SLOTS = IntImmutableList.of(9, 10, 11, 12, 13, 14, 15, 16, 17);
    public static final IntImmutableList CRAFTING_SLOTS = IntImmutableList.of(0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17);
    public static final byte EMPTY_SCHEM_SLOT = 18;
    public static final byte FILLED_SCHEM_SLOT = 19;

    protected boolean hasRecipe = false;

    LazyOptional<? extends net.minecraftforge.items.IItemHandler>[] handlers =
            SidedInvWrapper.create(this, Direction.UP, Direction.DOWN, Direction.NORTH);

    protected NonNullList<ItemStack> contents = NonNullList.withSize(20, ItemStack.EMPTY);

    public MultiblockWorkshopBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(WORKSHOP_BLOCK_ENTITY.get(), pPos, pBlockState);
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        this.contents = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(pTag, this.contents);
        this.contentsUpdated();
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        super.saveAdditional(pTag);
        ContainerHelper.saveAllItems(pTag, this.contents);
    }

    @Override
    public int[] getSlotsForFace(Direction pSide) {
        if (pSide == Direction.DOWN) {
            return ArrayUtils.add(CRAFTING_SLOTS.toIntArray(), FILLED_SCHEM_SLOT);
        } else {
            return pSide == Direction.UP ? new int[]{EMPTY_SCHEM_SLOT} : CRAFTING_SLOTS.toIntArray();
        }
    }

    @Override
    public boolean canPlaceItem(int pIndex, ItemStack pStack) {
        if (pIndex == FILLED_SCHEM_SLOT) return false;
        if (pIndex == EMPTY_SCHEM_SLOT) return true;
        return CRAFTING_SLOTS.intStream().anyMatch(integer -> integer == pIndex);
    }

    // TODO
    @Override
    public boolean canPlaceItemThroughFace(int pIndex, ItemStack pItemStack, @Nullable Direction pDirection) {
        return this.canPlaceItem(pIndex, pItemStack);
    }

    // TODO
    @Override
    public boolean canTakeItemThroughFace(int pIndex, ItemStack pStack, Direction pDirection) {
        if (pDirection == Direction.DOWN && SHAPED_SLOTS.intStream().anyMatch(i -> i == pIndex)) {
            return pStack.is(Items.BUCKET);
        } else {
            return true;
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.nobullship.workshop");
    }

    @Override
    protected AbstractContainerMenu createMenu(int pContainerId, Inventory pInventory) {
        return new MultiblockWorkshopMenu(pContainerId, pInventory, this);
    }

    @Override
    public int getContainerSize() {
        return this.contents.size();
    }

    @Override
    public boolean isEmpty() {
        for(ItemStack itemstack : this.contents) {
            if (!itemstack.isEmpty()) return false;
        }

        return true;
    }

    @Override
    public ItemStack getItem(int pSlot) {
        return this.contents.get(pSlot);
    }

    @Override
    public ItemStack removeItem(int pSlot, int pAmount) {
        return ContainerHelper.removeItem(this.contents, pSlot, pAmount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int pSlot) {
        return ContainerHelper.takeItem(this.contents, pSlot);
    }

    @Override
    public void setItem(int pSlot, ItemStack pStack) {
        this.contents.set(pSlot, pStack);
        if (pStack.getCount() > this.getMaxStackSize()) {
            pStack.setCount(this.getMaxStackSize());
        }
        this.contentsUpdated();
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
        this.contents.clear();
    }

    @Override
    public void fillStackedContents(StackedContents pHelper) {
        for(ItemStack itemstack : this.contents) {
            pHelper.accountStack(itemstack);
        }
    }

    @Override
    public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @javax.annotation.Nullable Direction facing) {
        if (!this.remove && facing != null && capability == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER) {
            if (facing == Direction.UP)
                return handlers[0].cast();
            else if (facing == Direction.DOWN)
                return handlers[1].cast();
            else
                return handlers[2].cast();
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        for (LazyOptional<? extends IItemHandler> handler : handlers)
            handler.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        this.handlers = net.minecraftforge.items.wrapper.SidedInvWrapper.create(this, Direction.UP, Direction.DOWN, Direction.NORTH);
    }

    public void contentsUpdated() {
        if (this.level == null) return;
        if (!(this.level instanceof ServerLevel serverLevel)) return;

        Optional<SchematicRecipe> recipe = serverLevel.getRecipeManager()
                .getRecipeFor(SchematicRecipe.Type.INSTANCE, this, serverLevel);

        this.hasRecipe = recipe.isPresent() && canInsertAmountIntoOutputSlot(this.contents) && canInsertItemIntoOutputSlot(this.contents);

        if (this.hasRecipe) this.doCraft(recipe.get());
    }

    protected void doCraft(SchematicRecipe recipe) {
        for (int i : SHAPELESS_SLOTS) {
            int difference = this.contents.get(i).getCount() - recipe.getShapelessIngredients().get(i).getCount();

            if (difference <= 0) {
                this.contents.set(i, ItemStack.EMPTY);
                continue;
            }

            this.contents.get(i).setCount(difference);
        }

        int schemDifference = this.contents.get(EMPTY_SCHEM_SLOT).getCount() - 1;
        if (schemDifference <= 0) {
            this.contents.set(EMPTY_SCHEM_SLOT, ItemStack.EMPTY);
        } else {
            this.contents.get(EMPTY_SCHEM_SLOT).setCount(schemDifference);
        }

        if (this.contents.get(FILLED_SCHEM_SLOT).isEmpty()) {
            this.contents.set(FILLED_SCHEM_SLOT, recipe.assemble(this));
        } else {
            this.contents.get(FILLED_SCHEM_SLOT).setCount(this.contents.get(FILLED_SCHEM_SLOT).getCount() + 1);
        }
    }

    protected static boolean canInsertItemIntoOutputSlot(NonNullList<ItemStack> contents) {
        return contents.get(FILLED_SCHEM_SLOT).getItem() == SCHEMATIC.get() || contents.get(FILLED_SCHEM_SLOT).isEmpty();
    }

    protected static boolean canInsertAmountIntoOutputSlot(NonNullList<ItemStack> contents) {
        return contents.get(FILLED_SCHEM_SLOT).getMaxStackSize() > contents.get(FILLED_SCHEM_SLOT).getCount();
    }
}
