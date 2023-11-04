package io.github.kawaiicakes.nobullship.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPatternBuilder;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class MultiblockRecipeBuilder extends BlockPatternBuilder {
    protected final ResourceLocation result;

    protected MultiblockRecipeBuilder(ResourceLocation result) {
        super();
        this.result = result;
    }

    public static MultiblockRecipeBuilder of(ResourceLocation result) {
        return new MultiblockRecipeBuilder(result);
    }

    public ResourceLocation getResult() {
        return this.result;
    }

    public void save(Consumer<FinishedMultiblockRecipe> consumer, ResourceLocation id) {
        consumer.accept(new Result(id, this.result, this.pattern, this.lookup, this.height, this.width));
    }

    @Override
    public MultiblockRecipeBuilder aisle(String... pAisle) {
        return (MultiblockRecipeBuilder) super.aisle(pAisle);
    }

    @Override
    public MultiblockRecipeBuilder where(char pSymbol, Predicate<BlockInWorld> pBlockMatcher) {
        return (MultiblockRecipeBuilder) super.where(pSymbol, pBlockMatcher);
    }

    public static class Result implements FinishedMultiblockRecipe {
        protected final ResourceLocation id;
        protected final ResourceLocation result;
        protected final List<String[]> recipe;
        protected final Map<Character, Predicate<BlockInWorld>> lookup;
        protected final int height;
        protected final int width;

        public Result(ResourceLocation id, ResourceLocation result, List<String[]> recipe, Map<Character, Predicate<BlockInWorld>> lookup, int height, int width) {
            this.id = id;
            this.result = result;
            this.recipe = recipe;
            this.lookup = lookup;
            this.height = height;
            this.width = width;
        }

        @Override
        public void serializeRecipeData(JsonObject pJson) {
            // TODO - add conditional serialization stuff
            JsonObject keyMappings = new JsonObject();
            for (Map.Entry<Character, Predicate<BlockInWorld>> entry : this.lookup.entrySet()) {
                JsonObject mapping = new JsonObject();
                // TODO - figure out how tf to serialize Predicate<BlockInWorld> then continue this
                mapping.addProperty();
                keyMappings.add(String.valueOf(entry.getKey()), mapping);
            }

            JsonObject recipePattern = new JsonObject();
            for (int i = 0; i < this.recipe.size(); i++) {
                recipePattern.add("z" + i, new JsonArray(this.recipe.get(i).length));

                for (int j = 0; j < this.recipe.get(i).length; j++) {
                    recipePattern.getAsJsonArray("z" + i).add(this.recipe.get(i)[j]);
                }
            }

            pJson.add("key", keyMappings);
            pJson.add("recipe", recipePattern);
            pJson.addProperty("height", this.height);
            pJson.addProperty("width", this.width);
            pJson.addProperty("result", this.result.toString());
        }

        @Override
        public ResourceLocation getId() {
            return this.id;
        }
    }
}
