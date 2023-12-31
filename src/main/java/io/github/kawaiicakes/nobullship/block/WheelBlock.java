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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static net.minecraft.world.level.block.Blocks.SHROOMLIGHT;

public abstract class WheelBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public WheelBlock(Properties pProperties) {
        super(pProperties
                .noOcclusion()
                .isViewBlocking((x,y,z) -> false)
                .isValidSpawn((w,x,y,z) -> false)
                .requiresCorrectToolForDrops()
        );
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.SOUTH)
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

    public static class WoodWheelBlock extends WheelBlock {
        public static final Map<Direction, VoxelShape> SHAPE_BY_DIRECTION = new HashMap<>(6);

        public WoodWheelBlock(Properties pProperties) {
            super(pProperties);
        }

        @Override
        public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
            return null;
        }
    }

    public static class TireBlock extends WheelBlock {
        public static final Map<Direction, VoxelShape> SHAPE_BY_DIRECTION = new HashMap<>(6);

        public TireBlock() {
            super(Properties.copy(SHROOMLIGHT));
        }

        @Override
        public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
            if (!SHAPE_BY_DIRECTION.containsKey(pState.getValue(FACING))) throw new IllegalArgumentException();
            return SHAPE_BY_DIRECTION.get(pState.getValue(FACING));
        }

        static {
            final List<Double[]> defaultShape = List.of(
                    new Double[]{0.0, 2.0, 12.0, 4.0, 14.0, 16.0},
                    new Double[]{12.0, 2.0, 12.0, 16.0, 14.0, 16.0},
                    new Double[]{2.0, 0.0, 12.0, 14.0, 4.0, 16.0},
                    new Double[]{2.0, 12.0, 12.0, 14.0, 16.0, 16.0},
                    new Double[]{6.5, 6.5, 12.0, 9.5, 9.5, 16.0}
            );

            for (Direction direction : Direction.values()) {
                double[][] rotatedDimensions = new double[5][];

                int shapeNumber = 0;
                for (Double[] dimensions : defaultShape) {
                    double[] castedDimensions = new double[6];
                    for (int i = 0; i < 6; i++) {
                        castedDimensions[i] = dimensions[i];
                    }

                    Direction.Axis axisOfRotation = !direction.equals(Direction.UP) && !direction.equals(Direction.DOWN)
                            ? Direction.Axis.Y : Direction.Axis.X;

                    Rotation rotation = switch (direction) {
                        case DOWN, EAST -> Rotation.CLOCKWISE_90;
                        case UP, WEST -> Rotation.COUNTERCLOCKWISE_90;
                        case NORTH -> Rotation.CLOCKWISE_180;
                        case SOUTH -> Rotation.NONE;
                    };

                    rotatedDimensions[shapeNumber++] = MetalIBeamBlock.rotateDimensions(axisOfRotation, rotation, castedDimensions);
                }

                SHAPE_BY_DIRECTION.put(direction, Shapes.or(
                        MetalIBeamBlock.generateShape(rotatedDimensions[0]),
                        MetalIBeamBlock.generateShape(rotatedDimensions[1]),
                        MetalIBeamBlock.generateShape(rotatedDimensions[2]),
                        MetalIBeamBlock.generateShape(rotatedDimensions[3]),
                        MetalIBeamBlock.generateShape(rotatedDimensions[4])));
            }
        }
    }
}
