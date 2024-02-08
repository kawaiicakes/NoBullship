package io.github.kawaiicakes.nobullship.api.multiblock;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import io.github.kawaiicakes.nobullship.api.BlockInWorldPredicate;
import io.github.kawaiicakes.nobullship.api.BlockInWorldPredicateBuilder;
import io.github.kawaiicakes.nobullship.multiblock.MultiblockPattern;
import net.minecraft.FieldsAreNonnullByDefault;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.github.kawaiicakes.nobullship.Registry.SCHEMATIC_BLOCK_ITEM;

/**
 * Bears no relation to <code>FinishedMultiblockRecipe</code>. Simply an immutable data carrier intended to cache
 * multiblock recipes.
 */
@ParametersAreNonnullByDefault
@FieldsAreNonnullByDefault
public record MultiblockRecipe(
        @Nullable String resultingEntityName,
        MultiblockPattern recipe,
        ResourceLocation result,
        @Nullable CompoundTag nbt,
        @Nullable ImmutableList<ItemStack> requisites,
        boolean hasSchematicBlock,
        int[] schematicBlockOffset
) {
    private static final Logger LOGGER = LogUtils.getLogger();
    @Override
    public MultiblockPattern recipe() {
        return new MultiblockPattern(
                (BlockInWorldPredicate[][][]) this.recipe.getPattern(),
                this.recipe.getPalette(),
                this.recipe.getTotalBlocks(),
                this.recipe.getSerializedPattern()
        );
    }

    @Override
    public ResourceLocation result() {
        return new ResourceLocation(this.result.toString());
    }

    @Nullable
    @Override
    public CompoundTag nbt() {
        return this.nbt != null ? this.nbt.copy() : null;
    }

    /**
     * This shit is made like this to ensure I return a deep copy; it kept returning mutable values
     */
    @Nullable
    @Override
    public ImmutableList<ItemStack> requisites() {
        if (this.requisites == null) return null;
        ImmutableList.Builder<ItemStack> toReturn = ImmutableList.builder();
        for (ItemStack stack : this.requisites) {
            toReturn.add(stack.copy());
        }
        return toReturn.build();
    }

    /**
     * Serializes this recipe into a <code>CompoundTag</code>. Returns null if the recipe is malformed.
     */
    @Nullable
    public CompoundTag toNbt() {
        CompoundTag toReturn = new CompoundTag();

        CompoundTag pattern = this.recipe.toNbt();
        if (pattern == null) return null;

        toReturn.put("recipe", pattern);

        if (this.resultingEntityName != null) {
            toReturn.putString("name", this.resultingEntityName);
        }

        toReturn.putString("result", this.result.toString());

        if (this.nbt != null) toReturn.put("nbt", this.nbt);

        if (this.requisites != null && !this.requisites.isEmpty()) {
            ListTag requisiteList = new ListTag();

            for (ItemStack item : this.requisites) {
                requisiteList.add(item.serializeNBT());
            }

            toReturn.put("requisites", requisiteList);
        }

        toReturn.putBoolean("hasSchematicBlock", this.hasSchematicBlock);
        toReturn.putIntArray("schematicBlockOffset", this.schematicBlockOffset);

        return toReturn;
    }

    /**
     * Deserializes a <code>CompoundTag</code> into a <code>MultiblockRecipe</code>.
     * Returns null if the NBT tag is malformed.
     */
    @Nullable
    public static MultiblockRecipe fromNbt(CompoundTag nbt) throws IllegalArgumentException {
        if (!(nbt.get("recipe") instanceof CompoundTag recipeNbt)) throw new IllegalArgumentException("Passed NBT does not contain a recipe!");
        if (!(nbt.get("result") instanceof StringTag stringNbt)) throw new IllegalArgumentException("Passed NBT does not contain a string!");

        MultiblockPattern pattern = MultiblockPattern.fromNbt(recipeNbt);
        if (pattern == null) return null;

        String name = null;
        if (nbt.get("name") instanceof StringTag stringTag) name = stringTag.getAsString();

        CompoundTag resultNbt = null;
        if (nbt.get("nbt") instanceof CompoundTag serializedResult) resultNbt = serializedResult.copy();

        ImmutableList<ItemStack> deserializedRequisites = null;
        if (nbt.get("requisites") instanceof ListTag requisiteList && requisiteList.getElementType() == Tag.TAG_COMPOUND) {
            ImmutableList.Builder<ItemStack> builder = new ImmutableList.Builder<>();

            for (Tag value : requisiteList) {
                builder.add(ItemStack.of((CompoundTag) value));
            }

            deserializedRequisites = builder.build();
        }

        boolean hasSchematicBlock = nbt.getBoolean("hasSchematicBlock");
        int[] schematicBlockOffset = nbt.getIntArray("schematicBlockOffset");

        return new MultiblockRecipe(
                name,
                pattern,
                new ResourceLocation(stringNbt.getAsString()),
                resultNbt,
                deserializedRequisites,
                hasSchematicBlock,
                schematicBlockOffset
        );
    }

    /**
     * Returns a <code>MultiblockRecipe</code> built from the kind of NBT used in vanilla structure NBTs.
     */
    @Nullable
    public static MultiblockRecipe fromRawNbt(CompoundTag nbt, ResourceLocation resultId) {
        try {
            if (!(nbt.get("blocks") instanceof ListTag blocksTag))
                throw new IllegalArgumentException("Passed NBT is malformed!");
            if (!(nbt.get("palette") instanceof ListTag paletteTag))
                throw new IllegalArgumentException("Passed NBT is malformed!");
            if (blocksTag.getElementType() != Tag.TAG_COMPOUND)
                throw new IllegalArgumentException("Passed NBT is malformed!");
            if (paletteTag.getElementType() != Tag.TAG_COMPOUND)
                throw new IllegalArgumentException("Passed NBT is malformed!");

            int[] size = new int[3];

            if (nbt.get("size") instanceof IntArrayTag sizeTag) {
                if (sizeTag.size() < 3)
                    throw new IllegalArgumentException("Passed NBT is malformed!");

                for (int i = 0; i < sizeTag.size(); i++) {
                    size[i] = sizeTag.getAsIntArray()[i];
                }
            } else if (nbt.get("size") instanceof ListTag sizeTag) {
                if (sizeTag.size() < 3)
                    throw new IllegalArgumentException("Passed NBT is malformed!");
                if (sizeTag.getElementType() != IntTag.TAG_INT)
                    throw new IllegalArgumentException("Passed NBT is malformed!");

                for (int i = 0; i < sizeTag.size(); i++) {
                    size[i] = sizeTag.getInt(i);
                }
            } else {
                throw new IllegalArgumentException("Passed NBT is malformed!");
            }

            MultiblockPatternBuilder patternBuilder = MultiblockPatternBuilder.of(resultId);

            List<Pair<Character, BlockState>> orderedMappedPalette = new ArrayList<>(paletteTag.size());
            for (Tag rawBlockStateTag : paletteTag) {
                if (!(rawBlockStateTag instanceof CompoundTag blockStateTag))
                    throw new IllegalArgumentException("Passed NBT is malformed!");

                int index = paletteTag.indexOf(rawBlockStateTag);
                char ch = (char) index;
                if (ch == ' ' || ch == '$') ch = (char) (0xFFFF - index);
                BlockState blockState = BlockState.CODEC.parse(NbtOps.INSTANCE, blockStateTag).getOrThrow(false, LOGGER::error);

                orderedMappedPalette.add(index, Pair.of(ch, blockState));
                patternBuilder.where(ch, BlockInWorldPredicateBuilder.of(blockState));
            }

            int[][][] pattern = new int[size[0]][size[1]][size[2]];
            for (Tag rawPatternTag : blocksTag) {
                if (!(rawPatternTag instanceof CompoundTag patternTag))
                    throw new IllegalArgumentException("Passed NBT is malformed!");
                if (!(patternTag.get("state") instanceof IntTag intTag))
                    throw new IllegalArgumentException("Passed NBT is malformed!");

                int[] pos = new int[3];

                if (patternTag.get("pos") instanceof IntArrayTag posTag) {
                    if (posTag.size() < 3)
                        throw new IllegalArgumentException("Passed NBT is malformed!");

                    for (int i = 0; i < posTag.size(); i++) {
                        pos[i] = posTag.getAsIntArray()[i];
                    }
                } else if (patternTag.get("pos") instanceof ListTag posTag) {
                    if (posTag.size() < 3)
                        throw new IllegalArgumentException("Passed NBT is malformed!");
                    if (posTag.getElementType() != IntTag.TAG_INT)
                        throw new IllegalArgumentException("Passed NBT is malformed!");

                    for (int i = 0; i < posTag.size(); i++) {
                        pos[i] = posTag.getInt(i);
                    }
                } else {
                    throw new IllegalArgumentException("Passed NBT is malformed!");
                }

                int patternX = pos[0];
                int patternY = pos[1];
                int patternZ = pos[2];

                pattern[patternX][(size[1] - 1) - patternY][(size[2] - 1) - patternZ] = intTag.getAsInt();
            }

            for (int depth = 0; depth < size[2]; depth++) {
                String[] yList = new String[size[1]];
                for (int height = 0; height < size[1]; height++) {
                    char[] chars = new char[size[0]];
                    for (int width = 0; width < size[0]; width++) {
                        chars[width] = orderedMappedPalette.get(pattern[width][height][depth]).getFirst();
                    }
                    yList[height] = (new String(chars));
                }
                patternBuilder.aisle(yList);
            }

            return new MultiblockRecipe(null, patternBuilder.build(), resultId, null, null, patternBuilder.hasSchematicBlock, new int[3]);

        } catch (RuntimeException e) {
            LOGGER.error("Error creating recipe from raw NBT!", e);
            return null;
        }
    }

    /**
     * Essentially a Json deserializer. Returns null if the argument contains syntax errors.
     */
    @Nullable
    public static MultiblockRecipe fromJson(JsonObject json, ICondition.IContext conditionContext) {
        JsonArray jsonConditions = null;
        if (json.has("conditions") && json.get("conditions").isJsonArray())
            jsonConditions = json.get("conditions").getAsJsonArray();
        JsonObject jsonKeys = json.getAsJsonObject("key");
        JsonObject jsonRecipe = json.getAsJsonObject("recipe");
        JsonObject jsonResult = json.getAsJsonObject("result");
        JsonArray jsonRequisites = json.getAsJsonArray("requisites");

        if (jsonKeys == null || jsonRecipe == null || jsonResult == null) {
            LOGGER.error("Sussy JSON syntax!");
            return null;
        }

        if (jsonConditions != null && !CraftingHelper.processConditions(jsonConditions, conditionContext)) return null;

        try {
            String name = null;
            if (jsonResult.has("name") && jsonResult.get("name").isJsonPrimitive()) {
                name = jsonResult.getAsJsonPrimitive("name").getAsString();
            }

            ResourceLocation result = new ResourceLocation(jsonResult.getAsJsonPrimitive("entity").getAsString());

            CompoundTag nbt = null;
            if (jsonResult.has("nbt")) {
                nbt = (CompoundTag) JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, jsonResult.get("nbt"));
            }

            byte schematicBlockChars = 0;
            Character schematicChar = null;

            MultiblockPatternBuilder builder = MultiblockPatternBuilder.of(result);

            for (Map.Entry<String, JsonElement> keyEntry : jsonKeys.entrySet()) {
                if (Objects.equals(keyEntry.getKey(), " ") || Objects.equals(keyEntry.getKey(), "$")) {
                    LOGGER.error("{} is a reserved character!", keyEntry.getKey());
                    return null;
                }

                BlockInWorldPredicateBuilder definitionForChar = BlockInWorldPredicateBuilder.fromJson(keyEntry.getValue().getAsJsonObject());

                if (schematicBlockChars > 1) {
                    LOGGER.error("You may not define two characters as being both schematic blocks!");
                    return null;
                }

                if ((definitionForChar.isForBlock() || definitionForChar.isForBlockState())
                        && definitionForChar.getItemized().is(SCHEMATIC_BLOCK_ITEM.get())) {
                    schematicBlockChars++;
                    schematicChar = keyEntry.getKey().charAt(0);
                }

                builder.where(keyEntry.getKey().charAt(0), definitionForChar);
            }

            byte numberOfSchematicBlocks = 0;
            int[] schematicBlockOffset = new int[3];
            for (int i = jsonRecipe.size() - 1; i >= 0 ; i--) {
                JsonArray aisle = jsonRecipe.getAsJsonArray("layer" + i);
                if (aisle.isEmpty()) return null;

                List<String> strings = new ArrayList<>(aisle.size());
                aisle.forEach(element -> strings.add(element.getAsString()));

                builder.aisle(strings.toArray(String[]::new));

                if (schematicChar == null) continue;
                int j = 0;
                for (String string : strings) {
                    int k = -1;
                    for (char blockChar : string.toCharArray()) {
                        k++;
                        if (numberOfSchematicBlocks > 1) {
                            LOGGER.error("You may not use more than one schematic block in a recipe!");
                            return null;
                        }
                        if (blockChar != schematicChar) continue;
                        numberOfSchematicBlocks++;
                        schematicBlockOffset[0] = (string.length() - 1) - k;
                        schematicBlockOffset[1] = j;
                        schematicBlockOffset[2] = (jsonRecipe.size() - 1) - i;
                    }
                    j++;
                }
            }

            NonNullList<ItemStack> requisites;
            ImmutableList<ItemStack> toReturnRequisites = null;
            if (jsonRequisites != null) {
                requisites = NonNullList.createWithCapacity(jsonRequisites.size());
                for (JsonElement element : jsonRequisites) {
                    if (element.isJsonObject()) requisites.add(ShapedRecipe.itemStackFromJson(element.getAsJsonObject()));
                }
                toReturnRequisites = ImmutableList.copyOf(requisites);
            }

            return new MultiblockRecipe(name, builder.build(), result, nbt, toReturnRequisites, schematicChar != null, schematicBlockOffset);
        } catch (RuntimeException e) {
            LOGGER.error("An error occurred during deserialization of BlockInWorldPredicate from JSON!", e);
            LOGGER.error(e.getMessage());
            return null;
        }
    }

    /**
     * This overload was written - very lazily at that - for use by the magic wand item.
     */
    @Nullable
    public static MultiblockPatternBuilder fromRawNbt(CompoundTag nbt, String pathName) {
        try {
            if (!(nbt.get("blocks") instanceof ListTag blocksTag))
                throw new IllegalArgumentException("Passed NBT is malformed!");
            if (!(nbt.get("palette") instanceof ListTag paletteTag))
                throw new IllegalArgumentException("Passed NBT is malformed!");
            if (blocksTag.getElementType() != Tag.TAG_COMPOUND)
                throw new IllegalArgumentException("Passed NBT is malformed!");
            if (paletteTag.getElementType() != Tag.TAG_COMPOUND)
                throw new IllegalArgumentException("Passed NBT is malformed!");

            int[] size = new int[3];

            if (nbt.get("size") instanceof IntArrayTag sizeTag) {
                if (sizeTag.size() < 3)
                    throw new IllegalArgumentException("Passed NBT is malformed!");

                for (int i = 0; i < sizeTag.size(); i++) {
                    size[i] = sizeTag.getAsIntArray()[i];
                }
            } else if (nbt.get("size") instanceof ListTag sizeTag) {
                if (sizeTag.size() < 3)
                    throw new IllegalArgumentException("Passed NBT is malformed!");
                if (sizeTag.getElementType() != IntTag.TAG_INT)
                    throw new IllegalArgumentException("Passed NBT is malformed!");

                for (int i = 0; i < sizeTag.size(); i++) {
                    size[i] = sizeTag.getInt(i);
                }
            } else {
                throw new IllegalArgumentException("Passed NBT is malformed!");
            }

            MultiblockPatternBuilder patternBuilder = MultiblockPatternBuilder.of(new ResourceLocation(pathName));

            List<Pair<Character, BlockState>> orderedMappedPalette = new ArrayList<>(paletteTag.size());
            for (Tag rawBlockStateTag : paletteTag) {
                if (!(rawBlockStateTag instanceof CompoundTag blockStateTag))
                    throw new IllegalArgumentException("Passed NBT is malformed!");

                int index = paletteTag.indexOf(rawBlockStateTag);
                char ch = (char) index;
                if (ch == ' ' || ch == '$') ch = (char) (0xFFFF - index);
                BlockState blockState = BlockState.CODEC.parse(NbtOps.INSTANCE, blockStateTag).getOrThrow(false, LOGGER::error);

                orderedMappedPalette.add(index, Pair.of(ch, blockState));
                patternBuilder.where(ch, BlockInWorldPredicateBuilder.of(blockState));
            }

            int[][][] pattern = new int[size[0]][size[1]][size[2]];
            for (Tag rawPatternTag : blocksTag) {
                if (!(rawPatternTag instanceof CompoundTag patternTag))
                    throw new IllegalArgumentException("Passed NBT is malformed!");
                if (!(patternTag.get("state") instanceof IntTag intTag))
                    throw new IllegalArgumentException("Passed NBT is malformed!");

                int[] pos = new int[3];

                if (patternTag.get("pos") instanceof IntArrayTag posTag) {
                    if (posTag.size() < 3)
                        throw new IllegalArgumentException("Passed NBT is malformed!");

                    for (int i = 0; i < posTag.size(); i++) {
                        pos[i] = posTag.getAsIntArray()[i];
                    }
                } else if (patternTag.get("pos") instanceof ListTag posTag) {
                    if (posTag.size() < 3)
                        throw new IllegalArgumentException("Passed NBT is malformed!");
                    if (posTag.getElementType() != IntTag.TAG_INT)
                        throw new IllegalArgumentException("Passed NBT is malformed!");

                    for (int i = 0; i < posTag.size(); i++) {
                        pos[i] = posTag.getInt(i);
                    }
                } else {
                    throw new IllegalArgumentException("Passed NBT is malformed!");
                }

                int patternX = pos[0];
                int patternY = pos[1];
                int patternZ = pos[2];

                pattern[patternX][(size[1] - 1) - patternY][(size[2] - 1) - patternZ] = intTag.getAsInt();
            }

            for (int depth = 0; depth < size[2]; depth++) {
                String[] yList = new String[size[1]];
                for (int height = 0; height < size[1]; height++) {
                    char[] chars = new char[size[0]];
                    for (int width = 0; width < size[0]; width++) {
                        chars[width] = orderedMappedPalette.get(pattern[width][height][depth]).getFirst();
                    }
                    yList[height] = (new String(chars));
                }
                patternBuilder.aisle(yList);
            }

            return patternBuilder;
        } catch (RuntimeException e) {
            LOGGER.error("Error creating recipe from raw NBT!", e);
            return null;
        }
    }
}
