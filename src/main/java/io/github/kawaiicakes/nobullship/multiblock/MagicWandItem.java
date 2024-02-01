package io.github.kawaiicakes.nobullship.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

import static io.github.kawaiicakes.nobullship.NoBullship.NO_BULLSHIP_TAB;

public class MagicWandItem extends Item {
    public MagicWandItem() {super(new Properties().tab(NO_BULLSHIP_TAB));}

    @Override
    public boolean isFoil(ItemStack pStack) {
        return !pStack.getOrCreateTag().isEmpty();
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        BlockPos clickedPos = context.getClickedPos();

        if (context.isSecondaryUseActive()) {
            stack.setTag(new CompoundTag());
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(Component.translatable("chat.nobullship.clear_pos"), true);
            }
            return InteractionResult.SUCCESS;
        }

        if (context.getHitResult().getType().equals(HitResult.Type.MISS)) {
            return InteractionResult.SUCCESS;
        }

        if (stack.getOrCreateTag().getIntArray("pos1").length < 3) {
            stack.addTagElement("pos1", new IntArrayTag(new int[]{clickedPos.getX(), clickedPos.getY(), clickedPos.getZ()}));
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(Component.translatable("chat.nobullship.pos1", clickedPos.toShortString()), true);
            }
        } else {
            stack.addTagElement("pos2", new IntArrayTag(new int[]{clickedPos.getX(), clickedPos.getY(), clickedPos.getZ()}));
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(Component.translatable("chat.nobullship.pos2", clickedPos.toShortString()), true);
            }
        }

        return super.onItemUseFirst(stack, context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        if (pLevel.isClientSide) return InteractionResultHolder.success(pPlayer.getItemInHand(pUsedHand));
        return InteractionResultHolder.fail(pPlayer.getItemInHand(pUsedHand));
    }
}
