package io.github.kawaiicakes.nobullship.api.schematic;

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
        // TODO: proper provider usage
        //noinspection SpellCheckingInspection
        ConditionalRecipe.builder()
                .addCondition(new ModLoadedCondition("siegemachines"))
                .addRecipe((consumer) ->
                        SchematicRecipeBuilder
                                .of(new ResourceLocation("siegemachines:battering_ram"))
                                .defineShaped('o', SIMPLE_WOOD_BEAM_BLOCK.get())
                                .shapedPattern("o")
                                .save(consumer, new ResourceLocation("siegemachines:battering_ram")))
                .build(pFinishedRecipeConsumer, new ResourceLocation("siegemachines:battering_ram"));
    }
}
