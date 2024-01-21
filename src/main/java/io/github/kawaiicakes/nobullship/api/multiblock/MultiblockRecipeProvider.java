package io.github.kawaiicakes.nobullship.api.multiblock;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.nobullship.compat.recipes.siegemachines.SiegeMachinesRecipes;
import io.github.kawaiicakes.nobullship.multiblock.FinishedMultiblockRecipe;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.Consumer;

import static net.minecraft.data.DataGenerator.Target.DATA_PACK;

public class MultiblockRecipeProvider implements DataProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    protected final DataGenerator.PathProvider path;

    public MultiblockRecipeProvider(DataGenerator pGenerator) {
        this.path = pGenerator.createPathProvider(DATA_PACK, "entity_recipes");
    }

    @Override
    public void run(CachedOutput pOutput) {
        Set<ResourceLocation> set = Sets.newHashSet();
        buildRecipes((finishedRecipe) -> {
                if (!set.add(finishedRecipe.getId())) {
                    throw new IllegalStateException("Duplicate recipe " + finishedRecipe.getId());
                } else {
                    saveRecipe(pOutput, finishedRecipe.serializeRecipe(), this.path.json(finishedRecipe.getId()));
                }
        });
    }

    @Override
    public String getName() {
        return "No Bullship! - Entity Recipe Provider";
    }

    protected void buildRecipes(Consumer<FinishedMultiblockRecipe> consumer) {
        SiegeMachinesRecipes.generateRecipes(consumer);
    }

    private static void saveRecipe(CachedOutput pOutput, JsonObject pRecipeJson, Path pPath) {
        try {
            DataProvider.saveStable(pOutput, pRecipeJson, pPath);
        } catch (IOException ioexception) {
            LOGGER.error("Couldn't save recipe {}", pPath, ioexception);
        }
    }
}
