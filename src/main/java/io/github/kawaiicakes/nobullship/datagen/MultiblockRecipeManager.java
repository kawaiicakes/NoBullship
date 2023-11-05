package io.github.kawaiicakes.nobullship.datagen;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.nobullship.data.MultiblockRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Map;

public class MultiblockRecipeManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
    private static final Logger LOGGER = LogUtils.getLogger();
    protected static MultiblockRecipeManager INSTANCE = null;

    /**
     * A map available on the serverside containing the recipe id as a key.
     */
    private Map<ResourceLocation, MultiblockRecipe> recipes = ImmutableMap.of();

    protected MultiblockRecipeManager() {
        super(GSON, "entity_recipes");
    }

    @Nullable
    public MultiblockRecipe getRecipe(ResourceLocation recipeId) {
        return this.recipes.getOrDefault(recipeId, null);
    }

    public static MultiblockRecipeManager getInstance() {
        if (INSTANCE == null) INSTANCE = new MultiblockRecipeManager();
        return INSTANCE;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        ImmutableMap.Builder<ResourceLocation, MultiblockRecipe> builder = ImmutableMap.builder();

        for(Map.Entry<ResourceLocation, JsonElement> entry : pObject.entrySet()) {
            ResourceLocation recipeId = entry.getKey();
            if (recipeId.getPath().startsWith("_")) continue;

            try {
                MultiblockRecipe deserializedRecipe
                        = MultiblockRecipe.fromJson(GsonHelper.convertToJsonObject(entry.getValue(), "top element"));
                if (deserializedRecipe == null) {
                    LOGGER.error("Skipping recipe " + recipeId + " due to invalid syntax!");
                    continue;
                }

                builder.put(recipeId, deserializedRecipe);
            } catch (IllegalArgumentException | JsonParseException jsonParseException) {
                LOGGER.error("Parsing error loading recipe {}", recipeId, jsonParseException);
            }
        }

        this.recipes = builder.build();
        LOGGER.info("Loaded {} recipes", recipes.size());
    }
}
