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

import static io.github.kawaiicakes.nobullship.block.MultiblockWorkshopBlockEntity.EMPTY_SCHEM_SLOT;

public class SchematicRecipe implements Recipe<MultiblockWorkshopBlockEntity> {
    private final ResourceLocation resultId;
    private final NonNullList<Ingredient> shaped;
    private final NonNullList<ItemStack> shapeless;

    public SchematicRecipe(ResourceLocation resultId, NonNullList<Ingredient> shaped, NonNullList<ItemStack> shapeless) {
        this.resultId = resultId;
        this.shaped = shaped;
        this.shapeless = shapeless;
    }

    public ImmutableList<Ingredient> getShapedIngredients() {
        return ImmutableList.copyOf(shaped);
    }

    public ImmutableList<ItemStack> getShapelessIngredients() {
        return ImmutableList.copyOf(shapeless);
    }

    /**
     * ONLY CALL THIS FROM THE SERVERSIDE. This method is used to determine if an entity should be rendered to the
     * workbench if a matching declaration exists; regardless of whether it can actually be crafted at present.
     */
    public boolean declarationMatches(MultiblockWorkshopBlockEntity workshop) {
        for (int i = 0; i < 9; i++) {
            if (!(this.shaped.get(i).test(workshop.getItem(i)))) return false;
        }

        return true;
    }

    @Override
    public boolean matches(MultiblockWorkshopBlockEntity workshop, Level pLevel) {
        if (pLevel.isClientSide) return false;

        if (!this.declarationMatches(workshop)) return false;

        // TODO: more sophisticated comparison
        for (int i = 9; i < 18; i++) {
            final int finalI = i;
            if (this.shapeless.stream()
                    .noneMatch(item -> item.equals(workshop.getItem(finalI), false))) return false;
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
        return this.resultId;
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
        public static final String ID = "schematic";
    }
}
