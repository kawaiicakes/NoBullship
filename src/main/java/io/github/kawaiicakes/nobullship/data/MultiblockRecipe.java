package io.github.kawaiicakes.nobullship.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.logging.LogUtils;
import net.minecraft.FieldsAreNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

import static net.minecraftforge.registries.ForgeRegistries.BLOCKS;

/**
 * Bears no relation to <code>FinishedMultiblockRecipe</code>. Simply an immutable data carrier intended to cache
 * multiblock recipes.
 */
@ParametersAreNonnullByDefault
@FieldsAreNonnullByDefault
public record MultiblockRecipe(
        BlockPattern recipe,
        ResourceLocation result
) {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Essentially a Json deserializer. Returns null if the argument contains syntax errors.
     */
    @Nullable
    public static MultiblockRecipe fromJson(JsonObject json) {
        JsonObject jsonKeys = json.getAsJsonObject("key");
        JsonObject jsonRecipe = json.getAsJsonObject("recipe");
        JsonPrimitive jsonResult = json.getAsJsonPrimitive("result");

        if (jsonKeys == null || jsonRecipe == null || jsonResult == null) return null;

        ResourceLocation result = new ResourceLocation(jsonResult.getAsString());

        MultiblockRecipeBuilder builder = MultiblockRecipeBuilder.of(result);

        for (Map.Entry<String, JsonElement> entry : jsonKeys.entrySet()) {
            if (Objects.equals(entry.getKey(), " ") || Objects.equals(entry.getKey(), "$")) {
                LOGGER.error("{} is a reserved character", entry.getKey());
                return null;
            }

            JsonElement mappedCharacterBlock = entry.getValue().getAsJsonObject().get("block");

            if (mappedCharacterBlock == null) return null;

            ResourceLocation blockLocation = new ResourceLocation(mappedCharacterBlock.getAsString());
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

                if (blockStateJson.get(keyPair.getKey()) == null) return null;
                String propertyValue = blockStateJson.get(keyPair.getKey()).getAsString();
                deserializedState.put(property, propertyValue);
            }

            builder.where(entry.getKey().charAt(0), BlockInWorld.hasState(testState -> {
                for (Map.Entry<Property<?>, String> propertyEntry : deserializedState.entrySet()) {
                    if (!testState.hasProperty(propertyEntry.getKey())) return false;
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

        return new MultiblockRecipe(builder.build(), result);
    }
}
