package io.github.kawaiicakes.nobullship.datagen;

import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.block.state.pattern.BlockPatternBuilder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;

import static net.minecraftforge.registries.ForgeRegistries.BLOCKS;

public class MultiblockRecipeManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * A map available on the serverside containing the recipe id as a key, and a <code>Pair</code> of the
     * <code>BlockPattern</code>, which is the recipe, and the result of the recipe as a <code>ResourceLocation</code>.
     */
    private Map<ResourceLocation, Pair<BlockPattern, ResourceLocation>> recipes = ImmutableMap.of();

    public MultiblockRecipeManager() {
        super(GSON, "entity_recipes");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        ImmutableMap.Builder<ResourceLocation, Pair<BlockPattern, ResourceLocation>> builder = ImmutableMap.builder();

        for(Map.Entry<ResourceLocation, JsonElement> entry : pObject.entrySet()) {
            ResourceLocation resourcelocation = entry.getKey();
            if (resourcelocation.getPath().startsWith("_")) continue;

            try {
                Pair<BlockPattern, ResourceLocation> recipe = fromJson(GsonHelper.convertToJsonObject(entry.getValue(), "top element"));
                if (recipe == null) {
                    LOGGER.error("Skipping recipe " + resourcelocation + " due to invalid syntax!");
                    continue;
                }

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
        return this.recipes.getOrDefault(recipeId, null).getFirst();
    }

    // TODO: ensure NullPointerExceptions do not occur in local variable declarations.
    /**
     * Essentially a deserializer. Returns null if the argument contains syntax errors.
     */
    @Nullable
    protected static Pair<BlockPattern, ResourceLocation> fromJson(JsonObject json) {
        JsonObject jsonKeys = json.getAsJsonObject("key");
        JsonObject jsonRecipe = json.getAsJsonObject("recipe");
        ResourceLocation result = new ResourceLocation(json.getAsJsonPrimitive("result").getAsString());

        BlockPatternBuilder builder = BlockPatternBuilder.start();

        for (Map.Entry<String, JsonElement> entry : jsonKeys.entrySet()) {
            ResourceLocation blockLocation
                    = new ResourceLocation(entry.getValue().getAsJsonObject().get("block").getAsString());
            Block block = RegistryObject.create(blockLocation, BLOCKS).get();

            JsonObject blockStateJson = entry.getValue().getAsJsonObject().getAsJsonObject("state");

            if (blockStateJson == null) {
                builder.where(entry.getKey().charAt(0), BlockInWorld.hasState(testState -> testState.is(block)));
                continue;
            }

            final Map<Property<?>, String> deserializedState = new HashMap<>(blockStateJson.size());
            for (Map.Entry<String, JsonElement> keyPair : blockStateJson.entrySet()) {
                Property<?> property = block.getStateDefinition().getProperty(keyPair.getKey());
                if (property == null) {
                    LOGGER.error("Property {} does not exist for {}", keyPair.getKey(), blockLocation);
                    continue;
                }

                String propertyValue = blockStateJson.get(keyPair.getKey()).getAsString();
                deserializedState.put(property, propertyValue);
            }

            builder.where(entry.getKey().charAt(0), BlockInWorld.hasState(testState -> {
                for (Map.Entry<Property<?>, String> propertyEntry : deserializedState.entrySet()) {
                    if (!testState.getValue(propertyEntry.getKey()).toString().equals(propertyEntry.getValue())) return false;
                }
                return true;
            }));
        }

        for (int i = 0; i < jsonRecipe.size(); i++) {
            JsonArray aisle = jsonRecipe.getAsJsonArray("z" + i);
            if (aisle.isEmpty()) return null;

            List<String> strings = new ArrayList<>(aisle.size());
            aisle.forEach(element -> strings.add(element.getAsString()));

            try {
                builder.aisle(strings.toArray(String[]::new));
            } catch (IllegalArgumentException exception) {
                LOGGER.error(exception.getMessage());
            }
        }

        return new Pair<>(builder.build(), result);
    }
}
