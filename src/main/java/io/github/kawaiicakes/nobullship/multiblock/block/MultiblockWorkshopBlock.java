package io.github.kawaiicakes.nobullship.multiblock.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import static io.github.kawaiicakes.nobullship.Registry.WORKSHOP_BLOCK_ENTITY;

public class MultiblockWorkshopBlock extends BaseEntityBlock {
    protected static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    protected static final double[] FOOT_RIGHT = {0, 0, 0, 2, 1.5, 16};
    protected static final double[] FOOT_LEFT = {14, 0, 0, 16, 1.5, 16};
    protected static final double[] VERTICAL_BAR_RIGHT = {0.25, 1.5, 10, 1.75, 13.5, 13};
    protected static final double[] VERTICAL_BAR_LEFT = {14.5, 1.5, 10, 16, 13.5, 13};
    protected static final double[] HORIZONTAL_BAR_BOTTOM = {1.5, 2, 11.75, 14.5, 3.25, 12.5};
    protected static final double[] HORIZONTAL_BAR_TOP = {1.5, 6, 11.75, 14.5, 7.25, 12.5};
    protected static final double[] BOX_1 = {1.5, 9, 1, 14.5, 12, 6};
    protected static final double[] BOX_2 = {1.5, 10, 6, 14.5, 14, 10};
    protected static final double[] BOX_3 = {1.5, 13, 10, 14.5, 16, 16};
    protected static final VoxelShape NORTH_SHAPE = generateForDirection(Direction.NORTH);
    protected static final VoxelShape EAST_SHAPE = generateForDirection(Direction.EAST);
    protected static final VoxelShape SOUTH_SHAPE = generateForDirection(Direction.SOUTH);
    protected static final VoxelShape WEST_SHAPE = generateForDirection(Direction.WEST);

    /**
     * Voxel coords should be defined assuming the model is facing north
     */
    protected static VoxelShape rotateProvidedDimensions(Direction direction, double[] dimensions) {
        return switch (direction) {
            case NORTH -> Block.box(dimensions[0], dimensions[1], dimensions[2], dimensions[3], dimensions[4], dimensions[5]);
            case EAST -> Block.box(16 - dimensions[5], dimensions[1], dimensions[0], 16 - dimensions[2], dimensions[4], dimensions[3]);
            case SOUTH -> Block.box(16 - dimensions[3], dimensions[1], 16 - dimensions[5], 16 - dimensions[0], dimensions[4], 16 - dimensions[2]);
            case WEST -> Block.box(dimensions[2], dimensions[1], 16 - dimensions[3], dimensions[5], dimensions[4], 16 - dimensions[0]);
            default -> throw new IllegalArgumentException("Invalid direction passed!");
        };
    }

    protected static VoxelShape generateForDirection(Direction direction) {
        return Shapes.or(
                rotateProvidedDimensions(direction, FOOT_RIGHT),
                rotateProvidedDimensions(direction, FOOT_LEFT),
                rotateProvidedDimensions(direction, VERTICAL_BAR_RIGHT),
                rotateProvidedDimensions(direction, VERTICAL_BAR_LEFT),
                rotateProvidedDimensions(direction, HORIZONTAL_BAR_BOTTOM),
                rotateProvidedDimensions(direction, HORIZONTAL_BAR_TOP),
                rotateProvidedDimensions(direction, BOX_1),
                rotateProvidedDimensions(direction, BOX_2),
                rotateProvidedDimensions(direction, BOX_3)
        );
    }

    public MultiblockWorkshopBlock() {
        super(BlockBehaviour.Properties
                .of(Material.METAL)
                .noOcclusion()
                .isViewBlocking((a,b,c) -> false)
                .isValidSpawn((w,x,y,z) -> false)
                .isSuffocating((x,y,z) -> false)
                .strength(4.0F, 6.0F));
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return switch (pState.getValue(FACING)) {
            case SOUTH -> SOUTH_SHAPE;
            case EAST -> EAST_SHAPE;
            case WEST -> WEST_SHAPE;
            default -> NORTH_SHAPE;
        };
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return switch (pState.getValue(FACING)) {
            case SOUTH -> SOUTH_SHAPE;
            case EAST -> EAST_SHAPE;
            case WEST -> WEST_SHAPE;
            default -> NORTH_SHAPE;
        };
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
        workshop.hasRecipe = null;
        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
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
        if (!pLevel.isClientSide) return null;
        return pBlockEntityType == WORKSHOP_BLOCK_ENTITY.get() ? MultiblockWorkshopBlockEntity::tick : null;
    }

    @SuppressWarnings("deprecation")
    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }
}
