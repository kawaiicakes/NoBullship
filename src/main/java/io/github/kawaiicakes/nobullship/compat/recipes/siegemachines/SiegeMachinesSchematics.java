package io.github.kawaiicakes.nobullship.compat.recipes.siegemachines;

import io.github.kawaiicakes.nobullship.api.multiblock.MultiblockPatternBuilder;
import io.github.kawaiicakes.nobullship.api.schematic.SchematicRecipeBuilder;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.ConditionalRecipe;
import net.minecraftforge.common.crafting.conditions.ModLoadedCondition;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static io.github.kawaiicakes.nobullship.Registry.*;
import static io.github.kawaiicakes.nobullship.compat.recipes.siegemachines.SiegeMachinesMultiblocks.ezRl;
import static net.minecraft.world.item.Items.*;

public class SiegeMachinesSchematics {
    protected static final Map<ResourceLocation, SchematicRecipeBuilder> RECIPES = new HashMap<>();

    public static void generateRecipes(Consumer<FinishedRecipe> finished) {
        generateRecipes();

        for (Map.Entry<ResourceLocation, SchematicRecipeBuilder> entry : RECIPES.entrySet()) {
            ConditionalRecipe.builder()
                        .addCondition(new ModLoadedCondition("siegemachines"))
                        .addRecipe(consumer -> entry.getValue().save(consumer, entry.getKey()))
                    .build(finished, entry.getKey());
        }

        RECIPES.clear();
    }

    protected static SchematicRecipeBuilder builder(String resultRecipe) {
        return SchematicRecipeBuilder.of(ezRl(resultRecipe));
    }

    protected static void generateRecipes() {
        RECIPES.put(ezRl("battering_ram"),
                builder("battering_ram")
                        .defineShaped('#', Ingredient.of(Items.LEATHER))
                        .defineShaped('-', Ingredient.of(ItemTags.LOGS))
                        .defineShaped('>', Ingredient.of(Items.IRON_BLOCK))
                        .defineShaped('o', Ingredient.of(WOOD_WHEEL_BLOCK.get()))
                        .addShapeless(Items.FEATHER.getDefaultInstance())
                        .shapedPattern("###")
                        .shapedPattern("-->")
                        .shapedPattern("o o")
        );

        RECIPES.put(ezRl("catapult"),
                builder("catapult")
                        .defineShaped('-', Ingredient.of(ItemTags.LOGS))
                        .defineShaped('>', Ingredient.of(Items.IRON_BLOCK))
                        .defineShaped('U', Ingredient.of(Items.CAULDRON))
                        .addShapeless(Items.BOW.getDefaultInstance())
                        .shapedPattern("U> ")
                        .shapedPattern("---")
        );

        RECIPES.put(ezRl("ballista"),
                builder("ballista")
                        .defineShaped('^', Ingredient.of(CROSSBOW))
                        .defineShaped('-', Ingredient.of(WOOD_PLANK_ITEM.get()))
                        .shapedPattern(" ^ ")
                        .shapedPattern("---")
        );

        RECIPES.put(ezRl("mortar"),
                builder("mortar")
                        .defineShaped('^', Ingredient.of(CAULDRON))
                        .defineShaped('-', Ingredient.of(WOOD_PLANK_ITEM.get()))
                        .defineShaped('o', Ingredient.of(WOOD_WHEEL_ITEM.get()))
                        .shapedPattern(" ^ ")
                        .shapedPattern("-o-")
        );

        RECIPES.put(ezRl("trebuchet"),
                builder("trebuchet")
                        .defineShaped('w', Ingredient.of(WOOD_SUPPORT_BEAM_ITEM.get()))
                        .defineShaped('l', Ingredient.of(LEAD))
                        .defineShaped('-', Ingredient.of(ItemTags.LOGS))
                        .shapedPattern(" w ")
                        .shapedPattern(" w ")
                        .shapedPattern("l--")
        );
    }
}
