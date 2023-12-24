package io.github.kawaiicakes.nobullship.api;

import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.nobullship.schematic.SchematicRecipe;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.properties.*;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.github.kawaiicakes.nobullship.multiblock.MultiblockPattern.CARDINAL;
import static io.github.kawaiicakes.nobullship.multiblock.SchematicRenderer.BlockIngredient.CARDINAL_NAMES;

/**
 * This is a <code>Predicate</code> implementation allowing for dynamic checking of rotations in directional
 * properties. An instance is used and <code>#setFacing</code> is called to rotate the accepted direction values
 * from the original.
 */
public class BlockInWorldPredicate implements Predicate<BlockInWorld> {
    protected static final Logger LOGGER = LogUtils.getLogger();
    public static final BlockInWorldPredicate WILDCARD = new BlockInWorldPredicate(Blocks.AIR, null, null, null, null, null) {
        @Override
        public BlockInWorldPredicate setFacing(Direction direction) {
            return this;
        }

        @Override
        public boolean test(BlockInWorld blockInWorld) {
            return !blockInWorld.getState().isAir();
        }
    };
    public static final BlockInWorldPredicate AIR = new BlockInWorldPredicate(Blocks.AIR, null, null, null, null, null) {
        @Override
        public BlockInWorldPredicate setFacing(Direction direction) {
            return this;
        }

        @Override
        public boolean test(BlockInWorld blockInWorld) {
            return blockInWorld.getState().isAir();
        }
    };

    protected Direction facing;
    @Nullable
    protected final Block block;
    @Nullable
    protected final BlockState blockState;
    @Nullable
    protected final TagKey<Block> blockTag;
    @Nullable
    protected final Map<String, Set<String>> properties;
    @Nullable
    protected final CompoundTag blockEntityNbtData;
    @Nullable
    protected final CompoundTag blockEntityNbtDataStrict;

    /**
     * Creates a new <code>BlockInWorldPredicate</code> facing the given direction. Any directional properties
     * will be rotated accordingly for the test.
     */
    protected BlockInWorldPredicate(@Nullable Block block, @Nullable BlockState blockState, @Nullable TagKey<Block> blockTag, @Nullable Map<String, Set<String>> properties,
                                    @Nullable CompoundTag blockEntityNbtData, @Nullable CompoundTag blockEntityNbtDataStrict) {
        if (block == null && blockState == null && blockTag == null) throw new IllegalArgumentException("The block, blockstate, and block tag are all null!");

        this.block = block;
        this.blockState = blockState;
        this.blockTag = blockTag;
        this.properties = properties;
        this.blockEntityNbtData = blockEntityNbtData;
        this.blockEntityNbtDataStrict = blockEntityNbtDataStrict;
        this.facing = Direction.SOUTH;
    }

    public BlockInWorldPredicate setFacing(Direction direction) {
        if (!Arrays.asList(CARDINAL).contains(direction)) throw new IllegalArgumentException(direction + " is an invalid Direction!");
        if (this.blockState != null) return this;
        this.facing = direction;
        return this;
    }

    @Override
    public boolean test(BlockInWorld blockInWorld) {
        Predicate<BlockInWorld> basicTest = this.getBasic();
        Predicate<BlockInWorld> propertiesPredicate = checkProperties(this.properties, this.facing);
        Predicate<BlockInWorld> nbtPredicateStrict = checkNbtMatch(this.blockEntityNbtDataStrict);
        Predicate<BlockInWorld> nbtPredicate = checkSoftNbtMatch(this.blockEntityNbtData);

        if (this.blockState != null) return basicTest.test(blockInWorld)
                && nbtPredicateStrict.test(blockInWorld)
                && nbtPredicate.test(blockInWorld);
        return basicTest.test(blockInWorld)
                && propertiesPredicate.test(blockInWorld)
                && nbtPredicateStrict.test(blockInWorld)
                && nbtPredicate.test(blockInWorld);
    }

    protected Predicate<BlockInWorld> getBasic() {
        if (this.block != null) return BlockInWorld.hasState(state -> state.is(this.block));
        else if (this.blockState != null) return (blockInWorld) -> blockInWorld.getState().equals(this.blockState);
        else //noinspection DataFlowIssue (making it to here implies blockTag is not null)
            return (blockInWorld) -> blockInWorld.getState().is(this.blockTag);
    }

    protected static Predicate<BlockInWorld> checkProperties(@Nullable Map<String, Set<String>> properties, Direction facing) {
        if (properties == null || properties.isEmpty()) return (block) -> true;

        return (block) -> {
            Map<String, Boolean> mapOfBooleanDirectionProperties = null;
            Map<String, WallSide> mapOfWallSideProperties = null;

            for (Map.Entry<String, Set<String>> entry : properties.entrySet()) {
                Property<?> propertyOfBlockInWorld;

                try {
                    propertyOfBlockInWorld = block.getState().getValues()
                            .keySet()
                            .stream()
                            .filter(property -> property.getName().equals(entry.getKey()))
                            .findFirst()
                            .orElseThrow();
                } catch (IllegalArgumentException | NoSuchElementException e) {
                    // These errors are expected if the property is not found in the blockstate.
                    return false;
                } catch (RuntimeException e) {
                    LOGGER.error("Unexpected error while checking properties!", e);
                    LOGGER.error(e.getMessage());
                    return false;
                }

                String valueAtPropertyOfBlockInWorld;
                try {
                    valueAtPropertyOfBlockInWorld = block.getState().getValue(propertyOfBlockInWorld).toString();
                } catch (RuntimeException e) {
                    LOGGER.error("Exception encountered!", e);
                    LOGGER.error("Error while determining value at property {} of block {}!", entry.getKey(), block.getPos());
                    LOGGER.error(e.getMessage());
                    return false;
                }

                // TODO: account for more states that indicate direction; such as signs (int property), etc.
                // TODO: maybe use of generics and extraction of this shit to a new method would be cleaner
                if (propertyOfBlockInWorld instanceof DirectionProperty) {
                    Set<Direction> setOfDirectionsDefault;

                    try {
                        setOfDirectionsDefault = entry.getValue()
                                .stream()
                                .map(Direction::byName)
                                .collect(Collectors.toSet());

                        if (setOfDirectionsDefault.contains(null)) throw new IllegalArgumentException("Value set contains invalid string!");
                    } catch (RuntimeException e) {
                        LOGGER.error("Error casting values to direction!", e);
                        return false;
                    }

                    Set<Direction> setOfDirections = setOfDirectionsDefault.stream().map(value -> rotateDirection(value, facing)).collect(Collectors.toSet());

                    Direction directionOfBlockInWorld = Direction.byName(valueAtPropertyOfBlockInWorld);
                    if (directionOfBlockInWorld == null) {
                        LOGGER.error("Value {} is not a direction for property {} at {}!", valueAtPropertyOfBlockInWorld, entry.getKey(), block.getPos());
                        return false;
                    }

                    if (!(setOfDirections.contains(directionOfBlockInWorld))) return false;
                } else if (propertyOfBlockInWorld instanceof EnumProperty<?> enumProperty) {
                    if (enumProperty.getValueClass().equals(Direction.Axis.class)) {
                        Set<Direction.Axis> setOfValidAxesDefault;

                        try {
                            setOfValidAxesDefault = entry.getValue()
                                    .stream()
                                    .map(Direction.Axis::byName)
                                    .collect(Collectors.toSet());

                            if (setOfValidAxesDefault.contains(null))
                                throw new IllegalArgumentException("Value set contains invalid string!");
                        } catch (RuntimeException e) {
                            LOGGER.error("Error casting values to axis!", e);
                            return false;
                        }

                        if (setOfValidAxesDefault.contains(Direction.Axis.X) && setOfValidAxesDefault.contains(Direction.Axis.Z))
                            continue;

                        Set<Direction.Axis> setOfValidAxes = setOfValidAxesDefault.stream().map(value -> rotateAxis(value, facing)).collect(Collectors.toSet());

                        Direction.Axis axisOfBlockInWorld = Direction.Axis.byName(valueAtPropertyOfBlockInWorld);
                        if (axisOfBlockInWorld == null) {
                            LOGGER.error("Value {} is not a valid axis for property {} at {}!", valueAtPropertyOfBlockInWorld, entry.getKey(), block.getPos());
                            return false;
                        }

                        if (!(setOfValidAxes.contains(axisOfBlockInWorld))) return false;
                    } else if (enumProperty.getValueClass().equals(WallSide.class)) {
                        //noinspection unchecked
                        EnumProperty<WallSide> axisProperty = (EnumProperty<WallSide>) enumProperty;
                        if (mapOfWallSideProperties == null) mapOfWallSideProperties = new HashMap<>();
                        WallSide wallSide;

                        try {
                            wallSide = WallSide.valueOf(valueAtPropertyOfBlockInWorld);
                        } catch (IllegalArgumentException e) {
                            LOGGER.error("Illegal value {} is not a WallSide value!", valueAtPropertyOfBlockInWorld);
                            return false;
                        }

                        mapOfWallSideProperties.put(axisProperty.getName(), wallSide);
                    }
                } else if (propertyOfBlockInWorld instanceof BooleanProperty booleanProperty) {
                    if (!CARDINAL_NAMES.contains(booleanProperty.getName())) continue;
                    if (mapOfBooleanDirectionProperties == null) mapOfBooleanDirectionProperties = new HashMap<>();
                    mapOfBooleanDirectionProperties.put(booleanProperty.getName(), Boolean.parseBoolean(valueAtPropertyOfBlockInWorld));
                }

                if (!entry.getValue().contains(valueAtPropertyOfBlockInWorld)) return false;
            }

            // TODO: handling of maps declared in this block

            return true;
        };
    }

    protected static Predicate<BlockInWorld> checkNbtMatch(@Nullable CompoundTag blockEntityNbtDataStrict) {
        if (blockEntityNbtDataStrict == null) return block -> true;

        return (block) -> {
            //noinspection DataFlowIssue ($blockEntityNbtDataStrict being non-null implies the block has an entity.)
            CompoundTag blockNbt = block.getEntity().saveWithFullMetadata();

            for (String key : blockEntityNbtDataStrict.getAllKeys()) {
                if (!blockNbt.contains(key)) return false;
                //noinspection DataFlowIssue
                if (!blockNbt.get(key).equals(blockEntityNbtDataStrict.get(key))) return false;
            }

            return true;
        };
    }

    /**
     * Helper method which generates a <code>{@literal Predicate<BlockInWorld>}</code> which checks for the presence
     * of NBT data with soft scrutiny. As an example to what the return does, containers need not match the NBT exactly;
     * only the count of the contents should meet or exceed what is stipulated in NBT.
     */
    protected static Predicate<BlockInWorld> checkSoftNbtMatch(@Nullable CompoundTag blockEntityNbtData) {
        if (blockEntityNbtData == null) return block -> true;

        return (block) -> {
            //noinspection DataFlowIssue ($blockEntityNbtData being non-null implies the block has an entity.)
            CompoundTag blockNbt = block.getEntity().saveWithFullMetadata();

            for (String key : blockEntityNbtData.getAllKeys()) {
                if (!key.equals("Items")) {
                    if (!blockNbt.contains(key)) return false;
                    //noinspection DataFlowIssue
                    if (!blockNbt.get(key).equals(blockEntityNbtData.get(key))) return false;
                    continue;
                }

                ListTag requirementContents = blockEntityNbtData.getList("Items", Tag.TAG_COMPOUND);
                if (requirementContents.isEmpty()) return false;

                ListTag containerContents = blockNbt.getList("Items", Tag.TAG_COMPOUND);
                if (containerContents.isEmpty() && !requirementContents.isEmpty()) return false;

                NonNullList<ItemStack> requirementsAsList = NonNullList.createWithCapacity(requirementContents.size());
                for (Tag slotTag : requirementContents) {
                    ItemStack slotStack = ItemStack.of((CompoundTag) slotTag);
                    requirementsAsList.add(slotStack);
                }
                List<ItemStack> summedRequirements = SchematicRecipe.getSummedContents(requirementsAsList);

                NonNullList<ItemStack> contentsAsList = NonNullList.createWithCapacity(containerContents.size());
                for (Tag slotTag : containerContents) {
                    ItemStack slotStack = ItemStack.of((CompoundTag) slotTag);
                    contentsAsList.add(slotStack);
                }
                List<ItemStack> summedContents = SchematicRecipe.getSummedContents(contentsAsList);

                if (!SchematicRecipe.compareSummedContents(summedRequirements, summedContents)) return false;
            }

            return true;
        };
    }

    public static Direction rotateDirection(Comparable<?> original, Direction rotated) {
        if (!(original instanceof Direction originalDirection))
            throw new IllegalArgumentException("Argument 'original' is not a direction!");

        return switch (rotated) {
            case SOUTH -> originalDirection.getOpposite();
            case WEST -> originalDirection.getCounterClockWise();
            case EAST -> originalDirection.getClockWise();
            default -> originalDirection;
        };
    }

    public static Direction.Axis rotateAxis(Direction.Axis axis, Direction rotated) {
        if (axis.isVertical()) return axis;
        return switch (rotated) {
            case EAST, WEST -> axis.equals(Direction.Axis.X) ? Direction.Axis.Z : Direction.Axis.X;
            default -> axis;
        };
    }
}
