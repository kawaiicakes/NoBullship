package io.github.kawaiicakes.nobullship.api;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.nobullship.Config;
import io.github.kawaiicakes.nobullship.api.multiblock.MultiblockRecipe;
import io.github.kawaiicakes.nobullship.multiblock.MultiblockPattern;
import io.github.kawaiicakes.nobullship.network.ClientboundUpdateNoBullshipPacket;
import io.github.kawaiicakes.nobullship.schematic.SchematicItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.event.ForgeEventFactory;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

import static io.github.kawaiicakes.nobullship.Registry.*;
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
    public static final Component FAIL3 = Component.translatable("chat.nobullship.fail3").withStyle(RED);
    public static final Component FAIL4 = Component.translatable("chat.nobullship.fail4").withStyle(RED);
    protected static MultiblockRecipeManager INSTANCE = null;

    protected int globalCooldownTime = 0;
    protected int maxGlobalCooldownTime = 400000;
    protected final ICondition.IContext conditionContext;

    /**
     * A map available on the serverside containing the recipe id as a key.
     */
    private Map<ResourceLocation, MultiblockRecipe> recipes = ImmutableMap.of();
    private LinkedList<ResourceLocation> resultNameCache;

    public MultiblockRecipeManager() {
        this(ICondition.IContext.EMPTY);
    }

    public MultiblockRecipeManager(ICondition.IContext conditionContext) {
        super(GSON, "entity_recipes");
        this.conditionContext = conditionContext;

        INSTANCE = this;
    }

    public void replaceRecipes(ClientboundUpdateNoBullshipPacket packet) {
        this.recipes = packet.recipes;
        this.globalCooldownTime = packet.globalCooldownTime;
        this.maxGlobalCooldownTime = packet.maxGlobalCooldownTime;

        List<ResourceLocation> whiteList = packet.whiteList;
        List<ResourceLocation> blackList = packet.blackList;

        this.resultNameCache = this.recipes
                .values()
                .stream()
                .map(MultiblockRecipe::result)
                .filter(location -> !whiteList.contains(location))
                .collect(Collectors.toCollection(LinkedList::new));

        this.resultNameCache.addAll(blackList);
    }

    public Map<ResourceLocation, MultiblockRecipe> getRecipes() {
        return this.recipes;
    }

    /**
     * Less expensive search method to determine if a drop produced by an itemized entity dying can exist whatsoever.
     * Uses a <code>LinkedList</code> whose most recent access becomes the first element to decrease time spent
     * iterating.
     */
    public boolean isValidResultLocation(ResourceLocation entityId) {
        if (this.resultNameCache.contains(entityId)) {
            if (this.resultNameCache.getFirst().equals(entityId)) return true;

            this.resultNameCache.removeFirstOccurrence(entityId);
            this.resultNameCache.addFirst(entityId);
            return true;
        }
        return false;
    }

    public int getGlobalCooldownTime() {
        return this.globalCooldownTime;
    }

    public int getMaxGlobalCooldownTime() {
        return this.maxGlobalCooldownTime;
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
    public boolean trySpawn(@Nullable MultiblockRecipe recipe, UseOnContext context) throws RuntimeException {
        // TODO: Fake player compat & config whether fake players can use this schematic...
        if (!(context.getLevel() instanceof ServerLevel level)) return false;
        if (recipe == null) {
            level.playSound(null, context.getClickedPos(), CONSTRUCT_FAILED.get(), SoundSource.PLAYERS, 0.78F, 1.0F);
            Objects.requireNonNull(((ServerPlayer) context.getPlayer()))
                    .sendSystemMessage(FAIL3, true);
            return false;
        }

        if (Config.DISABLE_GLOBAL_COOLDOWN.get() && this.globalCooldownTime > this.maxGlobalCooldownTime) {
            level.playSound(null, context.getClickedPos(), CONSTRUCT_FAILED.get(), SoundSource.PLAYERS, 0.78F, 1.0F);
            Objects.requireNonNull(((ServerPlayer) context.getPlayer()))
                    .sendSystemMessage(FAIL4, true);
            return false;
        }

        MultiblockPattern pattern = recipe.recipe();
        ResourceLocation resultLocation = recipe.result();
        CompoundTag nbt = recipe.nbt();

        BlockPos pos = context.getClickedPos();

        Player player = context.getPlayer();

        ImmutableList<ItemStack> requisites = null;

        try {
             requisites = SchematicItem.yoinkSummedRequisites(context.getItemInHand());
        } catch (IllegalArgumentException ignored) {}

        if (requisites != null && !requisites.isEmpty()) {
            if (player != null && !player.isCreative()) {
                List<ItemStack> summedContents = getSummedContents(player.getInventory().items);
                if (!compareSummedContents(requisites, summedContents)) {
                    level.playSound(null, pos, CONSTRUCT_FAILED.get(), SoundSource.PLAYERS, 0.78F, 1.0F);
                    Objects.requireNonNull(((ServerPlayer) player))
                            .sendSystemMessage(FAIL2, true);
                    return false;
                }
            }
        }

        BlockPattern.BlockPatternMatch match;
        if (!recipe.hasSchematicBlock()) {
            match = pattern.find(level, pos);
        } else {
            match = pattern.findExact(level, pos, recipe.schematicBlockOffset());
        }

        if (match == null) {
            level.playSound(null, pos, CONSTRUCT_FAILED.get(), SoundSource.PLAYERS, 0.78F, 1.0F);
            Objects.requireNonNull(((ServerPlayer) context.getPlayer()))
                    .sendSystemMessage(FAIL, true);
            return false;
        }

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

        removeItemsFromPlayer(player, level, context, pos, requisites);

        if (nbt == null) nbt = new CompoundTag();
        nbt.putString("id", resultLocation.toString());

        boolean matchWidthIsEven = (match.getWidth() ^ 1) > match.getWidth();
        boolean matchDepthIsEven = (match.getDepth() ^ 1) > match.getDepth();

        BlockPos posForSpawn = match.getBlock(match.getWidth() / 2, match.getHeight() - 1, match.getDepth() / 2).getPos();

        double xPos;
        double zPos;

        Direction forwardsDirection = match.getForwards();

        switch (forwardsDirection) {
            case NORTH -> {
                xPos = matchWidthIsEven ? posForSpawn.getX() : posForSpawn.getX() + 0.5;
                zPos = matchDepthIsEven ? posForSpawn.getZ() + 1.0 : posForSpawn.getZ() + 0.5;
            }
            case SOUTH -> {
                xPos = matchWidthIsEven ? posForSpawn.getX() + 1.0 : posForSpawn.getX() + 0.5;
                zPos = matchDepthIsEven ? posForSpawn.getZ() : posForSpawn.getZ() + 0.5;
            }
            case EAST -> {
                xPos = matchDepthIsEven ? posForSpawn.getX() : posForSpawn.getX() + 0.5;
                zPos = matchWidthIsEven ? posForSpawn.getZ() : posForSpawn.getZ() + 0.5;
            }
            case WEST -> {
                xPos = matchDepthIsEven ? posForSpawn.getX() + 1.0 : posForSpawn.getX() + 0.5;
                zPos = matchWidthIsEven ? posForSpawn.getZ() + 1.0 : posForSpawn.getZ() + 0.5;
            }
            default -> {
                xPos = posForSpawn.getX();
                zPos = posForSpawn.getZ();
            }
        }

        double finalXPos = xPos;
        double finalZPos = zPos;
        Entity entity = EntityType.loadEntityRecursive(nbt, level, (entityType) -> {
            entityType.moveTo(finalXPos, (double)posForSpawn.getY() + 0.05D, finalZPos, entityType.getYRot(), entityType.getXRot());
            return entityType;
        });

        if (entity == null) {
            LOGGER.error("Unable to spawn entity {}!", resultLocation);
            throw new RuntimeException("Unable to spawn entity " + resultLocation + "!");
        } else {
            float yRot = match.getForwards().toYRot();
            entity.setYRot(yRot);

            if (entity instanceof Mob mob) {
                if (!ForgeEventFactory.doSpecialSpawn(mob, level, (float)entity.getX(), (float)entity.getY(), (float)entity.getZ(), null, MobSpawnType.MOB_SUMMONED)) {
                    mob.setYBodyRot(yRot);
                    mob.setYHeadRot(yRot);
                    mob.finalizeSpawn(level, level.getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.COMMAND, null, null);
                }
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

        return true;
    }

    public void incrementGlobalCooldown(int ticks) {
        this.globalCooldownTime += ticks;
    }

    public void decrementGlobalCooldown() {
        if (this.globalCooldownTime <= 0) {
            this.globalCooldownTime = 0;
            return;
        }
        this.globalCooldownTime--;
    }

    public static MultiblockRecipeManager getInstance() {
        if (INSTANCE == null) new MultiblockRecipeManager();
        return INSTANCE;
    }

    protected static void removeItemsFromPlayer(@Nullable Player player, Level level, UseOnContext context, BlockPos pos, @Nullable ImmutableList<ItemStack> requisites) {
        if (player == null || player.isCreative()) return;

        ItemStack itemInHand = context.getItemInHand();
        if (Objects.requireNonNull(itemInHand.getTag()).contains("nobullshipUses", TAG_INT)) {
            int uses = itemInHand.getTag().getInt("nobullshipUses");
            if (uses <= 1) {
                itemInHand.shrink(1);
                level.playSound(null, pos, CONSTRUCT_EXPENDED.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                itemInHand = null;
            }
        }
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

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        ImmutableMap.Builder<ResourceLocation, MultiblockRecipe> builder = ImmutableMap.builder();

        for(Map.Entry<ResourceLocation, JsonElement> entry : pObject.entrySet()) {
            ResourceLocation recipeId = entry.getKey();
            if (recipeId.getPath().startsWith("_")) continue;

            try {
                MultiblockRecipe deserializedRecipe
                        = MultiblockRecipe.fromJson(GsonHelper.convertToJsonObject(entry.getValue(), "top element"), this.conditionContext);
                if (deserializedRecipe == null) continue;

                builder.put(recipeId, deserializedRecipe);
            } catch (IllegalArgumentException | JsonParseException jsonParseException) {
                LOGGER.error("Parsing error loading recipe {}", recipeId, jsonParseException);
            }
        }

        this.recipes = builder.build();

        List<ResourceLocation> whiteList = Config.DROP_WHITELIST.get().stream().map(ResourceLocation::new).toList();
        List<ResourceLocation> blackList = Config.DROP_BLACKLIST.get().stream().map(ResourceLocation::new).toList();

        this.resultNameCache = this.recipes
                .values()
                .stream()
                .map(MultiblockRecipe::result)
                .filter(whiteList::contains)
                .collect(Collectors.toCollection(LinkedList::new));

        this.resultNameCache.addAll(blackList);

        LOGGER.info("Loaded {} recipes", recipes.size());
    }
}
