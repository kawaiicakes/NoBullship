package io.github.kawaiicakes.nobullship.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPatternBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static net.minecraft.world.level.block.Blocks.AIR;
import static net.minecraftforge.registries.ForgeRegistries.BLOCKS;

public class MultiblockRecipeBuilder extends BlockPatternBuilder {
    protected final ResourceLocation result;
    protected final Map<String, BlockState> lookupSimple = new HashMap<>();

    protected MultiblockRecipeBuilder(ResourceLocation result) {
        super();
        this.result = result;
    }

    public static MultiblockRecipeBuilder of(ResourceLocation result) {
        return (MultiblockRecipeBuilder) (new MultiblockRecipeBuilder(result)).where(' ', AIR.defaultBlockState()).where('$', (state) -> true);
    }

    public ResourceLocation getResult() {
        return this.result;
    }

    public void save(Consumer<FinishedMultiblockRecipe> consumer, ResourceLocation id) {
        consumer.accept(new Result(id, this.result, this.pattern, this.lookupSimple, this.height, this.width));
    }

    @Override
    public MultiblockRecipeBuilder aisle(String... pAisle) {
        return (MultiblockRecipeBuilder) super.aisle(pAisle);
    }

    /**
     * This method must be used if a proper lookup key mapping is to be generated.
     */
    public MultiblockRecipeBuilder where(char pSymbol, BlockState state) {
        this.lookupSimple.put(String.valueOf(pSymbol), state);
        return (MultiblockRecipeBuilder) this.where(pSymbol, BlockInWorld.hasState(testState -> Objects.equals(testState, state)));
    }

    public static class Result implements FinishedMultiblockRecipe {
        protected final ResourceLocation id;
        protected final ResourceLocation result;
        protected final List<String[]> recipe;
        protected final Map<String, BlockState> lookup;
        protected final int height;
        protected final int width;

        public Result(ResourceLocation id, ResourceLocation result, List<String[]> recipe, Map<String, BlockState> lookup, int height, int width) {
            this.id = id;
            this.result = result;
            this.recipe = recipe;
            this.lookup = lookup;
            this.height = height;
            this.width = width;
        }

        @Override
        public void serializeRecipeData(JsonObject pJson) {
            // TODO - add conditional serialization stuff
            JsonObject keyMappings = new JsonObject();
            for (Map.Entry<String, BlockState> entry : this.lookup.entrySet()) {
                if (entry.getKey().equals(" ")) continue;
                JsonObject mapping = new JsonObject();

                JsonObject properties = new JsonObject();
                entry.getValue().getValues().forEach((property, comparable) ->
                        properties.addProperty(property.getName(), comparable.toString()));

                // Given where this method is being called, it's impossible for the registry to not be loaded... right?
                // noinspection DataFlowIssue
                mapping.addProperty("block", BLOCKS.getKey(entry.getValue().getBlock()).toString());
                if (properties.size() != 0) mapping.add("state", properties);
                keyMappings.add(String.valueOf(entry.getKey()), mapping);
            }

            JsonObject recipePattern = new JsonObject();
            for (int i = 0; i < this.recipe.size(); i++) {
                recipePattern.add("z" + i, new JsonArray(this.recipe.get(i).length));

                for (int j = 0; j < this.recipe.get(i).length; j++) {
                    recipePattern.getAsJsonArray("z" + i).add(this.recipe.get(i)[j]);
                }
            }

            pJson.add("key", keyMappings);
            pJson.add("recipe", recipePattern);
            pJson.addProperty("result", this.result.toString());
        }

        @Override
        public ResourceLocation getId() {
            return this.id;
        }
    }
}
