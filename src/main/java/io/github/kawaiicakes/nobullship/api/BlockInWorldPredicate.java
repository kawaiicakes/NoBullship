package io.github.kawaiicakes.nobullship.api;

import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.nobullship.schematic.SchematicRecipe;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Predicate;

import static io.github.kawaiicakes.nobullship.multiblock.MultiblockPattern.CARDINAL;

/**
 * This is a <code>Predicate</code> implementation allowing for dynamic checking of rotations in directional
 * properties. An instance is used and <code>#setFacing</code> is called to rotate the accepted direction values
 * from the original.
 */
public class BlockInWorldPredicate implements Predicate<BlockInWorld> {
    protected static final Logger LOGGER = LogUtils.getLogger();
    public static final BlockInWorldPredicate WILDCARD = new BlockInWorldPredicate(Blocks.AIR, null, null, null, null, null, null) {
        @Override
        public BlockInWorldPredicate setFacing(Direction direction) {
            return this;
        }

        @Override
        public boolean test(BlockInWorld blockInWorld) {
            return true;
        }
    };
    public static final BlockInWorldPredicate AIR = new BlockInWorldPredicate(Blocks.AIR, null, null, null, null, null, null) {
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
    protected final ResourceLocation blockId;
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
    protected BlockInWorldPredicate(@Nullable Block block, @Nullable BlockState blockState, @Nullable TagKey<Block> blockTag, @Nullable ResourceLocation blockId, @Nullable Map<String, Set<String>> properties,
                                    @Nullable CompoundTag blockEntityNbtData, @Nullable CompoundTag blockEntityNbtDataStrict) {
        if (block == null && blockState == null && blockTag == null) throw new IllegalArgumentException("The block, blockstate, and block tag are all null!");

        this.block = block;
        this.blockState = blockState;
        this.blockTag = blockTag;
        this.blockId = blockId;
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
        else if (this.blockState != null) return (blockInWorld) -> {
            // FIXME: this temporary fix for accepting visually-identical stairs
            if (!blockInWorld.getState().is(BlockTags.STAIRS)) return blockInWorld.getState().equals(this.blockState);
            if (this.blockState.equals(blockInWorld.getState())) return true;

            Direction currentFacing = blockInWorld.getState().getValue(StairBlock.FACING);
            StairsShape currentShape = blockInWorld.getState().getValue(StairBlock.SHAPE);

            if (currentShape.equals(StairsShape.STRAIGHT)) return false;

            Direction newFacing = getNewStairsFacing(currentShape, currentFacing);
            StairsShape newShape = switch (currentShape) {
                case STRAIGHT -> throw new RuntimeException();
                case INNER_LEFT -> StairsShape.INNER_RIGHT;
                case INNER_RIGHT -> StairsShape.INNER_LEFT;
                case OUTER_LEFT -> StairsShape.OUTER_RIGHT;
                case OUTER_RIGHT -> StairsShape.OUTER_LEFT;
            };

            BlockState identicalCounterpart = blockInWorld.getState();
            identicalCounterpart = identicalCounterpart.setValue(StairBlock.FACING, newFacing);
            identicalCounterpart = identicalCounterpart.setValue(StairBlock.SHAPE, newShape);

            return this.blockState.equals(identicalCounterpart);
        };
        else if (this.blockTag != null) return (blockInWorld) -> blockInWorld.getState().is(this.blockTag);
        else if (this.blockId != null) return (blockInWorld) -> {
            // exists in case for some reason the builder screws up and ends up leaving $block null
            ResourceLocation rl = ForgeRegistries.BLOCKS.getKey(blockInWorld.getState().getBlock());
            if (rl == null) return false;
            return rl.equals(this.blockId);
        };
        else throw new RuntimeException();
    }

    protected static Predicate<BlockInWorld> checkProperties(@Nullable Map<String, Set<String>> properties, Direction facing) {
        if (properties == null || properties.isEmpty()) return (block) -> true;

        Rotation rotationToNorth = rotationToNorth(facing);

        return (block) -> {
            boolean toReturn = true;
            // I don't know if this cast is fine...
            BlockState rotatedState = block.getState().rotate((LevelAccessor) block.getLevel(), block.getPos(), rotationToNorth);

            for (Map.Entry<String, Set<String>> entry : properties.entrySet()) {
                Property<?> propertyOfBlockInWorld;

                try {
                    propertyOfBlockInWorld = rotatedState.getValues()
                            .keySet()
                            .stream()
                            .filter(property -> property.getName().equals(entry.getKey()))
                            .findFirst()
                            .orElseThrow();
                } catch (IllegalArgumentException | NoSuchElementException e) {
                    // These errors are expected if the property is not found in the blockstate. No message is necessary
                    return false;
                } catch (RuntimeException e) {
                    LOGGER.error("Unexpected error while checking properties!", e);
                    LOGGER.error(e.getMessage());
                    return false;
                }

                String valueAtPropertyOfBlockInWorld;
                try {
                    valueAtPropertyOfBlockInWorld = rotatedState.getValue(propertyOfBlockInWorld).toString();
                } catch (RuntimeException e) {
                    LOGGER.error("Exception encountered!", e);
                    LOGGER.error("Error while determining value at property {} of block {}!", entry.getKey(), block.getPos());
                    LOGGER.error(e.getMessage());
                    return false;
                }

                if (!entry.getValue().contains(valueAtPropertyOfBlockInWorld)) {
                    toReturn = false;
                    break;
                }
            }

            // FIXME: intended temp fix for accepting visually-identical stairs
            if (!toReturn && block.getState().getBlock() instanceof StairBlock && !rotatedState.getValue(StairBlock.SHAPE).equals(StairsShape.STRAIGHT)) {
                StairsShape oldShape = rotatedState.getValue(StairBlock.SHAPE);
                Direction oldFacing = rotatedState.getValue(StairBlock.FACING);

                StairsShape newShape = switch (oldShape) {
                    case STRAIGHT -> throw new RuntimeException();
                    case INNER_LEFT -> StairsShape.INNER_RIGHT;
                    case INNER_RIGHT -> StairsShape.INNER_LEFT;
                    case OUTER_LEFT -> StairsShape.OUTER_RIGHT;
                    case OUTER_RIGHT -> StairsShape.OUTER_LEFT;
                };
                Direction newFacing = getNewStairsFacing(oldShape, oldFacing);

                for (Map.Entry<String, Set<String>> entry : properties.entrySet()) {
                    Property<?> propertyOfBlockInWorld;

                    try {
                        propertyOfBlockInWorld = rotatedState.getValues()
                                .keySet()
                                .stream()
                                .filter(property -> property.getName().equals(entry.getKey()))
                                .findFirst()
                                .orElseThrow();
                    } catch (IllegalArgumentException | NoSuchElementException e) {
                        // These errors are expected if the property is not found in the blockstate. No message is necessary
                        return false;
                    } catch (RuntimeException e) {
                        LOGGER.error("Unexpected error while checking properties!", e);
                        LOGGER.error(e.getMessage());
                        return false;
                    }

                    String valueAtPropertyOfBlockInWorld;
                    try {
                        if (propertyOfBlockInWorld.getName().equals("facing")) {
                            valueAtPropertyOfBlockInWorld = newFacing.getSerializedName();
                        } else if (propertyOfBlockInWorld.getName().equals("shape")) {
                            valueAtPropertyOfBlockInWorld = newShape.getSerializedName();
                        } else {
                            valueAtPropertyOfBlockInWorld = rotatedState.getValue(propertyOfBlockInWorld).toString();
                        }
                    } catch (RuntimeException e) {
                        LOGGER.error("Exception encountered!", e);
                        LOGGER.error("Error while determining value at property {} of block {}!", entry.getKey(), block.getPos());
                        LOGGER.error(e.getMessage());
                        return false;
                    }

                    if (!entry.getValue().contains(valueAtPropertyOfBlockInWorld)) {
                        toReturn = false;
                        break;
                    }

                    toReturn = true;
                }
            }

            return toReturn;
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

    /**
     * Returns the <code>Rotation</code> necessary to rotate the passed argument to face north again.
     * This is written solely for use in the horizontal plane.
     * @param facing the <code>Direction</code> something is facing. Meaningless for values of UP or DOWN.
     * @return the <code>Rotation</code> necessary to point the passed <code>Direction</code> north once more.
     */
    public static Rotation rotationToNorth(Direction facing) {
        return switch (facing) {
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.CLOCKWISE_90;
            case EAST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    public static Direction getNewStairsFacing(StairsShape shape, Direction oldFacing) {
        return switch (shape) {
            case STRAIGHT -> throw new IllegalArgumentException();
            case INNER_LEFT, OUTER_LEFT -> switch (oldFacing) {
                case NORTH -> Direction.WEST;
                case SOUTH -> Direction.EAST;
                case WEST -> Direction.SOUTH;
                case EAST -> Direction.NORTH;
                default -> oldFacing;
            };
            case INNER_RIGHT, OUTER_RIGHT -> switch (oldFacing) {
                case NORTH -> Direction.EAST;
                case SOUTH -> Direction.WEST;
                case WEST -> Direction.NORTH;
                case EAST -> Direction.SOUTH;
                default -> oldFacing;
            };
        };
    }
}
