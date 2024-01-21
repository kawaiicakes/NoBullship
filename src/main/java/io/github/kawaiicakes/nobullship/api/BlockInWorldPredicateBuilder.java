package io.github.kawaiicakes.nobullship.api;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
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
    public final MatchType matchType;
    protected Map<String, Set<String>> properties = new HashMap<>();
    @Nullable
    protected CompoundTag blockEntityNbtData;
    @Nullable
    protected CompoundTag blockEntityNbtDataStrict;

    protected BlockInWorldPredicateBuilder(Block block) {
        this.block = block;
        this.blockState = null;
        this.blockTag = null;
        this.matchType = MatchType.BLOCK;
    }

    protected BlockInWorldPredicateBuilder(BlockState blockState) {
        this.block = blockState.getBlock();
        this.blockState = blockState;
        this.blockTag = null;
        this.matchType = MatchType.BLOCKSTATE;
    }

    protected BlockInWorldPredicateBuilder(TagKey<Block> blockTag) {
        this.block = null;
        this.blockState = null;
        this.blockTag = blockTag;
        this.matchType = MatchType.TAG;
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

    /**
     * Stipulates that a given property in the <code>BlockState</code> must be one of the passed values. If a
     * key already exists, the passed value will be added to the <code>Set</code> of "allowed" values for that
     * property.
     */
    public BlockInWorldPredicateBuilder requireProperties(Property<?> property, Set<Comparable<?>> value) {
        if (this.matchType.equals(MatchType.BLOCKSTATE)) throw new UnsupportedOperationException("This builder is already looking for an exact match to the passed BlockState!");
        if (this.block != null) {
            if (!this.block.defaultBlockState().getValues().containsKey(property))
                throw new IllegalArgumentException(property + " does not belong to " + this.block.defaultBlockState() + "!");
            if (value.stream().anyMatch(comparable -> !property.getPossibleValues().contains(comparable)))
                throw new IllegalArgumentException(value + " cannot be associated with " + property + "!");
        }

        String propertyName = property.getName();
        Set<String> valueSet = value.stream().map(Object::toString).collect(Collectors.toSet());

        this.properties.putIfAbsent(propertyName, new HashSet<>());
        if (this.properties.containsKey(propertyName)) {
            this.properties.get(propertyName).addAll(valueSet);
        }

        return this;
    }

    /**
     * Stipulates that a given property in the <code>BlockState</code> must be one of the passed values. If a
     * key already exists, the passed value will be added to the <code>Set</code> of "allowed" values for that
     * property.
     */
    public BlockInWorldPredicateBuilder requireProperties(String property, Set<String> values) {
        if (this.matchType.equals(MatchType.BLOCKSTATE)) throw new UnsupportedOperationException("This builder is already looking for an exact match to the passed BlockState!");
        if (this.block != null) {
            try {
                Property<?> propertyActual = this.block.defaultBlockState().getValues()
                        .keySet()
                        .stream()
                        .filter(prop -> prop.getName().equals(property))
                        .findFirst()
                        .orElseThrow();

                if (values.stream()
                        .anyMatch(comparable -> propertyActual.getPossibleValues()
                                .stream()
                                .map(Object::toString)
                                .noneMatch(valueSetString -> valueSetString.equals(comparable)))) {
                    LOGGER.error("One or more values in argument are unable to be associated with {}!", property);
                    throw new IllegalArgumentException(values + " cannot be associated with " + property + "!");
                }
            } catch (RuntimeException e) {
                LOGGER.error("Unable to associate property with builder!", e);
                throw new RuntimeException(e);
            }
        }

        this.properties.putIfAbsent(property, new HashSet<>());
        if (this.properties.containsKey(property)) {
            this.properties.get(property).addAll(values);
        }

        return this;
    }

    /**
     * Stipulates that a given property in the <code>BlockState</code> must be equal to the passed value. If a
     * key already exists, the passed value will be added to the <code>Set</code> of "allowed" values for that
     * property.
     */
    public BlockInWorldPredicateBuilder requireProperty(Property<?> property, Comparable<?> value) {
        if (this.matchType.equals(MatchType.BLOCKSTATE)) throw new UnsupportedOperationException("This builder is already looking for an exact match to the passed BlockState!");
        if (this.block != null) {
            if (!this.block.defaultBlockState().getValues().containsKey(property))
                throw new IllegalArgumentException(property + " does not belong to " + this.block.defaultBlockState() + "!");
            if (!property.getPossibleValues().contains(value))
                throw new IllegalArgumentException(value + " cannot be associated with " + property + "!");
        }

        String propertyName = property.getName();

        this.properties.putIfAbsent(propertyName, new HashSet<>());
        if (this.properties.containsKey(propertyName)) {
            this.properties.get(propertyName).add(value.toString());
        }

        return this;
    }

    /**
     * Stipulates that a given property in the <code>BlockState</code> must be equal to the passed value. If a
     * key already exists, the passed value will be added to the <code>Set</code> of "allowed" values for that
     * property.
     */
    public BlockInWorldPredicateBuilder requireProperty(String property, String value) {
        if (this.matchType.equals(MatchType.BLOCKSTATE)) throw new UnsupportedOperationException("This builder is already looking for an exact match to the passed BlockState!");
        if (this.block != null) {
            try {
                Property<?> propertyActual = this.block.defaultBlockState().getValues()
                        .keySet()
                        .stream()
                        .filter(prop -> prop.getName().equals(property))
                        .findFirst()
                        .orElseThrow();

                if (propertyActual.getPossibleValues().stream().noneMatch(possibleValue -> possibleValue.toString().equals(value))) {
                    LOGGER.error("Value {} in argument is unable to be associated with {}!", value, property);
                    throw new IllegalArgumentException(value + " cannot be associated with " + property + "!");
                }
            } catch (RuntimeException e) {
                LOGGER.error("Unable to associate property with builder!", e);
                throw new RuntimeException(e);
            }
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
        if (!this.isForTag() && !(this.block instanceof EntityBlock)) throw new IllegalArgumentException(this.block + " cannot have a block entity, so it cannot have NBT data!");
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
        if (!this.isForTag() && !(this.block instanceof EntityBlock)) throw new IllegalArgumentException(this.block + " cannot have a block entity, so it cannot have NBT data!");
        if (this.blockEntityNbtData == null) this.blockEntityNbtData = new CompoundTag();
        this.blockEntityNbtData.merge(tag);
        return this;
    }

    /**
     * Returns an arbitrarily made copy of the NBT data this builder holds (if any exists).
     */
    public CompoundTag getNaiveNbt() {
        CompoundTag toReturn = new CompoundTag();
        if (this.getBlockEntityNbtData() != null) toReturn.merge(this.getBlockEntityNbtData());
        if (this.getBlockEntityNbtDataStrict() != null) toReturn.merge(this.getBlockEntityNbtDataStrict());
        return toReturn;
    }

    /**
     * Returns a <code>BlockInWorldPredicate</code> whose <code>#test</code> looks for the criteria
     * specified by this builder. This overload only works if there are no directional properties specified as
     * a requisite.
     */
    public BlockInWorldPredicate build() {
        Block blockToArg = this.block;
        BlockState stateToArg = this.isForBlockState() ? this.blockState : null;
        if (this.isForTag()) blockToArg = null;
        return new BlockInWorldPredicate(blockToArg, stateToArg, this.blockTag, this.properties, this.blockEntityNbtData, this.blockEntityNbtDataStrict);
    }

    @Nullable
    public JsonObject toJson() throws RuntimeException {
        JsonObject toReturn;

        try {
            toReturn = switch (this.matchType) {
                case BLOCK -> this.serializeBlockToJson();
                case BLOCKSTATE -> this.serializeBlockStateToJson();
                case TAG -> this.serializeBlockTagToJson();
            };
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }

        return toReturn;
    }

    public static BlockInWorldPredicateBuilder fromJson(JsonObject serializedBuilder) throws IllegalArgumentException {
        Set<String> keySet = serializedBuilder.keySet();
        if (keySet.isEmpty()) throw new IllegalArgumentException("Error while deserializing! Predicate is empty!");

        int validTypeCount = 0;
        MatchType builderType = null;
        Set<String> validTypeSet = Arrays.stream(MatchType.values()).map(MatchType::getSerializedName).collect(Collectors.toSet());
        for (String key : keySet) {
            if (!validTypeSet.contains(key)) continue;
            if (validTypeCount++ > 1) throw new IllegalArgumentException("More than one predicate type attempted to be passed!");
            builderType = MatchType.fromSerializedName(key);
        }
        if (builderType == null) throw new RuntimeException("Unable to resolve predicate match type!");

        try {
            return switch (builderType) {
                case BLOCK -> deserializeBlockFromJson(serializedBuilder);
                case BLOCKSTATE -> deserializeBlockStateFromJson(serializedBuilder);
                case TAG -> deserializeBlockTagFromJson(serializedBuilder);
            };
        } catch (RuntimeException e) {
            LOGGER.error("Error encountered while deserializing predicate from JSON!", e);
            LOGGER.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Nullable
    public CompoundTag toNbt() {
        CompoundTag toReturn = new CompoundTag();

        try {
            CompoundTag predicateNbt = switch (this.matchType) {
                case BLOCK -> this.serializeBlockToNbt();
                case BLOCKSTATE -> this.serializeBlockStateToNbt();
                case TAG -> this.serializeBlockTagToNbt();
            };

            toReturn.put(this.matchType.getSerializedName(), predicateNbt);
        } catch (RuntimeException e) {
            LOGGER.error("Error while serializing builder to NBT!", e);
            LOGGER.error(e.getMessage());
            return null;
        }

        if (this.blockEntityNbtData != null) toReturn.put("nbt", this.blockEntityNbtData);
        if (this.blockEntityNbtDataStrict != null) toReturn.put("nbt_strict", this.blockEntityNbtDataStrict);

        return toReturn;
    }

    @Nullable
    public static BlockInWorldPredicateBuilder fromNbt(CompoundTag nbt) {
        try {
            BlockInWorldPredicateBuilder toReturn;

            CompoundTag serializedPredicate;
            Set<String> keySet = nbt.getAllKeys();
            if (keySet.isEmpty()) throw new IllegalArgumentException("Error while deserializing! Predicate is empty!");

            int validTypeCount = 0;
            MatchType builderType = null;
            Set<String> validTypeSet = Arrays.stream(MatchType.values()).map(MatchType::getSerializedName).collect(Collectors.toSet());
            for (String key : keySet) {
                if (!validTypeSet.contains(key)) continue;
                if (validTypeCount++ > 1) throw new IllegalArgumentException("More than one predicate type attempted to be passed!");
                builderType = MatchType.fromSerializedName(key);
            }
            if (builderType == null) throw new RuntimeException("Unable to resolve predicate match type!");

            serializedPredicate = nbt.getCompound(builderType.getSerializedName());
            if (serializedPredicate.isEmpty()) throw new RuntimeException("Unable to resolve serialized predicate as a CompoundTag!");

            toReturn = switch (builderType) {
                case BLOCK -> deserializeBlockFromNbt(serializedPredicate);
                case BLOCKSTATE -> deserializeBlockStateFromNbt(serializedPredicate);
                case TAG -> deserializeBlockTagFromNbt(serializedPredicate);
            };

            if (!nbt.getCompound("nbt").isEmpty()) toReturn.requireNbt(nbt.getCompound("nbt"));
            if (!nbt.getCompound("nbt_strict").isEmpty()) toReturn.requireStrictNbt(nbt.getCompound("nbt_strict"));

            return toReturn;
        } catch (RuntimeException e) {
            LOGGER.error("Error while deserializing builder from NBT!", e);
            LOGGER.error(e.getMessage());
            return null;
        }
    }

    /*
            DE/SERIALIZATION HELPER METHODS BEGIN HERE
     */

    protected CompoundTag serializeBlockToNbt() throws RuntimeException {
        try {
            CompoundTag toReturn = new CompoundTag();
            CompoundTag propertyPairs = new CompoundTag();
            for (Map.Entry<String, Set<String>> propertyEntry : this.properties.entrySet()) {
                ListTag propertyValues = new ListTag();
                for (String valueString : propertyEntry.getValue()) {
                    propertyValues.add(StringTag.valueOf(valueString));
                }

                if (propertyValues.isEmpty()) continue;
                propertyPairs.put(propertyEntry.getKey(), propertyValues);
            }

            if (!propertyPairs.isEmpty()) toReturn.put("Properties", propertyPairs);
            toReturn.putString("Name", Objects.requireNonNull(BLOCKS.getKey(this.block)).toString());

            return toReturn;
        } catch (RuntimeException e) {
            LOGGER.error("Error serializing block predicate to NBT!", e);
            LOGGER.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    protected CompoundTag serializeBlockStateToNbt() throws RuntimeException {
        try {
            return (CompoundTag) BlockState.CODEC.encodeStart(NbtOps.INSTANCE, this.blockState).getOrThrow(false, LOGGER::error);
        } catch (ClassCastException e) {
            LOGGER.error("Unable to cast serialized blockstate NBT to CompoundTag!", e);
            throw new RuntimeException(e);
        } catch (RuntimeException e) {
            LOGGER.error("Error serializing blockstate predicate to NBT!", e);
            LOGGER.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    protected CompoundTag serializeBlockTagToNbt() throws RuntimeException {
        try {
            CompoundTag toReturn = new CompoundTag();
            CompoundTag propertyPairs = new CompoundTag();
            for (Map.Entry<String, Set<String>> propertyEntry : this.properties.entrySet()) {
                ListTag propertyValues = new ListTag();
                for (String valueString : propertyEntry.getValue()) {
                    propertyValues.add(StringTag.valueOf(valueString));
                }

                if (propertyValues.isEmpty()) continue;
                propertyPairs.put(propertyEntry.getKey(), propertyValues);
            }

            if (!propertyPairs.isEmpty()) toReturn.put("Properties", propertyPairs);
            toReturn.putString("Name", Objects.requireNonNull(this.blockTag).location().toString());

            return toReturn;
        } catch (RuntimeException e) {
            LOGGER.error("Error serializing block tag predicate to NBT!", e);
            LOGGER.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    protected JsonObject serializeBlockToJson() throws RuntimeException {
        if (this.block == null) throw new RuntimeException("This builder does not check against a block!");
        if (this.isForBlockState()) throw new RuntimeException("This builder does not check against a block!");

        JsonObject toReturn  = new JsonObject();
        JsonObject block = new JsonObject();

        JsonObject properties = new JsonObject();
        for (Map.Entry<String, Set<String>> propertyEntry : this.properties.entrySet()) {
            JsonArray propertyValues = new JsonArray(propertyEntry.getValue().size());
            propertyEntry.getValue().forEach(propertyValues::add);
            properties.add(propertyEntry.getKey(), propertyValues);
        }
        if (properties.size() != 0) {
            block.add("Properties", properties);
        }

        try {
            block.addProperty("Name", Objects.requireNonNull(BLOCKS.getKey(this.block)).toString());
            toReturn.add("block", block);
            this.serializeNbtToJson(toReturn);
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }

        return toReturn;
    }

    protected JsonObject serializeBlockStateToJson() throws RuntimeException {
        if (this.blockState == null) throw new RuntimeException("This builder does not check against a blockstate!");
        JsonObject toReturn = new JsonObject();

        JsonElement blockstate;
        try {
            blockstate = BlockState.CODEC.encodeStart(JsonOps.INSTANCE, this.blockState).getOrThrow(false, LOGGER::error);
            toReturn.add("blockstate", blockstate);
            this.serializeNbtToJson(toReturn);
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }

        return toReturn;
    }

    protected JsonObject serializeBlockTagToJson() throws RuntimeException {
        if (this.blockTag == null) throw new RuntimeException("This builder does not check against a block tag!");
        JsonObject toReturn = new JsonObject();
        JsonObject tag = new JsonObject();

        JsonObject properties = new JsonObject();
        for (Map.Entry<String, Set<String>> propertyEntry : this.properties.entrySet()) {
            JsonArray propertyValues = new JsonArray(propertyEntry.getValue().size());
            propertyEntry.getValue().forEach(propertyValues::add);
            properties.add(propertyEntry.getKey(), propertyValues);
        }
        if (properties.size() != 0) {
            tag.add("Properties", properties);
        }

        try {
            tag.addProperty("Name", this.blockTag.location().toString());
            toReturn.add("tag", tag);
            this.serializeNbtToJson(toReturn);
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }

        return toReturn;
    }

    protected void serializeNbtToJson(JsonObject root) throws RuntimeException {
        if (this.block != null && (!(this.block instanceof EntityBlock)))
            throw new RuntimeException("This builder cannot support NBT as it matches against a block which cannot be an entity!");

        JsonElement jsonNbt = null;
        JsonElement jsonNbtStrict = null;
        try {
            if (this.getBlockEntityNbtData() != null) {
                jsonNbt = CompoundTag.CODEC.encodeStart(
                        JsonOps.INSTANCE, this.getBlockEntityNbtData()).getOrThrow(false, LOGGER::error);
            }

            if (this.getBlockEntityNbtDataStrict() != null) {
                jsonNbtStrict = CompoundTag.CODEC.encodeStart(
                        JsonOps.INSTANCE, this.getBlockEntityNbtDataStrict()).getOrThrow(false, LOGGER::error);
            }
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }

        if (jsonNbt != null) {
            root.add("nbt", jsonNbt);
        }

        if (jsonNbtStrict != null) {
            root.add("nbt_strict", jsonNbtStrict);
        }
    }

    protected static BlockInWorldPredicateBuilder deserializeBlockFromNbt(CompoundTag serializedPredicate) throws RuntimeException {
        try {
            BlockInWorldPredicateBuilder toReturn;

            Block block = BLOCKS.getValue(new ResourceLocation(serializedPredicate.getString("Name")));
            if (block == null) throw new IllegalArgumentException("Argument does not contain a valid block!");

            toReturn = BlockInWorldPredicateBuilder.of(block);

            CompoundTag propertiesTag = serializedPredicate.getCompound("Properties");
            for (String propertyString : propertiesTag.getAllKeys()) {
                ListTag propertyValuesTag = propertiesTag.getList(propertyString, Tag.TAG_STRING);

                Set<String> valueStringSet = new HashSet<>(propertyValuesTag.size());
                for (Tag valueTag : propertyValuesTag) {
                    StringTag valueCompound = (StringTag) valueTag;
                    valueStringSet.add(valueCompound.getAsString());
                }
                if (!valueStringSet.isEmpty()) toReturn.requireProperties(propertyString, valueStringSet);
            }

            CompoundTag nbtTag = serializedPredicate.getCompound("nbt");
            CompoundTag nbtStrictTag = serializedPredicate.getCompound("nbt_strict");
            if (!nbtTag.isEmpty()) toReturn.requireNbt(nbtTag);
            if (!nbtStrictTag.isEmpty()) toReturn.requireStrictNbt(nbtStrictTag);

            return toReturn;
        } catch (RuntimeException e) {
            LOGGER.error("Error while deserializing block from NBT!", e);
            LOGGER.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    protected static BlockInWorldPredicateBuilder deserializeBlockStateFromNbt(CompoundTag serializedPredicate) throws RuntimeException {
        try {
            BlockState blockState = BlockState.CODEC.parse(NbtOps.INSTANCE, serializedPredicate).getOrThrow(false, LOGGER::error);
            return BlockInWorldPredicateBuilder.of(blockState);
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
    }

    protected static BlockInWorldPredicateBuilder deserializeBlockTagFromNbt(CompoundTag serializedPredicate) throws RuntimeException {
        try {
            BlockInWorldPredicateBuilder toReturn;

            TagKey<Block> blockTag = BlockTags.create(new ResourceLocation(serializedPredicate.getString("Name")));

            toReturn = BlockInWorldPredicateBuilder.of(blockTag);

            CompoundTag propertiesTag = serializedPredicate.getCompound("Properties");
            for (String propertyString : propertiesTag.getAllKeys()) {
                ListTag propertyValuesTag = propertiesTag.getList(propertyString, Tag.TAG_STRING);

                Set<String> valueStringSet = new HashSet<>(propertyValuesTag.size());
                for (Tag valueTag : propertyValuesTag) {
                    StringTag valueCompound = (StringTag) valueTag;
                    valueStringSet.add(valueCompound.getAsString());
                }
                if (!valueStringSet.isEmpty()) toReturn.requireProperties(propertyString, valueStringSet);
            }

            CompoundTag nbtTag = serializedPredicate.getCompound("nbt");
            CompoundTag nbtStrictTag = serializedPredicate.getCompound("nbt_strict");
            if (!nbtTag.isEmpty()) toReturn.requireNbt(nbtTag);
            if (!nbtStrictTag.isEmpty()) toReturn.requireStrictNbt(nbtStrictTag);

            return toReturn;
        } catch (RuntimeException e) {
            LOGGER.error("Error while deserializing block from NBT!", e);
            LOGGER.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    protected static BlockInWorldPredicateBuilder deserializeBlockFromJson(JsonElement serializedPredicate) throws RuntimeException {
        try {
            JsonObject predicateObj = serializedPredicate.getAsJsonObject();
            BlockInWorldPredicateBuilder toReturn;
            JsonObject blockAsJson = predicateObj.getAsJsonObject("block");

            Block block = BLOCKS.getValue(new ResourceLocation(blockAsJson.getAsJsonPrimitive("Name").getAsString()));
            if (block == null) throw new RuntimeException("Block was null during deserialization!");
            toReturn = BlockInWorldPredicateBuilder.of(block);

            JsonObject propertiesJson = blockAsJson.getAsJsonObject("Properties");
            if (propertiesJson == null) propertiesJson = new JsonObject();
            for (Map.Entry<String, JsonElement> propertyEntryJson : propertiesJson.entrySet()) {
                JsonArray propertyValuesJson = propertyEntryJson.getValue().getAsJsonArray();
                if (propertyValuesJson == null) {
                    LOGGER.error("Unable to retrieve values for property {} as a JSON array!", propertyEntryJson.getKey());
                    throw new IllegalArgumentException();
                }
                Set<String> valueStringSet = new HashSet<>(propertyValuesJson.size());
                for (JsonElement valueJson : propertyValuesJson) {
                    JsonPrimitive valuePrimitive = valueJson.getAsJsonPrimitive();
                    valueStringSet.add(valuePrimitive.getAsString());
                }
                if (!valueStringSet.isEmpty()) toReturn.requireProperties(propertyEntryJson.getKey(), valueStringSet);
            }

            deserializeNbtFromJson(serializedPredicate, toReturn);

            return toReturn;
        } catch (NullPointerException n) {
            LOGGER.error("NullPointerException during deserialization!", n);
            throw new RuntimeException(n);
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
    }

    protected static BlockInWorldPredicateBuilder deserializeBlockStateFromJson(JsonElement serializedPredicate) throws RuntimeException {
        BlockInWorldPredicateBuilder toReturn;

        JsonElement blockStateAsJson;
        try {
            blockStateAsJson = serializedPredicate.getAsJsonObject().get("blockstate");
            BlockState blockState = BlockState.CODEC.parse(
                    JsonOps.INSTANCE, blockStateAsJson).getOrThrow(false, LOGGER::error);
            toReturn = BlockInWorldPredicateBuilder.of(blockState);
            deserializeNbtFromJson(serializedPredicate, toReturn);
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }

        return toReturn;
    }

    protected static BlockInWorldPredicateBuilder deserializeBlockTagFromJson(JsonElement serializedPredicate) throws RuntimeException {
        try {
            JsonObject predicateObj = serializedPredicate.getAsJsonObject();
            BlockInWorldPredicateBuilder toReturn;
            JsonObject blockAsJson = predicateObj.getAsJsonObject("tag");

            TagKey<Block> tag = BlockTags.create(new ResourceLocation(blockAsJson.getAsJsonPrimitive("Name").getAsString()));
            toReturn = BlockInWorldPredicateBuilder.of(tag);

            JsonObject propertiesJson = predicateObj.getAsJsonObject("Properties");
            if (propertiesJson == null) propertiesJson = new JsonObject();
            for (Map.Entry<String, JsonElement> propertyEntryJson : propertiesJson.entrySet()) {
                JsonArray propertyValuesJson = propertyEntryJson.getValue().getAsJsonArray();
                if (propertyValuesJson == null) {
                    LOGGER.error("Unable to retrieve values for property {} as a JSON array!", propertyEntryJson.getKey());
                    throw new IllegalArgumentException();
                }
                Set<String> valueStringSet = new HashSet<>(propertyValuesJson.size());
                for (JsonElement valueJson : propertyValuesJson) {
                    JsonPrimitive valuePrimitive = valueJson.getAsJsonPrimitive();
                    valueStringSet.add(valuePrimitive.getAsString());
                }
                if (!valueStringSet.isEmpty()) toReturn.requireProperties(propertyEntryJson.getKey(), valueStringSet);
            }

            deserializeNbtFromJson(serializedPredicate, toReturn);

            return toReturn;
        } catch (NullPointerException n) {
            LOGGER.error("NullPointerException during deserialization!", n);
            throw new RuntimeException(n);
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
    }

    protected static void deserializeNbtFromJson(JsonElement root, BlockInWorldPredicateBuilder builder) throws RuntimeException {
        try {
            JsonObject rootAsObj= root.getAsJsonObject();
            JsonElement nbtJson = rootAsObj.get("nbt");
            JsonElement nbtJsonStrict = rootAsObj.get("nbt_strict");

            if (nbtJson != null) {
                builder.requireNbt(
                        CompoundTag.CODEC.parse(JsonOps.INSTANCE, nbtJson).getOrThrow(false, LOGGER::error));
            }

            if (nbtJsonStrict != null) {
                builder.requireStrictNbt(
                        CompoundTag.CODEC.parse(JsonOps.INSTANCE, nbtJsonStrict).getOrThrow(false, LOGGER::error));
            }
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
    }

    /*
            DE/SERIALIZATION HELPER METHODS END HERE
     */

    /**
     * Returns a <code>Set</code> of all possible <code>BlockState</code>s satisfying this builder. This method is mainly used for the palette in the
     * <code>MultiblockPattern</code>.
     */
    public Set<BlockState> getValidBlockstates() {
        return switch (this.matchType) {
            case BLOCK -> {
                assert this.block != null;
                yield this.allValidStatesForBlock(this.block);
            }
            case BLOCKSTATE -> {
                assert this.blockState != null;
                yield Collections.singleton(this.blockState);
            }
            case TAG -> {
                assert this.blockTag != null;
                Set<BlockState> toReturn = new HashSet<>();
                for (Block block : Objects.requireNonNull(BLOCKS.tags()).getTag(this.blockTag)) {
                    toReturn.addAll(this.allValidStatesForBlock(block));
                }
                yield toReturn;
            }
        };
    }

    protected <T extends Comparable<T>, V extends T> Set<BlockState> allValidStatesForBlock(Block block) {
        if (block.getStateDefinition().getPossibleStates().size() == 1)
            return Collections.singleton(block.defaultBlockState());

        Set<BlockState> toReturn = new HashSet<>();

        Set<Map<String, String>> allProperties = getPropertyPermutations(this.properties);

        try {
            for (Map<String, String> stringMap : allProperties) {
                BlockState blockState = block.defaultBlockState();
                for (Map.Entry<String, String> stringEntry : stringMap.entrySet()) {

                    //noinspection unchecked
                    Property<T> property = (Property<T>) blockState
                            .getProperties()
                            .stream()
                            .filter(property1 -> property1.getName().equals(stringEntry.getKey()))
                            .findFirst()
                            .orElse(null);

                    if (property == null) {
                        blockState = null;
                        break;
                    }

                    //noinspection unchecked
                    V value = (V) property.getValue(stringEntry.getValue()).orElse(null);

                    if (value == null) {
                        blockState = null;
                        break;
                    }

                    blockState = blockState.setValue(property, value);
                }

                if (blockState == null) continue;
                toReturn.add(blockState);
            }

            return toReturn;
        } catch (RuntimeException e) {
            LOGGER.error("Unable to return valid blockstates for builder of block " + block + " !", e);
            LOGGER.error(e.getMessage());
            return new HashSet<>();
        }
    }

    public ItemStack getItemized() {
        if (this.block != null) {
            if (this.blockEntityNbtDataStrict == null) return this.block.asItem().getDefaultInstance();

            CompoundTag ogTag = this.block.asItem().getDefaultInstance().getOrCreateTag().copy();

                    // TODO: redo nbt system by allowing specification of how a tag should be checked.
                    /*
                        There is also the issue of determining how to display non-strict data.

                        Consider a system where "helper" data is parsed with the NBT during deserialization from JSON;
                        this helper data then examines the given NBT data using criteria the helper specifies.

                        Having a strict and non-strict set of data (like as of now) complicates things for me. It will
                        be difficult for me to maintain compatibility with different edge cases.
                        In this scenario, merging $blockEntityNbtData into the itemized BIWPredicateBuilder
                        will not give an appropriate representation for stuff like containers.
                     */

            CompoundTag blockEntityTag = new CompoundTag();
            blockEntityTag.put("BlockEntityTag", this.getNaiveNbt());
            ogTag.merge(blockEntityTag);

            ItemStack toReturn = this.block.asItem().getDefaultInstance();
            toReturn.setTag(ogTag);
            return toReturn;
        } else {
            // TODO proper ingredient thingy
            //noinspection DataFlowIssue
            return Ingredient.of(ItemTags.create(new ResourceLocation(this.blockTag.toString()))).getItems()[0];
        }
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

    public boolean isForBlock() {
        return this.matchType.equals(MatchType.BLOCK);
    }

    public boolean isForBlockState() {
        return this.matchType.equals(MatchType.BLOCKSTATE);
    }

    public boolean isForTag() {
        return this.matchType.equals(MatchType.TAG);
    }

    /**
     * Simple method indicating if this BIWPredicate contains NBT data.
     */
    public boolean requiresNbt() {
        return this.blockEntityNbtData != null || this.blockEntityNbtDataStrict != null;
    }

    // erm ackshually these are combinations but the word permutation is cooler
    protected static Set<Map<String, String>> getPropertyPermutations(Map<String, Set<String>> properties) {
        Set<Map<String, String>> toReturn = new HashSet<>();

        List<Map.Entry<String, Set<String>>> propertiesList = properties.entrySet().stream().toList();

        List<List<String>> valuesList = new ArrayList<>(propertiesList.size());
        for (Map.Entry<String, Set<String>> propertyEntries : propertiesList) {
            valuesList.add(propertyEntries.getValue().stream().toList());
        }
        List<List<String>> cartesianProduct = Lists.cartesianProduct(valuesList);

        for (List<String> strings : cartesianProduct) {
            Map<String, String> propertyMap = new HashMap<>();

            for (String string : strings) {
                int indexOfString = strings.indexOf(string);
                String correspondingPropertyName = propertiesList.get(indexOfString).getKey();
                propertyMap.put(correspondingPropertyName, string);
            }

            toReturn.add(propertyMap);
        }

        return toReturn;
    }

    public enum MatchType implements StringRepresentable {
        BLOCK("block"),
        BLOCKSTATE("blockstate"),
        TAG("tag");

        private final String name;

        MatchType(String pName) {
            this.name = pName;
        }

        @Override
        public String toString() {
            return this.name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        @Nullable
        public static MatchType fromSerializedName(String name) {
            for (MatchType type : MatchType.values()) {
                if (type.getSerializedName().equals(name)) return type;
            }
            return null;
        }
    }
}
