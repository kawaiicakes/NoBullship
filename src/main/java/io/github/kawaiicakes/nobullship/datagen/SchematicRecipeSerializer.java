package io.github.kawaiicakes.nobullship.datagen;

import com.google.gson.JsonObject;
import io.github.kawaiicakes.nobullship.data.SchematicRecipe;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.Nullable;

public class SchematicRecipeSerializer implements RecipeSerializer<SchematicRecipe> {
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
