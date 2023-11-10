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

    @Nullable
    @Override
    public BlockPatternMatch find(LevelReader pLevel, BlockPos pPos) {
        LoadingCache<BlockPos, BlockInWorld> loadingcache = createLevelCache(pLevel, false);
        int i = Math.max(Math.max(this.getWidth(), this.getHeight()), this.getDepth());

        for(BlockPos blockpos : BlockPos.betweenClosed(pPos.offset(-i + 1, 0, -i + 1), pPos.offset(i - 1, i - 1, i - 1))) {
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
