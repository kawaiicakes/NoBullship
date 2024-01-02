package io.github.kawaiicakes.nobullship.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static net.minecraft.world.level.block.Blocks.*;

public abstract class WheelBlock extends Block implements SimpleWaterloggedBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    protected WheelBlock(Properties pProperties) {
        super(pProperties
                .noOcclusion()
                .isViewBlocking((x,y,z) -> false)
                .isValidSpawn((w,x,y,z) -> false)
                .isSuffocating((x,y,z) -> false)
                .requiresCorrectToolForDrops()
        );
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.SOUTH)
                .setValue(WATERLOGGED, false)
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
        boolean isInWater = pContext.getLevel().getFluidState(pContext.getClickedPos()).getType() == Fluids.WATER;
        return this.defaultBlockState()
                .setValue(FACING, pContext.getClickedFace().getOpposite())
                .setValue(WATERLOGGED, isInWater);
    }

    @Override
    public boolean propagatesSkylightDown(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public FluidState getFluidState(BlockState pState) {
        return pState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING, WATERLOGGED);
    }

    public static class WoodWheelBlock extends WheelBlock {
        public static final Map<Direction, VoxelShape> SHAPE_BY_DIRECTION = new HashMap<>(6);

        public WoodWheelBlock() {
            super(Properties.copy(OAK_WOOD).strength(2.0F).sound(SoundType.WOOD));
        }

        @Override
        public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
            if (!SHAPE_BY_DIRECTION.containsKey(pState.getValue(FACING))) throw new IllegalArgumentException();
            return SHAPE_BY_DIRECTION.get(pState.getValue(FACING));
        }

        static {
            final double[][] defaultShape = new double[][]{
                    new double[]{0.0, 2.0, 14.0, 3.0, 14.0, 16.0},
                    new double[]{13.0, 2.0, 14.0, 16.0, 14.0, 16.0},
                    new double[]{2.0, 0.0, 14.0, 14.0, 3.0, 16.0},
                    new double[]{2.0, 13.0, 14.0, 14.0, 16.0, 16.0},
                    new double[]{7.1587, 2.0, 14.25, 8.8413, 14, 15.75},
                    new double[]{2.0, 7.1587, 14.25, 14, 8.8413, 15.75},
                    new double[]{6.5, 6.5, 14.0, 9.5, 9.5, 16.0}
            };

            for (Direction direction : Direction.values()) {
                double[][] rotatedDimensions = new double[7][];

                int shapeNumber = 0;
                for (double[] dimensions : defaultShape) {
                    Direction.Axis axisOfRotation = !direction.equals(Direction.UP) && !direction.equals(Direction.DOWN)
                            ? Direction.Axis.Y : Direction.Axis.X;

                    Rotation rotation = switch (direction) {
                        case DOWN, EAST -> Rotation.CLOCKWISE_90;
                        case UP, WEST -> Rotation.COUNTERCLOCKWISE_90;
                        case NORTH -> Rotation.CLOCKWISE_180;
                        case SOUTH -> Rotation.NONE;
                    };

                    rotatedDimensions[shapeNumber++] = MetalIBeamBlock.rotateDimensions(axisOfRotation, rotation, dimensions);
                }

                SHAPE_BY_DIRECTION.put(direction, Shapes.or(
                        MetalIBeamBlock.generateShape(rotatedDimensions[0]),
                        MetalIBeamBlock.generateShape(rotatedDimensions[1]),
                        MetalIBeamBlock.generateShape(rotatedDimensions[2]),
                        MetalIBeamBlock.generateShape(rotatedDimensions[3]),
                        MetalIBeamBlock.generateShape(rotatedDimensions[4]),
                        MetalIBeamBlock.generateShape(rotatedDimensions[5]),
                        MetalIBeamBlock.generateShape(rotatedDimensions[6])));
            }
        }
    }

    public static class TireBlock extends WheelBlock {
        public static final Map<Direction, VoxelShape> SHAPE_BY_DIRECTION = new HashMap<>(6);

        public TireBlock() {
            super(Properties
                    .of(Material.CLAY, MaterialColor.GRASS)
                    .sound(SoundType.SHROOMLIGHT)
                    .strength(4.0F)
            );
        }

        @Override
        public void fallOn(Level pLevel, BlockState pState, BlockPos pPos, Entity pEntity, float pFallDistance) {
            if (pEntity.isSuppressingBounce()) {
                super.fallOn(pLevel, pState, pPos, pEntity, pFallDistance);
            } else {
                pEntity.causeFallDamage(pFallDistance, 0.0F, DamageSource.FALL);
            }

        }

        @Override
        public void updateEntityAfterFallOn(BlockGetter pLevel, Entity pEntity) {
            if (pEntity.isSuppressingBounce()) {
                super.updateEntityAfterFallOn(pLevel, pEntity);
            } else {
                Vec3 vec3 = pEntity.getDeltaMovement();
                if (vec3.y < 0.0D) {
                    double d0 = pEntity instanceof LivingEntity ? 1.0D : 0.8D;
                    pEntity.setDeltaMovement(vec3.x, -vec3.y * d0, vec3.z);
                }
            }
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
