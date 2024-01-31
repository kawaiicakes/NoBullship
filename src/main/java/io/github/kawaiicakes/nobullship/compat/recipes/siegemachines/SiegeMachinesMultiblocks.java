package io.github.kawaiicakes.nobullship.compat.recipes.siegemachines;

import io.github.kawaiicakes.nobullship.api.BlockInWorldPredicateBuilder;
import io.github.kawaiicakes.nobullship.api.multiblock.MultiblockPatternBuilder;
import io.github.kawaiicakes.nobullship.block.FullLengthBeamBlock;
import io.github.kawaiicakes.nobullship.multiblock.FinishedMultiblockRecipe;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraftforge.common.crafting.conditions.ModLoadedCondition;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static io.github.kawaiicakes.nobullship.Registry.*;
import static io.github.kawaiicakes.nobullship.block.FullLengthBeamBlock.*;
import static io.github.kawaiicakes.nobullship.block.FullLengthBeamBlock.DOWN;
import static io.github.kawaiicakes.nobullship.block.FullLengthBeamBlock.UP;
import static io.github.kawaiicakes.nobullship.block.WheelBlock.FACING;
import static net.minecraft.world.item.Items.*;
import static net.minecraft.world.level.block.Blocks.IRON_BLOCK;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.*;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;

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
                .requireProperty(BlockStateProperties.UP, true)
                .requireProperty(BlockStateProperties.DOWN, true);
    }

    protected static BlockInWorldPredicateBuilder zAxisWoodBeam() {
        return BlockInWorldPredicateBuilder
                .of(WOOD_SUPPORT_BEAM_BLOCK.get())
                .requireProperty(FullLengthBeamBlock.HORIZONTAL_AXIS, Direction.Axis.Z);
    }

    protected static BlockInWorldPredicateBuilder xAxisWoodBeam() {
        return BlockInWorldPredicateBuilder
                .of(WOOD_SUPPORT_BEAM_BLOCK.get())
                .requireProperty(FullLengthBeamBlock.HORIZONTAL_AXIS, Direction.Axis.X);
    }

    protected static BlockInWorldPredicateBuilder xAxisMetalBeam() {
        return BlockInWorldPredicateBuilder
                .of(METAL_BEAM_BLOCK.get())
                .requireProperty(FullLengthBeamBlock.HORIZONTAL_AXIS, Direction.Axis.X);
    }

    protected static void generateRecipes() {
        Block simpleWoodBeam = SIMPLE_WOOD_BEAM_BLOCK.get();

        ItemStack leather = Items.LEATHER.getDefaultInstance();
        ItemStack leads = Items.LEAD.getDefaultInstance();
        ItemStack planks = WOOD_PLANK_ITEM.get().getDefaultInstance();
        ItemStack nails = NAILS_ITEM.get().getDefaultInstance();

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
                                .requireProperty(BlockStateProperties.UP, true))
                        .where('r', BlockInWorldPredicateBuilder
                                .of(simpleWoodBeam)
                                .requireProperty(WEST, true)
                                .requireProperty(SOUTH, true)
                                .requireProperty(BlockStateProperties.DOWN, true))
                        .where('l', BlockInWorldPredicateBuilder
                                .of(simpleWoodBeam)
                                .requireProperty(EAST, true)
                                .requireProperty(SOUTH, true)
                                .requireProperty(BlockStateProperties.DOWN, true))
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
                                .requireProperty(BlockStateProperties.DOWN, true))
                        .where('*', BlockInWorldPredicateBuilder
                                .of(simpleWoodBeam)
                                .requireProperty(WEST, true)
                                .requireProperty(NORTH, true)
                                .requireProperty(SOUTH, true)
                                .requireProperty(BlockStateProperties.DOWN, true))
                        .where('A', BlockInWorldPredicateBuilder
                                .of(simpleWoodBeam)
                                .requireProperty(EAST, true)
                                .requireProperty(WEST, true)
                                .requireProperty(SOUTH, true)
                                .requireProperty(BlockStateProperties.UP, true)
                                .requireProperty(NORTH, true))
                        .where('v', BlockInWorldPredicateBuilder
                                .of(simpleWoodBeam)
                                .requireProperty(EAST, true)
                                .requireProperty(BlockStateProperties.DOWN, true)
                                .requireProperty(NORTH, true))
                        .where('w', BlockInWorldPredicateBuilder
                                .of(simpleWoodBeam)
                                .requireProperty(WEST, true)
                                .requireProperty(BlockStateProperties.DOWN, true)
                                .requireProperty(NORTH, true))
                        .where('b', BlockInWorldPredicateBuilder
                                .of(simpleWoodBeam)
                                .requireProperty(EAST, true)
                                .requireProperty(WEST, true)
                                .requireProperty(NORTH, true)
                                .requireProperty(BlockStateProperties.UP, true))
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
                                .of(WOOD_SUPPORT_BEAM_BLOCK.get())
                                .requireProperty(FullLengthBeamBlock.HORIZONTAL_AXIS, Direction.Axis.X)
                                .requireProperty(DOWN, FullLengthBeamBlock.BeamConnection.NONE))
                        .where('/', BlockInWorldPredicateBuilder
                                .of(BlockTags.LOGS)
                                .requireProperty(AXIS, Direction.Axis.Y))
                        .where('-', BlockInWorldPredicateBuilder.of(METAL_BEAM_BLOCK.get())
                                .requireProperty(FullLengthBeamBlock.HORIZONTAL_AXIS, Direction.Axis.X)
                                .requireProperty(DOWN, FullLengthBeamBlock.BeamConnection.NONE))
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
                                " lgr ")
                        .aisle(
                                "     ",
                                " /g/ ",
                                " /-/ ")
                        .aisle(
                                "  |  ",
                                "  |  ",
                                " l|r ")
                        .aisle(
                                "     ",
                                "     ",
                                "pl-re")
                        .addRequisite(cauldron)
                        .addRequisite(hooks)
                        .addRequisite(leather2)
                        .addRequisite(leads2)
                        .addRequisite(planks2)
                        .addRequisite(nails2)
        );

        ItemStack ballistaStick = STICK.getDefaultInstance();
        ItemStack ballistaCrossbow = CROSSBOW.getDefaultInstance();
        ItemStack ballistaPlanks = WOOD_PLANK_ITEM.get().getDefaultInstance();
        ItemStack ballistaSupportBeams = WOOD_SUPPORT_BEAM_ITEM.get().getDefaultInstance();
        ItemStack ballistaIron = IRON_INGOT.getDefaultInstance();
        ItemStack ballistaLeads = LEAD.getDefaultInstance();
        ItemStack ballistaBars = IRON_BARS.getDefaultInstance();

        ballistaStick.setCount(4);
        ballistaPlanks.setCount(4);
        ballistaSupportBeams.setCount(2);
        ballistaIron.setCount(3);
        ballistaLeads.setCount(2);

        RECIPES.put(ezRl("ballista"),
                builder("ballista")
                        .where('|', zAxisWoodBeam().requireProperty(VERTICAL, true).requireProperty(UP, BeamConnection.PARALLEL))
                        .where('-', zAxisWoodBeam().requireProperty(VERTICAL, false))
                        .where('o', woodWheel(Direction.DOWN))
                        .aisle(
                                "o",
                                "|"
                        )
                        .aisle(
                                " ",
                                "-"
                        )
                        .addRequisite(ballistaStick)
                        .addRequisite(ballistaCrossbow)
                        .addRequisite(ballistaPlanks)
                        .addRequisite(ballistaSupportBeams)
                        .addRequisite(ballistaIron)
                        .addRequisite(ballistaLeads)
                        .addRequisite(ballistaBars)
        );

        ItemStack mortarFlint = FLINT.getDefaultInstance();
        ItemStack mortarIron = IRON_INGOT.getDefaultInstance();
        ItemStack mortarIronBlock = Items.IRON_BLOCK.getDefaultInstance();
        ItemStack mortarHooks = TRIPWIRE_HOOK.getDefaultInstance();
        ItemStack mortarLeather = LEATHER.getDefaultInstance();

        mortarIron.setCount(4);
        mortarIronBlock.setCount(3);
        mortarHooks.setCount(2);
        mortarLeather.setCount(2);

        RECIPES.put(ezRl("mortar"),
                builder("mortar")
                        .where('-', xAxisWoodBeam().requireProperty(LEFT, true).requireProperty(RIGHT, true))
                        .where('m', xAxisWoodBeam().requireProperty(RIGHT, true))
                        .where('+', BlockInWorldPredicateBuilder.of(METAL_BEAM_BLOCK.get()).requireProperty(LEFT, true).requireProperty(RIGHT, true))
                        .where('r', BlockInWorldPredicateBuilder.of(SIMPLE_WOOD_BEAM_BLOCK.get()).requireProperty(NORTH, true).requireProperty(WEST, true))
                        .where('l', BlockInWorldPredicateBuilder.of(SIMPLE_WOOD_BEAM_BLOCK.get()).requireProperty(NORTH, true).requireProperty(EAST, true))
                        .where('<', woodWheel(Direction.EAST))
                        .where('>', woodWheel(Direction.WEST))
                        .aisle(" mm ")
                        .aisle("<++>")
                        .aisle(" -- ")
                        .aisle(" lr ")
                        .addRequisite(mortarFlint)
                        .addRequisite(mortarIron)
                        .addRequisite(mortarIronBlock)
                        .addRequisite(mortarHooks)
                        .addRequisite(mortarLeather)
        );

        ItemStack trebuchetLadders = LADDER.getDefaultInstance();
        ItemStack trebuchetSupport = WOOD_SUPPORT_BEAM_ITEM.get().getDefaultInstance();
        ItemStack trebuchetNails = NAILS_ITEM.get().getDefaultInstance();
        ItemStack trebuchetIron = IRON_INGOT.getDefaultInstance();
        ItemStack trebuchetGravel = GRAVEL.getDefaultInstance();
        ItemStack trebuchetLeads = LEAD.getDefaultInstance();
        ItemStack trebuchetLeather = LEATHER.getDefaultInstance();
        ItemStack trebuchetSticks = STICK.getDefaultInstance();
        ItemStack trebuchetHooks = TRIPWIRE_HOOK.getDefaultInstance();

        trebuchetLadders.setCount(4);
        trebuchetSupport.setCount(6);
        trebuchetNails.setCount(64);
        trebuchetIron.setCount(30);
        trebuchetGravel.setCount(32);
        trebuchetLeads.setCount(24);
        trebuchetLeather.setCount(14);
        trebuchetSticks.setCount(8);

        RECIPES.put(ezRl("trebuchet"),
                builder("trebuchet")
                        .where('-', BlockInWorldPredicateBuilder.of(BlockTags.LOGS).requireProperty(AXIS, Direction.Axis.X))
                        .where('.', BlockInWorldPredicateBuilder.of(BlockTags.LOGS).requireProperty(AXIS, Direction.Axis.Z))
                        .where('^', BlockInWorldPredicateBuilder.of(BlockTags.LOGS).requireProperty(AXIS, Direction.Axis.Y))
                        .where('b', BlockInWorldPredicateBuilder.of(INDUSTRIAL_WOOD_POLYBEAM_BLOCK.get()).requireProperty(NORTH, true).requireProperty(SOUTH, true))
                        .where('l', BlockInWorldPredicateBuilder.of(INDUSTRIAL_WOOD_POLYBEAM_BLOCK.get()).requireProperty(NORTH, true).requireProperty(EAST, true))
                        .where('r', BlockInWorldPredicateBuilder.of(INDUSTRIAL_WOOD_POLYBEAM_BLOCK.get()).requireProperty(NORTH, true).requireProperty(WEST, true))
                        .where('d', BlockInWorldPredicateBuilder.of(SIMPLE_WOOD_BEAM_BLOCK.get()).requireProperty(EAST, true).requireProperty(WEST, true))
                        .where('_', BlockInWorldPredicateBuilder.of(BlockTags.WOODEN_SLABS).requireProperty(SLAB_TYPE, SlabType.BOTTOM))
                        .where('<', woodWheel(Direction.EAST))
                        .where('>', woodWheel(Direction.WEST))
                        .where('+', zAxisWoodBeam().requireProperty(VERTICAL, true).requireProperty(LEFT, true).requireProperty(RIGHT, true))
                        .where('c', xAxisMetalBeam())
                        .where('e', BlockInWorldPredicateBuilder.of(INDUSTRIAL_WOOD_POLYBEAM_BLOCK.get()).requireProperty(NORTH, true).requireProperty(SOUTH, true).requireProperty(EAST, true))
                        .where('f', BlockInWorldPredicateBuilder.of(INDUSTRIAL_WOOD_POLYBEAM_BLOCK.get()).requireProperty(NORTH, true).requireProperty(SOUTH, true).requireProperty(WEST, true))
                        .where('g', BlockInWorldPredicateBuilder.of(SIMPLE_WOOD_BEAM_BLOCK.get()).requireProperty(SOUTH, true).requireProperty(WEST, true))
                        .where('h', BlockInWorldPredicateBuilder.of(SIMPLE_WOOD_BEAM_BLOCK.get()).requireProperty(SOUTH, true).requireProperty(EAST, true))
                        .where('i', BlockInWorldPredicateBuilder.of(SIMPLE_WOOD_BEAM_BLOCK.get()).requireProperty(SOUTH, true).requireProperty(NORTH, true))
                        .where('j', BlockInWorldPredicateBuilder.of(SIMPLE_WOOD_BEAM_BLOCK.get()).requireProperty(NORTH, true).requireProperty(WEST, true))
                        .where('k', BlockInWorldPredicateBuilder.of(SIMPLE_WOOD_BEAM_BLOCK.get()).requireProperty(NORTH, true).requireProperty(EAST, true))
                        .where('m', BlockInWorldPredicateBuilder.of(WOOD_SUPPORT_BEAM_BLOCK.get()).requireProperty(VERTICAL, true).requireProperty(FullLengthBeamBlock.HORIZONTAL_AXIS, Direction.Axis.Z).requireProperty(DOWN, BeamConnection.PARALLEL))
                        .where('n', BlockInWorldPredicateBuilder.of(WOOD_SUPPORT_BEAM_BLOCK.get()).requireProperty(VERTICAL, true).requireProperty(FullLengthBeamBlock.HORIZONTAL_AXIS, Direction.Axis.Z).requireProperty(UP, BeamConnection.PARALLEL))
                        .where('o', BlockInWorldPredicateBuilder.of(Blocks.BARREL).requireProperty(FACING, Direction.UP).requireProperty(FACING, Direction.DOWN))
                        .where('p', BlockInWorldPredicateBuilder.of(WOOD_SUPPORT_BEAM_BLOCK.get()).requireProperty(VERTICAL, false).requireProperty(FullLengthBeamBlock.HORIZONTAL_AXIS, Direction.Axis.Z).requireProperty(UP, BeamConnection.PARALLEL))
                        .where('q', BlockInWorldPredicateBuilder.of(WOOD_SUPPORT_BEAM_BLOCK.get()).requireProperty(VERTICAL, true).requireProperty(FullLengthBeamBlock.HORIZONTAL_AXIS, Direction.Axis.Z))
                        .where('s', BlockInWorldPredicateBuilder.of(INDUSTRIAL_WOOD_POLYBEAM_BLOCK.get()).requireProperty(NORTH, true).requireProperty(WEST, true).requireProperty(EAST, true))
                        .where('t', BlockInWorldPredicateBuilder.of(WOOD_SUPPORT_BEAM_BLOCK.get()).requireProperty(VERTICAL, true).requireProperty(FullLengthBeamBlock.HORIZONTAL_AXIS, Direction.Axis.Z).requireProperty(DOWN, BeamConnection.PARALLEL).requireProperty(UP, BeamConnection.PARALLEL))
                        .where('u', BlockInWorldPredicateBuilder.of(BlockTags.WOODEN_TRAPDOORS).requireProperty(BlockStateProperties.FACING, Direction.WEST).requireProperty(OPEN, true).requireProperty(HALF, Half.BOTTOM))
                        .where('v', BlockInWorldPredicateBuilder.of(BlockTags.WOODEN_TRAPDOORS).requireProperty(BlockStateProperties.FACING, Direction.EAST).requireProperty(OPEN, true).requireProperty(HALF, Half.BOTTOM))
                        .where('w', BlockInWorldPredicateBuilder.of(METAL_BEAM_BLOCK.get()).requireProperty(FullLengthBeamBlock.HORIZONTAL_AXIS, Direction.Axis.X).requireProperty(VERTICAL, false).requireProperty(DOWN, BeamConnection.PERPENDICULAR))
                        .where('x', BlockInWorldPredicateBuilder.of(INDUSTRIAL_WOOD_POLYBEAM_BLOCK.get()).requireProperty(BlockStateProperties.UP, true).requireProperty(BlockStateProperties.DOWN, true))
                        .where('y', BlockInWorldPredicateBuilder.of(SIMPLE_WOOD_BEAM_BLOCK.get()).requireProperty(BlockStateProperties.UP, true).requireProperty(BlockStateProperties.DOWN, true))
                        .where('z', BlockInWorldPredicateBuilder.of(WOOD_SUPPORT_BEAM_BLOCK.get()).requireProperty(FullLengthBeamBlock.HORIZONTAL_AXIS, Direction.Axis.Z).requireProperty(VERTICAL, true).requireProperty(LEFT, true))
                        .where('A', BlockInWorldPredicateBuilder.of(WOOD_SUPPORT_BEAM_BLOCK.get()).requireProperty(FullLengthBeamBlock.HORIZONTAL_AXIS, Direction.Axis.Z).requireProperty(VERTICAL, true).requireProperty(RIGHT, true))
                        .where('B', xAxisWoodBeam().requireProperty(VERTICAL, false))
                        .where('C', BlockInWorldPredicateBuilder.SCHEMATIC)
                        .where('D', BlockInWorldPredicateBuilder.of(SIMPLE_WOOD_BEAM_BLOCK.get()).requireProperty(BlockStateProperties.DOWN, true))
                        .aisle(
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "   n n   ",
                                "   n n   ",
                                "   zBA   "
                        )
                        .aisle(
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "   n n   ",
                                "   m m   ",
                                "   m m   ",
                                " hd. .dg "
                        )
                        .aisle(
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "   n n   ",
                                "   q q   ",
                                "   pop   ",
                                "    o    ",
                                "         ",
                                " i b_b i "
                        )
                        .aisle(
                                "    D    ",
                                "    y    ",
                                "    y    ",
                                "    y    ",
                                "    y    ",
                                "    y    ",
                                "    y    ",
                                "    x    ",
                                "    x    ",
                                "    x    ",
                                "    x    ",
                                "    ^    ",
                                "    ^    ",
                                "    ^    ",
                                "   w^w   ",
                                "   q^q   ",
                                "   t^t   ",
                                "   u v   ",
                                "   tot   ",
                                "   qoq   ",
                                "   q q   ",
                                "hs-----sg"
                        )
                        .aisle(
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "   m m   ",
                                "   q q   ",
                                "   pop   ",
                                "    o    ",
                                "         ",
                                "i  bCb  i"
                        )
                        .aisle(
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "   m m   ",
                                "   n n   ",
                                "   n n   ",
                                "i  ._.  i"
                        )
                        .aisle(
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "   m m   ",
                                "   m m   ",
                                "kg ._. hj"
                        )
                        .aisle(
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                " i b_b i "
                        )
                        .aisle(
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                " kdf_edj "
                        )
                        .aisle(
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "  <+c+>  ",
                                "   ^_^   "
                        )
                        .aisle(
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "   b_b   "
                        )
                        .aisle(
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "   b_b   "
                        )
                        .aisle(
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "   b_b   "
                        )
                        .aisle(
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "         ",
                                "   ldr   "
                        )
                        .addRequisite(trebuchetLadders)
                        .addRequisite(trebuchetSupport)
                        .addRequisite(trebuchetNails)
                        .addRequisite(trebuchetIron)
                        .addRequisite(trebuchetGravel)
                        .addRequisite(trebuchetLeads)
                        .addRequisite(trebuchetLeather)
                        .addRequisite(trebuchetSticks)
                        .addRequisite(trebuchetHooks)
        );
    }
}
