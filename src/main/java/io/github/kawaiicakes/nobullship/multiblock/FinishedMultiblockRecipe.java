package io.github.kawaiicakes.nobullship.multiblock;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

public interface FinishedMultiblockRecipe {
    void serializeRecipeData(JsonObject pJson);

    /**
     * Gets the JSON for the recipe.
     */
    default JsonObject serializeRecipe() {
        JsonObject jsonobject = new JsonObject();
        this.serializeRecipeData(jsonobject);
        return jsonobject;
    }

    /**
     * Gets the ID for the recipe.
     */
    ResourceLocation getId();
}
