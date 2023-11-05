package io.github.kawaiicakes.nobullship.datagen;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.nobullship.data.MultiblockRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Map;

import static net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES;

public class MultiblockRecipeManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
    private static final Logger LOGGER = LogUtils.getLogger();
    protected static MultiblockRecipeManager INSTANCE = null;

    /**
     * A map available on the serverside containing the recipe id as a key.
     */
    private Map<ResourceLocation, MultiblockRecipe> recipes = ImmutableMap.of();

    protected MultiblockRecipeManager() {
        super(GSON, "entity_recipes");
    }

    @Nullable
    public MultiblockRecipe getRecipe(ResourceLocation recipeId) {
        return this.recipes.getOrDefault(recipeId, null);
    }

    /**
     * Pass a recipe ID and the context using it into here to attempt to spawn the result.
     * @return <code>true</code> if the entity is spawned. <code>false</code> otherwise.
     */
    public boolean trySpawn(ResourceLocation recipeId, UseOnContext context) {
        // TODO: config whether fake players can use this item...
        if (!(context.getLevel() instanceof ServerLevel level)) return false;

        MultiblockRecipe cachedRecipe
                = this.recipes.getOrDefault(recipeId, null);
        if (cachedRecipe == null) return false;

        BlockPattern pattern = cachedRecipe.recipe();
        ResourceLocation resultLocation = cachedRecipe.result();

        BlockPos pos = context.getClickedPos();

        // FIXME: if the blockstate of one of the blocks changes as this matches, that block will not be removed.
        BlockPattern.BlockPatternMatch match = pattern.find(level, pos);
        if (match == null) return false;

        for (int i = 0; i < pattern.getWidth(); ++i) {
            for (int j = 0; j < pattern.getHeight(); ++j) {
                BlockInWorld blockinworld = match.getBlock(i, j, 0);
                level.setBlock(blockinworld.getPos(), Blocks.AIR.defaultBlockState(), 2);
            }
        }

        Entity entity = RegistryObject.create(resultLocation, ENTITY_TYPES).get().create(level);
        if (entity == null) return false;

        BlockPos blockpos = match.getBlock(1, 2, 0).getPos();
        entity.moveTo((double)blockpos.getX() + 0.5D, (double)blockpos.getY() + 0.55D, (double)blockpos.getZ() + 0.5D, match.getForwards().getAxis() == Direction.Axis.X ? 0.0F : 90.0F, 0.0F);
        entity.setYRot(match.getForwards().getAxis() == Direction.Axis.X ? 0.0F : 90.0F);
        level.addFreshEntity(entity);

        return true;
    }

    public static MultiblockRecipeManager getInstance() {
        if (INSTANCE == null) INSTANCE = new MultiblockRecipeManager();
        return INSTANCE;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        ImmutableMap.Builder<ResourceLocation, MultiblockRecipe> builder = ImmutableMap.builder();

        for(Map.Entry<ResourceLocation, JsonElement> entry : pObject.entrySet()) {
            ResourceLocation recipeId = entry.getKey();
            if (recipeId.getPath().startsWith("_")) continue;

            try {
                MultiblockRecipe deserializedRecipe
                        = MultiblockRecipe.fromJson(GsonHelper.convertToJsonObject(entry.getValue(), "top element"));
                if (deserializedRecipe == null) {
                    LOGGER.error("Skipping recipe " + recipeId + " due to invalid syntax!");
                    continue;
                }

                builder.put(recipeId, deserializedRecipe);
            } catch (IllegalArgumentException | JsonParseException jsonParseException) {
                LOGGER.error("Parsing error loading recipe {}", recipeId, jsonParseException);
            }
        }

        this.recipes = builder.build();
        LOGGER.info("Loaded {} recipes", recipes.size());
    }
}
