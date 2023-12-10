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
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPatternBuilder;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static net.minecraft.world.level.block.Blocks.AIR;
import static net.minecraftforge.registries.ForgeRegistries.BLOCKS;

public class MultiblockRecipeBuilder extends BlockPatternBuilder {
    protected static final Logger LOGGER = LogUtils.getLogger();
    protected final ResourceLocation result;
    protected final Map<String, BlockInWorldPredicateBuilder> lookupSimple = new HashMap<>();
    @Nullable
    protected CompoundTag nbt;
    @Nullable
    protected NonNullList<ItemStack> requisites;

    /**
     * This constructor is only intended to be accessed by subclassing types. This is so the static <code>#of</code>
     * method may be used to more easily perform some "setup", and encourage usage of this to start from there.
     * @param nbt the <code>CompoundTag</code> NBT data to be given to the spawned entity.
     */
    protected MultiblockRecipeBuilder(ResourceLocation result, @Nullable CompoundTag nbt) {
        this.lookup.put(' ', BlockInWorldPredicateBuilder.of(AIR).build());
        this.lookup.put('$', BlockInWorldPredicate.WILDCARD);
        this.result = result;
        this.nbt = nbt;
    }

    /**
     * Begin building a <code>MultiblockRecipe</code> which will spawn the passed entity <code>ResourceLocation</code>.
     * @param result the <code>ResourceLocation</code> representing the name & namespace of the entity intended to be
     *               spawned.
     */
    public static MultiblockRecipeBuilder of(ResourceLocation result) {
        return of(result, null);
    }

    /**
     * Begin building a <code>MultiblockRecipe</code> which will spawn the passed entity <code>ResourceLocation</code>.
     * Identical to overload <code>#of(ResourceLocation)</code>; but this overload allows passing in the NBT data that
     * will be associated with the spawned entity.
     * @param result the <code>ResourceLocation</code> representing the name & namespace of the entity intended to be
     *               spawned.
     */
    public static MultiblockRecipeBuilder of(ResourceLocation result, @Nullable CompoundTag nbt) {
        return new MultiblockRecipeBuilder(result, nbt);
    }

    /**
     * Sets the <code>CompoundTag</code> NBT data to be given to the spawned entity.
     * e.g. Setting a creeper's blast radius to 300.
     */
    public MultiblockRecipeBuilder setTagOfResult(CompoundTag tag) {
        this.nbt = tag;
        return this;
    }

    /**
     * Adds an <code>ItemStack</code> that will be needed in the player's inventory to
     * successfully construct a multiblock recipe.
     */
    public MultiblockRecipeBuilder addRequisite(ItemStack item) {
        if (this.requisites == null) this.requisites = NonNullList.createWithCapacity(1);
        this.requisites.add(item);
        return this;
    }

    /**
     * Adds a <code>NonNullList</code> of <code>ItemStack</code>s that will be needed in the
     * player's inventory to successfully construct a multiblock recipe.
     */
    public MultiblockRecipeBuilder addRequisites(NonNullList<ItemStack> items) {
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
                this.lookupSimple.values().stream().map(BlockInWorldPredicateBuilder::getDefaultBlockState).toList(),
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
                    ItemStack itemStack = new ItemStack(this.lookupSimple.get(String.valueOf(entry.getKey())).getDefaultBlockState().getBlock());
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
        consumer.accept(new Result(id, this.result, this.nbt, this.pattern, this.requisites, this.lookupSimple, this.height, this.width));
    }

    /**
     * Adds a "layer" to the recipe pattern that "comes out" of the monitor. This method works very similarly
     * to how a shaped recipe pattern looks in a datapack. A <code>String</code>'s <code>char</code> length
     * is considered the x-length of the pattern, whereas the number of args (otherwise, the size of the array)
     * passed is the y-height of the pattern. It then follows that the number of calls to <code>#aisle</code>
     * in this builder is the z-width of the pattern.
     */
    @Override
    public MultiblockRecipeBuilder aisle(String... pAisle) {
        return (MultiblockRecipeBuilder) super.aisle(pAisle);
    }

    /**
     * Defines what <code>char</code> corresponds to a <code>BlockState</code>. All <code>char</code>s
     * used in calls to <code>#aisle</code> must be defined using this method. ' ' and '$' are used as
     * air and a wildcard respectively. These are reserved symbols; don't use them!
     * @param block the <code>{@link BlockInWorldPredicateBuilder}</code> representing the block this symbol
     *              is assigned to. The <code>BlockInWorldPredicateBuilder</code> allows for very fine
     *              control over defining the permitted block state(s).
     */
    public MultiblockRecipeBuilder where(char pSymbol, BlockInWorldPredicateBuilder block) {
        if (pSymbol == ' ' || pSymbol == '$') {
            LOGGER.error("{} is a reserved character!", pSymbol);
            throw new IllegalArgumentException(pSymbol + " is a reserved character!");
        }

        this.lookupSimple.put(String.valueOf(pSymbol), block);
        return (MultiblockRecipeBuilder) super.where(pSymbol, block.build());
    }

    /**
     * Don't use this!
     * This has been made non-functional because I have no reasonable way of getting the blocks that actually
     * make up the pattern in this method; this stops me from being able to calculate how many/what kind of
     * blocks are in the recipe.
     */
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
        protected final ResourceLocation id;
        protected final ResourceLocation result;
        @Nullable
        protected final CompoundTag nbt;
        protected final List<String[]> recipe;
        @Nullable
        protected final NonNullList<ItemStack> requisites;
        protected final Map<String, BlockInWorldPredicateBuilder> lookup;
        protected final int height;
        protected final int width;

        public Result(ResourceLocation id, ResourceLocation result, @Nullable CompoundTag nbt, List<String[]> recipe, @Nullable NonNullList<ItemStack> requisites, Map<String, BlockInWorldPredicateBuilder> lookup, int height, int width) {
            this.id = id;
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
                JsonObject mapping = new JsonObject();

                JsonObject properties = new JsonObject();
                for (Property<?> property : entry.getValue().getDefaultBlockState().getProperties()) {
                    if (!entry.getValue().isRequiredProperty(property)) continue;
                    properties.add(property.getName(), entry.getValue().getPropertyValuesAsJsonArray(property));
                }

                JsonObject nbt = new JsonObject();
                JsonObject nbtStrict = new JsonObject();
                Tag nbtForParse = entry.getValue().getBlockEntityNbtData();
                Tag nbtStrictForParse = entry.getValue().getBlockEntityNbtDataStrict();
                if (nbtForParse != null) nbt.add("nbt", NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, nbtForParse));
                if (nbtStrictForParse != null) nbtStrict.add("nbt_strict", NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, nbtStrictForParse));

                // Given where this method is being called, it's impossible for the registry to not be loaded.
                // noinspection DataFlowIssue
                mapping.addProperty("block", BLOCKS.getKey(entry.getValue().getDefaultBlockState().getBlock()).toString());
                if (properties.size() != 0) mapping.add("state", properties);
                if (nbt.size() != 0) mapping.add("nbt", nbt);
                if (nbtStrict.size() != 0) mapping.add("nbt_strict", nbtStrict);
                keyMappings.add(String.valueOf(entry.getKey()), mapping);
            }

            JsonObject recipePattern = new JsonObject();
            for (int i = 0; i < this.recipe.size(); i++) {
                recipePattern.add("layer" + i, new JsonArray(this.recipe.get(i).length));

                for (int j = 0; j < this.recipe.get(i).length; j++) {
                    recipePattern.getAsJsonArray("layer" + i).add(this.recipe.get(i)[j]);
                }
            }

            JsonObject jsonResult = new JsonObject();
            jsonResult.addProperty("entity", this.result.toString());
            if (this.nbt != null) jsonResult.add("nbt", NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, this.nbt));

            pJson.add("key", keyMappings);
            pJson.add("recipe", recipePattern);
            pJson.add("result", jsonResult);

            if (this.requisites != null && !this.requisites.isEmpty()) {
                JsonArray jsonRequisites = new JsonArray();
                for (ItemStack item : this.requisites) {
                    JsonElement serialized = ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, item).get().orThrow();
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
