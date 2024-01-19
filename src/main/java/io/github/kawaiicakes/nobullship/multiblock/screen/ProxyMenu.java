package io.github.kawaiicakes.nobullship.multiblock.screen;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

// TODO
public class ProxyMenu extends AbstractContainerMenu {
    protected final AbstractContainerMenu originalMenu;

    public ProxyMenu(AbstractContainerMenu originalMenu, int pContainerId, Inventory inventory) {
        super(originalMenu.getType(), pContainerId);
        this.originalMenu = originalMenu;
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return true;
    }
}
