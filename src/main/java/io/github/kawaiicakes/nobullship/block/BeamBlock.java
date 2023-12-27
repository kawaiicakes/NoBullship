package io.github.kawaiicakes.nobullship.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static io.github.kawaiicakes.nobullship.Registry.METAL_BEAM_BLOCK;

public class BeamBlock extends Block implements SimpleWaterloggedBlock {
    public static final double[] BASE_SHAPE_BOTTOM = {6.0, 5.0, 0.0, 10, 6.0, 16};
    public static final double[] BASE_SHAPE_MIDDLE = {7.5, 6.0, 0.0, 8.5, 10.0, 16.0};
    public static final double[] BASE_SHAPE_TOP = {6.0, 10, 0.0, 10, 11, 16};
    public static final double[] SIDE_SHAPE_BOTTOM = {10, 5.0, 6.0, 16, 6.0, 10};
    public static final double[] SIDE_SHAPE_MIDDLE = {8.0, 6.0, 7.5, 16, 10, 8.5};
    public static final double[] SIDE_SHAPE_TOP = {10, 10, 6.0, 16, 11, 10};
    public static final double[] TOP_SHAPE_FRONT = {6.0, 8.0, 5.0, 10.0, 16.0, 6.0};
    public static final double[] TOP_SHAPE_MIDDLE = {7.5, 11, 6.0, 8.5, 16, 10};
    public static final double[] TOP_SHAPE_BACK = {6.0, 8.0, 10, 10.0, 16.0, 11};

    public static final EnumProperty<Direction.Axis> HORIZONTAL_AXIS = EnumProperty.create(
            "horizontal_axis",
            Direction.Axis.class,
            Direction.Axis.X,
            Direction.Axis.Z
    );
    public static final BooleanProperty VERTICAL = BooleanProperty.create("vertical");
    public static final EnumProperty<BeamConnection> UP = EnumProperty.create("up", BeamConnection.class);
    public static final EnumProperty<BeamConnection> DOWN = EnumProperty.create("down", BeamConnection.class);
    public static final BooleanProperty LEFT = BooleanProperty.create("left");
    public static final BooleanProperty RIGHT = BooleanProperty.create("right");
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public BeamBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(HORIZONTAL_AXIS, Direction.Axis.X)
                .setValue(VERTICAL, Boolean.FALSE)
                .setValue(UP, BeamConnection.NONE)
                .setValue(DOWN, BeamConnection.NONE)
                .setValue(LEFT, Boolean.FALSE)
                .setValue(RIGHT, Boolean.FALSE)
                .setValue(WATERLOGGED, Boolean.FALSE)
        );
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        Direction.Axis horizontalAxis = pState.getValue(HORIZONTAL_AXIS);
        boolean isVertical = pState.getValue(VERTICAL);
        boolean hasLeft = pState.getValue(LEFT);
        boolean hasRight = pState.getValue(RIGHT);
        BeamConnection hasAbove = pState.getValue(UP);
        BeamConnection hasBelow = pState.getValue(DOWN);

        VoxelShape leftSide = hasLeft ? getLeftShape(isVertical, horizontalAxis) : Shapes.empty();
        VoxelShape rightSide = hasRight ? getRightShape(isVertical, horizontalAxis) : Shapes.empty();
        VoxelShape aboveTop = !hasAbove.equals(BeamConnection.NONE) ? getTopShape(isVertical, horizontalAxis, hasAbove) : Shapes.empty();
        VoxelShape belowTop = !hasBelow.equals(BeamConnection.NONE) ? getBottomShape(isVertical, horizontalAxis, hasBelow) : Shapes.empty();

        return Shapes.or(
                getBaseShape(isVertical, horizontalAxis),
                leftSide,
                rightSide,
                aboveTop,
                belowTop
        );
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState mirror(BlockState pState, Mirror pMirror) {
        return super.mirror(pState, pMirror);
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState rotate(BlockState pState, Rotation pRotation) {
        return super.rotate(pState, pRotation);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        Direction.Axis axisDirection = pContext.getHorizontalDirection().getAxis();

        boolean isVertical = pContext.getClickedFace().getAxis().equals(Direction.Axis.Y);

        Level level = pContext.getLevel();
        BlockPos placementPos = pContext.getClickedPos();

        BlockState blockAbove = level.getBlockState(getAbove(placementPos, axisDirection, isVertical));
        BlockState blockBelow = level.getBlockState(getBelow(placementPos, axisDirection, isVertical));
        BlockState blockOnLeft = level.getBlockState(getLeft(placementPos, axisDirection));
        BlockState blockOnRight = level.getBlockState(getRight(placementPos, axisDirection));

        BeamConnection connectionAbove = BeamConnection.NONE;
        BeamConnection connectionBelow = BeamConnection.NONE;

        if (blockAbove.is(METAL_BEAM_BLOCK.get()) && blockAbove.getValue(VERTICAL) != isVertical) {
            connectionAbove = axisDirection.equals(blockAbove.getValue(HORIZONTAL_AXIS)) ? BeamConnection.PARALLEL : BeamConnection.PERPENDICULAR;
        }

        if (blockBelow.is(METAL_BEAM_BLOCK.get()) && blockBelow.getValue(VERTICAL) != isVertical) {
            connectionBelow = axisDirection.equals(blockBelow.getValue(HORIZONTAL_AXIS)) ? BeamConnection.PARALLEL : BeamConnection.PERPENDICULAR;
        }

        boolean attachesOnLeft = blockOnLeft.is(METAL_BEAM_BLOCK.get()) && !blockOnLeft.getValue(HORIZONTAL_AXIS).equals(axisDirection);
        boolean attachesOnRight = blockOnRight.is(METAL_BEAM_BLOCK.get()) && !blockOnRight.getValue(HORIZONTAL_AXIS).equals(axisDirection);

        boolean isInWater = pContext.getLevel().getFluidState(placementPos).getType() == Fluids.WATER;

        return this.defaultBlockState()
                .setValue(HORIZONTAL_AXIS, axisDirection)
                .setValue(VERTICAL, isVertical)
                .setValue(UP, connectionAbove)
                .setValue(DOWN, connectionBelow)
                .setValue(LEFT, attachesOnLeft)
                .setValue(RIGHT, attachesOnRight)
                .setValue(WATERLOGGED, isInWater);
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState updateShape(BlockState pState, Direction pDirection, BlockState pNeighborState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pNeighborPos) {
        if (!pNeighborState.is(METAL_BEAM_BLOCK.get())) return pState;

        return super.updateShape(pState, pDirection, pNeighborState, pLevel, pCurrentPos, pNeighborPos);
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
        pBuilder.add(HORIZONTAL_AXIS, VERTICAL, UP, DOWN, LEFT, RIGHT, WATERLOGGED);
    }

    public static VoxelShape getBaseShape(boolean isVertical, Direction.Axis horizontalAxis) {
        if (!isVertical) {
            return switch (horizontalAxis) {
                case X -> generateShape(
                        rotateDimensions(Direction.Axis.Y, Rotation.COUNTERCLOCKWISE_90, BASE_SHAPE_BOTTOM),
                        rotateDimensions(Direction.Axis.Y, Rotation.COUNTERCLOCKWISE_90, BASE_SHAPE_MIDDLE),
                        rotateDimensions(Direction.Axis.Y, Rotation.COUNTERCLOCKWISE_90, BASE_SHAPE_TOP));
                case Z -> generateShape(
                        BASE_SHAPE_BOTTOM,
                        BASE_SHAPE_MIDDLE,
                        BASE_SHAPE_TOP);
                default -> throw new IllegalArgumentException();
            };
        } else {
            return switch (horizontalAxis) {
                case X -> generateShape(
                        rotateDimensions(Direction.Axis.Z, Rotation.CLOCKWISE_90, rotateDimensions(Direction.Axis.Y, Rotation.COUNTERCLOCKWISE_90, BASE_SHAPE_BOTTOM)),
                        rotateDimensions(Direction.Axis.Z, Rotation.CLOCKWISE_90, rotateDimensions(Direction.Axis.Y, Rotation.COUNTERCLOCKWISE_90, BASE_SHAPE_MIDDLE)),
                        rotateDimensions(Direction.Axis.Z, Rotation.CLOCKWISE_90, rotateDimensions(Direction.Axis.Y, Rotation.COUNTERCLOCKWISE_90, BASE_SHAPE_TOP)));
                case Z -> generateShape(
                        rotateDimensions(Direction.Axis.X, Rotation.CLOCKWISE_90, BASE_SHAPE_BOTTOM),
                        rotateDimensions(Direction.Axis.X, Rotation.CLOCKWISE_90, BASE_SHAPE_MIDDLE),
                        rotateDimensions(Direction.Axis.X, Rotation.CLOCKWISE_90, BASE_SHAPE_TOP));
                default -> throw new IllegalArgumentException();
            };
        }
    }

    public static VoxelShape getLeftShape(boolean isVertical, Direction.Axis horizontalAxis) {
        if (!isVertical) {
            return switch (horizontalAxis) {
                case X -> generateShape(
                        rotateDimensions(Direction.Axis.Y, Rotation.CLOCKWISE_90, SIDE_SHAPE_BOTTOM),
                        rotateDimensions(Direction.Axis.Y, Rotation.CLOCKWISE_90, SIDE_SHAPE_MIDDLE),
                        rotateDimensions(Direction.Axis.Y, Rotation.CLOCKWISE_90, SIDE_SHAPE_TOP));
                case Z -> generateShape(
                        SIDE_SHAPE_BOTTOM,
                        SIDE_SHAPE_MIDDLE,
                        SIDE_SHAPE_TOP);
                default -> throw new IllegalArgumentException();
            };
        } else {
            return switch (horizontalAxis) {
                case X -> generateShape(
                        rotateDimensions(Direction.Axis.Z, Rotation.CLOCKWISE_90, rotateDimensions(Direction.Axis.Y, Rotation.COUNTERCLOCKWISE_90, SIDE_SHAPE_BOTTOM)),
                        rotateDimensions(Direction.Axis.Z, Rotation.CLOCKWISE_90, rotateDimensions(Direction.Axis.Y, Rotation.COUNTERCLOCKWISE_90, SIDE_SHAPE_MIDDLE)),
                        rotateDimensions(Direction.Axis.Z, Rotation.CLOCKWISE_90, rotateDimensions(Direction.Axis.Y, Rotation.COUNTERCLOCKWISE_90, SIDE_SHAPE_TOP)));
                case Z -> generateShape(
                        rotateDimensions(Direction.Axis.Z, Rotation.CLOCKWISE_90, SIDE_SHAPE_BOTTOM),
                        rotateDimensions(Direction.Axis.Z, Rotation.CLOCKWISE_90, SIDE_SHAPE_MIDDLE),
                        rotateDimensions(Direction.Axis.Z, Rotation.CLOCKWISE_90, SIDE_SHAPE_TOP));
                default -> throw new IllegalArgumentException();
            };
        }
    }

    public static VoxelShape getRightShape(boolean isVertical, Direction.Axis horizontalAxis) {
        if (!isVertical) {
            return switch (horizontalAxis) {
                case X -> generateShape(
                        rotateDimensions(Direction.Axis.Y, Rotation.COUNTERCLOCKWISE_90, SIDE_SHAPE_BOTTOM),
                        rotateDimensions(Direction.Axis.Y, Rotation.COUNTERCLOCKWISE_90, SIDE_SHAPE_MIDDLE),
                        rotateDimensions(Direction.Axis.Y, Rotation.COUNTERCLOCKWISE_90, SIDE_SHAPE_TOP));
                case Z -> generateShape(
                        rotateDimensions(Direction.Axis.Y, Rotation.CLOCKWISE_180, SIDE_SHAPE_BOTTOM),
                        rotateDimensions(Direction.Axis.Y, Rotation.CLOCKWISE_180, SIDE_SHAPE_MIDDLE),
                        rotateDimensions(Direction.Axis.Y, Rotation.CLOCKWISE_180, SIDE_SHAPE_TOP));
                default -> throw new IllegalArgumentException();
            };
        } else {
            return switch (horizontalAxis) {
                case X -> generateShape(
                        rotateDimensions(Direction.Axis.Z, Rotation.CLOCKWISE_90, rotateDimensions(Direction.Axis.Y, Rotation.COUNTERCLOCKWISE_90, SIDE_SHAPE_BOTTOM)),
                        rotateDimensions(Direction.Axis.Z, Rotation.CLOCKWISE_90, rotateDimensions(Direction.Axis.Y, Rotation.COUNTERCLOCKWISE_90, SIDE_SHAPE_MIDDLE)),
                        rotateDimensions(Direction.Axis.Z, Rotation.CLOCKWISE_90, rotateDimensions(Direction.Axis.Y, Rotation.COUNTERCLOCKWISE_90, SIDE_SHAPE_TOP)));
                case Z -> generateShape(
                        rotateDimensions(Direction.Axis.Z, Rotation.CLOCKWISE_90, SIDE_SHAPE_BOTTOM),
                        rotateDimensions(Direction.Axis.Z, Rotation.CLOCKWISE_90, SIDE_SHAPE_MIDDLE),
                        rotateDimensions(Direction.Axis.Z, Rotation.CLOCKWISE_90, SIDE_SHAPE_TOP));
                default -> throw new IllegalArgumentException();
            };
        }
    }

    public static VoxelShape getTopShape(boolean isVertical, Direction.Axis horizontalAxis, BeamConnection connectionType) {
        if (!isVertical) {
            return switch (connectionType) {
                case PARALLEL -> {
                    if (horizontalAxis.equals(Direction.Axis.X)) {
                        yield generateShape(
                                rotateDimensions(Direction.Axis.Y, Rotation.COUNTERCLOCKWISE_90, TOP_SHAPE_FRONT),
                                rotateDimensions(Direction.Axis.Y, Rotation.COUNTERCLOCKWISE_90, TOP_SHAPE_MIDDLE),
                                rotateDimensions(Direction.Axis.Y, Rotation.COUNTERCLOCKWISE_90, TOP_SHAPE_BACK));
                    } else {
                        yield generateShape(
                                TOP_SHAPE_FRONT,
                                TOP_SHAPE_MIDDLE,
                                TOP_SHAPE_BACK);
                    }
                }
                case PERPENDICULAR -> {
                    if (horizontalAxis.equals(Direction.Axis.Z)) {
                        yield generateShape(
                                rotateDimensions(Direction.Axis.Y, Rotation.COUNTERCLOCKWISE_90, TOP_SHAPE_FRONT),
                                rotateDimensions(Direction.Axis.Y, Rotation.COUNTERCLOCKWISE_90, TOP_SHAPE_MIDDLE),
                                rotateDimensions(Direction.Axis.Y, Rotation.COUNTERCLOCKWISE_90, TOP_SHAPE_BACK));
                    } else {
                        yield generateShape(
                                TOP_SHAPE_FRONT,
                                TOP_SHAPE_MIDDLE,
                                TOP_SHAPE_BACK);
                    }
                }
                default -> throw new IllegalArgumentException();
            };
        } else {
            return switch (connectionType) {
                case PARALLEL -> {
                    if (horizontalAxis.equals(Direction.Axis.X)) {
                        yield generateShape(
                                rotateDimensions(Direction.Axis.Z, Rotation.CLOCKWISE_90, rotateDimensions(Direction.Axis.Y, Rotation.COUNTERCLOCKWISE_90, TOP_SHAPE_FRONT)),
                                rotateDimensions(Direction.Axis.Z, Rotation.CLOCKWISE_90, rotateDimensions(Direction.Axis.Y, Rotation.COUNTERCLOCKWISE_90, TOP_SHAPE_MIDDLE)),
                                rotateDimensions(Direction.Axis.Z, Rotation.CLOCKWISE_90, rotateDimensions(Direction.Axis.Y, Rotation.COUNTERCLOCKWISE_90, TOP_SHAPE_BACK)));
                    } else {
                        yield generateShape(
                                rotateDimensions(Direction.Axis.Z, Rotation.CLOCKWISE_90, TOP_SHAPE_FRONT),
                                rotateDimensions(Direction.Axis.Z, Rotation.CLOCKWISE_90, TOP_SHAPE_MIDDLE),
                                rotateDimensions(Direction.Axis.Z, Rotation.CLOCKWISE_90, TOP_SHAPE_BACK));
                    }
                }
                case PERPENDICULAR -> {
                    if (horizontalAxis.equals(Direction.Axis.Z)) {
                        yield generateShape(
                                rotateDimensions(Direction.Axis.Z, Rotation.CLOCKWISE_90, rotateDimensions(Direction.Axis.Y, Rotation.COUNTERCLOCKWISE_90, TOP_SHAPE_FRONT)),
                                rotateDimensions(Direction.Axis.Z, Rotation.CLOCKWISE_90, rotateDimensions(Direction.Axis.Y, Rotation.COUNTERCLOCKWISE_90, TOP_SHAPE_MIDDLE)),
                                rotateDimensions(Direction.Axis.Z, Rotation.CLOCKWISE_90, rotateDimensions(Direction.Axis.Y, Rotation.COUNTERCLOCKWISE_90, TOP_SHAPE_BACK)));
                    } else {
                        yield generateShape(
                                rotateDimensions(Direction.Axis.Z, Rotation.CLOCKWISE_90, TOP_SHAPE_FRONT),
                                rotateDimensions(Direction.Axis.Z, Rotation.CLOCKWISE_90, TOP_SHAPE_MIDDLE),
                                rotateDimensions(Direction.Axis.Z, Rotation.CLOCKWISE_90, TOP_SHAPE_BACK));
                    }
                }
                default -> throw new IllegalArgumentException();
            };
        }
    }

    public static VoxelShape getBottomShape(boolean isVertical, Direction.Axis horizontalAxis, BeamConnection connectionType) {
        if (!isVertical) {
            return switch (connectionType) {
                case PARALLEL -> {
                    if (horizontalAxis.equals(Direction.Axis.X)) {
                        yield generateShape(
                                rotateDimensions(Direction.Axis.Y, Rotation.COUNTERCLOCKWISE_90, TOP_SHAPE_FRONT),
                                rotateDimensions(Direction.Axis.Y, Rotation.COUNTERCLOCKWISE_90, TOP_SHAPE_MIDDLE),
                                rotateDimensions(Direction.Axis.Y, Rotation.COUNTERCLOCKWISE_90, TOP_SHAPE_BACK));
                    } else {
                        yield generateShape(
                                TOP_SHAPE_FRONT,
                                TOP_SHAPE_MIDDLE,
                                TOP_SHAPE_BACK);
                    }
                }
                case PERPENDICULAR -> {
                    if (horizontalAxis.equals(Direction.Axis.Z)) {
                        yield generateShape(
                                rotateDimensions(Direction.Axis.Y, Rotation.COUNTERCLOCKWISE_90, TOP_SHAPE_FRONT),
                                rotateDimensions(Direction.Axis.Y, Rotation.COUNTERCLOCKWISE_90, TOP_SHAPE_MIDDLE),
                                rotateDimensions(Direction.Axis.Y, Rotation.COUNTERCLOCKWISE_90, TOP_SHAPE_BACK));
                    } else {
                        yield generateShape(
                                TOP_SHAPE_FRONT,
                                TOP_SHAPE_MIDDLE,
                                TOP_SHAPE_BACK);
                    }
                }
                default -> throw new IllegalArgumentException();
            };
        } else {
            return switch (connectionType) {
                case PARALLEL -> {
                    if (horizontalAxis.equals(Direction.Axis.X)) {
                        yield generateShape(
                                rotateDimensions(Direction.Axis.Z, Rotation.CLOCKWISE_90, rotateDimensions(Direction.Axis.Y, Rotation.COUNTERCLOCKWISE_90, TOP_SHAPE_FRONT)),
                                rotateDimensions(Direction.Axis.Z, Rotation.CLOCKWISE_90, rotateDimensions(Direction.Axis.Y, Rotation.COUNTERCLOCKWISE_90, TOP_SHAPE_MIDDLE)),
                                rotateDimensions(Direction.Axis.Z, Rotation.CLOCKWISE_90, rotateDimensions(Direction.Axis.Y, Rotation.COUNTERCLOCKWISE_90, TOP_SHAPE_BACK)));
                    } else {
                        yield generateShape(
                                TOP_SHAPE_FRONT,
                                TOP_SHAPE_MIDDLE,
                                TOP_SHAPE_BACK);
                    }
                }
                case PERPENDICULAR -> {
                    if (horizontalAxis.equals(Direction.Axis.Z)) {
                        yield generateShape(
                                rotateDimensions(Direction.Axis.Z, Rotation.CLOCKWISE_90, rotateDimensions(Direction.Axis.Y, Rotation.COUNTERCLOCKWISE_90, TOP_SHAPE_FRONT)),
                                rotateDimensions(Direction.Axis.Z, Rotation.CLOCKWISE_90, rotateDimensions(Direction.Axis.Y, Rotation.COUNTERCLOCKWISE_90, TOP_SHAPE_MIDDLE)),
                                rotateDimensions(Direction.Axis.Z, Rotation.CLOCKWISE_90, rotateDimensions(Direction.Axis.Y, Rotation.COUNTERCLOCKWISE_90, TOP_SHAPE_BACK)));
                    } else {
                        yield generateShape(
                                TOP_SHAPE_FRONT,
                                TOP_SHAPE_MIDDLE,
                                TOP_SHAPE_BACK);
                    }
                }
                default -> throw new IllegalArgumentException();
            };
        }
    }

    public static double[] rotateDimensions(Direction.Axis axis, Rotation rotation, double[] dimensions) {
        return switch (rotation) {
            case NONE -> dimensions;
            case CLOCKWISE_90 -> switch (axis) {
                case X ->
                        new double[]{dimensions[0], 16 - dimensions[5], dimensions[1], dimensions[3], 16 - dimensions[2], dimensions[4]};
                case Y ->
                        // 1, 2, 3, 2, 4, 6
                        // 3, 2, 14, 6, 4, 15
                        new double[]{dimensions[2], dimensions[1], 16 - dimensions[3], dimensions[5], dimensions[4], 16 - dimensions[0]};
                case Z ->
                        new double[]{16 - dimensions[4], dimensions[0], dimensions[2], 16 - dimensions[1], dimensions[3], dimensions[5]};
            };
            case CLOCKWISE_180 -> switch (axis) {
                case X ->
                        new double[]{dimensions[0], 16 - dimensions[4], 16 - dimensions[5], dimensions[3], 16 - dimensions[1], 16 - dimensions[2]};
                case Y ->
                        new double[]{16 - dimensions[3], dimensions[1], 16 - dimensions[5], 16 - dimensions[0], dimensions[4], 16 - dimensions[2]};
                case Z ->
                        new double[]{16 - dimensions[3], 16 - dimensions[4], dimensions[2], 16 - dimensions[0], 16 - dimensions[1], dimensions[5]};
            };
            case COUNTERCLOCKWISE_90 -> switch (axis) {
                case X ->
                        new double[]{dimensions[0], dimensions[2], 16 - dimensions[4], dimensions[3], dimensions[5], 16 - dimensions[1]};
                case Y ->
                        // 1, 2, 3, 2, 4, 6
                        // 10, 2, 1, 13, 4, 2
                        new double[]{16 - dimensions[5], dimensions[1], dimensions[0], 16 - dimensions[2], dimensions[4], dimensions[3]};
                case Z ->
                        new double[]{dimensions[1], 16 - dimensions[3], dimensions[2], dimensions[4], 16 - dimensions[0], dimensions[5]};
            };
        };
    }

    public static VoxelShape generateShape(double[]... dimensions) {
        List<VoxelShape> voxelShapes = new ArrayList<>(dimensions.length);
        for (double[] arr : dimensions) {
            voxelShapes.add(getBoxOf(arr));
        }
        return Shapes.or(Shapes.empty(), voxelShapes.toArray(VoxelShape[]::new));
    }

    public static VoxelShape getBoxOf(double[] dimensions) {
        return Block.box(dimensions[0], dimensions[1], dimensions[2], dimensions[3], dimensions[4], dimensions[5]);
    }

    public static BlockPos getLeft(BlockPos pos, Direction.Axis beamAxis) {
        return switch (beamAxis) {
            case X -> pos.north();
            case Z -> pos.east();
            default -> throw new IllegalArgumentException("Horizontal axes only!");
        };
    }

    public static BlockPos getRight(BlockPos pos, Direction.Axis beamAxis) {
        return switch (beamAxis) {
            case X -> pos.south();
            case Z -> pos.west();
            default -> throw new IllegalArgumentException("Horizontal axes only!");
        };
    }

    public static BlockPos getAbove(BlockPos pos, Direction.Axis beamAxis, boolean isVertical) {
        if (isVertical) {
            return switch (beamAxis) {
                case X -> pos.east();
                case Z -> pos.south();
                default -> throw new IllegalArgumentException("Horizontal axes only!");
            };
        } else {
            return pos.above();
        }
    }

    public static BlockPos getBelow(BlockPos pos, Direction.Axis beamAxis, boolean isVertical) {
        if (isVertical) {
            return switch (beamAxis) {
                case X -> pos.west();
                case Z -> pos.north();
                default -> throw new IllegalArgumentException("Horizontal axes only!");
            };
        } else {
            return pos.below();
        }
    }

    public enum BeamConnection implements StringRepresentable {
        NONE("none"),
        PARALLEL("parallel"),
        PERPENDICULAR("perpendicular");

        private final String name;

        BeamConnection(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}
