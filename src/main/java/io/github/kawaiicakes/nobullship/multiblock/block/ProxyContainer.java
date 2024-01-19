package io.github.kawaiicakes.nobullship.multiblock.block;

import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.nobullship.multiblock.screen.ProxyMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

import java.lang.ref.WeakReference;

public class ProxyContainer<T extends BlockEntity> extends BaseContainerBlockEntity {
    protected static final Logger LOGGER = LogUtils.getLogger();
    protected final WeakReference<T> originalContainer;

    public ProxyContainer(T originalContainer, BlockPos pPos, BlockState pBlockState) {
        super(originalContainer.getType(), pPos, pBlockState);
        this.originalContainer = new WeakReference<>(originalContainer);
    }

    @Override
    protected Component getDefaultName() {
        return this.originalContainer.get() instanceof Nameable nameable ?
                nameable.getDisplayName() :
                Component.translatable("gui.nobullship.empty_block_entity");
    }

    @Override
    protected AbstractContainerMenu createMenu(int pContainerId, Inventory pInventory) {
        T t = this.originalContainer.get();

        try {
            if (t instanceof MenuProvider menuProvider) {
                AbstractContainerMenu menu = menuProvider.createMenu(pContainerId, pInventory, pInventory.player);
                return menu != null ? menu : new ProxyMenu(pContainerId, pInventory);
            } else {
                return new ProxyMenu(pContainerId, pInventory);
            }
        } catch (RuntimeException e) {
            LOGGER.error("Error attempting to create menu for proxy container!", e);
            return new ProxyMenu(pContainerId, pInventory);
        }
    }

    @Override
    public int getContainerSize() {
        return this.originalContainer.get() instanceof BaseContainerBlockEntity base ?
                base.getContainerSize() : 0;
    }

    @Override
    public boolean isEmpty() {
        if (this.originalContainer.get() instanceof Container container) return container.isEmpty();
        return true;
    }

    @Override
    public ItemStack getItem(int pSlot) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int pSlot, int pAmount) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int pSlot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int pSlot, ItemStack pStack) {}

    @Override
    public boolean stillValid(Player pPlayer) {
        return this.originalContainer.get() != null;
    }

    @Override
    public void clearContent() {}
}
