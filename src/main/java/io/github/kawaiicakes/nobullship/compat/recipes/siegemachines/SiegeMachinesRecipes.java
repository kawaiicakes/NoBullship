package io.github.kawaiicakes.nobullship.compat.recipes.siegemachines;

import io.github.kawaiicakes.nobullship.api.BlockInWorldPredicateBuilder;
import io.github.kawaiicakes.nobullship.api.multiblock.MultiblockPatternBuilder;
import io.github.kawaiicakes.nobullship.multiblock.FinishedMultiblockRecipe;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.crafting.conditions.ModLoadedCondition;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static io.github.kawaiicakes.nobullship.Registry.SIMPLE_WOOD_BEAM_BLOCK;
import static io.github.kawaiicakes.nobullship.Registry.WOOD_WHEEL_BLOCK;
import static io.github.kawaiicakes.nobullship.block.WheelBlock.FACING;
import static net.minecraft.world.level.block.Blocks.IRON_BLOCK;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.*;

public class SiegeMachinesRecipes {
    protected static final Map<ResourceLocation, MultiblockPatternBuilder> RECIPES = new HashMap<>();

    protected static MultiblockPatternBuilder builder(String resultEntity) {
        //noinspection SpellCheckingInspection
        return MultiblockPatternBuilder
                .of(ezRl(resultEntity))
                .addCondition(new ModLoadedCondition("siegemachines"));
    }

    public static void generateRecipes(Consumer<FinishedMultiblockRecipe> consumer) {
        for (Map.Entry<ResourceLocation, MultiblockPatternBuilder> entry : RECIPES.entrySet()) {
            ResourceLocation id = entry.getKey();
            MultiblockPatternBuilder builder = entry.getValue();

            builder.save(consumer, id);
        }
    }

    protected static ResourceLocation ezRl(String string) {
        //noinspection SpellCheckingInspection
        return new ResourceLocation("siegemachines", string);
    }

    static {
        Block woodWheel = WOOD_WHEEL_BLOCK.get();
        Block simpleWoodBeam = SIMPLE_WOOD_BEAM_BLOCK.get();
        ItemStack leather = Items.LEATHER.getDefaultInstance();
        leather.setCount(2);
        CompoundTag ramTag = new CompoundTag();
        ramTag.putFloat("Health", 500.00F);

        RECIPES.put(ezRl("battering_ram"),
                builder("battering_ram")
                        .where('<', BlockInWorldPredicateBuilder
                                .of(woodWheel)
                                .requireProperty(FACING, Direction.EAST))
                        .where('>', BlockInWorldPredicateBuilder
                                .of(woodWheel)
                                .requireProperty(FACING, Direction.WEST))
                        .where('-', BlockInWorldPredicateBuilder
                                .of(simpleWoodBeam)
                                .requireProperty(EAST, true)
                                .requireProperty(WEST, true))
                        .where('a', BlockInWorldPredicateBuilder
                                .of(simpleWoodBeam)
                                .requireProperty(EAST, true)
                                .requireProperty(WEST, true)
                                .requireProperty(SOUTH, true)
                                .requireProperty(UP, true))
                        .where('r', BlockInWorldPredicateBuilder
                                .of(simpleWoodBeam)
                                .requireProperty(WEST, true)
                                .requireProperty(SOUTH, true)
                                .requireProperty(DOWN, true))
                        .where('l', BlockInWorldPredicateBuilder
                                .of(simpleWoodBeam)
                                .requireProperty(EAST, true)
                                .requireProperty(SOUTH, true)
                                .requireProperty(DOWN, true))
                        .where('|', BlockInWorldPredicateBuilder
                                .of(simpleWoodBeam)
                                .requireProperty(UP, true)
                                .requireProperty(DOWN, true))
                        .where('O', BlockInWorldPredicateBuilder
                                .of(BlockTags.LOGS)
                                .requireProperty(AXIS, Direction.Axis.Z))
                        .where('.', BlockInWorldPredicateBuilder
                                .of(simpleWoodBeam)
                                .requireProperty(NORTH, true)
                                .requireProperty(SOUTH, true))
                        .where('+', BlockInWorldPredicateBuilder
                                .of(simpleWoodBeam)
                                .requireProperty(EAST, true)
                                .requireProperty(NORTH, true)
                                .requireProperty(SOUTH, true)
                                .requireProperty(DOWN, true))
                        .where('*', BlockInWorldPredicateBuilder
                                .of(simpleWoodBeam)
                                .requireProperty(WEST, true)
                                .requireProperty(NORTH, true)
                                .requireProperty(SOUTH, true)
                                .requireProperty(DOWN, true))
                        .where('A', BlockInWorldPredicateBuilder
                                .of(simpleWoodBeam)
                                .requireProperty(EAST, true)
                                .requireProperty(WEST, true)
                                .requireProperty(SOUTH, true)
                                .requireProperty(UP, true)
                                .requireProperty(NORTH, true))
                        .where('v', BlockInWorldPredicateBuilder
                                .of(simpleWoodBeam)
                                .requireProperty(EAST, true)
                                .requireProperty(DOWN, true)
                                .requireProperty(NORTH, true))
                        .where('w', BlockInWorldPredicateBuilder
                                .of(simpleWoodBeam)
                                .requireProperty(WEST, true)
                                .requireProperty(DOWN, true)
                                .requireProperty(NORTH, true))
                        .where('b', BlockInWorldPredicateBuilder
                                .of(simpleWoodBeam)
                                .requireProperty(EAST, true)
                                .requireProperty(WEST, true)
                                .requireProperty(NORTH, true)
                                .requireProperty(UP, true))
                        .where('G', BlockInWorldPredicateBuilder.of(IRON_BLOCK))
                        .aisle(
                                " l-r ",
                                " |O| ",
                                "<a-a>"
                        )
                        .aisle(
                                " . . ",
                                "  O  ",
                                " . . "
                                )
                        .aisle(
                                " +-* ",
                                " |O| ",
                                "<A-A>"
                        )
                        .aisle(
                                " . . ",
                                "  O  ",
                                " . . "
                        )
                        .aisle(
                                " v-w ",
                                " |O| ",
                                "<b-b>"
                        )
                        .aisle(
                                "     ",
                                "  G  ",
                                "     "
                        )
                        .setTagOfResult(ramTag)
                        .addRequisite(leather)
        );
    }
}
