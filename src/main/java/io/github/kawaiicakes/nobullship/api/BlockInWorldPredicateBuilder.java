package io.github.kawaiicakes.nobullship.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Direction;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

import static net.minecraftforge.registries.ForgeRegistries.BLOCKS;

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

    @Nullable
    protected Block block;
    @Nullable
    protected BlockState blockState;
    @Nullable
    protected TagKey<Block> blockTag;
    protected boolean exactMatch = false;
    protected Map<Property<?>, Set<Comparable<?>>> properties = new HashMap<>();
    @Nullable
    protected CompoundTag blockEntityNbtData;
    @Nullable
    protected CompoundTag blockEntityNbtDataStrict;

    protected BlockInWorldPredicateBuilder(Block block) {
        this.block = block;
        this.blockState = null;
        this.blockTag = null;
    }

    protected BlockInWorldPredicateBuilder(BlockState blockState) {
        this.block = blockState.getBlock();
        this.blockState = blockState;
        this.blockTag = null;
        this.exactMatch = true;
    }

    protected BlockInWorldPredicateBuilder(TagKey<Block> blockTag) {
        this.block = null;
        this.blockState = null;
        this.blockTag = blockTag;
    }

    /**
     * Returns a new <code>BlockInWorldPredicateBuilder</code> that matches for the passed <code>Block</code>.
     */
    public static BlockInWorldPredicateBuilder of(Block block) {
        return new BlockInWorldPredicateBuilder(block);
    }

    /**
     * Returns a new <code>BlockInWorldPredicateBuilder</code> that looks for an exact match to the
     * passed <code>BlockState</code>. Using this <code>#of</code> overload will disable the functionality
     * of the rest of the builder.
     */
    public static BlockInWorldPredicateBuilder of(BlockState blockState) {
        return new BlockInWorldPredicateBuilder(blockState);
    }

    /**
     * Returns a new <code>BlockInWorldPredicateBuilder</code> that looks for a match to a block tag.
     */
    public static BlockInWorldPredicateBuilder of(TagKey<Block> blockTag) {
        return new BlockInWorldPredicateBuilder(blockTag);
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
        if (this.exactMatch) throw new UnsupportedOperationException("This builder is already looking for an exact match to the passed BlockState!");
        if (this.block != null) {
            if (!this.block.defaultBlockState().getValues().containsKey(property))
                throw new IllegalArgumentException(property + " does not belong to " + this.block.defaultBlockState() + "!");
            if (value.stream().anyMatch(comparable -> !property.getPossibleValues().contains(comparable)))
                throw new IllegalArgumentException(value + " cannot be associated with " + property + "!");
        }

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
        if (this.exactMatch) throw new UnsupportedOperationException("This builder is already looking for an exact match to the passed BlockState!");
        if (this.block != null) {
            if (!this.block.defaultBlockState().getValues().containsKey(property))
                throw new IllegalArgumentException(property + " does not belong to " + this.block.defaultBlockState() + "!");
            if (!property.getPossibleValues().contains(value))
                throw new IllegalArgumentException(value + " cannot be associated with " + property + "!");
        }

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
        if (!(this.block instanceof EntityBlock)) throw new IllegalArgumentException(this.block + " cannot have a block entity, so it cannot have NBT data!");
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
        if (!(this.block instanceof EntityBlock)) throw new IllegalArgumentException(this.block + " cannot have a block entity, so it cannot have NBT data!");
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
        Block blockToArg = this.block;
        BlockState stateToArg = null;
        if (this.exactMatch) stateToArg = this.blockState;
        else if (this.blockTag != null) blockToArg = null;
        return new BlockInWorldPredicate(blockToArg, stateToArg, this.blockTag, this.properties, this.blockEntityNbtData, this.blockEntityNbtDataStrict);
    }

    /**
     * Returns the <code>BlockState</code> associated with this builder.
     * Returns <code>null</code> if this builder matches for a block tag.
     */
    @Nullable
    public BlockState getBlockState() {
        if (this.blockTag != null) return null;
        return this.blockState;
    }

    @Nullable
    public String getBlockTagAsString() {
        return this.blockTag == null ? null : this.blockTag.toString();
    }

    @Nullable
    public String getBlockAsString() {
        return this.block == null ? null : this.block.toString();
    }

    public boolean isExactMatch() {
        return this.exactMatch;
    }

    @Nullable
    public JsonObject toJson() {
        JsonObject toReturn = new JsonObject();

        if (this.isExactMatch()) {
            JsonElement blockStateExact =
                    BlockState.CODEC.encodeStart(JsonOps.INSTANCE, this.getBlockState()).getOrThrow(false, LOGGER::error);
            toReturn.add("blockstate", blockStateExact);
        } else if (this.getBlockState() == null) {
            toReturn.addProperty("block_tag", this.getBlockTagAsString());
        } else {
            // Given where this method is being called, it's impossible for the registry to not be loaded.
            toReturn.addProperty("block", this.getBlockAsString());
        }

        JsonObject properties = this.getPropertiesAsJson();
        if (properties == null) {
            LOGGER.error("Error serializing predicate!");
            throw new IllegalArgumentException();
        }

        JsonObject nbt = new JsonObject();
        JsonObject nbtStrict = new JsonObject();
        Tag nbtForParse = this.getBlockEntityNbtData();
        Tag nbtStrictForParse = this.getBlockEntityNbtDataStrict();
        if (nbtForParse != null) nbt.add("nbt", NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, nbtForParse));
        if (nbtStrictForParse != null) nbtStrict.add("nbt_strict", NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, nbtStrictForParse));

        if (properties.size() != 0) toReturn.add("state", properties);
        if (nbt.size() != 0) toReturn.add("nbt", nbt);
        if (nbtStrict.size() != 0) toReturn.add("nbt_strict", nbtStrict);

        return toReturn;
    }

    public static <T extends Enum<T> & StringRepresentable> BlockInWorldPredicateBuilder fromJson(JsonObject serializedBuilder) throws IllegalArgumentException {
        BlockInWorldPredicateBuilder toReturn;

        JsonElement mappedCharacterBlock = serializedBuilder.getAsJsonObject().get("block");
        JsonElement mappedCharacterBlockState = serializedBuilder.getAsJsonObject().get("blockstate");
        JsonElement mappedCharacterBlockTag = serializedBuilder.getAsJsonObject().get("block_tag");

        byte nonNullCount = 0;
        if (mappedCharacterBlock != null) nonNullCount++;
        if (mappedCharacterBlockState != null) nonNullCount++;
        if (mappedCharacterBlockTag != null) nonNullCount++;

        if (nonNullCount == 0) {
            LOGGER.error("There is no block associated with this recipe!");
            throw new IllegalArgumentException();
        }

        if (nonNullCount > 1) {
            LOGGER.error("More than one predicate block exists for this recipe!");
            throw new IllegalArgumentException();
        }

        JsonObject propertyJson = serializedBuilder.getAsJsonObject().getAsJsonObject("state");
        JsonObject nbtJsonStrict = serializedBuilder.getAsJsonObject().getAsJsonObject("nbt_strict");
        JsonObject nbtJson = serializedBuilder.getAsJsonObject().getAsJsonObject("nbt");

        if (mappedCharacterBlock != null) {
            ResourceLocation blockLocation = new ResourceLocation(mappedCharacterBlock.getAsString());
            Block block = RegistryObject.create(blockLocation, BLOCKS).get();

            if ((nbtJsonStrict != null || nbtJson != null) && !(block instanceof EntityBlock)) {
                LOGGER.error("Block {} does not have a block entity and cannot hold NBT data!", block);
                throw new IllegalArgumentException();
            }

            if (propertyJson == null && nbtJsonStrict == null && nbtJson == null) {
                toReturn = (BlockInWorldPredicateBuilder.of(block));
                return toReturn;
            }

            if (propertyJson == null) propertyJson = new JsonObject();

            final Map<Property<?>, Set<String>> deserializedProperties = new HashMap<>(propertyJson.size());
            for (Map.Entry<String, JsonElement> stateEntry : propertyJson.entrySet()) {
                if (!stateEntry.getValue().isJsonArray()) {
                    LOGGER.error("Values for property {} are not a valid JSON array!", stateEntry.getKey());
                    throw new IllegalArgumentException();
                }

                Property<?> property = block.getStateDefinition().getProperty(stateEntry.getKey());
                if (property == null) {
                    LOGGER.error("Property {} does not exist for {}", stateEntry.getKey(), blockLocation);
                    throw new IllegalArgumentException();
                }

                if (propertyJson.get(stateEntry.getKey()) == null) {
                    // wtf is this doing??? why did I write this? It seems pretty roundabout...
                    throw new IllegalArgumentException();
                }

                Set<String> propertyValues = new HashSet<>();
                for (JsonElement element : stateEntry.getValue().getAsJsonArray()) {
                    if (!element.isJsonPrimitive()) {
                        LOGGER.error("Element {} in value definition for property {} is not a valid primitive!", element, stateEntry.getKey());
                        throw new IllegalArgumentException();
                    }
                    propertyValues.add(element.getAsString());
                }

                deserializedProperties.put(property, propertyValues);
            }

            BlockInWorldPredicateBuilder predicateBuilder = BlockInWorldPredicateBuilder.of(block);
            for (Map.Entry<Property<?>, Set<String>> stateEntry : deserializedProperties.entrySet()) {
                predicateBuilder.requireProperties(stateEntry.getKey(),
                        stateEntry.getValue()
                                .stream()
                                .map(stateEntry.getKey()::getValue)
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .collect(Collectors.toSet())
                );
            }

            if (nbtJsonStrict != null) {
                CompoundTag nbtForPredicate = (CompoundTag) JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, nbtJsonStrict);
                predicateBuilder.requireStrictNbt(nbtForPredicate);
            }

            if (nbtJson != null) {
                CompoundTag nbtForPredicate = (CompoundTag) JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, nbtJson);
                predicateBuilder.requireNbt(nbtForPredicate);
            }

            toReturn = predicateBuilder;
        } else if (mappedCharacterBlockState != null) {
            BlockState deserializedBlockState = BlockState.CODEC.parse(JsonOps.INSTANCE, mappedCharacterBlockState).getOrThrow(false, LOGGER::error);
            BlockInWorldPredicateBuilder predicateBuilder = BlockInWorldPredicateBuilder.of(deserializedBlockState);

            if (propertyJson != null) {
                throw new IllegalArgumentException("Properties attempted to be defined for an exact blockstate match!");
            }

            if (nbtJsonStrict != null) {
                CompoundTag nbtForPredicate = (CompoundTag) JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, nbtJsonStrict);
                predicateBuilder.requireStrictNbt(nbtForPredicate);
            }

            if (nbtJson != null) {
                CompoundTag nbtForPredicate = (CompoundTag) JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, nbtJson);
                predicateBuilder.requireNbt(nbtForPredicate);
            }

            toReturn = predicateBuilder;
        } else {
            TagKey<Block> blockTag = BlockTags.create(new ResourceLocation(mappedCharacterBlockTag.getAsString()));

            if (propertyJson == null && nbtJsonStrict == null && nbtJson == null) {
                toReturn = (BlockInWorldPredicateBuilder.of(blockTag));
                return toReturn;
            }

            if (propertyJson == null) propertyJson = new JsonObject();

            final Map<Property<?>, Set<String>> deserializedProperties = new HashMap<>(propertyJson.size());
            for (Map.Entry<String, JsonElement> stateEntry : propertyJson.entrySet()) {
                if (!(stateEntry.getValue() instanceof JsonObject stateObject)) {
                    LOGGER.error("Values for property {} are not a valid JSON object!", stateEntry.getKey());
                    throw new IllegalArgumentException();
                }

                if (propertyJson.get(stateEntry.getKey()) == null) {
                    // wtf is this doing??? why did I write this? It seems pretty roundabout...
                    throw new IllegalArgumentException();
                }

                JsonObject propertyType = stateObject.getAsJsonObject("property_definition");
                JsonArray propertyValuesArray = stateObject.getAsJsonArray("values");

                if (propertyType.getAsString().isEmpty() || propertyValuesArray.isEmpty()) {
                    LOGGER.error("Type or values for property {} could not be found!", stateEntry.getKey());
                    throw new IllegalArgumentException();
                }

                Property<?> property;

                try {
                    switch (propertyType.getAsJsonObject("type").getAsString()) {
                        case "int" -> {
                            JsonElement max = propertyType.get("max");
                            JsonElement min = propertyType.get("min");

                            if (max == null || min == null) {
                                LOGGER.error("Max/min undefined for int property {}!", stateEntry.getKey());
                                throw new IllegalArgumentException();
                            }

                            property = IntegerProperty.create(stateEntry.getKey(), min.getAsInt(), max.getAsInt());
                        }
                        case "boolean" -> property = BooleanProperty.create(stateEntry.getKey());
                        case "enum" -> {
                            JsonElement clazzName = propertyType.get("class");

                            if (clazzName == null || !clazzName.isJsonPrimitive()) throw new IllegalArgumentException();

                            Class<T> clazz;
                            try {
                                //noinspection unchecked
                                clazz = (Class<T>) Class.forName("net.minecraft.world.level.block.state.properties." + clazzName.getAsString());
                            } catch (ClassNotFoundException | ClassCastException e) {
                                try {
                                    //noinspection unchecked
                                    clazz = (Class<T>) Class.forName(clazzName.getAsString());
                                } catch (ClassNotFoundException f) {
                                    LOGGER.error("No class could be found while deserializing enum property {}!", stateEntry.getKey());
                                    throw new RuntimeException(f);
                                } catch (RuntimeException g) {
                                    LOGGER.error("Class unable to be casted while deserializing enum property {}!", stateEntry.getKey());
                                    throw new RuntimeException(g);
                                }
                            }

                            JsonArray possibleValues = propertyType.getAsJsonArray("possible_values");
                            Set<T> possibleEnums = new HashSet<>(possibleValues.size());
                            for (JsonElement element : possibleValues) {
                                if (!element.isJsonPrimitive()) throw new IllegalArgumentException();
                                T enumValue = Enum.valueOf(clazz, element.getAsString());

                                possibleEnums.add(enumValue);
                            }

                            property = EnumProperty.create(stateEntry.getKey(), clazz, possibleEnums);
                        }
                        case "direction" -> {
                            JsonArray possibleValues = propertyType.getAsJsonArray("possible_values");
                            Set<Direction> possibleDirections = new HashSet<>(possibleValues.size());
                            for (JsonElement element : possibleValues) {
                                if (!element.isJsonPrimitive()) throw new IllegalArgumentException();
                                Direction direction = Direction.byName(element.getAsString());
                                if (direction == null) throw new IllegalArgumentException();

                                possibleDirections.add(direction);
                            }

                            property = DirectionProperty.create(stateEntry.getKey(), possibleDirections);
                        }
                        default -> {
                            LOGGER.error("Unrecognized property type {}!", propertyType.getAsString());
                            throw new IllegalArgumentException();
                        }
                    }
                } catch (RuntimeException e) {
                    LOGGER.error("Exception:", e);
                    LOGGER.error("An error occurred while deserializing properties!");
                    LOGGER.error(e.getMessage());
                    throw new IllegalArgumentException();
                }

                Set<String> propertyValues = new HashSet<>();
                for (JsonElement element : propertyValuesArray) {
                    if (!element.isJsonPrimitive()) {
                        LOGGER.error("Element {} in value definition for property {} is not a valid primitive!", element, stateEntry.getKey());
                        throw new IllegalArgumentException();
                    }
                    propertyValues.add(element.getAsString());
                }

                deserializedProperties.put(property, propertyValues);
            }

            BlockInWorldPredicateBuilder predicateBuilder = BlockInWorldPredicateBuilder.of(blockTag);
            for (Map.Entry<Property<?>, Set<String>> stateEntry : deserializedProperties.entrySet()) {
                predicateBuilder.requireProperties(stateEntry.getKey(),
                        stateEntry.getValue()
                                .stream()
                                .map(stateEntry.getKey()::getValue)
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .collect(Collectors.toSet())
                );
            }

            if (nbtJsonStrict != null) {
                CompoundTag nbtForPredicate = (CompoundTag) JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, nbtJsonStrict);
                predicateBuilder.requireStrictNbt(nbtForPredicate);
            }

            if (nbtJson != null) {
                CompoundTag nbtForPredicate = (CompoundTag) JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, nbtJson);
                predicateBuilder.requireNbt(nbtForPredicate);
            }

            toReturn = predicateBuilder;
        }

        return toReturn;
    }

    @Nullable
    public JsonObject getPropertiesAsJson() {
        JsonObject toReturn = new JsonObject();
        if (this.exactMatch) return toReturn;

        if (this.block != null) {
            for (Map.Entry<Property<?>, Set<Comparable<?>>> propertyEntry : this.properties.entrySet()) {
                Property<?> property = propertyEntry.getKey();
                String canonicalPropertyName = propertyEntry.getKey().getClass().getCanonicalName();

                toReturn.addProperty("property_name", propertyEntry.getKey().getName());
                toReturn.addProperty("qualified_name", canonicalPropertyName);
                toReturn.addProperty("value_type", propertyEntry.getKey().getValueClass().getCanonicalName());

                try {
                    switch (canonicalPropertyName) {
                        case "net.minecraft.world.level.block.state.properties.IntegerProperty" -> {
                            IntegerProperty integerProperty = (IntegerProperty) property;
                            toReturn.addProperty("max", integerProperty.getPossibleValues().stream().max(Comparator.naturalOrder()).orElseThrow());
                            toReturn.addProperty("min", integerProperty.getPossibleValues().stream().min(Comparator.naturalOrder()).orElseThrow());
                        }
                        case "net.minecraft.world.level.block.state.properties.BooleanProperty" -> {
                        }
                        case "net.minecraft.world.level.block.state.properties.EnumProperty", "net.minecraft.world.level.block.state.properties.DirectionProperty" -> {
                            EnumProperty<?> enumProperty = (EnumProperty<?>) property;
                            JsonArray validValues = new JsonArray(enumProperty.getPossibleValues().size());
                            for (StringRepresentable value : enumProperty.getPossibleValues()) {
                                validValues.add(value.getSerializedName());
                            }
                            toReturn.add("possible_values", validValues);
                        }
                        default -> {
                            LOGGER.error("Unrecognized Property instance {} failed to be serialized to NBT!", canonicalPropertyName);
                            return null;
                        }
                    }
                } catch (ClassCastException e) {
                    LOGGER.error("Class cast exception while handling property: {}!", e.getMessage());
                    return null;
                } catch (NoSuchElementException e) {
                    LOGGER.error("No such integer for max/min in IntegerProperty exists!", e);
                    return null;
                } catch (RuntimeException e) {
                    LOGGER.error("Error during property serialization!", e);
                    return null;
                }
            }
        } else {
            for (Map.Entry<Property<?>, Set<Comparable<?>>> propertyEntry : this.properties.entrySet()) {
                JsonObject propertyDefinition = new JsonObject();
                JsonArray arrayValues = new JsonArray();
                Property<?> property = propertyEntry.getKey();
                String canonicalPropertyName = propertyEntry.getKey().getClass().getCanonicalName();

                toReturn.addProperty("property_name", propertyEntry.getKey().getName());
                toReturn.addProperty("qualified_name", canonicalPropertyName);
                toReturn.addProperty("value_type", propertyEntry.getKey().getValueClass().getCanonicalName());

                try {
                    switch (canonicalPropertyName) {
                        case "net.minecraft.world.level.block.state.properties.IntegerProperty" -> {
                            IntegerProperty integerProperty = (IntegerProperty) property;
                            propertyDefinition.addProperty("type", "int");
                            propertyDefinition.addProperty("max", integerProperty.getPossibleValues().stream().max(Comparator.naturalOrder()).orElseThrow());
                            propertyDefinition.addProperty("min", integerProperty.getPossibleValues().stream().min(Comparator.naturalOrder()).orElseThrow());
                        }
                        case "net.minecraft.world.level.block.state.properties.BooleanProperty" -> propertyDefinition.addProperty("type", "boolean");
                        case "net.minecraft.world.level.block.state.properties.EnumProperty" -> {
                            EnumProperty<?> enumProperty = (EnumProperty<?>) property;
                            propertyDefinition.addProperty("type", "enum");
                            propertyDefinition.addProperty("class", enumProperty.getValueClass().getCanonicalName());

                            JsonArray validValues = new JsonArray(enumProperty.getPossibleValues().size());
                            for (StringRepresentable value : enumProperty.getPossibleValues()) {
                                validValues.add(value.getSerializedName());
                            }
                            toReturn.add("possible_values", validValues);
                        }
                        case "net.minecraft.world.level.block.state.properties.DirectionProperty" -> {
                            DirectionProperty directionProperty = (DirectionProperty) property;
                            propertyDefinition.addProperty("type", "direction");
                            JsonArray possibleDirections = new JsonArray();
                            for (Direction direction : directionProperty.getPossibleValues()) {
                                possibleDirections.add(direction.getSerializedName());
                            }
                            propertyDefinition.add("possible_values", possibleDirections);
                        }
                        default -> {
                            LOGGER.error("Unrecognized Property instance {} failed to be serialized to NBT!", canonicalPropertyName);
                            return null;
                        }
                    }
                } catch (ClassCastException e) {
                    LOGGER.error("Class cast exception while handling property: {}!", e.getMessage());
                    return null;
                } catch (NoSuchElementException e) {
                    LOGGER.error("No such integer for max/min in IntegerProperty exists!", e);
                    return null;
                } catch (RuntimeException e) {
                    LOGGER.error("Error during property serialization!", e);
                    return null;
                }

                toReturn.add("property_definition", propertyDefinition);
                toReturn.add("values", arrayValues);
            }
        }

        return toReturn;
    }

    @Nullable
    public CompoundTag toNbt() {
        CompoundTag toReturn = new CompoundTag();

        if (this.exactMatch) {
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
        } else if (this.blockTag == null) {
            if (this.block == null) {
                LOGGER.error("Block in builder is null despite no block tag existing! If you are seeing this, report it to the author!");
                return null;
            }
            toReturn.putString("block", this.block.toString());
        } else {
            toReturn.putString("blockTag", this.blockTag.location().toString());
        }

        if (!this.properties.isEmpty() && !this.exactMatch) {
            ListTag propertiesNbt = new ListTag();

            for (Map.Entry<Property<?>, Set<Comparable<?>>> entry : this.properties.entrySet()) {
                CompoundTag keyPairNbt = new CompoundTag();

                Property<?> property = entry.getKey();
                String canonicalPropertyName = entry.getKey().getClass().getCanonicalName();

                CompoundTag propertyNbt = new CompoundTag();
                propertyNbt.putString("property_name", entry.getKey().getName());
                propertyNbt.putString("qualified_name", canonicalPropertyName);
                propertyNbt.putString("value_type", entry.getKey().getValueClass().getCanonicalName());

                try {
                    switch (canonicalPropertyName) {
                        case "net.minecraft.world.level.block.state.properties.IntegerProperty" -> {
                            IntegerProperty integerProperty = (IntegerProperty) property;
                            propertyNbt.putInt("max", integerProperty.getPossibleValues().stream().max(Comparator.naturalOrder()).orElseThrow());
                            propertyNbt.putInt("min", integerProperty.getPossibleValues().stream().min(Comparator.naturalOrder()).orElseThrow());
                        }
                        case "net.minecraft.world.level.block.state.properties.BooleanProperty" -> {
                        }
                        case "net.minecraft.world.level.block.state.properties.EnumProperty", "net.minecraft.world.level.block.state.properties.DirectionProperty" -> {
                            EnumProperty<?> enumProperty = (EnumProperty<?>) property;
                            ListTag validValues = new ListTag();
                            for (StringRepresentable value : enumProperty.getPossibleValues()) {
                                validValues.add(StringTag.valueOf(value.getSerializedName()));
                            }
                            propertyNbt.put("possible_values", validValues);
                        }
                        default -> {
                            LOGGER.error("Unrecognized Property instance {} failed to be serialized to NBT!", canonicalPropertyName);
                            return null;
                        }
                    }
                } catch (ClassCastException e) {
                    LOGGER.error("Class cast exception while handling property: {}!", e.getMessage());
                    return null;
                } catch (NoSuchElementException e) {
                    LOGGER.error("No such integer for max/min in IntegerProperty exists!", e);
                    return null;
                } catch (RuntimeException e) {
                    LOGGER.error("Error during property serialization!", e);
                    return null;
                }

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
    public static <T extends Enum<T> & StringRepresentable> BlockInWorldPredicateBuilder fromNbt(CompoundTag nbt) {
        BlockInWorldPredicateBuilder toReturn;
        if (!nbt.getCompound("blockState").isEmpty()) {
            try {
                BlockState blockstate = BlockState.CODEC.parse(NbtOps.INSTANCE, nbt.get("blockState")).getOrThrow(false, null);
                return BlockInWorldPredicateBuilder.of(blockstate);
            } catch (RuntimeException e) {
                LOGGER.error("Error deserializing BlockInWorldPredicateBuilder from NBT!", e);
                return null;
            }
        } else if (nbt.getString("blockTag").isEmpty()) {
            String blockString = nbt.getString("block");
            if (blockString.isEmpty()) {
                LOGGER.error("Block in string is empty despite no block tag existing! If you are seeing this, report it to the author!");
                return null;
            }
            Block block2 = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(blockString));
            if (block2 == null) {
                LOGGER.error("No such block '{}' exists!", blockString);
                return null;
            }

            toReturn = BlockInWorldPredicateBuilder.of(block2);
        } else {
            toReturn = BlockInWorldPredicateBuilder.of(BlockTags.create(new ResourceLocation(nbt.getString("blockTag"))));
        }

        try {
            if (!toReturn.isExactMatch() && nbt.get("properties") instanceof ListTag propertiesTag) {
                for (Tag keyPairTag : propertiesTag) {
                    CompoundTag keyPair = (CompoundTag) keyPairTag;
                    CompoundTag propertyTag = ((CompoundTag) keyPairTag).getCompound("property");

                    String propertyName = propertyTag.getString("property_name");
                    String propertyClass = propertyTag.getString("qualified_name");
                    String valueClass = propertyTag.getString("value_type");

                    if (propertyName.isEmpty() || propertyClass.isEmpty() || valueClass.isEmpty()) {
                        LOGGER.error("Information on property name, its class, or the value's class is missing!");
                        return null;
                    }

                    // Caching sets of strings proved harder than anticipated. I found a way around it though that I am
                    // more satisfied with.
                    Property<?> propertyForBlock;

                    switch (propertyClass) {
                        case "net.minecraft.world.level.block.state.properties.IntegerProperty" ->
                                propertyForBlock = IntegerProperty.create(propertyName, propertyTag.getInt("min"), propertyTag.getInt("max"));
                        case "net.minecraft.world.level.block.state.properties.BooleanProperty" ->
                                propertyForBlock = BooleanProperty.create(propertyName);
                        case "net.minecraft.world.level.block.state.properties.EnumProperty", "net.minecraft.world.level.block.state.properties.DirectionProperty" -> {
                            ListTag validValuesTag = propertyTag.getList("possible_values", Tag.TAG_STRING);
                            //noinspection unchecked
                            Class<T> enumClass = (Class<T>) Class.forName(valueClass);
                            Collection<T> enums = new ArrayList<>(validValuesTag.size());
                            for (Tag stringTag : validValuesTag) {
                                enums.add(Enum.valueOf(enumClass, stringTag.getAsString().toUpperCase(Locale.ROOT)));
                            }

                            propertyForBlock = EnumProperty.create(propertyName, enumClass, enums);
                        }
                        default -> {
                            LOGGER.error("Unrecognized Property instance {} failed to be deserialized!", propertyClass);
                            return null;
                        }
                    }

                    ListTag valuesList = keyPair.getList("values", Tag.TAG_STRING);
                    Set<Comparable<?>> valuesSet = new HashSet<>(valuesList.size());
                    for (Tag string : valuesList) {
                        Comparable<?> comparable = propertyForBlock.getValue(string.getAsString()).orElseThrow();
                        valuesSet.add(comparable);
                    }

                    toReturn.requireProperties(propertyForBlock, valuesSet);
                }
            }
        } catch (ClassNotFoundException e) {
            LOGGER.error("Class in NBT does not exist: {}!", e.getMessage());
            return null;
        } catch (ClassCastException e) {
            LOGGER.error("Class cast exception during property deserialization!", e);
            return null;
        } catch (IllegalArgumentException e) {
            LOGGER.error("No such enum exists!", e);
            LOGGER.error(e.getMessage());
            return null;
        } catch (RuntimeException e) {
            LOGGER.error("BlockInWorldPredicateBuilder contains malformed property definition!", e);
            LOGGER.error(e.getMessage());
            return null;
        }

        if (nbt.get("nbt") instanceof CompoundTag nbtTag) toReturn.requireNbt(nbtTag);
        if (nbt.get("nbt_strict") instanceof CompoundTag nbtTagStrict) toReturn.requireNbt(nbtTagStrict);

        return toReturn;
    }
}
