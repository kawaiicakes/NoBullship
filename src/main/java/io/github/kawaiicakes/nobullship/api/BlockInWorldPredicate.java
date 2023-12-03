package io.github.kawaiicakes.nobullship.api;

import io.github.kawaiicakes.nobullship.schematic.SchematicRecipe;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

import static io.github.kawaiicakes.nobullship.multiblock.MultiblockPattern.CARDINAL;

/**
 * This is a <code>Predicate</code> implementation allowing for dynamic checking of rotations in directional
 * properties. An instance is used and <code>#setFacing</code> is called to rotate the accepted direction values
 * from the original.
 */
public class BlockInWorldPredicate implements Predicate<BlockInWorld> {
    @SuppressWarnings("DataFlowIssue")
    public static final BlockInWorldPredicate WILDCARD = new BlockInWorldPredicate(null, null, null, null) {
        @Override
        public boolean test(BlockInWorld blockInWorld) {
            return true;
        }
    };

    protected Direction facing;
    protected final BlockState block;
    @Nullable
    protected final Map<Property<?>, Set<Comparable<?>>> properties;
    @Nullable
    protected final CompoundTag blockEntityNbtData;
    @Nullable
    protected final CompoundTag blockEntityNbtDataStrict;

    /**
     * Creates a new <code>BlockInWorldPredicate</code> facing the given direction. Any directional properties
     * will be rotated accordingly for the test.
     */
    protected BlockInWorldPredicate(BlockState block, @Nullable Map<Property<?>, Set<Comparable<?>>> properties,
                                    @Nullable CompoundTag blockEntityNbtData, @Nullable CompoundTag blockEntityNbtDataStrict) {
        this.block = block;
        this.properties = properties;
        this.blockEntityNbtData = blockEntityNbtData;
        this.blockEntityNbtDataStrict = blockEntityNbtDataStrict;
        this.facing = Direction.SOUTH;
    }

    public BlockInWorldPredicate setFacing(Direction direction) {
        if (!Arrays.asList(CARDINAL).contains(direction)) throw new IllegalArgumentException(direction + " is an invalid Direction!");
        this.facing = direction;
        return this;
    }

    @Override
    public boolean test(BlockInWorld blockInWorld) {
        Predicate<BlockInWorld> basicTest = BlockInWorld.hasState(state -> state.is(this.block.getBlock()));
        Predicate<BlockInWorld> propertiesPredicate = checkProperties(this.properties, this.facing);
        Predicate<BlockInWorld> nbtPredicateStrict = checkNbtMatch(this.blockEntityNbtDataStrict);
        Predicate<BlockInWorld> nbtPredicate = checkSoftNbtMatch(this.blockEntityNbtData);

        return basicTest.test(blockInWorld)
                && propertiesPredicate.test(blockInWorld)
                && nbtPredicateStrict.test(blockInWorld)
                && nbtPredicate.test(blockInWorld);
    }

    protected static Predicate<BlockInWorld> checkProperties(@Nullable Map<Property<?>, Set<Comparable<?>>> properties, Direction facing) {
        Predicate<BlockInWorld> propertiesPredicate = (block) -> true;
        if (properties != null && !properties.isEmpty()) {
            propertiesPredicate = (block) -> {
                for (Map.Entry<Property<?>, Set<Comparable<?>>> entry : properties.entrySet()) {
                    // TODO: implement facing (dynamic direction checks)
                    if (!entry.getValue().contains(block.getState().getValue(entry.getKey()))) return false;
                }
                return true;
            };
        }
        return propertiesPredicate;
    }

    protected static Predicate<BlockInWorld> checkNbtMatch(@Nullable CompoundTag blockEntityNbtDataStrict) {
        Predicate<BlockInWorld> nbtPredicateStrict = (block) -> true;
        if (blockEntityNbtDataStrict != null) {
            nbtPredicateStrict = (block) -> {
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
        return nbtPredicateStrict;
    }

    /**
     * Helper method which generates a <code>{@literal Predicate<BlockInWorld>}</code> which checks for the presence
     * of NBT data with soft scrutiny. As an example to what the return does, containers need not match the NBT exactly;
     * only the count of the contents should meet or exceed what is stipulated in NBT.
     */
    protected static Predicate<BlockInWorld> checkSoftNbtMatch(@Nullable CompoundTag blockEntityNbtData) {
        Predicate<BlockInWorld> nbtPredicate = block -> true;

        if (blockEntityNbtData != null) {
            nbtPredicate = (block) -> {
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

        return nbtPredicate;
    }
}
