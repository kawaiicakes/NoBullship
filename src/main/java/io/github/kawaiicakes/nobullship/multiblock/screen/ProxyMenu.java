package io.github.kawaiicakes.nobullship.multiblock.screen;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import static io.github.kawaiicakes.nobullship.Registry.PROXY_MENU;

public class ProxyMenu extends AbstractContainerMenu {
    public ProxyMenu(int pContainerId, Inventory inventory, FriendlyByteBuf data) {
        this(pContainerId, inventory);
    }

    public ProxyMenu(int pContainerId, Inventory inventory) {
        super(PROXY_MENU.get(), pContainerId);

        this.addPlayerHotbar(inventory);
        this.addPlayerInventory(inventory);
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return true;
    }

    protected void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, (i * 18) + 110));
            }
        }
    }

    protected void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 168));
        }
    }
}
