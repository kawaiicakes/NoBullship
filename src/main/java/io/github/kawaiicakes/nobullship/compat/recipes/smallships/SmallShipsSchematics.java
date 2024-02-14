package io.github.kawaiicakes.nobullship.compat.recipes.smallships;

import io.github.kawaiicakes.nobullship.api.schematic.SchematicRecipeBuilder;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.ConditionalRecipe;
import net.minecraftforge.common.crafting.conditions.ModLoadedCondition;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static io.github.kawaiicakes.nobullship.Registry.PITCH_ITEM;

public class SmallShipsSchematics {
    protected static final Map<ResourceLocation, SchematicRecipeBuilder> RECIPES = new HashMap<>();

    protected static ResourceLocation ezRl(String string) {
        //noinspection SpellCheckingInspection
        return new ResourceLocation("smallships", string);
    }
    public static void generateRecipes(Consumer<FinishedRecipe> finished) {
        generateRecipes();

        for (Map.Entry<ResourceLocation, SchematicRecipeBuilder> entry : RECIPES.entrySet()) {
            ConditionalRecipe.builder()
                    .addCondition(new ModLoadedCondition("smallships"))
                    .addRecipe(consumer -> entry.getValue().save(consumer, entry.getKey()))
                    .build(finished, entry.getKey());
        }

        RECIPES.clear();
    }

    protected static SchematicRecipeBuilder builder(String resultRecipe) {
        return SchematicRecipeBuilder.of(ezRl(resultRecipe));
    }

    protected static void generateRecipes() {
        String[] woodTypes = {
                "oak",
                "dark_oak",
                "birch",
                "acacia",
                "spruce",
                "jungle",
                "mangrove"
        };

        ItemStack cogString = Items.STRING.getDefaultInstance();
        cogString.setCount(4);

        ItemStack briggString = Items.STRING.getDefaultInstance();
        briggString.setCount(8);

        ItemStack galleyString = Items.STRING.getDefaultInstance();
        galleyString.setCount(2);

        ItemStack paddles = Items.WOODEN_SHOVEL.getDefaultInstance();
        paddles.setCount(8);

        ItemStack waterproofing = PITCH_ITEM.get().getDefaultInstance();
        waterproofing.setCount(3);

        Ingredient leadIngredient = Ingredient.of(Items.LEAD);
        Ingredient stringIngredient = Ingredient.of(Items.STRING);
        Ingredient chestIngredient = Ingredient.of(Items.CHEST);

        for (String woodType : woodTypes) {
            Ingredient boatIngredient = Ingredient.of(ForgeRegistries.ITEMS.getValue(new ResourceLocation(woodType + "_boat")));
            Ingredient chestBoatIngredient = Ingredient.of(ForgeRegistries.ITEMS.getValue(new ResourceLocation(woodType + "_chest_boat")));

            RECIPES.put(ezRl(woodType + "_cog_schematic"),
                    builder(woodType + "_cog")
                            .defineShaped('u', boatIngredient)
                            .defineShaped('l', leadIngredient)
                            .defineShaped('O', ezRl("sail"), false)
                            .shapedPattern("lOl")
                            .shapedPattern("uuu")
                            .addRequisite(ezRl("sail"))
                            .addRequisite(cogString)
                            .addRequisite(waterproofing)
            );

            RECIPES.put(ezRl(woodType + "_brigg_schematic"),
                    builder(woodType + "_brigg")
                            .defineShaped('u', chestBoatIngredient)
                            .defineShaped('l', leadIngredient)
                            .defineShaped('O', ezRl("sail"), false)
                            .shapedPattern("OlO")
                            .shapedPattern("uuu")
                            .addRequisite(ezRl("sail"), 2, null)
                            .addRequisite(briggString)
                            .addRequisite(waterproofing)
            );

            RECIPES.put(ezRl(woodType + "_drakkar_schematic"),
                    builder(woodType + "_drakkar")
                            .defineShaped('u', boatIngredient)
                            .defineShaped('l', leadIngredient)
                            .defineShaped('O', ezRl("sail"), false)
                            .defineShaped('s', stringIngredient)
                            .defineShaped('c', chestIngredient)
                            .shapedPattern("sOs")
                            .shapedPattern("clc")
                            .shapedPattern("uuu")
                            .addRequisite(ezRl("sail"))
                            .addRequisite(cogString)
                            .addRequisite(paddles)
                            .addRequisite(waterproofing)
            );

            RECIPES.put(ezRl(woodType + "_galley_schematic"),
                    builder(woodType + "_galley")
                            .defineShaped('u', boatIngredient)
                            .defineShaped('l', leadIngredient)
                            .defineShaped('O', ezRl("sail"), false)
                            .defineShaped('c', chestIngredient)
                            .shapedPattern("lll")
                            .shapedPattern("cOc")
                            .shapedPattern("uuu")
                            .addRequisite(ezRl("sail"))
                            .addRequisite(galleyString)
                            .addRequisite(paddles)
                            .addRequisite(waterproofing)
            );
        }
    }
}
