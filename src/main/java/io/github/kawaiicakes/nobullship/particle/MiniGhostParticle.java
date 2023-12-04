package io.github.kawaiicakes.nobullship.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class MiniGhostParticle extends Particle {
    protected MiniGhostParticle(ClientLevel pLevel, double pX, double pY, double pZ) {
        super(pLevel, pX, pY, pZ);
    }

    @Override
    public void render(VertexConsumer pBuffer, Camera pRenderInfo, float pPartialTicks) {

    }

    @Override
    public ParticleRenderType getRenderType() {
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    public static class MiniGhostParticleProvider implements ParticleProvider<MiniGhostParticleOption> {
        @Nullable
        @Override
        public Particle createParticle(MiniGhostParticleOption pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
            return null;
        }
    }
}
