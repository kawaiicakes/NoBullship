package io.github.kawaiicakes.nobullship.api;

import com.google.gson.JsonArray;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
    private static final Logger LOGGER = LogUtils.getLogger();

    protected BlockState blockState;
    protected Map<Property<?>, Set<Comparable<?>>> properties = new HashMap<>();
    @Nullable
    protected CompoundTag blockEntityNbtData;
    @Nullable
    protected CompoundTag blockEntityNbtDataStrict;

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

    @Nullable
    public CompoundTag getBlockEntityNbtData() {
        if (this.blockEntityNbtData == null) return null;
        return this.blockEntityNbtData.copy();
    }

    @Nullable
    public CompoundTag getBlockEntityNbtDataStrict() {
        if (this.blockEntityNbtDataStrict == null) return null;
        return this.blockEntityNbtDataStrict.copy();
    }

    /**
     * Stipulates that a given property in the <code>BlockState</code> must be one of the passed values. If a
     * key already exists, the passed value will be added to the <code>Set</code> of "allowed" values for that
     * property.
     */
    public BlockInWorldPredicateBuilder requireProperties(Property<?> property, Set<Comparable<?>> value) {
        if (!this.blockState.getValues().containsKey(property)) throw new IllegalArgumentException(property + " does not belong to " + this.blockState + "!");
        if (value.stream().anyMatch(comparable -> !property.getPossibleValues().contains(comparable))) throw new IllegalArgumentException(value + " cannot be associated with " + property + "!");

        this.properties.putIfAbsent(property, new HashSet<>());
        if (this.properties.containsKey(property)) {
            this.properties.get(property).addAll(value);
        }

        return this;
    }

    /**
     * Stipulates that a given property in the <code>BlockState</code> must be equal to the passed value. If a
     * key already exists, the passed value will be added to the <code>Set</code> of "allowed" values for that
     * property.
     */
    public BlockInWorldPredicateBuilder requireProperty(Property<?> property, Comparable<?> value) {
        if (!this.blockState.getValues().containsKey(property)) throw new IllegalArgumentException(property + " does not belong to " + this.blockState + "!");
        if (!property.getPossibleValues().contains(value)) throw new IllegalArgumentException(value + " cannot be associated with " + property + "!");

        this.properties.putIfAbsent(property, new HashSet<>());
        if (this.properties.containsKey(property)) {
            this.properties.get(property).add(value);
        }

        return this;
    }

    /**
     * Specifies the exact NBT data a <code>BlockEntity</code> must have to allow the built predicate to test true.
     * The passed NBT data will be merged into the existing.
     */
    public BlockInWorldPredicateBuilder requireStrictNbt(CompoundTag tag) {
        if (!(this.blockState.getBlock() instanceof EntityBlock)) throw new IllegalArgumentException(this.blockState + " does not have a block entity, so it cannot have NBT data!");
        if (this.blockEntityNbtDataStrict == null) this.blockEntityNbtDataStrict = new CompoundTag();
        this.blockEntityNbtDataStrict.merge(tag);
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
        if (!(this.blockState.getBlock() instanceof EntityBlock)) throw new IllegalArgumentException(this.blockState + " does not have a block entity, so it cannot have NBT data!");
        if (this.blockEntityNbtData == null) this.blockEntityNbtData = new CompoundTag();
        this.blockEntityNbtData.merge(tag);
        return this;
    }

    /**
     * Returns a <code>BlockInWorldPredicate</code> whose <code>#test</code> looks for the criteria
     * specified by this builder. This overload only works if there are no directional properties specified as
     * a requisite.
     */
    public BlockInWorldPredicate build() {
        return new BlockInWorldPredicate(this.blockState, this.properties, this.blockEntityNbtData, this.blockEntityNbtDataStrict);
    }

    /**
     * Returns the default <code>BlockState</code> of the <code>Block</code> passed to this builder.
     */
    public BlockState getDefaultBlockState() {
        return this.blockState;
    }

    /**
     * Returns a <code>JsonArray</code> containing the values at the passed property.
     */
    public JsonArray getPropertyValuesAsJsonArray(Property<?> property) {
        if (!this.properties.containsKey(property)) throw new IllegalArgumentException("No such property " + property + " exists for this builder!");
        JsonArray toReturn = new JsonArray();
        this.properties.get(property).forEach(value -> toReturn.add(value.toString()));
        return toReturn;
    }

    public boolean isRequiredProperty(Property<?> property) {
        return this.properties.containsKey(property);
    }

    @Nullable
    public CompoundTag toNbt() {
        CompoundTag toReturn = new CompoundTag();

        Tag tag;
        try {
            tag = BlockState.CODEC.encodeStart(NbtOps.INSTANCE, this.blockState).getOrThrow(false, (error) -> {
            });
        } catch (RuntimeException e) {
            LOGGER.error("BlockState could not be serialized to NBT!");
            return null;
        }

        if (!(tag instanceof CompoundTag blockNbt)) {
            LOGGER.error("BlockState could not be serialized to a CompoundTag!");
            return null;
        }
        toReturn.put("blockState", blockNbt);

        if (!this.properties.isEmpty()) {
            ListTag propertiesNbt = new ListTag();

            for (Map.Entry<Property<?>, Set<Comparable<?>>> entry : this.properties.entrySet()) {
                CompoundTag keyPairNbt = new CompoundTag();

                CompoundTag propertyNbt = new CompoundTag();
                propertyNbt.putString("name", entry.getKey().getName());
                propertyNbt.putString("type", entry.getKey().getValueClass().getSimpleName());
                keyPairNbt.put("property", propertyNbt);

                ListTag valuesNbt = new ListTag();
                for (Comparable<?> comparable : entry.getValue()) {
                    valuesNbt.add(StringTag.valueOf(comparable.toString()));
                }
                keyPairNbt.put("values", valuesNbt);

                propertiesNbt.add(keyPairNbt);
            }

            toReturn.put("properties", propertiesNbt);
        }

        if (this.blockEntityNbtData != null) toReturn.put("nbt", this.blockEntityNbtData);
        if (this.blockEntityNbtDataStrict != null) toReturn.put("nbt_strict", this.blockEntityNbtDataStrict);

        return toReturn;
    }

    @Nullable
    public static BlockInWorldPredicateBuilder fromNbt(CompoundTag nbt) {
        BlockState blockstate;
        try {
            blockstate = BlockState.CODEC.parse(NbtOps.INSTANCE, nbt.get("blockState")).getOrThrow(false, null);
        } catch (RuntimeException e) {
            LOGGER.error("Error deserializing BlockInWorldPredicateBuilder from NBT!", e);
            return null;
        }

        BlockInWorldPredicateBuilder toReturn = BlockInWorldPredicateBuilder.of(blockstate.getBlock());

        try {
            if (nbt.get("properties") instanceof ListTag propertiesTag) {
                for (Tag keyPairTag : propertiesTag) {
                    CompoundTag keyPair = (CompoundTag) keyPairTag;
                    CompoundTag propertyTag = ((CompoundTag) keyPairTag).getCompound("property");

                    Property<?> propertyForBlock = blockstate.getProperties()
                            .stream()
                            .filter(property -> property.getName().equals(propertyTag.getString("name"))
                                    && property.getValueClass().getSimpleName().equals(propertyTag.getString("type")))
                            .findFirst()
                            .orElse(null);

                    if (propertyForBlock == null) throw new IllegalArgumentException("Passed NBT does not contain valid properties!");

                    ListTag valuesList = keyPair.getList("values", Tag.TAG_STRING);
                    Set<Comparable<?>> valuesSet = new HashSet<>(valuesList.size());
                    for (Tag string : valuesList) {
                        Comparable<?> comparable = propertyForBlock.getValue(string.getAsString()).orElseThrow();
                        valuesSet.add(comparable);
                    }

                    toReturn.requireProperties(propertyForBlock, valuesSet);
                }
            }
        } catch (RuntimeException e) {
            LOGGER.error("BlockInWorldPredicateBuilder contains malformed property definition!", e);
            return null;
        }

        if (nbt.get("nbt") instanceof CompoundTag nbtTag) toReturn.requireNbt(nbtTag);
        if (nbt.get("nbt_strict") instanceof CompoundTag nbtTagStrict) toReturn.requireNbt(nbtTagStrict);

        return toReturn;
    }
}
