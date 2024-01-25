package io.github.kawaiicakes.nobullship.api;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.Tags;

import java.util.function.Consumer;

import static io.github.kawaiicakes.nobullship.Registry.*;
import static net.minecraft.world.item.Items.*;

public class NoBullshipRecipeProvider extends RecipeProvider {
    public NoBullshipRecipeProvider(DataGenerator pGenerator) {
        super(pGenerator);
    }

    @Override
    protected void buildCraftingRecipes(Consumer<FinishedRecipe> pFinishedRecipeConsumer) {
        materialToSimpleBeam(pFinishedRecipeConsumer, ItemTags.PLANKS, SIMPLE_WOOD_BEAM_BLOCK.get());
        materialToSimpleBeam(pFinishedRecipeConsumer, Tags.Items.INGOTS_IRON, SIMPLE_METAL_BEAM_BLOCK.get());

        ShapedRecipeBuilder.shaped(WOOD_PLANK_ITEM.get(), 16)
                .define('s', Ingredient.of(ItemTags.WOODEN_SLABS))
                .define('+', Ingredient.of(PITCH_ITEM.get()))
                .pattern("sss")
                .pattern("+++")
                .pattern("sss")
                .group("no_bs_planks")
                .unlockedBy("has_wood_slabs", has(ItemTags.WOODEN_SLABS))
                .save(pFinishedRecipeConsumer);

        ShapedRecipeBuilder.shaped(LENS_ITEM.get(), 2)
                .define('g', Ingredient.of(Tags.Items.GLASS))
                .pattern(" g ")
                .pattern("  g")
                .pattern(" g ")
                .group("no_bs_lenses")
                .unlockedBy("has_glass", has(Tags.Items.GLASS))
                .save(pFinishedRecipeConsumer);

        ShapedRecipeBuilder.shaped(CIRCUIT_ITEM.get(), 2)
                .define('r', Ingredient.of(Tags.Items.DUSTS_REDSTONE))
                .define('c', COMPARATOR)
                .define('e', REPEATER)
                .define('g', Ingredient.of(Tags.Items.INGOTS_GOLD))
                .define('l', LIGHTNING_ROD)
                .pattern("rg ")
                .pattern("rcl")
                .pattern("rel")
                .group("no_bs_circuits")
                .unlockedBy("has_redstone", has(Tags.Items.DUSTS_REDSTONE))
                .save(pFinishedRecipeConsumer);

        ShapedRecipeBuilder.shaped(NAILS_ITEM.get(), 6)
                .define('h', Ingredient.of(Tags.Items.INGOTS_IRON))
                .define('s', Ingredient.of(Tags.Items.NUGGETS_IRON))
                .pattern("h")
                .pattern("s")
                .pattern("s")
                .group("no_bs_nails")
                .unlockedBy("has_iron", has(Tags.Items.INGOTS_IRON))
                .save(pFinishedRecipeConsumer);
    }

    public static void materialToSimpleBeam(Consumer<FinishedRecipe> pFinishedRecipeConsumer, TagKey<Item> material, Block beam) {
        ShapedRecipeBuilder.shaped(beam, 6)
                .define('m', material)
                .define('p', Ingredient.of(PITCH_ITEM.get()))
                .pattern("pmp")
                .pattern("mmm")
                .pattern("pmp")
                .group("no_bs_simple_beams")
                .unlockedBy("has_beam_" + material.location().getPath(), has(material))
                .save(pFinishedRecipeConsumer);
    }
}
