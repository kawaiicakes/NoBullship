package io.github.kawaiicakes.nobullship.datagen;

import com.google.common.collect.Sets;
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

    // TODO: redo error checking... see vanilla examples maybe? also redo NBT checking on shaped/shapeless inputs better
    // TODO: optimize... see SharedRecipe
    @SuppressWarnings("SpellCheckingInspection")
    @Override
    public SchematicRecipe fromJson(ResourceLocation pRecipeId, JsonObject pSerializedRecipe) throws IllegalArgumentException {
        JsonElement mikeTypeson = pSerializedRecipe.get("type");
        if (mikeTypeson == null || !Objects.equals(mikeTypeson.getAsString(), "nobullship:schematic_workbench"))
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
        String[] shapedPattern = shrink(patternFromJsonArray(shapedson));

        JsonArray shapelesson = pSerializedRecipe.getAsJsonArray("shapeless_input");
        if (shapelesson == null || shapelesson.size() > 9 || shapelesson.isEmpty()) throw throwNewSyntaxError(pRecipeId, "shapeless_input");

        NonNullList<Ingredient> shapedInput = dissolvePattern(shapedPattern, charToIngredientMap);
        NonNullList<ItemStack> shapelessInput = NonNullList.withSize(9, ItemStack.EMPTY);

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

    protected static JsonSyntaxException throwNewSyntaxError(ResourceLocation pRecipeId, String message) {
        LOGGER.error("Recipe {} has invalid JSON syntax: {}!", pRecipeId, message);
        return new JsonSyntaxException("Recipe " + pRecipeId + " has invalid JSON syntax: " + message + "!");
    }

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

    public static NonNullList<Ingredient> dissolvePattern(String[] pPattern, Map<Character, Ingredient> pKeys) {
        NonNullList<Ingredient> nonnulllist = NonNullList.withSize(9, Ingredient.EMPTY);
        Set<Character> set = Sets.newHashSet(pKeys.keySet());
        set.remove(' ');

        for(int i = 0; i < pPattern.length; i++) {
            for(int j = 0; j < pPattern[i].length(); j++) {
                Character character = pPattern[i].charAt(j);
                Ingredient ingredient = pKeys.get(character);
                if (ingredient == null) {
                    throw new JsonSyntaxException("Pattern references symbol '" + character + "' but it's not defined in the key");
                }

                set.remove(character);
                nonnulllist.set(j + 3 * i, ingredient);
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
            throw new JsonSyntaxException("Invalid pattern: too many rows, " + 3 + " is maximum");
        } else if (astring.length == 0) {
            throw new JsonSyntaxException("Invalid pattern: empty pattern not allowed");
        } else {
            for(int i = 0; i < astring.length; ++i) {
                String s = GsonHelper.convertToString(pPatternArray.get(i), "pattern[" + i + "]");
                if (s.length() > 3) {
                    throw new JsonSyntaxException("Invalid pattern: too many columns, " + 3 + " is maximum");
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

            LOGGER.info(Arrays.toString(astring));
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
    public static int lastNonSpace(String pEntry) {
        for (int i = pEntry.length(); true; i--) {
            if (pEntry.charAt(i - 1) == ' ') return i - 1;
        }
    }
}
