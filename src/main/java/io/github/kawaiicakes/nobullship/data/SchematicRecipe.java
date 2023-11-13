package io.github.kawaiicakes.nobullship.data;

import com.google.common.collect.ImmutableList;
import io.github.kawaiicakes.nobullship.block.MultiblockWorkshopBlockEntity;
import io.github.kawaiicakes.nobullship.datagen.SchematicRecipeSerializer;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

import static io.github.kawaiicakes.nobullship.NoBullship.SCHEMATIC;
import static io.github.kawaiicakes.nobullship.block.MultiblockWorkshopBlockEntity.EMPTY_SCHEM_SLOT;
import static io.github.kawaiicakes.nobullship.block.MultiblockWorkshopBlockEntity.SHAPELESS_SLOTS;
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
        return ImmutableList.copyOf(shaped);
    }

    public ImmutableList<ItemStack> getShapelessIngredients() {
        return ImmutableList.copyOf(shapeless);
    }

    /**
     * This method is defunct and not to be used until I add NBT compatibility to crafting.
     * Tests for "crafting equality" between standard to item. Equality is defined as follows:<br>
     * <ul>
     *     <li>standard and item are of the same <code>Item</code></li>
     *     <li>item contains all the NBT data standard has.</li>
     *     <ul>
     *         <li>if standard has no NBT data, item may have any NBT data.</li>
     *     </ul>
     *     <li>the count of item is greater or equal to standard.</li>
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
     * ONLY CALL THIS FROM THE SERVERSIDE. This method is used to determine if an entity should be rendered to the
     * workbench if a matching declaration exists; regardless of whether it can actually be crafted at present.
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
    // it would be unwise to assume that the resulting item is a default SchematicItem ItemStack with the attached tag.
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
        return SchematicRecipeSerializer.INSTANCE;
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
}
