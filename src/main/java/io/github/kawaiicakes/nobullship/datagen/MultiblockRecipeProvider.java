package io.github.kawaiicakes.nobullship.datagen;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.nobullship.data.FinishedMultiblockRecipe;
import io.github.kawaiicakes.nobullship.data.MultiblockRecipeBuilder;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.predicate.BlockStatePredicate;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.Consumer;

import static io.github.kawaiicakes.nobullship.NoBullship.MOD_ID;
import static net.minecraft.data.DataGenerator.Target.DATA_PACK;

public class MultiblockRecipeProvider implements DataProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    protected final DataGenerator.PathProvider path;

    public MultiblockRecipeProvider(DataGenerator pGenerator, ExistingFileHelper fileHelper) {
        this.path = pGenerator.createPathProvider(DATA_PACK, "multiblock_recipes");
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
        return MOD_ID + ":" + "multiblock_recipe_provider";
    }

    protected void buildRecipes(Consumer<FinishedMultiblockRecipe> consumer) {
        buildCreeper(consumer);
    }

    private static void saveRecipe(CachedOutput pOutput, JsonObject pRecipeJson, Path pPath) {
        try {
            DataProvider.saveStable(pOutput, pRecipeJson, pPath);
        } catch (IOException ioexception) {
            LOGGER.error("Couldn't save recipe {}", pPath, ioexception);
        }
    }

    private static void buildCreeper(Consumer<FinishedMultiblockRecipe> consumer) {
        ((MultiblockRecipeBuilder) MultiblockRecipeBuilder.of(new ResourceLocation("minecraft", "creeper"))
                .where('#', BlockInWorld.hasState(BlockStatePredicate.forBlock(Blocks.LIME_WOOL)))
                .aisle(
                        " # ",
                        " # ",
                        "###"
                )).save(consumer, new ResourceLocation("minecraft", "creeper"));
    }
}
