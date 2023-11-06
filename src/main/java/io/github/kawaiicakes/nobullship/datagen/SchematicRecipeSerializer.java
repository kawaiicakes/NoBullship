package io.github.kawaiicakes.nobullship.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
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
import java.util.Set;

import static io.github.kawaiicakes.nobullship.NoBullship.MOD_ID;

public class SchematicRecipeSerializer implements RecipeSerializer<SchematicRecipe> {
    protected static final Logger LOGGER = LogUtils.getLogger();
    public static final SchematicRecipeSerializer INSTANCE = new SchematicRecipeSerializer();
    public static final ResourceLocation ID = new ResourceLocation(MOD_ID, "schematic_workbench");

    // TODO: redo error checking... see vanilla examples maybe? also redo NBT checking on shaped/shapeless inputs better
    // TODO: optimize... see SharedRecipe
    @SuppressWarnings("SpellCheckingInspection")
    @Override
    public SchematicRecipe fromJson(ResourceLocation pRecipeId, JsonObject pSerializedRecipe) throws IllegalArgumentException {
        if (!Objects.equals(pSerializedRecipe.getAsString(), "nobullship:schematic_workbench"))
            throw throwNewSyntaxError(pRecipeId, "recipe type");

        JsonObject keyson = pSerializedRecipe.getAsJsonObject("key");
        if (keyson == null) throw throwNewSyntaxError(pRecipeId, "key");

        Set<Map.Entry<String, JsonElement>> keysonMembers = keyson.entrySet();
        if (keysonMembers.isEmpty()) throw throwNewSyntaxError(pRecipeId, "key");

        Map<Character, ItemStack> charToItemMap = new HashMap<>(keysonMembers.size());
        for (Map.Entry<String, JsonElement> entry : keysonMembers) {
            final char charKey = entry.getKey().charAt(0);

            if (!(entry.getValue() instanceof JsonObject entryObject)) throw throwNewSyntaxError(pRecipeId, "key has invalid item");

            charToItemMap.put(charKey, ShapedRecipe.itemStackFromJson(entryObject));
        }

        JsonObject declarason = pSerializedRecipe.getAsJsonObject("declaration");
        if (declarason == null) throw throwNewSyntaxError(pRecipeId, "declaration");

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
                ItemStack stackAtChar = charToItemMap.get(charset[j]);
                if (stackAtChar == null || stackAtChar.isEmpty()) throw throwNewSyntaxError(pRecipeId, "no such key exists");

                shapedInput.add(i + j, Ingredient.of(stackAtChar));
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
