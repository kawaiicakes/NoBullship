package io.github.kawaiicakes.nobullship.multiblock.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import static io.github.kawaiicakes.nobullship.NoBullship.WORKSHOP_BLOCK_ENTITY;

public class MultiblockWorkshopBlock extends BaseEntityBlock {
    public MultiblockWorkshopBlock(Properties pProperties) {
        super(pProperties);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (pState.getBlock() == pNewState.getBlock()) {
            super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
            return;
        }

        BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
        if (!(blockEntity instanceof MultiblockWorkshopBlockEntity workshop)) return;

        SimpleContainer inventory = new SimpleContainer(workshop.itemHandler.getSlots());
        for (int i = 0; i < workshop.itemHandler.getSlots(); i++) {
            inventory.setItem(i, workshop.getItem(i));
        }

        Containers.dropContents(pLevel, pPos, inventory);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new MultiblockWorkshopBlockEntity(pPos, pState);
    }

    @SuppressWarnings("deprecation")
    @Override
    public InteractionResult use(BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult) {
        if (level.isClientSide) return InteractionResult.sidedSuccess(true);

        if (level.getBlockEntity(blockPos) instanceof MultiblockWorkshopBlockEntity castedEntity) {
            NetworkHooks.openScreen((ServerPlayer) player, castedEntity, blockPos);
        } else {
            throw new IllegalStateException("Missing Container provider for Workshop!");
        }

        return InteractionResult.sidedSuccess(false);
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!(world.getBlockEntity(pos) instanceof MultiblockWorkshopBlockEntity entity)) {
            super.setPlacedBy(world, pos, state, placer, stack);
            return;
        }

        if (!stack.hasCustomHoverName()) return;

        entity.setCustomName(stack.getHoverName());
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return pBlockEntityType == WORKSHOP_BLOCK_ENTITY.get() ? MultiblockWorkshopBlockEntity::tick : null;
    }

    @SuppressWarnings("deprecation")
    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }
}
