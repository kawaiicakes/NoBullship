package io.github.kawaiicakes.nobullship.data;

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
    private final ResourceLocation id;
    private final NonNullList<Ingredient> declaration;
    private final NonNullList<ItemStack> ingredients;

    public SchematicRecipe(ResourceLocation id, NonNullList<Ingredient> declaration, NonNullList<ItemStack> ingredients) {
        this.id = id;
        this.declaration = declaration;
        this.ingredients = ingredients;
    }

    /**
     * ONLY CALL THIS FROM THE SERVERSIDE. This method is used to determine if an entity should be rendered to the
     * workbench if a matching declaration exists; regardless of whether it can actually be crafted at present.
     */
    public boolean declarationMatches(MultiblockWorkshopBlockEntity workshop) {
        for (int i = 0; i < 9; i++) {
            if (!(this.declaration.get(i).test(workshop.getItem(i)))) return false;
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
            if (this.ingredients.stream()
                    .noneMatch(item -> item.equals(workshop.getItem(finalI), false))) return false;
        }

        return true;
    }

    @Override
    public ItemStack assemble(MultiblockWorkshopBlockEntity workshop) {
        // contents inside the Container MUST be treated as though immutable as per Forge docs
        // Why? idfk lmao. Do I want to find out? No.
        ItemStack copyOfInputSchematic = workshop.getItem(EMPTY_SCHEM_SLOT).copy();

        if (copyOfInputSchematic.isEmpty()) return ItemStack.EMPTY;

        if (copyOfInputSchematic.getTag() == null) copyOfInputSchematic.setTag(new CompoundTag());
        copyOfInputSchematic.getTag().putString("nobullshipRecipe", this.id.toString());

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
        return this.id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SchematicRecipeSerializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return Type.INSTANCE;
    }

    public static class Type implements RecipeType<SchematicRecipe> {
        private Type() {}
        public static final Type INSTANCE = new Type();
        public static final String ID = "schematic";
    }
}
