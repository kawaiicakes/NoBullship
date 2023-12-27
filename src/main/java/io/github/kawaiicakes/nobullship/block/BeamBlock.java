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
import org.jetbrains.annotations.Nullable;

import static io.github.kawaiicakes.nobullship.Registry.METAL_BEAM_BLOCK;

// TODO: continue overriding and implementing methods
public class BeamBlock extends Block implements SimpleWaterloggedBlock {
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
