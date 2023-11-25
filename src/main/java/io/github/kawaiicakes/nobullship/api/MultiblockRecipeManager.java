package io.github.kawaiicakes.nobullship.api;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.nobullship.api.multiblock.MultiblockRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
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

import static io.github.kawaiicakes.nobullship.NoBullship.*;
import static io.github.kawaiicakes.nobullship.schematic.SchematicRecipe.compareSummedContents;
import static io.github.kawaiicakes.nobullship.schematic.SchematicRecipe.getSummedContents;
import static net.minecraft.ChatFormatting.RED;
import static net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE;
import static net.minecraft.nbt.Tag.TAG_INT;

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

    public Map<ResourceLocation, NonNullList<ItemStack>> getBlockItemsForRecipes() {
        Map<ResourceLocation, NonNullList<ItemStack>> toReturn = new HashMap<>();

        this.recipes.forEach((key, value) -> {
            NonNullList<ItemStack> blocks = NonNullList.create();
            blocks.addAll(value.recipe().getTotalBlocks());
            toReturn.put(key, blocks);
        });

        return toReturn;
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

        Player player = context.getPlayer();
        ImmutableList<ItemStack> requisites = cachedRecipe.requisites();
        if (requisites != null && !requisites.isEmpty()) {
            if (player != null && !player.isCreative()) {
                List<ItemStack> summedContents = getSummedContents(player.getInventory().items);
                List<ItemStack> requirementContents = getSummedContents(requisites);
                if (!compareSummedContents(requirementContents, summedContents)) {
                    level.playSound(null, pos, CONSTRUCT_FAILED.get(), SoundSource.PLAYERS, 0.78F, 1.0F);
                    Objects.requireNonNull(((ServerPlayer) player))
                            .sendSystemMessage(FAIL2, true);
                    return;
                }
            }
        }

        BlockPattern.BlockPatternMatch match = pattern.find(level, pos);
        if (match == null) {
            level.playSound(null, pos, CONSTRUCT_FAILED.get(), SoundSource.PLAYERS, 0.78F, 1.0F);
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
            ItemStack itemInHand = context.getItemInHand();
            if (Objects.requireNonNull(itemInHand.getTag()).contains("nobullshipUses", TAG_INT)) {
                int uses = itemInHand.getTag().getInt("nobullshipUses");
                if (uses <= 1) {
                    itemInHand.shrink(1);
                    level.playSound(null, pos, CONSTRUCT_EXPENDED.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                    itemInHand = null;
                }
            } // FIXME: I hate nesting...
            if (requisites != null) {
                for (int i = 0; i < player.getInventory().items.size(); i++) {
                    ItemStack stackInSlot = player.getInventory().items.get(i);
                    ItemStack craftingRemaining = stackInSlot.getCraftingRemainingItem();

                    final ItemStack finalItemstack = stackInSlot;
                    ItemStack requiredItemRemaining = requisites.stream()
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
            if (itemInHand != null && Objects.requireNonNull(itemInHand.getTag()).contains("nobullshipUses", TAG_INT)) {
                ItemStack toNewStack = itemInHand;
                if (itemInHand.getCount() > 1) {
                    itemInHand.shrink(1);
                    toNewStack = itemInHand.copy();
                    toNewStack.setCount(1);
                }
                //noinspection DataFlowIssue (since toNewStack is based off itemInHand, it is known that it's not null)
                int uses = toNewStack.getTag().getInt("nobullshipUses");
                toNewStack.getTag().remove("nobullshipUses");
                toNewStack.getTag().putInt("nobullshipUses", uses - 1);

                if (toNewStack != itemInHand && !player.getInventory().add(toNewStack)) player.drop(toNewStack, false);
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
