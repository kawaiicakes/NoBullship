package io.github.kawaiicakes.nobullship.api.schematic;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;

import java.util.function.Consumer;

/**
 * Use this class to datagen schematic recipes. Works pretty much exactly like subclassing <code>RecipeProvider</code>
 * itself. Just override <code>#buildCraftingRecipes</code>. {@link SchematicRecipeBuilder} provides easy ways to build
 * recipes inside of overrides. Simply call <code>#save</code> just like a normal <code>RecipeBuilder</code>.
 * <br><br>
 * p.s. Don't call super in implementations otherwise duplicate recipes will be generated and deleted, needlessly
 * using resources.
 */
public class SchematicRecipeProvider extends RecipeProvider {
    public SchematicRecipeProvider(DataGenerator pGenerator) {
        super(pGenerator);
    }

    @Override
    protected void buildCraftingRecipes(Consumer<FinishedRecipe> pFinishedRecipeConsumer) {
    }
}
