package io.github.kawaiicakes.nobullship.schematic;

import com.google.common.collect.ImmutableList;
import io.github.kawaiicakes.nobullship.Config;
import io.github.kawaiicakes.nobullship.api.MultiblockRecipeManager;
import io.github.kawaiicakes.nobullship.api.multiblock.MultiblockRecipe;
import io.github.kawaiicakes.nobullship.multiblock.MultiblockPattern;
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
import static io.github.kawaiicakes.nobullship.Registry.SCHEMATIC;
import static io.github.kawaiicakes.nobullship.schematic.SchematicRecipe.getSummedContents;
import static net.minecraft.ChatFormatting.*;
import static net.minecraft.nbt.Tag.*;

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
    public static final MutableComponent REMAINING_USES = Component.translatable("tooltip.nobullship.remaining_uses").withStyle(ITALIC);

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
        if (pStack.getTag() != null && pStack.getTag().contains("nobullshipUses", TAG_INT))
            pTooltipComponents.add(REMAINING_USES.copy().append(" " + pStack.getTag().getInt("nobullshipUses")));
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
            String requisiteCharacter = "[✔] ";
            if (playerContents.stream().noneMatch(content ->
                ItemStack.isSameItemSameTags(content, requirement) && content.getCount() >= requirement.getCount()
            )) {
                colour = RED;
                requisiteCharacter = "[✘] ";
            }

            toAdd.add(Component.literal(requisiteCharacter).withStyle(Style.EMPTY.withColor(colour)).append(fromStack(requirement)));
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
        // Cooldown is added so spamming this isn't possible. BlockPattern#find is an expensive call.
        if (pContext.getPlayer() == null) return InteractionResult.FAIL;
        if (pContext.getPlayer().getCooldowns().isOnCooldown(this)) return InteractionResult.FAIL;

        if (pContext.getHitResult().getType() != HitResult.Type.BLOCK) return InteractionResult.FAIL;
        if (pContext.getLevel().isClientSide) return InteractionResult.SUCCESS;

        CompoundTag nbt = stack.getTag();
        if (nbt == null) return InteractionResult.FAIL;
        String noBsRecipe = nbt.getString("nobullshipRecipe");
        if (noBsRecipe.isEmpty()) return InteractionResult.FAIL;

        MultiblockRecipeManager manager = MultiblockRecipeManager.getInstance();
        MultiblockRecipe recipe = manager.getRecipe(new ResourceLocation(noBsRecipe)).orElse(null);

        MultiblockPattern pattern = null;
        if (recipe != null) pattern = recipe.recipe();

        // arbitrary default recipe size.
        int recipeSize = 9;
        if (pattern != null) recipeSize = pattern.getDepth() * pattern.getHeight() * pattern.getWidth();
        int cooldownTimeTicks = Math.min(
                Math.max((int) (recipeSize * Config.COOLDOWN_MULTIPLIER.get()), 20 * (Config.MINIMUM_COOLDOWN.get()).intValue()),
                20 * Config.MAXIMUM_COOLDOWN.get().intValue()
        );
        pContext.getPlayer().getCooldowns().addCooldown(this, cooldownTimeTicks);
        manager.incrementGlobalCooldown(cooldownTimeTicks);

        try {
            if (!manager.trySpawn(recipe, pContext)) return InteractionResult.FAIL;
        } catch (RuntimeException e) {
            return InteractionResult.FAIL;
        }

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

    /**
     * @param schematic The <code>ItemStack</code> of a SchematicItem with valid requisite data
     * @return an immutable list containing the summed requisites
     */
    public static ImmutableList<ItemStack> yoinkSummedRequisites(ItemStack schematic) throws IllegalArgumentException {
        if (!schematic.is(SCHEMATIC.get())) throw new IllegalArgumentException("Argument is not a SchematicItem!");
        if (!schematic.hasTag()) throw new IllegalArgumentException("Schematic does not have NBT!");

        assert schematic.getTag() != null;
        ListTag list = schematic.getTag().getList("nobullshipRequisites", TAG_COMPOUND).copy();

        List<ItemStack> nonSumRequirements = list.stream()
                .map(tag -> (CompoundTag) tag)
                .map(ItemStack::of)
                .toList();

        return ImmutableList.copyOf(getSummedContents(nonSumRequirements));
    }
}
