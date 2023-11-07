package io.github.kawaiicakes.nobullship.datagen;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.nobullship.data.SchematicRecipe;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;

import static io.github.kawaiicakes.nobullship.NoBullship.MOD_ID;

public class SchematicRecipeSerializer implements RecipeSerializer<SchematicRecipe> {
    protected static final Logger LOGGER = LogUtils.getLogger();
    public static final SchematicRecipeSerializer INSTANCE = new SchematicRecipeSerializer();
    public static final ResourceLocation ID = new ResourceLocation(MOD_ID, "schematic_workbench");

    @SuppressWarnings("SpellCheckingInspection")
    @Override
    public SchematicRecipe fromJson(ResourceLocation pRecipeId, JsonObject pSerializedRecipe) throws IllegalArgumentException {
        JsonElement mikeTypeson = pSerializedRecipe.get("type");
        if (mikeTypeson == null || !Objects.equals(mikeTypeson.getAsString(), ID.toString()))
            throw throwNewSyntaxError(pRecipeId, "recipe type");

        if (pSerializedRecipe.getAsJsonObject("declaration") == null)
            throw throwNewSyntaxError(pRecipeId, "declaration");
        JsonObject declarason = pSerializedRecipe.getAsJsonObject("declaration");

        if (declarason.getAsJsonPrimitive("output_id") == null) throw throwNewSyntaxError(pRecipeId, "output_id");
        ResourceLocation resultId = new ResourceLocation(declarason.getAsJsonPrimitive("output_id").getAsString());

        Map<String, Ingredient> charToIngredientMap = ingredientKeyFromJson(GsonHelper.getAsJsonObject(declarason, "key"));
        String[] shaped = shrink(patternFromJsonArray(GsonHelper.getAsJsonArray(declarason, "shaped_input")));
        byte recipeWidth = (byte) shaped[0].length();
        byte recipeHeight = (byte) shaped.length;
        NonNullList<Ingredient> shapedInput = dissolvePattern(shaped, charToIngredientMap, recipeWidth, recipeHeight);

        NonNullList<ItemStack> shapelessInput = itemsFromJson(GsonHelper.getAsJsonArray(pSerializedRecipe, "shapeless_input"));
        if (shapelessInput.size() > 9) throw new JsonParseException("Too many ingredients for shapeless recipe. The maximum is 9");

        return new SchematicRecipe(pRecipeId, resultId, shapedInput, shapelessInput, recipeWidth, recipeHeight);
    }

    @Override
    public @Nullable SchematicRecipe fromNetwork(ResourceLocation pRecipeId, FriendlyByteBuf pBuffer) {
        byte width = pBuffer.readByte();
        byte height = pBuffer.readByte();

        String resultId = pBuffer.readUtf();

        NonNullList<Ingredient> shapedList = NonNullList.withSize(width * height, Ingredient.EMPTY);
        shapedList.replaceAll(_ignored -> Ingredient.fromNetwork(pBuffer));

        NonNullList<ItemStack> shapelessList = NonNullList.withSize(9, ItemStack.EMPTY);
        shapelessList.replaceAll(_ignored -> pBuffer.readItem());

        return new SchematicRecipe(pRecipeId, new ResourceLocation(resultId), shapedList, shapelessList, width, height);
    }

    @Override
    public void toNetwork(FriendlyByteBuf pBuffer, SchematicRecipe pRecipe) {
        pBuffer.writeByte(pRecipe.actualShapedWidth);
        pBuffer.writeByte(pRecipe.actualShapedHeight);

        pBuffer.writeUtf(pRecipe.getId().toString());

        pRecipe.getShapedIngredients().forEach(ingredient -> ingredient.toNetwork(pBuffer));

        pRecipe.getShapelessIngredients().forEach(pBuffer::writeItem);
    }

    protected static JsonSyntaxException throwNewSyntaxError(ResourceLocation pRecipeId, String message) {
        LOGGER.error("Recipe {} has invalid JSON syntax: {}!", pRecipeId, message);
        return new JsonSyntaxException("Recipe " + pRecipeId + " has invalid JSON syntax: " + message + "!");
    }

    public static Map<String, Ingredient> ingredientKeyFromJson(JsonObject keySet) {
        Map<String, Ingredient> map = new HashMap<>(keySet.size());

        for(Map.Entry<String, JsonElement> entry : keySet.entrySet()) {
            if (entry.getKey().length() != 1) {
                throw new JsonSyntaxException("Invalid key entry: '" + entry.getKey() + "' is an invalid symbol (must be 1 character only).");
            }

            if (" ".equals(entry.getKey())) {
                throw new JsonSyntaxException("Invalid key entry: ' ' is a reserved symbol.");
            }

            map.put(entry.getKey(), Ingredient.fromJson(entry.getValue()));
        }

        map.put(" ", Ingredient.EMPTY);
        return map;
    }

    public static NonNullList<Ingredient> dissolvePattern(String[] pPattern, Map<String, Ingredient> pKeys, byte actualWidth, byte actualHeight) {
        NonNullList<Ingredient> nonnulllist = NonNullList.withSize(actualWidth * actualHeight, Ingredient.EMPTY);
        Set<String> set = new HashSet<>(pKeys.keySet());
        set.remove(" ");

        for(int i = 0; i < pPattern.length; i++) {
            for(int j = 0; j < pPattern[i].length(); j++) {
                String character = pPattern[i].substring(j, j + 1);
                Ingredient ingredient = pKeys.get(character);
                if (ingredient == null) {
                    throw new JsonSyntaxException("Pattern references symbol '" + character + "' but it's not defined in the key");
                }

                set.remove(character);
                nonnulllist.set(j + actualWidth * i, ingredient);
            }
        }

        if (!set.isEmpty()) {
            throw new JsonSyntaxException("Key defines symbols that aren't used in pattern: " + set);
        } else {
            return nonnulllist;
        }
    }

    public static String[] patternFromJsonArray(JsonArray pPatternArray) {
        String[] astring = new String[pPatternArray.size()];
        if (astring.length > 3) {
            throw new JsonSyntaxException("Invalid pattern: too many rows, 3 is maximum");
        } else if (astring.length == 0) {
            throw new JsonSyntaxException("Invalid pattern: empty pattern not allowed");
        } else {
            for(int i = 0; i < astring.length; ++i) {
                String s = GsonHelper.convertToString(pPatternArray.get(i), "pattern[" + i + "]");
                if (s.length() > 3) {
                    throw new JsonSyntaxException("Invalid pattern: too many columns, 3 is maximum");
                }

                if (i > 0 && astring[0].length() != s.length()) {
                    throw new JsonSyntaxException("Invalid pattern: each row must be the same width");
                }

                astring[i] = s;
            }

            return astring;
        }
    }

    public static String[] shrink(String... pToShrink) {
        int i = Integer.MAX_VALUE;
        int j = 0;
        int k = 0;
        int l = 0;

        for(int i1 = 0; i1 < pToShrink.length; ++i1) {
            String s = pToShrink[i1];
            i = Math.min(i, firstNonSpace(s));
            int j1 = lastNonSpace(s);
            j = Math.max(j, j1);
            if (j1 < 0) {
                if (k == i1) {
                    ++k;
                }

                ++l;
            } else {
                l = 0;
            }
        }

        if (pToShrink.length == l) {
            return new String[0];
        } else {
            String[] astring = new String[pToShrink.length - l - k];

            for(int k1 = 0; k1 < astring.length; ++k1) {
                astring[k1] = pToShrink[k1 + k].substring(i, j + 1);
            }

            return astring;
        }
    }

    public static int firstNonSpace(String pEntry) {
        for (int i = 0; i < pEntry.length(); i++) {
            if (pEntry.charAt(i) == ' ') return i;
        }
        return 0;
    }

    // I would have done --i, but IntelliJ would give me an out-of-bounds error for #charAt...
    // Addendum 1: Oh? Does --i and i-- do the same thing in these loops?
    public static int lastNonSpace(String pEntry) {
        for (int i = pEntry.length() - 1; i >= 0; i--) {
            if (pEntry.charAt(i) == ' ') return i;
        }
        return 0;
    }

    public static NonNullList<ItemStack> itemsFromJson(JsonArray jsonArray) {
        NonNullList<ItemStack> nonnulllist = NonNullList.create();

        // TODO: Shapeless can take Ingredients with a count greater than 1? One can dream...
        // Perhaps a Map containing the Ingredient and a short for the count?
        for (int i = 0; i < jsonArray.size(); ++i) {
            ItemStack ingredient = ShapedRecipe.itemStackFromJson(jsonArray.get(i).getAsJsonObject());
            nonnulllist.add(ingredient);
        }

        return nonnulllist;
    }
}
