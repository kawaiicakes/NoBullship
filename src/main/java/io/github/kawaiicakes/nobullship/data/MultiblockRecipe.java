package io.github.kawaiicakes.nobullship.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class MultiblockRecipe {
    public final ResourceLocation id;
    public final List<String[]> recipe;
    public final ResourceLocation result;

    public MultiblockRecipe(ResourceLocation id, List<String[]> recipe, ResourceLocation result) {
        this.id = id;
        this.recipe = recipe;
        this.result = result;
    }

    public JsonObject serializeRecipeData() {
        JsonObject pJson = new JsonObject();
        JsonObject recipePattern = new JsonObject();

        for (int i = 0; i < recipe.size(); i++) {
            recipePattern.add("z" + i, new JsonArray(recipe.get(i).length));
            
            for (int j = 0; j < recipe.get(i).length; j++) {
                recipePattern.getAsJsonArray("z" + i).add(recipe.get(i)[j]);
            }
        }

        pJson.add("recipe", recipePattern);
        pJson.addProperty("result", this.result.toString());
        return pJson;
    }
}
