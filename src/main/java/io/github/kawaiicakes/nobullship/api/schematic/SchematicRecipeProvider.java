package io.github.kawaiicakes.nobullship.api.schematic;

import io.github.kawaiicakes.nobullship.compat.recipes.siegemachines.SiegeMachinesSchematics;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.ConditionalRecipe;
import net.minecraftforge.common.crafting.conditions.ModLoadedCondition;

import java.util.function.Consumer;

import static io.github.kawaiicakes.nobullship.Registry.SIMPLE_WOOD_BEAM_BLOCK;

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
        SiegeMachinesSchematics.generateRecipes(pFinishedRecipeConsumer);
    }
}
