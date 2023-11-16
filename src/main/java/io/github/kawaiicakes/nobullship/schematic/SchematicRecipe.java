package io.github.kawaiicakes.nobullship.schematic;

import com.google.common.collect.ImmutableList;
import com.google.gson.*;
import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.nobullship.multiblock.block.MultiblockWorkshopBlockEntity;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;

import static io.github.kawaiicakes.nobullship.NoBullship.MOD_ID;
import static io.github.kawaiicakes.nobullship.NoBullship.SCHEMATIC;
import static io.github.kawaiicakes.nobullship.multiblock.block.MultiblockWorkshopBlockEntity.EMPTY_SCHEM_SLOT;
import static io.github.kawaiicakes.nobullship.multiblock.block.MultiblockWorkshopBlockEntity.SHAPELESS_SLOTS;
import static net.minecraft.world.item.Items.AIR;

public class SchematicRecipe implements Recipe<MultiblockWorkshopBlockEntity> {
    private final ResourceLocation recipeId;
    private final ResourceLocation resultId;
    private final ImmutableList<Ingredient> shaped;
    private final ImmutableList<ItemStack> shapeless;
    public final byte actualShapedWidth;
    public final byte actualShapedHeight;

    public SchematicRecipe(ResourceLocation recipeId, ResourceLocation resultId, ImmutableList<Ingredient> shaped, ImmutableList<ItemStack> shapeless, byte actualShapedWidth, byte actualShapedHeight) {
        this.recipeId = recipeId;
        this.resultId = resultId;
        this.shaped = shaped;
        this.shapeless = shapeless;
        this.actualShapedWidth = actualShapedWidth;
        this.actualShapedHeight = actualShapedHeight;
    }

    public ImmutableList<Ingredient> getShapedIngredients() {
        ImmutableList.Builder<Ingredient> toReturn = ImmutableList.builder();
        // FIXME this looks like it can still be mutated... maybe reimplement this
        for (Ingredient ingredient : this.shaped) {
            toReturn.add(ingredient);
        }
        return toReturn.build();
    }

    public ImmutableList<ItemStack> getShapelessIngredients() {
        ImmutableList.Builder<ItemStack> toReturn = ImmutableList.builder();
        for (ItemStack stack : this.shapeless) {
            toReturn.add(stack.copy());
        }
        return toReturn.build();
    }

    /**
     * This method is defunct and not to be used until I add NBT compatibility to crafting.
     * Tests for "crafting equality" between standard to schematic. Equality is defined as follows:<br>
     * <ul>
     *     <li>standard and schematic are of the same <code>Item</code></li>
     *     <li>schematic contains all the NBT data standard has.</li>
     *     <ul>
     *         <li>if standard has no NBT data, schematic may have any NBT data.</li>
     *     </ul>
     *     <li>the count of schematic is greater or equal to standard.</li>
     * </ul>
     */
    public static boolean itemMatchesStandard(ItemStack standard, ItemStack item) {
        if (!standard.is(item.getItem())) return false;

        if (item.getCount() < standard.getCount()) return false;

        final CompoundTag stag = standard.getTag();
        if (stag == null) return true;

        final CompoundTag itemTag = item.getTag();
        if (itemTag == null) return false;
        if (stag.equals(itemTag)) return true;

        if (stag.getAllKeys().stream().noneMatch(itemTag::contains)) return false;

        for (String key : stag.getAllKeys()) {
            if (stag.getTagType(key) != itemTag.getTagType(key)) return false;
            //noinspection DataFlowIssue
            if (!stag.get(key).equals(itemTag.get(key))) return false;
        }

        return true;
    }

    public ResourceLocation getResultId() {
        return this.resultId;
    }

    @Override
    public boolean matches(MultiblockWorkshopBlockEntity workshop, Level pLevel) {
        if (!this.shapedMatches(workshop)) return false;
        if (!this.shapelessMatches(workshop)) return false;

        return workshop.getItem(EMPTY_SCHEM_SLOT).is(SCHEMATIC.get());
    }

    public boolean shapelessMatches(MultiblockWorkshopBlockEntity workshop) {
        if (this.shapeless.isEmpty()) return true;

        NonNullList<ItemStack> unsortedContents = NonNullList.createWithCapacity(0);
        for (int i: SHAPELESS_SLOTS) {
            if (workshop.getItem(i).is(AIR)) continue;
            unsortedContents.add(workshop.getItem(i));
        }

        List<ItemStack> summedContents = getSummedContents(unsortedContents);
        List<ItemStack> requirementContents = getSummedContents(this.getShapelessIngredients());

        return compareSummedContents(requirementContents, summedContents);
    }

    /**
     * This method is used to determine if an entity should be rendered to the workbench if a matching
     * declaration exists; regardless of whether it can actually be crafted at present.
     */
    public boolean shapedMatches(MultiblockWorkshopBlockEntity workshop) {
        for (byte i = 0; i <= 3 - this.actualShapedWidth; i++) {
            for (byte j = 0; j <= 3 - this.actualShapedHeight; j++) {
                if (this.shapedMatches(workshop, i, j, true)) {
                    return true;
                }

                if (this.shapedMatches(workshop, i, j, false)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks if a region of the shaped input matches
     */
    protected boolean shapedMatches(MultiblockWorkshopBlockEntity entity, byte pWidth, byte pHeight, boolean mirrored) {
        for(int i = 0; i < 3; i++) {
            for(int j = 0; j < 3; j++) {
                int k = i - pWidth;
                int l = j - pHeight;
                Ingredient ingredient = Ingredient.EMPTY;
                if (k >= 0 && l >= 0 && k < this.actualShapedWidth && l < this.actualShapedHeight) {
                    if (mirrored) {
                        ingredient = this.shaped.get(this.actualShapedWidth - k - 1 + l * this.actualShapedWidth);
                    } else {
                        ingredient = this.shaped.get(k + l * this.actualShapedWidth);
                    }
                }

                if (!ingredient.test(entity.getItem(i + j * 3))) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public ItemStack assemble(MultiblockWorkshopBlockEntity workshop) {
        // contents inside the Container MUST be treated as though immutable as per Forge docs
        // Why? idfk lmao. Do I want to find out? No.
        // Comments are being left here as a reminder in case the implementation changes again;
        // but now #getItem returns an ItemStack which is safe to modify.
        ItemStack copyOfInputSchematic = workshop.getItem(EMPTY_SCHEM_SLOT);

        if (copyOfInputSchematic.isEmpty()) return ItemStack.EMPTY;

        if (copyOfInputSchematic.getTag() == null) copyOfInputSchematic.setTag(new CompoundTag());
        copyOfInputSchematic.getTag().putString("nobullshipRecipe", this.resultId.toString());
        copyOfInputSchematic.setCount(1);

        return copyOfInputSchematic;
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return true;
    }

    // Since an empty schematic with arbitrary NBT data may be put in, and come out with the same data + a new tag,
    // it would be unwise to assume that the resulting schematic is a default SchematicItem ItemStack with the attached tag.
    // Mods which rely on rendering shit based on ItemStack NBT would not like this and could imply to the player that
    // the rendering would break after having a recipe written into it, as one example.
    // Addendum 1: ah fuck it whatever lol the mod doesn't even support NBT crafting at the moment; neither does vanilla.
    // I'll worry about this later since returning an empty ItemStack probably does more harm than good at this stage
    @Override
    public ItemStack getResultItem() {
        CompoundTag resultTag = new CompoundTag();
        resultTag.putString("nobullshipRecipe", this.resultId.toString());

        ItemStack toReturn = SCHEMATIC.get().getDefaultInstance().copy();
        toReturn.setTag(resultTag);

        return toReturn;
    }

    @Override
    public ResourceLocation getId() {
        return this.recipeId;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return Type.INSTANCE;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    /**
     * Don't use this
     */
    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.create();
    }

    /**
     * Returns a <code>NonNullList</code> of size 10 whose first 9 indices correspond to
     * the shapeless inputs of the workbench. The last index is the empty schematic slot.
     * @return the meaningful remaining contents of this container within the context of the caller.
     */
    @Override
    public NonNullList<ItemStack> getRemainingItems(MultiblockWorkshopBlockEntity pContainer) {
        NonNullList<ItemStack> contents = NonNullList.withSize(pContainer.getContainerSize(), ItemStack.EMPTY);

        for (int i : ArrayUtils.add(SHAPELESS_SLOTS.toIntArray(), EMPTY_SCHEM_SLOT)) {
            ItemStack item = pContainer.getItem(i);
            if (!this.shapeless.contains(item)) continue;

            if (item.hasCraftingRemainingItem()) {
                contents.set(i - 9, item.getCraftingRemainingItem());
            }
        }

        return contents;
    }

    /**
     * Utility method for returning the summed contents of the <code>ItemStack</code>s in the passed
     * <code>List</code>. Useful for shapeless crafting checks. Does not modify stacks in the passed
     * <code>List</code>.
     */
    public static List<ItemStack> getSummedContents(List<ItemStack> rawItems) {
        NonNullList<ItemStack> copiedItems = NonNullList.createWithCapacity(rawItems.size());
        for (ItemStack item : rawItems) {
            copiedItems.add(item.copy());
        }

        List<ItemStack> toReturn = new ArrayList<>(rawItems.size());

        for (ItemStack content : copiedItems) {
            if (content.isEmpty()) continue;

            int newCount = copiedItems.stream()
                    .filter(standard -> ItemStack.isSameItemSameTags(standard, content))
                    .mapToInt(ItemStack::getCount)
                    .sum();

            ItemStack newStack = content.copy();
            newStack.setCount(newCount);
            toReturn.add(newStack);

            copiedItems.replaceAll((param) -> {
                if (ItemStack.isSameItemSameTags(content, param)) return ItemStack.EMPTY;
                return param;
            });
        }

        return toReturn;
    }

    /**
     * Utility comparison method. The contents in requirements are compared against those in container. If none of the
     * contents match the given type of <code>ItemStack</code>, or if the count is less than the requirement, returns
     * false.
     * @param requirements A <code>List</code> representing the summed contents of a container whose contents correspond
     *                     to the required items and number of items being compared against.
     * @param container     A <code>List</code> representing the summed contents of a container whose contents
     *                      are to be scrutinized.
     * @return true if the container has all items in requirements equal to or greater than the associated count.
     */
    public static boolean compareSummedContents(List<ItemStack> requirements, List<ItemStack> container) {
        for (ItemStack requirement: requirements) {
            ItemStack scrutinizedStack = container
                    .stream()
                    .filter(contentStandard -> ItemStack.isSameItemSameTags(contentStandard, requirement))
                    .findAny()
                    .orElse(null);
            if (scrutinizedStack == null) return false;

            int contentCount = container.stream()
                    .filter(standard -> ItemStack.isSameItemSameTags(standard, requirement))
                    .mapToInt(ItemStack::getCount)
                    .sum();

            if (contentCount < requirement.getCount()) return false;
        }
        return true;
    }

    public static class Type implements RecipeType<SchematicRecipe> {
        private Type() {}
        public static final Type INSTANCE = new Type();
        public static final String ID = "schematic_workbench";
    }

    public static class Serializer implements RecipeSerializer<SchematicRecipe> {
        protected static final Logger LOGGER = LogUtils.getLogger();
        public static final Serializer INSTANCE = new Serializer();
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
            ImmutableList<Ingredient> shapedInput = ImmutableList.copyOf(dissolvePattern(shaped, charToIngredientMap, recipeWidth, recipeHeight));

            ImmutableList<ItemStack> shapelessInput = ImmutableList.copyOf(itemsFromJson(GsonHelper.getAsJsonArray(pSerializedRecipe, "shapeless_input")));
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

            return new SchematicRecipe(pRecipeId, new ResourceLocation(resultId), ImmutableList.copyOf(shapedList), ImmutableList.copyOf(shapelessList), width, height);
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
            int i;
            for(i = 0; i < pEntry.length() && pEntry.charAt(i) == ' '; ++i) {
            }
            return i;
        }

        // I would have done --i, but IntelliJ would give me an out-of-bounds error for #charAt...
        // Addendum 1: Oh? Does --i and i-- do the same thing in these loops?
        // Addendum 2: what the fuck am I doing...
        public static int lastNonSpace(String pEntry) {
            int i;
            for(i = pEntry.length() - 1; i >= 0 && pEntry.charAt(i) == ' '; --i) {
            }
            return i;
        }

        public static NonNullList<ItemStack> itemsFromJson(JsonArray jsonArray) {
            NonNullList<ItemStack> nonnulllist = NonNullList.createWithCapacity(jsonArray.size());

            // TODO: Shapeless can take Ingredients with a count greater than 1? One can dream...
            // Perhaps a Map containing the Ingredient and a short for the count?
            for (int i = 0; i < jsonArray.size(); ++i) {
                ItemStack ingredient = ShapedRecipe.itemStackFromJson(jsonArray.get(i).getAsJsonObject());
                nonnulllist.add(ingredient);
            }

            return nonnulllist;
        }
    }
}
