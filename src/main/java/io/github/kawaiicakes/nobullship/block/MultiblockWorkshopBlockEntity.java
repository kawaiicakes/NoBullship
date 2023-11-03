package io.github.kawaiicakes.nobullship.block;

import io.github.kawaiicakes.nobullship.screen.MultiblockWorkshopMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

import static io.github.kawaiicakes.nobullship.NoBullship.WORKSHOP_BLOCK_ENTITY;

public class MultiblockWorkshopBlockEntity extends BlockEntity implements MenuProvider, Container {
    protected final NonNullList<ItemStack> contents = NonNullList.withSize(9, ItemStack.EMPTY);

    public MultiblockWorkshopBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(WORKSHOP_BLOCK_ENTITY.get(), pPos, pBlockState);
    }

    @Override
    public int getContainerSize() {
        return this.contents.size();
    }

    @Override
    public boolean isEmpty() {
        return this.contents.isEmpty();
    }

    @Override
    public ItemStack getItem(int pSlot) {
        return this.contents.get(pSlot);
    }

    @Override
    public ItemStack removeItem(int pSlot, int pAmount) {
        int difference = this.contents.get(pSlot).getCount() - pAmount;

        this.contents.get(pSlot).setCount(Mth.clamp(difference, 0, this.contents.get(pSlot).getCount()));
        return this.contents.get(pSlot);
    }

    @Override
    public ItemStack removeItemNoUpdate(int pSlot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int pSlot, ItemStack pStack) {
        this.contents.set(pSlot, pStack);
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return this.getBlockPos().closerThan(pPlayer.blockPosition(), 8.0D);
    }

    @Override
    public void clearContent() {
        Collections.fill(this.contents, ItemStack.EMPTY);
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
}
