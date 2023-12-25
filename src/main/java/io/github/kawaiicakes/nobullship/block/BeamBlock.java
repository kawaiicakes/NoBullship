package io.github.kawaiicakes.nobullship.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

// TODO: continue overriding and implementing methods
public class BeamBlock extends Block implements SimpleWaterloggedBlock {
    public static final EnumProperty<Direction.Axis> HORIZONTAL_AXIS = EnumProperty.create(
            "horizontal_axis",
            Direction.Axis.class,
            Direction.Axis.X,
            Direction.Axis.Z
    );
    public static final BooleanProperty VERTICAL = BooleanProperty.create("vertical");
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public BeamBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(HORIZONTAL_AXIS, Direction.Axis.X)
                .setValue(VERTICAL, Boolean.FALSE)
                .setValue(UP, Boolean.FALSE)
                .setValue(DOWN, Boolean.FALSE)
                .setValue(NORTH, Boolean.FALSE)
                .setValue(EAST, Boolean.FALSE)
                .setValue(SOUTH, Boolean.FALSE)
                .setValue(WEST, Boolean.FALSE)
                .setValue(WATERLOGGED, Boolean.FALSE)
        );
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState rotate(BlockState pState, Rotation pRotation) {
        return super.rotate(pState, pRotation);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return super.getStateForPlacement(pContext);
    }

    @SuppressWarnings("deprecation")
    @Override
    public FluidState getFluidState(BlockState pState) {
        return pState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
    }

    @Override
    public boolean propagatesSkylightDown(BlockState pState, BlockGetter pReader, BlockPos pPos) {
        return !pState.getValue(WATERLOGGED);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(HORIZONTAL_AXIS, VERTICAL, UP, DOWN, NORTH, EAST, SOUTH, WEST, WATERLOGGED);
    }
}
