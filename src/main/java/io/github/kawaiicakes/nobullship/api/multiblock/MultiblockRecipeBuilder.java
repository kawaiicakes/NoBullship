package io.github.kawaiicakes.nobullship.api.multiblock;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPatternBuilder;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static net.minecraft.world.level.block.Blocks.AIR;
import static net.minecraftforge.registries.ForgeRegistries.BLOCKS;

public class MultiblockRecipeBuilder extends BlockPatternBuilder {
    protected final Logger LOGGER = LogUtils.getLogger();
    protected final ResourceLocation result;
    @Nullable
    protected CompoundTag nbt;
    protected final Map<String, BlockState> lookupSimple = new HashMap<>();

    protected MultiblockRecipeBuilder(ResourceLocation result, @Nullable CompoundTag nbt) {
        super();
        this.result = result;
        this.nbt = nbt;
    }

    public static MultiblockRecipeBuilder of(ResourceLocation result) {
        return (MultiblockRecipeBuilder) (new MultiblockRecipeBuilder(result, null)).where(' ', (state) -> state.getState().is(AIR)).where('$', (state) -> true);
    }

    public static MultiblockRecipeBuilder of(ResourceLocation result, CompoundTag nbt) {
        return (MultiblockRecipeBuilder) (new MultiblockRecipeBuilder(result, nbt)).where(' ', (state) -> state.getState().is(AIR)).where('$', (state) -> true);
    }

    public ResourceLocation getResult() {
        return this.result;
    }

    @Override
    public MultiblockPattern build() {
        return new MultiblockPattern(this.createPattern());
    }

    public void save(Consumer<FinishedMultiblockRecipe> consumer, ResourceLocation id) {
        consumer.accept(new Result(id, this.result, this.nbt, this.pattern, this.lookupSimple, this.height, this.width));
    }

    @Override
    public MultiblockRecipeBuilder aisle(String... pAisle) {
        return (MultiblockRecipeBuilder) super.aisle(pAisle);
    }

    /**
     * This method must be used if a proper lookup key mapping is to be generated.
     */
    public MultiblockRecipeBuilder where(char pSymbol, BlockState state) {
        if (pSymbol == ' ' || pSymbol == '$') {
            LOGGER.error("{} is a reserved character!", pSymbol);
            throw new IllegalArgumentException(pSymbol + " is a reserved character!");
        }

        this.lookupSimple.put(String.valueOf(pSymbol), state);
        return (MultiblockRecipeBuilder) this.where(pSymbol, BlockInWorld.hasState(testState -> Objects.equals(testState, state)));
    }

    public static class Result implements FinishedMultiblockRecipe {
        protected final ResourceLocation id;
        protected final ResourceLocation result;
        @Nullable
        protected final CompoundTag nbt;
        protected final List<String[]> recipe;
        protected final Map<String, BlockState> lookup;
        protected final int height;
        protected final int width;

        public Result(ResourceLocation id, ResourceLocation result, @Nullable CompoundTag nbt, List<String[]> recipe, Map<String, BlockState> lookup, int height, int width) {
            this.id = id;
            this.result = result;
            this.nbt = nbt;
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
                recipePattern.add("layer" + i, new JsonArray(this.recipe.get(i).length));

                for (int j = 0; j < this.recipe.get(i).length; j++) {
                    recipePattern.getAsJsonArray("layer" + i).add(this.recipe.get(i)[j]);
                }
            }

            JsonObject jsonResult = new JsonObject();
            jsonResult.addProperty("entity", this.result.toString());
            if (this.nbt != null) jsonResult.add("nbt", NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, this.nbt));

            pJson.add("key", keyMappings);
            pJson.add("recipe", recipePattern);
            pJson.add("result", jsonResult);
        }

        @Override
        public ResourceLocation getId() {
            return this.id;
        }
    }
}
