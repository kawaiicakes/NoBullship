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

import java.util.Arrays;

import static io.github.kawaiicakes.nobullship.block.MultiblockWorkshopBlockEntity.EMPTY_SCHEM_SLOT;

public class SchematicRecipe implements Recipe<MultiblockWorkshopBlockEntity> {
    private final ResourceLocation recipeId;
    private final ResourceLocation resultId;
    private final NonNullList<Ingredient> shaped;
    private final NonNullList<ItemStack> shapeless;
    public final byte actualShapedWidth;
    public final byte actualShapedHeight;

    public SchematicRecipe(ResourceLocation recipeId, ResourceLocation resultId, NonNullList<Ingredient> shaped, NonNullList<ItemStack> shapeless, byte actualShapedWidth, byte actualShapedHeight) {
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

    /**
     * ONLY CALL THIS FROM THE SERVERSIDE. This method is used to determine if an entity should be rendered to the
     * workbench if a matching declaration exists; regardless of whether it can actually be crafted at present.
     */
    public boolean declarationMatches(MultiblockWorkshopBlockEntity workshop) {
        for(byte i = 0; i <= 3 - this.actualShapedWidth; i++) {
            for(byte j = 0; j <= 3 - this.actualShapedHeight; j++) {
                if (this.declarationMatches(workshop, i, j, true)) {
                    return true;
                }

                if (this.declarationMatches(workshop, i, j, false)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean matches(MultiblockWorkshopBlockEntity workshop, Level pLevel) {
        if (pLevel.isClientSide) return false;

        if (!this.declarationMatches(workshop)) return false;

        if (!this.shapeless.isEmpty()) {
            for (int i = 9; i < 18; i++) {
                final int finalI = i;
                if (this.shapeless.stream()
                        .noneMatch(standard -> itemMatchesStandard(standard, workshop.getItem(finalI)))) return false;
            }
        }

        return true;
    }

    /**
     * Checks if a region of the shaped input matches
     */
    protected boolean declarationMatches(MultiblockWorkshopBlockEntity entity, byte pWidth, byte pHeight, boolean mirrored) {
        for(int i = 0; i < 3; ++i) {
            for(int j = 0; j < 3; ++j) {
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

                int finalJ = j;
                int finalI = i;
                if (Arrays
                        .stream(ingredient.getItems())
                        .noneMatch(standard -> itemMatchesStandard(standard, entity.getItem(finalI + finalJ * 3)))
                ) return false;
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
    @Override
    public ItemStack getResultItem() {
        return ItemStack.EMPTY;
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

    public static class Type implements RecipeType<SchematicRecipe> {
        private Type() {}
        public static final Type INSTANCE = new Type();
        public static final String ID = "schematic_workbench";
    }
}
