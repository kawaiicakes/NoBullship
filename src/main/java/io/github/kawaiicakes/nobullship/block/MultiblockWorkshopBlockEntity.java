package io.github.kawaiicakes.nobullship.block;

import io.github.kawaiicakes.nobullship.screen.MultiblockWorkshopMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

import static io.github.kawaiicakes.nobullship.NoBullship.WORKSHOP_BLOCK_ENTITY;

public class MultiblockWorkshopBlockEntity extends BlockEntity implements MenuProvider, Container, StackedContentsCompatible {
    protected final NonNullList<ItemStack> contents = NonNullList.withSize(9, ItemStack.EMPTY);
    protected MultiblockWorkshopMenu menu;

    public MultiblockWorkshopBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(WORKSHOP_BLOCK_ENTITY.get(), pPos, pBlockState);
    }

    @Override
    public int getContainerSize() {
        return this.contents.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack item : this.contents) {
            if (!item.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int pSlot) {
        return pSlot >= this.getContainerSize() ? ItemStack.EMPTY : this.contents.get(pSlot);
    }

    @Override
    public ItemStack removeItem(int pSlot, int pAmount) {
        ItemStack itemstack = ContainerHelper.removeItem(this.contents, pSlot, pAmount);
        if (!itemstack.isEmpty()) {
            this.menu.slotsChanged(this);
        }

        return itemstack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int pSlot) {
        return ContainerHelper.takeItem(this.contents, pSlot);
    }

    @Override
    public void setItem(int pSlot, ItemStack pStack) {
        this.contents.set(pSlot, pStack);
        this.menu.slotsChanged(this);
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return this.getBlockPos().closerThan(pPlayer.blockPosition(), 8.0D);
    }

    @Override
    public void clearContent() {
        this.contents.clear();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.nobullship.workshop");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new MultiblockWorkshopMenu(pContainerId, pPlayerInventory, this);
    }

    @Override
    public void fillStackedContents(StackedContents pHelper) {
        for(ItemStack itemstack : this.contents) {
            pHelper.accountSimpleStack(itemstack);
        }
    }
}
