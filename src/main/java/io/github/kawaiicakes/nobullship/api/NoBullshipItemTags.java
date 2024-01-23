package io.github.kawaiicakes.nobullship.api;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.BlockTagsProvider;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import static io.github.kawaiicakes.nobullship.Registry.*;

public class NoBullshipItemTags extends ItemTagsProvider {
    public NoBullshipItemTags(DataGenerator pGenerator, BlockTagsProvider pBlockTagsProvider, String modId, @Nullable ExistingFileHelper existingFileHelper) {
        super(pGenerator, pBlockTagsProvider, modId, existingFileHelper);
    }

    protected TagKey<Item> tag(String name) {
        return ItemTags.create(new ResourceLocation(name));
    }

    @Override
    protected void addTags() {
        this.tag(tag("nobullship:building_material"))
                .add(
                        METAL_BEAM_ITEM.get(),
                        WOOD_WHEEL_ITEM.get(),
                        SIMPLE_WOOD_BEAM_ITEM.get(),
                        SIMPLE_METAL_BEAM_ITEM.get(),
                        WILDCARD_ITEM.get(),
                        TIRE_ITEM.get()
                );

        this.tag(tag("nobullship:component"))
                .add(PITCH_ITEM.get())
                .add(WOOD_PLANK_ITEM.get())
                .add(LENS_ITEM.get())
                .add(CIRCUIT_ITEM.get())
                .add(NAILS_ITEM.get());

        this.tag(tag("forge:tar"))
                .add(PITCH_ITEM.get());

        this.tag(tag("forge:circuits"))
                .add(CIRCUIT_ITEM.get());

        this.tag(tag("forge:circuits/basic"))
                .add(CIRCUIT_ITEM.get());

        this.tag(tag("forge:nails/iron"))
                .add(NAILS_ITEM.get());

        this.tag(tag("forge:beams/wood"))
                .add(SIMPLE_WOOD_BEAM_ITEM.get())
                .add(WOOD_PLANK_ITEM.get());

        this.tag(tag("forge:beams/iron"))
                .add(SIMPLE_METAL_BEAM_ITEM.get())
                .add(METAL_BEAM_ITEM.get());

        this.tag(tag("forge:lenses"))
                .add(LENS_ITEM.get());
    }
}
