package io.github.kawaiicakes.nobullship.schematic;

import io.github.kawaiicakes.nobullship.api.MultiblockRecipeManager;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

import static io.github.kawaiicakes.nobullship.NoBullship.NO_BULLSHIP_TAB;
import static io.github.kawaiicakes.nobullship.NoBullship.SCHEMATIC;
import static io.github.kawaiicakes.nobullship.schematic.SchematicRecipe.getSummedContents;
import static net.minecraft.ChatFormatting.*;
import static net.minecraft.nbt.Tag.TAG_COMPOUND;
import static net.minecraft.nbt.Tag.TAG_LIST;

/**
 * Care is taken to ensure that using the schematic does not alter the blockstate of the clicked block.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SchematicItem extends Item {
    public static final MutableComponent DIVIDER = Component.literal("---------").withStyle(BOLD);
    public static final Style GOLD_STANDARD = Style.EMPTY.withColor(GOLD);
    public static final MutableComponent ITEM_LIST = Component.translatable("tooltip.nobullship.items").withStyle(ITALIC);
    public static final MutableComponent BLANK = Component.literal("");
    public static final MutableComponent BLANK_SCHEMATIC = Component.translatable("tooltip.nobullship.blank_schematic").withStyle(GOLD_STANDARD);

    public SchematicItem() {
        super(new Properties().tab(NO_BULLSHIP_TAB));
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack toReturn = super.getDefaultInstance();

        if (toReturn.hasTag()) return toReturn;

        toReturn.setTag(new CompoundTag());
        return toReturn;
    }

    @Override
    public Component getName(ItemStack pStack) {
        MutableComponent toReturn = Component.translatable(this.getDescriptionId(pStack));
        return pStack.getTag() != null && !pStack.getTag().getString("nobullshipRecipe").isEmpty()
                ? toReturn.withStyle(AQUA) : toReturn;
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        if (pStack.getTag() == null || pStack.getTag().getString("nobullshipRecipe").isEmpty()) {
            pTooltipComponents.add(DIVIDER);
            pTooltipComponents.add(BLANK_SCHEMATIC);
            return;
        }

        if (pStack.getTag() != null && pStack.getTag().contains("nobullshipRequisites", TAG_LIST)) {
            pTooltipComponents.add(DIVIDER);
            pTooltipComponents.add(ITEM_LIST);
        }

        pTooltipComponents.add(DIVIDER);
        pTooltipComponents.add(Component.translatable(
                "tooltip.nobullship.filled_schematic",
                pStack.getTag().getString("nobullshipRecipe"))
                .withStyle(GOLD_STANDARD));
    }

    @SubscribeEvent
    public static void onRenderTooltip(ItemTooltipEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer player)) return;
        ItemStack stack = event.getItemStack();
        if (!stack.is(SCHEMATIC.get())) return;
        if (!event.getToolTip().contains(ITEM_LIST)) return;
        int insertionIndex = event.getToolTip().indexOf(ITEM_LIST) + 1;
        if (stack.getTag() == null || !stack.getTag().contains("nobullshipRequisites", TAG_LIST)) return;

        event.getToolTip().add(insertionIndex, BLANK);
        insertionIndex++;

        //noinspection DataFlowIssue
        ListTag list = event.getItemStack().getTag().getList("nobullshipRequisites", TAG_COMPOUND).copy();
        List<Component> toAdd = new ArrayList<>(list.size());

        List<ItemStack> nonSumRequirements = list.stream()
                .map(tag -> (CompoundTag) tag)
                .map(ItemStack::of)
                .toList();

        List<ItemStack> requirements = getSummedContents(nonSumRequirements);
        List<ItemStack> playerContents = getSummedContents(player.getInventory().items);

        for (ItemStack requirement : requirements) {
            ChatFormatting colour = GREEN;
            if (playerContents.stream().noneMatch(content ->
                ItemStack.isSameItemSameTags(content, requirement) && content.getCount() >= requirement.getCount()
            )) colour = RED;

            toAdd.add(fromStack(requirement).withStyle(Style.EMPTY.withColor(colour)));
        }

        event.getToolTip().addAll(insertionIndex, toAdd);
    }

    public static MutableComponent fromStack(ItemStack stack) {
        String itemName = stack.getDisplayName().getString();
        if (stack.getTag() != null) itemName = stack.getDisplayName().getString() + "(NBT)";

        return Component.literal(itemName + " ×" + stack.getCount());
    }

    @Override
    public String getDescriptionId(ItemStack pStack) {
        CompoundTag nbt = pStack.getTag();
        if (nbt == null) return this.getDescriptionId();

        if (!(nbt.getString("nobullshipRecipe").isEmpty())) return this.getDescriptionId() + ".filled";

        return this.getDescriptionId();
    }

    @Override
    public Component getHighlightTip(ItemStack item, Component displayName) {
        if (item.getTag() == null) return displayName;
        if (item.getTag().getString("nobullshipRecipe").isEmpty()) return displayName;

        MutableComponent returnComp = displayName.copy().append(" - " + item.getTag().getString("nobullshipRecipe"));
        return returnComp.withStyle(GOLD_STANDARD);
    }

    @Override
    public boolean isFoil(ItemStack pStack) {
        if (pStack.getTag() == null) return false;
        return !(pStack.getTag().getString("nobullshipRecipe").isEmpty());
    }

    // So long as a claim mod sets $useItem to DENY in RightClickBlock event, this will not bypass claim mods.
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext pContext) {
        // TODO: configurable options for cooldown time
        // Cooldown is added so spamming this isn't possible. BlockPattern#find is an expensive call.
        if (pContext.getPlayer() == null) return InteractionResult.FAIL;
        if (pContext.getPlayer().getCooldowns().isOnCooldown(this)) return InteractionResult.FAIL;


        if (pContext.getHitResult().getType() != HitResult.Type.BLOCK) return InteractionResult.FAIL;
        if (pContext.getLevel().isClientSide) return InteractionResult.SUCCESS;

        CompoundTag nbt = stack.getTag();
        if (nbt == null) return InteractionResult.FAIL;
        String noBsRecipe = nbt.getString("nobullshipRecipe");
        if (noBsRecipe.isEmpty()) return InteractionResult.FAIL;

        pContext.getPlayer().getCooldowns().addCooldown(this, 20);

        MultiblockRecipeManager.getInstance().trySpawn(new ResourceLocation(noBsRecipe), pContext);

        return InteractionResult.FAIL;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        if (pLevel.isClientSide) return InteractionResultHolder.success(pPlayer.getItemInHand(pUsedHand));
        return InteractionResultHolder.fail(pPlayer.getItemInHand(pUsedHand));
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        return InteractionResult.FAIL;
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        return true;
    }
}