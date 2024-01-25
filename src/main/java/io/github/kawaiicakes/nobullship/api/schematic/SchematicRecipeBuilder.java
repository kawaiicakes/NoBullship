package io.github.kawaiicakes.nobullship.api.schematic;

import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
    protected final NonNullList<ItemStack> shapeless = NonNullList.withSize(9, ItemStack.EMPTY);
    protected final NonNullList<ItemStack> requisites = NonNullList.create();
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

    public SchematicRecipeBuilder addRequisite(ItemStack requisite) {
        if (requisite.isEmpty()) throw new IllegalArgumentException("Passed requisite may not be empty!");
        this.requisites.add(requisite);
        return this;
    }

    public SchematicRecipeBuilder addRequisites(NonNullList<ItemStack> requisites) {
        if (requisites.stream().anyMatch(ItemStack::isEmpty)) {
            throw new IllegalArgumentException("Passed list has an empty item!");
        }
        this.requisites.addAll(requisites);
        return this;
    }

    public SchematicRecipeBuilder maxUsages(int maxUsages) {
        if (maxUsages < 1) throw new IllegalArgumentException("Max usages must be an integer greater than or equal to 1!");
        this.maximumSchematicUsage = maxUsages;
        return this;
    }

    public void save(Consumer<FinishedRecipe> pFinishedRecipeConsumer, ResourceLocation pRecipeId) {
        this.ensureValid(pRecipeId);
        pFinishedRecipeConsumer.accept(new Result(pRecipeId, this.resultId, this.pattern, this.key, this.shapeless, this.requisites, this.maximumSchematicUsage));
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
            }
        }
    }

    public static class Result implements FinishedRecipe {
        protected static final Logger LOGGER = LogUtils.getLogger();

        protected final ResourceLocation id;
        protected final ResourceLocation resultId;
        protected final List<String> pattern;
        protected final Map<Character, Ingredient> key;
        protected final NonNullList<ItemStack> shapeless;
        protected final NonNullList<ItemStack> requisites;
        protected final int maximumSchematicUsage;

        public Result(ResourceLocation id, ResourceLocation resultId, List<String> pattern, Map<Character, Ingredient> key, NonNullList<ItemStack> shapeless, NonNullList<ItemStack> requisites, int maximumSchematicUsage) {
            this.id = id;
            this.resultId = resultId;
            this.pattern = pattern;
            this.key = key;
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
                if (item.isEmpty()) continue;
                JsonObject serializedItem = new JsonObject();
                serializedItem.addProperty("item", Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(item.getItem())).toString());
                if (item.getCount() > 1) {
                    serializedItem.addProperty("count", item.getCount());
                }
                if (item.hasTag()) {
                    serializedItem.add("nbt", CompoundTag.CODEC.encodeStart(JsonOps.INSTANCE, item.getOrCreateTag()).getOrThrow(false, LOGGER::error));
                }
                shapelessJson.add(serializedItem);
            }
            pJson.add("shapeless_input", shapelessJson);

            if (!this.requisites.isEmpty()) {
                JsonArray requisitesJson = new JsonArray();
                for (ItemStack item : this.requisites) {
                    JsonElement serialized = ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, item).get().orThrow();
                    requisitesJson.add(serialized);
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
