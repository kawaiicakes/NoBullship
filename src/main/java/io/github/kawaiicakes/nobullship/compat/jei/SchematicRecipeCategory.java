package io.github.kawaiicakes.nobullship.compat.jei;

import com.google.common.collect.ImmutableList;
import io.github.kawaiicakes.nobullship.schematic.SchematicRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

import static io.github.kawaiicakes.nobullship.NoBullship.*;
import static io.github.kawaiicakes.nobullship.schematic.SchematicRecipe.Type.ID;
import static io.github.kawaiicakes.nobullship.schematic.SchematicRecipe.getSummedContents;
import static io.github.kawaiicakes.nobullship.multiblock.screen.MultiblockWorkshopScreen.TEXTURE;
import static mezz.jei.api.constants.VanillaTypes.ITEM_STACK;
import static mezz.jei.api.recipe.RecipeIngredientRole.INPUT;
import static mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT;

public class SchematicRecipeCategory implements IRecipeCategory<SchematicRecipe> {
    public static final ResourceLocation UID = new ResourceLocation(MOD_ID, ID);
    public static final Component TITLE = Component.translatable("block.nobullship.workshop");

    protected final IDrawable background;
    protected final IDrawable icon;

    public SchematicRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 352, 219);
        this.icon = helper.createDrawableIngredient(ITEM_STACK, new ItemStack(WORKSHOP_ITEM.get()));
    }

    @Override
    public RecipeType<SchematicRecipe> getRecipeType() {
        return NoBullshipJEI.SCHEMATIC_TYPE;
    }

    @Override
    public Component getTitle() {
        return TITLE;
    }

    @Override
    public IDrawable getBackground() {
        return this.background;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SchematicRecipe recipe, IFocusGroup focuses) {
        ImmutableList<Ingredient> shapedIngredients = recipe.getShapedIngredients();
        for(int i = 0; i < 3; ++i) {
            for(int j = 0; j < 3; ++j) {
                if (j + i * 3 >= shapedIngredients.size()) break;
                builder.addSlot(INPUT, 97 + j * 18, 30 + i * 18).addIngredients(shapedIngredients.get(j + i * 3));
            }
        }

        List<ItemStack> shapelessIngredients = getSummedContents(recipe.getShapelessIngredients());

        for (int i = 0; i < 9; ++i) {
            if (i >= shapelessIngredients.size()) break;
            builder.addSlot(INPUT, 97 + i * 18, 102).addItemStack(shapelessIngredients.get(i));
        }

        builder.addSlot(INPUT, 169, 48).addItemStack(SCHEMATIC.get().getDefaultInstance());
        builder.addSlot(OUTPUT, 169, 26).addItemStack(recipe.getResultItem());
    }
}