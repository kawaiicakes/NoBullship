package io.github.kawaiicakes.nobullship.datagen;

import com.google.gson.JsonObject;
import io.github.kawaiicakes.nobullship.data.SchematicRecipe;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.Nullable;

import static io.github.kawaiicakes.nobullship.NoBullship.MOD_ID;

public class SchematicRecipeSerializer implements RecipeSerializer<SchematicRecipe> {
    public static final SchematicRecipeSerializer INSTANCE = new SchematicRecipeSerializer();
    public static final ResourceLocation ID = new ResourceLocation(MOD_ID, "schematic_workbench");

    @Override
    public SchematicRecipe fromJson(ResourceLocation pRecipeId, JsonObject pSerializedRecipe) {
        return null;
    }

    @Override
    public @Nullable SchematicRecipe fromNetwork(ResourceLocation pRecipeId, FriendlyByteBuf pBuffer) {
        return null;
    }

    @Override
    public void toNetwork(FriendlyByteBuf pBuffer, SchematicRecipe pRecipe) {

    }
}
