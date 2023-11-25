package io.github.kawaiicakes.nobullship.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The <code>BlockPredicateBuilder</code> allows easy instantiation of a <code>Predicate{@literal <BlockInWorld>}</code>
 * instance. This is useful for delicate handling of logic in use with <code>MultiblockRecipeBuilder</code>. This class
 * features many convenience methods and default case handling.
 * <br><br>
 * If only passed a <code>Block</code>, the built <code>Predicate{@literal <BlockInWorld>}</code> will return true if
 * <code>BlockInWorld#getState()#is(Block)</code> is true. Check the documentation on the builder methods to see
 * what the possible criteria are.
 */
public class BlockInWorldPredicateBuilder {
    protected BlockState blockState;
    protected Map<Property<?>, Set<Comparable<?>>> properties = new HashMap<>();
    protected Map<Property<?>, Set<Comparable<?>>> strictProperties = new HashMap<>();
    @Nullable
    protected CompoundTag blockEntityNbtData;

    protected BlockInWorldPredicateBuilder(Block block) {
        this.blockState = block.defaultBlockState();
    }

    /**
     * Returns a new <code>BlockInWorldPredicateBuilder</code> that, when built, matches if the predicate is the
     * same type of <code>Block</code> as passed.
     */
    public static BlockInWorldPredicateBuilder of(Block block) {
        return new BlockInWorldPredicateBuilder(block);
    }

    /**
     * Returns a new <code>BlockInWorldPredicateBuilder</code> that, when built, matches if the predicate is the
     * same type of <code>Block</code> as passed. Ensure that <code>ForgeRegistries</code> are loaded when this
     * is called.
     * @param blockLocation the <code>ResourceLocation</code> of the <code>Block</code> to test against.
     */
    public static BlockInWorldPredicateBuilder of(ResourceLocation blockLocation) {
        Block toReturn = ForgeRegistries.BLOCKS.getValue(blockLocation);
        if (toReturn == null) throw new IllegalArgumentException("Block " + blockLocation + " does not exist!");
        return new BlockInWorldPredicateBuilder(toReturn);
    }

    /**
     * Stipulates that at least one of the properties in the <code>BlockState</code> must equal the passed value. If a
     * key already exists, the passed value will be added to the <code>Set</code> of "allowed" values for that
     * property.
     */
    public BlockInWorldPredicateBuilder requireProperty(Property<?> property, Comparable<?> value) {
        if (!this.blockState.getValues().containsKey(property)) throw new IllegalArgumentException(property + " does not belong to " + this.blockState + "!");
        if (!property.getPossibleValues().contains(value)) throw new IllegalArgumentException(value + " cannot be associated with " + property + "!");

        this.properties.computeIfAbsent(property, k -> Set.of(value));
        if (this.properties.containsKey(property)) {
            this.properties.get(property).add(value);
        }

        return this;
    }

    /**
     * Using this indicates the <code>BlockState</code>'s properties must match 1:1. The passed <code>Set</code> of
     * values will be the allowed values for that property; otherwise, the state properties of <code>$blockState</code>
     * are used to "fill in" the remaining properties for the strict match.
     * <br><br>
     * Note that if this has been called, it will take precedence over <code>#requireProperty</code> and render calls
     * to it moot.
     */
    public BlockInWorldPredicateBuilder requireExactProperties(Property<?> property, Set<Comparable<?>> values) {
        if (!this.blockState.getValues().containsKey(property)) throw new IllegalArgumentException(property + " does not belong to " + this.blockState + "!");
        if (values.stream().anyMatch(value -> !property.getPossibleValues().contains(value))) throw new IllegalArgumentException("A value in the passed Set cannot be associated with " + property + "!");

        if (this.strictProperties.isEmpty()) {
            this.blockState.getValues().forEach((key, state) -> this.strictProperties.put(key, Set.of(state)));
        }

        this.strictProperties.put(property, values);
        return this;
    }

    /**
     * Specifies the exact NBT data a <code>BlockEntity</code> must have to allow the built predicate to test true.
     * Note that the passed NBT data will be merged into the existing one in this builder.
     */
    public BlockInWorldPredicateBuilder requireStrictNbt(CompoundTag tag) {
        if (!(this.blockState.getBlock() instanceof EntityBlock)) throw new IllegalArgumentException(this.blockState + " does not have a block entity, so cannot have NBT data!");
        tag.putBoolean("softDependency", false);
        if (this.blockEntityNbtData == null) this.blockEntityNbtData = new CompoundTag();
        this.blockEntityNbtData.merge(tag);
        return this;
    }

    /**
     * Specifies the NBT data a <code>BlockEntity</code> must have to allow the built predicate to test true.
     * Note that the passed NBT data will be merged into the existing one in this builder.
     * <br><br>
     * A non-exact match is permitted; in the case of containers, a match will be returned if the contents of
     * the container meet or exceed the requirements stipulated by the passed NBT, without regard to the placement
     * of the contents.
     */
    public BlockInWorldPredicateBuilder requireNbt(CompoundTag tag) {
        if (!(this.blockState.getBlock() instanceof EntityBlock)) throw new IllegalArgumentException(this.blockState + " does not have a block entity, so cannot have NBT data!");
        tag.putBoolean("softDependency", true);
        if (this.blockEntityNbtData == null) this.blockEntityNbtData = new CompoundTag();
        this.blockEntityNbtData.merge(tag);
        return this;
    }

    /**
     * Returns a <code>Predicate{@literal <BlockInWorld>}</code> whose <code>#test</code> looks for the criteria
     * specified by this builder.
     */
    public Predicate<BlockInWorld> build() {
        Predicate<BlockInWorld> toReturn = BlockInWorld.hasState(state -> state.is(this.blockState.getBlock()));

        if (this.properties.isEmpty() && this.strictProperties.isEmpty() && this.blockEntityNbtData == null)
            return toReturn;

        Predicate<BlockInWorld> propertiesPredicate = (block) -> true;
        if (!this.properties.isEmpty() && this.strictProperties.isEmpty()) {
            propertiesPredicate = (block) -> {
                for (Map.Entry<Property<?>, Set<Comparable<?>>> entry : this.properties.entrySet()) {
                    if (!entry.getValue().contains(block.getState().getValue(entry.getKey()))) return false;
                }
                return true;
            };
        }

        Predicate<BlockInWorld> strictPropertiesPredicate = (block) -> true;
        if (!this.strictProperties.isEmpty())
            strictPropertiesPredicate = (block) -> block.getState()
                    .getValues()
                    .entrySet()
                    .stream()
                    .allMatch(entry -> this.strictProperties.get(entry.getKey()).contains(entry.getValue()));

        Predicate<BlockInWorld> nbtPredicate = (block) -> true;
        if (this.blockEntityNbtData != null) {
            nbtPredicate = (block) -> {
                for (String key : this.blockEntityNbtData.getAllKeys()) {
                    // TODO: implement this and make use of the tag softDependency
                }
                return true;
            };
        }

        return toReturn.and(propertiesPredicate).and(strictPropertiesPredicate).and(nbtPredicate);
    }

    public BlockState getBlockState() {
        return this.blockState;
    }
}
