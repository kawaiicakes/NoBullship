package io.github.kawaiicakes.nobullship.api;

import net.minecraft.core.Registry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.BlockTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import static io.github.kawaiicakes.nobullship.Registry.*;
import static net.minecraftforge.common.Tags.Blocks.STORAGE_BLOCKS;

public class NoBullshipBlockTags extends BlockTagsProvider {
    public static final TagKey<Block> POLYBEAM_CONNECTOR = create("simple-beam_connector");
    public static final TagKey<Block> FL_BEAM_CONNECTOR = create("full_length-beam_connector");
    public static final TagKey<Block> NO_BS_MATERIAL = create("building_material");
    public static final TagKey<Block> SIMPLE_BEAM_TAG = create("simple_beam");

    public NoBullshipBlockTags(DataGenerator pGenerator, String modId, @Nullable ExistingFileHelper existingFileHelper) {
        super(pGenerator, modId, existingFileHelper);
    }

    protected TagKey<Block> tag(String name) {
        return BlockTags.create(new ResourceLocation(name));
    }

    @Override
    protected void addTags() {
        this.tag(POLYBEAM_CONNECTOR)
                .addTag(BlockTags.LOGS)
                .addTag(BlockTags.PLANKS)
                .addTag(STORAGE_BLOCKS);

        this.tag(FL_BEAM_CONNECTOR)
                .add(METAL_BEAM_BLOCK.get())
                .add(WOOD_SUPPORT_BEAM_BLOCK.get());

        this.tag(NO_BS_MATERIAL).add(
                WILDCARD_BLOCK.get(),
                METAL_BEAM_BLOCK.get(),
                TIRE_BLOCK.get(),
                WOOD_WHEEL_BLOCK.get(),
                SIMPLE_WOOD_BEAM_BLOCK.get(),
                SIMPLE_METAL_BEAM_BLOCK.get()
        );

        this.tag(SIMPLE_BEAM_TAG).add(
                SIMPLE_WOOD_BEAM_BLOCK.get(),
                SIMPLE_METAL_BEAM_BLOCK.get()
        );

        this.tag(tag("forge:beams/wood"))
                .add(SIMPLE_WOOD_BEAM_BLOCK.get());

        this.tag(tag("forge:beams/iron"))
                .add(
                        SIMPLE_METAL_BEAM_BLOCK.get(),
                        METAL_BEAM_BLOCK.get()
                );

        this.tag(tag("forge:wheels"))
                .add(
                        TIRE_BLOCK.get(),
                        WOOD_WHEEL_BLOCK.get()
                );
    }

    protected static TagKey<Block> create(String pName) {
        return TagKey.create(Registry.BLOCK_REGISTRY, new ResourceLocation("nobullship:" + pName));
    }
}
