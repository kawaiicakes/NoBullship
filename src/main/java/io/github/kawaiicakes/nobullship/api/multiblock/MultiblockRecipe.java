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
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Bears no relation to <code>FinishedMultiblockRecipe</code>. Simply an immutable data carrier intended to cache
 * multiblock recipes.
 * @param resultingEntityName a <code>String</code> representing the translatable key of the text displayed in the workshop for the entity this recipe creates.
 */
@ParametersAreNonnullByDefault
@FieldsAreNonnullByDefault
public record MultiblockRecipe(
        @Nullable String resultingEntityName,
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

        if (this.resultingEntityName != null) {
            toReturn.putString("name", this.resultingEntityName);
        }

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

        String name = null;
        if (nbt.get("name") instanceof StringTag stringTag) name = stringTag.getAsString();

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
                name,
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

        try {
            String name = null;
            if (jsonResult.has("name") && jsonResult.get("name").isJsonPrimitive()) {
                name = jsonResult.getAsJsonPrimitive("name").getAsString();
            }

            ResourceLocation result = new ResourceLocation(jsonResult.getAsJsonPrimitive("entity").getAsString());

            CompoundTag nbt = null;
            if (jsonResult.has("nbt")) {
                nbt = (CompoundTag) JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, jsonResult.get("nbt"));
            }

            MultiblockPatternBuilder builder = MultiblockPatternBuilder.of(result);

            for (Map.Entry<String, JsonElement> keyEntry : jsonKeys.entrySet()) {
                if (Objects.equals(keyEntry.getKey(), " ") || Objects.equals(keyEntry.getKey(), "$")) {
                    LOGGER.error("{} is a reserved character!", keyEntry.getKey());
                    return null;
                }

                builder.where(keyEntry.getKey().charAt(0), BlockInWorldPredicateBuilder.fromJson(keyEntry.getValue().getAsJsonObject()));
            }

            for (int i = jsonRecipe.size() - 1; i >= 0 ; i--) {
                JsonArray aisle = jsonRecipe.getAsJsonArray("layer" + i);
                if (aisle.isEmpty()) return null;

                List<String> strings = new ArrayList<>(aisle.size());
                aisle.forEach(element -> strings.add(element.getAsString()));

                builder.aisle(strings.toArray(String[]::new));
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

            return new MultiblockRecipe(name, builder.build(), result, nbt, toReturnRequisites);
        } catch (RuntimeException e) {
            LOGGER.error("An error occurred during deserialization of BlockInWorldPredicate from JSON!", e);
            LOGGER.error(e.getMessage());
            return null;
        }
    }
}
