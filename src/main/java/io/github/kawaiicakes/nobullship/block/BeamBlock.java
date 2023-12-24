package io.github.kawaiicakes.nobullship.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class BeamBlock extends Block {
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
        );
    }
}
