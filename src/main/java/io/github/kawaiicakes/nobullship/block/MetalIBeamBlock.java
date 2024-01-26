package io.github.kawaiicakes.nobullship.block;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;

public class MetalIBeamBlock extends FullLengthBeamBlock {
    public MetalIBeamBlock() {
        super(
                BlockBehaviour.Properties.of(Material.METAL)
                    .sound(SoundType.ANVIL)
                    .noOcclusion()
                    .isViewBlocking((x,y,z) -> false)
                    .isValidSpawn((w,x,y,z) -> !w.getValue(VERTICAL))
                    .requiresCorrectToolForDrops()
                    .strength(5.0F, 6.0F),
                new double[][]{
                        new double[]{6.0, 5.0, 0.0, 10, 6.0, 16},
                        new double[]{7.5, 6.0, 0.0, 8.5, 10.0, 16.0},
                        new double[]{6.0, 10, 0.0, 10, 11, 16}
                },
                new double[][]{
                        new double[]{10, 5.0, 6.0, 16, 6.0, 10},
                        new double[]{8.0, 6.0, 7.5, 16, 10, 8.5},
                        new double[]{10, 10, 6.0, 16, 11, 10}
                },
                new double[][]{
                        new double[]{8.0, 5.025, 6.0, 16, 5.975, 10},
                        new double[]{8.0, 5.975, 7.5, 16, 10.025, 8.5},
                        new double[]{8.0, 10.025, 6.0, 16, 10.975, 10}
                },
                new double[][]{
                        new double[]{6.0, 8.0, 5.0, 10.0, 16.0, 6.0},
                        new double[]{7.5, 11, 6.0, 8.5, 16, 10},
                        new double[]{6.0, 8.0, 10, 10.0, 16.0, 11}
                }
        );
    }
}
