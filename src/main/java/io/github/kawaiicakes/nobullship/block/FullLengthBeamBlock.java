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

import static io.github.kawaiicakes.nobullship.api.NoBullshipBlockTags.FL_BEAM_CONNECTOR;
import static io.github.kawaiicakes.nobullship.block.WheelBlock.FACING;

public abstract class FullLengthBeamBlock extends Block implements SimpleWaterloggedBlock {
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

    public final List<VoxelShape> baseShapes = new ArrayList<>(4);
    public final List<VoxelShape> sideShapes = new ArrayList<>(4);
    public final List<VoxelShape> vSideShapes = new ArrayList<>(4);
    public final List<VoxelShape> topShapes = new ArrayList<>(8);

    public FullLengthBeamBlock(Properties properties, double[][] base, double[][] side, double[][] vSide, double[][] top) {
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
        this.generateShapes(base, side, vSide, top);
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
        boolean connectionLeft = pState.getValue(LEFT);
        boolean connectionRight = pState.getValue(RIGHT);

        final Direction.Axis rotatedAxis = axis.equals(Direction.Axis.Z) ? Direction.Axis.X : Direction.Axis.Z;

        switch (pRotation) {
            case NONE -> {
                return pState;
            }
            case CLOCKWISE_90 -> {
                if (axis.equals(Direction.Axis.X)) {
                    axis = rotatedAxis;
                    break;
                }

                axis = rotatedAxis;
                connectionLeft = connectionRight;
                connectionRight = pState.getValue(LEFT);
            }
            case CLOCKWISE_180 -> {
                connectionLeft = connectionRight;
                connectionRight = pState.getValue(LEFT);
            }
            case COUNTERCLOCKWISE_90 -> {
                if (axis.equals(Direction.Axis.Z)) {
                    axis = rotatedAxis;
                    break;
                }

                axis = rotatedAxis;
                connectionLeft = connectionRight;
                connectionRight = pState.getValue(LEFT);
            }
        }

        return pState
                .setValue(HORIZONTAL_AXIS, axis)
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

        Direction upAttachmentDirection =
                switch (axisDirection) {
                    case X -> {
                        if (isVertical) yield Direction.WEST;
                        yield Direction.DOWN;
                    }
                    case Z -> {
                        if (isVertical) yield Direction.NORTH;
                        yield Direction.DOWN;
                    }
                    default -> throw new IllegalArgumentException();
                };
        Direction downAttachmentDirection =
                switch (axisDirection) {
                    case X -> {
                        if (isVertical) yield Direction.EAST;
                        yield Direction.UP;
                    }
                    case Z -> {
                        if (isVertical) yield Direction.SOUTH;
                        yield Direction.UP;
                    }
                    default -> throw new IllegalArgumentException();
                };

        if (blockAbove.getBlock() instanceof FullLengthBeamBlock) {
            if (!isVertical) {
                if (blockAbove.getValue(VERTICAL)) {
                    connectionAbove = blockAbove.getValue(HORIZONTAL_AXIS).equals(axisDirection) ? BeamConnection.PARALLEL : BeamConnection.PERPENDICULAR;
                }
            } else {
                connectionAbove = BeamConnection.PARALLEL;
            }
        } else if (blockAbove.getBlock() instanceof WheelBlock && blockAbove.getValue(FACING).equals(upAttachmentDirection)) {
            connectionAbove = BeamConnection.PARALLEL;
        } else if (blockAbove.getBlock() instanceof SimpleBeamBlock.Polybeam || blockAbove.is(FL_BEAM_CONNECTOR)) {
            connectionAbove = BeamConnection.PARALLEL;
        }

        if (blockBelow.getBlock() instanceof FullLengthBeamBlock) {
            if (!isVertical) {
                if (blockBelow.getValue(VERTICAL)) {
                    connectionBelow = blockBelow.getValue(HORIZONTAL_AXIS).equals(axisDirection) ? BeamConnection.PARALLEL : BeamConnection.PERPENDICULAR;
                }
            } else {
                connectionBelow = BeamConnection.PARALLEL;
            }
        } else if (blockBelow.getBlock() instanceof WheelBlock && blockBelow.getValue(FACING).equals(downAttachmentDirection)) {
            connectionBelow = BeamConnection.PARALLEL;
        } else if (blockBelow.getBlock() instanceof SimpleBeamBlock.Polybeam || blockBelow.is(FL_BEAM_CONNECTOR)) {
            connectionBelow = BeamConnection.PARALLEL;
        }

        Direction leftAttachmentDirection =
                switch (axisDirection) {
                    case X -> Direction.SOUTH;
                    case Z -> Direction.WEST;
                    default -> throw new IllegalArgumentException();
                };
        Direction rightAttachmentDirection =
                switch (axisDirection) {
                    case X -> Direction.NORTH;
                    case Z -> Direction.EAST;
                    default -> throw new IllegalArgumentException();
                };

        boolean attachesOnLeft =
                blockOnLeft.getBlock() instanceof FullLengthBeamBlock ||
                (blockOnLeft.getBlock() instanceof WheelBlock && blockOnLeft.getValue(FACING).equals(leftAttachmentDirection)) ||
                blockOnLeft.getBlock() instanceof SimpleBeamBlock.Polybeam || blockOnLeft.is(FL_BEAM_CONNECTOR);
        boolean attachesOnRight = blockOnRight.getBlock() instanceof FullLengthBeamBlock||
                (blockOnRight.getBlock() instanceof WheelBlock && blockOnRight.getValue(FACING).equals(rightAttachmentDirection)) ||
                blockOnRight.getBlock() instanceof SimpleBeamBlock.Polybeam || blockOnRight.is(FL_BEAM_CONNECTOR);

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

        Block neighborBlock = pNeighborState.getBlock();
        boolean neighborIsFLBeam = neighborBlock instanceof FullLengthBeamBlock;
        boolean isNeighborVertical = neighborIsFLBeam ? pNeighborState.getValue(VERTICAL) : false;
        boolean neighborIsWheel = neighborBlock instanceof WheelBlock;
        boolean neighborIsPolybeam = neighborBlock instanceof SimpleBeamBlock.Polybeam;
        boolean neighborIsTagged = pNeighborState.is(FL_BEAM_CONNECTOR);
        Direction wheelDirection = neighborIsWheel ? pNeighborState.getValue(FACING) : null;

        switch (pDirection) {
            case DOWN -> {
                if (!isVertical) {
                    if (neighborIsFLBeam) {
                        if (isNeighborVertical) {
                            connectionBelow = pNeighborState.getValue(HORIZONTAL_AXIS).equals(axis) ? BeamConnection.PARALLEL : BeamConnection.PERPENDICULAR;
                        } else {
                            connectionBelow = BeamConnection.NONE;
                        }
                    } else if (Direction.UP.equals(wheelDirection) || neighborIsPolybeam || neighborIsTagged) {
                        connectionBelow = BeamConnection.PARALLEL;
                    } else {
                        connectionBelow = BeamConnection.NONE;
                    }
                }
            }
            case UP -> {
                if (!isVertical) {
                    if (neighborIsFLBeam) {
                        if (isNeighborVertical) {
                            connectionAbove = pNeighborState.getValue(HORIZONTAL_AXIS).equals(axis) ? BeamConnection.PARALLEL : BeamConnection.PERPENDICULAR;
                        } else {
                            connectionAbove = BeamConnection.NONE;
                        }
                    } else if (Direction.DOWN.equals(wheelDirection) || neighborIsPolybeam || neighborIsTagged) {
                        connectionAbove = BeamConnection.PARALLEL;
                    } else {
                        connectionAbove = BeamConnection.NONE;
                    }
                }
            }
            case NORTH -> {
                if (axis.equals(Direction.Axis.X)) {
                    connectionLeft = neighborIsFLBeam ||
                            (Direction.SOUTH.equals(wheelDirection))
                            || neighborIsPolybeam
                            || neighborIsTagged;
                } else {
                    if (isVertical) {
                        if (neighborIsFLBeam || Direction.SOUTH.equals(wheelDirection) || neighborIsPolybeam || neighborIsTagged) {
                            connectionBelow = BeamConnection.PARALLEL;
                        } else {
                            connectionBelow = BeamConnection.NONE;
                        }
                    }
                }
            }
            case SOUTH -> {
                if (axis.equals(Direction.Axis.X)) {
                    connectionRight = neighborIsFLBeam ||
                            (Direction.NORTH.equals(wheelDirection)) ||
                            neighborIsPolybeam
                            || neighborIsTagged;
                } else {
                    if (isVertical) {
                        if (neighborIsFLBeam || Direction.NORTH.equals(wheelDirection) || neighborIsPolybeam || neighborIsTagged) {
                            connectionAbove = BeamConnection.PARALLEL;
                        } else {
                            connectionAbove = BeamConnection.NONE;
                        }
                    }
                }
            }
            case WEST -> {
                if (axis.equals(Direction.Axis.Z)) {
                    connectionRight = neighborIsFLBeam
                            || (Direction.EAST.equals(wheelDirection))
                            || neighborIsPolybeam
                            || neighborIsTagged;
                } else {
                    if (isVertical) {
                        if (neighborIsFLBeam || Direction.EAST.equals(wheelDirection) || neighborIsPolybeam || neighborIsTagged) {
                            connectionBelow = BeamConnection.PARALLEL;
                        } else {
                            connectionBelow = BeamConnection.NONE;
                        }
                    }
                }
            }
            case EAST -> {
                if (axis.equals(Direction.Axis.Z)) {
                    connectionLeft = neighborIsFLBeam ||
                            (Direction.WEST.equals(wheelDirection))
                            || neighborIsPolybeam
                            || neighborIsTagged;
                } else {
                    if (isVertical) {
                        if (neighborIsFLBeam || Direction.WEST.equals(wheelDirection) || neighborIsPolybeam || neighborIsTagged) {
                            connectionAbove = BeamConnection.PARALLEL;
                        } else {
                            connectionAbove = BeamConnection.NONE;
                        }
                    }
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

    public VoxelShape getBaseShape(boolean isVertical, Direction.Axis horizontalAxis) {
        if (!isVertical) {
            return switch (horizontalAxis) {
                case X -> baseShapes.get(1);
                case Z -> baseShapes.get(0);
                default -> throw new IllegalArgumentException();
            };
        } else {
            return switch (horizontalAxis) {
                case X -> baseShapes.get(3);
                case Z -> baseShapes.get(2);
                default -> throw new IllegalArgumentException();
            };
        }
    }

    public VoxelShape getLeftShape(boolean isVertical, Direction.Axis horizontalAxis) {
        if (!isVertical) {
            return switch (horizontalAxis) {
                case X -> sideShapes.get(1);
                case Z -> sideShapes.get(0);
                default -> throw new IllegalArgumentException();
            };
        } else {
            return switch (horizontalAxis) {
                case X -> vSideShapes.get(1);
                case Z -> vSideShapes.get(0);
                default -> throw new IllegalArgumentException();
            };
        }
    }

    public VoxelShape getRightShape(boolean isVertical, Direction.Axis horizontalAxis) {
        if (!isVertical) {
            return switch (horizontalAxis) {
                case X -> sideShapes.get(3);
                case Z -> sideShapes.get(2);
                default -> throw new IllegalArgumentException();
            };
        } else {
            return switch (horizontalAxis) {
                case X -> vSideShapes.get(3);
                case Z -> vSideShapes.get(2);
                default -> throw new IllegalArgumentException();
            };
        }
    }

    public VoxelShape getTopShape(boolean isVertical, Direction.Axis horizontalAxis, BeamConnection connectionType) {
        if (!isVertical) {
            return switch (connectionType) {
                case PARALLEL -> {
                    if (horizontalAxis.equals(Direction.Axis.X)) {
                        yield topShapes.get(1);
                    } else {
                        yield topShapes.get(0);
                    }
                }
                case PERPENDICULAR -> {
                    if (horizontalAxis.equals(Direction.Axis.Z)) {
                        yield topShapes.get(1);
                    } else {
                        yield topShapes.get(0);
                    }
                }
                default -> throw new IllegalArgumentException();
            };
        } else {
            if (horizontalAxis.equals(Direction.Axis.X)) {
                return topShapes.get(5);
            } else {
                return topShapes.get(4);
            }
        }
    }

    public VoxelShape getBottomShape(boolean isVertical, Direction.Axis horizontalAxis, BeamConnection connectionType) {
        if (!isVertical) {
            return switch (connectionType) {
                case PARALLEL -> {
                    if (horizontalAxis.equals(Direction.Axis.X)) {
                        yield topShapes.get(3);
                    } else {
                        yield topShapes.get(2);
                    }
                }
                case PERPENDICULAR -> {
                    if (horizontalAxis.equals(Direction.Axis.Z)) {
                        yield topShapes.get(3);
                    } else {
                        yield topShapes.get(2);
                    }
                }
                default -> throw new IllegalArgumentException();
            };
        } else {
            if (horizontalAxis.equals(Direction.Axis.X)) {
                return topShapes.get(7);
            } else {
                return topShapes.get(6);
            }
        }
    }

    private void generateShapes(double[][] base, double[][] side, double[][] vSide, double[][] top) {
        // non-vertical Z
        baseShapes.add(generateShape(base));
        // non-vertical X
        baseShapes.add(generateShape(quickRotateYCw(base)));
        // vertical Z
        baseShapes.add(generateShape(quickRotateXCw(base)));
        // vertical X
        baseShapes.add(generateShape(quickRotateZCw(quickRotateYCw(base))));

        // z left
        sideShapes.add(generateShape(side));
        // x left
        sideShapes.add(generateShape(quickRotateYCw(side)));
        // z right
        sideShapes.add(generateShape(quickRotateYCw(quickRotateYCw(side))));
        // x right
        sideShapes.add(generateShape(quickRotateXCw(quickRotateXCw(quickRotateYCw(side)))));

        // z left
        vSideShapes.add(generateShape(vSide));
        // x left
        vSideShapes.add(generateShape(quickRotateYCw(vSide)));
        // z right
        vSideShapes.add(generateShape(quickRotateYCw(quickRotateYCw(vSide))));
        // x right
        vSideShapes.add(generateShape(quickRotateYCcw(vSide)));

        // non-vertical top Z
        topShapes.add(generateShape(top));
        // non-vertical top X
        topShapes.add(generateShape(quickRotateYCw(top)));
        // non-vertical bottom Z
        topShapes.add(generateShape(quickRotateXCw(quickRotateXCw(top))));
        // non-vertical bottom X
        topShapes.add(generateShape(quickRotateYCw(quickRotateZCw(quickRotateZCw(top)))));
        // vertical top Z
        topShapes.add(generateShape(quickRotateXCw(top)));
        // vertical top X
        topShapes.add(generateShape(quickRotateYCw(quickRotateXCw(top))));
        // vertical bottom Z
        topShapes.add(generateShape(quickRotateYCw(quickRotateYCw(quickRotateXCw(top)))));
        // vertical bottom X
        topShapes.add(generateShape(quickRotateZCw(quickRotateYCw(top))));
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

    public enum BeamConnection implements StringRepresentable {
        NONE("none"),
        PARALLEL("parallel"),
        PERPENDICULAR("perpendicular");

        private final String name;

        BeamConnection(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}
