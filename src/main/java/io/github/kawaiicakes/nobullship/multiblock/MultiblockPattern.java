package io.github.kawaiicakes.nobullship.multiblock;

import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public class MultiblockPattern extends BlockPattern {
    public static final Direction[] CARDINAL = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    protected final ImmutableList<BlockState> palette;
    protected final ImmutableList<ItemStack> totalBlocks;

    public MultiblockPattern(Predicate<BlockInWorld>[][][] pPattern, List<BlockState> palette, NonNullList<ItemStack> totalBlocks) {
        super(pPattern);
        this.palette = ImmutableList.copyOf(palette);
        this.totalBlocks = ImmutableList.copyOf(totalBlocks);
    }

    public boolean patternContains(BlockState state) {
        for (BlockState block : this.palette) {
            if (state.is(block.getBlock())) return true;
        }
        return false;
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
                    if (!this.pattern[k][j][i].test(pCache.getUnchecked(translateAndRotate(pPos, pFinger, pThumb, i, j, k)))) {
                        return null;
                    }
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
        // TODO
        return new CompoundTag();
    }

    public static MultiblockPattern fromNbt(CompoundTag nbt) {
        // TODO
        return new MultiblockPattern();
    }
}
