package io.github.kawaiicakes.nobullship.datagen;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.nobullship.data.SchematicRecipe;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static io.github.kawaiicakes.nobullship.NoBullship.MOD_ID;

public class SchematicRecipeSerializer implements RecipeSerializer<SchematicRecipe> {
    protected static final Logger LOGGER = LogUtils.getLogger();
    public static final SchematicRecipeSerializer INSTANCE = new SchematicRecipeSerializer();
    public static final ResourceLocation ID = new ResourceLocation(MOD_ID, "schematic_workbench");

    public static Map<Character, ItemStack> itemstackKeyFromJson(JsonObject keySet) {
        Map<Character, ItemStack> map = new HashMap<>(keySet.size());

        for(Map.Entry<String, JsonElement> entry : keySet.entrySet()) {
            if (entry.getKey().length() != 1) {
                throw new JsonSyntaxException("Invalid key entry: '" + entry.getKey() + "' is an invalid symbol (must be 1 character only).");
            }

            if (" ".equals(entry.getKey())) {
                throw new JsonSyntaxException("Invalid key entry: ' ' is a reserved symbol.");
            }

            map.put(entry.getKey().charAt(0), ShapedRecipe.itemStackFromJson(entry.getValue().getAsJsonObject()));
        }

        map.put(' ', ItemStack.EMPTY);
        return map;
    }

    public static Map<Character, Ingredient> ingredientKeyFromJson(JsonObject keySet) {
        Map<Character, Ingredient> map = new HashMap<>(keySet.size());

        for(Map.Entry<String, JsonElement> entry : keySet.entrySet()) {
            if (entry.getKey().length() != 1) {
                throw new JsonSyntaxException("Invalid key entry: '" + entry.getKey() + "' is an invalid symbol (must be 1 character only).");
            }

            if (" ".equals(entry.getKey())) {
                throw new JsonSyntaxException("Invalid key entry: ' ' is a reserved symbol.");
            }

            map.put(entry.getKey().charAt(0), Ingredient.fromJson(entry.getValue()));
        }

        map.put(' ', Ingredient.EMPTY);
        return map;
    }

    // TODO: redo error checking... see vanilla examples maybe? also redo NBT checking on shaped/shapeless inputs better
    // TODO: optimize... see SharedRecipe
    @SuppressWarnings("SpellCheckingInspection")
    @Override
    public SchematicRecipe fromJson(ResourceLocation pRecipeId, JsonObject pSerializedRecipe) throws IllegalArgumentException {
        if (!Objects.equals(pSerializedRecipe.getAsString(), "nobullship:schematic_workbench"))
            throw throwNewSyntaxError(pRecipeId, "recipe type");

        JsonObject keyson = pSerializedRecipe.getAsJsonObject("key");
        if (keyson == null) throw throwNewSyntaxError(pRecipeId, "shapeless input key");

        JsonObject declarason = pSerializedRecipe.getAsJsonObject("declaration");
        if (declarason == null) throw throwNewSyntaxError(pRecipeId, "declaration");

        JsonObject shapedKeyson = declarason.getAsJsonObject("key");
        if (shapedKeyson == null) throw throwNewSyntaxError(pRecipeId, "declaration key");

        Map<Character, ItemStack> charToItemMap = itemstackKeyFromJson(keyson);
        Map<Character, Ingredient> charToIngredientMap = ingredientKeyFromJson(shapedKeyson);

        if (declarason.getAsJsonPrimitive("output_id") == null) throw throwNewSyntaxError(pRecipeId, "output_id");
        ResourceLocation resultId = new ResourceLocation(declarason.getAsJsonPrimitive("output_id").getAsString());

        JsonArray shapedson = declarason.getAsJsonArray("shaped_input");
        if (shapedson == null || shapedson.size() > 3 || shapedson.isEmpty()) throw throwNewSyntaxError(pRecipeId, "shaped_input");

        JsonArray shapelesson = pSerializedRecipe.getAsJsonArray("shapeless_input");
        if (shapelesson == null || shapelesson.size() > 9 || shapelesson.isEmpty()) throw throwNewSyntaxError(pRecipeId, "shapeless_input");

        NonNullList<Ingredient> shapedInput = NonNullList.withSize(9, Ingredient.EMPTY);
        NonNullList<ItemStack> shapelessInput = NonNullList.withSize(9, ItemStack.EMPTY);

        for (byte i = 0; i < 3; i++) {
            if (!(shapedson.get(i) instanceof JsonPrimitive element)) throw throwNewSyntaxError(pRecipeId, "shaped_input entries");
            final char[] charset = element.getAsString().toCharArray();

            for (byte j = 0; j < 3; j++) {
                Ingredient ingredientAtChar = charToIngredientMap.get(charset[j]);
                if (ingredientAtChar == null || ingredientAtChar.isEmpty()) throw throwNewSyntaxError(pRecipeId, "no such key exists");

                shapedInput.add(i + j, ingredientAtChar);
            }
        }

        for (JsonElement jsonElement : shapelesson) {
            if (!(jsonElement instanceof JsonPrimitive element)) throw throwNewSyntaxError(pRecipeId, "shapeless_input entries");
            final char character = element.getAsCharacter();

            ItemStack stackAtChar = charToItemMap.get(character);
            if (stackAtChar == null || stackAtChar.isEmpty()) throw throwNewSyntaxError(pRecipeId, "no such key exists");

            shapelessInput.add(character, stackAtChar);
        }

        return new SchematicRecipe(resultId, shapedInput, shapelessInput);
    }

    @Override
    public @Nullable SchematicRecipe fromNetwork(ResourceLocation pRecipeId, FriendlyByteBuf pBuffer) {
        String resultId = pBuffer.readUtf();

        NonNullList<Ingredient> shapedList = NonNullList.withSize(9, Ingredient.EMPTY);
        shapedList.replaceAll(_ignored -> Ingredient.fromNetwork(pBuffer));

        NonNullList<ItemStack> shapelessList = NonNullList.withSize(9, ItemStack.EMPTY);
        shapelessList.replaceAll(_ignored -> pBuffer.readItem());

        return new SchematicRecipe(new ResourceLocation(resultId), shapedList, shapelessList);
    }

    @Override
    public void toNetwork(FriendlyByteBuf pBuffer, SchematicRecipe pRecipe) {
        pBuffer.writeUtf(pRecipe.getId().toString());

        pRecipe.getShapedIngredients().forEach(ingredient -> ingredient.toNetwork(pBuffer));

        pRecipe.getShapelessIngredients().forEach(pBuffer::writeItem);
    }

    protected static IllegalArgumentException throwNewSyntaxError(ResourceLocation pRecipeId, String message) {
        LOGGER.error("Recipe {} has invalid JSON syntax: {}!", pRecipeId, message);
        return new IllegalArgumentException("Recipe " + pRecipeId + " has invalid JSON syntax: " + message + "!");
    }
}
