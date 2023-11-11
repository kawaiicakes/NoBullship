package io.github.kawaiicakes.nobullship.data;

import io.github.kawaiicakes.nobullship.block.MultiblockWorkshopBlockEntity;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.RecipeHolder;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Optional;

import static io.github.kawaiicakes.nobullship.block.MultiblockWorkshopBlockEntity.EMPTY_SCHEM_SLOT;
import static io.github.kawaiicakes.nobullship.block.MultiblockWorkshopBlockEntity.SHAPELESS_SLOTS;

public class SchematicResultSlot extends Slot {
    protected static Container EMPTY = new SimpleContainer(0);
    protected final MultiblockWorkshopBlockEntity blockEntity;
    protected final IItemHandler itemHandler;
    protected Player player;
    protected int removeCount;

    public SchematicResultSlot(MultiblockWorkshopBlockEntity blockEntity, IItemHandler handler, Player player, int slot, int xPosition, int yPosition) {
        super(EMPTY, slot, xPosition, yPosition);
        this.blockEntity = blockEntity;
        this.itemHandler = handler;
        this.player = player;
    }

    @Override
    public ItemStack getItem() {
        return this.itemHandler.getStackInSlot(this.getContainerSlot());
    }

    @Override
    public void set(ItemStack stack) {
        ((IItemHandlerModifiable) this.itemHandler).setStackInSlot(this.getContainerSlot(), stack);
        this.setChanged();
    }

    @Override
    public void initialize(ItemStack stack) {
        ((IItemHandlerModifiable) this.itemHandler).setStackInSlot(this.getContainerSlot(), stack);
        this.setChanged();
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public ItemStack remove(int amount) {
        if (this.hasItem()) {
            this.removeCount += Math.min(amount, this.getItem().getCount());
        }

        return this.itemHandler.extractItem(this.getContainerSlot(), amount, false);
    }

    @Override
    public boolean hasItem() {
        return !this.itemHandler.getStackInSlot(this.getContainerSlot()).isEmpty();
    }

    @Override
    public void onQuickCraft(ItemStack pOldStack, ItemStack pNewStack) {
        super.onQuickCraft(pOldStack, pNewStack);
    }

    @Override
    protected void onQuickCraft(ItemStack pStack, int pAmount) {
        this.removeCount += pAmount;
        this.checkTakeAchievements(pStack);
    }

    @Override
    protected void onSwapCraft(int pNumItemsCrafted) {
        this.removeCount += pNumItemsCrafted;
    }

    @Override
    public boolean mayPickup(Player playerIn) {
        return !this.itemHandler.extractItem(this.getContainerSlot(), 1, true).isEmpty();
    }

    @Override
    protected void checkTakeAchievements(ItemStack pStack) {
        if (this.removeCount > 0) {
            pStack.onCraftedBy(this.player.level, this.player, this.removeCount);
            ForgeEventFactory.firePlayerCraftingEvent(this.player, pStack, this.blockEntity);
        }

        this.removeCount = 0;
    }

    // TODO: I took this from vanilla lol clean it up
    @Override
    public void onTake(Player pPlayer, ItemStack pStack) {
        if (blockEntity.getLevel() == null) return;

        this.checkTakeAchievements(pStack);
        ForgeHooks.setCraftingPlayer(pPlayer);
        NonNullList<ItemStack> remainingItems = getRemainingItemsForRecipe(this.blockEntity, (IItemHandlerModifiable) this.itemHandler, blockEntity.getLevel());
        ForgeHooks.setCraftingPlayer(null);

        for (int i : ArrayUtils.add(MultiblockWorkshopBlockEntity.SHAPELESS_SLOTS.toIntArray(), EMPTY_SCHEM_SLOT)) {
            int j = i - 9;

            ItemStack itemstack = this.itemHandler.getStackInSlot(i);
            ItemStack itemstack1 = remainingItems.get(j);

            if (!itemstack.isEmpty()) {
                this.itemHandler.extractItem(i, 1, false);
                itemstack = this.itemHandler.getStackInSlot(i);
            }

            if (itemstack1.isEmpty()) continue;

            if (itemstack.isEmpty()) {
                ((IItemHandlerModifiable) this.itemHandler).setStackInSlot(i, itemstack1);
            } else if (ItemStack.isSame(itemstack, itemstack1) && ItemStack.tagMatches(itemstack, itemstack1)) {
                itemstack1.grow(itemstack.getCount());
                ((IItemHandlerModifiable) this.itemHandler).setStackInSlot(i, itemstack1);
            } else if (!this.player.getInventory().add(itemstack1)) {
                this.player.drop(itemstack1, false);
            }
        }
    }

    @Override
    public int getMaxStackSize() {
        return this.itemHandler.getSlotLimit(this.getContainerSlot());
    }

    protected static NonNullList<ItemStack> getRemainingItemsForRecipe(MultiblockWorkshopBlockEntity entity, IItemHandlerModifiable handler, Level pLevel) {
        Optional<SchematicRecipe> optional = pLevel.getRecipeManager().getRecipeFor(SchematicRecipe.Type.INSTANCE, entity, pLevel);

        if (optional.isPresent()) {
            return optional.get().getRemainingItems(entity);
        }

        NonNullList<ItemStack> nonnulllist = NonNullList.withSize(entity.getContainerSize(), ItemStack.EMPTY);

        for(int i : ArrayUtils.add(SHAPELESS_SLOTS.toIntArray(), EMPTY_SCHEM_SLOT)) {
            nonnulllist.set(i - 9, handler.getStackInSlot(i));
        }

        return nonnulllist;
    }
}
