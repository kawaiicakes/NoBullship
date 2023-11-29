package io.github.kawaiicakes.nobullship.api.multiblock;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import io.github.kawaiicakes.nobullship.api.BlockInWorldPredicateBuilder;
import io.github.kawaiicakes.nobullship.multiblock.MultiblockPattern;
import net.minecraft.FieldsAreNonnullByDefault;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.properties.Property;
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
        return new MultiblockPattern(this.recipe.getPattern(), this.recipe.getPalette(), this.recipe.getTotalBlocks());
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

    public CompoundTag toNbt() {
        CompoundTag toReturn = new CompoundTag();

        toReturn.put("recipe", this.recipe.toNbt());

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

    public static MultiblockRecipe fromNbt(CompoundTag nbt) {
        if (!(nbt.get("recipe") instanceof CompoundTag recipeNbt)) throw new IllegalArgumentException("Passed NBT does not contain a recipe!");
        if (!(nbt.get("result") instanceof StringTag stringNbt)) throw new IllegalArgumentException("Passed NBT does not contain a string!");

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
                MultiblockPattern.fromNbt(recipeNbt),
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
                LOGGER.error("{} is a reserved character", keyEntry.getKey());
                return null;
            }

            JsonElement mappedCharacterBlock = keyEntry.getValue().getAsJsonObject().get("block");

            if (mappedCharacterBlock == null) {
                LOGGER.error("There is no block associated with this recipe!");
                return null;
            }

            ResourceLocation blockLocation = new ResourceLocation(mappedCharacterBlock.getAsString());
            Block block = RegistryObject.create(blockLocation, BLOCKS).get();

            JsonObject blockStateJson = keyEntry.getValue().getAsJsonObject().getAsJsonObject("state");
            JsonObject nbtJsonStrict = keyEntry.getValue().getAsJsonObject().getAsJsonObject("nbt_strict");
            JsonObject nbtJson = keyEntry.getValue().getAsJsonObject().getAsJsonObject("nbt");

            if ((nbtJsonStrict != null || nbtJson != null) && !(block instanceof EntityBlock)) {
                LOGGER.error("Block {} does not have a block entity and cannot hold NBT data!", block);
                return null;
            }

            if (blockStateJson == null && nbtJsonStrict == null && nbtJson == null) {
                builder.where(keyEntry.getKey().charAt(0), BlockInWorldPredicateBuilder.of(block));
                continue;
            }

            if (blockStateJson == null) blockStateJson = new JsonObject();

            final Map<Property<?>, Set<String>> deserializedState = new HashMap<>(blockStateJson.size());
            for (Map.Entry<String, JsonElement> stateEntry : blockStateJson.entrySet()) {
                if (!stateEntry.getValue().isJsonArray()) {
                    LOGGER.error("Values for property {} are not a valid JSON array!", stateEntry.getKey());
                    return null;
                }

                Property<?> property = block.getStateDefinition().getProperty(stateEntry.getKey());
                if (property == null) {
                    LOGGER.error("Property {} does not exist for {}", stateEntry.getKey(), blockLocation);
                    return null;
                }

                if (blockStateJson.get(stateEntry.getKey()) == null) {
                    // wtf is this doing??? why did I write this? It seems pretty roundabout...
                    return null;
                }

                Set<String> propertyValues = new HashSet<>();
                for (JsonElement element : stateEntry.getValue().getAsJsonArray()) {
                    if (!element.isJsonPrimitive()) {
                        LOGGER.error("Element {} in value definition for property {} is not a valid primitive!", element, stateEntry.getKey());
                        return null;
                    }
                    propertyValues.add(element.getAsString());
                }

                deserializedState.put(property, propertyValues);
            }

            BlockInWorldPredicateBuilder predicateBuilder = BlockInWorldPredicateBuilder.of(block);
            for (Map.Entry<Property<?>, Set<String>> stateEntry : deserializedState.entrySet()) {
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

            builder.where(keyEntry.getKey().charAt(0), predicateBuilder);
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
}
