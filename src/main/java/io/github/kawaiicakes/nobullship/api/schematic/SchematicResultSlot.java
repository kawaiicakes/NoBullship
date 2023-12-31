package io.github.kawaiicakes.nobullship.api.schematic;

import io.github.kawaiicakes.nobullship.multiblock.block.MultiblockWorkshopBlockEntity;
import io.github.kawaiicakes.nobullship.schematic.SchematicRecipe;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

import static io.github.kawaiicakes.nobullship.multiblock.block.MultiblockWorkshopBlockEntity.EMPTY_SCHEM_SLOT;
import static io.github.kawaiicakes.nobullship.multiblock.block.MultiblockWorkshopBlockEntity.SHAPELESS_SLOTS;
import static io.github.kawaiicakes.nobullship.schematic.SchematicRecipe.getSummedContents;
import static net.minecraft.sounds.SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT;
import static net.minecraft.sounds.SoundSource.PLAYERS;

/**
 * <code>Slot</code> implementation designed to work with an <code>ItemStackHandler</code> but functioning like
 * the vanilla crafting <code>ResultSlot</code>. Simply access the item handler of whatever is associated with
 * this slot and set the item in the handler at the same index as the <code>int</code> slot passed in the
 * constructor.
 */
public class SchematicResultSlot extends Slot {
    protected static Container EMPTY = new SimpleContainer(0);
    protected final MultiblockWorkshopBlockEntity blockEntity;
    protected final IItemHandler itemHandler;
    protected Player player;
    protected int removeCount;

    public SchematicResultSlot(MultiblockWorkshopBlockEntity blockEntity, IItemHandler handler, Player player, int slot, int xPosition, int yPosition) {
        super(EMPTY, slot, xPosition, yPosition);
        this.blockEntity = blockEntity;
        this.itemHandler = handler;
        this.player = player;
    }

    @Override
    public ItemStack getItem() {
        return this.itemHandler.getStackInSlot(this.getContainerSlot());
    }

    @Override
    public void set(ItemStack stack) {
        ((IItemHandlerModifiable) this.itemHandler).setStackInSlot(this.getContainerSlot(), stack);
        this.setChanged();
    }

    @Override
    public void setChanged() {
        this.blockEntity.setChanged();
    }

    @Override
    public void initialize(ItemStack stack) {
        ((IItemHandlerModifiable) this.itemHandler).setStackInSlot(this.getContainerSlot(), stack);
        this.setChanged();
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public ItemStack remove(int amount) {
        if (this.hasItem()) {
            this.removeCount += Math.min(amount, this.getItem().getCount());
        }

        return this.itemHandler.extractItem(this.getContainerSlot(), amount, false);
    }

    @Override
    public boolean hasItem() {
        return !this.itemHandler.getStackInSlot(this.getContainerSlot()).isEmpty();
    }

    @Override
    public void onQuickCraft(ItemStack pOldStack, ItemStack pNewStack) {
        super.onQuickCraft(pOldStack, pNewStack);
    }

    @Override
    protected void onQuickCraft(ItemStack pStack, int pAmount) {
        this.removeCount += pAmount;
        this.checkTakeAchievements(pStack);
    }

    @Override
    protected void onSwapCraft(int pNumItemsCrafted) {
        this.removeCount += pNumItemsCrafted;
    }

    @Override
    public boolean mayPickup(Player playerIn) {
        return !this.itemHandler.extractItem(this.getContainerSlot(), 1, true).isEmpty();
    }

    @Override
    protected void checkTakeAchievements(ItemStack pStack) {
        if (this.removeCount > 0) {
            pStack.onCraftedBy(this.player.level, this.player, this.removeCount);
            ForgeEventFactory.firePlayerCraftingEvent(this.player, pStack, this.blockEntity);
        }

        this.removeCount = 0;
    }

    @Override
    public void onTake(Player pPlayer, ItemStack pStack) {
        if (blockEntity.getLevel() == null) return;

        this.checkTakeAchievements(pStack);

        Optional<SchematicRecipe> optional = getCurrentRecipe(this.blockEntity.getLevel(), this.blockEntity);
        SchematicRecipe recipe = optional.orElse(null);
        if (recipe == null) throw new IllegalStateException("Attempted to take a crafted schematic when no recipe exists!");

        ForgeHooks.setCraftingPlayer(pPlayer);
        List<ItemStack> remainingItems
                = getRemainingItemsForRecipe(recipe, this.blockEntity, (IItemHandlerModifiable) this.itemHandler);
        ForgeHooks.setCraftingPlayer(null);

        List<ItemStack> requiredItemMap
                = getSummedContents(recipe.getShapelessIngredients());

        for (int i : ArrayUtils.add(MultiblockWorkshopBlockEntity.SHAPELESS_SLOTS.toIntArray(), EMPTY_SCHEM_SLOT)) {
            ItemStack stackInSlot = this.itemHandler.getStackInSlot(i);
            ItemStack craftingRemaining = remainingItems.get(i - 9);

            final ItemStack finalItemstack = stackInSlot;
            ItemStack requiredItemRemaining = requiredItemMap.stream()
                    .filter(standard -> ItemStack.isSameItemSameTags(standard, finalItemstack))
                    .findFirst()
                    .orElse(null);

            if (SHAPELESS_SLOTS.contains(i) && requiredItemRemaining == null) continue;

            int decrement = 1;
            if (requiredItemRemaining != null) decrement
                    = Math.min(stackInSlot.getCount(), requiredItemRemaining.getCount());

            if (!stackInSlot.isEmpty()) {
                this.itemHandler.extractItem(i, decrement, false);
                stackInSlot = this.itemHandler.getStackInSlot(i);

                if (requiredItemRemaining != null) requiredItemRemaining.shrink(decrement);
            }

            if (craftingRemaining.isEmpty()) continue;

            if (stackInSlot.isEmpty()) {
                ((IItemHandlerModifiable) this.itemHandler).setStackInSlot(i, craftingRemaining);
            } else if (ItemStack.isSame(stackInSlot, craftingRemaining) && ItemStack.tagMatches(stackInSlot, craftingRemaining)) {
                craftingRemaining.grow(stackInSlot.getCount());
                ((IItemHandlerModifiable) this.itemHandler).setStackInSlot(i, craftingRemaining);
            } else if (!this.player.getInventory().add(craftingRemaining)) {
                this.player.drop(craftingRemaining, false);
            }
        }

        this.blockEntity.getLevel().playSound(pPlayer, this.blockEntity.getBlockPos(), UI_CARTOGRAPHY_TABLE_TAKE_RESULT, PLAYERS, 1.0F, 0.9F);
    }

    @Override
    public int getMaxStackSize() {
        return this.itemHandler.getSlotLimit(this.getContainerSlot());
    }

    protected static NonNullList<ItemStack> getRemainingItemsForRecipe(@Nullable SchematicRecipe recipe, MultiblockWorkshopBlockEntity entity, IItemHandlerModifiable handler) {
        if (recipe != null) {
            return recipe.getRemainingItems(entity);
        }

        NonNullList<ItemStack> nonnulllist = NonNullList.withSize(entity.getContainerSize(), ItemStack.EMPTY);

        for(int i : ArrayUtils.add(SHAPELESS_SLOTS.toIntArray(), EMPTY_SCHEM_SLOT)) {
            nonnulllist.set(i - 9, handler.getStackInSlot(i));
        }

        return nonnulllist;
    }

    protected Optional<SchematicRecipe> getCurrentRecipe(Level pLevel, MultiblockWorkshopBlockEntity entity) {
        return pLevel.getRecipeManager().getRecipeFor(SchematicRecipe.Type.INSTANCE, entity, pLevel);
    }
}
