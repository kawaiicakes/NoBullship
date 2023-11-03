package io.github.kawaiicakes.nobullship.data;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import static io.github.kawaiicakes.nobullship.NoBullship.MOD_ID;
import static net.minecraft.data.DataGenerator.Target.DATA_PACK;

public class MultiblockRecipeProvider implements DataProvider {
    protected final DataGenerator.PathProvider path;

    public MultiblockRecipeProvider(DataGenerator pGenerator) {
        this.path = pGenerator.createPathProvider(DATA_PACK, "recipes");
    }

    @Override
    public void run(CachedOutput pOutput) throws IOException {
        Set<ResourceLocation> recipeIds = new HashSet<>();
    }

    @Override
    public String getName() {
        return MOD_ID + ":multiblock_recipe_provider";
    }
}
