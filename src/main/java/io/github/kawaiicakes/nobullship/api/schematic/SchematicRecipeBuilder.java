package io.github.kawaiicakes.nobullship.api.schematic;

import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.github.kawaiicakes.nobullship.schematic.SchematicRecipe;
import net.minecraft.core.NonNullList;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public class SchematicRecipeBuilder {
    protected final ResourceLocation resultId;
    protected final List<String> pattern = new ArrayList<>();
    protected final Map<Character, Ingredient> key = new LinkedHashMap<>();
    protected final NonNullList<ItemStack> shapeless = NonNullList.withSize(9, ItemStack.EMPTY);
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
        if (this.key.containsKey(pSymbol)) {
            throw new IllegalArgumentException("Symbol '" + pSymbol + "' is already defined!");
        } else if (pSymbol == ' ') {
            throw new IllegalArgumentException("Symbol ' ' (whitespace) is reserved and cannot be defined");
        } else {
            this.key.put(pSymbol, pIngredient);
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

        this.shapeless.set(cursor++, requirement);
        return this;
    }

    public SchematicRecipeBuilder maxUsages(int maxUsages) {
        if (maxUsages < 1) throw new IllegalArgumentException("Max usages must be an integer greater than or equal to 1!");
        this.maximumSchematicUsage = maxUsages;
        return this;
    }

    public void save(Consumer<FinishedRecipe> pFinishedRecipeConsumer, ResourceLocation pRecipeId) {
        this.ensureValid(pRecipeId);
        pFinishedRecipeConsumer.accept(new Result(pRecipeId, this.resultId, this.pattern, this.key, this.shapeless, this.maximumSchematicUsage));
    }

    public void ensureValid(ResourceLocation pId) {
        if (this.pattern.isEmpty()) {
            throw new IllegalStateException("No pattern is defined for shaped component in recipe " + pId + "!");
        } else {
            Set<Character> set = Sets.newHashSet(this.key.keySet());
            set.remove(' ');

            for(String s : this.pattern) {
                for(int i = 0; i < s.length(); ++i) {
                    char c0 = s.charAt(i);
                    if (!this.key.containsKey(c0) && c0 != ' ') {
                        throw new IllegalStateException("Pattern in recipe " + pId + " uses undefined symbol '" + c0 + "'");
                    }

                    set.remove(c0);
                }
            }

            if (!set.isEmpty()) {
                throw new IllegalStateException("Ingredients are defined but not used in pattern for recipe " + pId);
            } else if (this.pattern.size() == 1 && this.pattern.get(0).length() == 1) {
                throw new IllegalStateException("Shaped recipe " + pId + " only takes in a single item - should it be a shapeless recipe instead?");
            }
        }
    }

    public static class Result implements FinishedRecipe {
        protected final ResourceLocation id;
        protected final ResourceLocation resultId;
        protected final List<String> pattern;
        protected final Map<Character, Ingredient> key;
        protected final NonNullList<ItemStack> shapeless;
        protected final int maximumSchematicUsage;

        public Result(ResourceLocation id, ResourceLocation resultId, List<String> pattern, Map<Character, Ingredient> key, NonNullList<ItemStack> shapeless, int maximumSchematicUsage) {
            this.id = id;
            this.resultId = resultId;
            this.pattern = pattern;
            this.key = key;
            this.shapeless = shapeless;
            this.maximumSchematicUsage = maximumSchematicUsage;
        }

        @Override
        public void serializeRecipeData(JsonObject pJson) {
            pJson.addProperty("type", "nobullship:schematic_workbench");

            //noinspection SpellCheckingInspection
            JsonObject declarason = new JsonObject();

            declarason.addProperty("output_id", this.resultId.toString());

            JsonObject keyJson = new JsonObject();
            for(Map.Entry<Character, Ingredient> entry : this.key.entrySet()) {
                keyJson.add(String.valueOf(entry.getKey()), entry.getValue().toJson());
            }
            declarason.add("key", keyJson);

            JsonArray patternJson = new JsonArray();
            for (String s : this.pattern) {
                patternJson.add(s);
            }
            declarason.add("shaped_input", patternJson);

            pJson.add("declaration", declarason);

            JsonArray shapelessJson = new JsonArray();
            for (ItemStack item : this.shapeless) {
                JsonElement serialized = ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, item).get().orThrow();
                shapelessJson.add(serialized);
            }
            pJson.add("shapeless_input", shapelessJson);

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
