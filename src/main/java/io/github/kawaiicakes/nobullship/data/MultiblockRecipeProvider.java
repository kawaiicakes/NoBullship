package io.github.kawaiicakes.nobullship.data;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static net.minecraft.world.item.Items.DIRT;
import static net.minecraft.world.item.Items.GRASS_BLOCK;

public class MultiblockRecipeProvider extends RecipeProvider {
    public static void generateTestRecipe(Consumer<FinishedRecipe> writer) {
        ShapelessRecipeBuilder.shapeless(DIRT).unlockedBy("has_dirt", has(DIRT)).requires(GRASS_BLOCK).save(writer);
    }

    public MultiblockRecipeProvider(DataGenerator pGenerator) {
        super(pGenerator);
    }

    @Override
    protected void buildCraftingRecipes(@NotNull Consumer<FinishedRecipe> pFinishedRecipeConsumer) {
        generateTestRecipe(pFinishedRecipeConsumer);
    }
}
