package io.github.kawaiicakes.nobullship.api.schematic;

import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import io.github.kawaiicakes.nobullship.schematic.SchematicRecipe;
import net.minecraft.core.NonNullList;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Consumer;

public class SchematicRecipeBuilder {
    protected final ResourceLocation resultId;
    protected final List<String> pattern = new ArrayList<>();
    protected final Map<Character, Ingredient> key = new LinkedHashMap<>();
    protected final Map<Character, Pair<ResourceLocation, Boolean>> rlKey = new LinkedHashMap<>();
    protected final NonNullList<CompoundTag> shapeless = NonNullList.withSize(9, new CompoundTag());
    protected final NonNullList<CompoundTag> requisites = NonNullList.create();
    protected int maximumSchematicUsage;
    protected byte cursor = 0;

    protected SchematicRecipeBuilder(ResourceLocation resultId) {
        this.resultId = resultId;
    }

    /**
     * Creates a new <code>SchematicRecipeBuilder</code>. All <code>SchematicRecipe</code>s must have a
     * shaped component. Strictly speaking, that's all that is required.
     * @param resultId the <code>ResourceLocation</code> corresponding to the full recipe ID of a multiblock recipe.
     */
    public static SchematicRecipeBuilder of(ResourceLocation resultId) {
        return new SchematicRecipeBuilder(resultId);
    }

    /**
     * Adds a key to the shaped recipe pattern.
     */
    public SchematicRecipeBuilder defineShaped(Character pSymbol, TagKey<Item> pTag) {
        return this.defineShaped(pSymbol, Ingredient.of(pTag));
    }

    /**
     * Adds a key to the shaped recipe pattern.
     */
    public SchematicRecipeBuilder defineShaped(Character pSymbol, ItemLike pItem) {
        return this.defineShaped(pSymbol, Ingredient.of(pItem));
    }

    /**
     * Adds a key to the shaped recipe pattern.
     */
    public SchematicRecipeBuilder defineShaped(Character pSymbol, Ingredient pIngredient) {
        if (this.key.containsKey(pSymbol) || this.rlKey.containsKey(pSymbol)) {
            throw new IllegalArgumentException("Symbol '" + pSymbol + "' is already defined!");
        } else if (pSymbol == ' ' || pSymbol == '$') {
            throw new IllegalArgumentException("Symbol '" + pSymbol + "' is reserved and cannot be defined");
        } else {
            this.key.put(pSymbol, pIngredient);
            return this;
        }
    }

    /**
     * Adds a key to the shaped recipe pattern, but defined using a <code>ResourceLocation</code>. Used by datagen
     * to specify items which don't exist from a dependency.
     */
    public SchematicRecipeBuilder defineShaped(Character pSymbol, ResourceLocation pIngredientLocation, boolean isTag) {
        if (this.key.containsKey(pSymbol) || this.rlKey.containsKey(pSymbol)) {
            throw new IllegalArgumentException("Symbol '" + pSymbol + "' is already defined!");
        } else if (pSymbol == ' ' || pSymbol == '$') {
            throw new IllegalArgumentException("Symbol '" + pSymbol + "' is reserved and cannot be defined");
        } else {
            this.rlKey.put(pSymbol, Pair.of(pIngredientLocation, isTag));
            return this;
        }
    }

    /**
     * Adds a new entry to the patterns for the shaped component of the recipe.
     */
    public SchematicRecipeBuilder shapedPattern(String pPattern) {
        if (!this.pattern.isEmpty() && pPattern.length() != this.pattern.get(0).length()) {
            throw new IllegalArgumentException("Pattern must be the same width on every line!");
        } else {
            this.pattern.add(pPattern);
            return this;
        }
    }

    public SchematicRecipeBuilder addShapeless(ItemStack requirement) {
        if (requirement.isEmpty()) throw new IllegalArgumentException("Passed requirement may not be empty!");
        if (!this.shapeless.get(8).isEmpty()) {
            throw new IllegalArgumentException("There may be at most 9 shapeless stacks!");
        }

        CompoundTag serializedItem = new CompoundTag();

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(requirement.getItem());
        if (itemId == null) throw new IllegalArgumentException();

        serializedItem.putString("item", itemId.toString());
        serializedItem.putInt("count", requirement.getCount());
        if (requirement.hasTag()) serializedItem.put("nbt", requirement.getOrCreateTag());

        this.shapeless.set(cursor++, serializedItem);
        return this;
    }

    public SchematicRecipeBuilder addShapeless(ResourceLocation itemId, int count, @Nullable CompoundTag nbt) {
        if (count < 1) throw new IllegalArgumentException("Count may not be less than 1!");
        if (!this.shapeless.get(8).isEmpty()) {
            throw new IllegalArgumentException("There may be at most 9 shapeless stacks!");
        }

        CompoundTag serializedItem = new CompoundTag();

        serializedItem.putString("item", itemId.toString());
        serializedItem.putInt("count", count);
        if (nbt != null) serializedItem.put("nbt", nbt);

        this.shapeless.set(cursor++, serializedItem);
        return this;
    }

    public SchematicRecipeBuilder addShapeless(ResourceLocation itemId) {
        return this.addShapeless(itemId, 1, null);
    }

    public SchematicRecipeBuilder addRequisite(ItemStack requisite) {
        if (requisite.isEmpty()) throw new IllegalArgumentException("Passed requisite may not be empty!");
        CompoundTag serializedItem = new CompoundTag();

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(requisite.getItem());
        if (itemId == null) throw new IllegalArgumentException();

        serializedItem.putString("item", itemId.toString());
        serializedItem.putInt("count", requisite.getCount());
        if (requisite.hasTag()) serializedItem.put("nbt", requisite.getOrCreateTag());

        this.requisites.add(serializedItem);
        return this;
    }

    public SchematicRecipeBuilder addRequisites(NonNullList<ItemStack> requisites) {
        if (requisites.stream().anyMatch(ItemStack::isEmpty)) {
            throw new IllegalArgumentException("Passed list has an empty item!");
        }
        this.requisites.addAll(requisites.stream().map((stack) -> {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (itemId == null) throw new IllegalArgumentException();
            CompoundTag serializedItem = new CompoundTag();
            serializedItem.putString("item", itemId.toString());
            serializedItem.putInt("count", stack.getCount());
            if (stack.hasTag()) serializedItem.put("nbt", stack.getOrCreateTag());
            return serializedItem;
        }).toList());
        return this;
    }

    public SchematicRecipeBuilder addRequisite(ResourceLocation requisiteId, int count, @Nullable CompoundTag nbt) {
        CompoundTag serializedItem = new CompoundTag();

        serializedItem.putString("item", requisiteId.toString());
        serializedItem.putInt("count", count);
        if (nbt != null) serializedItem.put("nbt", nbt);

        this.requisites.add(serializedItem);
        return this;
    }

    public SchematicRecipeBuilder addRequisite(ResourceLocation requisiteId) {
        return this.addRequisite(requisiteId, 1, null);
    }

    public SchematicRecipeBuilder maxUsages(int maxUsages) {
        if (maxUsages < 1) throw new IllegalArgumentException("Max usages must be an integer greater than or equal to 1!");
        this.maximumSchematicUsage = maxUsages;
        return this;
    }

    public void save(Consumer<FinishedRecipe> pFinishedRecipeConsumer, ResourceLocation pRecipeId) {
        this.ensureValid(pRecipeId);
        pFinishedRecipeConsumer.accept(new Result(pRecipeId, this.resultId, this.pattern, this.key, this.rlKey, this.shapeless, this.requisites, this.maximumSchematicUsage));
    }

    public void ensureValid(ResourceLocation pId) {
        if (this.pattern.isEmpty()) {
            throw new IllegalStateException("No pattern is defined for shaped component in recipe " + pId + "!");
        } else {
            Set<Character> set = Sets.newHashSet(this.key.keySet());
            set.addAll(this.rlKey.keySet());
            set.remove(' ');

            for(String s : this.pattern) {
                for(int i = 0; i < s.length(); ++i) {
                    char c0 = s.charAt(i);
                    if (!this.key.containsKey(c0) && !this.rlKey.containsKey(c0) && c0 != ' ') {
                        throw new IllegalStateException("Pattern in recipe " + pId + " uses undefined symbol '" + c0 + "'");
                    }

                    set.remove(c0);
                }
            }

            if (!set.isEmpty()) {
                throw new IllegalStateException("Ingredients are defined but not used in pattern for recipe " + pId);
            }
        }
    }

    public static class Result implements FinishedRecipe {
        protected static final Logger LOGGER = LogUtils.getLogger();

        protected final ResourceLocation id;
        protected final ResourceLocation resultId;
        protected final List<String> pattern;
        protected final Map<Character, Ingredient> key;
        protected final Map<Character, Pair<ResourceLocation, Boolean>> rlKey;
        protected final NonNullList<CompoundTag> shapeless;
        protected final NonNullList<CompoundTag> requisites;
        protected final int maximumSchematicUsage;

        public Result(ResourceLocation id, ResourceLocation resultId, List<String> pattern, Map<Character, Ingredient> key, Map<Character, Pair<ResourceLocation, Boolean>> rlKey,NonNullList<CompoundTag> shapeless, NonNullList<CompoundTag> requisites, int maximumSchematicUsage) {
            this.id = id;
            this.resultId = resultId;
            this.pattern = pattern;
            this.key = key;
            this.rlKey = rlKey;
            this.shapeless = shapeless;
            this.requisites = requisites;
            this.maximumSchematicUsage = maximumSchematicUsage;
        }

        @Override
        public void serializeRecipeData(JsonObject pJson) {
            pJson.addProperty("type", "nobullship:schematic_workbench");

            //noinspection SpellCheckingInspection
            JsonObject declarason = new JsonObject();

            declarason.addProperty("output_id", this.resultId.toString());

            JsonObject keyJson = new JsonObject();
            for (Map.Entry<Character, Ingredient> entry : this.key.entrySet()) {
                keyJson.add(String.valueOf(entry.getKey()), entry.getValue().toJson());
            }
            for (Map.Entry<Character, Pair<ResourceLocation, Boolean>> entry : this.rlKey.entrySet()) {
                JsonObject ingredientJson = new JsonObject();

                if (entry.getValue().getSecond()) {
                    ingredientJson.addProperty("tag", entry.getKey().toString());
                } else {
                    ingredientJson.addProperty("item", entry.getKey().toString());
                }

                keyJson.add(String.valueOf(entry.getKey()), ingredientJson);
            }
            declarason.add("key", keyJson);

            JsonArray patternJson = new JsonArray();
            for (String s : this.pattern) {
                patternJson.add(s);
            }
            declarason.add("shaped_input", patternJson);

            pJson.add("declaration", declarason);

            JsonArray shapelessJson = new JsonArray();
            for (CompoundTag itemTag : this.shapeless) {
                if (itemTag.getString("item").isEmpty()) continue;
                JsonObject serializedItem = new JsonObject();
                serializedItem.addProperty("item", itemTag.getString("item"));
                if (itemTag.getInt("count") > 1) serializedItem.addProperty("count", itemTag.getInt("count"));
                if (!itemTag.getCompound("nbt").isEmpty()) serializedItem.add("nbt", CompoundTag.CODEC.encodeStart(JsonOps.INSTANCE, itemTag.getCompound("nbt")).getOrThrow(false, LOGGER::error));
                shapelessJson.add(serializedItem);
            }
            pJson.add("shapeless_input", shapelessJson);

            if (!this.requisites.isEmpty()) {
                JsonArray requisitesJson = new JsonArray();
                for (CompoundTag itemTag : this.requisites) {
                    if (itemTag.getString("item").isEmpty()) continue;
                    JsonObject serializedItem = new JsonObject();
                    serializedItem.addProperty("item", itemTag.getString("item"));
                    if (itemTag.getInt("count") > 0) serializedItem.addProperty("count", itemTag.getInt("count"));
                    if (!itemTag.getCompound("nbt").isEmpty()) serializedItem.add("nbt", CompoundTag.CODEC.encodeStart(JsonOps.INSTANCE, itemTag.getCompound("nbt")).getOrThrow(false, LOGGER::error));
                    requisitesJson.add(serializedItem);
                }
                pJson.add("requisites", requisitesJson);
            }

            if (maximumSchematicUsage > 0) pJson.addProperty("max_usages", this.maximumSchematicUsage);
        }

        @Override
        public ResourceLocation getId() {
            return this.id;
        }

        @Override
        public RecipeSerializer<?> getType() {
            return SchematicRecipe.Serializer.INSTANCE;
        }

        @Nullable
        @Override
        public JsonObject serializeAdvancement() {
            return null;
        }

        @Nullable
        @Override
        public ResourceLocation getAdvancementId() {
            return null;
        }
    }
}
