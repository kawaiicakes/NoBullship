package io.github.kawaiicakes.nobullship.data;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import static io.github.kawaiicakes.nobullship.NoBullship.MOD_ID;
import static net.minecraft.data.DataGenerator.Target.DATA_PACK;

public class MultiblockRecipeProvider implements DataProvider {
    private static final Logger LOGGER = LogUtils.getLogger();

    protected static MultiblockRecipe testRecipe() {
        ArrayList<String[]> array = new ArrayList<>(1);
        array.add(new String[]{" # ", " # ", "###"});
        return new MultiblockRecipe(
                new ResourceLocation(MOD_ID, "balls"),
                array,
                new ResourceLocation("creeper")
        );
    }

    protected final DataGenerator.PathProvider path;

    public MultiblockRecipeProvider(DataGenerator pGenerator, ExistingFileHelper fileHelper) {
        this.path = pGenerator.createPathProvider(DATA_PACK, "multiblock_recipes");
    }

    @Override
    public void run(CachedOutput pOutput) throws IOException {
        Set<ResourceLocation> recipeIds = new HashSet<>();
        saveRecipe(pOutput, testRecipe().serializeRecipeData(), this.path.json(testRecipe().id));
    }

    @Override
    public String getName() {
        return MOD_ID + ":" + "multiblock_recipe_provider";
    }

    private static void saveRecipe(CachedOutput pOutput, JsonObject pRecipeJson, Path pPath) {
        try {
            DataProvider.saveStable(pOutput, pRecipeJson, pPath);
        } catch (IOException ioexception) {
            LOGGER.error("Couldn't save recipe {}", pPath, ioexception);
        }

    }
}
