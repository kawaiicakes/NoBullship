package io.github.kawaiicakes.nobullship.api;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.nobullship.multiblock.MultiblockRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraftforge.event.ForgeEventFactory;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;

import static io.github.kawaiicakes.nobullship.NoBullship.CONSTRUCT_SUCCESS;
import static io.github.kawaiicakes.nobullship.schematic.SchematicRecipe.compareSummedContents;
import static io.github.kawaiicakes.nobullship.schematic.SchematicRecipe.getSummedContents;
import static net.minecraft.ChatFormatting.RED;
import static net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE;
import static net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN;

public class MultiblockRecipeManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Component FAIL = Component.translatable("chat.nobullship.fail").withStyle(RED);
    public static final Component FAIL2 = Component.translatable("chat.nobullship.fail2").withStyle(RED);
    protected static MultiblockRecipeManager INSTANCE = null;

    /**
     * A map available on the serverside containing the recipe id as a key.
     */
    private Map<ResourceLocation, MultiblockRecipe> recipes = ImmutableMap.of();

    protected MultiblockRecipeManager() {
        super(GSON, "entity_recipes");
    }

    public Optional<MultiblockRecipe> getRecipe(ResourceLocation id) {
        return Optional.ofNullable(this.recipes.getOrDefault(id, null));
    }

    @Nullable
    public Entity getEntityForRecipe(ResourceLocation id, Level level) {
        MultiblockRecipe recipe = this.recipes.getOrDefault(id, null);
        if (recipe == null) return null;

        CompoundTag nbt = recipe.nbt() == null ? new CompoundTag() : recipe.nbt();
        //noinspection DataFlowIssue
        nbt.putString("id", recipe.result().toString());

        return EntityType.loadEntityRecursive(nbt, level, (type) -> type);
    }

    /**
     * Pass a recipe ID and the context using it into here to attempt to spawn the result.
     */
    public void trySpawn(ResourceLocation recipeId, UseOnContext context) {
        // TODO: config whether fake players can use this schematic...
        if (!(context.getLevel() instanceof ServerLevel level)) return;

        MultiblockRecipe cachedRecipe
                = this.recipes.getOrDefault(recipeId, null);
        if (cachedRecipe == null) return;

        BlockPattern pattern = cachedRecipe.recipe();
        ResourceLocation resultLocation = cachedRecipe.result();
        CompoundTag nbt = cachedRecipe.nbt();

        BlockPos pos = context.getClickedPos();

        Player player = null;
        boolean satisfiesRequirements;
        ImmutableList<ItemStack> requisites = cachedRecipe.requisites();
        if (requisites != null && !requisites.isEmpty()) {
            if (context.getPlayer() != null && !context.getPlayer().isCreative()) {
                player = context.getPlayer();
                List<ItemStack> summedContents = getSummedContents(context.getPlayer().getInventory().items);
                List<ItemStack> requirementContents = getSummedContents(requisites);
                satisfiesRequirements = compareSummedContents(requirementContents, summedContents);
                if (!satisfiesRequirements) {
                    level.playSound(null, pos, BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
                    Objects.requireNonNull(((ServerPlayer) context.getPlayer()))
                            .sendSystemMessage(FAIL2, true);
                    return;
                }
            }
        }

        BlockPattern.BlockPatternMatch match = pattern.find(level, pos);
        if (match == null) {
            level.playSound(null, pos, BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
            Objects.requireNonNull(((ServerPlayer) context.getPlayer()))
                    .sendSystemMessage(FAIL, true);
            return;
        }

        // TODO: dynamic checking for BlockState directions (specifically, rotating required facings w/ the orientation)
        // FIXME: other checks, missing logic & optimizations (see CarvedPumpkinBlock)
        for (int i = 0; i < pattern.getDepth(); ++i) {
            for (int j = 0; j < pattern.getWidth(); ++j) {
                for (int k = 0; k < pattern.getHeight(); ++k) {
                    BlockInWorld blockinworld = match.getBlock(j, k, i);
                    level.setBlock(blockinworld.getPos(), Blocks.AIR.defaultBlockState(), 2);
                    level.levelEvent(2001, blockinworld.getPos(), Block.getId(blockinworld.getState()));
                }
            }
        }

        level.playSound(null, pos, CONSTRUCT_SUCCESS.get(), SoundSource.PLAYERS, 0.77F, 1.0F);
        level.sendParticles(LARGE_SMOKE, pos.getX(), pos.getY(), pos.getZ(), 7, 0.2, 0.2, 0.2, 0.3);

        if (player != null && !player.isCreative()) {
            ArrayList<ItemStack> temporaryRequisites = new ArrayList<>(requisites);

            for (int i = 0; i < player.getInventory().items.size(); i++) {
                ItemStack stackInSlot = player.getInventory().items.get(i);
                ItemStack craftingRemaining = stackInSlot.getCraftingRemainingItem();

                final ItemStack finalItemstack = stackInSlot;
                ItemStack requiredItemRemaining = temporaryRequisites.stream()
                        .filter(standard -> ItemStack.isSameItemSameTags(standard, finalItemstack))
                        .findFirst()
                        .orElse(null);

                if (requiredItemRemaining == null) continue;

                int decrement = Math.min(stackInSlot.getCount(), requiredItemRemaining.getCount());

                if (!stackInSlot.isEmpty()) {
                    stackInSlot.shrink(decrement);
                    requiredItemRemaining.shrink(decrement);
                }

                if (craftingRemaining.isEmpty()) continue;

                if (stackInSlot.isEmpty()) {
                    player.getInventory().setItem(i, craftingRemaining);
                } else if (ItemStack.isSame(stackInSlot, craftingRemaining) && ItemStack.tagMatches(stackInSlot, craftingRemaining)) {
                    craftingRemaining.grow(stackInSlot.getCount());
                    player.getInventory().setItem(i, craftingRemaining);
                } else if (!player.getInventory().add(craftingRemaining)) {
                    player.drop(craftingRemaining, false);
                }
            }
        }

        if (nbt == null) nbt = new CompoundTag();
        nbt.putString("id", resultLocation.toString());
        BlockPos blockpos = match.getBlock(1, 2, 0).getPos();

        Entity entity = EntityType.loadEntityRecursive(nbt, level, (entityType) -> {
            entityType.moveTo((double)blockpos.getX() + 0.5D, (double)blockpos.getY() + 0.55D, (double)blockpos.getZ() + 0.5D, entityType.getYRot(), entityType.getXRot());
            return entityType;
        });

        if (entity == null) {
            LOGGER.error("Unable to spawn entity {}!", resultLocation);
            throw new RuntimeException("Unable to spawn entity " + resultLocation + "!");
        } else {
            if (entity instanceof Mob mob) {
                if (!ForgeEventFactory.doSpecialSpawn(mob, level, (float)entity.getX(), (float)entity.getY(), (float)entity.getZ(), null, MobSpawnType.MOB_SUMMONED))
                    mob.finalizeSpawn(level, level.getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.COMMAND, null, null);
            }

            if (!level.tryAddFreshEntityWithPassengers(entity)) {
                throw new IllegalArgumentException("Entity " + entity.getName() + " has a duplicate UUID!");
            }
        }

        // This doesn't seem to do anything on the server... is this intended for the client?
        for (int i1 = 0; i1 < pattern.getDepth(); ++i1) {
            for (int j1 = 0; j1 < pattern.getWidth(); ++j1) {
                for (int k1 = 0; k1 < pattern.getHeight(); ++k1) {
                    BlockInWorld blockInWorld = match.getBlock(j1, k1, i1);
                    level.blockUpdated(blockInWorld.getPos(), Blocks.AIR);
                }
            }
        }
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
