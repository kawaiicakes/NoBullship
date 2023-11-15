package io.github.kawaiicakes.nobullship.compat.jei;

import io.github.kawaiicakes.nobullship.schematic.SchematicRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.List;
import java.util.Objects;

import static io.github.kawaiicakes.nobullship.NoBullship.MOD_ID;

@JeiPlugin
public class NoBullshipJEI implements IModPlugin {
    public static RecipeType<SchematicRecipe> SCHEMATIC_TYPE
            = new RecipeType<>(SchematicRecipeCategory.UID, SchematicRecipe.class);
    // TODO: multiblock JEI compat

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(MOD_ID, "jei_compat");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new SchematicRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        RecipeManager recipeManager = Objects.requireNonNull(Minecraft.getInstance().level).getRecipeManager();

        List<SchematicRecipe> schematicRecipes = recipeManager.getAllRecipesFor(SchematicRecipe.Type.INSTANCE);
        registration.addRecipes(SCHEMATIC_TYPE, schematicRecipes);
    }
}
