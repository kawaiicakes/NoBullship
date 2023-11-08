package io.github.kawaiicakes.nobullship.screen;

import io.github.kawaiicakes.nobullship.block.MultiblockWorkshopBlockEntity;
import io.github.kawaiicakes.nobullship.data.SchematicRecipe;
import io.github.kawaiicakes.nobullship.data.SchematicResultSlot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.SlotItemHandler;

import java.util.Objects;
import java.util.Optional;

import static io.github.kawaiicakes.nobullship.NoBullship.WORKSHOP_MENU;
import static io.github.kawaiicakes.nobullship.block.MultiblockWorkshopBlockEntity.EMPTY_SCHEM_SLOT;
import static io.github.kawaiicakes.nobullship.block.MultiblockWorkshopBlockEntity.FILLED_SCHEM_SLOT;

public class MultiblockWorkshopMenu extends AbstractContainerMenu implements ContainerListener {
    public final MultiblockWorkshopBlockEntity entity;
    protected Player player;
    protected int filledSchemSlotID;

    public MultiblockWorkshopMenu(int pContainerId, Inventory inventory, FriendlyByteBuf data) {
        this(pContainerId, inventory, Objects.requireNonNull(inventory.player.level.getBlockEntity(data.readBlockPos())));
    }

    public MultiblockWorkshopMenu(int pContainerId, Inventory inventory, BlockEntity entity) {
        super(WORKSHOP_MENU.get(), pContainerId);
        checkContainerSize(inventory, 20);
        this.entity = (MultiblockWorkshopBlockEntity) entity;
        this.player = inventory.player;

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

            this.addSlot(new SlotItemHandler(handler, EMPTY_SCHEM_SLOT, 169, 48));
            this.filledSchemSlotID = this.addSlot(new SchematicResultSlot(this.entity, handler, this.player, FILLED_SCHEM_SLOT, 169, 26)).index;
        });

        this.addSlotListener(this);
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

    @Override
    public void slotChanged(AbstractContainerMenu pContainerToSend, int slot, ItemStack pStack) {
        if (pContainerToSend != this) return;

        this.slotsChanged(this.entity);
    }

    @Override
    public void slotsChanged(Container pContainer) {
        if (pContainer != this.entity) return;
        final MultiblockWorkshopBlockEntity pEntity = ((MultiblockWorkshopBlockEntity) pContainer);
        if (!(pEntity.getLevel() instanceof ServerLevel serverLevel)) return;

        ItemStack output = ItemStack.EMPTY;
        Optional<SchematicRecipe> optional = serverLevel.getServer().getRecipeManager().getRecipeFor(SchematicRecipe.Type.INSTANCE, pEntity, serverLevel);
        if (optional.isPresent()) {
            output = optional.get().assemble(pEntity);
        }

        this.slots.get(this.filledSchemSlotID).set(output);
    }

    @Override
    public void dataChanged(AbstractContainerMenu pContainerMenu, int pDataSlotIndex, int pValue) {}
}
