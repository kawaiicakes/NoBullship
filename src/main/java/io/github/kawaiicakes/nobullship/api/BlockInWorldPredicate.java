package io.github.kawaiicakes.nobullship.api;

import io.github.kawaiicakes.nobullship.schematic.SchematicRecipe;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.github.kawaiicakes.nobullship.multiblock.MultiblockPattern.CARDINAL;
import static net.minecraft.world.level.block.Blocks.AIR;

/**
 * This is a <code>Predicate</code> implementation allowing for dynamic checking of rotations in directional
 * properties. An instance is used and <code>#setFacing</code> is called to rotate the accepted direction values
 * from the original.
 */
public class BlockInWorldPredicate implements Predicate<BlockInWorld> {
    public static final BlockInWorldPredicate WILDCARD = new BlockInWorldPredicate(AIR, null, null, null, null, null) {
        @Override
        public BlockInWorldPredicate setFacing(Direction direction) {
            return this;
        }

        @Override
        public boolean test(BlockInWorld blockInWorld) {
            return !blockInWorld.getState().isAir();
        }
    };

    protected Direction facing;
    @Nullable
    protected final Block block;
    @Nullable
    protected final BlockState blockState;
    @Nullable
    protected final TagKey<Block> blockTag;
    @Nullable // TODO: replace set in map with set of strings pursuant to BlockInWorldPredicateBuilder#L302
    protected final Map<Property<?>, Set<Comparable<?>>> properties;
    @Nullable
    protected final CompoundTag blockEntityNbtData;
    @Nullable
    protected final CompoundTag blockEntityNbtDataStrict;

    /**
     * Creates a new <code>BlockInWorldPredicate</code> facing the given direction. Any directional properties
     * will be rotated accordingly for the test.
     */
    protected BlockInWorldPredicate(@Nullable Block block, @Nullable BlockState blockState, @Nullable TagKey<Block> blockTag, @Nullable Map<Property<?>, Set<Comparable<?>>> properties,
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
        if (this.blockState != null) return basicTest.test(blockInWorld);
        Predicate<BlockInWorld> propertiesPredicate = checkProperties(this.properties, this.facing);
        Predicate<BlockInWorld> nbtPredicateStrict = checkNbtMatch(this.blockEntityNbtDataStrict);
        Predicate<BlockInWorld> nbtPredicate = checkSoftNbtMatch(this.blockEntityNbtData);

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

    protected static Predicate<BlockInWorld> checkProperties(@Nullable Map<Property<?>, Set<Comparable<?>>> properties, Direction facing) {
        if (properties == null || properties.isEmpty()) return (block) -> true;

        return (block) -> {
            for (Map.Entry<Property<?>, Set<Comparable<?>>> entry : properties.entrySet()) {
                final Set<Comparable<?>> checkedValues = entry.getValue();
                Comparable<?> valueOfBlockInWorld;

                try {
                    valueOfBlockInWorld = block.getState().getValue(entry.getKey());
                } catch (IllegalArgumentException e) {
                    return false;
                }

                if (entry.getKey() instanceof DirectionProperty directionProperty
                        && directionProperty.getPossibleValues().containsAll(List.of(CARDINAL))) {

                    Set<Direction> setOfDirections = checkedValues.stream().map(value -> rotateValue(value, facing)).collect(Collectors.toSet());

                    if (!(valueOfBlockInWorld instanceof Direction)) return false;
                    if (!(setOfDirections.contains(valueOfBlockInWorld))) return false;
                    continue;
                }

                if (!checkedValues.contains(valueOfBlockInWorld)) return false;
            }
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

    public static Direction rotateValue(Comparable<?> original, Direction rotated) {
        if (!(original instanceof Direction originalDirection))
            throw new IllegalArgumentException("Argument 'original' is not a direction!");

        return switch (rotated) {
            case SOUTH -> originalDirection.getOpposite();
            case WEST -> originalDirection.getCounterClockWise();
            case EAST -> originalDirection.getClockWise();
            default -> originalDirection;
        };
    }
}
