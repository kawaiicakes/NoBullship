package io.github.kawaiicakes.nobullship.api.multiblock;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import io.github.kawaiicakes.nobullship.api.BlockInWorldPredicate;
import io.github.kawaiicakes.nobullship.api.BlockInWorldPredicateBuilder;
import io.github.kawaiicakes.nobullship.multiblock.MultiblockPattern;
import net.minecraft.FieldsAreNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.stream.Collectors;

import static net.minecraftforge.registries.ForgeRegistries.BLOCKS;

/**
 * Bears no relation to <code>FinishedMultiblockRecipe</code>. Simply an immutable data carrier intended to cache
 * multiblock recipes.
 */
@ParametersAreNonnullByDefault
@FieldsAreNonnullByDefault
public record MultiblockRecipe(
        MultiblockPattern recipe,
        ResourceLocation result,
        @Nullable CompoundTag nbt,
        @Nullable ImmutableList<ItemStack> requisites
) {
    private static final Logger LOGGER = LogUtils.getLogger();
    @Override
    public MultiblockPattern recipe() {
        return new MultiblockPattern(
                (BlockInWorldPredicate[][][]) this.recipe.getPattern(),
                this.recipe.getPalette(),
                this.recipe.getTotalBlocks(),
                this.recipe.getSerializedPattern()
        );
    }

    @Override
    public ResourceLocation result() {
        return new ResourceLocation(this.result.toString());
    }

    @Nullable
    @Override
    public CompoundTag nbt() {
        return this.nbt != null ? this.nbt.copy() : null;
    }

    /**
     * This shit is made like this to ensure I return a deep copy; it kept returning mutable values
     */
    @Nullable
    @Override
    public ImmutableList<ItemStack> requisites() {
        if (this.requisites == null) return null;
        ImmutableList.Builder<ItemStack> toReturn = ImmutableList.builder();
        for (ItemStack stack : this.requisites) {
            toReturn.add(stack.copy());
        }
        return toReturn.build();
    }

    /**
     * Serializes this recipe into a <code>CompoundTag</code>. Returns null if the recipe is malformed.
     */
    @Nullable
    public CompoundTag toNbt() {
        CompoundTag toReturn = new CompoundTag();

        CompoundTag pattern = this.recipe.toNbt();
        if (pattern == null) return null;

        toReturn.put("recipe", pattern);

        toReturn.putString("result", this.result.toString());

        if (this.nbt != null) toReturn.put("nbt", this.nbt);

        if (this.requisites != null && !this.requisites.isEmpty()) {
            ListTag requisiteList = new ListTag();

            for (ItemStack item : this.requisites) {
                requisiteList.add(item.serializeNBT());
            }

            toReturn.put("requisites", requisiteList);
        }

        return toReturn;
    }

    /**
     * Deserializes a <code>CompoundTag</code> into a <code>MultiblockRecipe</code>.
     * Returns null if the NBT tag is malformed.
     */
    @Nullable
    public static MultiblockRecipe fromNbt(CompoundTag nbt) throws IllegalArgumentException {
        if (!(nbt.get("recipe") instanceof CompoundTag recipeNbt)) throw new IllegalArgumentException("Passed NBT does not contain a recipe!");
        if (!(nbt.get("result") instanceof StringTag stringNbt)) throw new IllegalArgumentException("Passed NBT does not contain a string!");

        MultiblockPattern pattern = MultiblockPattern.fromNbt(recipeNbt);
        if (pattern == null) return null;

        CompoundTag resultNbt = null;
        if (nbt.get("nbt") instanceof CompoundTag serializedResult) resultNbt = serializedResult.copy();

        ImmutableList<ItemStack> deserializedRequisites = null;
        if (nbt.get("requisites") instanceof ListTag requisiteList && requisiteList.getElementType() == Tag.TAG_COMPOUND) {
            ImmutableList.Builder<ItemStack> builder = new ImmutableList.Builder<>();

            for (Tag value : requisiteList) {
                builder.add(ItemStack.of((CompoundTag) value));
            }

            deserializedRequisites = builder.build();
        }

        return new MultiblockRecipe(
                pattern,
                new ResourceLocation(stringNbt.getAsString()),
                resultNbt,
                deserializedRequisites
        );
    }

    /**
     * Essentially a Json deserializer. Returns null if the argument contains syntax errors.
     */
    @Nullable
    public static MultiblockRecipe fromJson(JsonObject json) {
        JsonObject jsonKeys = json.getAsJsonObject("key");
        JsonObject jsonRecipe = json.getAsJsonObject("recipe");
        JsonObject jsonResult = json.getAsJsonObject("result");
        JsonArray jsonRequisites = json.getAsJsonArray("requisites");

        if (jsonKeys == null || jsonRecipe == null || jsonResult == null) {
            LOGGER.error("Sussy JSON syntax!");
            return null;
        }

        ResourceLocation result = new ResourceLocation(jsonResult.getAsJsonPrimitive("entity").getAsString());

        CompoundTag nbt = null;
        if (jsonResult.has("nbt")) {
            nbt = (CompoundTag) JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, jsonResult.get("nbt"));
        }

        MultiblockRecipeBuilder builder = MultiblockRecipeBuilder.of(result);

        for (Map.Entry<String, JsonElement> keyEntry : jsonKeys.entrySet()) {
            if (Objects.equals(keyEntry.getKey(), " ") || Objects.equals(keyEntry.getKey(), "$")) {
                LOGGER.error("{} is a reserved character!", keyEntry.getKey());
                return null;
            }

            try {
                deserializePredicateBlockToBuilder(keyEntry, builder);
            } catch (RuntimeException e) {
                LOGGER.error("An error occurred during deserialization from JSON!", e);
                return null;
            }
        }

        for (int i = jsonRecipe.size() - 1; i >= 0 ; i--) {
            JsonArray aisle = jsonRecipe.getAsJsonArray("layer" + i);
            if (aisle.isEmpty()) return null;

            List<String> strings = new ArrayList<>(aisle.size());
            aisle.forEach(element -> strings.add(element.getAsString()));

            try {
                builder.aisle(strings.toArray(String[]::new));
            } catch (IllegalArgumentException exception) {
                LOGGER.error(exception.getMessage());
                return null;
            }
        }

        NonNullList<ItemStack> requisites;
        ImmutableList<ItemStack> toReturnRequisites = null;
        if (jsonRequisites != null) {
            requisites = NonNullList.createWithCapacity(jsonRequisites.size());
            for (JsonElement element : jsonRequisites) {
                if (element.isJsonObject()) requisites.add(ShapedRecipe.itemStackFromJson(element.getAsJsonObject()));
            }
            toReturnRequisites = ImmutableList.copyOf(requisites);
        }

        return new MultiblockRecipe(builder.build(), result, nbt, toReturnRequisites);
    }

    public static <T extends Enum<T> & StringRepresentable> void deserializePredicateBlockToBuilder(Map.Entry<String, JsonElement> key, MultiblockRecipeBuilder builder) {
        JsonElement mappedCharacterBlock = key.getValue().getAsJsonObject().get("block");
        JsonElement mappedCharacterBlockState = key.getValue().getAsJsonObject().get("blockstate");
        JsonElement mappedCharacterBlockTag = key.getValue().getAsJsonObject().get("block_tag");

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

        JsonObject propertyJson = key.getValue().getAsJsonObject().getAsJsonObject("state");
        JsonObject nbtJsonStrict = key.getValue().getAsJsonObject().getAsJsonObject("nbt_strict");
        JsonObject nbtJson = key.getValue().getAsJsonObject().getAsJsonObject("nbt");

        if (mappedCharacterBlock != null) {
            ResourceLocation blockLocation = new ResourceLocation(mappedCharacterBlock.getAsString());
            Block block = RegistryObject.create(blockLocation, BLOCKS).get();

            if ((nbtJsonStrict != null || nbtJson != null) && !(block instanceof EntityBlock)) {
                LOGGER.error("Block {} does not have a block entity and cannot hold NBT data!", block);
                throw new IllegalArgumentException();
            }

            if (propertyJson == null && nbtJsonStrict == null && nbtJson == null) {
                builder.where(key.getKey().charAt(0), BlockInWorldPredicateBuilder.of(block));
                return;
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

            builder.where(key.getKey().charAt(0), predicateBuilder);
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

            builder.where(key.getKey().charAt(0), predicateBuilder);
        } else {
            TagKey<Block> blockTag = BlockTags.create(new ResourceLocation(mappedCharacterBlockTag.getAsString()));

            if (propertyJson == null && nbtJsonStrict == null && nbtJson == null) {
                builder.where(key.getKey().charAt(0), BlockInWorldPredicateBuilder.of(blockTag));
                return;
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

            builder.where(key.getKey().charAt(0), predicateBuilder);
        }
    }
}
