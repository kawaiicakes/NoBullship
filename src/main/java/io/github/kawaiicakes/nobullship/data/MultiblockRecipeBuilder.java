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
            JsonObject recipePattern = new JsonObject();

            for (int i = 0; i < recipe.size(); i++) {
                recipePattern.add("z" + i, new JsonArray(recipe.get(i).length));

                for (int j = 0; j < recipe.get(i).length; j++) {
                    recipePattern.getAsJsonArray("z" + i).add(recipe.get(i)[j]);
                }
            }

            pJson.add("recipe", recipePattern);
            pJson.addProperty("result", this.result.toString());
        }

        @Override
        public ResourceLocation getId() {
            return this.id;
        }
    }
}
