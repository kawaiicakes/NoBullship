package io.github.kawaiicakes.nobullship.data;

import io.github.kawaiicakes.nobullship.block.MultiblockWorkshopBlockEntity;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SchematicResultSlot extends Slot {
    protected static ResultContainer resultHolder = new ResultContainer();
    protected final MultiblockWorkshopBlockEntity blockEntity;
    protected Player player;
    protected final int index;
    protected int removeCount;

    public SchematicResultSlot(MultiblockWorkshopBlockEntity blockEntity, Player player, int index, int xPosition, int yPosition) {
        super(resultHolder, index, xPosition, yPosition);
        this.blockEntity = blockEntity;
        this.player = player;
        this.index = index;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public ItemStack remove(int pAmount) {
        if (this.hasItem()) {
            this.removeCount += Math.min(pAmount, this.getItem().getCount());
        }

        return super.remove(pAmount);
    }

    @Override
    protected void onQuickCraft(ItemStack pStack, int pAmount) {
        this.removeCount += pAmount;
    }

    @Override
    protected void onSwapCraft(int pNumItemsCrafted) {
        this.removeCount += pNumItemsCrafted;
    }

    @Override
    public void onTake(Player pPlayer, ItemStack pStack) {
        NonNullList<ItemStack> nonnulllist = pPlayer.level.getRecipeManager().getRemainingItemsFor(SchematicRecipe.Type.INSTANCE, this.blockEntity, pPlayer.level);
        for(int i = 0; i < nonnulllist.size(); ++i) {
            ItemStack itemstack = this.blockEntity.getItem(i);
            ItemStack itemstack1 = nonnulllist.get(i);
            if (!itemstack.isEmpty()) {
                this.blockEntity.removeItem(i, 1);
                itemstack = this.blockEntity.getItem(i);
            }

            if (!itemstack1.isEmpty()) {
                if (itemstack.isEmpty()) {
                    this.blockEntity.setItem(i, itemstack1);
                } else if (ItemStack.isSame(itemstack, itemstack1) && ItemStack.tagMatches(itemstack, itemstack1)) {
                    itemstack1.grow(itemstack.getCount());
                    this.blockEntity.setItem(i, itemstack1);
                } else if (!this.player.getInventory().add(itemstack1)) {
                    this.player.drop(itemstack1, false);
                }
            }
        }
    }

    @Override
    public int getMaxStackSize() {
        return this.getItem().getMaxStackSize();
    }
}
