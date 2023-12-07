package io.github.kawaiicakes.nobullship.multiblock;

import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import io.github.kawaiicakes.nobullship.api.BlockInWorldPredicate;
import io.github.kawaiicakes.nobullship.api.BlockInWorldPredicateBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.*;

import static net.minecraft.world.level.block.Blocks.AIR;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

public class MultiblockPattern extends BlockPattern {
    public static final Direction[] CARDINAL = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    protected final ImmutableList<BlockState> palette;
    protected final ImmutableList<ItemStack> totalBlocks;
    @Nullable
    protected CompoundTag serializedPattern;

    /**
     * Serverside
     */
    public MultiblockPattern(BlockInWorldPredicate[][][] pPattern, List<BlockState> palette, NonNullList<ItemStack> totalBlocks, @Nullable CompoundTag serializedPattern) {
        super(pPattern);
        this.palette = ImmutableList.copyOf(palette);
        this.totalBlocks = ImmutableList.copyOf(totalBlocks);
        this.serializedPattern = serializedPattern;
    }

    public boolean patternContains(BlockState state) {
        for (BlockState block : this.palette) {
            if (state.is(block.getBlock())) return true;
        }
        return false;
    }

    @Nullable
    public CompoundTag getSerializedPattern() {
        if (this.serializedPattern == null) return null;
        return this.serializedPattern.copy();
    }

    public ImmutableList<BlockState> getPalette() {
        ImmutableList.Builder<BlockState> builder = ImmutableList.builder();
        for (BlockState state : this.palette) {
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
        for(int i = 0; i < this.width; ++i) {
            for(int j = 0; j < this.height; ++j) {
                for(int k = 0; k < this.depth; ++k) {
                    boolean hasMatchAt = ((BlockInWorldPredicate) this.pattern[k][j][i]).setFacing(pFinger).test(pCache.getUnchecked(translateAndRotate(pPos, pFinger, pThumb, i, j, k)));
                    if (!hasMatchAt) return null;
                }
            }
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

    public CompoundTag toNbt() {
        CompoundTag toReturn = new CompoundTag();

        ListTag paletteTag = new ListTag();
        for (BlockState block : this.palette) {
            Tag blockTag = BlockState.CODEC.encodeStart(NbtOps.INSTANCE, block).getOrThrow(false, null);
            if (!(blockTag instanceof CompoundTag compoundTag)) throw new RuntimeException("BlockState could not be parsed as a CompoundTag!");
            paletteTag.add(compoundTag);
        }
        toReturn.put("palette", paletteTag);

        ListTag totalBlocksTag = new ListTag();
        for (ItemStack stack : this.totalBlocks) {
           totalBlocksTag.add(stack.serializeNBT());
        }
        toReturn.put("total_blocks", totalBlocksTag);

        if (this.serializedPattern != null) toReturn.put("serialized_palette", this.serializedPattern);
        else throw new RuntimeException("The serialized pattern does not exist!");

        return toReturn;
    }

    public static MultiblockPattern fromNbt(CompoundTag nbt) {
        ListTag paletteTag = nbt.getList("palette", Tag.TAG_COMPOUND);
        ListTag totalBlocksTag = nbt.getList("total_blocks", Tag.TAG_COMPOUND);
        CompoundTag serializedTag = nbt.getCompound("serialized_palette");

        ListTag patternList = serializedTag.getList("pattern", Tag.TAG_LIST);
        List<String[]> pattern = new ArrayList<>(patternList.size());
        int patternHeight = 1;
        int patternWidth = 1;
        for (Tag patternTag : patternList) {
            ListTag patternListTag = (ListTag) patternTag;
            if (patternListTag.getElementType() != Tag.TAG_STRING) throw new IllegalArgumentException("Passed NBT does not contain valid type of list elements for pattern!");
            if (patternListTag.size() > patternHeight) patternHeight = patternListTag.size();
            List<String> tempList = new ArrayList<>(patternListTag.size());
            for (Tag string : patternListTag) {
                if (string.getAsString().length() > patternWidth) patternWidth = string.getAsString().length();
                tempList.add(string.getAsString());
            }
            pattern.add(tempList.toArray(new String[patternListTag.size()]));
        }

        CompoundTag originalPaletteTag = serializedTag.getCompound("palette");
        Map<String, BlockInWorldPredicateBuilder> paletteMap = new HashMap<>(originalPaletteTag.size());
        for (String key : originalPaletteTag.getAllKeys()) {
            CompoundTag tagAtKey = originalPaletteTag.getCompound(key);
            paletteMap.put(key, BlockInWorldPredicateBuilder.fromNbt(tagAtKey));
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
                        blockPredicate = BlockInWorldPredicateBuilder.of(AIR).build();
                    } else {
                        blockPredicate = paletteMap.get(stringAt).build();
                    }

                    predicate[i][j][k] = blockPredicate;
                }
            }
        }

        NonNullList<BlockState> paletteList = NonNullList.createWithCapacity(paletteTag.size());
        for (Tag tag : paletteTag) {
            BlockState deserialized = BlockState.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow(false, null);
            paletteList.add(deserialized);
        }

        NonNullList<ItemStack> totalBlocksList = NonNullList.createWithCapacity(totalBlocksTag.size());
        for (Tag itemTag : totalBlocksTag) {
            totalBlocksList.add(ItemStack.of((CompoundTag) itemTag));
        }

        return new MultiblockPattern(predicate, paletteList, totalBlocksList, serializedTag);
    }

    public static List<String[]> rawPatternFromNbt(CompoundTag patternTag) {
        ListTag patternList = patternTag.getList("pattern", Tag.TAG_LIST);
        List<String[]> pattern = new ArrayList<>(patternList.size());
        int patternHeight = 1;
        int patternWidth = 1;
        for (Tag tag : patternList) {
            ListTag patternListTag = (ListTag) tag;
            if (patternListTag.getElementType() != Tag.TAG_STRING) throw new IllegalArgumentException("Passed NBT does not contain valid type of list elements for pattern!");
            if (patternListTag.size() > patternHeight) patternHeight = patternListTag.size();
            List<String> tempList = new ArrayList<>(patternListTag.size());
            for (Tag string : patternListTag) {
                if (string.getAsString().length() > patternWidth) patternWidth = string.getAsString().length();
                tempList.add(string.getAsString());
            }
            pattern.add(tempList.toArray(new String[patternListTag.size()]));
        }
        return pattern;
    }

    public static Map<Character, BlockState> rawPaletteFromNbt(CompoundTag paletteTag, @Nullable Direction facing) {
        CompoundTag originalPaletteTag = paletteTag.getCompound("palette");
        Map<Character, BlockState> paletteMap = new HashMap<>(originalPaletteTag.size());
        for (String key : originalPaletteTag.getAllKeys()) {
            CompoundTag tagAtKey = originalPaletteTag.getCompound(key);

            BlockState blockstate = BlockState.CODEC.parse(NbtOps.INSTANCE, tagAtKey.get("blockState")).get().orThrow();
            BlockState forPalette = blockstate;

            if (tagAtKey.get("properties") instanceof ListTag propertiesTag) {
                for (Tag keyPairTag : propertiesTag) {
                    CompoundTag keyPair = (CompoundTag) keyPairTag;
                    CompoundTag propertyTag = keyPair.getCompound("property");

                    Property<?> propertyForBlock = blockstate.getBlock().getStateDefinition().getProperty(propertyTag.getString("name"));

                    if (propertyForBlock == null) throw new IllegalArgumentException("Passed NBT does not contain valid properties!");

                    ListTag valuesList = keyPair.getList("values", Tag.TAG_STRING);

                    if (propertyForBlock instanceof DirectionProperty && facing != null) {
                        forPalette = blockstate.setValue(HORIZONTAL_FACING,
                                BlockInWorldPredicate.rotateValue(HORIZONTAL_FACING.getValue(valuesList.getString(0)).orElse(Direction.NORTH), facing));
                        continue;
                    }

                    // FIXME: this is so scuffed lol
                    //noinspection unchecked
                    forPalette = blockstate.setValue(propertyForBlock.getClass().cast(propertyForBlock), Objects.requireNonNull(blockstate.getValue(propertyForBlock).getClass().cast(propertyForBlock.getValue(valuesList.getString(0)).orElse(null))));
                }
            }

            paletteMap.put(key.charAt(0), forPalette);
        }
        return paletteMap;
    }
}
