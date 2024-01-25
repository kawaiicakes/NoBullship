package io.github.kawaiicakes.nobullship.compat.recipes.siegemachines;

import io.github.kawaiicakes.nobullship.api.BlockInWorldPredicateBuilder;
import io.github.kawaiicakes.nobullship.api.multiblock.MultiblockPatternBuilder;
import io.github.kawaiicakes.nobullship.block.MetalIBeamBlock;
import io.github.kawaiicakes.nobullship.multiblock.FinishedMultiblockRecipe;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraftforge.common.crafting.conditions.ModLoadedCondition;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static io.github.kawaiicakes.nobullship.Registry.*;
import static io.github.kawaiicakes.nobullship.block.WheelBlock.FACING;
import static net.minecraft.world.level.block.Blocks.IRON_BLOCK;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.*;

public class SiegeMachinesMultiblocks {
    protected static final Map<ResourceLocation, MultiblockPatternBuilder> RECIPES = new HashMap<>();

    protected static MultiblockPatternBuilder builder(String resultEntity) {
        //noinspection SpellCheckingInspection
        return MultiblockPatternBuilder
                .of(ezRl(resultEntity))
                .addCondition(new ModLoadedCondition("siegemachines"));
    }

    public static void generateRecipes(Consumer<FinishedMultiblockRecipe> consumer) {
        generateRecipes();

        for (Map.Entry<ResourceLocation, MultiblockPatternBuilder> entry : RECIPES.entrySet()) {
            ResourceLocation id = entry.getKey();
            MultiblockPatternBuilder builder = entry.getValue();

            builder.save(consumer, id);
        }

        RECIPES.clear();
    }

    protected static ResourceLocation ezRl(String string) {
        //noinspection SpellCheckingInspection
        return new ResourceLocation("siegemachines", string);
    }

    protected static BlockInWorldPredicateBuilder woodWheel(Direction facing) {
        return BlockInWorldPredicateBuilder
                .of(WOOD_WHEEL_BLOCK.get())
                .requireProperty(FACING, facing);
    }

    protected static BlockInWorldPredicateBuilder ewWoodBeam() {
        return BlockInWorldPredicateBuilder
                .of(SIMPLE_WOOD_BEAM_BLOCK.get())
                .requireProperty(EAST, true)
                .requireProperty(WEST, true);
    }

    protected static BlockInWorldPredicateBuilder nsWoodBeam() {
        return BlockInWorldPredicateBuilder
                .of(SIMPLE_WOOD_BEAM_BLOCK.get())
                .requireProperty(NORTH, true)
                .requireProperty(SOUTH, true);
    }

    protected static BlockInWorldPredicateBuilder udWoodBeam() {
        return BlockInWorldPredicateBuilder
                .of(SIMPLE_WOOD_BEAM_BLOCK.get())
                .requireProperty(UP, true)
                .requireProperty(DOWN, true);
    }

    protected static void generateRecipes() {
        Block simpleWoodBeam = SIMPLE_WOOD_BEAM_BLOCK.get();

        ItemStack leather = Items.LEATHER.getDefaultInstance();
        ItemStack leads = Items.LEAD.getDefaultInstance();
        ItemStack planks = WOOD_PLANK_ITEM.get().getDefaultInstance();
        ItemStack nails = NAILS_ITEM.get().getDefaultInstance();

        TagKey<Block> gates = BlockTags.FENCE_GATES;

        leather.setCount(4);
        leads.setCount(6);
        planks.setCount(22);
        nails.setCount(32);

        RECIPES.put(ezRl("battering_ram"),
                builder("battering_ram")
                        .where('<', woodWheel(Direction.EAST))
                        .where('>', woodWheel(Direction.WEST))
                        .where('-', ewWoodBeam())
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
                        .where('|', udWoodBeam())
                        .where('O', BlockInWorldPredicateBuilder
                                .of(BlockTags.LOGS)
                                .requireProperty(AXIS, Direction.Axis.Z))
                        .where('.', nsWoodBeam())
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
                                "     ",
                                "  G  ",
                                "     "
                        )
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
                        .addRequisite(leather)
                        .addRequisite(leads)
                        .addRequisite(planks)
                        .addRequisite(nails)
        );

        ItemStack cauldron = Items.CAULDRON.getDefaultInstance();
        ItemStack hooks = Items.TRIPWIRE_HOOK.getDefaultInstance();
        ItemStack leather2 = Items.LEATHER.getDefaultInstance();
        ItemStack leads2 = Items.LEAD.getDefaultInstance();
        ItemStack planks2 = WOOD_PLANK_ITEM.get().getDefaultInstance();
        ItemStack nails2 = WOOD_PLANK_ITEM.get().getDefaultInstance();

        leather2.setCount(6);
        leads2.setCount(11);
        planks2.setCount(6);
        nails2.setCount(50);
        hooks.setCount(5);

        RECIPES.put(ezRl("catapult"),
                builder("catapult")
                        .where('l', BlockInWorldPredicateBuilder
                                .of(BlockTags.WOODEN_STAIRS)
                                .requireProperty(StairBlock.FACING, Direction.WEST)
                                .requireProperty(WATERLOGGED, false)
                                .requireProperty(STAIRS_SHAPE, StairsShape.STRAIGHT)
                                .requireProperty(HALF, Half.BOTTOM))
                        .where('r', BlockInWorldPredicateBuilder
                                .of(BlockTags.WOODEN_STAIRS)
                                .requireProperty(StairBlock.FACING, Direction.EAST)
                                .requireProperty(WATERLOGGED, false)
                                .requireProperty(STAIRS_SHAPE, StairsShape.STRAIGHT)
                                .requireProperty(HALF, Half.BOTTOM))
                        .where('g', BlockInWorldPredicateBuilder
                                .of(gates)
                                .requireProperty(FenceGateBlock.FACING, Direction.SOUTH)
                                .requireProperty(FenceGateBlock.FACING, Direction.NORTH)
                                .requireProperty(IN_WALL, false)
                                .requireProperty(POWERED, false)
                                .requireProperty(OPEN, false))
                        .where('/', BlockInWorldPredicateBuilder
                                .of(BlockTags.LOGS)
                                .requireProperty(AXIS, Direction.Axis.Y))
                        .where('-', BlockInWorldPredicateBuilder.of(METAL_BEAM_BLOCK.get())
                                .requireProperty(MetalIBeamBlock.HORIZONTAL_AXIS, Direction.Axis.X)
                                .requireProperty(MetalIBeamBlock.DOWN, MetalIBeamBlock.BeamConnection.NONE))
                        .where('|', BlockInWorldPredicateBuilder
                                .of(BlockTags.WOODEN_FENCES))
                        .where('p', BlockInWorldPredicateBuilder
                                .of(Blocks.LEVER)
                                .requireProperty(ATTACH_FACE, AttachFace.WALL)
                                .requireProperty(LeverBlock.FACING, Direction.WEST))
                        .where('e', BlockInWorldPredicateBuilder
                                .of(Blocks.LEVER)
                                .requireProperty(ATTACH_FACE, AttachFace.WALL)
                                .requireProperty(LeverBlock.FACING, Direction.EAST))
                        .aisle(
                                "     ",
                                "     ",
                                "pl-re")
                        .aisle(
                                "  |  ",
                                "  |  ",
                                " l|r ")
                        .aisle(
                                "     ",
                                " /g/ ",
                                " /-/ ")
                        .aisle(
                                "     ",
                                "     ",
                                " lgr ")
                        .addRequisite(cauldron)
                        .addRequisite(hooks)
                        .addRequisite(leather2)
                        .addRequisite(leads2)
                        .addRequisite(planks2)
                        .addRequisite(nails2)
        );
    }
}
