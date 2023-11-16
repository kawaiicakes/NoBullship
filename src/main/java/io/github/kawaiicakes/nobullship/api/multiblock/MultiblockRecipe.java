package io.github.kawaiicakes.nobullship.api.multiblock;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import io.github.kawaiicakes.nobullship.multiblock.MultiblockPattern;
import net.minecraft.FieldsAreNonnullByDefault;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
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
        MultiblockPattern recipe,
        ResourceLocation result,
        @Nullable CompoundTag nbt,
        @Nullable ImmutableList<ItemStack> requisites
) {
    private static final Logger LOGGER = LogUtils.getLogger();
    @Override
    public MultiblockPattern recipe() {
        return new MultiblockPattern(this.recipe.getPattern());
    }

    @Override
    public ResourceLocation result() {
        return new ResourceLocation(this.result.toString());
    }

    @Nullable
    @Override
    public CompoundTag nbt() {
        return this.nbt != null ? this.nbt.copy() : null;
    }

    /**
     * This shit is made like this to ensure I return a shallow copy; it kept returning mutable values
     */
    @Nullable
    @Override
    public ImmutableList<ItemStack> requisites() {
        if (this.requisites == null) return null;
        ImmutableList.Builder<ItemStack> toReturn = ImmutableList.builder();
        for (ItemStack stack : this.requisites) {
            toReturn.add(stack.copy());
        }
        return toReturn.build();
    }

    /**
     * Essentially a Json deserializer. Returns null if the argument contains syntax errors.
     */
    @Nullable
    public static MultiblockRecipe fromJson(JsonObject json) {
        JsonObject jsonKeys = json.getAsJsonObject("key");
        JsonObject jsonRecipe = json.getAsJsonObject("recipe");
        JsonObject jsonResult = json.getAsJsonObject("result");
        JsonArray jsonRequisites = json.getAsJsonArray("requisites");

        if (jsonKeys == null || jsonRecipe == null || jsonResult == null) {
            LOGGER.error("Sussy JSON syntax!");
            return null;
        }

        ResourceLocation result = new ResourceLocation(jsonResult.getAsJsonPrimitive("entity").getAsString());


        CompoundTag nbt = null;
        if (jsonResult.has("nbt")) {
            nbt = (CompoundTag) JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, jsonResult.get("nbt"));
        }

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

        for (int i = jsonRecipe.size() - 1; i >= 0 ; i--) {
            JsonArray aisle = jsonRecipe.getAsJsonArray("layer" + i);
            if (aisle.isEmpty()) return null;

            List<String> strings = new ArrayList<>(aisle.size());
            aisle.forEach(element -> strings.add(element.getAsString()));

            try {
                builder.aisle(strings.toArray(String[]::new));
            } catch (IllegalArgumentException exception) {
                LOGGER.error(exception.getMessage());
            }
        }

        NonNullList<ItemStack> requisites;
        ImmutableList<ItemStack> toReturnRequisites = null;
        if (jsonRequisites != null) {
            requisites = NonNullList.createWithCapacity(jsonRequisites.size());
            for (JsonElement element : jsonRequisites) {
                if (element.isJsonObject()) requisites.add(ShapedRecipe.itemStackFromJson(element.getAsJsonObject()));
            }
            toReturnRequisites = ImmutableList.copyOf(requisites);
        }

        return new MultiblockRecipe(builder.build(), result, nbt, toReturnRequisites);
    }
}
