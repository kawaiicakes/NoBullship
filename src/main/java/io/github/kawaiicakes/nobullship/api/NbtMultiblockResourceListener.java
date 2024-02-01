package io.github.kawaiicakes.nobullship.api;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.nobullship.api.multiblock.MultiblockRecipe;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class NbtMultiblockResourceListener extends SimplePreparableReloadListener<Map<ResourceLocation, CompoundTag>> {
    protected static final Logger LOGGER = LogUtils.getLogger();
    protected static final String PATH_SUFFIX = ".nbt";
    protected static final int PATH_SUFFIX_LENGTH = ".nbt".length();
    private final MultiblockRecipeManager multiblockRecipeManager;
    private final String directory;

    public NbtMultiblockResourceListener(MultiblockRecipeManager multiblockRecipeManager) {
        this.multiblockRecipeManager = multiblockRecipeManager;
        this.directory = "nbt_entity_recipes";
    }

    @Override
    protected Map<ResourceLocation, CompoundTag> prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        Map<ResourceLocation, CompoundTag> toReturn = new HashMap<>();

        int i = this.directory.length() + 1;

        for(Map.Entry<ResourceLocation, Resource> entry : pResourceManager.listResources(this.directory, (fileResourceLocation) ->
                fileResourceLocation.getPath().endsWith(PATH_SUFFIX)).entrySet()) {
            ResourceLocation resourceId = entry.getKey();
            String s = resourceId.getPath();
            ResourceLocation fullResourceId = new ResourceLocation(resourceId.getNamespace(), s.substring(i, s.length() - PATH_SUFFIX_LENGTH));

            try {
                InputStream inputStream = entry.getValue().open();

                try {
                    CompoundTag fromFile = NbtIo.readCompressed(inputStream);
                    //noinspection ConstantValue
                    if (fromFile != null) {
                        CompoundTag duplicateTag = toReturn.put(fullResourceId, fromFile);
                        if (duplicateTag != null) {
                            throw new IllegalStateException("Duplicate data file ignored with ID " + fullResourceId);
                        }
                    } else {
                        LOGGER.error("Couldn't load data file {} from {} as it's null or empty", fullResourceId, resourceId);
                    }
                } catch (Throwable e) {
                    //noinspection ConstantValue
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (Throwable f) {
                            f.addSuppressed(f);
                        }
                    }

                    throw e;
                }

                //noinspection ConstantValue
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IllegalArgumentException | IOException parseException) {
                LOGGER.error("Couldn't parse data file {} from {}", fullResourceId, resourceId, parseException );
            }
        }

        return toReturn;
    }

    @Override
    protected void apply(Map<ResourceLocation, CompoundTag> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        ImmutableMap.Builder<ResourceLocation, MultiblockRecipe> builder = ImmutableMap.builder();

        for (Map.Entry<ResourceLocation, CompoundTag> entry : pObject.entrySet()) {
            ResourceLocation recipeId = entry.getKey();
            if (recipeId.getPath().startsWith("_")) continue;

            try {
                MultiblockRecipe deserializedRecipe
                        = MultiblockRecipe.fromRawNbt(entry.getValue(), recipeId);
                if (deserializedRecipe == null) continue;

                builder.put(recipeId, deserializedRecipe);
            } catch (IllegalArgumentException | JsonParseException jsonParseException) {
                LOGGER.error("Parsing error loading recipe {}", recipeId, jsonParseException);
            }
        }

        this.multiblockRecipeManager.addRecipes(builder.build());
    }
}
