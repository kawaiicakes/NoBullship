package io.github.kawaiicakes.nobullship.datagen;

import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.block.state.pattern.BlockPatternBuilder;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

public class MultiblockRecipeManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
    private static final Logger LOGGER = LogUtils.getLogger();

    private Map<ResourceLocation, BlockPattern> recipes = ImmutableMap.of();

    public MultiblockRecipeManager() {
        super(GSON, "entity_recipes");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        ImmutableMap.Builder<ResourceLocation, BlockPattern> builder = ImmutableMap.builder();

        for(Map.Entry<ResourceLocation, JsonElement> entry : pObject.entrySet()) {
            ResourceLocation resourcelocation = entry.getKey();
            if (resourcelocation.getPath().startsWith("_")) continue;

            try {
                BlockPattern recipe = fromJson(GsonHelper.convertToJsonObject(entry.getValue(), "top element"));
                if (recipe == null) continue;
                builder.put(resourcelocation, recipe);
            } catch (IllegalArgumentException | JsonParseException jsonParseException) {
                LOGGER.error("Parsing error loading recipe {}", resourcelocation, jsonParseException);
            }
        }

        this.recipes = builder.build();
        LOGGER.info("Loaded {} recipes", recipes.size());
    }

    @Nullable
    public BlockPattern checkPattern(ResourceLocation recipeId) {
        return this.recipes.getOrDefault(recipeId, null);
    }

    /**
     * Essentially a deserializer.
     */
    @Nullable
    protected static BlockPattern fromJson(JsonObject json) {
        JsonObject jsonRecipe = json.getAsJsonObject("recipe");
        BlockPatternBuilder builder = BlockPatternBuilder.start();

        for (int i = 0; i < json.size(); i++) {
            JsonArray aisle = jsonRecipe.getAsJsonArray("z" + i);
            if (aisle.isEmpty()) return null;
        }

        return builder.build();
    }
}
