package io.github.kawaiicakes.nobullship.screen;

import io.github.kawaiicakes.nobullship.block.MultiblockWorkshopBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Objects;

import static io.github.kawaiicakes.nobullship.NoBullship.WORKSHOP_BLOCK;
import static io.github.kawaiicakes.nobullship.NoBullship.WORKSHOP_MENU;

public class MultiblockWorkshopMenu extends AbstractContainerMenu {
    public final MultiblockWorkshopBlockEntity entity;
    private final Level level;

    public MultiblockWorkshopMenu(int pContainerId, Inventory inventory, FriendlyByteBuf data) {
        this(pContainerId, inventory, (MultiblockWorkshopBlockEntity) Objects.requireNonNull(inventory.player.level.getBlockEntity(data.readBlockPos())));
    }

    public MultiblockWorkshopMenu(int pContainerId, Inventory inventory, MultiblockWorkshopBlockEntity entity) {
        super(WORKSHOP_MENU.get(), pContainerId);
        this.entity = entity;
        this.level = inventory.player.getLevel();

        this.addPlayerInventory(inventory);
        this.addPlayerHotbar(inventory);

        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(entity, i, 97 + i * 18, 70));
        }
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack pStack, Slot pSlot) {
        return pSlot.container != this.entity && super.canTakeItemForPickAll(pStack, pSlot);
    }

    @Override
    public void removed(Player pPlayer) {
        super.removed(pPlayer);
        this.clearContainer(pPlayer, pPlayer.getInventory());
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        return pPlayer.inventoryMenu.quickMoveStack(pPlayer, pIndex);
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return stillValid(ContainerLevelAccess.create(level, this.entity.getBlockPos()), pPlayer, WORKSHOP_BLOCK.get());
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 97 + l * 18, 86 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 97 + i * 18, 195));
        }
    }
}
