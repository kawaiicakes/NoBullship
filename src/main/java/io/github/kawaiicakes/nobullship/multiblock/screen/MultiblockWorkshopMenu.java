package io.github.kawaiicakes.nobullship.multiblock.screen;

import com.google.common.collect.ImmutableList;
import io.github.kawaiicakes.nobullship.api.MultiblockRecipeManager;
import io.github.kawaiicakes.nobullship.api.multiblock.MultiblockRecipe;
import io.github.kawaiicakes.nobullship.api.schematic.SchematicResultSlot;
import io.github.kawaiicakes.nobullship.multiblock.block.MultiblockWorkshopBlockEntity;
import io.github.kawaiicakes.nobullship.schematic.SchematicRecipe;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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

import static io.github.kawaiicakes.nobullship.Registry.SCHEMATIC;
import static io.github.kawaiicakes.nobullship.Registry.WORKSHOP_MENU;
import static io.github.kawaiicakes.nobullship.multiblock.block.MultiblockWorkshopBlockEntity.EMPTY_SCHEM_SLOT;
import static io.github.kawaiicakes.nobullship.multiblock.block.MultiblockWorkshopBlockEntity.FILLED_SCHEM_SLOT;

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
                    this.addSlot(new SlotItemHandler(handler, j + i * 3, 21 + j * 18, 17 + i * 18));
                }
            }

            for (int i = 0; i < 9; ++i) {
                this.addSlot(new SlotItemHandler(handler, i + 9, 21 + i * 18, 89));
            }

            this.addSlot(new SlotItemHandler(handler, EMPTY_SCHEM_SLOT, 93, 35));
            this.filledSchemSlotID = this.addSlot(new SchematicResultSlot(this.entity, handler, this.player, FILLED_SCHEM_SLOT, 93, 13)).index;
        });

        this.addSlotListener(this);
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        Slot slot = this.slots.get(pIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stackInSlot = slot.getItem();
        ItemStack toReturn = stackInSlot.copy();

        if (pIndex == this.filledSchemSlotID) {
            if (!this.moveItemStackTo(stackInSlot, 0, 36, true)) {
                return ItemStack.EMPTY;
            }

            slot.onQuickCraft(stackInSlot, toReturn);
        } else if (pIndex >= 0 && pIndex <= 35) {
            if (this.validEmptySchematic(stackInSlot)) {
                if (!this.moveItemStackTo(stackInSlot, this.filledSchemSlotID - 1, this.filledSchemSlotID, false))
                    if (!this.moveItemStackTo(stackInSlot, 45, this.filledSchemSlotID, false))
                        return ItemStack.EMPTY;
            }

            if (!this.moveItemStackTo(stackInSlot, 45, this.filledSchemSlotID, false)) {

                if (pIndex < 26) {
                    if (!this.moveItemStackTo(stackInSlot, 26, 36, false))
                        return ItemStack.EMPTY;
                }

                if (!this.moveItemStackTo(stackInSlot, 0, 27, false))
                    return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stackInSlot, 0, 36, false)) {
            return ItemStack.EMPTY;
        }

        if (stackInSlot.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (stackInSlot.getCount() == toReturn.getCount()) return ItemStack.EMPTY;

        slot.onTake(pPlayer, stackInSlot);

        this.slotsChanged(this.entity);
        if (pIndex == this.filledSchemSlotID) {
            pPlayer.drop(stackInSlot, false);
        }

        return toReturn;
    }

    public boolean validEmptySchematic(ItemStack stack) {
        if (!stack.is(SCHEMATIC.get())) return false;
        if (!stack.hasTag()) return true;
        //noinspection DataFlowIssue
        return !stack.getTag().contains("nobullshipRecipe", Tag.TAG_STRING);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack pStack, Slot pSlot) {
        return pSlot.getSlotIndex() != this.filledSchemSlotID;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return this.entity.stillValid(pPlayer);
    }

    protected void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 21 + l * 18, (i * 18) + 124));
            }
        }
    }

    protected void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 21 + i * 18, 182));
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
        Optional<SchematicRecipe> optional = serverLevel.getRecipeManager().getRecipeFor(SchematicRecipe.Type.INSTANCE, pEntity, serverLevel);

        if (optional.isEmpty()) {
            this.setResult(output);
            return;
        }

        output = optional.get().assemble(pEntity);
        Optional<MultiblockRecipe> multiblockRecipeOptional
                = MultiblockRecipeManager.getInstance().getRecipe(optional.get().getResultId());

        ListTag requisiteList = new ListTag();

        if (multiblockRecipeOptional.isPresent() && multiblockRecipeOptional.get().requisites() != null) {
            //noinspection DataFlowIssue
            for (ItemStack item : multiblockRecipeOptional.get().requisites()) {
                requisiteList.add(item.serializeNBT());
            }
        }

        ImmutableList<ItemStack> requisiteListFromSchematic = optional.get().getRequisites();
        if (requisiteListFromSchematic != null) {
            for (ItemStack item : requisiteListFromSchematic) {
                requisiteList.add(item.serializeNBT());
            }
        }

        //noinspection DataFlowIssue (output is guaranteed to have a tag here since a recipe match is present)
        output.getTag().put("nobullshipRequisites", requisiteList);

        this.entity.setActiveRecipe(optional.orElse(null));
        this.setResult(output);
    }

    public void setResult(ItemStack result) {
        this.entity.itemHandler.setStackInSlot(FILLED_SCHEM_SLOT, result);
    }

    @Override
    public void dataChanged(AbstractContainerMenu pContainerMenu, int pDataSlotIndex, int pValue) {}
}
