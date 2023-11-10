package io.github.kawaiicakes.nobullship.data;

import com.google.common.cache.LoadingCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class MultiblockPattern extends BlockPattern {
    public MultiblockPattern(Predicate<BlockInWorld>[][][] pPattern) {
        super(pPattern);
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
     */
    @Nullable
    @Override
    public BlockPatternMatch find(LevelReader pLevel, BlockPos pPos) {
        LoadingCache<BlockPos, BlockInWorld> loadingcache = createLevelCache(pLevel, false);
        int i = Math.max(Math.max(this.getWidth(), this.getHeight()), this.getDepth());

        for(BlockPos blockpos : BlockPos.betweenClosed(pPos.offset(-i + 1, -i + 1, -i + 1), pPos.offset(i - 1, i - 1, i - 1))) {
            for(Direction direction : Direction.values()) {
                for(Direction direction1 : Direction.values()) {
                    if (direction1 != direction && direction1 != direction.getOpposite()) {
                        BlockPattern.BlockPatternMatch blockpattern$blockpatternmatch = this.matches(blockpos, direction, direction1, loadingcache);
                        if (blockpattern$blockpatternmatch != null) {
                            return blockpattern$blockpatternmatch;
                        }
                    }
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
}
