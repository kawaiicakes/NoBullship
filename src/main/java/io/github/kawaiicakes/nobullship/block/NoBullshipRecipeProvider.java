package io.github.kawaiicakes.nobullship.block;

import net.minecraft.core.Registry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

import static io.github.kawaiicakes.nobullship.Registry.*;

public class NoBullshipRecipeProvider extends RecipeProvider {
    public NoBullshipRecipeProvider(DataGenerator pGenerator) {
        super(pGenerator);
    }

    @Override
    protected void buildCraftingRecipes(Consumer<FinishedRecipe> pFinishedRecipeConsumer) {
        materialToSimpleBeam(pFinishedRecipeConsumer, TagKey.create(Registry.ITEM_REGISTRY, ezRL("minecraft", "planks")), SIMPLE_WOOD_BEAM_BLOCK.get());
        materialToSimpleBeam(pFinishedRecipeConsumer, TagKey.create(Registry.ITEM_REGISTRY, ezRL("forge", "ingots/iron")), SIMPLE_METAL_BEAM_BLOCK.get());
    }

    public static void materialToSimpleBeam(Consumer<FinishedRecipe> pFinishedRecipeConsumer, TagKey<Item> material, Block beam) {
        ShapedRecipeBuilder.shaped(beam, 6)
                .define('m', material)
                .define('p', Ingredient.of(PITCH_ITEM.get()))
                .pattern("ppp")
                .pattern("mmm")
                .pattern("ppp")
                .group("no_bs_simple_beams")
                .unlockedBy("has_beam_" + material.location().getPath(), has(material))
                .save(pFinishedRecipeConsumer);
    }

    /**
     * ezRL = ez ResourceLocation lol
     */
    public static ResourceLocation ezRL(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }
}
