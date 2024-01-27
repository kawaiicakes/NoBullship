package io.github.kawaiicakes.nobullship.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.Material;
import org.jetbrains.annotations.Nullable;

import static io.github.kawaiicakes.nobullship.Registry.METAL_BEAM_BLOCK;
import static io.github.kawaiicakes.nobullship.block.WheelBlock.FACING;

public class WoodSupportBeamBlock extends FullLengthBeamBlock {
    public WoodSupportBeamBlock() {
        super(
                BlockBehaviour.Properties.of(Material.WOOD)
                        .sound(SoundType.LADDER)
                        .noOcclusion()
                        .isViewBlocking((x,y,z) -> false)
                        .isValidSpawn((w,x,y,z) -> !w.getValue(VERTICAL))
                        .requiresCorrectToolForDrops()
                        .strength(3.2F, 4.0F),
                new double[][]{
                        new double[]{
                                6.5, 5.0, 0, 9.5, 11, 16
                        }
                },
                new double[][]{
                        new double[]{
                                9.5, 5.0, 6.5, 16, 11, 9.5
                        }
                },
                new double[][]{
                        new double[]{
                                9.5, 5.0, 6.5, 16, 11, 9.5
                        }
                },
                new double[][]{
                        new double[]{
                                6.5, 8, 5, 9.5, 16, 11
                        }
                }
        );
    }
}
