package io.github.kawaiicakes.nobullship.data;

import io.github.kawaiicakes.nobullship.block.MultiblockWorkshopBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import static io.github.kawaiicakes.nobullship.NoBullship.SCHEMATIC_RECIPE;
import static io.github.kawaiicakes.nobullship.NoBullship.SCHEMATIC_SERIALIZER;
import static io.github.kawaiicakes.nobullship.block.MultiblockWorkshopBlockEntity.EMPTY_SCHEM_SLOT;

public class SchematicRecipe implements Recipe<MultiblockWorkshopBlockEntity> {
    @Override
    public boolean matches(MultiblockWorkshopBlockEntity pContainer, Level pLevel) {
        return false;
    }

    @Override
    public ItemStack assemble(MultiblockWorkshopBlockEntity pContainer) {
        // contents inside the Container MUST be treated as though immutable as per Forge docs
        // Why? idfk lmao. Do I want to find out? No.
        ItemStack copyOfInputSchematic = pContainer.getItem(EMPTY_SCHEM_SLOT).copy();

        if (copyOfInputSchematic.getTag() == null) copyOfInputSchematic.setTag(new CompoundTag());
        copyOfInputSchematic.getTag().putString("nobullshipRecipe", "ballsmungus");

        return copyOfInputSchematic;
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return false;
    }

    @Override
    public ItemStack getResultItem() {
        return null;
    }

    @Override
    public ResourceLocation getId() {
        return null;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SCHEMATIC_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return SCHEMATIC_RECIPE.get();
    }

    public static class Type implements RecipeType<SchematicRecipe> {
    }
}
