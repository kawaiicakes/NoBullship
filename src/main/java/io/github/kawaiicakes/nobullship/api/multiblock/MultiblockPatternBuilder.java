package io.github.kawaiicakes.nobullship.api.multiblock;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import io.github.kawaiicakes.nobullship.api.BlockInWorldPredicate;
import io.github.kawaiicakes.nobullship.api.BlockInWorldPredicateBuilder;
import io.github.kawaiicakes.nobullship.multiblock.FinishedMultiblockRecipe;
import io.github.kawaiicakes.nobullship.multiblock.MultiblockPattern;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPatternBuilder;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static io.github.kawaiicakes.nobullship.Registry.SCHEMATIC_BLOCK_ITEM;

/**
 * When building a recipe, either in JSON or using this builder, a couple assumptions must be made.
 * You must assume you are facing north, and that the "front" of the recipe is also facing north.
 * You must assume you are looking at the "rear" of this recipe. With this in mind, the vanilla
 * {@link net.minecraft.world.level.block.state.pattern.BlockPattern.BlockPatternMatch} class defines a special <code>BlockPos</code> -
 * in a field {@link net.minecraft.world.level.block.state.pattern.BlockPattern.BlockPatternMatch#frontTopLeft} - where the pattern is measured and matches from. Despite this name, under the assumptions made here, this
 * <code>BlockPos</code> actually represents the topmost, leftmost, *closest* (or otherwise, rear) block. You must bear
 * this in mind when proceeding with work here to avoid confusion.
 */
public class MultiblockPatternBuilder extends BlockPatternBuilder {
    protected static final Logger LOGGER = LogUtils.getLogger();
    protected List<ICondition> conditions = new ArrayList<>();
    protected List<ICondition[]> currentConditions = new ArrayList<>();
    protected String resultingEntityName = null;
    protected final ResourceLocation result;
    protected final Map<String, BlockInWorldPredicateBuilder> lookupSimple = new HashMap<>();
    @Nullable
    protected CompoundTag nbt;
    @Nullable
    protected NonNullList<ItemStack> requisites;
    protected boolean hasSchematicBlock = false;

    /**
     * This constructor is only intended to be accessed by subclassing types. This is so the static <code>#of</code>
     * method may be used to more easily perform some "setup", and encourage usage of this to start from there.
     * @param nbt the <code>CompoundTag</code> NBT data to be given to the spawned entity.
     */
    protected MultiblockPatternBuilder(ResourceLocation result, @Nullable CompoundTag nbt) {
        this.lookup.put(' ', BlockInWorldPredicate.AIR);
        this.lookup.put('$', BlockInWorldPredicate.WILDCARD);
        this.result = result;
        this.nbt = nbt;
    }

    /**
     * Begin building a <code>MultiblockRecipe</code> which will spawn the passed entity <code>ResourceLocation</code>.
     * @param result the <code>ResourceLocation</code> representing the name & namespace of the entity intended to be
     *               spawned.
     */
    public static MultiblockPatternBuilder of(ResourceLocation result) {
        return of(result, null);
    }

    /**
     * Begin building a <code>MultiblockRecipe</code> which will spawn the passed entity <code>ResourceLocation</code>.
     * Identical to overload <code>#of(ResourceLocation)</code>; but this overload allows passing in the NBT data that
     * will be associated with the spawned entity.
     * @param result the <code>ResourceLocation</code> representing the name & namespace of the entity intended to be
     *               spawned.
     */
    public static MultiblockPatternBuilder of(ResourceLocation result, @Nullable CompoundTag nbt) {
        return new MultiblockPatternBuilder(result, nbt);
    }

    public MultiblockPatternBuilder addCondition(ICondition condition) {
        this.conditions.add(condition);
        return this;
    }

    /**
     * Sets the lang key for the name displayed in the workshop for the entity. Not necessary to use; the workshop will
     * default to using the entity's display name if the key is not set.
     * @param name The <code>String</code> representing a lang key for the text to display. (e.g. entity.minecraft.zombie)
     */
    public MultiblockPatternBuilder setEntityWorkshopDisplayName(String name) {
        this.resultingEntityName = name;
        return this;
    }

    /**
     * Sets the <code>CompoundTag</code> NBT data to be given to the spawned entity.
     * e.g. Setting a creeper's blast radius to 300.
     */
    public MultiblockPatternBuilder setTagOfResult(CompoundTag tag) {
        this.nbt = tag;
        return this;
    }

    /**
     * Adds an <code>ItemStack</code> that will be needed in the player's inventory to
     * successfully construct a multiblock recipe.
     */
    public MultiblockPatternBuilder addRequisite(ItemStack item) {
        if (this.requisites == null) this.requisites = NonNullList.createWithCapacity(1);
        this.requisites.add(item);
        return this;
    }

    /**
     * Adds a <code>NonNullList</code> of <code>ItemStack</code>s that will be needed in the
     * player's inventory to successfully construct a multiblock recipe.
     */
    public MultiblockPatternBuilder addRequisites(NonNullList<ItemStack> items) {
        if (this.requisites == null) this.requisites = items;
        this.requisites.addAll(items);
        return this;
    }

    /**
     * Call this whenever a <code>MultiblockPattern</code> is desired.
     * Call <code>#save</code> in the appropriate <code>RecipeProvider</code>
     * if you need to serialize this instead!
     */
    @Override
    public MultiblockPattern build() {
        return new MultiblockPattern(
                this.createPattern(),
                new ArrayList<>(this.lookupSimple.values()),
                this.totalBlocks(),
                this.serializePatternNbt()
        );
    }

    @Override
    protected BlockInWorldPredicate[][][] createPattern() {
        this.ensureAllCharactersMatched();
        BlockInWorldPredicate[][][] predicate = (BlockInWorldPredicate[][][]) Array.newInstance(BlockInWorldPredicate.class, this.pattern.size(), this.height, this.width);

        for(int i = 0; i < this.pattern.size(); ++i) {
            for(int j = 0; j < this.height; ++j) {
                for(int k = 0; k < this.width; ++k) {
                    predicate[i][j][k] = (BlockInWorldPredicate) this.lookup.get((this.pattern.get(i))[j].charAt(k));
                }
            }
        }

        return predicate;
    }

    protected NonNullList<ItemStack> totalBlocks() {
        NonNullList<ItemStack> toReturn = NonNullList.create();

        Map<Character, Integer> totalCount = new HashMap<>();
        this.lookupSimple.keySet().forEach(key -> totalCount.put(key.charAt(0), 0));

        for (String[] strings : this.pattern) {
            for (String string : strings) {
                for (char character : string.toCharArray()) {
                    if (character == ' ' || character == '$') continue;
                    int count = totalCount.get(character);
                    totalCount.put(character, ++count);
                }
            }
        }

        totalCount.entrySet()
                .stream()
                .map(entry -> {
                    ItemStack itemStack = this.lookupSimple.get(String.valueOf(entry.getKey())).getItemized();
                    itemStack.setCount(entry.getValue());
                    return itemStack;
                })
                .forEach(toReturn::add);

        return toReturn;
    }

    /**
     * Call this inside of the appropriate <code>RecipeProvider</code> instance to serialize this recipe.
     * Use <code>#build</code> if you need the <code>MultiblockPattern</code> instead!
     */
    public void save(Consumer<FinishedMultiblockRecipe> consumer, ResourceLocation id) {
        this.currentConditions.add(conditions.toArray(new ICondition[currentConditions.size()]));
        if (this.hasSchematicBlock) {
            if (this.totalBlocks().stream().anyMatch(stack -> stack.is(SCHEMATIC_BLOCK_ITEM.get()) && stack.getCount() > 1)) {
                LOGGER.error("You may not use more than one schematic block in a recipe!");
                throw new IllegalArgumentException("You may not use more than one schematic block in a recipe!");
            }
        }
        consumer.accept(this.getResult(id));
    }

    @Nullable
    public Result getResult(ResourceLocation id) {
        try {
            return new Result(id, this.currentConditions, this.resultingEntityName, this.result, this.nbt, this.pattern, this.requisites, this.lookupSimple, this.height, this.width);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    /**
     * Adds a "layer" to the recipe pattern that "comes out" of the monitor. This method works very similarly
     * to how a shaped recipe pattern looks in a datapack. A <code>String</code>'s <code>char</code> length
     * is considered the x-length of the pattern, whereas the number of args (otherwise, the size of the array)
     * passed is the y-height of the pattern. It then follows that the number of calls to <code>#aisle</code>
     * in this builder is the z-width of the pattern.
     * <br><br>
     * The first call to this will be the "front" layer of the recipe.
     */
    @Override
    public MultiblockPatternBuilder aisle(String... pAisle) {
        return (MultiblockPatternBuilder) super.aisle(pAisle);
    }

    /**
     * Defines what <code>char</code> corresponds to a <code>BlockState</code>. All <code>char</code>s
     * used in calls to <code>#aisle</code> must be defined using this method. ' ' and '$' are used as
     * air and a wildcard respectively. These are reserved symbols; don't use them!
     * @param block the <code>{@link BlockInWorldPredicateBuilder}</code> representing the block this symbol
     *              is assigned to. The <code>BlockInWorldPredicateBuilder</code> allows for very fine
     *              control over defining the permitted block state(s).
     */
    public MultiblockPatternBuilder where(char pSymbol, BlockInWorldPredicateBuilder block) {
        if (pSymbol == ' ' || pSymbol == '$') {
            LOGGER.error("{} is a reserved character!", pSymbol);
            throw new IllegalArgumentException(pSymbol + " is a reserved character!");
        }

        if (block.equals(BlockInWorldPredicateBuilder.SCHEMATIC)) {
            if (this.hasSchematicBlock) {
                LOGGER.error("You may not use more than one schematic block in a recipe!");
                throw new IllegalArgumentException("You may not use more than one schematic block in a recipe!");
            }
            this.hasSchematicBlock = true;
        }

        this.lookupSimple.put(String.valueOf(pSymbol), block);
        return (MultiblockPatternBuilder) super.where(pSymbol, block.build());
    }

    /**
     * Don't use this!
     * This has been made non-functional because I have no reasonable way of getting the blocks that actually
     * make up the pattern in this method; this stops me from being able to calculate how many/what kind of
     * blocks are in the recipe.
     */
    @Deprecated
    @Override
    public BlockPatternBuilder where(char pSymbol, Predicate<BlockInWorld> pBlockMatcher) {
        throw new UnsupportedOperationException("This overload of #where does not function!");
    }

    public CompoundTag serializePatternNbt() {
        CompoundTag toReturn = new CompoundTag();

        ListTag patternTag = new ListTag();
        for (String[] strings : this.pattern) {
            ListTag stringArrayTag = new ListTag();
            for (String string : strings) {
                stringArrayTag.add(StringTag.valueOf(string));
            }
            patternTag.add(stringArrayTag);
        }
        toReturn.put("pattern", patternTag);

        CompoundTag paletteTag = new CompoundTag();
        for (Map.Entry<String, BlockInWorldPredicateBuilder> entry : this.lookupSimple.entrySet()) {
            CompoundTag value = entry.getValue().toNbt();
            if (value == null) {
                LOGGER.error("Unable to deserialize BlockInWorldPredicateBuilder to NBT!");
                continue;
            }
            paletteTag.put(entry.getKey(), value);
        }
        toReturn.put("palette", paletteTag);

        return toReturn;
    }

    public static class Result implements FinishedMultiblockRecipe {
        protected List<ICondition[]> conditions;
        protected final ResourceLocation id;
        @Nullable
        protected final String resultingEntityName;
        protected final ResourceLocation result;
        @Nullable
        protected final CompoundTag nbt;
        protected final List<String[]> recipe;
        @Nullable
        protected final NonNullList<ItemStack> requisites;
        protected final Map<String, BlockInWorldPredicateBuilder> lookup;
        protected final int height;
        protected final int width;

        public Result(ResourceLocation id, List<ICondition[]> conditions, @Nullable String resultingEntityName, ResourceLocation result, @Nullable CompoundTag nbt, List<String[]> recipe, @Nullable NonNullList<ItemStack> requisites, Map<String, BlockInWorldPredicateBuilder> lookup, int height, int width) {
            this.conditions = conditions;
            this.id = id;
            this.resultingEntityName = resultingEntityName;
            this.result = result;
            this.nbt = nbt;
            this.recipe = recipe;
            this.requisites = requisites;
            this.lookup = lookup;
            this.height = height;
            this.width = width;
        }

        @Override
        public void serializeRecipeData(JsonObject pJson) {
            JsonObject keyMappings = new JsonObject();
            for (Map.Entry<String, BlockInWorldPredicateBuilder> entry : this.lookup.entrySet()) {
                try {
                    keyMappings.add(entry.getKey(), entry.getValue().toJson());
                } catch (RuntimeException e) {
                    LOGGER.error("Error serializing recipe!", e);
                    return;
                }
            }

            JsonArray conditionsArray = new JsonArray();
            for (ICondition[] condition : this.conditions) {
                for (ICondition c : condition) conditionsArray.add(CraftingHelper.serialize(c));
            }
            if (!conditionsArray.isEmpty()) pJson.add("conditions", conditionsArray);

            JsonObject recipePattern = new JsonObject();
            for (int i = 0; i < this.recipe.size(); i++) {
                recipePattern.add("layer" + i, new JsonArray(this.recipe.get(i).length));

                for (int j = 0; j < this.recipe.get(i).length; j++) {
                    recipePattern.getAsJsonArray("layer" + i).add(this.recipe.get(i)[j]);
                }
            }

            JsonObject jsonResult = new JsonObject();
            if (this.resultingEntityName != null) jsonResult.addProperty("name", this.resultingEntityName);
            jsonResult.addProperty("entity", this.result.toString());
            if (this.nbt != null) jsonResult.add("nbt", NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, this.nbt));

            pJson.add("key", keyMappings);
            pJson.add("recipe", recipePattern);
            pJson.add("result", jsonResult);

            if (this.requisites != null && !this.requisites.isEmpty()) {
                JsonArray jsonRequisites = new JsonArray();
                for (ItemStack item : this.requisites) {
                    JsonObject serialized = new JsonObject();

                    ResourceLocation itemName = ForgeRegistries.ITEMS.getKey(item.getItem());
                    if (itemName == null) continue;
                    serialized.addProperty("item", itemName.toString());

                    if (!item.isEmpty() && item.hasTag()) {
                        JsonElement itemTag = NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, item.getOrCreateTag());
                        if (itemTag.isJsonObject()) serialized.add("nbt", itemTag);
                    }

                    if (item.getCount() > 1) serialized.addProperty("count", item.getCount());

                    jsonRequisites.add(serialized);
                }
                pJson.add("requisites", jsonRequisites);
            }
        }

        @Override
        public ResourceLocation getId() {
            return this.id;
        }
    }
}
