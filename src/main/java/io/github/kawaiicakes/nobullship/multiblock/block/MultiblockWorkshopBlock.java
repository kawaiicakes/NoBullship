package io.github.kawaiicakes.nobullship.multiblock.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import static io.github.kawaiicakes.nobullship.NoBullship.WORKSHOP_BLOCK_ENTITY;

public class MultiblockWorkshopBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public MultiblockWorkshopBlock() {
        super(BlockBehaviour.Properties.of(Material.METAL).noOcclusion().strength(5).requiresCorrectToolForDrops());
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING);
    }

    @Override
    public BlockState rotate(BlockState state, LevelAccessor level, BlockPos pos, Rotation direction) {
        return state.setValue(FACING, direction.rotate(state.getValue(FACING)));
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
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
