package io.github.kawaiicakes.nobullship.multiblock;

import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.nobullship.api.BlockInWorldPredicate;
import io.github.kawaiicakes.nobullship.api.BlockInWorldPredicateBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.github.kawaiicakes.nobullship.Registry.SCHEMATIC_BLOCK;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

public class MultiblockPattern extends BlockPattern {
    protected static final Logger LOGGER = LogUtils.getLogger();
    public static final Direction[] CARDINAL = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    protected final ImmutableList<BlockInWorldPredicateBuilder> palette;
    protected final ImmutableList<ItemStack> totalBlocks;
    @Nullable
    protected CompoundTag serializedPattern;

    /**
     * Serverside
     */
    public MultiblockPattern(BlockInWorldPredicate[][][] pPattern, List<BlockInWorldPredicateBuilder> palette, NonNullList<ItemStack> totalBlocks, @Nullable CompoundTag serializedPattern) {
        super(pPattern);
        this.palette = ImmutableList.copyOf(palette);
        this.totalBlocks = ImmutableList.copyOf(totalBlocks);
        this.serializedPattern = serializedPattern;
    }

    public boolean patternContains(BlockState state) {
        for (BlockInWorldPredicateBuilder block : this.palette) {
            for (BlockState blockState : block.getValidBlockstates()) {
                if (state.is(blockState.getBlock())) return true;
            }
        }
        return false;
    }

    @Nullable
    public CompoundTag getSerializedPattern() {
        if (this.serializedPattern == null) return null;
        return this.serializedPattern.copy();
    }

    public ImmutableList<BlockInWorldPredicateBuilder> getPalette() {
        ImmutableList.Builder<BlockInWorldPredicateBuilder> builder = ImmutableList.builder();
        for (BlockInWorldPredicateBuilder state : this.palette) {
            builder.add(state);
        }
        return builder.build();
    }

    public NonNullList<ItemStack> getTotalBlocks() {
        NonNullList<ItemStack> toReturn = NonNullList.createWithCapacity(this.totalBlocks.size());
        this.totalBlocks.forEach(stack -> toReturn.add(stack.copy()));
        return toReturn;
    }

    /**
     * Checks that the given pattern & rotation is at the block coordinates.
     */
    @Nullable
    @Override
    protected BlockPattern.BlockPatternMatch matches(BlockPos pPos, Direction pFinger, Direction pThumb, LoadingCache<BlockPos, BlockInWorld> pCache) {
        try {
            for (int i = 0; i < this.width; ++i) {
                for (int j = 0; j < this.height; ++j) {
                    for (int k = 0; k < this.depth; ++k) {
                        boolean hasMatchAt = ((BlockInWorldPredicate) this.pattern[k][j][i]).setFacing(pFinger).test(pCache.getUnchecked(translateAndRotate(pPos, pFinger, pThumb, i, j, k)));
                        if (!hasMatchAt) return null;
                    }
                }
            }
        } catch (RuntimeException e) {
            LOGGER.error("Exception while testing block predicates against world:", e);
            return null;
        }

        return new BlockPattern.BlockPatternMatch(pPos, pFinger, pThumb, pCache, this.width, this.height, this.depth);
    }

    /**
     * In the superclass, this method creates an <code>Iterator</code> containing all the <code>BlockPos</code> inside
     * a volume specified by the positions of its furthest-apart corners. This volume is equal to a cube of a length
     * equal to the longest side of the <code>BlockPattern</code>. For every position in this volume, and for every
     * possible direction the pattern may be facing, a test is conducted to see if the pattern exists there.
     * <br><br>
     * This is explained in detail here to be referenced when I optimize this logic... For now, I've actually made the
     * performance worse since the previous volume was too small to properly account for asymmetric 3D patterns.
     * <br><br>
     * I've since only made this check 4 orientations as opposed to 24; namely the four cardinal directions while upright.
     * This should significantly improve performance for minimal tradeoff. I've also added an early return if the checked
     * block at the <code>BlockPos</code> does not exist in the recipe.
     */
    @Nullable
    @Override
    public BlockPatternMatch find(LevelReader pLevel, BlockPos pPos) {
        LoadingCache<BlockPos, BlockInWorld> loadingcache = createLevelCache(pLevel, false);
        int i = Math.max(Math.max(this.getWidth(), this.getHeight()), this.getDepth());

        BlockState blockAt = pLevel.getBlockState(pPos);
        if (!this.patternContains(blockAt)) return null;

        for(BlockPos blockpos : BlockPos.betweenClosed(pPos.offset(-i + 1, -i + 1, -i + 1), pPos.offset(i - 1, i - 1, i - 1))) {
            for (Direction direction : CARDINAL) {
                BlockPattern.BlockPatternMatch match = this.matches(blockpos, direction, Direction.UP, loadingcache);
                if (match != null) {
                    return match;
                }
            }
        }

        return null;
    }

    @Nullable
    public BlockPatternMatch findExact(LevelReader pLevel, BlockPos pPos, int[] schematicBlockOffset) {
        if (schematicBlockOffset.length != 3) return null;
        LoadingCache<BlockPos, BlockInWorld> loadingcache = createLevelCache(pLevel, false);
        BlockState blockAt = pLevel.getBlockState(pPos);
        if (!blockAt.is(SCHEMATIC_BLOCK.get()) || !this.patternContains(blockAt)) return null;
        Direction direction = blockAt.getValue(HORIZONTAL_FACING);
        BlockPos offsetPos = translateAndRotate(pPos, direction, Direction.UP, -schematicBlockOffset[0], -schematicBlockOffset[1], -schematicBlockOffset[2]);
        return this.matches(offsetPos, direction, Direction.UP, loadingcache);
    }

    /**
     * It was a bit hard to read what <code>BlockPattern#translateAndRotate</code> was doing, so I redid it here.
     */
    public static BlockPos translateAndRotate(BlockPos pPos, Direction pFinger, Direction pThumb, int pPalmOffset, int pThumbOffset, int pFingerOffset) {
        if (pFinger == pThumb || pFinger == pThumb.getOpposite()) throw new IllegalArgumentException("Invalid forwards & up combination");

        final Vec3i fingerNormal = pFinger.getNormal();
        final Vec3i thumbNormal = pThumb.getNormal();
        final Vec3i palmNormal = fingerNormal.cross(thumbNormal);

        return pPos.offset(
                thumbNormal.getX() * -pThumbOffset + palmNormal.getX() * pPalmOffset + fingerNormal.getX() * pFingerOffset,
                thumbNormal.getY() * -pThumbOffset + palmNormal.getY() * pPalmOffset + fingerNormal.getY() * pFingerOffset,
                thumbNormal.getZ() * -pThumbOffset + palmNormal.getZ() * pPalmOffset + fingerNormal.getZ() * pFingerOffset
        );
    }

    /**
     * Returns null if pattern is malformed.
     */
    @Nullable
    public CompoundTag toNbt() {
        CompoundTag toReturn;

        try {
            toReturn = new CompoundTag();

            ListTag paletteTag = new ListTag();
            for (BlockInWorldPredicateBuilder block : this.palette) {
                paletteTag.add(block.toNbt());
            }
            toReturn.put("palette", paletteTag);

            ListTag totalBlocksTag = new ListTag();
            for (ItemStack stack : this.totalBlocks) {
                totalBlocksTag.add(stack.serializeNBT());
            }
            toReturn.put("total_blocks", totalBlocksTag);

            if (this.serializedPattern != null) toReturn.put("serialized_palette", this.serializedPattern);
            else throw new RuntimeException("The serialized pattern does not exist!");
        } catch (RuntimeException e) {
            LOGGER.error("Error serializing pattern to NBT!", e);
            return null;
        }

        return toReturn;
    }

    @Nullable
    public static MultiblockPattern fromNbt(CompoundTag nbt) {
        ListTag paletteTag = nbt.getList("palette", Tag.TAG_COMPOUND);
        ListTag totalBlocksTag = nbt.getList("total_blocks", Tag.TAG_COMPOUND);
        CompoundTag serializedTag = nbt.getCompound("serialized_palette");

        ListTag patternList = serializedTag.getList("pattern", Tag.TAG_LIST);
        List<String[]> pattern = new ArrayList<>(patternList.size());
        int patternHeight = 1;
        int patternWidth = 1;

        try {
            for (Tag patternTag : patternList) {
                ListTag patternListTag = (ListTag) patternTag;
                if (patternListTag.getElementType() != Tag.TAG_STRING)
                    throw new IllegalArgumentException("Passed NBT does not contain valid type of list elements for pattern!");
                if (patternListTag.size() > patternHeight) patternHeight = patternListTag.size();
                List<String> tempList = new ArrayList<>(patternListTag.size());
                for (Tag string : patternListTag) {
                    if (string.getAsString().length() > patternWidth) patternWidth = string.getAsString().length();
                    tempList.add(string.getAsString());
                }
                pattern.add(tempList.toArray(new String[patternListTag.size()]));
            }
        } catch (IllegalArgumentException e) {
            LOGGER.error("Unable to deserialize pattern from NBT!");
            return null;
        }

        CompoundTag originalPaletteTag = serializedTag.getCompound("palette");
        Map<String, BlockInWorldPredicateBuilder> paletteMap = new HashMap<>(originalPaletteTag.size());
        for (String key : originalPaletteTag.getAllKeys()) {
            CompoundTag tagAtKey = originalPaletteTag.getCompound(key);
            BlockInWorldPredicateBuilder builder = BlockInWorldPredicateBuilder.fromNbt(tagAtKey);
            if (builder == null) {
                LOGGER.error("Unable to deserialize BlockInWorldPredicate from NBT!");
                return null;
            }
            paletteMap.put(key, builder);
        }

        BlockInWorldPredicate[][][] predicate = (BlockInWorldPredicate[][][]) Array.newInstance(BlockInWorldPredicate.class, pattern.size(), patternHeight, patternWidth);

        for(int i = 0; i < pattern.size(); ++i) {
            for(int j = 0; j < patternHeight; ++j) {
                for(int k = 0; k < patternWidth; ++k) {
                    String stringAt = String.valueOf((pattern.get(i))[j].charAt(k));
                    BlockInWorldPredicate blockPredicate;

                    if (stringAt.equals("$")) {
                        blockPredicate = BlockInWorldPredicate.WILDCARD;
                    } else if (stringAt.equals(" ")) {
                        blockPredicate = BlockInWorldPredicate.AIR;
                    } else {
                        blockPredicate = paletteMap.get(stringAt).build();
                    }

                    predicate[i][j][k] = blockPredicate;
                }
            }
        }

        NonNullList<BlockInWorldPredicateBuilder> paletteList = NonNullList.createWithCapacity(paletteTag.size());
        try {
            for (Tag tag : paletteTag) {
                CompoundTag deserialized = (CompoundTag) tag;
                paletteList.add(BlockInWorldPredicateBuilder.fromNbt(deserialized));
            }
        } catch (RuntimeException e) {
            LOGGER.error("BlockState unable to be deserialized from NBT!", e);
            return null;
        }

        NonNullList<ItemStack> totalBlocksList = NonNullList.createWithCapacity(totalBlocksTag.size());
        for (Tag itemTag : totalBlocksTag) {
            totalBlocksList.add(ItemStack.of((CompoundTag) itemTag));
        }

        return new MultiblockPattern(predicate, paletteList, totalBlocksList, serializedTag);
    }

    @Nullable
    public static List<String[]> rawPatternFromNbt(CompoundTag patternTag) {
        ListTag patternList = patternTag.getList("pattern", Tag.TAG_LIST);
        if (patternList.isEmpty()) return null;

        List<String[]> pattern = new ArrayList<>(patternList.size());
        int patternHeight = 1;
        int patternWidth = 1;

        try {
            for (Tag tag : patternList) {
                ListTag patternListTag = (ListTag) tag;
                if (patternListTag.getElementType() != Tag.TAG_STRING)
                    throw new IllegalArgumentException("Passed NBT does not contain valid type of list elements for pattern!");
                if (patternListTag.size() > patternHeight) patternHeight = patternListTag.size();
                List<String> tempList = new ArrayList<>(patternListTag.size());
                for (Tag string : patternListTag) {
                    if (string.getAsString().length() > patternWidth) patternWidth = string.getAsString().length();
                    tempList.add(string.getAsString());
                }
                pattern.add(tempList.toArray(new String[patternListTag.size()]));
            }
        } catch (IllegalArgumentException e) {
            LOGGER.error(e.getMessage());
            return null;
        }

        return pattern;
    }

    @Nullable
    public static Map<Character, BlockInWorldPredicateBuilder> rawPaletteFromNbt(CompoundTag paletteTag) {
        CompoundTag originalPaletteTag = paletteTag.getCompound("palette");
        if (originalPaletteTag.isEmpty()) return null;

        Map<Character, BlockInWorldPredicateBuilder> paletteMap = new HashMap<>(originalPaletteTag.size());
        for (String key : originalPaletteTag.getAllKeys()) {
            CompoundTag tagAtKey = originalPaletteTag.getCompound(key);
            if (tagAtKey.isEmpty()) {
                LOGGER.error("Palette key at {} is empty!", key);
                return null;
            }

            BlockInWorldPredicateBuilder forPalette = BlockInWorldPredicateBuilder.fromNbt(tagAtKey);
            if (forPalette == null) {
                LOGGER.error("Unable to deserialize block predicate for {}! Palette was disposed!", key);
                return null;
            }

            paletteMap.put(key.charAt(0), forPalette);
        }
        return paletteMap;
    }
}
