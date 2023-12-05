package io.github.kawaiicakes.nobullship.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class MiniGhostParticle extends TextureSheetParticle {
    protected MiniGhostParticle(ClientLevel pLevel, double pX, double pY, double pZ, BlockState block) {
        super(pLevel, pX, pY, pZ);
        //noinspection deprecation
        this.setSprite(Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(block));
        this.gravity = 0.0F;
        this.lifetime = 1;
        this.hasPhysics = false;
        this.quadSize = 0.15F;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.TERRAIN_SHEET;
    }

    public float getQuadSize(float pScaleFactor) {
        return 0.15F;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<BlockParticleOption> {
        @Nullable
        @Override
        public Particle createParticle(BlockParticleOption pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
            return new MiniGhostParticle(pLevel, pX, pY, pZ, pType.getState());
        }
    }
}
