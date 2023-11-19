package io.github.kawaiicakes.nobullship.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import org.jetbrains.annotations.Nullable;

public class MiniGhostParticleProvider implements ParticleProvider<MiniGhostParticleOption> {
    @Nullable
    @Override
    public Particle createParticle(MiniGhostParticleOption pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
        return null;
    }
}
