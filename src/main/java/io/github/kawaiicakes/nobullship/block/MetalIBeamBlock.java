package io.github.kawaiicakes.nobullship.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static io.github.kawaiicakes.nobullship.Registry.METAL_BEAM_BLOCK;

public class MetalIBeamBlock extends Block implements SimpleWaterloggedBlock {
    public static final List<VoxelShape> BASE_SHAPES = new ArrayList<>(4);
    public static final List<VoxelShape> SIDE_SHAPES = new ArrayList<>(4);
    public static final List<VoxelShape> VERTICAL_SIDE_SHAPES = new ArrayList<>(4);
    public static final List<VoxelShape> TOP_SHAPES = new ArrayList<>(8);

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

    public MetalIBeamBlock() {
        super(BlockBehaviour.Properties.of(Material.METAL)
                .sound(SoundType.ANVIL)
                .noOcclusion()
                .isViewBlocking((x,y,z) -> false)
                .isValidSpawn((w,x,y,z) -> !w.getValue(VERTICAL))
                .requiresCorrectToolForDrops()
        );
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
        VoxelShape aboveTop;
        VoxelShape belowTop;

        if (!isVertical) {
            aboveTop = !hasAbove.equals(BeamConnection.NONE) ? getTopShape(false, horizontalAxis, hasAbove) : Shapes.empty();
            belowTop = !hasBelow.equals(BeamConnection.NONE) ? getBottomShape(false, horizontalAxis, hasBelow) : Shapes.empty();
        } else {
            aboveTop = hasAbove.equals(BeamConnection.PARALLEL) ? getTopShape(true, horizontalAxis, BeamConnection.PARALLEL) : Shapes.empty();
            belowTop = hasBelow.equals(BeamConnection.PARALLEL) ? getBottomShape(true, horizontalAxis, BeamConnection.PARALLEL) : Shapes.empty();
        }

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
        Direction.Axis axis = pState.getValue(HORIZONTAL_AXIS);

        return switch (pMirror) {
            case NONE -> pState;
            case LEFT_RIGHT -> axis.equals(Direction.Axis.Z) ? this.rotate(pState, Rotation.CLOCKWISE_180) : pState;
            case FRONT_BACK -> axis.equals(Direction.Axis.X) ? this.rotate(pState, Rotation.CLOCKWISE_180) : pState;
        };
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState rotate(BlockState pState, Rotation pRotation) {
        Direction.Axis axis = pState.getValue(HORIZONTAL_AXIS);
        boolean isVertical = pState.getValue(VERTICAL);
        BeamConnection connectionAbove = pState.getValue(UP);
        BeamConnection connectionBelow = pState.getValue(DOWN);
        boolean connectionLeft = pState.getValue(LEFT);
        boolean connectionRight = pState.getValue(RIGHT);

        final Direction.Axis rotatedAxis = axis.equals(Direction.Axis.Z) ? Direction.Axis.X : Direction.Axis.Z;
        final boolean xorUpDownHasConnection = !connectionAbove.equals(BeamConnection.NONE) ^ !connectionBelow.equals(BeamConnection.NONE);
        final BeamConnection invertedAbove = connectionAbove.equals(BeamConnection.PARALLEL) ? BeamConnection.PERPENDICULAR : BeamConnection.PARALLEL;
        final BeamConnection invertedBelow = connectionBelow.equals(BeamConnection.PARALLEL) ? BeamConnection.PERPENDICULAR : BeamConnection.PARALLEL;

        switch (pRotation) {
            case NONE -> {
                return pState;
            }
            case CLOCKWISE_90 -> {
                axis = rotatedAxis;

                if (isVertical) {
                    final BeamConnection tempUp = connectionAbove;
                    final BeamConnection tempDown = connectionBelow;
                    final boolean tempLeft = connectionLeft;
                    final boolean tempRight = connectionRight;

                    if (connectionLeft ^ connectionRight) {
                        connectionLeft = tempRight;
                        connectionRight = tempLeft;
                    }

                    if (xorUpDownHasConnection) {
                        connectionAbove = tempDown;
                        connectionBelow = tempUp;
                    }
                } else {
                    final boolean tempLeft = connectionLeft;
                    final boolean tempRight = connectionRight;

                    if (connectionLeft ^ connectionRight) {
                        connectionLeft = tempRight;
                        connectionRight = tempLeft;
                    }

                    if (!connectionAbove.equals(BeamConnection.NONE)) {
                        connectionAbove = invertedAbove;
                    }

                    if (!connectionBelow.equals(BeamConnection.NONE)) {
                        connectionBelow = invertedBelow;
                    }
                }
            }
            case CLOCKWISE_180 -> {
                if (connectionLeft ^ connectionRight) {
                    connectionLeft = !connectionLeft;
                    connectionRight = !connectionRight;
                }

                if (xorUpDownHasConnection) {
                    final BeamConnection tempAbove = connectionAbove;

                    connectionAbove = connectionBelow;
                    connectionBelow = tempAbove;
                }
            }
            case COUNTERCLOCKWISE_90 -> {
                axis = rotatedAxis;

                if (!isVertical) {
                    if (!connectionAbove.equals(BeamConnection.NONE)) {
                        connectionAbove = invertedAbove;
                    }

                    if (!connectionBelow.equals(BeamConnection.NONE)) {
                        connectionBelow = invertedBelow;
                    }
                }
            }
        }

        return pState
                .setValue(HORIZONTAL_AXIS, axis)
                .setValue(UP, connectionAbove)
                .setValue(DOWN, connectionBelow)
                .setValue(LEFT, connectionLeft)
                .setValue(RIGHT, connectionRight);
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

        if (blockAbove.is(METAL_BEAM_BLOCK.get())) {
            if (!isVertical) {
                if (blockAbove.getValue(VERTICAL)) {
                    connectionAbove = blockAbove.getValue(HORIZONTAL_AXIS).equals(axisDirection) ? BeamConnection.PARALLEL : BeamConnection.PERPENDICULAR;
                }
            } else {
                connectionAbove = BeamConnection.PARALLEL;
            }
        }

        if (blockBelow.is(METAL_BEAM_BLOCK.get())) {
            if (!isVertical) {
                if (blockBelow.getValue(VERTICAL)) {
                    connectionBelow = blockBelow.getValue(HORIZONTAL_AXIS).equals(axisDirection) ? BeamConnection.PARALLEL : BeamConnection.PERPENDICULAR;
                }
            } else {
                connectionBelow = BeamConnection.PARALLEL;
            }
        }

        boolean attachesOnLeft = blockOnLeft.is(METAL_BEAM_BLOCK.get());
        boolean attachesOnRight = blockOnRight.is(METAL_BEAM_BLOCK.get());

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
        Direction.Axis axis = pState.getValue(HORIZONTAL_AXIS);
        boolean isVertical = pState.getValue(VERTICAL);
        BeamConnection connectionAbove = pState.getValue(UP);
        BeamConnection connectionBelow = pState.getValue(DOWN);
        boolean connectionLeft = pState.getValue(LEFT);
        boolean connectionRight = pState.getValue(RIGHT);

        switch (pDirection) {
            case DOWN -> {
                if (!isVertical) {
                    if (!pNeighborState.is(METAL_BEAM_BLOCK.get()))
                        connectionBelow = BeamConnection.NONE;
                    else {
                        if (pNeighborState.getValue(VERTICAL)) {
                            connectionBelow = pNeighborState.getValue(HORIZONTAL_AXIS).equals(axis) ? BeamConnection.PARALLEL : BeamConnection.PERPENDICULAR;
                        } else {
                            connectionBelow = BeamConnection.NONE;
                        }
                    }
                }
            }
            case UP -> {
                if (!isVertical) {
                    if (!pNeighborState.is(METAL_BEAM_BLOCK.get()))
                        connectionAbove = BeamConnection.NONE;
                    else {
                        if (pNeighborState.getValue(VERTICAL)) {
                            connectionAbove = pNeighborState.getValue(HORIZONTAL_AXIS).equals(axis) ? BeamConnection.PARALLEL : BeamConnection.PERPENDICULAR;
                        } else {
                            connectionAbove = BeamConnection.NONE;
                        }
                    }
                }
            }
            case NORTH -> {
                if (axis.equals(Direction.Axis.X)) {
                    connectionLeft = pNeighborState.is(METAL_BEAM_BLOCK.get());
                } else {
                    if (isVertical) connectionBelow = !pNeighborState.is(METAL_BEAM_BLOCK.get()) ? BeamConnection.NONE : BeamConnection.PARALLEL;
                }
            }
            case SOUTH -> {
                if (axis.equals(Direction.Axis.X)) {
                    connectionRight = pNeighborState.is(METAL_BEAM_BLOCK.get());
                } else {
                    if (isVertical) connectionAbove = !pNeighborState.is(METAL_BEAM_BLOCK.get()) ? BeamConnection.NONE : BeamConnection.PARALLEL;
                }
            }
            case WEST -> {
                if (axis.equals(Direction.Axis.Z)) {
                    connectionRight = pNeighborState.is(METAL_BEAM_BLOCK.get());
                } else {
                    if (isVertical) connectionBelow = !pNeighborState.is(METAL_BEAM_BLOCK.get()) ? BeamConnection.NONE : BeamConnection.PARALLEL;
                }
            }
            case EAST -> {
                if (axis.equals(Direction.Axis.Z)) {
                    connectionLeft = pNeighborState.is(METAL_BEAM_BLOCK.get());
                } else {
                    if (isVertical) connectionAbove = !pNeighborState.is(METAL_BEAM_BLOCK.get()) ? BeamConnection.NONE : BeamConnection.PARALLEL;
                }
            }
        }

        return pState
                .setValue(UP, connectionAbove)
                .setValue(DOWN, connectionBelow)
                .setValue(LEFT, connectionLeft)
                .setValue(RIGHT, connectionRight);
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

    public static VoxelShape getBaseShape(boolean isVertical, Direction.Axis horizontalAxis) {
        if (!isVertical) {
            return switch (horizontalAxis) {
                case X -> BASE_SHAPES.get(1);
                case Z -> BASE_SHAPES.get(0);
                default -> throw new IllegalArgumentException();
            };
        } else {
            return switch (horizontalAxis) {
                case X -> BASE_SHAPES.get(3);
                case Z -> BASE_SHAPES.get(2);
                default -> throw new IllegalArgumentException();
            };
        }
    }

    public static VoxelShape getLeftShape(boolean isVertical, Direction.Axis horizontalAxis) {
        if (!isVertical) {
            return switch (horizontalAxis) {
                case X -> SIDE_SHAPES.get(1);
                case Z -> SIDE_SHAPES.get(0);
                default -> throw new IllegalArgumentException();
            };
        } else {
            return switch (horizontalAxis) {
                case X -> VERTICAL_SIDE_SHAPES.get(1);
                case Z -> VERTICAL_SIDE_SHAPES.get(0);
                default -> throw new IllegalArgumentException();
            };
        }
    }

    public static VoxelShape getRightShape(boolean isVertical, Direction.Axis horizontalAxis) {
        if (!isVertical) {
            return switch (horizontalAxis) {
                case X -> SIDE_SHAPES.get(3);
                case Z -> SIDE_SHAPES.get(2);
                default -> throw new IllegalArgumentException();
            };
        } else {
            return switch (horizontalAxis) {
                case X -> VERTICAL_SIDE_SHAPES.get(3);
                case Z -> VERTICAL_SIDE_SHAPES.get(2);
                default -> throw new IllegalArgumentException();
            };
        }
    }

    public static VoxelShape getTopShape(boolean isVertical, Direction.Axis horizontalAxis, BeamConnection connectionType) {
        if (!isVertical) {
            return switch (connectionType) {
                case PARALLEL -> {
                    if (horizontalAxis.equals(Direction.Axis.X)) {
                        yield TOP_SHAPES.get(1);
                    } else {
                        yield TOP_SHAPES.get(0);
                    }
                }
                case PERPENDICULAR -> {
                    if (horizontalAxis.equals(Direction.Axis.Z)) {
                        yield TOP_SHAPES.get(1);
                    } else {
                        yield TOP_SHAPES.get(0);
                    }
                }
                default -> throw new IllegalArgumentException();
            };
        } else {
            if (horizontalAxis.equals(Direction.Axis.X)) {
                return TOP_SHAPES.get(5);
            } else {
                return TOP_SHAPES.get(4);
            }
        }
    }

    public static VoxelShape getBottomShape(boolean isVertical, Direction.Axis horizontalAxis, BeamConnection connectionType) {
        if (!isVertical) {
            return switch (connectionType) {
                case PARALLEL -> {
                    if (horizontalAxis.equals(Direction.Axis.X)) {
                        yield TOP_SHAPES.get(3);
                    } else {
                        yield TOP_SHAPES.get(2);
                    }
                }
                case PERPENDICULAR -> {
                    if (horizontalAxis.equals(Direction.Axis.Z)) {
                        yield TOP_SHAPES.get(3);
                    } else {
                        yield TOP_SHAPES.get(2);
                    }
                }
                default -> throw new IllegalArgumentException();
            };
        } else {
            if (horizontalAxis.equals(Direction.Axis.X)) {
                return TOP_SHAPES.get(7);
            } else {
                return TOP_SHAPES.get(6);
            }
        }
    }

    public static double[] rotateDimensions(Direction.Axis axis, Rotation rotation, double[] dimensions) {
        return switch (rotation) {
            case NONE -> dimensions;
            case CLOCKWISE_90 -> switch (axis) {
                case X ->
                        new double[]{dimensions[0], 16 - dimensions[5], dimensions[1], dimensions[3], 16 - dimensions[2], dimensions[4]};
                case Y ->
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

    public static double[][] quickRotateZCw(double[][] original) {
        double[][] rotatedDimensions = new double[original.length][];
        byte count = 0;
        for (double[] dimensions : original) {
            rotatedDimensions[count++] = rotateDimensions(Direction.Axis.Z, Rotation.CLOCKWISE_90, dimensions);
        }
        return rotatedDimensions;
    }

    public static double[][] quickRotateYCw(double[][] original) {
        double[][] rotatedDimensions = new double[original.length][];
        byte count = 0;
        for (double[] dimensions : original) {
            rotatedDimensions[count++] = rotateDimensions(Direction.Axis.Y, Rotation.CLOCKWISE_90, dimensions);
        }
        return rotatedDimensions;
    }

    public static double[][] quickRotateXCw(double[][] original) {
        double[][] rotatedDimensions = new double[original.length][];
        byte count = 0;
        for (double[] dimensions : original) {
            rotatedDimensions[count++] = rotateDimensions(Direction.Axis.X, Rotation.CLOCKWISE_90, dimensions);
        }
        return rotatedDimensions;
    }
    public static double[][] quickRotateYCcw(double[][] original) {
        double[][] rotatedDimensions = new double[original.length][];
        byte count = 0;
        for (double[] dimensions : original) {
            rotatedDimensions[count++] = rotateDimensions(Direction.Axis.Y, Rotation.COUNTERCLOCKWISE_90, dimensions);
        }
        return rotatedDimensions;
    }

    static {
        final double[][] baseShape = new double[][]{
                new double[]{6.0, 5.0, 0.0, 10, 6.0, 16},
                new double[]{7.5, 6.0, 0.0, 8.5, 10.0, 16.0},
                new double[]{6.0, 10, 0.0, 10, 11, 16}
        };
        final double[][] sideShape = new double[][]{
                new double[]{10, 5.0, 6.0, 16, 6.0, 10},
                new double[]{8.0, 6.0, 7.5, 16, 10, 8.5},
                new double[]{10, 10, 6.0, 16, 11, 10}
        };
        final double[][] verticalSideShape = new double[][]{
                new double[]{8.0, 5.025, 6.0, 16, 5.975, 10},
                new double[]{8.0, 5.975, 7.5, 16, 10.025, 8.5},
                new double[]{8.0, 10.025, 6.0, 16, 10.975, 10}
        };
        final double[][] topShape = new double[][]{
                new double[]{6.0, 8.0, 5.0, 10.0, 16.0, 6.0},
                new double[]{7.5, 11, 6.0, 8.5, 16, 10},
                new double[]{6.0, 8.0, 10, 10.0, 16.0, 11}
        };

        // non-vertical Z
        BASE_SHAPES.add(generateShape(baseShape));
        // non-vertical X
        BASE_SHAPES.add(generateShape(quickRotateYCw(baseShape)));
        // vertical Z
        BASE_SHAPES.add(generateShape(quickRotateXCw(baseShape)));
        // vertical X
        BASE_SHAPES.add(generateShape(quickRotateZCw(quickRotateYCw(baseShape))));

        // z left
        SIDE_SHAPES.add(generateShape(sideShape));
        // x left
        SIDE_SHAPES.add(generateShape(quickRotateYCw(sideShape)));
        // z right
        SIDE_SHAPES.add(generateShape(quickRotateYCw(quickRotateYCw(sideShape))));
        // x right
        SIDE_SHAPES.add(generateShape(quickRotateXCw(quickRotateXCw(quickRotateYCw(sideShape)))));

        // z left
        VERTICAL_SIDE_SHAPES.add(generateShape(verticalSideShape));
        // x left
        VERTICAL_SIDE_SHAPES.add(generateShape(quickRotateYCw(verticalSideShape)));
        // z right
        VERTICAL_SIDE_SHAPES.add(generateShape(quickRotateYCw(quickRotateYCw(verticalSideShape))));
        // x right
        VERTICAL_SIDE_SHAPES.add(generateShape(quickRotateYCcw(verticalSideShape)));

        // non-vertical top Z
        TOP_SHAPES.add(generateShape(topShape));
        // non-vertical top X
        TOP_SHAPES.add(generateShape(quickRotateYCw(topShape)));
        // non-vertical bottom Z
        TOP_SHAPES.add(generateShape(quickRotateXCw(quickRotateXCw(topShape))));
        // non-vertical bottom X
        TOP_SHAPES.add(generateShape(quickRotateYCw(quickRotateZCw(quickRotateZCw(topShape)))));
        // vertical top Z
        TOP_SHAPES.add(generateShape(quickRotateXCw(topShape)));
        // vertical top X
        TOP_SHAPES.add(generateShape(quickRotateYCw(quickRotateXCw(topShape))));
        // vertical bottom Z
        TOP_SHAPES.add(generateShape(quickRotateYCw(quickRotateYCw(quickRotateXCw(topShape)))));
        // vertical bottom X
        TOP_SHAPES.add(generateShape(quickRotateZCw(quickRotateYCw(topShape))));
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
