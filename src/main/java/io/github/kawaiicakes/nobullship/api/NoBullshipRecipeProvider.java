package io.github.kawaiicakes.nobullship.api;

import net.minecraft.core.Registry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

import static io.github.kawaiicakes.nobullship.Registry.*;
import static net.minecraft.world.item.Items.*;

public class NoBullshipRecipeProvider extends RecipeProvider {
    public NoBullshipRecipeProvider(DataGenerator pGenerator) {
        super(pGenerator);
    }

    @Override
    protected void buildCraftingRecipes(Consumer<FinishedRecipe> pFinishedRecipeConsumer) {
        materialToSimpleBeam(pFinishedRecipeConsumer, TagKey.create(Registry.ITEM_REGISTRY, ezRL("minecraft", "planks")), SIMPLE_WOOD_BEAM_BLOCK.get());
        materialToSimpleBeam(pFinishedRecipeConsumer, TagKey.create(Registry.ITEM_REGISTRY, ezRL("forge", "ingots/iron")), SIMPLE_METAL_BEAM_BLOCK.get());

        ShapedRecipeBuilder.shaped(WOOD_PLANK_ITEM.get(), 6)
                .define('s', Ingredient.of(ItemTags.create(ezRL("minecraft", "wooden_slabs"))))
                .pattern("sss")
                .group("no_bs_planks")
                .unlockedBy("has_wood_slabs", has(ItemTags.create(ezRL("minecraft", "wooden_slabs"))))
                .save(pFinishedRecipeConsumer);

        ShapedRecipeBuilder.shaped(LENS_ITEM.get(), 2)
                .define('g', Ingredient.of(ItemTags.create(ezRL("forge", "glass"))))
                .pattern(" g ")
                .pattern("  g")
                .pattern(" g ")
                .group("no_bs_lenses")
                .unlockedBy("has_glass", has(ItemTags.create(ezRL("forge", "glass"))))
                .save(pFinishedRecipeConsumer);

        ShapedRecipeBuilder.shaped(CIRCUIT_ITEM.get(), 2)
                .define('r', Ingredient.of(ItemTags.create(ezRL("forge", "dusts/redstone"))))
                .define('c', COMPARATOR)
                .define('e', REPEATER)
                .define('g', Ingredient.of(ItemTags.create(ezRL("forge", "ingots/gold"))))
                .define('l', LIGHTNING_ROD)
                .pattern("rg ")
                .pattern("rcl")
                .pattern("rel")
                .group("no_bs_circuits")
                .unlockedBy("has_redstone", has(ItemTags.create(ezRL("forge", "dusts/redstone"))))
                .save(pFinishedRecipeConsumer);

        ShapedRecipeBuilder.shaped(NAILS_ITEM.get(), 6)
                .define('h', Ingredient.of(ItemTags.create(ezRL("forge", "ingots/iron"))))
                .define('s', Ingredient.of(ItemTags.create(ezRL("forge", "nuggets/iron"))))
                .pattern("h")
                .pattern("s")
                .pattern("s")
                .group("no_bs_nails")
                .unlockedBy("has_iron", has(ItemTags.create(ezRL("forge", "ingots/iron"))))
                .save(pFinishedRecipeConsumer);
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
