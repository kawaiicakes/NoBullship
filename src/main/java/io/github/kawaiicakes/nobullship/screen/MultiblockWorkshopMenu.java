package io.github.kawaiicakes.nobullship.screen;

import io.github.kawaiicakes.nobullship.block.MultiblockWorkshopBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.SlotItemHandler;

import java.util.Objects;

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

        this.entity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            for(int i = 0; i < 3; ++i) {
                for(int j = 0; j < 3; ++j) {
                    this.addSlot(new SlotItemHandler(handler, j + i * 3, 97 + j * 18, 30 + i * 18));
                }
            }

            for (int i = 0; i < 9; ++i) {
                this.addSlot(new SlotItemHandler(handler, i + 9, 97 + i * 18, 102));
            }

            this.addSlot(new SlotItemHandler(handler, 18, 169, 48));
            this.addSlot(new SlotItemHandler(handler, 19, 169, 26));
        });
    }

    // TODO
    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        return pPlayer.inventoryMenu.quickMoveStack(pPlayer, pIndex);
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return this.entity.stillValid(pPlayer);
    }

    protected void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 97 + l * 18, (i * 18) + 137));
            }
        }
    }

    protected void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 97 + i * 18, 195));
        }
    }
}
