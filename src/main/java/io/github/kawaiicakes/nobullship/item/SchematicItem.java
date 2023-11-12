package io.github.kawaiicakes.nobullship.item;

import io.github.kawaiicakes.nobullship.datagen.MultiblockRecipeManager;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

import static io.github.kawaiicakes.nobullship.NoBullship.NO_BULLSHIP_TAB;
import static net.minecraft.ChatFormatting.*;

/**
 * Care is taken to ensure that using the schematic does not alter the blockstate of the clicked block.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SchematicItem extends Item {
    public SchematicItem() {
        super(new Properties().tab(NO_BULLSHIP_TAB));
    }

    @Override
    public Component getName(ItemStack pStack) {
        MutableComponent toReturn = Component.translatable(this.getDescriptionId(pStack));
        return pStack.getTag() != null && !pStack.getTag().getString("nobullshipRecipe").isEmpty()
                ? toReturn.withStyle(AQUA) : toReturn;
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        if (pStack.getTag() == null) super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
        final String recipeName = pStack.getTag().getString("nobullshipRecipe");
        if (recipeName.isEmpty()) {
            pTooltipComponents.add(Component.translatable("tooltip.nobullship.blank_schematic").withStyle(Style.EMPTY.withColor(GOLD)));
        }

        pTooltipComponents.add(Component.translatable("tooltip.nobullship.filled_schematic", recipeName).withStyle(Style.EMPTY.withColor(GOLD)));
    }

    @Override
    public String getDescriptionId(ItemStack pStack) {
        CompoundTag nbt = pStack.getTag();
        if (nbt == null) return this.getDescriptionId();

        if (!(nbt.getString("nobullshipRecipe").isEmpty())) return this.getDescriptionId() + ".filled";

        return this.getDescriptionId();
    }

    // FIXME: redo this
    @Override
    public Component getHighlightTip(ItemStack item, Component displayName) {
        if (item.getTag() == null) return displayName;
        if (item.getTag().getString("nobullshipRecipe").isEmpty()) return displayName;

        MutableComponent returnComp = displayName.copy().append(" - " + item.getTag().getString("nobullshipRecipe"));
        return returnComp.withStyle(Style.EMPTY.withColor(DARK_AQUA));
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
