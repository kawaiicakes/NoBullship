package io.github.kawaiicakes.nobullship.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public abstract class WheelBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public WheelBlock(Properties pProperties) {
        super(pProperties
                .noOcclusion()
        );
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.EAST)
        );
    }

    @SuppressWarnings("deprecation")
    @Override
    public abstract VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext);

    @SuppressWarnings("deprecation")
    @Override
    public BlockState mirror(BlockState pState, Mirror pMirror) {
        return pMirror.equals(Mirror.LEFT_RIGHT) ?
                (pState.getValue(FACING).getAxis().equals(Direction.Axis.Z) ? this.rotate(pState, Rotation.CLOCKWISE_180) : pState) :
                (pState.getValue(FACING).getAxis().equals(Direction.Axis.X) ? this.rotate(pState, Rotation.CLOCKWISE_180) : pState);
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState rotate(BlockState pState, Rotation pRotation) {
        Direction facing = pState.getValue(FACING);
        return pState.setValue(FACING, pRotation.rotate(facing));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState().setValue(FACING, pContext.getClickedFace().getOpposite());
    }

    @Override
    public boolean propagatesSkylightDown(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return true;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING);
    }
}
