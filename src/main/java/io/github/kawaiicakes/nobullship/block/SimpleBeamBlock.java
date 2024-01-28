package io.github.kawaiicakes.nobullship.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import org.jetbrains.annotations.Nullable;

import static io.github.kawaiicakes.nobullship.api.NoBullshipBlockTags.POLYBEAM_CONNECTOR;
import static io.github.kawaiicakes.nobullship.block.WheelBlock.FACING;

public abstract class SimpleBeamBlock extends PipeBlock implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public SimpleBeamBlock(float pApothem, Properties pProperties) {
        super(pApothem, pProperties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false)
                .setValue(WATERLOGGED, false));
    }

    @SuppressWarnings("deprecation")
    @Override
    public abstract boolean isPathfindable(BlockState pState, BlockGetter pLevel, BlockPos pPos, PathComputationType pType);

    @Override
    public abstract boolean propagatesSkylightDown(BlockState pState, BlockGetter pReader, BlockPos pPos);

    @SuppressWarnings("deprecation")
    @Override
    public BlockState rotate(BlockState pState, Rotation pRotation) {
        return switch (pRotation) {
            case CLOCKWISE_180:
                yield pState
                        .setValue(NORTH, pState.getValue(SOUTH))
                        .setValue(EAST, pState.getValue(WEST))
                        .setValue(SOUTH, pState.getValue(NORTH))
                        .setValue(WEST, pState.getValue(EAST));
            case COUNTERCLOCKWISE_90:
                yield pState
                        .setValue(NORTH, pState.getValue(EAST))
                        .setValue(EAST, pState.getValue(SOUTH))
                        .setValue(SOUTH, pState.getValue(WEST))
                        .setValue(WEST, pState.getValue(NORTH));
            case CLOCKWISE_90:
                yield pState
                        .setValue(NORTH, pState.getValue(WEST))
                        .setValue(EAST, pState.getValue(NORTH))
                        .setValue(SOUTH, pState.getValue(EAST))
                        .setValue(WEST, pState.getValue(SOUTH));
            default:
                yield pState;
        };
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState mirror(BlockState pState, Mirror pMirror) {
        return switch (pMirror) {
            case LEFT_RIGHT:
                yield pState.setValue(NORTH, pState.getValue(SOUTH)).setValue(SOUTH, pState.getValue(NORTH));
            case FRONT_BACK:
                yield pState.setValue(EAST, pState.getValue(WEST)).setValue(WEST, pState.getValue(EAST));
            default:
                yield super.mirror(pState, pMirror);
        };
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        Level level = pContext.getLevel();
        BlockPos pos = pContext.getClickedPos();

        boolean isInWater = pContext.getLevel().getFluidState(pContext.getClickedPos()).getType() == Fluids.WATER;
        BlockState below = level.getBlockState(pos.below());
        BlockState above = level.getBlockState(pos.above());
        BlockState north = level.getBlockState(pos.north());
        BlockState east = level.getBlockState(pos.east());
        BlockState south = level.getBlockState(pos.south());
        BlockState west = level.getBlockState(pos.west());

        return this.defaultBlockState()
                .setValue(DOWN, this.isAttachableTo(below, Direction.DOWN))
                .setValue(UP, this.isAttachableTo(above, Direction.UP))
                .setValue(NORTH, this.isAttachableTo(north, Direction.NORTH))
                .setValue(EAST, this.isAttachableTo(east, Direction.EAST))
                .setValue(SOUTH, this.isAttachableTo(south, Direction.SOUTH))
                .setValue(WEST, this.isAttachableTo(west, Direction.WEST))
                .setValue(WATERLOGGED, isInWater);
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState updateShape(BlockState pState, Direction pDirection, BlockState pNeighborState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pNeighborPos) {
        boolean flag = this.isAttachableTo(pNeighborState, pDirection);
        return pState.setValue(PROPERTY_BY_DIRECTION.get(pDirection), flag);
    }

    @SuppressWarnings("deprecation")
    @Override
    public FluidState getFluidState(BlockState pState) {
        return pState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, WATERLOGGED);
    }

    public boolean isAttachableTo(BlockState blockState, Direction attachmentDirection) {
        Block block = blockState.getBlock();
        return blockState.is(this)
                || block instanceof SimpleBeamBlock
                || (block instanceof WheelBlock && blockState.getValue(FACING).equals(attachmentDirection.getOpposite()))
                || blockState.is(POLYBEAM_CONNECTOR);
    }

    public static class Polybeam extends SimpleBeamBlock {
        public Polybeam(Properties pProperties) {
            super(0.125F, pProperties
                    .noOcclusion()
                    .requiresCorrectToolForDrops()
                    .isViewBlocking((x,y,z) -> false)
                    .isValidSpawn((w,x,y,z) -> false));
        }

        @Override
        public boolean isPathfindable(BlockState pState, BlockGetter pLevel, BlockPos pPos, PathComputationType pType) {
            return false;
        }

        @Override
        public boolean propagatesSkylightDown(BlockState pState, BlockGetter pReader, BlockPos pPos) {
            return true;
        }

        @Override
        public boolean isAttachableTo(BlockState blockState, Direction attachmentDirection) {
            return super.isAttachableTo(blockState, attachmentDirection) || blockState.getBlock() instanceof FullLengthBeamBlock;
        }
    }

    public static class IndustrialPolybeam extends SimpleBeamBlock {
        public IndustrialPolybeam(Properties pProperties) {
            super(0.25F, pProperties
                    .noOcclusion()
                    .requiresCorrectToolForDrops()
                    .isViewBlocking((x,y,z) -> false)
                    .isValidSpawn((w,x,y,z) -> false));
        }

        @Override
        public boolean isPathfindable(BlockState pState, BlockGetter pLevel, BlockPos pPos, PathComputationType pType) {
            return false;
        }

        @Override
        public boolean propagatesSkylightDown(BlockState pState, BlockGetter pReader, BlockPos pPos) {
            return true;
        }
    }
}
